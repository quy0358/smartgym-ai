package ntu.quy65132908.smartgym_ai.ui.community;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;

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

    private static Post post(String id, String authorName, int likes) {
        Post post = new Post();
        post.setId(id);
        post.setAuthorName(authorName);
        post.setContent("content");
        post.setLikes(likes);
        post.setLikedBy(Collections.singletonList("uid-1"));
        post.setCreatedAt(1_000L);
        return post;
    }
}
