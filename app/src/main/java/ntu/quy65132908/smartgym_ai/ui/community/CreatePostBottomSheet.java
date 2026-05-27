package ntu.quy65132908.smartgym_ai.ui.community;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Post;

public class CreatePostBottomSheet extends BottomSheetDialogFragment {
    private static final String ARG_POST_ID = "post_id";
    private static final String ARG_AUTHOR_ID = "author_id";
    private static final String ARG_CONTENT = "content";

    private CommunityViewModel viewModel;
    private TextInputEditText etContent;
    private MaterialButton btnSubmit;
    private ProgressBar progressSubmit;
    private boolean hasContent;
    private boolean editMode;
    private String postId;
    private String authorId;

    public static CreatePostBottomSheet newCreateInstance() {
        return new CreatePostBottomSheet();
    }

    public static CreatePostBottomSheet newEditInstance(Post post) {
        CreatePostBottomSheet sheet = new CreatePostBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_POST_ID, post != null ? post.getId() : "");
        args.putString(ARG_AUTHOR_ID, post != null ? post.getAuthorId() : "");
        args.putString(ARG_CONTENT, post != null ? post.getContent() : "");
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.bottom_sheet_create_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment()).get(CommunityViewModel.class);
        etContent = view.findViewById(R.id.et_post_content);
        btnSubmit = view.findViewById(R.id.btn_submit_post);
        progressSubmit = view.findViewById(R.id.progress_submit_post);

        Bundle args = getArguments();
        editMode = args != null && args.containsKey(ARG_POST_ID);
        postId = args != null ? args.getString(ARG_POST_ID, "") : "";
        authorId = args != null ? args.getString(ARG_AUTHOR_ID, "") : "";
        String initialContent = args != null ? args.getString(ARG_CONTENT, "") : "";

        TextView title = view.findViewById(R.id.tv_post_sheet_title);
        title.setText(editMode ? R.string.community_edit_post_title : R.string.community_create_post_title);
        etContent.setText(initialContent);
        etContent.setSelection(etContent.getText() != null ? etContent.getText().length() : 0);
        hasContent = initialContent.trim().length() > 0;

        etContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasContent = s != null && s.toString().trim().length() > 0;
                updateSubmitState(currentState());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSubmit.setOnClickListener(v -> {
            String content = etContent.getText() != null ? etContent.getText().toString() : "";
            if (editMode) {
                Post post = new Post();
                post.setId(postId);
                post.setAuthorId(authorId);
                viewModel.updatePost(post, content);
            } else {
                viewModel.createPost(content);
            }
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::updateSubmitState);
        viewModel.getEvent().observe(getViewLifecycleOwner(), event -> {
            if (event == null) {
                return;
            }
            if (event.getType() == CommunityUiEvent.Type.POST_CREATED
                    || event.getType() == CommunityUiEvent.Type.POST_UPDATED) {
                dismiss();
            }
        });
        updateSubmitState(currentState());
    }

    private void updateSubmitState(CommunityUiState state) {
        if (btnSubmit == null || progressSubmit == null || state == null) {
            return;
        }
        boolean submitting = state.isSubmittingPost();
        boolean updating = editMode && state.getPendingActionPostIds().contains(postId);
        boolean busy = submitting || updating;
        btnSubmit.setEnabled(hasContent && !busy);
        if (editMode) {
            btnSubmit.setText(updating ? R.string.community_updating_post : R.string.community_update_post);
        } else {
            btnSubmit.setText(submitting ? R.string.community_submitting_post : R.string.community_submit_post);
        }
        progressSubmit.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    private CommunityUiState currentState() {
        return viewModel != null && viewModel.getUiState().getValue() != null
                ? viewModel.getUiState().getValue()
                : CommunityUiState.initial();
    }
}
