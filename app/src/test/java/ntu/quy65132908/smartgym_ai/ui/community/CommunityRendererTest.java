package ntu.quy65132908.smartgym_ai.ui.community;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ContextThemeWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;

import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Post;
import ntu.quy65132908.smartgym_ai.databinding.FragmentCommunityBinding;

@RunWith(RobolectricTestRunner.class)
public class CommunityRendererTest {

    private Context context;
    private FragmentCommunityBinding binding;
    private PostAdapter adapter;

    @Before
    public void setup() {
        context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_SmartGymAI);
        binding = FragmentCommunityBinding.inflate(LayoutInflater.from(context));
        adapter = new PostAdapter(post -> {});
    }

    @Test
    public void renderInitialLoading_showsProgressAndHidesFeedItems() {
        CommunityRenderer.render(binding, adapter, CommunityUiState.initial());

        assertEquals(View.VISIBLE, binding.progressBar.getVisibility());
        assertEquals(View.GONE, binding.rvPosts.getVisibility());
        assertEquals(View.GONE, binding.tvEmpty.getVisibility());
    }

    @Test
    public void renderEmpty_keepsSwipeRefreshVisibleAndShowsEmptyMessage() {
        CommunityUiState state = CommunityUiState.loaded(Collections.emptyList());

        CommunityRenderer.render(binding, adapter, state);

        assertEquals(View.VISIBLE, binding.swipeRefresh.getVisibility());
        assertEquals(View.GONE, binding.progressBar.getVisibility());
        assertEquals(View.GONE, binding.rvPosts.getVisibility());
        assertEquals(View.VISIBLE, binding.tvEmpty.getVisibility());
        assertEquals("Chưa có bài viết nào", binding.tvEmpty.getText().toString());
    }

    @Test
    public void renderPosts_showsFeedAndSubmitsPosts() {
        Post post = new Post();
        post.setId("p1");
        post.setAuthorName("Quy");

        CommunityRenderer.render(binding, adapter, CommunityUiState.loaded(Collections.singletonList(post)));

        assertEquals(View.GONE, binding.progressBar.getVisibility());
        assertEquals(View.VISIBLE, binding.rvPosts.getVisibility());
        assertEquals(View.GONE, binding.tvEmpty.getVisibility());
        assertEquals(1, adapter.getItemCount());
    }

    @Test
    public void renderRefreshing_setsSwipeRefreshIndicatorOnly() {
        CommunityUiState state = CommunityUiState.initial().withRefreshing(true);

        CommunityRenderer.render(binding, adapter, state);

        assertTrue(binding.swipeRefresh.isRefreshing());
        assertFalse(binding.fabPost.isEnabled());
    }
}
