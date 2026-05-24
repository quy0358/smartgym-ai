package ntu.quy65132908.smartgym_ai.ui.navigation;

import androidx.annotation.IdRes;

/**
 * Interface for Activities that host a BottomNavigationView.
 * Decouples Fragments from direct Activity layout manipulation.
 */
public interface BottomNavHost {
    void selectTab(@IdRes int menuItemId);
}
