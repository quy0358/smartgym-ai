package ntu.quy65132908.smartgym_ai.data.repository;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProgressRepositoryTest {

    @Test
    public void calculateBmiOrNull_usesWeightAndHeightCentimeters() {
        Float bmi = ProgressRepository.calculateBmiOrNull(72f, 180f);

        assertEquals(22.22f, bmi, 0.01f);
        assertEquals("Bình thường", ProgressRepository.categoryForBmi(bmi.floatValue()));
    }
}
