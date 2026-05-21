package ntu.quy65132908.smartgym_ai.ui.community;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import ntu.quy65132908.smartgym_ai.R;

public class CreatePostBottomSheet extends BottomSheetDialogFragment {

    private CommunityViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_create_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get ViewModel from parent fragment
        viewModel = new ViewModelProvider(requireParentFragment()).get(CommunityViewModel.class);

        TextInputEditText etContent = view.findViewById(R.id.et_post_content);
        MaterialButton btnSubmit = view.findViewById(R.id.btn_submit_post);

        // Enable button only when content is non-empty
        etContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnSubmit.setEnabled(s.toString().trim().length() > 0);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSubmit.setOnClickListener(v -> {
            String content = etContent.getText() != null ? etContent.getText().toString() : "";
            viewModel.createPost(content);
        });

        // Dismiss on successful post
        viewModel.getPostCreated().observe(getViewLifecycleOwner(), created -> {
            if (created != null && created) {
                dismiss();
            }
        });
    }
}
