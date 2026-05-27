package ntu.quy65132908.smartgym_ai;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FirestoreRulesTest {

    @Test
    public void validUserAllowsProfileAndProgressSyncedFields() throws IOException {
        String rules = readRules();

        assertTrue(rules.contains("'targetWeight'"));
        assertTrue(rules.contains("'gender'"));
        assertTrue(rules.contains("'birthDate'"));
        assertTrue(rules.contains("'fitnessLevel'"));
        assertTrue(rules.contains("'onboardingCompleted'"));
        assertTrue(rules.contains("'onboardingCompletedAt'"));
        assertTrue(rules.contains("request.resource.data.weight is number"));
        assertTrue(rules.contains("request.resource.data.bmi is number"));
        assertTrue(rules.contains("request.resource.data.bmiCategory is string"));
    }

    @Test
    public void workoutRulesAllowFieldsEmittedByModels() throws IOException {
        String rules = readRules();

        assertTrue(rules.contains("'isCustom'"));
        assertTrue(rules.contains("request.resource.data.isCustom is bool"));
        assertTrue(rules.contains("'catalogItemId'"));
        assertTrue(rules.contains("request.resource.data.catalogItemId is string"));
    }

    @Test
    public void communityLikeRulesKeepLikedByDocumentBounded() throws IOException {
        String rules = readRules();

        assertTrue(rules.contains("resource.data.likedBy.size() < 10000"));
        assertTrue(rules.contains("request.auth.uid in resource.data.likedBy"));
    }

    private String readRules() throws IOException {
        return new String(Files.readAllBytes(Paths.get("../firestore.rules")), StandardCharsets.UTF_8);
    }
}
