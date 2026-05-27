package ntu.quy65132908.smartgym_ai.ui.community;

import android.view.View;

import ntu.quy65132908.smartgym_ai.databinding.FragmentCommunityBinding;

public final class CommunityRenderer {
    private static final float DISABLED_ACTION_ALPHA = 0.5f;
    private static final float ENABLED_ACTION_ALPHA = 1f;

    private CommunityRenderer() {}

    public static void render(
            FragmentCommunityBinding binding,
            PostAdapter adapter,
            CommunityUiState state
    ) {
        if (binding == null || adapter == null || state == null) {
            return;
        }

        adapter.setPendingLikePostIds(state.getPendingLikePostIds());
        adapter.setPendingActionPostIds(state.getPendingActionPostIds());
        adapter.submitList(state.getPosts());

        boolean hasPosts = !state.getPosts().isEmpty();
        binding.swipeRefresh.setVisibility(View.VISIBLE);
        binding.swipeRefresh.setRefreshing(state.isRefreshing());
        binding.progressBar.setVisibility(state.isInitialLoading() ? View.VISIBLE : View.GONE);
        binding.rvPosts.setVisibility(hasPosts ? View.VISIBLE : View.GONE);
        binding.tvEmpty.setVisibility(state.isEmpty() ? View.VISIBLE : View.GONE);
        binding.tvEmpty.setText(state.getEmptyMessage());

        boolean actionEnabled = !state.isRefreshing()
                && !state.isSubmittingPost()
                && state.getPendingActionPostIds().isEmpty();
        binding.fabPost.setEnabled(actionEnabled);
        binding.fabPost.setAlpha(actionEnabled ? ENABLED_ACTION_ALPHA : DISABLED_ACTION_ALPHA);
    }
}
