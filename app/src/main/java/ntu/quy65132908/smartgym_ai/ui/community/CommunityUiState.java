package ntu.quy65132908.smartgym_ai.ui.community;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ntu.quy65132908.smartgym_ai.data.model.Post;

public final class CommunityUiState {
    static final String DEFAULT_EMPTY_MESSAGE = "Chưa có bài viết nào";

    private final List<Post> posts;
    private final boolean initialLoading;
    private final boolean refreshing;
    private final boolean submittingPost;
    private final Set<String> pendingLikePostIds;
    private final Set<String> pendingActionPostIds;
    private final String emptyMessage;

    private CommunityUiState(
            List<Post> posts,
            boolean initialLoading,
            boolean refreshing,
            boolean submittingPost,
            Set<String> pendingLikePostIds,
            Set<String> pendingActionPostIds,
            String emptyMessage
    ) {
        this.posts = Collections.unmodifiableList(new ArrayList<>(posts));
        this.initialLoading = initialLoading;
        this.refreshing = refreshing;
        this.submittingPost = submittingPost;
        this.pendingLikePostIds = Collections.unmodifiableSet(new HashSet<>(pendingLikePostIds));
        this.pendingActionPostIds = Collections.unmodifiableSet(new HashSet<>(pendingActionPostIds));
        this.emptyMessage = emptyMessage;
    }

    public static CommunityUiState initial() {
        return new CommunityUiState(
                Collections.emptyList(),
                true,
                false,
                false,
                Collections.emptySet(),
                Collections.emptySet(),
                DEFAULT_EMPTY_MESSAGE
        );
    }

    public static CommunityUiState loaded(List<Post> posts) {
        return initial().withPosts(posts).withInitialLoading(false);
    }

    public List<Post> getPosts() {
        return posts;
    }

    public boolean isInitialLoading() {
        return initialLoading;
    }

    public boolean isRefreshing() {
        return refreshing;
    }

    public boolean isSubmittingPost() {
        return submittingPost;
    }

    public Set<String> getPendingLikePostIds() {
        return pendingLikePostIds;
    }

    public Set<String> getPendingActionPostIds() {
        return pendingActionPostIds;
    }

    public String getEmptyMessage() {
        return emptyMessage;
    }

    public boolean isEmpty() {
        return !initialLoading && posts.isEmpty();
    }

    public CommunityUiState withPosts(List<Post> posts) {
        return new CommunityUiState(
                posts != null ? posts : Collections.emptyList(),
                initialLoading,
                refreshing,
                submittingPost,
                pendingLikePostIds,
                pendingActionPostIds,
                emptyMessage
        );
    }

    public CommunityUiState withInitialLoading(boolean initialLoading) {
        return new CommunityUiState(
                posts,
                initialLoading,
                refreshing,
                submittingPost,
                pendingLikePostIds,
                pendingActionPostIds,
                emptyMessage
        );
    }

    public CommunityUiState withRefreshing(boolean refreshing) {
        return new CommunityUiState(
                posts,
                initialLoading,
                refreshing,
                submittingPost,
                pendingLikePostIds,
                pendingActionPostIds,
                emptyMessage
        );
    }

    public CommunityUiState withSubmittingPost(boolean submittingPost) {
        return new CommunityUiState(
                posts,
                initialLoading,
                refreshing,
                submittingPost,
                pendingLikePostIds,
                pendingActionPostIds,
                emptyMessage
        );
    }

    public CommunityUiState withPendingLikePostIds(Set<String> pendingLikePostIds) {
        return new CommunityUiState(
                posts,
                initialLoading,
                refreshing,
                submittingPost,
                pendingLikePostIds != null ? pendingLikePostIds : Collections.emptySet(),
                pendingActionPostIds,
                emptyMessage
        );
    }

    public CommunityUiState withPendingActionPostIds(Set<String> pendingActionPostIds) {
        return new CommunityUiState(
                posts,
                initialLoading,
                refreshing,
                submittingPost,
                pendingLikePostIds,
                pendingActionPostIds != null ? pendingActionPostIds : Collections.emptySet(),
                emptyMessage
        );
    }
}
