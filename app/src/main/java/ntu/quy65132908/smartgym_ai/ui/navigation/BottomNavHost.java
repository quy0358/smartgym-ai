package ntu.quy65132908.smartgym_ai.ui.navigation;

import androidx.annotation.IdRes;

/**
 * Interface cho Activity chứa BottomNavigationView.
 * Tách Fragment khỏi thao tác trực tiếp trên layout của Activity.
 */
public interface BottomNavHost {
    void selectTab(@IdRes int menuItemId);
}
