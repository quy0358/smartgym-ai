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

        viewModel.getPosts().observe(getViewLifecycleOwner(), posts -> {
            binding.progressBar.setVisibility(View.GONE);
            if (posts != null && !posts.isEmpty()) {
                adapter.submitList(posts);
                binding.tvEmpty.setVisibility(View.GONE);
                binding.swipeRefresh.setVisibility(View.VISIBLE);
            } else {
                binding.tvEmpty.setVisibility(View.VISIBLE);
            }
        });

        // Pull-to-refresh
        binding.swipeRefresh.setOnRefreshListener(() -> {
            // The real-time listener already provides updates, just show refresh briefly
            binding.swipeRefresh.setRefreshing(false);
        });

        viewModel.getIsRefreshing().observe(getViewLifecycleOwner(), refreshing -> {
            if (refreshing != null) {
                binding.swipeRefresh.setRefreshing(refreshing);
                if (refreshing) {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.tvEmpty.setVisibility(View.GONE);
                }
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
