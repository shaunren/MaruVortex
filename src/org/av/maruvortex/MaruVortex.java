package org.av.maruvortex;

import android.os.Bundle;
import android.app.Activity;



import java.util.*;
import android.os.SystemClock;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;



public class MaruVortex extends Activity  {

	boolean accelerometerAvailable = false;
    boolean isEnabled = false;
    float x, y, z;
		

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(new Panel(this));
		/*
		sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		*/

	}



}
class Panel extends SurfaceView implements SurfaceHolder.Callback{
	//GAME VARS
	int level = 1;
	int score = 0;
	Random r = new Random();
	
	//PAINT VARS
	Paint whitePaint = new Paint();
	Paint aA = new Paint(Paint.ANTI_ALIAS_FLAG);
	
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
		bulletW = bulletBitmap.getWidth();
		bulletH = bulletBitmap.getHeight();
		squareW = squareBitmap.getWidth();
		squareH = squareBitmap.getHeight();
		parabolicW = parabolicBitmap.getWidth();
		parabolicH = parabolicBitmap.getHeight();
		turningW = turningBitmap.getWidth();
		turningH = turningBitmap.getHeight();

	}
	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
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
	@Override
	public void onDraw (Canvas canvas){
		
		screenW = canvas.getWidth();
		screenH = canvas.getHeight();
		if(canvas==null) return;
		long nt = SystemClock.elapsedRealtime();
		if (t < 0) t=nt;
		canvas.drawColor(Color.BLACK);
		
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
				//Log.d(AVTAG, i.getx() + " " + i.gety() + " " + j.getx() + " " + j.gety());
				//Log.d(AVTAG, Math.sqrt(sq(i.getx()-j.getx())+sq(i.gety()-j.gety())) + " " + (i.getRadius()+j.getRadius()));
				if(sq(i.getx()-j.getx())+sq(i.gety()-j.gety())<=sq(i.getRadius()+j.getRadius())) {
					rms.add(i);
					rms2.add(j);
				    score++;
				}

			}
		}

		squares.removeAll(rms2);
		for (Bullet i : bullets) {
			for (ParabolicParticle j : parabolics) {
				//Log.d(AVTAG, i.getx() + " " + i.gety() + " " + j.getx() + " " + j.gety());
				//Log.d(AVTAG, Math.sqrt(sq(i.getx()-j.getx())+sq(i.gety()-j.gety())) + " " + (i.getRadius()+j.getRadius()));
				if(sq(i.getx()-j.getx())+sq(i.gety()-j.gety())<=sq(i.getRadius()+j.getRadius())) {

					rms.add(i);
					rms3.add(j);
				    score++;
				}
			}
		}

		parabolics.removeAll(rms3);
		for (Bullet i : bullets) {
			for (TurningParticle j : turns) {
				//Log.d(AVTAG, i.getx() + " " + i.gety() + " " + j.getx() + " " + j.gety());
				//Log.d(AVTAG, Math.sqrt(sq(i.getx()-j.getx())+sq(i.gety()-j.gety())) + " " + (i.getRadius()+j.getRadius()));
				if(sq(i.getx()-j.getx())+sq(i.gety()-j.gety())<=sq(i.getRadius()+j.getRadius())) {

					rms.add(i);
					rms4.add(j);
				    score++;
				}
			}
		}
		turns.removeAll(rms4);
		
		
		bullets.removeAll(rms);
		
		
		
		for (BoxParticle i : squares) {
			i.update(((double)(nt-t))/1000);
			matrix.setRotate(i.getAngle(),squareW/2,squareH/2);
			matrix.postTranslate(i.getx()-squareW/2, i.gety()-squareH/2);
			//Log.d(AVTAG, "Position: " + i.getx() + ", " + i.gety());
			canvas.drawBitmap(squareBitmap, matrix, aA);
			//canvas.drawBitmap(square, i.getx()-testWidth>>1, i.gety()-testHeight>>1, null);				
			//Log.d(AVTAG, Double.toString(Math.atan2(screenH/2 - _y,  screenW/2 - _x)));
		}
		for (ParabolicParticle i : parabolics) {
			i.update(((double)(nt-t))/1000);
			matrix.setRotate(i.getAngle(),parabolicW/2,parabolicH/2);
			matrix.postTranslate(i.getx()-parabolicW/2, i.gety()-parabolicH/2);
			canvas.drawBitmap(parabolicBitmap, matrix, aA);
		}
		for (TurningParticle i : turns) {
			i.update(((double)(nt-t))/1000);
			matrix.setRotate(i.getAngle(),turningW/2,turningH/2);
			matrix.postTranslate(i.getx()-turningW/2, i.gety()-turningH/2);
			canvas.drawBitmap(turningBitmap, matrix, aA);
		}

		for(Bullet i : bullets) {
			i.update(((double)(nt-t))/1000);


			matrix.setRotate(i.getAngle(),bulletW/2,bulletH/2);
			matrix.postTranslate(i.getx()-bulletW/2, i.gety()-bulletH/2);
			//Log.d(AVTAG, ""+ i.getAngle());
			//if (i.id==15)
				//Log.d(AVTAG, "Position:" + i.getx() + " " + i.gety());
			canvas.drawBitmap(bulletBitmap, matrix, aA);
			//Log.d(AVTAG,""+ i.getAngle());
		}

		canvas.drawBitmap(mcBitmap, _x-mcBitmap.getWidth()/2, _y-mcBitmap.getHeight()/2, null);
		if(score >= 10){
			level = 2;
		}
		canvas.drawText("Score: " + score, 50, 25, whitePaint);
		canvas.drawText("Level: " + level, screenW - 75, 25, whitePaint);
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
	public DrawThread(SurfaceHolder surfaceHolder, Panel panel){
		_surfaceHolder = surfaceHolder;
		_panel = panel;

	}
	public void setRunning(boolean run){
		_run = run;
	}
	@Override
	public void run() {
		Canvas c;
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
