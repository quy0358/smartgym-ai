package ntu.quy65132908.smartgym_ai.ui.community;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.model.Post;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.CommunityRepository;
import ntu.quy65132908.smartgym_ai.util.InputValidator;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

@HiltViewModel
public class CommunityViewModel extends ViewModel {
    private final CommunityRepository communityRepo;
    private final AuthRepository authRepo;
    private final MutableLiveData<List<Post>> posts = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> error = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> postCreated = new SingleLiveEvent<>();

    public LiveData<List<Post>> getPosts() { return posts; }
    public LiveData<Boolean> getIsRefreshing() { return isRefreshing; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getPostCreated() { return postCreated; }

    @Inject
    public CommunityViewModel(CommunityRepository communityRepo, AuthRepository authRepo) {
        this.communityRepo = communityRepo;
        this.authRepo = authRepo;
        startListening();
    }

    private void startListening() {
        isRefreshing.setValue(true);
        communityRepo.listenToPosts(new CommunityRepository.PostsCallback() {
            @Override
            public void onSuccess(List<Post> list) { isRefreshing.postValue(false); posts.postValue(list); }
            @Override
            public void onError(Exception e) { isRefreshing.postValue(false); error.postValue(e.getMessage()); }
        });
    }

    /**
     * H2: Real refresh — restart listener to force re-query.
     */
    public void refresh() {
        communityRepo.removeListener();
        startListening();
    }

    public void createPost(String content) {
        FirebaseUser u = authRepo.getCurrentUser();
        if (u == null) return;
        String sanitized = InputValidator.sanitizeContent(content);
        if (sanitized.isEmpty()) { error.setValue("Nội dung trống"); return; }
        String name = u.getDisplayName() != null ? u.getDisplayName() : "Người dùng";
        communityRepo.createPost(u.getUid(), name, sanitized, new CommunityRepository.SimpleCallback() {
            @Override
            public void onSuccess() { postCreated.postValue(true); }
            @Override
            public void onError(Exception e) { error.postValue(e.getMessage()); }
        });
    }

    public void toggleLike(String postId, boolean isLiked) {
        FirebaseUser u = authRepo.getCurrentUser();
        if (u == null) return;
        communityRepo.toggleLike(postId, u.getUid(), isLiked, new CommunityRepository.SimpleCallback() {
            @Override
            public void onSuccess() {}
            @Override
            public void onError(Exception e) { error.postValue(e.getMessage()); }
        });
    }

    @Override
    protected void onCleared() { super.onCleared(); communityRepo.removeListener(); }
}
