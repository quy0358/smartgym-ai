package ntu.quy65132908.smartgym_ai.data.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import ntu.quy65132908.smartgym_ai.data.model.Exercise;
import ntu.quy65132908.smartgym_ai.data.model.CustomWorkoutTemplate;
import ntu.quy65132908.smartgym_ai.data.model.Workout;
import ntu.quy65132908.smartgym_ai.data.model.WorkoutSession;

@Singleton
public class WorkoutRepository {
    private static final Comparator<Exercise> EXERCISE_ORDERING = (left, right) -> {
        int leftOrder = left != null ? left.getOrderIndex() : 0;
        int rightOrder = right != null ? right.getOrderIndex() : 0;
        if (leftOrder != rightOrder) {
            return Integer.compare(leftOrder, rightOrder);
        }
        return safeString(left != null ? left.getName() : null)
                .compareToIgnoreCase(safeString(right != null ? right.getName() : null));
    };

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
                .get(Source.DEFAULT)
                .addOnSuccessListener(snap -> {
                    List<Exercise> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Exercise e = doc.toObject(Exercise.class);
                        e.setId(doc.getId());
                        list.add(e);
                    }
                    Collections.sort(list, EXERCISE_ORDERING);
                    cb.onSuccess(list);
                }).addOnFailureListener(cb::onError);
    }

    public void getWorkoutSessions(String uid, WorkoutSessionListCallback cb) {
        firestore.collection("users").document(uid).collection("workoutSessions")
                .orderBy("completedAt")
                .get(Source.DEFAULT)
                .addOnSuccessListener(snap -> {
                    List<WorkoutSession> sessions = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        WorkoutSession session = doc.toObject(WorkoutSession.class);
                        session.setId(doc.getId());
                        if (session.getWorkoutId() == null || session.getWorkoutId().trim().isEmpty()) {
                            session.setWorkoutId(doc.getId());
                        }
                        sessions.add(session);
                    }
                    cb.onSuccess(sessions);
                }).addOnFailureListener(cb::onError);
    }

    public void markExerciseComplete(String uid, String wId, String eId, boolean done, SimpleCallback cb) {
        markExerciseCompleteAndSyncWorkout(uid, wId, eId, done, new CompletionCallback() {
            @Override
            public void onSuccess(boolean workoutCompleted) {
                cb.onSuccess();
            }

            @Override
            public void onError(Exception e) {
                cb.onError(e);
            }
        });
    }

    public void markExerciseCompleteAndSyncWorkout(String uid, String wId, String eId, boolean done, CompletionCallback cb) {
        markExerciseCompleteAndSyncWorkout(uid, wId, eId, done, WorkoutSession.SOURCE_MANUAL, cb);
    }

    public void markExerciseCompleteAndSyncWorkout(String uid,
                                                   String wId,
                                                   String eId,
                                                   boolean done,
                                                   String source,
                                                   CompletionCallback cb) {
        DocumentReference workoutRef = firestore.collection("users").document(uid)
                .collection("workouts").document(wId);
        workoutRef.get(Source.DEFAULT)
                .addOnSuccessListener(workoutDoc -> syncWorkoutCompletion(
                        uid,
                        workoutRef,
                        workoutDoc,
                        eId,
                        done,
                        source,
                        cb))
                .addOnFailureListener(cb::onError);
    }

    private void syncWorkoutCompletion(String uid,
                                       DocumentReference workoutRef,
                                       DocumentSnapshot workoutDoc,
                                       String exerciseId,
                                       boolean done,
                                       String source,
                                       CompletionCallback cb) {
        workoutRef.collection("exercises")
                .get()
                .addOnSuccessListener(snap -> {
                    boolean hasExercises = snap != null && !snap.isEmpty();
                    boolean allCompleted = hasExercises;
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            Boolean completed = doc.getId().equals(exerciseId)
                                    ? done
                                    : doc.getBoolean("isCompleted");
                            if (!Boolean.TRUE.equals(completed)) {
                                allCompleted = false;
                                break;
                            }
                        }
                    }
                    final boolean completedForCallback = allCompleted;
                    WriteBatch batch = firestore.batch();
                    batch.update(workoutRef.collection("exercises").document(exerciseId), "isCompleted", done);
                    batch.update(workoutRef, "isCompleted", completedForCallback);
                    DocumentReference sessionRef = firestore.collection("users").document(uid)
                            .collection("workoutSessions").document(workoutRef.getId());
                    if (completedForCallback) {
                        Workout workout = workoutDoc != null && workoutDoc.exists()
                                ? workoutDoc.toObject(Workout.class)
                                : null;
                        WorkoutSession session = WorkoutSession.fromWorkout(
                                workoutRef.getId(),
                                workout,
                                System.currentTimeMillis(),
                                source);
                        batch.set(sessionRef, session.toMap());
                    } else {
                        batch.delete(sessionRef);
                    }
                    batch.commit()
                            .addOnSuccessListener(update -> cb.onSuccess(completedForCallback))
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
            for (int exerciseIndex = 0; exerciseIndex < exercises.size(); exerciseIndex++) {
                Exercise exercise = exercises.get(exerciseIndex);
                if (exercise == null) {
                    continue;
                }
                DocumentReference exerciseRef = workoutRef.collection("exercises").document();
                exercise.setId(exerciseRef.getId());
                exercise.setOrderIndex(exerciseIndex);
                batch.set(exerciseRef, exercise);
            }
        }
        batch.commit()
                .addOnSuccessListener(r -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void replaceWorkoutExercises(String uid, String workoutId, List<Exercise> selectedExercises, SimpleCallback cb) {
        if (safeString(uid).isEmpty() || safeString(workoutId).isEmpty()) {
            cb.onError(new IllegalArgumentException("Workout id is required"));
            return;
        }
        if (selectedExercises == null || selectedExercises.isEmpty()) {
            cb.onError(new IllegalArgumentException("Exercise list is empty"));
            return;
        }

        DocumentReference workoutRef = firestore.collection("users").document(uid)
                .collection("workouts").document(workoutId);
        workoutRef.get(Source.DEFAULT)
                .addOnSuccessListener(workoutDoc -> workoutRef.collection("exercises")
                        .get(Source.DEFAULT)
                        .addOnSuccessListener(existing -> replaceWorkoutExercises(
                                uid,
                                workoutRef,
                                workoutDoc,
                                existing,
                                selectedExercises,
                                cb))
                        .addOnFailureListener(cb::onError))
                .addOnFailureListener(cb::onError);
    }

    private void replaceWorkoutExercises(String uid,
                                         DocumentReference workoutRef,
                                         DocumentSnapshot workoutDoc,
                                         QuerySnapshot existingExercises,
                                         List<Exercise> selectedExercises,
                                         SimpleCallback cb) {
        Map<String, Boolean> completedByCatalogId = new HashMap<>();
        Map<String, Boolean> completedByFallbackKey = new HashMap<>();
        if (existingExercises != null) {
            for (QueryDocumentSnapshot doc : existingExercises) {
                Exercise oldExercise = doc.toObject(Exercise.class);
                String catalogItemId = safeString(oldExercise.getCatalogItemId());
                if (!catalogItemId.isEmpty()) {
                    completedByCatalogId.put(catalogItemId, oldExercise.isCompleted());
                }
                completedByFallbackKey.put(fallbackExerciseKey(oldExercise), oldExercise.isCompleted());
            }
        }

        List<Exercise> preparedExercises = new ArrayList<>();
        boolean allCompleted = true;
        for (int index = 0; index < selectedExercises.size(); index++) {
            Exercise prepared = copyExerciseForReplace(selectedExercises.get(index));
            prepared.setOrderIndex(index);
            prepared.setCompleted(completionFor(prepared, completedByCatalogId, completedByFallbackKey));
            if (!prepared.isCompleted()) {
                allCompleted = false;
            }
            preparedExercises.add(prepared);
        }
        if (preparedExercises.isEmpty()) {
            allCompleted = false;
        }

        WriteBatch batch = firestore.batch();
        if (existingExercises != null) {
            for (QueryDocumentSnapshot doc : existingExercises) {
                batch.delete(doc.getReference());
            }
        }
        for (Exercise exercise : preparedExercises) {
            DocumentReference exerciseRef = workoutRef.collection("exercises").document();
            exercise.setId(exerciseRef.getId());
            batch.set(exerciseRef, exercise);
        }

        int durationMinutes = Math.max(15, preparedExercises.size() * 8);
        batch.update(
                workoutRef,
                "exerciseCount", preparedExercises.size(),
                "durationMinutes", durationMinutes,
                "isCompleted", allCompleted);
        DocumentReference sessionRef = firestore.collection("users").document(uid)
                .collection("workoutSessions").document(workoutRef.getId());
        if (allCompleted) {
            Workout workout = workoutDoc != null && workoutDoc.exists()
                    ? workoutDoc.toObject(Workout.class)
                    : null;
            if (workout != null) {
                workout.setId(workoutRef.getId());
                workout.setExercises(preparedExercises);
                workout.setExerciseCount(preparedExercises.size());
                workout.setDurationMinutes(durationMinutes);
                workout.setCompleted(true);
            }
            WorkoutSession session = WorkoutSession.fromWorkout(
                    workoutRef.getId(),
                    workout,
                    System.currentTimeMillis(),
                    WorkoutSession.SOURCE_MANUAL);
            batch.set(sessionRef, session.toMap());
        } else {
            batch.delete(sessionRef);
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

            for (int exerciseIndex = 0; exerciseIndex < exercises.size(); exerciseIndex++) {
                Exercise exercise = exercises.get(exerciseIndex);
                if (exercise == null) {
                    continue;
                }
                DocumentReference exerciseRef = workoutRef.collection("exercises").document();
                exercise.setId(exerciseRef.getId());
                exercise.setOrderIndex(exerciseIndex);
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

    private Exercise copyExerciseForReplace(Exercise exercise) {
        if (exercise == null) {
            return new Exercise();
        }
        Exercise copy = new Exercise(
                null,
                exercise.getName(),
                exercise.getSets(),
                exercise.getReps(),
                exercise.getWeight(),
                false);
        copy.setNotes(exercise.getNotes());
        copy.setPoseTypeKey(exercise.getPoseTypeKey());
        copy.setPrimaryMuscle(exercise.getPrimaryMuscle());
        copy.setCatalogItemId(exercise.getCatalogItemId());
        copy.setDurationSeconds(exercise.getDurationSeconds());
        return copy;
    }

    private boolean completionFor(Exercise exercise,
                                  Map<String, Boolean> completedByCatalogId,
                                  Map<String, Boolean> completedByFallbackKey) {
        String catalogItemId = safeString(exercise != null ? exercise.getCatalogItemId() : null);
        if (!catalogItemId.isEmpty() && completedByCatalogId.containsKey(catalogItemId)) {
            return Boolean.TRUE.equals(completedByCatalogId.get(catalogItemId));
        }
        return Boolean.TRUE.equals(completedByFallbackKey.get(fallbackExerciseKey(exercise)));
    }

    private String fallbackExerciseKey(Exercise exercise) {
        if (exercise == null) {
            return "";
        }
        return normalizeKey(exercise.getName())
                + "|"
                + normalizeKey(exercise.getPrimaryMuscle())
                + "|"
                + normalizeKey(exercise.getPoseTypeKey());
    }

    public interface WorkoutListCallback {
        void onSuccess(List<Workout> w);
        void onError(Exception e);
    }

    public interface ExerciseListCallback {
        void onSuccess(List<Exercise> e);
        void onError(Exception e);
    }

    public interface WorkoutSessionListCallback {
        void onSuccess(List<WorkoutSession> sessions);
        void onError(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface CompletionCallback extends SimpleCallback {
        void onSuccess(boolean workoutCompleted);

        @Override
        default void onSuccess() {
            onSuccess(false);
        }

        void onError(Exception e);
    }

    private static String safeString(String value) {
        return value != null ? value : "";
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
}
