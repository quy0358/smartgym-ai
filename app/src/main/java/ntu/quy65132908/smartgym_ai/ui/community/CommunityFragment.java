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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Post;
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

        adapter = new PostAdapter(
                post -> viewModel.toggleLike(post),
                new PostAdapter.OnPostActionListener() {
                    @Override
                    public void onEdit(Post post) {
                        CreatePostBottomSheet.newEditInstance(post)
                                .show(getChildFragmentManager(), "edit_post");
                    }

                    @Override
                    public void onDelete(Post post) {
                        confirmDelete(post);
                    }
                }
        );
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            adapter.setCurrentUserId(user.getUid());
        }

        binding.rvPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPosts.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
        viewModel.getUiState().observe(getViewLifecycleOwner(),
                state -> CommunityRenderer.render(binding, adapter, state));
        viewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });

        binding.fabPost.setOnClickListener(v ->
                CreatePostBottomSheet.newCreateInstance().show(getChildFragmentManager(), "create_post")
        );
    }

    private void confirmDelete(Post post) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.post_delete_title)
                .setMessage(R.string.post_delete_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.post_delete_confirm, (dialog, which) -> viewModel.deletePost(post))
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
