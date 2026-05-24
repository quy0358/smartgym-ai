package ntu.quy65132908.smartgym_ai.ui.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.databinding.FragmentCommunityBinding;

@AndroidEntryPoint
public class CommunityFragment extends Fragment {

    private FragmentCommunityBinding binding;
    private CommunityViewModel viewModel;
    private PostAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCommunityBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CommunityViewModel.class);

        adapter = new PostAdapter((post, isLiked) -> viewModel.toggleLike(post.getId(), isLiked));
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) adapter.setCurrentUserId(user.getUid());

        binding.rvPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPosts.setAdapter(adapter);

        // H3: Fix visibility logic — show loading initially
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);
        binding.swipeRefresh.setVisibility(View.GONE);

        viewModel.getPosts().observe(getViewLifecycleOwner(), posts -> {
            // H3: Only update visibility when not refreshing
            Boolean refreshing = viewModel.getIsRefreshing().getValue();
            boolean isRefreshing = refreshing != null && refreshing;

            if (!isRefreshing) {
                binding.progressBar.setVisibility(View.GONE);
            }

            if (posts != null && !posts.isEmpty()) {
                adapter.submitList(posts);
                binding.tvEmpty.setVisibility(View.GONE);
                binding.swipeRefresh.setVisibility(View.VISIBLE);
            } else if (!isRefreshing) {
                binding.tvEmpty.setVisibility(View.VISIBLE);
                binding.swipeRefresh.setVisibility(View.GONE);
            }
        });

        // H2: Real pull-to-refresh — actually refresh data
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());

        viewModel.getIsRefreshing().observe(getViewLifecycleOwner(), refreshing -> {
            if (refreshing != null) {
                binding.swipeRefresh.setRefreshing(refreshing);
                if (refreshing) {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.tvEmpty.setVisibility(View.GONE);
                } else {
                    binding.progressBar.setVisibility(View.GONE);
                }
            }
        });

        // H8: Error handling
        viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });

        binding.fabPost.setOnClickListener(v ->
            new CreatePostBottomSheet().show(getChildFragmentManager(), "create_post")
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
