
package com.souq.albukamal;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.util.Log;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

    private FrameLayout rootLayout;
    private LinearLayout loadingScreen;
    private TextView loadingText;
    private Button retryButton;

    private Handler handler = new Handler(Looper.getMainLooper());

    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 3000;
    private static final long LOAD_TIMEOUT = 20000;

    private Runnable retryRunnable;
    private Runnable timeoutRunnable;

    private boolean pageLoaded = false;

    private static final String HOME =
            "https://souq-albukamal.rakan-alkhalaf7.workers.dev/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createLayout();
        createWebView();
        loadHome();
    }

    private void createLayout() {

        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.WHITE);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);

        rootLayout.addView(webView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

        loadingScreen = new LinearLayout(this);
        loadingScreen.setOrientation(LinearLayout.VERTICAL);
        loadingScreen.setGravity(Gravity.CENTER);
        loadingScreen.setBackgroundColor(Color.WHITE);
        loadingScreen.setPadding(40, 40, 40, 40);

        TextView title = new TextView(this);
        title.setText("سوق البوكمال");
        title.setTextSize(28);
        title.setTextColor(Color.rgb(20, 130, 75));
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        loadingScreen.addView(title);

        ProgressBar progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams progressParams =
                new LinearLayout.LayoutParams(
                        100, 100);
        progressParams.topMargin = 40;
        progressParams.bottomMargin = 25;
        progressParams.gravity = Gravity.CENTER;

        loadingScreen.addView(progressBar, progressParams);

        loadingText = new TextView(this);
        loadingText.setText("جاري تحميل السوق...");
        loadingText.setTextSize(17);
        loadingText.setTextColor(Color.DKGRAY);
        loadingText.setGravity(Gravity.CENTER);

        loadingScreen.addView(loadingText);

        retryButton = new Button(this);
        retryButton.setText("إعادة المحاولة");
        retryButton.setVisibility(View.GONE);
        retryButton.setOnClickListener(v -> {
            retryCount = 0;
            loadHome();
        });

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = 25;

        loadingScreen.addView(retryButton, buttonParams);

        rootLayout.addView(loadingScreen,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

        setContentView(rootLayout);
    }

    private void createWebView() {

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
                        || url.startsWith(
                        "https://souq-albukamal.rakan-alkhalaf7.workers.dev/")) {
                    return false;
                }

                if (url.startsWith("https://wa.me/")
                        || url.startsWith("whatsapp://")
                        || url.startsWith("tel:")
                        || url.startsWith("mailto:")) {

                    try {
                        startActivity(new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(url)));
                    } catch (Exception e) {
                        Log.e("Souq", "External link error", e);
                    }

                    return true;
                }

                return false;
            }

            @Override
            public void onPageStarted(
                    WebView view, String url, android.graphics.Bitmap favicon) {

                if (!pageLoaded) {
                    showLoading("جاري تحميل السوق...");
                }

                startTimeout();
            }

            @Override
            public void onPageCommitVisible(
                    WebView view, String url) {

                pageLoaded = true;
                retryCount = 0;
                cancelTimeout();
                hideLoading();
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error) {

                if (request.isForMainFrame()) {

                    Log.e("Souq", "Loading error: "
                            + error.getDescription());

                    if (!pageLoaded) {
                        automaticRetry();
                    }
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
    }

    private void loadHome() {

        pageLoaded = false;
        showLoading("جاري الاتصال بسوق البوكمال...");

        cancelTimeout();

        webView.stopLoading();
        webView.loadUrl(HOME);

        startTimeout();
    }

    private void showLoading(String text) {

        loadingText.setText(text);
        retryButton.setVisibility(View.GONE);
        loadingScreen.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {

        loadingScreen.setVisibility(View.GONE);
    }

    private void startTimeout() {

        cancelTimeout();

        timeoutRunnable = () -> {

            if (!pageLoaded) {
                automaticRetry();
            }
        };

        handler.postDelayed(timeoutRunnable, LOAD_TIMEOUT);
    }

    private void cancelTimeout() {

        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }

    private void automaticRetry() {

        if (pageLoaded) return;

        cancelTimeout();

        if (retryCount < MAX_RETRIES) {

            retryCount++;

            showLoading("تأخر الاتصال، جاري إعادة المحاولة "
                    + retryCount + " من " + MAX_RETRIES + "...");

            retryRunnable = () -> {
                if (!pageLoaded) {
                    loadHome();
                }
            };

            handler.postDelayed(retryRunnable, RETRY_DELAY);

        } else {

            showLoading("تعذر تحميل السوق. تأكد من اتصال الإنترنت وحاول مجدداً.");
            retryButton.setVisibility(View.VISIBLE);
        }
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

        cancelTimeout();

        if (retryRunnable != null) {
            handler.removeCallbacks(retryRunnable);
        }

        if (fileCallback != null) {
            fileCallback.onReceiveValue(null);
            fileCallback = null;
        }

        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
