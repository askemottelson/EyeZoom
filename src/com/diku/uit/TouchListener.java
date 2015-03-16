package com.diku.uit;

import android.graphics.Matrix;  
import android.graphics.PointF;  
import android.util.FloatMath;  
import android.util.Log;
import android.view.MotionEvent;  
import android.view.View;  
import android.view.View.OnTouchListener;  
import android.widget.ImageView; 

public class TouchListener implements OnTouchListener {  

	public static Matrix oldMatrix = new Matrix();  

	static final int NONE = 0;  
	static final int DRAG = 1;
	int mode = NONE;  
	
	float width = 675; // width of drawable
	float height = 1300; // height of drawable
	
	PointF start = new PointF();
	
	private ImageView view;  
	
	public void setView(View v){
		this.view = (ImageView) v;
	}

	@Override  
	public boolean onTouch(View v, MotionEvent event) {
		Matrix matrix = new Matrix(view.getImageMatrix());
		
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:  
				oldMatrix.set(matrix);  
				start.set(event.getX(), event.getY());  
				mode = DRAG;  
				break;  
			case MotionEvent.ACTION_UP:  
			case MotionEvent.ACTION_POINTER_UP:  
				mode = NONE;  
				break;  
			case MotionEvent.ACTION_MOVE:  
				if (mode == DRAG) { 
					matrix.set(oldMatrix);
					
					float[] matrixValues = new float[9];
					
					matrix.getValues(matrixValues);
			        float matrixX = matrixValues[2];
			        float matrixY = matrixValues[5];
			        width = matrixValues[0] * (((ImageView) view).getDrawable()
			                                .getIntrinsicWidth());
			        height = matrixValues[4] * (((ImageView) view).getDrawable()
			                                .getIntrinsicHeight());

			        float dx = event.getX() - start.x;
			        float dy = event.getY() - start.y;

			        //if image will go outside left bound
			        if (matrixX + dx > 0){
			            dx = -matrixX;
			        	
			        }
			        //if image will go outside right bound
			        if(matrixX + dx + width < view.getWidth()){
			            dx = view.getWidth() - matrixX - width;
			        }
			        //if image will go outside top bound
			        if (matrixY + dy > 0){
			            dy = -matrixY;
			        	
			        }
			        //if image will go outside bottom bound
			        if(matrixY + dy + height < view.getHeight()){
			            dy = view.getHeight() - matrixY - height;
			        }
			        
			        matrix.postTranslate(dx, dy);
				}
				
				break;  
		}

		view.setImageMatrix(matrix);
		
		return true;
	}  
}
