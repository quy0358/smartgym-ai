package ntu.quy65132908.smartgym_ai.ui.workout;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Exercise;
import ntu.quy65132908.smartgym_ai.data.model.ExerciseCatalogFilters;
import ntu.quy65132908.smartgym_ai.data.model.ExerciseCatalogItem;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ExerciseCatalogRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;
import ntu.quy65132908.smartgym_ai.util.DateUtils;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

@HiltViewModel
public class ExerciseLibraryViewModel extends ViewModel {
    private final ExerciseCatalogRepository catalogRepository;
    private final WorkoutRepository workoutRepository;
    private final AuthRepository authRepository;
    private final Context appContext;

    private final MutableLiveData<List<ExerciseCatalogItem>> items = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Set<String>> selectedIds = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<Integer> selectedCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> canSave = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> message = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> saveComplete = new SingleLiveEvent<>();

    private String query = "";
    private ExerciseCatalogFilters filters = ExerciseCatalogFilters.all();
    private final List<ExerciseCatalogItem> selectedItems = new ArrayList<>();
    private final String workoutId;

    @Inject
    public ExerciseLibraryViewModel(@ApplicationContext Context appContext,
                                    ExerciseCatalogRepository catalogRepository,
                                    WorkoutRepository workoutRepository,
                                    AuthRepository authRepository,
                                    SavedStateHandle savedStateHandle) {
        this.appContext = appContext;
        this.catalogRepository = catalogRepository;
        this.workoutRepository = workoutRepository;
        this.authRepository = authRepository;
        this.workoutId = safeString(savedStateHandle.get("workoutId"));
        search("");
        if (isEditMode()) {
            loadExistingSelection();
        }
    }

    public LiveData<List<ExerciseCatalogItem>> getItems() { return items; }
    public LiveData<Set<String>> getSelectedIds() { return selectedIds; }
    public LiveData<Integer> getSelectedCount() { return selectedCount; }
    public LiveData<Boolean> getIsSaving() { return isSaving; }
    public LiveData<Boolean> getCanSave() { return canSave; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<Boolean> getSaveComplete() { return saveComplete; }
    public boolean isEditMode() { return !workoutId.isEmpty(); }

    public void search(String query) {
        this.query = query != null ? query : "";
        catalogRepository.search(this.query, filters, new ExerciseCatalogRepository.CatalogCallback() {
            @Override
            public void onSuccess(List<ExerciseCatalogItem> result) {
                items.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                message.setValue(appContext.getString(R.string.exercise_library_load_error));
            }
        });
    }

    public void clearFilters() {
        filters = ExerciseCatalogFilters.all();
        search(query);
    }

    public void showPoseSupportedOnly() {
        filters = ExerciseCatalogFilters.all().setPoseSupportedOnly(true);
        search(query);
    }

    public void filterEquipment(String equipment) {
        filters = ExerciseCatalogFilters.all().setEquipment(equipment);
        search(query);
    }

    public void filterDifficulty(String difficulty) {
        filters = ExerciseCatalogFilters.all().setDifficulty(difficulty);
        search(query);
    }

    public void toggle(ExerciseCatalogItem item) {
        if (item == null || item.getId() == null) {
            return;
        }
        String id = item.getId();
        ExerciseCatalogItem existing = null;
        for (ExerciseCatalogItem selected : selectedItems) {
            if (id.equals(selected.getId())) {
                existing = selected;
                break;
            }
        }
        if (existing != null) {
            selectedItems.remove(existing);
        } else {
            selectedItems.add(item);
        }
        publishSelection();
    }

    public void saveSelectedWorkout() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            message.setValue(appContext.getString(R.string.exercise_login_required));
            return;
        }
        if (selectedItems.isEmpty()) {
            message.setValue(appContext.getString(R.string.exercise_select_required));
            return;
        }
        isSaving.setValue(true);
        canSave.setValue(false);
        if (isEditMode()) {
            updateExistingWorkout(user.getUid());
        } else {
            createCustomWorkout(user.getUid());
        }
    }

    private void loadExistingSelection() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            return;
        }
        workoutRepository.getExercises(user.getUid(), workoutId, new WorkoutRepository.ExerciseListCallback() {
            @Override
            public void onSuccess(List<Exercise> exercises) {
                selectedItems.clear();
                selectedItems.addAll(matchCatalogItems(exercises != null ? exercises : Collections.emptyList()));
                publishSelection();
            }

            @Override
            public void onError(Exception e) {
                message.postValue(appContext.getString(R.string.exercise_library_load_error));
            }
        });
    }

    private List<ExerciseCatalogItem> matchCatalogItems(List<Exercise> exercises) {
        List<ExerciseCatalogItem> catalogItems = catalogRepository.getSeedItems();
        List<ExerciseCatalogItem> matches = new ArrayList<>();
        Set<String> matchedIds = new HashSet<>();
        for (Exercise exercise : exercises) {
            ExerciseCatalogItem match = findCatalogItem(exercise, catalogItems);
            if (match != null && matchedIds.add(match.getId())) {
                matches.add(match);
            }
        }
        return matches;
    }

    private ExerciseCatalogItem findCatalogItem(Exercise exercise, List<ExerciseCatalogItem> catalogItems) {
        String catalogItemId = safeString(exercise != null ? exercise.getCatalogItemId() : null);
        if (!catalogItemId.isEmpty()) {
            for (ExerciseCatalogItem item : catalogItems) {
                if (catalogItemId.equals(item.getId())) {
                    return item;
                }
            }
        }
        String fallbackKey = fallbackExerciseKey(exercise);
        for (ExerciseCatalogItem item : catalogItems) {
            if (fallbackKey.equals(fallbackCatalogKey(item))) {
                return item;
            }
        }
        String poseTypeKey = normalizeKey(exercise != null ? exercise.getPoseTypeKey() : null);
        if (!poseTypeKey.isEmpty()) {
            for (ExerciseCatalogItem item : catalogItems) {
                if (poseTypeKey.equals(normalizeKey(item != null ? item.getPoseTypeKey() : null))) {
                    return item;
                }
            }
        }
        return null;
    }

    private void createCustomWorkout(String uid) {
        Workout workout = new Workout();
        workout.setTitle(appContext.getString(R.string.exercise_custom_title));
        workout.setSubtitle(query != null && !query.trim().isEmpty()
                ? query.trim()
                : appContext.getString(R.string.exercise_custom_goal));
        workout.setIntensity(appContext.getString(R.string.exercise_custom_intensity));
        workout.setDayType(Workout.DAY_TYPE_TRAINING);
        workout.setCustom(true);
        workout.setDayOfWeek(DateUtils.getTodayDayOfWeek());
        workout.setDurationMinutes(Math.max(15, selectedItems.size() * 8));
        workout.setCompleted(false);
        List<Exercise> exercises = new ArrayList<>();
        for (ExerciseCatalogItem selected : selectedItems) {
            exercises.add(selected.toExercise());
        }
        workout.setExercises(exercises);
        workout.setExerciseCount(exercises.size());
        workoutRepository.saveWorkout(uid, workout, new WorkoutRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                isSaving.postValue(false);
                selectedItems.clear();
                publishSelection();
                message.postValue(appContext.getString(R.string.exercise_custom_saved));
            }

            @Override
            public void onError(Exception e) {
                isSaving.postValue(false);
                canSave.postValue(!selectedItems.isEmpty());
                message.postValue(appContext.getString(R.string.exercise_custom_save_error));
            }
        });
    }

    private void updateExistingWorkout(String uid) {
        List<Exercise> exercises = new ArrayList<>();
        for (ExerciseCatalogItem selected : selectedItems) {
            exercises.add(selected.toExercise());
        }
        workoutRepository.replaceWorkoutExercises(uid, workoutId, exercises, new WorkoutRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                isSaving.postValue(false);
                canSave.postValue(!selectedItems.isEmpty());
                message.postValue(appContext.getString(R.string.exercise_custom_updated));
                saveComplete.postValue(true);
            }

            @Override
            public void onError(Exception e) {
                isSaving.postValue(false);
                canSave.postValue(!selectedItems.isEmpty());
                message.postValue(appContext.getString(R.string.exercise_custom_update_error));
            }
        });
    }

    private void publishSelection() {
        Set<String> ids = new HashSet<>();
        for (ExerciseCatalogItem item : selectedItems) {
            ids.add(item.getId());
        }
        selectedIds.setValue(ids);
        selectedCount.setValue(selectedItems.size());
        canSave.setValue(!selectedItems.isEmpty() && !Boolean.TRUE.equals(isSaving.getValue()));
    }

    private static String fallbackExerciseKey(Exercise exercise) {
        if (exercise == null) {
            return "";
        }
        return normalizeKey(exercise.getName())
                + "|"
                + normalizeKey(exercise.getPrimaryMuscle())
                + "|"
                + normalizeKey(exercise.getPoseTypeKey());
    }

    private static String fallbackCatalogKey(ExerciseCatalogItem item) {
        if (item == null) {
            return "";
        }
        return normalizeKey(item.getName())
                + "|"
                + normalizeKey(item.getPrimaryMuscle())
                + "|"
                + normalizeKey(item.getPoseTypeKey());
    }

    private static String normalizeKey(String value) {
        String decomposed = Normalizer.normalize(safeString(value), Normalizer.Form.NFD)
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("\\p{M}", "");
        return decomposed.trim()
                .toLowerCase(Locale.ROOT)
                .replace("-", " ")
                .replace("_", " ")
                .replaceAll("\\s+", " ");
    }

    private static String safeString(String value) {
        return value != null ? value : "";
    }
}
