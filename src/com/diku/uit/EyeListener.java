package com.diku.uit;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import android.util.Log;
import android.widget.ImageView;

// somewhat based on: http://romanhosek.cz/android-eye-detection-updated-for-opencv-2-4-6/
public class EyeListener implements CvCameraViewListener2 {
	
	private static final String TAG = "UIT::EyeListener";
	private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
	
	private Mat mRgba;
	private Mat mGray;
	
	private int learn_frames = 0;
	private Mat teplateR;
	private Mat teplateL;
	private int method = Imgproc.TM_SQDIFF; // eye method = TM_SQDIFF
	private float mRelativeFaceSize = 0.2f;
	private int mAbsoluteFaceSize = 0;

	double xCenter = -1;
	double yCenter = -1;
	
	private CascadeClassifier mJavaDetector;
	private CascadeClassifier mJavaDetectorEye;
	
	private ImageView hubble;
	
	int threshold = 20; // distance from eyes from swiching zoomin/zomoout
	float scaleFactor = .02f; // how fast to zoom

	
	public void setImageView(ImageView iv){
		hubble = iv;
	}
	
	public void zoom(int s){
		if(hubble == null)
			return;
		
		float scale;
		
		if(s > 0){
			scale = 1.0f + scaleFactor;
		} else {
			scale = 1.0f - scaleFactor;
		}
		
		// scale the image matrix
		hubble.getImageMatrix().postScale(scale, scale);
		
		// this will force a redraw in a later cycle (not ui thread)
		hubble.postInvalidate();
	}
	
	public void setDetectors(CascadeClassifier d, CascadeClassifier e){
		mJavaDetector = d;
		mJavaDetectorEye = e;
	}
	
	public void onCameraViewStarted(int width, int height) {
		mGray = new Mat();
		mRgba = new Mat();
	}

	public void onCameraViewStopped() {
		mGray.release();
		mRgba.release();
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		mGray.release();
		mRgba.release();
		
		mRgba = inputFrame.rgba();
		mGray = inputFrame.gray();
		
		// transpose 90 degrees
		Core.transpose(mRgba, mRgba);
		Core.transpose(mGray, mGray);
		
		// mirror horizontal
		Core.flip(mRgba, mRgba, 0);
		Core.flip(mGray, mGray, 0);
		
		// mirror vertical
		Core.flip(mRgba, mRgba, 1);
		Core.flip(mGray, mGray, 1);

		if (mAbsoluteFaceSize == 0) {
			int height = mGray.rows();
			if (Math.round(height * mRelativeFaceSize) > 0) {
				mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
			}
		}
		
		MatOfRect faces = new MatOfRect();

		if (mJavaDetector != null)
			mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2,
					2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
					new Size(mAbsoluteFaceSize, mAbsoluteFaceSize),
					new Size());

		Rect[] facesArray = faces.toArray();
		
		// no faces, stop here
		if(facesArray.length == 0){
			return mRgba;
		}
		
		Core.rectangle(mRgba, facesArray[0].tl(), facesArray[0].br(),
				FACE_RECT_COLOR, 3);
		xCenter = (facesArray[0].x + facesArray[0].width + facesArray[0].x) / 2;
		yCenter = (facesArray[0].y + facesArray[0].y + facesArray[0].height) / 2;
		Point center = new Point(xCenter, yCenter);

		Core.circle(mRgba, center, 10, new Scalar(255, 0, 0, 255), 3);

		Rect r = facesArray[0];
		// compute the eye area
		Rect eyearea = new Rect(r.x + r.width / 8,
				(int) (r.y + (r.height / 4.5)), r.width - 2 * r.width / 8,
				(int) (r.height / 3.0));
		// split it
		Rect eyearea_right = new Rect(r.x + r.width / 16,
				(int) (r.y + (r.height / 4.5)),
				(r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
		Rect eyearea_left = new Rect(r.x + r.width / 16
				+ (r.width - 2 * r.width / 16) / 2,
				(int) (r.y + (r.height / 4.5)),
				(r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
		// draw the area - mGray is working grayscale mat, if you want to
		// see area in rgb preview, change mGray to mRgba
		Core.rectangle(mRgba, eyearea_left.tl(), eyearea_left.br(),
				new Scalar(255, 0, 0, 255), 2);
		Core.rectangle(mRgba, eyearea_right.tl(), eyearea_right.br(),
				new Scalar(255, 0, 0, 255), 2);

		if (learn_frames < 5) {
			teplateR = get_template(mJavaDetectorEye, eyearea_right, 24);
			teplateL = get_template(mJavaDetectorEye, eyearea_left, 24);
			learn_frames++;
		} else {
			// Learning finished, use the new templates for template
			// matching
			 Rect right = match_eye(eyearea_right, teplateR, method); 
			 Rect left = match_eye(eyearea_left, teplateL, method);

			 // zoom if we can find both eyes
			 if(right != null && left != null){
				 // the vertical distance between eyes
				 int dist = right.y - left.y;
				 
				 if(dist > threshold){
					// zoom in, right eye on top
					zoom(1);
				 } else if(dist < -threshold) {
					// zoom out, left eye on top
					zoom(-1);
				 } else {
					 // do nothing, eyes even
				 }
			 }
		}
		
		return mRgba;
	}
	
	private Rect match_eye(Rect area, Mat mTemplate, int type) {
		Point matchLoc;
		Mat mROI = mGray.submat(area);
		int result_cols = mROI.cols() - mTemplate.cols() + 1;
		int result_rows = mROI.rows() - mTemplate.rows() + 1;
		// Check for bad template size
		if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
			return null;
		}
		Mat mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

		// match with SQDIFF method
		Imgproc.matchTemplate(mROI, mTemplate, mResult, method);

		Core.MinMaxLocResult mmres = Core.minMaxLoc(mResult);
		matchLoc = mmres.minLoc;

		Point matchLoc_tx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
		Point matchLoc_ty = new Point(matchLoc.x + mTemplate.cols() + area.x,
				matchLoc.y + mTemplate.rows() + area.y);

		Core.rectangle(mRgba, matchLoc_tx, matchLoc_ty, new Scalar(255, 255, 0,
				255));
		 Rect rec = new Rect(matchLoc_tx,matchLoc_ty);
		 
		 return rec;
	}

	private Mat get_template(CascadeClassifier clasificator, Rect area, int size) {
		Mat template = new Mat();
		Mat mROI = mGray.submat(area);
		MatOfRect eyes = new MatOfRect();
		Point iris = new Point();
		Rect eye_template = new Rect();
		clasificator.detectMultiScale(mROI, eyes, 1.15, 2,
				Objdetect.CASCADE_FIND_BIGGEST_OBJECT
						| Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
				new Size());

		Rect[] eyesArray = eyes.toArray();
		for (int i = 0; i < eyesArray.length;) {
			Rect e = eyesArray[i];
			e.x = area.x + e.x;
			e.y = area.y + e.y;
			Rect eye_only_rectangle = new Rect((int) e.tl().x,
					(int) (e.tl().y + e.height * 0.4), (int) e.width,
					(int) (e.height * 0.6));
			mROI = mGray.submat(eye_only_rectangle);
			Mat vyrez = mRgba.submat(eye_only_rectangle);
			
			
			Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

			Core.circle(vyrez, mmG.minLoc, 2, new Scalar(255, 255, 255, 255), 2);
			iris.x = mmG.minLoc.x + eye_only_rectangle.x;
			iris.y = mmG.minLoc.y + eye_only_rectangle.y;
			eye_template = new Rect((int) iris.x - size / 2, (int) iris.y
					- size / 2, size, size);
			Core.rectangle(mRgba, eye_template.tl(), eye_template.br(),
					new Scalar(255, 0, 0, 255), 2);
			template = (mGray.submat(eye_template)).clone();
			return template;
		}
		return template;
	}
	
	public void reset(){
		// reset learning data, forcing 5 new learning frames
		learn_frames = 0;
	}
}
