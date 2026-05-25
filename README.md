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

Checklist Google Sign-In:

1. Bật provider Google trong Firebase Authentication.
2. Thêm SHA-1 và SHA-256 cho debug/release app `ntu.quy65132908.smartgym_ai` trong Firebase project settings.
3. Tải lại `app/google-services.json` sau khi Firebase tạo Android OAuth client và Web OAuth client.
4. Đảm bảo `google-services.json` có `oauth_client` và `@string/default_web_client_id` không còn chứa `placeholder`.
5. Nếu Google button báo lỗi cấu hình, kiểm tra lại package name, SHA fingerprints và OAuth consent screen.

## Kiểm tra

```bash
.\gradlew.bat lintDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```
