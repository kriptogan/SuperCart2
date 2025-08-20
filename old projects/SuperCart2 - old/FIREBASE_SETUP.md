# Firebase Setup Guide for SuperCart2

## Project Preparation ✅
The project has been prepared with:
- Firebase dependencies added to build.gradle.kts files
- Google Services plugin configured
- FirebaseManager class created
- Configuration placeholders set up

## Step-by-Step Firebase Console Setup

### 1. Go to Firebase Console
1. Open your web browser
2. Navigate to: https://console.firebase.google.com/
3. Sign in with your Google account

### 2. Create New Project
1. Click "Create a project" or "Add project"
2. Enter project name: `SuperCart2` (or your preferred name)
3. Click "Continue"
4. **Disable Google Analytics** (optional - you can enable later)
5. Click "Create project"

### 3. Add Android App
1. Click the Android icon (</>) to add an Android app
2. Enter Android package name: `com.kriptogan.supercart2`
3. Enter app nickname: `SuperCart2` (optional)
4. **Skip** SHA-1 certificate for now (we'll add this later if needed)
5. Click "Register app"

### 4. Download Configuration File
1. Download the `google-services.json` file
2. **Place this file in the `app/` directory** of your project
3. Click "Next"

### 5. Add Firebase SDK
1. The console will show you the next steps
2. Click "Next" (we've already added the dependencies)
3. Click "Continue to console"

### 6. Enable Firestore Database
1. In the left sidebar, click "Firestore Database"
2. Click "Create database"
3. Choose "Start in test mode" (we'll add security rules later)
4. Select a location (choose closest to your users)
5. Click "Enable"

### 7. Set Up Authentication (Optional)
1. In the left sidebar, click "Authentication"
2. Click "Get started"
3. Click "Email/Password"
4. Enable it and click "Save"

## Project Integration

### 1. Place google-services.json
- Copy the downloaded `google-services.json` file to your project's `app/` directory
- Make sure it's at the same level as your `app/build.gradle.kts` file

### 2. Sync Project
- In Android Studio, click "Sync Now" when prompted
- Or go to File → Sync Project with Gradle Files

### 3. Test Connection
- Build and run your app
- Check logcat for Firebase connection messages

## Security Rules (Later)
After testing, you'll want to add proper Firestore security rules:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Add your security rules here
  }
}
```

## Next Steps
1. Complete the Firebase Console setup above
2. Download and place `google-services.json` in the app directory
3. Sync your project
4. Test the Firebase connection
5. Start implementing Firebase operations in your app

## Troubleshooting
- If you get build errors, make sure `google-services.json` is in the correct location
- Check that all dependencies are properly synced
- Verify your package name matches exactly in Firebase Console
