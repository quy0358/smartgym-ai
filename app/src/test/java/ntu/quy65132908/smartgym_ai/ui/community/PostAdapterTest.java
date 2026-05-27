package ntu.quy65132908.smartgym_ai.ui.community;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.FrameLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowPopupMenu;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import ntu.quy65132908.smartgym_ai.R;
import ntu.quy65132908.smartgym_ai.data.model.Post;

@RunWith(RobolectricTestRunner.class)
public class PostAdapterTest {

    private Context context;
    private ViewGroup parent;

    @Before
    public void setup() {
        context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_SmartGymAI);
        parent = new FrameLayout(context);
    }

    @Test
    public void bind_emptyAuthorUsesDefaultNameAndAvatar() {
        PostAdapter adapter = new PostAdapter(post -> {});
        PostAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.bindPostForTest(holder, post("p1", "", 0));

        assertEquals(context.getString(R.string.post_user_default), holder.tvAuthor.getText().toString());
        assertEquals(context.getString(R.string.default_avatar_letter), holder.tvAvatar.getText().toString());
    }

    @Test
    public void bind_pendingLikedPostDisablesLikeButtonAndUsesLikedDescription() {
        PostAdapter adapter = new PostAdapter(post -> {});
        adapter.setCurrentUserId("uid-1");
        adapter.setPendingLikePostIds(Collections.singleton("p1"));
        PostAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.bindPostForTest(holder, post("p1", "Quy", 1));

        assertFalse(holder.btnLike.isEnabled());
        assertTrue(holder.btnLike.getAlpha() < 1f);
        assertEquals(context.getString(R.string.post_unlike_a11y), holder.btnLike.getContentDescription().toString());
    }

    @Test
    public void bind_nonOwnerHidesPostActions() {
        PostAdapter adapter = new PostAdapter(post -> {});
        adapter.setCurrentUserId("uid-1");
        PostAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        Post post = post("p2", "Other", 1);
        post.setAuthorId("uid-2");

        adapter.bindPostForTest(holder, post);

        assertEquals(View.GONE, holder.btnPostActions.getVisibility());
    }

    @Test
    public void bind_ownerShowsPostActionsAndPopupCallbacks() {
        AtomicReference<String> action = new AtomicReference<>();
        PostAdapter adapter = new PostAdapter(
                post -> action.set("like:" + post.getId()),
                new PostAdapter.OnPostActionListener() {
                    @Override
                    public void onEdit(Post post) {
                        action.set("edit:" + post.getId());
                    }

                    @Override
                    public void onDelete(Post post) {
                        action.set("delete:" + post.getId());
                    }
                }
        );
        adapter.setCurrentUserId("uid-1");
        PostAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        Post post = post("p1", "Quy", 1);
        post.setAuthorId("uid-1");

        adapter.bindPostForTest(holder, post);

        assertEquals(View.VISIBLE, holder.btnPostActions.getVisibility());

        holder.btnPostActions.performClick();
        PopupMenu menu = ShadowPopupMenu.getLatestPopupMenu();
        menu.getMenu().performIdentifierAction(R.id.action_edit_post, 0);
        assertEquals("edit:p1", action.get());

        holder.btnPostActions.performClick();
        menu = ShadowPopupMenu.getLatestPopupMenu();
        menu.getMenu().performIdentifierAction(R.id.action_delete_post, 0);
        assertEquals("delete:p1", action.get());
    }

    private static Post post(String id, String authorName, int likes) {
        Post post = new Post();
        post.setId(id);
        post.setAuthorId("uid-1");
        post.setAuthorName(authorName);
        post.setContent("content");
        post.setLikes(likes);
        post.setLikedBy(Collections.singletonList("uid-1"));
        post.setCreatedAt(1_000L);
        return post;
    }
}
