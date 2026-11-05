# KOIN - Group Fund Management Application

Course: Mobile Application Development  
Institution: Ton Duc Thang University (TDTU)  
Students:  
- Do Quoc Trung (524H0132) - Leader  
- Truong Cao Sang (524H0125) - Member  

---

## 1. System Requirements

Ensure your computer has the following tools installed:
- Android Studio (Iguana, Jellyfish, Koala, Ladybug, or newer).
- JDK 17 (bundled inside Android Studio).
- Android SDK with API 26 (minimum) up to API 34/37.
- Android Virtual Device (AVD) running Android 13 (API 33) or Android 14 (API 34), or a physical Android phone with USB Debugging enabled.

---

## 2. Step-by-Step Setup & Run Guide

### Step 1: Clone the Project
Open Terminal / Command Prompt and run:
```bash
git clone https://github.com/DoQuocTrung06/SharedFund-App.git
```
*(Or click the green "Code" button on GitHub > "Download ZIP" and extract the folder).*

### Step 2: Open in Android Studio
1. Open Android Studio.
2. Click "Open" and select the `SharedFund-App` folder.
3. Wait for the initial Gradle Sync to complete.

### Step 3: Verify Gradle JDK (Important)
If Gradle Sync shows a Java error:
1. Go to "Settings" > "Build, Execution, Deployment" > "Build Tools" > "Gradle".
2. Set "Gradle JDK" to "Embedded JDK 17" (or version 17+).
3. Click "Sync Project with Gradle Files".

### Step 4: Run the Application
1. In Device Manager, launch your emulator (or connect your physical Android phone).
2. Click the green "Run" button (or press `Shift + F10`).
3. The app will install and open directly to the Login screen.

---

## 3. Pre-configured Test Accounts

You can sign in immediately using any of the following pre-created accounts:

| Account | Email | Password |
| :--- | :--- | :--- |
| Account 1 | `doquoctrung2k@gmail.com` | `123456` |
| Account 2 | `doquocviet2k@gmail.com` | `V@123456` |
| Account 3 | `phamquoccuong@gmail.com` | `C@123456` |

---

## 4. Key Feature Test Scenarios

### Scenario 1: Login & Fund Overview
1. Sign in using any test account above.
2. On the Home screen, tap on any group fund to view current balance, member list, and transaction history.

### Scenario 2: VietQR Banking & Image Picker
1. In any fund room, tap "Chuyển tiền" (Transfer).
2. Select the "Quét QR" tab.
3. Choose either:
   - "QUÉT CAMERA": Point the camera at any Vietnamese banking VietQR code.
   - "CHỌN ẢNH QR": Select a QR code image from the device gallery. A sample image (`sample_vietqr.png`) is provided in the project root directory; simply drag-and-drop it into the Android Emulator.
4. The system automatically decodes Bank BIN, Account Number, and Recipient Name.
5. Enter amount and note > Tap "Tiếp tục" > Review confirmation card > Confirm to see the result card with transaction ID `#SF-...`.

### Scenario 3: Expense Approval & Rejection Workflow
1. A member submits a fund withdrawal request.
2. The fund manager sees an amber banner for pending approval requests.
3. Tap the item to "Approve" (deducts room balance) or "Reject" (enter reason to cancel request).

### Scenario 4: Room Background & Role Delegation
1. In any fund room, tap the cover image icon to select a new room background image.
2. Go to the "Thành viên" (Members) tab to promote members to Treasurer or transfer Room Ownership.

---

## 5. Troubleshooting

- Gradle Sync Error: Make sure your Gradle JDK in Android Studio Settings is set to JDK 17 (do not use JDK 8).
- Camera on Emulator: If the emulator shows a virtual scene, use "CHỌN ẢNH QR" with the provided `sample_vietqr.png`, or open emulator Extended Controls > Camera > select your computer webcam.
