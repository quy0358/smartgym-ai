package ntu.quy65132908.smartgym_ai.ui.community;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import ntu.quy65132908.smartgym_ai.data.model.Post;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.CommunityRepository;

public class CommunityViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private CommunityRepository communityRepository;
    @Mock private AuthRepository authRepository;
    @Mock private FirebaseUser firebaseUser;

    private AtomicReference<CommunityRepository.PostsCallback> postsCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        postsCallback = new AtomicReference<>();
        doAnswer(invocation -> {
            postsCallback.set(invocation.getArgument(0));
            return null;
        }).when(communityRepository).listenToPosts(any());
        when(firebaseUser.getUid()).thenReturn("uid-1");
        when(firebaseUser.getDisplayName()).thenReturn("  Quy  ");
        when(authRepository.getCurrentUser()).thenReturn(firebaseUser);
        stubProfileDisplayName("Quy");
    }

    @Test
    public void init_successShowsPostsAndStopsInitialLoading() {
        CommunityViewModel viewModel = createViewModel();
        Post first = post("p1", "Quy");
        Post second = post("p2", "Người dùng");

        postsCallback.get().onSuccess(Arrays.asList(first, second));

        CommunityUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertFalse(state.isInitialLoading());
        assertFalse(state.isRefreshing());
        assertFalse(state.isEmpty());
        assertEquals(2, state.getPosts().size());
    }

    @Test
    public void init_emptyShowsEmptyStateAndKeepsRefreshSurfaceVisible() {
        CommunityViewModel viewModel = createViewModel();

        postsCallback.get().onSuccess(Collections.emptyList());

        CommunityUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertFalse(state.isInitialLoading());
        assertTrue(state.isEmpty());
        assertEquals("Chưa có bài viết nào", state.getEmptyMessage());
    }

    @Test
    public void init_ownGenericPostsUseProfileDisplayName() {
        stubProfileDisplayName("Nguyễn Thanh Quý");
        CommunityViewModel viewModel = createViewModel();
        Post post = post("p1", "Người dùng");
        post.setAuthorId("uid-1");

        postsCallback.get().onSuccess(Collections.singletonList(post));

        CommunityUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertEquals("Nguyễn Thanh Quý", state.getPosts().get(0).getAuthorName());
    }

    @Test
    public void createPost_loggedOutShowsMessageAndSkipsRepository() {
        when(authRepository.getCurrentUser()).thenReturn(null);
        CommunityViewModel viewModel = createViewModel();

        viewModel.createPost("xin chào");

        assertEquals("Bạn cần đăng nhập để đăng bài.", viewModel.getMessage().getValue());
        verify(communityRepository, never()).createPost(any(), any(), any(), any());
    }

    @Test
    public void createPost_trimsContentTogglesSubmittingAndEmitsCreatedEvent() {
        CommunityViewModel viewModel = createViewModel();
        AtomicReference<CommunityRepository.SimpleCallback> createCallback = new AtomicReference<>();
        doAnswer(invocation -> {
            createCallback.set(invocation.getArgument(3));
            return null;
        }).when(communityRepository).createPost(any(), any(), any(), any());

        viewModel.createPost("  tập tốt hôm nay  ");

        assertTrue(viewModel.getUiState().getValue().isSubmittingPost());
        ArgumentCaptor<String> authorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(communityRepository).createPost(eq("uid-1"), authorCaptor.capture(), contentCaptor.capture(), any());
        assertEquals("Quy", authorCaptor.getValue());
        assertEquals("tập tốt hôm nay", contentCaptor.getValue());

        createCallback.get().onSuccess();

        assertFalse(viewModel.getUiState().getValue().isSubmittingPost());
        assertTrue(Boolean.TRUE.equals(viewModel.getPostCreated().getValue()));
    }

    @Test
    public void createPost_usesProfileDisplayNameWhenFirebaseDisplayNameIsMissing() {
        when(firebaseUser.getDisplayName()).thenReturn(null);
        stubProfileDisplayName("Nguyễn Thanh Quý");
        CommunityViewModel viewModel = createViewModel();
        AtomicReference<CommunityRepository.SimpleCallback> createCallback = new AtomicReference<>();
        doAnswer(invocation -> {
            createCallback.set(invocation.getArgument(3));
            return null;
        }).when(communityRepository).createPost(any(), any(), any(), any());

        viewModel.createPost("  hôm nay tập tốt  ");

        ArgumentCaptor<String> authorCaptor = ArgumentCaptor.forClass(String.class);
        verify(communityRepository).createPost(eq("uid-1"), authorCaptor.capture(), eq("hôm nay tập tốt"), any());
        assertEquals("Nguyễn Thanh Quý", authorCaptor.getValue());
        createCallback.get().onSuccess();
    }

    @Test
    public void toggleLike_addsPendingPostAndClearsItAfterSuccess() {
        CommunityViewModel viewModel = createViewModel();
        Post post = post("p1", "Quy");
        AtomicReference<CommunityRepository.SimpleCallback> likeCallback = new AtomicReference<>();
        doAnswer(invocation -> {
            likeCallback.set(invocation.getArgument(2));
            return null;
        }).when(communityRepository).toggleLike(any(), any(), any());

        viewModel.toggleLike(post);

        assertTrue(viewModel.getUiState().getValue().getPendingLikePostIds().contains("p1"));
        verify(communityRepository).toggleLike(eq("p1"), eq("uid-1"), any());

        likeCallback.get().onSuccess();

        assertFalse(viewModel.getUiState().getValue().getPendingLikePostIds().contains("p1"));
    }

    @Test
    public void toggleLike_loggedOutShowsMessageAndSkipsRepository() {
        when(authRepository.getCurrentUser()).thenReturn(null);
        CommunityViewModel viewModel = createViewModel();

        viewModel.toggleLike(post("p1", "Quy"));

        assertEquals("Bạn cần đăng nhập để thích bài viết.", viewModel.getMessage().getValue());
        verify(communityRepository, never()).toggleLike(any(), any(), any());
    }

    @Test
    public void updatePost_ownPostTrimsContentAndClearsPendingAfterSuccess() {
        CommunityViewModel viewModel = createViewModel();
        AtomicReference<CommunityRepository.SimpleCallback> updateCallback = new AtomicReference<>();
        doAnswer(invocation -> {
            updateCallback.set(invocation.getArgument(2));
            return null;
        }).when(communityRepository).updatePostContent(any(), any(), any());

        viewModel.updatePost(post("p1", "Quy"), "  ná»— lá»±c hÆ¡n hÃ´m nay  ");

        assertTrue(viewModel.getUiState().getValue().getPendingActionPostIds().contains("p1"));
        verify(communityRepository).updatePostContent(eq("p1"), eq("ná»— lá»±c hÆ¡n hÃ´m nay"), any());

        updateCallback.get().onSuccess();

        assertFalse(viewModel.getUiState().getValue().getPendingActionPostIds().contains("p1"));
    }

    @Test
    public void updatePost_nonOwnerShowsMessageAndSkipsRepository() {
        CommunityViewModel viewModel = createViewModel();
        Post otherPost = post("p2", "Other");
        otherPost.setAuthorId("uid-2");

        viewModel.updatePost(otherPost, "ná»™i dung má»›i");

        assertNotNull(viewModel.getMessage().getValue());
        verify(communityRepository, never()).updatePostContent(any(), any(), any());
    }

    @Test
    public void deletePost_ownPostClearsPendingAfterSuccess() {
        CommunityViewModel viewModel = createViewModel();
        AtomicReference<CommunityRepository.SimpleCallback> deleteCallback = new AtomicReference<>();
        doAnswer(invocation -> {
            deleteCallback.set(invocation.getArgument(1));
            return null;
        }).when(communityRepository).deletePost(any(), any());

        viewModel.deletePost(post("p1", "Quy"));

        assertTrue(viewModel.getUiState().getValue().getPendingActionPostIds().contains("p1"));
        verify(communityRepository).deletePost(eq("p1"), any());

        deleteCallback.get().onSuccess();

        assertFalse(viewModel.getUiState().getValue().getPendingActionPostIds().contains("p1"));
    }

    @Test
    public void deletePost_loggedOutShowsMessageAndSkipsRepository() {
        when(authRepository.getCurrentUser()).thenReturn(null);
        CommunityViewModel viewModel = createViewModel();

        viewModel.deletePost(post("p1", "Quy"));

        assertNotNull(viewModel.getMessage().getValue());
        verify(communityRepository, never()).deletePost(any(), any());
    }

    private CommunityViewModel createViewModel() {
        return new CommunityViewModel(communityRepository, authRepository);
    }

    private void stubProfileDisplayName(String displayName) {
        doAnswer(invocation -> {
            AuthRepository.DisplayNameCallback cb = invocation.getArgument(0);
            cb.onSuccess(displayName);
            return null;
        }).when(authRepository).getCurrentUserDisplayName(any());
    }

    private static Post post(String id, String author) {
        Post post = new Post();
        post.setId(id);
        post.setAuthorId("uid-1");
        post.setAuthorName(author);
        post.setContent("content");
        post.setCreatedAt(1_000L);
        post.setLikes(1);
        return post;
    }
}
