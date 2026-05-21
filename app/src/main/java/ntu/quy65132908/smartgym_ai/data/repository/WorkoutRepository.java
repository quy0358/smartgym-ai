package ntu.quy65132908.smartgym_ai.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import ntu.quy65132908.smartgym_ai.data.model.Exercise;
import ntu.quy65132908.smartgym_ai.data.model.Workout;

@Singleton
public class WorkoutRepository {
    private final FirebaseFirestore firestore;

    @Inject
    public WorkoutRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public void getWeeklyPlan(String uid, WorkoutListCallback cb) {
        firestore.collection("users").document(uid).collection("workouts")
                .orderBy("dayOfWeek").get()
                .addOnSuccessListener(snap -> {
                    List<Workout> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Workout w = doc.toObject(Workout.class);
                        w.setId(doc.getId());
                        list.add(w);
                    }
                    cb.onSuccess(list);
                }).addOnFailureListener(cb::onError);
    }

    public void getExercises(String uid, String workoutId, ExerciseListCallback cb) {
        firestore.collection("users").document(uid).collection("workouts").document(workoutId)
                .collection("exercises").get()
                .addOnSuccessListener(snap -> {
                    List<Exercise> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Exercise e = doc.toObject(Exercise.class);
                        e.setId(doc.getId());
                        list.add(e);
                    }
                    cb.onSuccess(list);
                }).addOnFailureListener(cb::onError);
    }

    public void markExerciseComplete(String uid, String wId, String eId, boolean done, SimpleCallback cb) {
        firestore.collection("users").document(uid).collection("workouts").document(wId)
                .collection("exercises").document(eId).update("isCompleted", done)
                .addOnSuccessListener(v -> cb.onSuccess()).addOnFailureListener(cb::onError);
    }

    public void saveWorkout(String uid, Workout w, SimpleCallback cb) {
        firestore.collection("users").document(uid).collection("workouts").add(w)
                .addOnSuccessListener(r -> cb.onSuccess()).addOnFailureListener(cb::onError);
    }

    public interface WorkoutListCallback {
        void onSuccess(List<Workout> w);
        void onError(Exception e);
    }

    public interface ExerciseListCallback {
        void onSuccess(List<Exercise> e);
        void onError(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }
}
