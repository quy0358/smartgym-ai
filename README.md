# smartgym-ai
Ứng dụng Android tích hợp AI phân tích sức khỏe, theo dõi thể trạng và đề xuất bài tập gym thông minh cá nhân hóa cho người dùng.

## Thiết lập local

1. Cài Android Studio/JDK 17 và đồng bộ Gradle.
2. Tạo `local.properties` ở root project:

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
DEEPSEEK_API_KEY=<debug-only-key>
```

`DEEPSEEK_API_KEY` chỉ được nhúng vào debug build. Release build để trống key và cần gọi AI qua backend tin cậy hoặc Firebase Callable Function, không ship raw API key trong APK. Nếu thiếu key, app phải hiển thị thông báo cấu hình và dùng kế hoạch fallback thay vì crash.

## Firebase

- App dùng Firebase Auth, Firestore và Storage.
- Rules được version control tại `firestore.rules` và `storage.rules`.
- Deploy rules bằng Firebase CLI sau khi chọn đúng project:

```bash
firebase deploy --only firestore:rules,storage
```

Google Sign-In cần OAuth Web Client thật từ Firebase/Google Cloud. `default_web_client_id` được Google Services plugin sinh từ `app/google-services.json`; không khai báo hardcoded trong `strings.xml` để tránh lệch project/flavor.

Checklist Google Sign-In:

1. Bật provider Google trong Firebase Authentication.
2. Thêm SHA-1 và SHA-256 cho debug/release app `ntu.quy65132908.smartgym_ai` trong Firebase project settings.
3. Tải lại `app/google-services.json` sau khi Firebase tạo Android OAuth client và Web OAuth client.
4. Đảm bảo `google-services.json` có `oauth_client` loại Web client (`client_type: 3`) để Gradle sinh `@string/default_web_client_id`.
5. Nếu Google button báo lỗi cấu hình, kiểm tra lại package name, SHA fingerprints và OAuth consent screen.

## Kiểm tra

```bash
.\gradlew.bat lintDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

## Ghi chú vận hành

- Nhắc lịch tập dùng WorkManager dạng one-time work tự đặt lịch lại. Cơ chế này tiết kiệm pin và phù hợp nhắc tương đối, nhưng có thể trễ khi thiết bị vào Doze. Nếu sản phẩm yêu cầu báo đúng phút tuyệt đối, cần thiết kế riêng bằng AlarmManager exact alarm và xử lý quyền hệ thống tương ứng.
- Community hiện lưu `likedBy` trong document bài viết để đơn giản hóa rules và UI. Nếu số lượng người dùng tăng lớn, nên chuyển sang subcollection likes hoặc Cloud Function aggregate counter để tránh document phình to.
