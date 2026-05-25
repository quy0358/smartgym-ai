package ntu.quy65132908.smartgym_ai.ui.community;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import ntu.quy65132908.smartgym_ai.R;

public class CreatePostBottomSheet extends BottomSheetDialogFragment {

    private CommunityViewModel viewModel;
    private TextInputEditText etContent;
    private MaterialButton btnSubmit;
    private ProgressBar progressSubmit;
    private boolean hasContent;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_create_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment()).get(CommunityViewModel.class);
        etContent = view.findViewById(R.id.et_post_content);
        btnSubmit = view.findViewById(R.id.btn_submit_post);
        progressSubmit = view.findViewById(R.id.progress_submit_post);

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
            viewModel.createPost(content);
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            updateSubmitState(state);
        });
        viewModel.getPostCreated().observe(getViewLifecycleOwner(), created -> {
            if (created != null && created) {
                dismiss();
            }
        });
    }

    private void updateSubmitState(CommunityUiState state) {
        if (btnSubmit == null || progressSubmit == null || state == null) {
            return;
        }
        boolean submitting = state.isSubmittingPost();
        btnSubmit.setEnabled(hasContent && !submitting);
        btnSubmit.setText(submitting ? R.string.community_submitting_post : R.string.community_submit_post);
        progressSubmit.setVisibility(submitting ? View.VISIBLE : View.GONE);
    }

    private CommunityUiState currentState() {
        return viewModel != null && viewModel.getUiState().getValue() != null
                ? viewModel.getUiState().getValue()
                : CommunityUiState.initial();
    }
}
