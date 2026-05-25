package ntu.quy65132908.smartgym_ai.data.repository;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Test;

import java.util.Collections;

import ntu.quy65132908.smartgym_ai.data.model.Post;

public class CommunityRepositoryTest {

    @Test
    public void mapPost_readsLegacyNumericCreatedAt() {
        DocumentSnapshot snapshot = snapshotWithCreatedAt(1_234L);

        Post post = CommunityRepository.mapPost(snapshot);

        assertEquals("p1", post.getId());
        assertEquals(1_234L, post.getCreatedAt());
    }

    @Test
    public void mapPost_readsServerTimestampCreatedAt() {
        DocumentSnapshot snapshot = snapshotWithCreatedAt(new Timestamp(2L, 500_000_000));

        Post post = CommunityRepository.mapPost(snapshot);

        assertEquals(2_500L, post.getCreatedAt());
    }

    private static DocumentSnapshot snapshotWithCreatedAt(Object createdAt) {
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(snapshot.getId()).thenReturn("p1");
        when(snapshot.getString("authorId")).thenReturn("uid-1");
        when(snapshot.getString("authorName")).thenReturn("Quy");
        when(snapshot.getString("content")).thenReturn("content");
        when(snapshot.getLong("likes")).thenReturn(2L);
        when(snapshot.get("likedBy")).thenReturn(Collections.singletonList("uid-2"));
        when(snapshot.get("createdAt")).thenReturn(createdAt);
        return snapshot;
    }
}
