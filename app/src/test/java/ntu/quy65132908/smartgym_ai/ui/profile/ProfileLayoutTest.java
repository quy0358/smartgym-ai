package ntu.quy65132908.smartgym_ai.ui.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.databinding.FragmentEditProfileBinding;
import ntu.quy65132908.smartgym_ai.databinding.FragmentProfileBinding;

@RunWith(RobolectricTestRunner.class)
public class ProfileLayoutTest {

    @Test
    public void profileLayout_inflatesWithBottomPaddingAndProgressOverlay() {
        Context context = themedContext();
        FragmentProfileBinding binding = FragmentProfileBinding.inflate(LayoutInflater.from(context));

        assertNotNull(binding.progressBar);
        assertEquals(context.getResources().getDimensionPixelSize(R.dimen.screen_bottom_padding),
                binding.scrollView.getPaddingBottom());
    }

    @Test
    public void editProfileLayout_inflatesWithToolbarAndDisabledSave() {
        Context context = themedContext();
        FragmentEditProfileBinding binding = FragmentEditProfileBinding.inflate(LayoutInflater.from(context));

        assertNotNull(binding.toolbar);
        assertNotNull(binding.tilDisplayName);
        assertNotNull(binding.tilWeight);
        assertNotNull(binding.tilHeight);
        assertNotNull(binding.tilGoal);
        assertEquals(false, binding.btnSave.isEnabled());
    }

    private Context themedContext() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_SmartGymAI);
    }
}
