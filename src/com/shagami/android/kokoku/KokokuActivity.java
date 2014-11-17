package com.shagami.android.kokoku;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Scanner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class KokokuActivity extends Activity {
	WebView mWebView;
	View decorView;
	private int uiBaseOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			  				  | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			  				  ;
	
	private int uiOptions = uiBaseOptions
			  | View.SYSTEM_UI_FLAG_FULLSCREEN
			  | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			  ;

	static int lastSetting = -1;

	private ArrayList<String> mURLList = null;
	private int mWaitTime;
	
	private int mURLIndex;

	private boolean mHasMessage;
	
	public final int _SITE_LOAD = 1; 
	
	// 
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage ( Message msg ) {
			if ( msg.what == _SITE_LOAD ) {
				mWebView.loadUrl(mURLList.get(mURLIndex));
			} else {
				super.dispatchMessage(msg);
			}
		}
	};
	
	// タッチされたら、ナビゲーションバーなどの状態を変更するだけなので、タッチは呼ばない
	//　ここだけ、Lintの警告をさせないように設定している
	View.OnTouchListener mTouchListener = new OnTouchListener() {
		@SuppressLint("ClickableViewAccessibility")
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			Log.d("kokoku", "ontouch/"+Integer.toString( v.getSystemUiVisibility()));
			
			if ( lastSetting != v.getSystemUiVisibility() ) {
				v.setSystemUiVisibility(uiOptions);
				
				lastSetting = uiOptions;
			}
			
			return true;
		}
	};
	
	// ページのロードが終わったら、次のページ待ちをするようにメッセージを投げる
	WebViewClient mClient = new WebViewClient() {
		Dialog mDialog = null;
		
		@Override
		public void onPageStarted (WebView view, String url, Bitmap favicon) {
			mDialog = new Dialog(view.getContext());
			
			mDialog.setTitle(R.string.page_loading);
			mDialog.show();
		}
		
		@Override
		public void onPageFinished (WebView view, String url) {
			Log.d("kokoku", "ロード完了:" + url);

			// dialogが生成されているならば、消す。
			if ( mDialog != null ) {
				mDialog.dismiss();
			}

			mDialog = null;
			
			if (mURLList != null) {
				Log.d("kokoku", "mURLIndex:"+Integer.toString(mURLIndex));
				
				// 読み込みが終わったら、次回までの時間待ちをするメッセージを送信する
				if (++mURLIndex >= mURLList.size()) {
					mURLIndex = 0;
				}
				
				Log.d("kokoku", "mURLIndex:"+Integer.toString(mURLIndex) + "/URLList.size:" + Integer.toString(mURLList.size()));
				
				if ( mWaitTime < 0 ) {
					mWaitTime  = 30;
				}
				
				// mWaitTimeは秒なので、ミリ秒に変換する
				mHandler.sendEmptyMessageDelayed(_SITE_LOAD, mWaitTime * 1000);
			}
		}
	};
	
	// ローカルにファイルがないときは、string.xmlにdefault_urlで定義してあるURLを読み込むが、
	// 時間待ちは一切しない
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_kokoku);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  
//		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);  
		
		mWebView = (WebView)findViewById(R.id.webKokoku);
		mWebView.setSystemUiVisibility(uiOptions);
		mWebView.setWebViewClient(mClient);

		mWebView.setOnTouchListener(mTouchListener);

		mWaitTime = -1;			// 30秒
		
		mClient = new WebViewClient();
		
		mHasMessage = false;
		
		// 内部ストレージの共通フォルダ、Downloadにファイルを置けるようにする
		String pathname = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsoluteFile() + "/url_list.txt";
		File fileHandle = new File(pathname);

		if ( fileHandle.exists() ) {
			Toast.makeText(this, fileHandle.getAbsoluteFile() + ":" + getString(R.string.url_file), Toast.LENGTH_LONG).show();
			
			try {
				FileInputStream fi = new FileInputStream(fileHandle);			
				Scanner sc = new Scanner(fi);

				// 1行ずつファイルを読み込んで、URLリストとして保存する。
				// 最初の行が数字で始まっていたら、ウェイトの秒数として保存すること
				mURLList  = new ArrayList<String>();
				while(sc.hasNext() ) {
					String s = sc.nextLine();
					Log.d("kokoku", s);
					
					// 1文字目が数字のときで、mWaitTimeが設定されていなければその数値を使う
					// URL抽出:http://stackoverflow.com/questions/163360/regular-expresion-to-match-urls-in-java
					if ( s.matches("\\b[0-9]*") ) {
						if ( mWaitTime < 0 ) { 
							mWaitTime = Integer.parseInt(s);
							
							Log.d("kokoku", "ウェイト");
						}
					} else if ( s.matches("\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]") ) {
						mURLList.add(s);
						
						Log.d("kokoku", "URLリスト追加");
					}
				}
				
				// URLリストが正しく読み込めたときは、その後の処理を登録する
				mURLIndex = 0;
				mWebView.loadUrl(mURLList.get(mURLIndex));
			} catch (Exception e) {
				// なにかエラーがあったときは、デフォルトのURLを表示して終わりにする
				mWebView.loadUrl(getString(R.string.default_url));
			}
		} else {
			// 設定ファイルが存在しないときは、デフォルトのURLをロードする
			Toast.makeText(this, fileHandle.getAbsoluteFile() + ":" + getString(R.string.no_url_file), Toast.LENGTH_LONG).show();
			mWebView.loadUrl(getString(R.string.default_url));
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		// 一時停止するときは、メッセージ処理を中断する
		if ( mHandler.hasMessages(_SITE_LOAD) ) {
			mHandler.removeMessages(_SITE_LOAD);
			mHasMessage = true;
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// 中断されていたときは、メッセージ処理を再開する
		if ( mHasMessage ) {
			mHasMessage = false;
			
			// 保存されている値が不定になっていたら、最初から読み込み直す
			if ( mURLIndex < 0 || mURLIndex >= mURLList.size() ) {
				mURLIndex = 0;
			}
			mWebView.loadUrl(mURLList.get(mURLIndex));
		}
	}
}