
package com.souqalbukamal.app;

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
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

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
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

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
                    if (url.startsWith("https://souq-al-bukamal.pages.dev")) {
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
                            Toast.makeText(
                                MainActivity.this,
                                "تعذر فتح الرابط",
                                Toast.LENGTH_SHORT
                            ).show();
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
                        Toast.makeText(
                            MainActivity.this,
                            "تعذر تحميل السوق، تحقق من الإنترنت",
                            Toast.LENGTH_LONG
                        ).show();
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
                        Toast.makeText(
                            MainActivity.this,
                            "تعذر فتح اختيار الصور",
                            Toast.LENGTH_SHORT
                        ).show();
                        return false;
                    }
                }
            });

            webView.loadUrl("https://souq-al-bukamal.pages.dev");

        } catch (Exception e) {
            Toast.makeText(
                this,
                "حدث خطأ أثناء تشغيل التطبيق",
                Toast.LENGTH_LONG
            ).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && fileCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams
                    .parseResult(resultCode, data);

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
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
