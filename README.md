# smartgym-ai
Ứng dụng Android tích hợp AI phân tích sức khỏe, theo dõi thể trạng và đề xuất bài tập gym thông minh cá nhân hóa cho người dùng.

## Thiết lập local

1. Cài Android Studio/JDK 17 và đồng bộ Gradle.
2. Tạo `local.properties` ở root project:

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
DEEPSEEK_API_KEY=<debug-only-key>
```

`DEEPSEEK_API_KEY` chỉ được nhúng vào debug build. Release build để trống key và cần gọi AI qua backend tin cậy hoặc Firebase Callable Function, không ship raw API key trong APK.

## Firebase

- App dùng Firebase Auth, Firestore và Storage.
- Rules được version control tại `firestore.rules` và `storage.rules`.
- Deploy rules bằng Firebase CLI sau khi chọn đúng project:

```bash
firebase deploy --only firestore:rules,storage
```

Google Sign-In cần OAuth Web Client thật từ Firebase/Google Cloud. Giá trị placeholder trong `strings.xml` phải được thay bằng `default_web_client_id` do Firebase tạo ra trước khi kiểm thử Google Sign-In.

## Kiểm tra

```bash
.\gradlew.bat lintDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```
