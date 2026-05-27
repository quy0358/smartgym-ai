package ntu.quy65132908.smartgym_ai.ui.community;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.model.Post;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.CommunityRepository;
import ntu.quy65132908.smartgym_ai.util.InputValidator;
import ntu.quy65132908.smartgym_ai.util.SingleLiveEvent;

@HiltViewModel
public class CommunityViewModel extends ViewModel {
    private static final String MESSAGE_LOGIN_TO_POST = "Bạn cần đăng nhập để đăng bài.";
    private static final String MESSAGE_LOGIN_TO_LIKE = "Bạn cần đăng nhập để thích bài viết.";
    private static final String MESSAGE_EMPTY_CONTENT = "Nội dung trống";
    private static final String MESSAGE_DEFAULT_USER = "Người dùng";
    private static final String MESSAGE_GENERIC_ERROR = "Đã xảy ra lỗi";

    private static final String MESSAGE_LOGIN_TO_MANAGE = "Bạn cần đăng nhập để quản lý bài viết.";
    private static final String MESSAGE_OWN_POST_ONLY = "Bạn chỉ có thể chỉnh sửa hoặc xóa bài viết của mình.";
    private static final String MESSAGE_POST_UPDATED = "Đã cập nhật bài viết.";
    private static final String MESSAGE_POST_DELETED = "Đã xóa bài viết.";

    private final CommunityRepository communityRepo;
    private final AuthRepository authRepo;
    private final MutableLiveData<CommunityUiState> uiState = new MutableLiveData<>(CommunityUiState.initial());
    private final SingleLiveEvent<String> message = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> postCreated = new SingleLiveEvent<>();
    private final SingleLiveEvent<CommunityUiEvent> event = new SingleLiveEvent<>();
    private String currentUserDisplayName = "";

    public LiveData<CommunityUiState> getUiState() { return uiState; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<Boolean> getPostCreated() { return postCreated; }
    public LiveData<CommunityUiEvent> getEvent() { return event; }

    @Inject
    public CommunityViewModel(CommunityRepository communityRepo, AuthRepository authRepo) {
        this.communityRepo = communityRepo;
        this.authRepo = authRepo;
        loadCurrentUserDisplayName();
        startListening(false);
    }

    private void startListening(boolean refreshing) {
        CommunityUiState current = currentState();
        uiState.setValue(current
                .withRefreshing(refreshing)
                .withInitialLoading(!refreshing && current.getPosts().isEmpty()));
        communityRepo.listenToPosts(new CommunityRepository.PostsCallback() {
            @Override
            public void onSuccess(List<Post> list) {
                uiState.postValue(currentState()
                        .withPosts(enrichPosts(list))
                        .withInitialLoading(false)
                        .withRefreshing(false));
            }

            @Override
            public void onError(Exception e) {
                uiState.postValue(currentState().withInitialLoading(false).withRefreshing(false));
                emitMessage(messageFrom(e));
            }
        });
    }

    public void refresh() {
        communityRepo.removeListener();
        startListening(true);
    }

    public void createPost(String content) {
        FirebaseUser user = authRepo.getCurrentUser();
        if (user == null) {
            emitMessage(MESSAGE_LOGIN_TO_POST);
            return;
        }

        String sanitized = InputValidator.sanitizeContent(content);
        if (sanitized.isEmpty()) {
            emitMessage(MESSAGE_EMPTY_CONTENT);
            return;
        }

        if (currentState().isSubmittingPost()) {
            return;
        }

        uiState.setValue(currentState().withSubmittingPost(true));
        authRepo.getCurrentUserDisplayName(new AuthRepository.DisplayNameCallback() {
            @Override
            public void onSuccess(String displayName) {
                String resolvedName = resolveDisplayName(displayName, user.getDisplayName());
                currentUserDisplayName = resolvedName;
                createPostWithResolvedName(user.getUid(), resolvedName, sanitized);
            }

            @Override
            public void onError(Exception e) {
                String resolvedName = resolveDisplayName("", user.getDisplayName());
                currentUserDisplayName = resolvedName;
                createPostWithResolvedName(user.getUid(), resolvedName, sanitized);
            }
        });
    }

    private void createPostWithResolvedName(String uid, String name, String sanitizedContent) {
        communityRepo.createPost(uid, name, sanitizedContent, new CommunityRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                uiState.postValue(currentState().withSubmittingPost(false));
                postCreated.postValue(true);
                event.postValue(CommunityUiEvent.postCreated());
            }

            @Override
            public void onError(Exception e) {
                uiState.postValue(currentState().withSubmittingPost(false));
                emitMessage(messageFrom(e));
            }
        });
    }

    public void toggleLike(Post post) {
        FirebaseUser user = authRepo.getCurrentUser();
        if (user == null) {
            emitMessage(MESSAGE_LOGIN_TO_LIKE);
            return;
        }
        if (post == null || post.getId() == null || post.getId().trim().isEmpty()) {
            return;
        }

        String postId = post.getId();
        Set<String> pending = new HashSet<>(currentState().getPendingLikePostIds());
        if (!pending.add(postId)) {
            return;
        }
        uiState.setValue(currentState().withPendingLikePostIds(pending));

        communityRepo.toggleLike(postId, user.getUid(), new CommunityRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                removePendingLike(postId);
            }

            @Override
            public void onError(Exception e) {
                removePendingLike(postId);
                emitMessage(messageFrom(e));
            }
        });
    }

    public void updatePost(Post post, String content) {
        FirebaseUser user = authRepo.getCurrentUser();
        if (user == null) {
            emitMessage(MESSAGE_LOGIN_TO_MANAGE);
            return;
        }
        if (!canManagePost(post, user)) {
            emitMessage(MESSAGE_OWN_POST_ONLY);
            return;
        }

        String sanitized = InputValidator.sanitizeContent(content);
        if (sanitized.isEmpty()) {
            emitMessage(MESSAGE_EMPTY_CONTENT);
            return;
        }

        String postId = post.getId();
        if (!addPendingAction(postId)) {
            return;
        }

        communityRepo.updatePostContent(postId, sanitized, new CommunityRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                removePendingAction(postId);
                emitMessage(MESSAGE_POST_UPDATED);
                event.postValue(CommunityUiEvent.postUpdated());
            }

            @Override
            public void onError(Exception e) {
                removePendingAction(postId);
                emitMessage(messageFrom(e));
            }
        });
    }

    public void deletePost(Post post) {
        FirebaseUser user = authRepo.getCurrentUser();
        if (user == null) {
            emitMessage(MESSAGE_LOGIN_TO_MANAGE);
            return;
        }
        if (!canManagePost(post, user)) {
            emitMessage(MESSAGE_OWN_POST_ONLY);
            return;
        }

        String postId = post.getId();
        if (!addPendingAction(postId)) {
            return;
        }

        communityRepo.deletePost(postId, new CommunityRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                removePendingAction(postId);
                emitMessage(MESSAGE_POST_DELETED);
                event.postValue(CommunityUiEvent.postDeleted());
            }

            @Override
            public void onError(Exception e) {
                removePendingAction(postId);
                emitMessage(messageFrom(e));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        communityRepo.removeListener();
    }

    private void removePendingLike(String postId) {
        Set<String> pending = new HashSet<>(currentState().getPendingLikePostIds());
        pending.remove(postId);
        uiState.postValue(currentState().withPendingLikePostIds(pending));
    }

    private boolean addPendingAction(String postId) {
        Set<String> pending = new HashSet<>(currentState().getPendingActionPostIds());
        if (!pending.add(postId)) {
            return false;
        }
        uiState.setValue(currentState().withPendingActionPostIds(pending));
        return true;
    }

    private void removePendingAction(String postId) {
        Set<String> pending = new HashSet<>(currentState().getPendingActionPostIds());
        pending.remove(postId);
        uiState.postValue(currentState().withPendingActionPostIds(pending));
    }

    private CommunityUiState currentState() {
        CommunityUiState state = uiState.getValue();
        return state != null ? state : CommunityUiState.initial();
    }

    private void loadCurrentUserDisplayName() {
        if (authRepo.getCurrentUser() == null) {
            currentUserDisplayName = "";
            return;
        }

        authRepo.getCurrentUserDisplayName(new AuthRepository.DisplayNameCallback() {
            @Override
            public void onSuccess(String displayName) {
                currentUserDisplayName = resolveDisplayName(displayName, null);
                if (!currentUserDisplayName.isEmpty()) {
                    uiState.postValue(currentState().withPosts(enrichPosts(currentState().getPosts())));
                }
            }

            @Override
            public void onError(Exception e) {
                currentUserDisplayName = "";
            }
        });
    }

    private List<Post> enrichPosts(List<Post> posts) {
        if (posts == null || posts.isEmpty() || currentUserDisplayName.isEmpty()) {
            return posts;
        }

        FirebaseUser user = authRepo.getCurrentUser();
        if (user == null) {
            return posts;
        }

        List<Post> enriched = new ArrayList<>(posts.size());
        for (Post post : posts) {
            if (post != null
                    && user.getUid().equals(post.getAuthorId())
                    && isGenericAuthorName(post.getAuthorName())) {
                enriched.add(copyPostWithAuthorName(post, currentUserDisplayName));
            } else {
                enriched.add(post);
            }
        }
        return enriched;
    }

    private static Post copyPostWithAuthorName(Post source, String authorName) {
        Post copy = new Post();
        copy.setId(source.getId());
        copy.setAuthorId(source.getAuthorId());
        copy.setAuthorName(authorName);
        copy.setContent(source.getContent());
        copy.setLikes(source.getLikes());
        copy.setLikedBy(new ArrayList<>(source.getLikedBy()));
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
    }

    private static boolean canManagePost(Post post, FirebaseUser user) {
        return post != null
                && user != null
                && post.getId() != null
                && !post.getId().trim().isEmpty()
                && user.getUid().equals(post.getAuthorId());
    }

    private static boolean isGenericAuthorName(String authorName) {
        String sanitized = InputValidator.sanitizeName(authorName);
        return sanitized.isEmpty() || MESSAGE_DEFAULT_USER.equals(sanitized);
    }

    private static String resolveDisplayName(String profileName, String firebaseName) {
        String resolved = InputValidator.sanitizeName(profileName);
        if (resolved.isEmpty()) {
            resolved = InputValidator.sanitizeName(firebaseName);
        }
        return resolved.isEmpty() ? MESSAGE_DEFAULT_USER : resolved;
    }

    private void emitMessage(String value) {
        message.postValue(value);
        event.postValue(CommunityUiEvent.message(value));
    }

    private static String messageFrom(Exception e) {
        return e != null && e.getMessage() != null ? e.getMessage() : MESSAGE_GENERIC_ERROR;
    }
}
