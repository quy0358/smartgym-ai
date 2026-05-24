package ntu.quy65132908.smartgym_ai.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

import ntu.quy65132908.smartgym_ai.data.model.Exercise;
import ntu.quy65132908.smartgym_ai.data.model.ExerciseCatalogFilters;
import ntu.quy65132908.smartgym_ai.data.model.ExerciseCatalogItem;

public class ExerciseCatalogRepositoryTest {

    @Test
    public void search_withQuery_returnsMatchingVietnameseExercises() {
        ExerciseCatalogRepository repository = new ExerciseCatalogRepository();

        List<ExerciseCatalogItem> results = repository.searchSync(
                "squat",
                ExerciseCatalogFilters.all()
        );

        assertFalse(results.isEmpty());
        assertEquals("squat", results.get(0).getId());
        assertEquals("Squat", results.get(0).getName());
        assertTrue(results.get(0).getPrimaryMuscle().contains("Chân"));
        assertTrue(results.get(0).getDefaultSets() > 0);
        assertTrue(results.get(0).getDefaultReps() > 0);
    }

    @Test
    public void search_withFilters_limitsByMuscleEquipmentDifficultyAndPoseSupport() {
        ExerciseCatalogRepository repository = new ExerciseCatalogRepository();
        ExerciseCatalogFilters filters = new ExerciseCatalogFilters()
                .setPrimaryMuscle("Ngực")
                .setEquipment("Không dụng cụ")
                .setDifficulty("Cơ bản")
                .setPoseSupportedOnly(true);

        List<ExerciseCatalogItem> results = repository.searchSync("", filters);

        assertFalse(results.isEmpty());
        for (ExerciseCatalogItem item : results) {
            assertEquals("Ngực", item.getPrimaryMuscle());
            assertEquals("Không dụng cụ", item.getEquipment());
            assertEquals("Cơ bản", item.getDifficulty());
            assertTrue(item.isPoseSupported());
        }
    }

    @Test
    public void catalogItemToExercise_preservesPoseTypeKey() {
        ExerciseCatalogItem item = new ExerciseCatalogItem(
                "plank",
                "Plank",
                "Core",
                "Khong dung cu",
                "Co ban",
                3,
                30,
                45,
                "plank",
                "Giu than nguoi thang");

        Exercise exercise = item.toExercise();

        assertEquals("plank", exercise.getPoseTypeKey());
    }
}
