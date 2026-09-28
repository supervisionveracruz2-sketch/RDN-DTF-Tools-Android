package com.rdn.dtf.tools;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.net.Uri;
import android.content.Intent;
import android.content.ContentValues;
import android.provider.MediaStore;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import android.util.Base64;

import java.io.OutputStream;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_CHOOSER = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.addJavascriptInterface(new AndroidBridge(), "RDNAndroid");
        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }

                fileCallback = callback;

                try {
                    startActivityForResult(params.createIntent(), FILE_CHOOSER);
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    public class AndroidBridge {

        @JavascriptInterface
        public void saveBase64Png(String base64, String fileName) {
            try {
                byte[] data = Base64.decode(base64, Base64.DEFAULT);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    runOnUiThread(() ->
                            Toast.makeText(
                                    MainActivity.this,
                                    "Se requiere Android 10 o superior.",
                                    Toast.LENGTH_LONG
                            ).show()
                    );
                    return;
                }

                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "image/png");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                Uri uri = getContentResolver().insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                );

                if (uri == null) {
                    throw new Exception("No se pudo crear el archivo.");
                }

                OutputStream out = getContentResolver().openOutputStream(uri);

                if (out == null) {
                    throw new Exception("No se pudo abrir Descargas.");
                }

                out.write(data);
                out.flush();
                out.close();

                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);

                getContentResolver().update(uri, values, null, null);

                runOnUiThread(() ->
                        Toast.makeText(
                                MainActivity.this,
                                "PNG guardado en Descargas",
                                Toast.LENGTH_LONG
                        ).show()
                );

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(
                                MainActivity.this,
                                "Error al guardar PNG: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
            }
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER && fileCallback != null) {
            Uri[] results =
                    WebChromeClient.FileChooserParams.parseResult(
                            resultCode,
                            data
                    );

            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
