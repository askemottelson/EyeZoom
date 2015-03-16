package com.diku.uit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.objdetect.CascadeClassifier;

import com.diku.uit.TouchListener;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class FdActivity extends Activity {

	private static final String TAG = "UIT::Activity";

	static {
        if (!OpenCVLoader.initDebug()) {
        	// open cv load error
        }
    }
	
	private CameraBridgeViewBase mOpenCvCameraView;
	
	private EyeListener eyeListener; 

	private File mCascadeFile;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");

				try {
					// load cascade file from application resources
					InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
					File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
					mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
					FileOutputStream os = new FileOutputStream(mCascadeFile);

					byte[] buffer = new byte[4096];
					int bytesRead;
					while ((bytesRead = is.read(buffer)) != -1) {
						os.write(buffer, 0, bytesRead);
					}
					is.close();
					os.close();

					// --------------------------------- load left eye
					// classificator -----------------------------------
					InputStream iser = getResources().openRawResource(R.raw.haarcascade_lefteye_2splits);
					File cascadeDirER = getDir("cascadeER", Context.MODE_PRIVATE);
					File cascadeFileER = new File(cascadeDirER, "haarcascade_eye_right.xml");
					FileOutputStream oser = new FileOutputStream(cascadeFileER);

					byte[] bufferER = new byte[4096];
					int bytesReadER;
					while ((bytesReadER = iser.read(bufferER)) != -1) {
						oser.write(bufferER, 0, bytesReadER);
					}
					iser.close();
					oser.close();

					CascadeClassifier mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
					if (mJavaDetector.empty()) {
						Log.e(TAG, "Failed to load cascade classifier");
						mJavaDetector = null;
					} else {
						Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
					}

					CascadeClassifier mJavaDetectorEye = new CascadeClassifier(
							cascadeFileER.getAbsolutePath());
					if (mJavaDetectorEye.empty()) {
						Log.e(TAG, "Failed to load cascade classifier");
						mJavaDetectorEye = null;
					} else {
						Log.i(TAG, "Loaded cascade classifier from "
								+ mCascadeFile.getAbsolutePath());
					}

					eyeListener.setDetectors(mJavaDetector, mJavaDetectorEye);
					cascadeDir.delete();

				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
				}
				
				mOpenCvCameraView.setCameraIndex(1);
				mOpenCvCameraView.enableView();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};
   
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.face_detect_surface_view);

		// set hubble background image and touch handler
	    ImageView imageView = (ImageView) findViewById(R.id.imageView);
	    TouchListener imageViewHandler = new TouchListener();
	    imageViewHandler.setView(imageView);
	    
	    imageView.setOnTouchListener(imageViewHandler);
	    Bitmap hubble = BitmapFactory.decodeResource(getResources(), R.drawable.hubble);
	    imageView.setImageBitmap(hubble);
	    
		// attach camera to view
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
		
		eyeListener = new EyeListener();
		eyeListener.setImageView(imageView);
		
		mOpenCvCameraView.setCvCameraViewListener(eyeListener);
		
		// initate open cv stuff
		mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null){
			mOpenCvCameraView.disableView();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		mOpenCvCameraView.enableView();
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null){
			mOpenCvCameraView.disableView();
		}
	}

	// when clicking the camera feed, reset learning
	public void onRecreateClick(View v)
    {
    	this.eyeListener.reset();
    }

}
