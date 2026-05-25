package ntu.quy65132908.smartgym_ai.di;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ChallengeRepository;
import ntu.quy65132908.smartgym_ai.data.repository.CommunityRepository;
import ntu.quy65132908.smartgym_ai.data.repository.DeepSeekRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ExerciseCatalogRepository;
import ntu.quy65132908.smartgym_ai.data.repository.InjuryProfileRepository;
import ntu.quy65132908.smartgym_ai.data.repository.NutritionRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ProgressRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ReminderRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;
import ntu.quy65132908.smartgym_ai.util.DeepSeekKeyProvider;
import ntu.quy65132908.smartgym_ai.util.ReminderScheduler;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public FirebaseAuth provideFirebaseAuth() {
        return FirebaseAuth.getInstance();
    }

    @Provides
    @Singleton
    public FirebaseFirestore provideFirestore() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        firestore.setFirestoreSettings(settings);
        return firestore;
    }

    @Provides
    @Singleton
    public AuthRepository provideAuthRepository(FirebaseAuth auth, FirebaseFirestore firestore) {
        return new AuthRepository(auth, firestore);
    }

    @Provides
    @Singleton
    public UserRepository provideUserRepository(FirebaseFirestore firestore) {
        return new UserRepository(firestore);
    }

    @Provides
    @Singleton
    public DeepSeekKeyProvider provideDeepSeekKeyProvider() {
        return new DeepSeekKeyProvider();
    }

    @Provides
    @Singleton
    public WorkoutRepository provideWorkoutRepository(FirebaseFirestore firestore) {
        return new WorkoutRepository(firestore);
    }

    @Provides
    @Singleton
    public ExerciseCatalogRepository provideExerciseCatalogRepository() {
        return new ExerciseCatalogRepository();
    }

    @Provides
    @Singleton
    public NutritionRepository provideNutritionRepository(FirebaseFirestore firestore) {
        return new NutritionRepository(firestore);
    }

    @Provides
    @Singleton
    public ReminderRepository provideReminderRepository(FirebaseFirestore firestore) {
        return new ReminderRepository(firestore);
    }

    @Provides
    @Singleton
    public ChallengeRepository provideChallengeRepository(FirebaseFirestore firestore) {
        return new ChallengeRepository(firestore);
    }

    @Provides
    @Singleton
    public InjuryProfileRepository provideInjuryProfileRepository(FirebaseFirestore firestore) {
        return new InjuryProfileRepository(firestore);
    }

    @Provides
    @Singleton
    public ReminderScheduler provideReminderScheduler() {
        return new ReminderScheduler();
    }

    @Provides
    @Singleton
    public ProgressRepository provideProgressRepository(FirebaseFirestore firestore) {
        return new ProgressRepository(firestore);
    }

    @Provides
    @Singleton
    public CommunityRepository provideCommunityRepository(FirebaseFirestore firestore) {
        return new CommunityRepository(firestore);
    }

    @Provides
    @Singleton
    public DeepSeekRepository provideDeepSeekRepository(DeepSeekKeyProvider keyProvider) {
        return new DeepSeekRepository(keyProvider);
    }
}
