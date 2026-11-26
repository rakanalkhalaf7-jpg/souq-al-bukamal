
package com.souq.albukamal;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.widget.TextView;
import android.util.Log;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private static final String HOME =
            "https://souq-al-bukamal.pages.dev/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setAllowFileAccess(false);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, String url) {
                return handleUrl(url);
            }

            private boolean handleUrl(String url) {
                if (url.startsWith(HOME)
                        || url.startsWith("https://souq-al-bukamal.pages.dev/")) {
                    return false;
                }

                if (url.startsWith("https://wa.me/")
                        || url.startsWith("whatsapp://")
                        || url.startsWith("tel:")
                        || url.startsWith("mailto:")) {
                    try {
                        startActivity(new Intent(
                                Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception e) {
                        Log.e("Souq", "External link error", e);
                    }
                    return true;
                }

                return false;
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error) {
                if (request.isForMainFrame()) {
                    Log.e("Souq", "Loading error: "
                            + error.getDescription());
                    showRetry();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }

                fileCallback = callback;

                try {
                    startActivityForResult(
                            params.createIntent(), 100);
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    callback.onReceiveValue(null);
                    return false;
                }
            }
        });

        webView.loadUrl(HOME);
    }

    private void showRetry() {
        TextView error = new TextView(this);
        error.setText("تعذر الاتصال بالإنترنت.\n\nاضغط هنا لإعادة المحاولة");
        error.setTextColor(Color.BLACK);
        error.setTextSize(18);
        error.setGravity(17);
        error.setBackgroundColor(Color.WHITE);
        error.setOnClickListener(v -> {
            webView.clearCache(false);
            webView.loadUrl(HOME);
        });
        setContentView(error);
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && fileCallback != null) {
            Uri[] results =
                    WebChromeClient.FileChooserParams.parseResult(
                            resultCode, data);
            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (fileCallback != null) {
            fileCallback.onReceiveValue(null);
            fileCallback = null;
        }

        if (webView != null) {
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
