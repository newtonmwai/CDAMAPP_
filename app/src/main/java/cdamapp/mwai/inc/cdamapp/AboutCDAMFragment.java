package cdamapp.mwai.inc.cdamapp;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class AboutCDAMFragment extends Fragment {

    private WebView webview;
    private static final String TAG = "Main";
    private ProgressDialog progressBar;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        View v = inflater.inflate(R.layout.about_cdam, container, false);

        String url = "https://coffee-dryness-acoustic-monitor-paprazzi.c9.io/index.html";

        WebView wv = (WebView)v.findViewById(R.id.aboutwebview);

        wv.loadUrl(url);
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        return v;

    }


}
