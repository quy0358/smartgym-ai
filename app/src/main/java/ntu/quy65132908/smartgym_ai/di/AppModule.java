package ntu.quy65132908.smartgym_ai.di;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.CommunityRepository;
import ntu.quy65132908.smartgym_ai.data.repository.GeminiRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ProgressRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.data.repository.WorkoutRepository;
import ntu.quy65132908.smartgym_ai.util.GeminiKeyProvider;

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
        return FirebaseFirestore.getInstance();
    }

    @Provides
    @Singleton
    public FirebaseStorage provideStorage() {
        return FirebaseStorage.getInstance();
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
    public GeminiKeyProvider provideGeminiKeyProvider() {
        return new GeminiKeyProvider();
    }

    @Provides
    @Singleton
    public WorkoutRepository provideWorkoutRepository(FirebaseFirestore firestore) {
        return new WorkoutRepository(firestore);
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
    public GeminiRepository provideGeminiRepository(GeminiKeyProvider keyProvider) {
        return new GeminiRepository(keyProvider);
    }
}
