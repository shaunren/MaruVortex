package org.av.maruvortex;

import android.os.Bundle;
import android.app.Activity;



import java.util.*;
import android.os.SystemClock;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;



public class MaruVortex extends Activity  {
	SensorManager mSensorManager;
	Panel _p;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(_p = new Panel(this));
	}
	
	@Override
    protected void onResume() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onResume();
        _p.start();
    }

    @Override
    protected void onPause() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onPause();
        _p.stop();
    }
    
	class Panel extends SurfaceView implements SurfaceHolder.Callback, SensorEventListener {
		//GAME VARS
		int level = 1;
		int score = 0;
		Random r = new Random();
		private Sensor mRotationVectorSensor;
		boolean berzerk = false;
		
		//PAINT VARS
		Paint redPaint = new Paint();
		Paint blackPaint = new Paint();
		Paint whitePaint = new Paint();
		Paint textPaint;

		Paint aA = new Paint(Paint.ANTI_ALIAS_FLAG);
		Paint invert = new Paint();
		float mx[] =
			{
			-1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 
			0.0f, -1.0f, 0.0f, 1.0f, 1.0f,
			0.0f, 0.0f, -1.0f, 1.0f, 1.0f, 
			0.0f, 0.0f, 0.0f, 1.0f, 0.0f
			};
		
		Paint bitmapPaint;


		//TIME VARS
		long start;
		long t = -1000, f = -1000, g = -100; //f is square particle, g is bullet
		
		//PARTICLE CONTAINERS
		HashSet<BoxParticle> squares = new HashSet<BoxParticle>();
		HashSet<Bullet> bullets = new HashSet<Bullet>();
		HashSet<ParabolicParticle> parabolics = new HashSet<ParabolicParticle>();
		HashSet<TurningParticle> turns = new HashSet<TurningParticle>();
		HashSet<Bullet> rms = new HashSet<Bullet>();
		HashSet<BoxParticle> rms2 = new HashSet<BoxParticle>();
		HashSet<ParabolicParticle> rms3 = new HashSet<ParabolicParticle>();
		HashSet<TurningParticle> rms4 = new HashSet<TurningParticle>();
		private DrawThread _thread;
		
		//DEFAULT POSITION
		private int _x = 20;
		private int _y = 20;
		
		//BITMAP RELATED VARS
		Matrix matrix = new Matrix();
		Bitmap mcBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mc);
		Bitmap squareBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.square);
		Bitmap parabolicBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.parabolic);
		Bitmap bulletBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bullet);
		Bitmap turningBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.turning);
		private int bulletW, bulletH, squareW, squareH, parabolicW, parabolicH, turningW, turningH;
		private int screenW, screenH;
		
		
		//DEBUG TAGS
		private static final String LOG_TAG = "MaruVortex";
		
		
		private int sq(int x){
			return x*x;
		}
		
		@Override
		public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
			// TODO Auto-generated method stub

		}
		@Override
		public void surfaceCreated(SurfaceHolder arg0) {
			start = SystemClock.elapsedRealtime();
			setKeepScreenOn(true);
			t = SystemClock.elapsedRealtime();
			_thread.setRunning(true);
			_thread.start();
			whitePaint.setColor(Color.WHITE);
			blackPaint.setColor(Color.BLACK);
			redPaint.setColor(Color.RED);
			bulletW = bulletBitmap.getWidth();
			bulletH = bulletBitmap.getHeight();
			squareW = squareBitmap.getWidth();
			squareH = squareBitmap.getHeight();
			parabolicW = parabolicBitmap.getWidth();
			parabolicH = parabolicBitmap.getHeight();
			turningW = turningBitmap.getWidth();
			turningH = turningBitmap.getHeight();
			ColorMatrix cm = new ColorMatrix(mx);
			invert.setColorFilter(new ColorMatrixColorFilter(cm));
			invert.setFlags(Paint.ANTI_ALIAS_FLAG);
			start();

		}
		@Override
		public void surfaceDestroyed(SurfaceHolder arg0) {
			stop();
			_thread.setRunning(false);
			while(true) {
				try {
					_thread.join();
					break;
				}
				catch (InterruptedException e) {

				}
			}

		}
		public Panel(Context context) {

			super(context);
			getHolder().addCallback(this);

			_thread = new DrawThread(getHolder(), this);
			setFocusable(true);
			// TODO Auto-generated constructor stub
		}
		
		public void start() {
            mSensorManager.registerListener(this, mRotationVectorSensor, 10000);
        }

        public void stop() {
            mSensorManager.unregisterListener(this);
        }
        
        @Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
        	Log.v(LOG_TAG, "Accuracy changed to " + accuracy);
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
				Log.d(LOG_TAG, "Sensor values: " + event.values[0] + ", " + event.values[1] + ", " + event.values[2]);
			}
		}
		
		@Override
		public void onDraw (Canvas canvas){
			if (canvas==null) return;
			
			if(berzerk) {
				bitmapPaint = invert;
				textPaint = blackPaint;
				canvas.drawColor(Color.WHITE);
			}
			else {
				bitmapPaint = aA;
				textPaint = whitePaint;
				canvas.drawColor(Color.BLACK);
			}
			
			screenW = canvas.getWidth();
			screenH = canvas.getHeight();
			long nt = SystemClock.elapsedRealtime();
			if (t < 0) t=nt;
			//canvas.drawColor(Color.BLACK);
			
			if(nt-g >= 100) {
				bullets.add(new Bullet(_x, _y, 100, 100, screenH, screenW));
				g = nt;
			}
			if(nt-f >= 1000) {
				if(level < 2)
					squares.add(new BoxParticle(r, screenH, screenW));
				else if(level < 3){
					parabolics.add(new ParabolicParticle(r, screenH, screenW));
					turns.add(new TurningParticle(r, screenH, screenW));
				}
				else
					turns.add(new TurningParticle(r, screenH, screenW));
				f = nt;
			}

			rms.clear();
			rms2.clear();
			rms3.clear();
			rms4.clear();
			for (Bullet i : bullets)
				if (!i.onscreen()) rms.add(i);
			for (BoxParticle i : squares)
				if(!i.onscreen()) rms2.add(i);
			for (ParabolicParticle i : parabolics)
				if(!i.onscreen()) rms3.add(i);
			for (TurningParticle i : turns)
				if(!i.onscreen()) rms4.add(i);
			bullets.removeAll(rms);
			squares.removeAll(rms2);
			parabolics.removeAll(rms3);
			turns.removeAll(rms4);
			rms.clear();
			rms2.clear();
			rms3.clear();
			rms4.clear();
			
			for (Bullet i : bullets) {
				for (BoxParticle j : squares) {
					if(sq(i.getx()-j.getx())+sq(i.gety()-j.gety())<=sq(i.getRadius()+j.getRadius())) {
						rms.add(i);
						rms2.add(j);
					    score++;
					}

				}
				for (ParabolicParticle j : parabolics) {
					if(sq(i.getx()-j.getx())+sq(i.gety()-j.gety())<=sq(i.getRadius()+j.getRadius())) {
						rms.add(i);
						rms3.add(j);
					    score++;
					}
				}
				for (TurningParticle j : turns) {
					if(sq(i.getx()-j.getx())+sq(i.gety()-j.gety())<=sq(i.getRadius()+j.getRadius())) {

						rms.add(i);
						rms4.add(j);
					    score++;
					}
				}
			}
			squares.removeAll(rms2);
			parabolics.removeAll(rms3);
			turns.removeAll(rms4);
			
			bullets.removeAll(rms);
			
			
			
			for (BoxParticle i : squares) {
				i.update(((double)(nt-t))/1000);
				matrix.setRotate(i.getAngle(),squareW/2,squareH/2);
				matrix.postTranslate(i.getx()-squareW/2, i.gety()-squareH/2);
				canvas.drawBitmap(squareBitmap, matrix, bitmapPaint);
			}
			for (ParabolicParticle i : parabolics) {
				i.update(((double)(nt-t))/1000);
				matrix.setRotate(i.getAngle(),parabolicW/2,parabolicH/2);
				matrix.postTranslate(i.getx()-parabolicW/2, i.gety()-parabolicH/2);
				canvas.drawBitmap(parabolicBitmap, matrix, bitmapPaint);
			}
			for (TurningParticle i : turns) {
				i.update(((double)(nt-t))/1000);
				matrix.setRotate(i.getAngle(),turningW/2,turningH/2);
				matrix.postTranslate(i.getx()-turningW/2, i.gety()-turningH/2);
				canvas.drawBitmap(turningBitmap, matrix, bitmapPaint);
			}

			for(Bullet i : bullets) {
				i.update(((double)(nt-t))/1000);


				matrix.setRotate(i.getAngle(),bulletW/2,bulletH/2);
				matrix.postTranslate(i.getx()-bulletW/2, i.gety()-bulletH/2);
				canvas.drawBitmap(bulletBitmap, matrix, bitmapPaint);
			}

			canvas.drawBitmap(mcBitmap, _x-mcBitmap.getWidth()/2, _y-mcBitmap.getHeight()/2, bitmapPaint);
			if(score >= 10){
				level = 2;
			}
			canvas.drawText("Score: " + score, 50, 25, textPaint);
			canvas.drawText("Level: " + level, screenW - 75, 25, textPaint);
			
			if(berzerk)
				canvas.drawText("BERZERK MODE ACTIVATED", screenW/2-100, 25, redPaint);
			
			
			t = nt;
			
		}

		@Override
		public boolean onTouchEvent (MotionEvent event){
			_x = (int) event.getX();
			_y = (int) event.getY();
			return true;
		}

		
	}
	
	class DrawThread extends Thread{
		private SurfaceHolder _surfaceHolder;
		private Panel _panel;
		private boolean _run = false;
		Bitmap screen = Bitmap.createBitmap(800, 480, Bitmap.Config.ARGB_8888);
		public DrawThread(SurfaceHolder surfaceHolder, Panel panel){
			_surfaceHolder = surfaceHolder;
			_panel = panel;

		}
		public void setRunning(boolean run){
			_run = run;
		}
		@Override
		public void run() {
			Canvas c = null;
			while(_run) {
				c = null;
				try {
					c = _surfaceHolder.lockCanvas();
					synchronized(_surfaceHolder) {
						_panel.onDraw(c);
					}
				}
				finally {
					if(c != null)
						_surfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}
	}

}


