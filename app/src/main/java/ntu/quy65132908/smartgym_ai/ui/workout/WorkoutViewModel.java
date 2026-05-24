package ntu.quy65132908.smartgym_ai.ui.workout;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.model.Exercise;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ChallengeRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

@HiltViewModel
public class WorkoutViewModel extends ViewModel {
    private final WorkoutRepository workoutRepo;
    private final ChallengeRepository challengeRepository;
    private final AuthRepository authRepo;

    private final MutableLiveData<WorkoutDetailUiState> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<String> snackbarMessage = new SingleLiveEvent<>();

    private final String workoutId;
    private final String subtitle;
    private final String dayType;
    private List<Exercise> currentExercises = new ArrayList<>();

    public LiveData<WorkoutDetailUiState> getUiState() { return uiState; }
    public LiveData<String> getSnackbarMessage() { return snackbarMessage; }

    @Inject
    public WorkoutViewModel(WorkoutRepository workoutRepo,
                            ChallengeRepository challengeRepository,
                            AuthRepository authRepo,
                            SavedStateHandle savedState) {
        this.workoutRepo = workoutRepo;
        this.challengeRepository = challengeRepository;
        this.authRepo = authRepo;

        this.workoutId = savedState.get("workoutId");
        String title = savedState.get("workoutTitle");
        Integer duration = savedState.get("workoutDuration");
        String rawDayType = savedState.get("dayType");
        this.dayType = Workout.normalizeDayType(rawDayType);
        this.subtitle = (title != null ? title : "") + " • " +
                (duration != null ? duration : 0) + " phút";

        if (Workout.DAY_TYPE_REST.equals(dayType)) {
            uiState.setValue(WorkoutDetailUiState.rest(subtitle));
        } else {
            loadExercises();
        }
    }

    public void loadExercises() {
        FirebaseUser u = authRepo.getCurrentUser();
        if (u == null || workoutId == null || workoutId.isEmpty()) return;

        uiState.setValue(WorkoutDetailUiState.loading(subtitle, dayType));

        workoutRepo.getExercises(u.getUid(), workoutId, new WorkoutRepository.ExerciseListCallback() {
            @Override
            public void onSuccess(List<Exercise> list) {
                currentExercises = copyExercises(list != null ? list : Collections.emptyList());
                uiState.postValue(WorkoutDetailUiState.success(currentExercises, subtitle, dayType));
            }

            @Override
            public void onError(Exception e) {
                uiState.postValue(WorkoutDetailUiState.error(
                        "Không thể tải bài tập.",
                        subtitle,
                        dayType));
            }
        });
    }

    public void toggleExercise(String exerciseId, boolean done) {
        FirebaseUser u = authRepo.getCurrentUser();
        if (u == null || workoutId == null || workoutId.isEmpty()
                || exerciseId == null || exerciseId.isEmpty()) {
            return;
        }

        boolean found = false;
        for (Exercise ex : currentExercises) {
            if (Objects.equals(ex.getId(), exerciseId)) {
                ex.setCompleted(done);
                found = true;
                break;
            }
        }
        if (!found) return;

        uiState.setValue(WorkoutDetailUiState.success(new ArrayList<>(currentExercises), subtitle, dayType));

        workoutRepo.markExerciseCompleteAndSyncWorkout(u.getUid(), workoutId, exerciseId, done,
                new WorkoutRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        // Optimistic state is already reflected in the UI.
                        recordChallengeProgressIfWorkoutComplete(u.getUid());
                    }

                    @Override
                    public void onError(Exception e) {
                        for (Exercise ex : currentExercises) {
                            if (Objects.equals(ex.getId(), exerciseId)) {
                                ex.setCompleted(!done);
                                break;
                            }
                        }
                        uiState.postValue(WorkoutDetailUiState.success(
                                new ArrayList<>(currentExercises),
                                subtitle,
                                dayType));
                        snackbarMessage.postValue("Không thể cập nhật. Thử lại.");
                    }
                });
    }

    public void retry() {
        loadExercises();
    }

    private List<Exercise> copyExercises(List<Exercise> exercises) {
        List<Exercise> copies = new ArrayList<>();
        for (Exercise exercise : exercises) {
            if (exercise == null) {
                continue;
            }
            Exercise copy = new Exercise(
                    exercise.getId(),
                    exercise.getName(),
                    exercise.getSets(),
                    exercise.getReps(),
                    exercise.getWeight(),
                    exercise.isCompleted());
            copy.setNotes(exercise.getNotes());
            copy.setPoseTypeKey(exercise.getPoseTypeKey());
            copies.add(copy);
        }
        return copies;
    }

    private void recordChallengeProgressIfWorkoutComplete(String uid) {
        if (!isWorkoutComplete()) {
            return;
        }
        challengeRepository.recordWorkoutCompletion(uid, new ChallengeRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                // Challenge progress is secondary to the workout toggle; no extra UI update is required.
            }

            @Override
            public void onError(Exception e) {
                // Do not roll back a completed workout if challenge progress could not be updated.
            }
        });
    }

    private boolean isWorkoutComplete() {
        if (currentExercises == null || currentExercises.isEmpty()) {
            return false;
        }
        for (Exercise exercise : currentExercises) {
            if (exercise == null || !exercise.isCompleted()) {
                return false;
            }
        }
        return true;
    }
}
