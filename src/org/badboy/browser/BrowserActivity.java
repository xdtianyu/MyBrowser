package org.badboy.browser;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.metalev.multitouch.controller.MultiTouchController;
import org.metalev.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas;
import org.metalev.multitouch.controller.MultiTouchController.PointInfo;
import org.metalev.multitouch.controller.MultiTouchController.PositionAndScale;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

/***
 * 使用WebView对象创建�?���?��网页浏覻器�?
 * @author BadBoy
 *
 */
public class BrowserActivity extends Activity implements MultiTouchObjectCanvas<Object>{
	
	//public static final String DOWNLOAD_ACTION = "android.intent.action.DOWNLOAD";
	
	private final static int MENU_NEW_WEB = Menu.FIRST; 
	private final static int MENU_SET_MAIN_PAGE = MENU_NEW_WEB + 1;
	
	private String defaultGoogleUrl = "";
	private String defaultUCWebUrl = "";
	private String defaultUrl = "";
	private WebView mWebView;
	//储存默认网址文件
	private String fileUrl = "fileUrl.txt";
	//对话框标�?	
	private final static int PROGRESS_DIALOG = 110;
	private final static int SET_DEFAULT_URL_DIALOG = 111;
	//下载进度条标�?	
	private final static int DOWNLOAD_PROGRESS_DIALOG = 121;
	private boolean isDownload = false;
	private int downloads = 0;
	//显示网页加载进度
	private ProgressDialog mDialog;
	private ProgressBar bar;
	//下载进度条显�?	
	private ProgressDialog downloadProgress;
	//多点触摸
	private MultiTouchController<Object> mMultiTouchController;
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //下面四条语句要在setContentView()方法之前调用，否则程序会出错，在本程序中，加入以下的语句貌似还没有什么影�?或�?有我还没发现
        this.requestWindowFeature(Window.FEATURE_LEFT_ICON);
        this.requestWindowFeature(Window.FEATURE_RIGHT_ICON);
        this.requestWindowFeature(Window.FEATURE_PROGRESS);
        this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        setContentView(R.layout.main);
        initWebView();
        performSearchIntent(getIntent());
        
        
    }
	
	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		performSearchIntent(intent);
	}
	/**
	 * 执行搜索，打�?���?	 */
	private void performSearchIntent(Intent intent) {
		if(Intent.ACTION_SEARCH.equals(intent.getAction())){
        	String query = intent.getStringExtra(SearchManager.QUERY);
        	//还没有对网址进行严格的解�?			
        	if(!query.startsWith("http://")){
        		mWebView.loadUrl("http://"+query);
        	}
        	if(query.startsWith("http://")){
        		mWebView.loadUrl(query);
        	}
        	/*else{
        		Toast.makeText(this, R.string.url_error, Toast.LENGTH_SHORT).show();
        	}*/
        }
	}
	
	private void initWebView() {
		defaultGoogleUrl = this.getResources().getString(R.string.default_google_url);
		defaultUCWebUrl = this.getResources().getString(R.string.default_ucweb_url);
		setDefaultURL();
		
		bar = (ProgressBar)findViewById(R.id.progress_bar);
		
		mMultiTouchController = new MultiTouchController<Object>(this,false);
		
		mWebView = (WebView)findViewById(R.id.webview);
        //使用mWebView的getSettings()方法设置支持JavaScript为true
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setScrollBarStyle(0);
        //用loadUrl方法加载网址
        mWebView.loadUrl(defaultUrl);
        //对mWebView设置WebViewClient对象,如果不设置此对象那么当单击mWebView中的链接时将由系�?        //默认的Browser来响应链接，即由默认的Browser打开链接，�?不是你自己写的Browser来打�?        //故为了mWebView自己处理页面中的�?��链接，则要扩展WebViewClient类，重载shouldOverrideUrlLoading()方法
        mWebView.setWebViewClient(new MyWebViewClient());
        //
        mWebView.setWebChromeClient(new MyWebChromeClient());
        //实现下载监听
		mWebView.setDownloadListener(new DownloadListener(){

			public void onDownloadStart(String url, String userAgent, String contentDisposition,
					String mimetype, long contentLength) {
				/*Uri uri = Uri.parse(url);
				Intent intent = new Intent(DOWNLOAD_ACTION,uri);
				startActivity(intent);*/
				
				//downloadFile(url, mimetype, mimetype, mimetype, contentLength);
				ContentValues values = new ContentValues();
				values.put(Downloads.COLUMN_URI, url);
			}
			
		});
	}
	/*
	 * 下列代码实现网络文件下载功能，目前下载的文件时存储在/data/data/org.badboy.browser/files目录下的�?	 * 后续还会更新这个方法，将其下载到SDCard上来。�?路是这样子的：下载文件时先棌查是否由sdcard或udisk外存储设备，
	 * 若有则直接下载到外存储设备，否则存储�?data/data/org.badboy.browser/files目录中�?�?��还覠增加�?��功能：就�?	 * 列出已下载文件，以及对文件copy，delete等操作，下载进度条�?
	 * 2011-3-4
	 */
	private void downloadFile(String url,String userAgent, String contentDisposition,
			String mimetype, long contentLength) {
		
		/*String filename = URLUtil.guessFileName(url,
                contentDisposition, mimetype);
		// Check to see if we have an SDCard
        String status = Environment.getExternalStorageState();
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
            int title;
            String msg;

            // Check to see if the SDCard is busy, same as the music app
            if (status.equals(Environment.MEDIA_SHARED)) {
                msg = getString(R.string.download_sdcard_busy_dlg_msg);
                title = R.string.download_sdcard_busy_dlg_title;
            } else {
                msg = getString(R.string.download_no_sdcard_dlg_msg, filename);
                title = R.string.download_no_sdcard_dlg_title;
            }

            new AlertDialog.Builder(this)
                .setTitle(title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(msg)
                .setPositiveButton(R.string.ok, null)
                .show();
            return;
        }*/
		isDownload = true;
		
		
		try {
			String filename = URLUtil.guessFileName(url,
	                contentDisposition, mimetype);
			URL url2 = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) url2.openConnection();
			conn.setDoInput(true);
			conn.connect();
			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				InputStream is = conn.getInputStream();
				FileOutputStream fos = this.openFileOutput(filename, Context.MODE_APPEND);
				int len = 0;
				byte[] buf = new byte[1024];
				while ((len = is.read(buf)) != -1) {
					fos.write(buf, 0, len);
					
				}
				is.close();
				fos.close();
				isDownload = false;
			} else {
				Toast.makeText(this, R.string.net_error, Toast.LENGTH_SHORT).show();
				isDownload = false;
			}

		} catch (Exception e) {
			e.printStackTrace();
			isDownload = false;
		}
		
	}

	private void setDefaultURL() {
		try {
			//android中使用openFileInput()方法得到文件输入�?			
			FileInputStream fis = this.openFileInput(fileUrl);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int len = 0;
			while((len = fis.read(buffer))!=-1){
				baos.write(buffer, 0, len);
			}
			//defaultUrl = new String(buffer,0,len);
			defaultUrl = baos.toString();
			fis.close();
			baos.close();
		} catch (Exception e) {
			e.printStackTrace();
			defaultUrl = defaultGoogleUrl;
		}
		//如果还是为空(即文件中还没有存储默认网�?，则使用defaultUCWebUrl
		if(defaultUrl.equals("")){
			defaultUrl = defaultGoogleUrl;//defaultUCWebUrl;
		}
	}
	/**
	 * 按键事件处理
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//按返回键可�?回之前浏览过的网�?		
		if((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()){
			mWebView.goBack();
			return true;
		}
		//搜索按键
		if(keyCode == KeyEvent.KEYCODE_SEARCH){
			onSearchRequested();
		}
		return super.onKeyDown(keyCode, event);
	}
	/**
	 * 重写WebChromeClient�?	 */
	private class MyWebChromeClient extends WebChromeClient {
		//设置网页加载进度�?		@Override
		public void onProgressChanged(WebView webview, int newProgress){
			BrowserActivity.this.getWindow().setFeatureInt(Window.FEATURE_PROGRESS, 100);
		
			bar.setProgress(newProgress);
			bar.setVisibility(View.VISIBLE);
			if(bar.getProgress()==100){
				bar.setVisibility(View.GONE);
			}
			super.onProgressChanged(webview, newProgress);
		}
	}
	/**
	 * 重写 WebViewClient�?由本Browser处理网页中的链接
	 */
	private class MyWebViewClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
		
	}
	/**
	 * Menu 按键的添加和事件处理
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//打开网址输入�?		
		menu.add(0,MENU_NEW_WEB,0,R.string.new_web_page)
			.setIcon(R.drawable.search_icon);
		//设置默认网址
		menu.add(0,MENU_SET_MAIN_PAGE,0,R.string.set_default_url)
		.setIcon(R.drawable.browser_icon);
		return super.onCreateOptionsMenu(menu);
	}
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()){
		case MENU_NEW_WEB:
			onSearchRequested();
			return true;
		case MENU_SET_MAIN_PAGE:
			this.showDialog(SET_DEFAULT_URL_DIALOG);
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}
	
	/**
	 * 重写onSearchRequested(),可实现调用Search Bar
	 */
	@Override
	public boolean onSearchRequested() {

		this.startSearch(null, false, null, false);
		return true;
	}
	/**
	 * 当覠调用this.showDialog(int id)方法来显示Dialog时，要重写下列方�?	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
		case PROGRESS_DIALOG:
			mDialog = new ProgressDialog(this);
			//mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mDialog.setMessage(getString(R.string.page_is_loading));
			return mDialog;
		case SET_DEFAULT_URL_DIALOG:
			return createDefaultUrlDialog();
		case DOWNLOAD_PROGRESS_DIALOG:
			return createDownloadProgress();
		default:
			return super.onCreateDialog(id);
		}
		
	}
	private Dialog createDownloadProgress() {
		downloadProgress = new ProgressDialog(this);
		downloadProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		downloadProgress.setMessage(getString(R.string.file_is_loading));
		return downloadProgress;
	}

	private Dialog createDefaultUrlDialog() {
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.default_url_dialog);
		dialog.setTitle(R.string.dialog_title);
		final EditText url = (EditText)dialog.findViewById(R.id.url);
		Button btnOK = (Button)dialog.findViewById(R.id.ok_btn);
		btnOK.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				defaultUrl = url.getText().toString();
				try {
					FileOutputStream fos = BrowserActivity.this.openFileOutput(fileUrl, Context.MODE_PRIVATE);
					byte[] buffer = defaultUrl.getBytes();
					fos.write(buffer);
					fos.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				dialog.dismiss();
				mWebView.loadUrl(defaultUrl);
			}
		});
		Button btnCancel = (Button)dialog.findViewById(R.id.cancel_btn);
		btnCancel.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				dialog.dismiss();
			}
		});
		return dialog;
	}

	/**
	 * 屏幕旋转时，程序会重新调用onCreate()方法，即重新加载程序。当不需要这么做时，
	 * 可在manifest文件中的Activity标签中加入xndroid:configChanges="orientation|keyboardHidden"
	 * 属�?，并且重写下列方法�?该方法中可以处理屏幕旋转时发生的动作
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		
		/*if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			
		} else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			
		}*/

		super.onConfigurationChanged(newConfig);
	}
	
	//-------------multitouch stuff ------------------------------------
	
	private int mCurrentZoom = 0;
	private static final double ZOOM_SENSITIVITY = 1.6;
	private static final float ZOOM_LOG_BASE_INV = 1.0f / (float) Math.log(2.0 / ZOOM_SENSITIVITY);
	private boolean isMultiTouchScale = false;
	@Override 
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (mMultiTouchController.onTouchEvent(event)) {
			if (!isMultiTouchScale) {
				event.setAction(MotionEvent.ACTION_CANCEL);
				super.dispatchTouchEvent(event);
			}
		}else{
			isMultiTouchScale = true;
			 if (super.dispatchTouchEvent(event)) {
				 return true;
			 }
			 return false;
		}
		return true;
	}
	public Object getDraggableObjectAtPoint(PointInfo touchPoint) {
		return new Object();
	}

	public void getPositionAndScale(Object obj,
			PositionAndScale objPosAndScaleOut) {
		objPosAndScaleOut.set(0.0f, 0.0f, true, 1.0f, false, 0.0f, 0.0f, false, 0.0f);
		mCurrentZoom = 0;
	}

	public void selectObject(Object obj, PointInfo touchPoint) {
		
	}

	public boolean setPositionAndScale(Object obj,
			PositionAndScale newObjPosAndScale, PointInfo touchPoint) {
		float newRelativeScale = newObjPosAndScale.getScale();
		int targetZoom = (int) Math.round(Math.log(newRelativeScale)
				* ZOOM_LOG_BASE_INV);
		while (mCurrentZoom > targetZoom) {
			mCurrentZoom--;
			mWebView.zoomOut();
		}
		while (mCurrentZoom < targetZoom) {
			mCurrentZoom++;
			mWebView.zoomIn();
		}
		return true;
	}
	
}