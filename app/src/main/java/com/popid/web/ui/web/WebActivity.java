package com.popid.web.ui.web;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.util.Log;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WebActivity extends Activity {
    private ValueCallback<Uri[]> fileChooserCallback;
    private String mCameraPhotoPath;
    private static final String TAG = WebActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri home = Uri.parse("https://www.poppay.ae?ref=carrefour");

        WebView view = new WebView(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        WebSettings settings = view.getSettings();
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setJavaScriptEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSupportMultipleWindows(true);

        view.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView vw, WebResourceRequest request) {
                if (request.getUrl().toString().contains(home.getHost())) {
                    vw.loadUrl(request.getUrl().toString());
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                    vw.getContext().startActivity(intent);
                }

                return true;
            }
        });
        view.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }

            @Override
            public boolean onShowFileChooser(WebView vw, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (fileChooserCallback != null) {
                    fileChooserCallback.onReceiveValue(null);
                }
                fileChooserCallback = filePathCallback;

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                        Log.e(TAG, "Unable to create Image File", ex);
                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                        Log.d(TAG, "Camera Path:"+ mCameraPhotoPath);
                        Uri photoURI = FileProvider.getUriForFile(WebActivity.this, "com.popid.web.fileprovider", photoFile);
                        Log.d(TAG, "photoURI: "+ photoURI);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    } else {
                        takePictureIntent = null;
                        Log.d(TAG, "PhotoFile null");
                    }
                }

                Intent selectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                selectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                selectionIntent.setType("image/*");
                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, selectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(chooserIntent, 0);

                return true;
            }
        });
        view.setOnKeyListener((v, keyCode, event) ->
        {
            WebView vw = (WebView) v;
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK && vw.canGoBack()) {
                vw.goBack();

                return true;
            }

            return false;
        });
//        view.setDownloadListener((uri, userAgent, contentDisposition, mimetype, contentLength) -> handleURI(uri));
//        view.setOnLongClickListener(v -> {
//            handleURI(((WebView) v).getHitTestResult().getExtra());
//
//            return true;
//        });

        view.loadUrl(home.toString());

        setContentView(view);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (intent == null) {
                // If there is not data, then we may have taken a photo
                Log.d(TAG, "OnActivityResult: mCameraPhotoPath: "+ mCameraPhotoPath);
                Log.d(TAG, "OnActivityResult: mCameraPhotoPath URI: "+ Uri.parse(mCameraPhotoPath));
                if (mCameraPhotoPath != null) {
                    results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                }
            } else {
                String dataString = intent.getDataString();
                Log.d(TAG, "OnActivityResult: dataString "+ dataString);

                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        } else {
            Log.d(TAG, "ACTIVITY Result NOT OK-" + resultCode);
        }
        fileChooserCallback.onReceiveValue(results);
        fileChooserCallback = null;
    }

//    private void handleURI(String uri) {
//        Log.d(TAG, "Handle URI: "+ uri);
//        if (uri != null) {
//            Intent i = new Intent(Intent.ACTION_VIEW);
//            i.setData(Uri.parse(uri.replaceFirst("^blob:", "")));
//            startActivity(i);
//        }
//    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = new File(storageDir, imageFileName + ".jpg");
        Log.d(TAG, "ImageFile:" +imageFile.getAbsolutePath());
        return imageFile;
    }
}