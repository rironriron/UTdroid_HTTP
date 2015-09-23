package jp.ac.u_tokyo.t.utdroid_http;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

public class MainActivity extends AppCompatActivity {
    /* Viewを格納する変数 */
    private Button buttonBack;
    private Button buttonForward;
    private Button buttonStop;
    private Button buttonReload;
    private Button buttonClearHistory;
    private EditText editTextUrl;
    private Button buttonGo;
    private WebView webView;
    private TextView textViewSource;

    /* 読込中のクルクルを表示するダイアログを管理する変数 */
    private ProgressDialog progress;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* それぞれの名前に対応するViewを取得する */
        buttonBack = (Button) findViewById(R.id.buttonBack);
        buttonForward = (Button) findViewById(R.id.buttonForward);
        buttonStop = (Button) findViewById(R.id.buttonStop);
        buttonReload = (Button) findViewById(R.id.buttonReload);
        buttonClearHistory = (Button) findViewById(R.id.buttonClearHistory);
        editTextUrl = (EditText) findViewById(R.id.editTextUrl);
        buttonGo = (Button) findViewById(R.id.buttonGo);
        webView = (WebView) findViewById(R.id.webView);
        textViewSource = (TextView) findViewById(R.id.textViewSource);

        /* WebViewの初期設定 */
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setSupportZoom(true);

        /**
         * WebViewの動作をカスタマイズするため、独自のWebViewClientを読み込ませる
         */
        webView.setWebViewClient(new WebViewClient() {
            /**
             * ページを読み込もうとする直前に呼ばれるメソッド
             */
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                /* 簡易なポップアップを表示 */
                Toast.makeText(MainActivity.this, "読込中："+url, Toast.LENGTH_SHORT).show();

                /* ページ遷移に関する処理を自前で実装し、WebViewに任せない場合はtrueを返す */
                return false;
            }

            /**
             * ページを読み込み始めた時に呼ばれるメソッド
             */
            @Override
            public void onPageStarted (WebView view, String url, Bitmap favicon) {
                /* 状態をUIに反映 */
                editTextUrl.setText(url);
                buttonStop.setEnabled(true);
                buttonReload.setEnabled(false);
                /* 戻れる場合は戻るボタンを有効に */
                buttonBack.setEnabled( view.canGoBack() );
                /* 進める場合は進むボタンを有効に */
                buttonForward.setEnabled( view.canGoForward() );
            }

            /**
             * ページを読み込み終わった時に呼ばれるメソッド
             */
            @Override
            public void onPageFinished (WebView view, String url) {
                /* 状態をUIに反映 */
                buttonStop.setEnabled(false);
                buttonReload.setEnabled(true);
            }

            /**
             * BASIC認証やDIGEST認証を要求された時に呼ばれるメソッド
             */
            @Override
            public void onReceivedHttpAuthRequest (WebView view, HttpAuthHandler handler, String host, String realm) {
                /* ドメインがexample.comなら、"yourID"と"yourPassword"で認証 */
                if (host.indexOf("example.com") > -1 ) {
                    handler.proceed("yourID", "yourPassword");
                }
            }
        });

        /* クリックした時の動作を指定する */
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* WebViewを前のページに戻す */
                webView.goBack();
            }
        });

        /* クリックした時の動作を指定する */
        buttonForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* WebViewを次のページに戻す */
                webView.goForward();
            }
        });

        /* クリックした時の動作を指定する */
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* WebViewの読み込みを停止する */
                webView.stopLoading();
                /* 状態をUIに反映 */
                buttonStop.setEnabled(false);
                buttonReload.setEnabled(true);
            }
        });

        /* クリックした時の動作を指定する */
        buttonReload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* WebViewの読み込みを停止する */
                webView.reload();
                /* 状態をUIに反映 */
                buttonStop.setEnabled(true);
                buttonReload.setEnabled(false);
            }
        });

        /* クリックした時の動作を指定する */
        buttonClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* WebViewの履歴を消去する */
                webView.clearHistory();
                /* 状態をUIに反映 */
                buttonBack.setEnabled(true);
                buttonForward.setEnabled(false);
            }
        });

        /* クリックした時の動作を指定する */
        buttonGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = editTextUrl.getText().toString();
                /* WebViewに指定のURLを読み込ませる */
                webView.loadUrl(url);
                /* 自前のコードでHTTP通信してTextViewに表示する（その1） */
                new HttpFetchTask().execute(url);
                /* 自前のコードでHTTP通信してTextViewに表示する（その2） */
                /* httpFetchWithAQuery(url); */
            }
        });

    }

    /**
     * 別スレッドでHTTP通信して内容を表示するためのクラス
     * AsyncTask<T1, T2, T3>
     *     T1 : doInBackground()の引数の型
     *     T2 : onProgressUpdate()の引数の型
     *     T3 : onPostExecute()の引数の型 = doInBackground()の返り値の型
     */
    private class HttpFetchTask extends AsyncTask<String, Void, String> {
        /**
         * メインの処理を行う前に、メインスレッド（UIスレッド）で行う処理
         */
        @Override
        protected void onPreExecute() {
            /* クルクルを表示 */
            progress = new ProgressDialog(MainActivity.this);
            progress.setMessage("Loading...");
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.show();
        }

        /**
         * バックグラウンドで別スレッドで）実行されるメインの処理
         */
        @Override
        protected String doInBackground(String... params) {
            if (params.length > 0) {
                try {
                    /* HTTP通信の準備 */
                    String url = params[0];
                    /* GETメソッド */
                    HttpGet httpGet = new HttpGet(url);
                    /* POSTメソッド */
                    /* HttpPost httpPost = new HttpPost(url); */
                    DefaultHttpClient client = new DefaultHttpClient();

                    /* ヘッダーの付与 */
                    httpGet.setHeader("deviceType", "phone");
                    httpGet.setHeader(CoreProtocolPNames.USER_AGENT, "android");

                    /* パラメータの付与（POSTのみ）*/
                    /*
                    ArrayList<NameValuePair> httpParams = new ArrayList<NameValuePair>();
                    httpParams.add(new BasicNameValuePair("subject", "件名"));
                    httpParams.add(new BasicNameValuePair("body", "本文"));
                    httpPost.setEntity(new UrlEncodedFormEntity(httpParams, "UTF-8"));
                    */

                    /* ベーシック認証 */
                    UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("yourID","yourPassword");
                    AuthScope scope = new AuthScope("example.com", 80);
                    client.getCredentialsProvider().setCredentials(scope, credentials);

                    /* HTTP通信の実行 */
                    HttpResponse httpResponse = client.execute(httpGet);
                    /* レスポンスコードの取得（Success:200、Auth Error:403、Not Found:404、Internal Server Error:500）*/
                    int statusCode = httpResponse.getStatusLine().getStatusCode();

                    /* レスポンス本体の取得 */
                    HttpEntity entity = httpResponse.getEntity();
                    String response = EntityUtils.toString(entity);
                    /* HTTP通信の後処理 */
                    entity.consumeContent();
                    client.getConnectionManager().shutdown();
                    if (statusCode == HttpStatus.SC_OK) {
                        /* 通信に成功したら内容を返す */
                        return response;
                    }
                } catch (Exception e) {
                    /* エラーが発生した時はLogを吐かせる */
                    e.printStackTrace();
                }
            }
            /* 通信に失敗したら"Error"という文字列を返す */
            return "Error";
        }

        /**
         * メインの処理を行った後に、メインスレッド（UIスレッド）で行う処理
         */
        @Override
        protected void onPostExecute(String response) {
            /* 結果をTextViewに出力する */
            textViewSource.setText(response);
            /* クルクルを消す */
            progress.cancel();
            progress = null;
        }
    }

    /**
     * AQueryを用いて、別スレッドでHTTP通信して内容を表示するメソッド
     */
    private void httpFetchWithAQuery(String url) {
        /* 文字列でコールバックを要求 */
        AjaxCallback<String> callback = new AjaxCallback<String>() {
            /* 読み込み完了時にこのメソッドが呼ばれる */
            @Override
            public void callback(String url, String response, AjaxStatus status) {
                if(status.getCode() == HttpStatus.SC_OK){
                    textViewSource.setText(response);
                }
            }
        };
        /* URLとメソッドをセット */
        callback.url(url);
        callback.method(AQuery.METHOD_GET);
        /* コールバックの型を指定 */
        callback.type(String.class);
        /* HTTP通信の実行 */
        new AQuery(this).ajax(callback);
    }
}
