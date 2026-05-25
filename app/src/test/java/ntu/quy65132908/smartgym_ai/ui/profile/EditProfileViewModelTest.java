package ntu.quy65132908.smartgym_ai.ui.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;

@RunWith(RobolectricTestRunner.class)
public class EditProfileViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private UserRepository userRepository;
    @Mock private AuthRepository authRepository;
    @Mock private FirebaseUser firebaseUser;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(firebaseUser.getUid()).thenReturn("uid-1");
        when(firebaseUser.getEmail()).thenReturn("test@email.com");
        when(authRepository.getCurrentUser()).thenReturn(firebaseUser);
        stubUserLoad(user());
    }

    @Test
    public void formChanged_validCommaNumbers_updatesBmiPreviewAndCanSave() {
        EditProfileViewModel viewModel = createViewModel();

        viewModel.onProfileFormChanged("Quy", "70,5", "167", "cơ bụng 6 múi");

        EditProfileUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertTrue(state.canSave());
        assertEquals(25.3f, state.getPreviewBmi(), 0.05f);
        assertEquals("Thừa cân", state.getPreviewBmiCategory());
    }

    @Test
    public void saveProfile_invalidRanges_setsErrorsAndSkipsRepository() {
        EditProfileViewModel viewModel = createViewModel();

        viewModel.saveProfile("", "10", "260", "");

        ProfileFormErrors errors = viewModel.getFormErrors().getValue();
        assertNotNull(errors);
        assertEquals(RuntimeEnvironment.getApplication().getString(R.string.profile_name_empty),
                errors.getDisplayNameError());
        assertEquals(RuntimeEnvironment.getApplication().getString(R.string.profile_weight_range_error),
                errors.getWeightError());
        assertEquals(RuntimeEnvironment.getApplication().getString(R.string.profile_height_range_error),
                errors.getHeightError());
        verify(userRepository, never()).updateUser(any(), any(), any());
    }

    @Test
    public void saveProfile_blankNumbers_clearStoredMetrics() {
        EditProfileViewModel viewModel = createViewModel();
        doAnswer(invocation -> {
            UserRepository.SimpleCallback cb = invocation.getArgument(2);
            cb.onSuccess();
            return null;
        }).when(userRepository).updateUser(eq("uid-1"), any(), any());

        viewModel.saveProfile("Quy", "", "", "");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).updateUser(eq("uid-1"), captor.capture(), any());
        User saved = captor.getValue();
        assertNull(saved.getWeight());
        assertNull(saved.getHeight());
        assertNull(saved.getBmi());
        assertFalse(saved.toMap().containsKey("bmiCategory"));
    }

    @Test
    public void init_withoutCurrentUser_exposesLoggedOutState() {
        when(authRepository.getCurrentUser()).thenReturn(null);

        EditProfileViewModel viewModel = createViewModel();

        EditProfileUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertTrue(state.isLoggedOut());
        assertFalse(state.canSave());
    }

    private EditProfileViewModel createViewModel() {
        return new EditProfileViewModel(
                userRepository,
                authRepository,
                RuntimeEnvironment.getApplication()
        );
    }

    private void stubUserLoad(User user) {
        doAnswer(invocation -> {
            UserRepository.UserCallback cb = invocation.getArgument(1);
            cb.onSuccess(user);
            return null;
        }).when(userRepository).getUser(eq("uid-1"), any());
    }

    private User user() {
        User user = new User("uid-1", "Quy", "test@email.com");
        user.setWeight(70f);
        user.setHeight(167f);
        user.setBmi(25.1f);
        user.setBmiCategory("Thừa cân");
        user.setGoal("cơ bụng 6 múi");
        return user;
    }
}
