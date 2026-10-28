package com.clipcollage.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.core.content.FileProvider;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Native helpers the web app cannot do from inside a WebView:
 * copy an image to the clipboard, read an image from the clipboard,
 * save to the Photos gallery, and open the share sheet.
 */
@CapacitorPlugin(name = "CollageNative")
public class CollageNativePlugin extends Plugin {

    private static String safeName(String name, String fallback) {
        if (name == null || name.trim().isEmpty()) return fallback;
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static byte[] decode(PluginCall call) throws Exception {
        String data = call.getString("data");
        if (data == null || data.isEmpty()) throw new Exception("No image data received");
        return Base64.decode(data, Base64.DEFAULT);
    }

    private Uri cacheUri(PluginCall call, byte[] bytes) throws Exception {
        File dir = new File(getContext().getCacheDir(), "shared");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("Cannot create the cache folder");
        File file = new File(dir, safeName(call.getString("name", "collage.png"), "collage.png"));
        FileOutputStream os = new FileOutputStream(file);
        try {
            os.write(bytes);
        } finally {
            os.close();
        }
        return FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", file);
    }

    private static String message(Exception e, String fallback) {
        String m = e.getMessage();
        return (m == null || m.isEmpty()) ? fallback : m;
    }

    @PluginMethod
    public void copyImage(final PluginCall call) {
        try {
            byte[] bytes = decode(call);
            final Uri uri = cacheUri(call, bytes);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newUri(getContext().getContentResolver(), "Collage", uri);
                        cm.setPrimaryClip(clip);
                        call.resolve();
                    } catch (Exception e) {
                        call.reject(message(e, "Could not copy the image"));
                    }
                }
            });
        } catch (Exception e) {
            call.reject(message(e, "Could not copy the image"));
        }
    }

    @PluginMethod
    public void readImage(final PluginCall call) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = cm.getPrimaryClip();
                    if (clip == null || clip.getItemCount() == 0) {
                        call.reject("The clipboard is empty");
                        return;
                    }
                    Uri found = null;
                    for (int i = 0; i < clip.getItemCount(); i++) {
                        Uri u = clip.getItemAt(i).getUri();
                        if (u != null) {
                            found = u;
                            break;
                        }
                    }
                    if (found == null) {
                        call.reject("No image on the clipboard");
                        return;
                    }
                    final Uri uri = found;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ContentResolver cr = getContext().getContentResolver();
                                String type = cr.getType(uri);
                                if (type != null && !type.startsWith("image/")) {
                                    call.reject("The clipboard does not hold an image");
                                    return;
                                }
                                InputStream is = cr.openInputStream(uri);
                                if (is == null) {
                                    call.reject("Could not open the copied image");
                                    return;
                                }
                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                try {
                                    byte[] buf = new byte[16384];
                                    int n;
                                    while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
                                } finally {
                                    is.close();
                                }
                                JSObject ret = new JSObject();
                                ret.put("data", Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP));
                                ret.put("mime", type == null ? "image/png" : type);
                                call.resolve(ret);
                            } catch (Exception e) {
                                call.reject(message(e, "Could not read the copied image"));
                            }
                        }
                    }).start();
                } catch (Exception e) {
                    call.reject(message(e, "Could not read the clipboard"));
                }
            }
        });
    }

    @PluginMethod
    public void saveToGallery(PluginCall call) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                call.reject("Saving to Photos needs Android 10 or newer. Use Share instead.");
                return;
            }
            byte[] bytes = decode(call);
            String name = safeName(call.getString("name", "collage.png"), "collage.png");
            String mime = call.getString("mime", "image/png");
            ContentResolver cr = getContext().getContentResolver();
            ContentValues v = new ContentValues();
            v.put(MediaStore.Images.Media.DISPLAY_NAME, name);
            v.put(MediaStore.Images.Media.MIME_TYPE, mime);
            v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Clipboard Collage");
            v.put(MediaStore.Images.Media.IS_PENDING, 1);
            Uri uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (uri == null) {
                call.reject("Could not create the image in Photos");
                return;
            }
            OutputStream os = cr.openOutputStream(uri);
            if (os == null) {
                call.reject("Could not write the image");
                return;
            }
            try {
                os.write(bytes);
            } finally {
                os.close();
            }
            ContentValues done = new ContentValues();
            done.put(MediaStore.Images.Media.IS_PENDING, 0);
            cr.update(uri, done, null, null);
            call.resolve();
        } catch (Exception e) {
            call.reject(message(e, "Could not save the image"));
        }
    }

    @PluginMethod
    public void shareImage(PluginCall call) {
        try {
            byte[] bytes = decode(call);
            Uri uri = cacheUri(call, bytes);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType(call.getString("mime", "image/png"));
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(send, "Share collage");
            getActivity().startActivity(chooser);
            call.resolve();
        } catch (Exception e) {
            call.reject(message(e, "Could not open the share sheet"));
        }
    }
}
