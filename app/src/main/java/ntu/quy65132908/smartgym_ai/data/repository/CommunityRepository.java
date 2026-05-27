package ntu.quy65132908.smartgym_ai.data.repository;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import ntu.quy65132908.smartgym_ai.data.model.Post;

@Singleton
public class CommunityRepository {
    private static final int MAX_LOCAL_LIKES_PER_POST = 10000;

    private final FirebaseFirestore firestore;
    private ListenerRegistration listener;

    @Inject
    public CommunityRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public void listenToPosts(PostsCallback cb) {
        removeListener();
        listener = firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(50)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        cb.onError(err);
                        return;
                    }
                    if (snap != null) {
                        List<Post> posts = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snap) {
                            posts.add(mapPost(doc));
                        }
                        cb.onSuccess(posts);
                    }
                });
    }

    public void createPost(String authorId, String authorName, String content, SimpleCallback cb) {
        Map<String, Object> data = new HashMap<>();
        data.put("authorId", authorId);
        data.put("authorName", authorName);
        data.put("content", content);
        data.put("likes", 0);
        data.put("likedBy", new ArrayList<>());
        data.put("createdAt", FieldValue.serverTimestamp());
        firestore.collection("posts").add(data)
                .addOnSuccessListener(r -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void updatePostContent(String postId, String content, SimpleCallback cb) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("content", content);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        firestore.collection("posts").document(postId).update(updates)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void deletePost(String postId, SimpleCallback cb) {
        firestore.collection("posts").document(postId).delete()
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void toggleLike(String postId, String uid, SimpleCallback cb) {
        DocumentReference postRef = firestore.collection("posts").document(postId);
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(postRef);
                    if (!snapshot.exists()) {
                        throw new IllegalStateException("Post not found");
                    }

                    List<String> updatedLikedBy = readLikedBy(snapshot.get("likedBy"));
                    if (updatedLikedBy.contains(uid)) {
                        updatedLikedBy.remove(uid);
                    } else {
                        if (updatedLikedBy.size() >= MAX_LOCAL_LIKES_PER_POST) {
                            throw new IllegalStateException("Post like limit reached");
                        }
                        updatedLikedBy.add(uid);
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("likedBy", updatedLikedBy);
                    updates.put("likes", updatedLikedBy.size());
                    transaction.update(postRef, updates);
                    return null;
                })
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    static Post mapPost(DocumentSnapshot doc) {
        Post post = new Post();
        post.setId(doc.getId());
        post.setAuthorId(doc.getString("authorId"));
        post.setAuthorName(doc.getString("authorName"));
        post.setContent(doc.getString("content"));
        Long likes = doc.getLong("likes");
        post.setLikes(likes != null ? likes.intValue() : 0);
        post.setLikedBy(readLikedBy(doc.get("likedBy")));
        post.setCreatedAt(readCreatedAt(doc.get("createdAt")));
        post.setUpdatedAt(readCreatedAt(doc.get("updatedAt")));
        return post;
    }

    private static long readCreatedAt(Object rawCreatedAt) {
        if (rawCreatedAt instanceof Timestamp) {
            return ((Timestamp) rawCreatedAt).toDate().getTime();
        }
        if (rawCreatedAt instanceof Number) {
            return ((Number) rawCreatedAt).longValue();
        }
        return 0L;
    }

    private static List<String> readLikedBy(Object rawLikedBy) {
        List<String> likedBy = new ArrayList<>();
        if (!(rawLikedBy instanceof List<?>)) {
            return likedBy;
        }

        for (Object item : (List<?>) rawLikedBy) {
            if (item instanceof String) {
                likedBy.add((String) item);
            }
        }
        return likedBy;
    }

    public void removeListener() {
        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }

    public interface PostsCallback {
        void onSuccess(List<Post> posts);
        void onError(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }
}
