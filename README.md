# KOIN - Smart Financial Management & Group Fund Application

**Course:** Mobile Application Development  
**Institution:** Ton Duc Thang University (TDTU)  
**Topic:** **Topic 14 - Daily income and expense log, weekly/monthly search and aggregation. Expense and invoice management, recurring transactions, and report export.**  

**Students:**  
- **Đỗ Quốc Trung** (524H0132)  
- **Trương Cao Sang** (524H0125)  
- **Đỗ Quốc Việt** (524H0137)  

---

## 1. Topic 14 Key Implementations

Koin is built as a native Android application using **Clean MVVM**, the **Repository Pattern**, and a **Supabase Cloud (PostgreSQL)** BaaS backend. The project strictly satisfies all requirements of **Topic 14**:
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

## 3. Step-by-Step Setup & Run Guide

### Step 1: Clone the Project
Open Terminal / Command Prompt and run:
```bash
git clone https://github.com/DoQuocTrung06/Koin-ExpenseTracker.git
```
*(Alternatively, click the green **Code** button on GitHub > select **Download ZIP**, and extract it into a local folder).*

### Step 2: Open in Android Studio
1. Launch **Android Studio** (Koala, Ladybug, or newer recommended).
2. On the Welcome screen, click **Open** (or go to menu **File > Open**).
3. Navigate to and select the extracted `Koin-ExpenseTracker` directory, then click **OK**.
4. Allow Android Studio to automatically download dependencies and complete the initial Gradle Sync.

### Step 3: Configure Java JDK (Important - Prevent Build Errors)
Ensure the Gradle build uses JDK 17:
1. Open **Settings** (Windows/Linux: `Ctrl + Alt + S` or `File > Settings`; macOS: `Android Studio > Settings`).
2. In the left sidebar, navigate to: **Build, Execution, Deployment > Build Tools > Gradle**.
3. Under **Gradle JDK**, select **Embedded JDK (version 17)** or any installed JDK 17+.
4. Click **Apply > OK**, then click the **Sync Project with Gradle Files** icon (elephant with blue sync arrow) in the top-right toolbar.

### Step 4: Prepare a Target Device
Ensure your computer has an active Internet connection to communicate with the Supabase Cloud backend, then choose either:
- **Option A (Android Emulator):** Open **Device Manager** in Android Studio > Launch a virtual device running **Android 8.0 (API 26)** up to **Android 14/15 (API 34/35)**.
- **Option B (Physical Device):** Enable **Developer Options** and **USB Debugging** on your phone > Connect the phone to your computer via USB cable.

### Step 5: Build and Run the App
1. In the top toolbar, ensure the run configuration is set to `app` and your target device is selected.
2. Click the green **Run** button (or press `Shift + F10`).
3. The APK will be compiled, installed, and launched directly to the Login screen.

---

## 4. Pre-configured Test Accounts

| Account | Email | Password | Role Description |
| :--- | :--- | :--- | :--- |
| **Đỗ Quốc Trung** | `doquoctrung2k@gmail.com` | `T@123456789a` | Fund Owner of "Quy Ca Nhan", holds full approval & admin rights |
| **Đỗ Quốc Việt** | `doquocviet2k@gmail.com` | `V@123456` | Fund Treasurer, delegated expense approval and payment execution |
| **Trương Cao Sang** | `caosang3112@gmail.com` | `S@123456` | Fund Member, requests expenses and submits fund join requests |

---

## 5. Topic 14 Feature Test Scenarios

1. **Daily Journal & Weekly/Monthly Filters:** Open "Quy Ca Nhan" > Go to History Tab > Toggle **"Tuần này" (This Week)** / **"Tháng này" (This Month)** to view real-time totals and category charts. Use the search bar for instant accent-insensitive lookups.
2. **Expense & Receipt Upload:** Create an expense > Attach receipt image via camera/gallery > Confirm upload to Supabase Storage and inspect transaction detail.
3. **CAS Concurrency Expense Approval:** Member requests expense > Manager approves via `approve_expense_transaction` RPC > Verifies optimistic CAS row lock preventing negative fund balances.
4. **Recurring Transactions:** Open "Giao dịch định kỳ" > Review rent/utility rules > Tap "Thanh toán (Pay)" to execute with deterministic UUID idempotency (rejects duplicate triggers).
5. **Statement & Report Export:** Open History > Tap **"Xuất báo cáo"** > Preview statement > Download HTML/XLS/CSV or Share via Android Print framework / PDF.
