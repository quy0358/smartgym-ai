# Phương pháp Superpowers
## Kiểm tra Skill bắt buộc
Trước khi đưa ra BẤT KỲ phản hồi nào, đánh giá yêu cầu của người dùng với TẤT CẢ các skill có sẵn trong `.roo/skills/`. Nếu có skill phù hợp, tải và tuân theo nó.
## Các Skill có sẵn
| Skill | Khi nào sử dụng | File |
|-------|-----------------|------|
| **brainstorming** | Trước BẤT KỲ công việc sáng tạo nào - tạo tính năng, xây dựng component, thêm chức năng | `.roo/skills/brainstorming/SKILL.md` |
| **writing-plans** | Khi bạn có spec/yêu cầu cho task nhiều bước, trước khi chạm vào code | `.roo/skills/writing-plans/SKILL.md` |
| **test-driven-development** | Khi triển khai bất kỳ tính năng hoặc sửa lỗi nào, trước khi viết code triển khai | `.roo/skills/test-driven-development/SKILL.md` |
| **systematic-debugging** | Khi gặp bất kỳ bug, test thất bại, hoặc hành vi không mong đợi nào | `.roo/skills/systematic-debugging/SKILL.md` |
| **verification-before-completion** | Trước khi tuyên bố công việc hoàn thành/đã sửa/đã pass | `.roo/skills/verification-before-completion/SKILL.md` |
| **executing-plans** | Khi bạn có kế hoạch triển khai đã được viết sẵn để thực thi | `.roo/skills/executing-plans/SKILL.md` |
| **subagent-driven-development** | Khi thực thi kế hoạch triển khai với các task độc lập | `.roo/skills/subagent-driven-development/SKILL.md` |
| **dispatching-parallel-agents** | Khi đối mặt với 2+ task độc lập có thể làm việc mà không cần chia sẻ trạng thái | `.roo/skills/dispatching-parallel-agents/SKILL.md` |
| **requesting-code-review** | Khi hoàn thành task hoặc trước khi merge để xác minh công việc đáp ứng yêu cầu | `.roo/skills/requesting-code-review/SKILL.md` |
| **receiving-code-review** | Khi nhận phản hồi code review | `.roo/skills/receiving-code-review/SKILL.md` |
| **using-git-worktrees** | Khi bắt đầu công việc tính năng cần cách ly | `.roo/skills/using-git-worktrees/SKILL.md` |
| **finishing-a-development-branch** | Khi triển khai hoàn tất, tất cả test đều pass | `.roo/skills/finishing-a-development-branch/SKILL.md` |
| **writing-skills** | Khi tạo skill mới hoặc chỉnh sửa skill hiện có | `.roo/skills/writing-skills/SKILL.md` |
## Quy trình cốt lõi
1. **Brainstorming** → Thiết kế trước khi code (CỔNG CỨNG: không code khi chưa có thiết kế được phê duyệt)
2. **Writing Plans** → Chia nhỏ thành các task TDD vừa miệng (2-5 phút mỗi task)
3. **TDD Implementation** → ĐỎ → xác minh thất bại → XANH → xác minh pass → TÁI CẤU TRÚC → commit
4. **Verification** → Bằng chứng trước khi khẳng định
5. **Code Review** → Trước khi đánh dấu hoàn thành
## Iron Laws
- **KHÔNG có code production khi chưa có test thất bại trước** (TDD)
- **KHÔNG sửa lỗi khi chưa điều tra nguyên nhân gốc trước** (Debugging)
- **KHÔNG code khi chưa có thiết kế được phê duyệt trước** (Brainstorming)
- **KHÔNG tuyên bố thành công khi chưa chạy xác minh** (Verification)
## Triết lý
- **Phát triển hướng kiểm thử (TDD)** - Luôn viết test trước
- **Có hệ thống thay vì tùy hứng** - Quy trình thay vì đoán mò
- **Giảm độ phức tạp** - Đơn giản là mục tiêu chính
- **Bằng chứng thay vì tuyên bố** - Xác minh trước khi tuyên bố thành công
- **YAGNI** - Bạn sẽ không cần nó đâu (You Aren't Gonna Need It)
- **DRY** - Đừng lặp lại chính mình (Don't Repeat Yourself)