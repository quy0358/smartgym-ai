package ntu.quy65132908.smartgym_ai.ui.workout;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.model.Exercise;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;

@HiltViewModel
public class WorkoutViewModel extends ViewModel {
    private final WorkoutRepository workoutRepo;
    private final AuthRepository authRepo;
    private final MutableLiveData<List<Exercise>> exercises = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LiveData<List<Exercise>> getExercises() { return exercises; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    @Inject
    public WorkoutViewModel(WorkoutRepository workoutRepo, AuthRepository authRepo) {
        this.workoutRepo = workoutRepo;
        this.authRepo = authRepo;
    }

    public void loadExercises(String workoutId) {
        FirebaseUser u = authRepo.getCurrentUser();
        if (u == null) return;
        isLoading.setValue(true);
        workoutRepo.getExercises(u.getUid(), workoutId, new WorkoutRepository.ExerciseListCallback() {
            @Override
            public void onSuccess(List<Exercise> list) { isLoading.postValue(false); exercises.postValue(list); }
            @Override
            public void onError(Exception e) { isLoading.postValue(false); }
        });
    }
}
