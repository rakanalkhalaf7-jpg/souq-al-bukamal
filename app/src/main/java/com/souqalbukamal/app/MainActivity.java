
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
import android.view.ViewGroup;
import android.util.Log;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

    private void showError(Throwable e) {
        Log.e("SouqCrash", "Application error", e);

        TextView errorView = new TextView(this);
        errorView.setTextColor(Color.RED);
        errorView.setBackgroundColor(Color.WHITE);
        errorView.setTextSize(16);
        errorView.setPadding(30, 50, 30, 50);

        errorView.setText(
            "خطأ في تشغيل سوق البوكمال\n\n" +
            e.getClass().getName() +
            "\n\n" +
            String.valueOf(e.getMessage())
        );

        setContentView(errorView);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            webView = new WebView(this);
            webView.setBackgroundColor(Color.WHITE);
            setContentView(webView);

            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setLoadsImagesAutomatically(true);
            settings.setAllowFileAccess(false);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);

            webView.setWebViewClient(new WebViewClient() {

                @Override
                public boolean shouldOverrideUrlLoading(
                        WebView view,
                        WebResourceRequest request) {
                    return handleUrl(request.getUrl().toString());
                }

                @Override
                public boolean shouldOverrideUrlLoading(
                        WebView view,
                        String url) {
                    return handleUrl(url);
                }

                private boolean handleUrl(String url) {
                    if (url.startsWith(
                            "https://souq-al-bukamal.pages.dev")) {
                        return false;
                    }

                    if (url.startsWith("https://wa.me/")
                            || url.startsWith("whatsapp://")
                            || url.startsWith("tel:")
                            || url.startsWith("mailto:")) {
                        try {
                            startActivity(new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(url)
                            ));
                        } catch (Exception e) {
                            Log.e("SouqCrash", "Link error", e);
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
                        Log.e(
                            "SouqCrash",
                            "Website loading error: " +
                            error.getDescription()
                        );
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
                        Intent intent = params.createIntent();
                        startActivityForResult(intent, 100);
                        return true;
                    } catch (Exception e) {
                        fileCallback = null;
                        callback.onReceiveValue(null);
                        Log.e("SouqCrash", "File chooser error", e);
                        return false;
                    }
                }
            });

            webView.loadUrl(
                "https://souq-al-bukamal.pages.dev"
            );

        } catch (Throwable e) {
            showError(e);
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && fileCallback != null) {
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
