# Clipboard Collage: build the APK

This folder builds an installable Android app (APK) in the cloud. You do not need Android Studio.

## Steps (about 10 minutes, once)

1. Sign in at github.com and create a new repository (private is fine). Name it anything, for example `clipboard-collage`.
2. Upload everything in this folder to the repository, keeping the folders as they are.
   The hidden folder `.github` must go too, because it holds the build recipe.
   Easiest way: on a laptop, open the new repository, choose **Add file > Upload files**, then drag the contents of this folder in and commit.
   If `.github/workflows/build-apk.yml` did not upload, choose **Add file > Create new file**, type that exact path as the file name, and paste in the contents of `build-apk.yml`.
3. Open the **Actions** tab, choose **Build APK** on the left, then **Run workflow**. The build takes about 5 to 8 minutes.
4. When it shows a green tick, open the run and download **Clipboard-Collage-apk** at the bottom (a zip containing `Clipboard-Collage.apk`).
5. Copy the APK to your phone, open it, and allow **Install unknown apps** when Android asks.

If the run fails, open it, copy the red error lines, and send them to me.

## What the app does

- Paste: tap **Paste** to take the image you copied.
- Copy collage: puts the finished collage on the clipboard so you can paste it into WhatsApp, Gmail and other apps.
- Download: saves into Photos, in the **Clipboard Collage** album (Android 10 and newer).
- Share or save: opens the Android share sheet.

The app works offline. The fonts download when you are online; otherwise it uses your phone's default font.
