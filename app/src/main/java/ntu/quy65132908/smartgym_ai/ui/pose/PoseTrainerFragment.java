package ntu.quy65132908.smartgym_ai.ui.pose;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.databinding.FragmentPoseTrainerBinding;

@AndroidEntryPoint
@ExperimentalGetImage
public class PoseTrainerFragment extends Fragment {
    private FragmentPoseTrainerBinding binding;
    private PoseTrainerViewModel viewModel;
    private PoseDetector poseDetector;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private TextToSpeech textToSpeech;
    private String lastSpokenFeedback = "";
    private long lastSpokenAt = 0L;
    private volatile boolean analyzingFrame;
    private boolean usingFrontCamera = true;
    private boolean voiceFeedbackEnabled = true;
    private boolean updatingExerciseSelection;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    showPermissionUi(false);
                    startCamera();
                } else {
                    viewModel.setPermissionDenied();
                    showPermissionUi(true);
                    if (binding != null) {
                        Snackbar.make(binding.getRoot(), R.string.pose_permission_denied, Snackbar.LENGTH_LONG).show();
                    }
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPoseTrainerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PoseTrainerViewModel.class);
        cameraExecutor = Executors.newSingleThreadExecutor();
        poseDetector = PoseDetection.getClient(new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build());

        setupToolbar();
        setupExerciseControls();
        setupCameraControls();
        setupTextToSpeech();
        observeViewModel();
        binding.btnGrantCamera.setOnClickListener(v -> requestCameraPermission());

        if (hasCameraPermission()) {
            showPermissionUi(false);
            startCamera();
        } else {
            showPermissionUi(true);
        }
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    private void setupExerciseControls() {
        binding.groupPoseType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (updatingExerciseSelection || !isChecked) {
                return;
            }
            if (checkedId == R.id.btn_pose_squat) {
                viewModel.selectExerciseType(ExerciseType.SQUAT);
            } else if (checkedId == R.id.btn_pose_plank) {
                viewModel.selectExerciseType(ExerciseType.PLANK);
            } else {
                viewModel.selectExerciseType(ExerciseType.PUSH_UP);
            }
        });
        binding.switchVoiceFeedback.setOnCheckedChangeListener((buttonView, isChecked) ->
                voiceFeedbackEnabled = isChecked);
    }

    private void setupCameraControls() {
        binding.btnSwitchCamera.setOnClickListener(v -> {
            usingFrontCamera = !usingFrontCamera;
            if (cameraProvider != null) {
                bindCameraUseCases();
            }
        });
    }

    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(requireContext().getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS && textToSpeech != null) {
                textToSpeech.setLanguage(new Locale("vi", "VN"));
                textToSpeech.setSpeechRate(0.96f);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || binding == null) return;
            binding.tvExerciseType.setText(state.getExerciseType().getDisplayName());
            binding.tvFeedback.setText(state.getFeedback());
            updateSelectedExerciseButton(state.getExerciseType());
            updatePrimaryMetric(state);
            binding.tvQuality.setText(state.getQualityPercent() > 0
                    ? state.getQualityPercent() + "%"
                    : "--%");
            binding.progressCamera.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            if (state.isPermissionDenied()) {
                showPermissionUi(true);
            }
            maybeSpeak(state.getFeedback());
        });
    }

    private void updatePrimaryMetric(PoseTrainerUiState state) {
        if (state.getExerciseType().usesDurationMetric()) {
            binding.tvPrimaryMetricLabel.setText(R.string.pose_hold_time_label);
            binding.tvReps.setText(getString(R.string.pose_hold_time_format, state.getHoldSeconds()));
        } else {
            binding.tvPrimaryMetricLabel.setText(R.string.pose_reps_label);
            binding.tvReps.setText(getString(R.string.pose_reps_format, state.getReps()));
        }
    }

    private void updateSelectedExerciseButton(ExerciseType exerciseType) {
        int checkedId;
        switch (exerciseType) {
            case SQUAT:
                checkedId = R.id.btn_pose_squat;
                break;
            case PLANK:
                checkedId = R.id.btn_pose_plank;
                break;
            case PUSH_UP:
            default:
                checkedId = R.id.btn_pose_push_up;
                break;
        }
        if (binding.groupPoseType.getCheckedButtonId() == checkedId) {
            return;
        }
        updatingExerciseSelection = true;
        binding.groupPoseType.check(checkedId);
        updatingExerciseSelection = false;
    }

    private void requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void showPermissionUi(boolean show) {
        if (binding == null) return;
        binding.layoutPermission.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void startCamera() {
        if (binding == null) return;
        viewModel.setLoading(true);
        ListenableFuture<ProcessCameraProvider> providerFuture = ProcessCameraProvider.getInstance(requireContext());
        providerFuture.addListener(() -> {
            try {
                cameraProvider = providerFuture.get();
                bindCameraUseCases();
                viewModel.setCameraReady(true);
            } catch (Exception e) {
                viewModel.setLoading(false);
                if (binding != null) {
                    Snackbar.make(binding.getRoot(), R.string.pose_camera_error, Snackbar.LENGTH_LONG).show();
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null || binding == null) return;

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        analysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        CameraSelector cameraSelector = usingFrontCamera
                ? CameraSelector.DEFAULT_FRONT_CAMERA
                : CameraSelector.DEFAULT_BACK_CAMERA;
        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                    getViewLifecycleOwner(),
                    cameraSelector,
                    preview,
                    analysis);
        } catch (Exception e) {
            if (!usingFrontCamera) {
                usingFrontCamera = true;
                bindCameraUseCases();
                return;
            }
            if (binding != null) {
                Snackbar.make(binding.getRoot(), R.string.pose_camera_error, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (analyzingFrame) {
            imageProxy.close();
            return;
        }
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        analyzingFrame = true;
        int rotation = imageProxy.getImageInfo().getRotationDegrees();
        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), rotation);
        int mappedImageWidth = imageProxy.getWidth();
        int mappedImageHeight = imageProxy.getHeight();
        if (rotation == 90 || rotation == 270) {
            mappedImageWidth = imageProxy.getHeight();
            mappedImageHeight = imageProxy.getWidth();
        }
        final int imageWidth = mappedImageWidth;
        final int imageHeight = mappedImageHeight;
        final boolean frameIsFrontCamera = usingFrontCamera;

        poseDetector.process(image)
                .addOnSuccessListener(pose -> {
                    PoseFrame frame = MlKitPoseMapper.map(pose, imageWidth, imageHeight, frameIsFrontCamera);
                    if (binding != null) {
                        binding.poseOverlay.setPoseFrame(frame);
                    }
                    viewModel.onPoseFrame(frame);
                })
                .addOnFailureListener(ignored -> {
                    if (binding != null) {
                        binding.poseOverlay.setPoseFrame(PoseFrame.empty());
                    }
                    viewModel.onPoseFrame(PoseFrame.empty());
                })
                .addOnCompleteListener(task -> {
                    analyzingFrame = false;
                    imageProxy.close();
                });
    }

    private void maybeSpeak(String feedback) {
        if (!voiceFeedbackEnabled || feedback == null || feedback.trim().isEmpty() || textToSpeech == null) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean noPersonMessage = feedback.contains("toàn thân");
        long sameMessageDelayMs = noPersonMessage ? 10000L : 6000L;
        if (feedback.equals(lastSpokenFeedback) && now - lastSpokenAt < sameMessageDelayMs) {
            return;
        }
        if (now - lastSpokenAt < 3500L) {
            return;
        }
        lastSpokenFeedback = feedback;
        lastSpokenAt = now;
        textToSpeech.speak(feedback, TextToSpeech.QUEUE_FLUSH, null, "pose_feedback");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (poseDetector != null) {
            poseDetector.close();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
            cameraExecutor = null;
        }
        binding = null;
    }
}
