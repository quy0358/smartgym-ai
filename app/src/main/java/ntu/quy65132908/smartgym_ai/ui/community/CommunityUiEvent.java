package ntu.quy65132908.smartgym_ai.ui.community;

public final class CommunityUiEvent {
    public enum Type {
        MESSAGE,
        POST_CREATED,
        POST_UPDATED,
        POST_DELETED
    }

    private final Type type;
    private final String message;

    private CommunityUiEvent(Type type, String message) {
        this.type = type;
        this.message = message;
    }

    public static CommunityUiEvent message(String message) {
        return new CommunityUiEvent(Type.MESSAGE, message);
    }

    public static CommunityUiEvent postCreated() {
        return new CommunityUiEvent(Type.POST_CREATED, null);
    }

    public static CommunityUiEvent postUpdated() {
        return new CommunityUiEvent(Type.POST_UPDATED, null);
    }

    public static CommunityUiEvent postDeleted() {
        return new CommunityUiEvent(Type.POST_DELETED, null);
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
