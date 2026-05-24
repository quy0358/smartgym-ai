package ntu.quy65132908.smartgym_ai.data.repository;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
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
                    if (err != null) { cb.onError(err); return; }
                    if (snap != null) {
                        List<Post> posts = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snap) {
                            Post p = doc.toObject(Post.class);
                            p.setId(doc.getId());
                            posts.add(p);
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
        data.put("createdAt", System.currentTimeMillis());
        firestore.collection("posts").add(data)
                .addOnSuccessListener(r -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void toggleLike(String postId, String uid, boolean isLiked, SimpleCallback cb) {
        DocumentReference postRef = firestore.collection("posts").document(postId);
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(postRef);
                    List<String> updatedLikedBy = readLikedBy(snapshot);

                    if (isLiked) {
                        updatedLikedBy.remove(uid);
                    } else if (!updatedLikedBy.contains(uid)) {
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

    private List<String> readLikedBy(DocumentSnapshot snapshot) {
        Object rawLikedBy = snapshot.get("likedBy");
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
        if (listener != null) { listener.remove(); listener = null; }
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
