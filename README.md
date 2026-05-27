# SmartGym AI

Ứng dụng Android tích hợp AI phân tích sức khỏe, theo dõi thể trạng và đề xuất bài tập gym thông minh cá nhân hóa cho người dùng. Sử dụng DeepSeek AI và Google ML Kit Pose Detection để mang đến trải nghiệm huấn luyện viên ảo ngay trên điện thoại.

---

## Tải APK

<div align="center">

### [TẢI PHIÊN BẢN MỚI NHẤT (APK)](https://github.com/quy0358/smartgym-ai/releases/latest)

[![Download APK](https://img.shields.io/badge/Download-APK-brightgreen?style=for-the-badge&logo=android&logoColor=white)](https://github.com/quy0358/smartgym-ai/releases/latest)

</div>

> **Mẹo:** Bạn cũng có thể vào trang [Releases](https://github.com/quy0358/smartgym-ai/releases) để xem tất cả phiên bản và changelog chi tiết.

**Cách cài đặt:**

| Bước | Hướng dẫn |
|------|-----------|
| 1 | Tải file `.apk` từ link trên về điện thoại Android |
| 2 | Vào **Cài đặt → Bảo mật → Cho phép cài đặt từ nguồn không xác định** |
| 3 | Mở file APK vừa tải và nhấn **Cài đặt** |
| 4 | Yêu cầu **Android 7.0** (API 24) trở lên |

> **Lưu ý:** APK được build tự động qua GitHub Actions CI/CD mỗi khi có phiên bản mới. Nếu gặp cảnh báo "Ứng dụng không xác định", hãy chọn **Cài đặt vẫn tiếp tục** — đây là hành vi bình thường khi cài APK ngoài Google Play.

---

## Tính năng nổi bật

| Tính năng | Mô tả |
|-----------|--------|
| AI Coach | Phân tích sức khỏe, đề xuất bài tập & dinh dưỡng cá nhân hóa bằng DeepSeek AI |
| Nhận diện tư thế | Phát hiện và đánh giá tư thế tập luyện real-time qua Camera (ML Kit Pose Detection) |
| Theo dõi tiến trình | Biểu đồ trực quan theo dõi cân nặng, số buổi tập, calo tiêu hao |
| Quản lý dinh dưỡng | Lên kế hoạch bữa ăn, theo dõi calo và macro hàng ngày |
| Bài tập đa dạng | Thư viện bài tập gym phong phú với hướng dẫn chi tiết |
| Cộng đồng | Chia sẻ thành tích, tương tác với người tập khác |
| Nhắc lịch tập | Thông báo thông minh nhắc lịch tập theo thời gian biểu |
| Đăng nhập Google | Xác thực nhanh chóng qua Google Sign-In |

---

## Công nghệ sử dụng

- **Ngôn ngữ:** Java (Android)
- **Kiến trúc:** MVVM + Hilt Dependency Injection
- **AI:** DeepSeek API (phân tích & đề xuất)
- **Computer Vision:** Google ML Kit Pose Detection + CameraX
- **Backend:** Firebase (Auth, Firestore, Storage)
- **UI:** Material Design 3, ViewBinding, Navigation Component
- **Biểu đồ:** MPAndroidChart
- **Background:** WorkManager (nhắc lịch tập)
- **Build:** Gradle KTS, JDK 17
- **CI/CD:** GitHub Actions (tự động build APK + tạo Release)

---

## Thiết lập & Chạy local

### Yêu cầu
- Android Studio Hedgehog trở lên
- JDK 17
- Android SDK (API 36)

### Bước 1: Clone dự án

```bash
git clone https://github.com/quy0358/smartgym-ai.git
cd smartgym-ai
```

### Bước 2: Cấu hình `local.properties`

Tạo file `local.properties` ở root project:

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
DEEPSEEK_API_KEY=<debug-only-key>
```

> **Lưu ý:** `DEEPSEEK_API_KEY` chỉ nhúng vào debug build. Release build để trống key — cần gọi AI qua backend tin cậy hoặc Firebase Callable Function, không ship raw API key trong APK. Nếu thiếu key, app hiển thị thông báo cấu hình và dùng kế hoạch fallback thay vì crash.

### Bước 3: Mở trong Android Studio

1. **File → Open** → chọn thư mục project.
2. Đợi Gradle sync hoàn tất.
3. Kết nối thiết bị hoặc khởi tạo Emulator.
4. **Run → Run 'app'**.

---

## Firebase

- App sử dụng **Firebase Auth**, **Firestore** và **Storage**.
- Rules được version control tại `firestore.rules` và `storage.rules`.
- Deploy rules bằng Firebase CLI:

```bash
firebase deploy --only firestore:rules,storage
```

### Google Sign-In

`default_web_client_id` được Google Services plugin sinh từ `app/google-services.json`; không khai báo hardcoded trong `strings.xml`.

**Checklist:**

1. Bật provider Google trong Firebase Authentication.
2. Thêm SHA-1 và SHA-256 cho debug/release app `ntu.quy65132908.smartgym_ai` trong Firebase project settings.
3. Tải lại `app/google-services.json` sau khi Firebase tạo Android OAuth client và Web OAuth client.
4. Đảm bảo `google-services.json` có `oauth_client` loại Web client (`client_type: 3`) để Gradle sinh `@string/default_web_client_id`.
5. Nếu Google button báo lỗi cấu hình, kiểm tra lại package name, SHA fingerprints và OAuth consent screen.

---

## Kiểm tra

```bash
.\gradlew.bat lintDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

---

## Giấy phép

Dự án được phát triển bởi **Nguyễn Thanh Quy** — Đồ án môn Lập trình thiết bị di động, Trường Đại học Nha Trang.

---

## Đóng góp

Mọi đóng góp đều được hoan nghênh! Hãy tạo [Issue](https://github.com/quy0358/smartgym-ai/issues) hoặc [Pull Request](https://github.com/quy0358/smartgym-ai/pulls) nếu bạn muốn cải thiện ứng dụng.
