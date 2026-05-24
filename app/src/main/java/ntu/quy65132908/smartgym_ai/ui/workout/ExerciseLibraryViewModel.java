package ntu.quy65132908.smartgym_ai.ui.workout;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    private String query = "";
    private ExerciseCatalogFilters filters = ExerciseCatalogFilters.all();
    private final List<ExerciseCatalogItem> selectedItems = new ArrayList<>();

    @Inject
    public ExerciseLibraryViewModel(@ApplicationContext Context appContext,
                                    ExerciseCatalogRepository catalogRepository,
                                    WorkoutRepository workoutRepository,
                                    AuthRepository authRepository) {
        this.appContext = appContext;
        this.catalogRepository = catalogRepository;
        this.workoutRepository = workoutRepository;
        this.authRepository = authRepository;
        search("");
    }

    public LiveData<List<ExerciseCatalogItem>> getItems() { return items; }
    public LiveData<Set<String>> getSelectedIds() { return selectedIds; }
    public LiveData<Integer> getSelectedCount() { return selectedCount; }
    public LiveData<Boolean> getIsSaving() { return isSaving; }
    public LiveData<Boolean> getCanSave() { return canSave; }
    public LiveData<String> getMessage() { return message; }

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
        Workout workout = new Workout();
        workout.setTitle(appContext.getString(R.string.exercise_custom_title));
        workout.setSubtitle(query != null && !query.trim().isEmpty()
                ? query.trim()
                : appContext.getString(R.string.exercise_custom_goal));
        workout.setIntensity(appContext.getString(R.string.exercise_custom_intensity));
        workout.setDayType(Workout.DAY_TYPE_TRAINING);
        workout.setDayOfWeek(DateUtils.getTodayDayOfWeek());
        workout.setDurationMinutes(Math.max(15, selectedItems.size() * 8));
        workout.setCompleted(false);
        List<Exercise> exercises = new ArrayList<>();
        for (ExerciseCatalogItem selected : selectedItems) {
            exercises.add(selected.toExercise());
        }
        workout.setExercises(exercises);
        workout.setExerciseCount(exercises.size());
        workoutRepository.saveWorkout(user.getUid(), workout, new WorkoutRepository.SimpleCallback() {
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

    private void publishSelection() {
        Set<String> ids = new HashSet<>();
        for (ExerciseCatalogItem item : selectedItems) {
            ids.add(item.getId());
        }
        selectedIds.setValue(ids);
        selectedCount.setValue(selectedItems.size());
        canSave.setValue(!selectedItems.isEmpty() && !Boolean.TRUE.equals(isSaving.getValue()));
    }
}
