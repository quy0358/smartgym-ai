package ntu.quy65132908.smartgym_ai.data.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import ntu.quy65132908.smartgym_ai.data.model.Exercise;
import ntu.quy65132908.smartgym_ai.data.model.CustomWorkoutTemplate;
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
                .collection("exercises")
                .orderBy("name")
                .get(Source.DEFAULT)
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
        markExerciseCompleteAndSyncWorkout(uid, wId, eId, done, cb);
    }

    public void markExerciseCompleteAndSyncWorkout(String uid, String wId, String eId, boolean done, SimpleCallback cb) {
        firestore.collection("users").document(uid).collection("workouts").document(wId)
                .collection("exercises").document(eId).update("isCompleted", done)
                .addOnSuccessListener(v -> syncWorkoutCompletion(uid, wId, cb))
                .addOnFailureListener(cb::onError);
    }

    private void syncWorkoutCompletion(String uid, String workoutId, SimpleCallback cb) {
        firestore.collection("users").document(uid).collection("workouts").document(workoutId)
                .collection("exercises")
                .get()
                .addOnSuccessListener(snap -> {
                    boolean hasExercises = snap != null && !snap.isEmpty();
                    boolean allCompleted = hasExercises;
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            Boolean completed = doc.getBoolean("isCompleted");
                            if (!Boolean.TRUE.equals(completed)) {
                                allCompleted = false;
                                break;
                            }
                        }
                    }
                    firestore.collection("users").document(uid).collection("workouts").document(workoutId)
                            .update("isCompleted", allCompleted)
                            .addOnSuccessListener(update -> cb.onSuccess())
                            .addOnFailureListener(cb::onError);
                })
                .addOnFailureListener(cb::onError);
    }

    public void saveWorkout(String uid, Workout w, SimpleCallback cb) {
        if (w == null) {
            cb.onError(new IllegalArgumentException("Workout is empty"));
            return;
        }
        CollectionReference workoutsRef = firestore.collection("users").document(uid).collection("workouts");
        DocumentReference workoutRef = workoutsRef.document();
        w.setId(workoutRef.getId());
        prepareWorkoutForSave(w);

        WriteBatch batch = firestore.batch();
        batch.set(workoutRef, w);
        if (w.isRestDay()) {
            batch.commit()
                    .addOnSuccessListener(r -> cb.onSuccess())
                    .addOnFailureListener(cb::onError);
            return;
        }
        List<Exercise> exercises = w.getExercises();
        if (exercises != null) {
            for (Exercise exercise : exercises) {
                DocumentReference exerciseRef = workoutRef.collection("exercises").document();
                exercise.setId(exerciseRef.getId());
                batch.set(exerciseRef, exercise);
            }
        }
        batch.commit()
                .addOnSuccessListener(r -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void saveWeeklyPlan(String uid, List<Workout> workouts, SimpleCallback cb) {
        if (workouts == null || workouts.isEmpty()) {
            cb.onError(new IllegalArgumentException("Weekly plan is empty"));
            return;
        }

        CollectionReference workoutsRef = firestore.collection("users").document(uid).collection("workouts");
        workoutsRef.get()
                .addOnSuccessListener(existing -> replaceWeeklyPlan(workoutsRef, existing, workouts, cb))
                .addOnFailureListener(cb::onError);
    }

    public void saveCustomWorkoutTemplate(String uid, CustomWorkoutTemplate template, SimpleCallback cb) {
        if (template == null || template.getExercises() == null || template.getExercises().isEmpty()) {
            cb.onError(new IllegalArgumentException("Custom workout template is empty"));
            return;
        }
        template.setCreatedAt(System.currentTimeMillis());
        firestore.collection("users").document(uid)
                .collection("customWorkouts")
                .add(template)
                .addOnSuccessListener(ref -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    private void replaceWeeklyPlan(CollectionReference workoutsRef,
                                   QuerySnapshot existingWorkouts,
                                   List<Workout> workouts,
                                   SimpleCallback cb) {
        WriteBatch batch = firestore.batch();
        if (existingWorkouts != null) {
            for (QueryDocumentSnapshot doc : existingWorkouts) {
                batch.delete(doc.getReference());
            }
        }

        for (Workout workout : workouts) {
            DocumentReference workoutRef = workoutsRef.document();
            workout.setId(workoutRef.getId());
            prepareWorkoutForSave(workout);
            batch.set(workoutRef, workout);
            if (workout.isRestDay()) {
                continue;
            }

            List<Exercise> exercises = workout.getExercises();
            if (exercises == null) {
                continue;
            }

            for (Exercise exercise : exercises) {
                DocumentReference exerciseRef = workoutRef.collection("exercises").document();
                exercise.setId(exerciseRef.getId());
                batch.set(exerciseRef, exercise);
            }
        }

        batch.commit()
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    private void prepareWorkoutForSave(Workout workout) {
        workout.setDayType(workout.getDayType());
        if (workout.isRestDay()) {
            workout.setDurationMinutes(0);
            workout.setExerciseCount(0);
            return;
        }
        if (workout.getExercises() != null) {
            workout.setExerciseCount(workout.getExercises().size());
        } else {
            workout.setExerciseCount(0);
        }
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
