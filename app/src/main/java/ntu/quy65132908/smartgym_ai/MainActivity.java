package ntu.quy65132908.smartgym_ai;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.data.repository.UserRepository;
import ntu.quy65132908.smartgym_ai.databinding.ActivityMainBinding;
import ntu.quy65132908.smartgym_ai.ui.navigation.BottomNavHost;
import ntu.quy65132908.smartgym_ai.ui.onboarding.OnboardingDestinationResolver;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements BottomNavHost {

    @Inject
    FirebaseAuth firebaseAuth;

    @Inject
    UserRepository userRepository;

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Thiết lập điều hướng.
        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            throw new IllegalStateException("NavHostFragment not found. Check activity_main.xml contains R.id.nav_host_fragment");
        }
        navController = navHostFragment.getNavController();

        // Thiết lập thanh điều hướng dưới.
        NavigationUI.setupWithNavController(binding.bottomNav, navController);

        // Hiện hoặc ẩn thanh điều hướng dưới theo đích đến.
        Set<Integer> mainDestinations = new HashSet<>(Arrays.asList(
                R.id.nav_dashboard, R.id.nav_workout, R.id.nav_progress,
                R.id.nav_community, R.id.nav_profile
        ));

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.nav_ai_analysis) {
                binding.bottomNav.setVisibility(View.VISIBLE);
                MenuItem workoutItem = binding.bottomNav.getMenu().findItem(R.id.nav_workout);
                if (workoutItem != null) {
                    workoutItem.setChecked(true);
                }
            } else if (destination.getId() == R.id.nav_pose_trainer) {
                binding.bottomNav.setVisibility(View.GONE);
            } else if (mainDestinations.contains(destination.getId())) {
                binding.bottomNav.setVisibility(View.VISIBLE);
            } else {
                binding.bottomNav.setVisibility(View.GONE);
            }
        });

        // Tự điều hướng người dùng đã đăng nhập qua bước thiết lập ban đầu bắt buộc khi cần.
        if (firebaseAuth.getCurrentUser() != null) {
            NavDestination currentDest = navController.getCurrentDestination();
            if (currentDest != null && currentDest.getId() == R.id.nav_login) {
                routeLoggedInUser(firebaseAuth.getCurrentUser().getUid());
            }
        }
    }

    private void routeLoggedInUser(String uid) {
        userRepository.getUser(uid, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    if (canRouteFromLogin()) {
                        navigateToInitialDestination(user);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    if (canRouteFromLogin()) {
                        navController.navigate(R.id.action_login_to_onboarding);
                    }
                });
            }
        });
    }

    private void navigateToInitialDestination(User user) {
        if (OnboardingDestinationResolver.requiresOnboarding(user)) {
            navController.navigate(R.id.action_login_to_onboarding);
        } else {
            navController.navigate(R.id.action_login_to_dashboard);
        }
    }

    private boolean canRouteFromLogin() {
        if (navController == null) {
            return false;
        }
        NavDestination currentDest = navController.getCurrentDestination();
        return currentDest != null && currentDest.getId() == R.id.nav_login;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    @Override
    public void selectTab(@IdRes int menuItemId) {
        if (binding.bottomNav != null) {
            binding.bottomNav.setSelectedItemId(menuItemId);
        }
    }
}
