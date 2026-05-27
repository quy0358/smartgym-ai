package ntu.quy65132908.smartgym_ai.ui.workout;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.navigation.NavDirections;

import java.util.Objects;

import ntu.quy65132908.smartgym_ai.R;

public final class WorkoutListFragmentDirections {

    private WorkoutListFragmentDirections() {
    }

    @NonNull
    public static ActionWorkoutToWorkoutDetail actionWorkoutToWorkoutDetail(
            @NonNull String workoutId,
            @NonNull String workoutTitle,
            int workoutDuration) {
        return new ActionWorkoutToWorkoutDetail(workoutId, workoutTitle, workoutDuration, "TRAINING", false);
    }

    @NonNull
    public static ActionWorkoutToWorkoutDetail actionWorkoutToWorkoutDetail(
            @NonNull String workoutId,
            @NonNull String workoutTitle,
            int workoutDuration,
            @NonNull String dayType) {
        return new ActionWorkoutToWorkoutDetail(workoutId, workoutTitle, workoutDuration, dayType, false);
    }

    @NonNull
    public static ActionWorkoutToWorkoutDetail actionWorkoutToWorkoutDetail(
            @NonNull String workoutId,
            @NonNull String workoutTitle,
            int workoutDuration,
            @NonNull String dayType,
            boolean isCustom) {
        return new ActionWorkoutToWorkoutDetail(workoutId, workoutTitle, workoutDuration, dayType, isCustom);
    }

    public static final class ActionWorkoutToWorkoutDetail implements NavDirections {
        private final String workoutId;
        private final String workoutTitle;
        private final int workoutDuration;
        private final String dayType;
        private final boolean isCustom;

        private ActionWorkoutToWorkoutDetail(
                @NonNull String workoutId,
                @NonNull String workoutTitle,
                int workoutDuration,
                @NonNull String dayType,
                boolean isCustom) {
            this.workoutId = workoutId;
            this.workoutTitle = workoutTitle;
            this.workoutDuration = workoutDuration;
            this.dayType = dayType;
            this.isCustom = isCustom;
        }

        @Override
        public int getActionId() {
            return R.id.action_workout_to_workout_detail;
        }

        @NonNull
        @Override
        public Bundle getArguments() {
            Bundle bundle = new Bundle();
            bundle.putString("workoutId", workoutId);
            bundle.putString("workoutTitle", workoutTitle);
            bundle.putInt("workoutDuration", workoutDuration);
            bundle.putString("dayType", dayType);
            bundle.putBoolean("isCustom", isCustom);
            return bundle;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ActionWorkoutToWorkoutDetail)) return false;
            ActionWorkoutToWorkoutDetail other = (ActionWorkoutToWorkoutDetail) obj;
            return workoutDuration == other.workoutDuration
                    && isCustom == other.isCustom
                    && Objects.equals(workoutId, other.workoutId)
                    && Objects.equals(workoutTitle, other.workoutTitle)
                    && Objects.equals(dayType, other.dayType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(workoutId, workoutTitle, workoutDuration, dayType, isCustom);
        }

        @NonNull
        @Override
        public String toString() {
            return "ActionWorkoutToWorkoutDetail{"
                    + "workoutId='" + workoutId + '\''
                    + ", workoutTitle='" + workoutTitle + '\''
                    + ", workoutDuration=" + workoutDuration
                    + ", dayType='" + dayType + '\''
                    + ", isCustom=" + isCustom
                    + '}';
        }
    }
}
