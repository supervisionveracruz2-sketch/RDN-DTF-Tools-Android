package com.rdn.dtf.tools;

import android.app.Activity;
import android.os.Bundle;
import android.net.Uri;
import android.content.Intent;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;

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
                    startActivityForResult(
                            params.createIntent(),
                            FILE_CHOOSER
                    );
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
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
