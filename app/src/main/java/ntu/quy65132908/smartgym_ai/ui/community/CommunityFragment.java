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
            if (posts != null) adapter.submitList(posts);
        });

        binding.fabPost.setOnClickListener(v -> {
            // TODO: Open create post dialog (Task 11)
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
