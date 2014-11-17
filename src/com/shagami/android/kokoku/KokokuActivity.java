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
	
	// �^�b�`���ꂽ��A�i�r�Q�[�V�����o�[�Ȃǂ̏�Ԃ�ύX���邾���Ȃ̂ŁA�^�b�`�͌Ă΂Ȃ�
	//�@���������ALint�̌x���������Ȃ��悤�ɐݒ肵�Ă���
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
	
	// �y�[�W�̃��[�h���I�������A���̃y�[�W�҂�������悤�Ƀ��b�Z�[�W�𓊂���
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
			Log.d("kokoku", "���[�h����:" + url);

			// dialog����������Ă���Ȃ�΁A�����B
			if ( mDialog != null ) {
				mDialog.dismiss();
			}

			mDialog = null;
			
			if (mURLList != null) {
				Log.d("kokoku", "mURLIndex:"+Integer.toString(mURLIndex));
				
				// �ǂݍ��݂��I�������A����܂ł̎��ԑ҂������郁�b�Z�[�W�𑗐M����
				if (++mURLIndex >= mURLList.size()) {
					mURLIndex = 0;
				}
				
				Log.d("kokoku", "mURLIndex:"+Integer.toString(mURLIndex) + "/URLList.size:" + Integer.toString(mURLList.size()));
				
				if ( mWaitTime < 0 ) {
					mWaitTime  = 30;
				}
				
				// mWaitTime�͕b�Ȃ̂ŁA�~���b�ɕϊ�����
				mHandler.sendEmptyMessageDelayed(_SITE_LOAD, mWaitTime * 1000);
			}
		}
	};
	
	// ���[�J���Ƀt�@�C�����Ȃ��Ƃ��́Astring.xml��default_url�Œ�`���Ă���URL��ǂݍ��ނ��A
	// ���ԑ҂��͈�؂��Ȃ�
	
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

		mWaitTime = -1;			// 30�b
		
		mClient = new WebViewClient();
		
		mHasMessage = false;
		
		// �����X�g���[�W�̋��ʃt�H���_�ADownload�Ƀt�@�C����u����悤�ɂ���
		String pathname = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsoluteFile() + "/url_list.txt";
		File fileHandle = new File(pathname);

		if ( fileHandle.exists() ) {
			Toast.makeText(this, fileHandle.getAbsoluteFile() + ":" + getString(R.string.url_file), Toast.LENGTH_LONG).show();
			
			try {
				FileInputStream fi = new FileInputStream(fileHandle);			
				Scanner sc = new Scanner(fi);

				// 1�s���t�@�C����ǂݍ���ŁAURL���X�g�Ƃ��ĕۑ�����B
				// �ŏ��̍s�������Ŏn�܂��Ă�����A�E�F�C�g�̕b���Ƃ��ĕۑ����邱��
				mURLList  = new ArrayList<String>();
				while(sc.hasNext() ) {
					String s = sc.nextLine();
					Log.d("kokoku", s);
					
					// 1�����ڂ������̂Ƃ��ŁAmWaitTime���ݒ肳��Ă��Ȃ���΂��̐��l���g��
					// URL���o:http://stackoverflow.com/questions/163360/regular-expresion-to-match-urls-in-java
					if ( s.matches("\\b[0-9]*") ) {
						if ( mWaitTime < 0 ) { 
							mWaitTime = Integer.parseInt(s);
							
							Log.d("kokoku", "�E�F�C�g");
						}
					} else if ( s.matches("\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]") ) {
						mURLList.add(s);
						
						Log.d("kokoku", "URL���X�g�ǉ�");
					}
				}
				
				// URL���X�g���������ǂݍ��߂��Ƃ��́A���̌�̏�����o�^����
				mURLIndex = 0;
				mWebView.loadUrl(mURLList.get(mURLIndex));
			} catch (Exception e) {
				// �Ȃɂ��G���[���������Ƃ��́A�f�t�H���g��URL��\�����ďI���ɂ���
				mWebView.loadUrl(getString(R.string.default_url));
			}
		} else {
			// �ݒ�t�@�C�������݂��Ȃ��Ƃ��́A�f�t�H���g��URL�����[�h����
			Toast.makeText(this, fileHandle.getAbsoluteFile() + ":" + getString(R.string.no_url_file), Toast.LENGTH_LONG).show();
			mWebView.loadUrl(getString(R.string.default_url));
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		// �ꎞ��~����Ƃ��́A���b�Z�[�W�����𒆒f����
		if ( mHandler.hasMessages(_SITE_LOAD) ) {
			mHandler.removeMessages(_SITE_LOAD);
			mHasMessage = true;
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// ���f����Ă����Ƃ��́A���b�Z�[�W�������ĊJ����
		if ( mHasMessage ) {
			mHasMessage = false;
			
			// �ۑ�����Ă���l���s��ɂȂ��Ă�����A�ŏ�����ǂݍ��ݒ���
			if ( mURLIndex < 0 || mURLIndex >= mURLList.size() ) {
				mURLIndex = 0;
			}
			mWebView.loadUrl(mURLList.get(mURLIndex));
		}
	}
}