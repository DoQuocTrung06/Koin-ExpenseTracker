# KOIN - Smart Financial Management & Group Fund Application

**Course:** Mobile Application Development (Phát Triển Ứng Dụng Di Động)  
**Institution:** Ton Duc Thang University (TDTU)  
**Topic:** **Đề tài 14 - Nhật ký thu chi hằng ngày, tìm kiếm và tổng hợp theo tuần/tháng. Quản lý chi phí, hóa đơn, giao dịch định kỳ và xuất báo cáo.**  

**Students:**  
- **Đỗ Quốc Trung** (524H0132) - Leader  
- **Trương Cao Sang** (524H0125) - Member  
- **Đỗ Quốc Việt** (524H0137) - Member  

---

## 1. Topic 14 Key Implementations

Koin is built on native Android with **Clean MVVM**, **Repository Pattern**, and a **Supabase Cloud (PostgreSQL)** BaaS backend. The project strictly satisfies all requirements of **Topic 14**:
- **Daily Journal & Multi-tier Filters:** Real-time chronological transaction logging, fast in-memory search, and dynamic aggregation (Today, This Week, This Month, Custom Date Ranges) with interactive category charts.
- **Expense & Bill Receipt Management:** Camera/Gallery receipt image capture with client-side compression stored in Supabase Storage (`receipts` bucket).
- **Scheduled Recurring Transactions:** Automated recurring expense manager (rent, utilities) with deterministic idempotency keys (`UUID(rule_id, cycle_key)`) preventing duplicate billing.
- **Financial Report Export:** Dynamic HTML5/CSS3 template generation with Android `PrintManager` spooling for PDF creation and multi-format sharing (HTML, XLS, CSV via `FileProvider`).
- **Financial Concurrency Safety:** Server-side atomic RPC functions with Pessimistic Locking (`SELECT ... FOR UPDATE`) and client-side Compare-And-Set (CAS) to eliminate double-spending.

---

## 2. Tech Stack

- **Platform & Language:** Native Android, Java 11 (`minSdk 26`, `targetSdk 37`, AGP `9.3.3`).
- **Architecture:** Pragmatic MVVM + Repository Pattern (AndroidX `ViewModel` & `LiveData 2.8.6`).
- **Backend (BaaS):** Supabase Cloud (PostgreSQL 15, PostgREST RESTful API, S3 Object Storage, GoTrue Auth).
- **Realtime:** Persistent Full-Duplex WebSockets (`Java-WebSocket 1.5.7`) subscribing to PostgreSQL CDC write-ahead logs.
- **Networking & Utilities:** `OkHttp 4.12.0`, `Gson 2.10.1`, `ZXing 4.3.0` (VietQR Banking).

---

## 3. Setup & Test Accounts

### Quick Setup:
```bash
git clone https://github.com/DoQuocTrung06/Koin-ExpenseTracker.git
```
Open the project in Android Studio, ensure **Gradle JDK** is set to **JDK 17+**, and run on an emulator or physical device (`minSdk 26+`).

### Pre-configured Accounts:

| Role | Email | Password | Description |
| :--- | :--- | :--- | :--- |
| **Owner (Chủ quỹ)** | `doquoctrung2k@gmail.com` | `T@123456789a` | Creator of "Quy Ca Nhan", holds full approval controls |
| **Treasurer (Phó quỹ)** | `doquocviet2k@gmail.com` | `V@123456` | Appointed treasurer with delegated approval rights |
| **Member (Thành viên)** | `caosang3112@gmail.com` | `S@123456` | General member account for payments and join requests |

---

## 4. Topic 14 Feature Test Scenarios

1. **Daily Journal & Weekly/Monthly Filters:** Open "Quy Ca Nhan" > Go to History Tab > Toggle **"Tuần này"** / **"Tháng này"** to view real-time totals and category charts. Use the search bar for instant accent-insensitive lookups.
2. **Expense & Receipt Upload:** Create an expense > Attach receipt image via camera/gallery > Confirm upload to Supabase Storage and inspect transaction detail.
3. **CAS Concurrency Expense Approval:** Member requests expense > Owner approves via `approve_expense_transaction` RPC > Verifies optimistic CAS row lock preventing negative fund balances.
4. **Recurring Transactions:** Open "Giao dịch định kỳ" > Review rent/utility rules > Tap "Thanh toán (Pay)" to execute with deterministic UUID idempotency (rejects duplicate triggers).
5. **Statement & Report Export:** Open History > Tap **"Xuất báo cáo"** > Preview statement > Download HTML/XLS/CSV or Share via Android Print framework / PDF.
