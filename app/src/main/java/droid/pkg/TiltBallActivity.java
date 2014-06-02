package droid.pkg;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;

import java.util.Timer;
import java.util.TimerTask;


public class TiltBallActivity extends Activity {
	
	BallView mBallView = null;
	Handler RedrawHandler = new Handler(); //so redraw occurs in main thread
	Timer mTmr = null;
	TimerTask mTsk = null;
	int mScrWidth, mScrHeight;
    android.graphics.PointF mBallPos, mBallSpd;
    FrameLayout mainView = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE); //hide title bar
        getWindow().setFlags(0xFFFFFFFF,
        		LayoutParams.FLAG_FULLSCREEN|LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //create pointer to main screen
        mainView = (android.widget.FrameLayout) findViewById(R.id.main_view);

        //get screen dimensions
        Display display = getWindowManager().getDefaultDisplay();  
        mScrWidth = display.getWidth(); 
        mScrHeight = display.getHeight();


    	mBallPos = new android.graphics.PointF();
    	mBallSpd = new android.graphics.PointF();

        
        //create variables for ball position and speed
        mBallPos.x = mScrWidth/2; 
        mBallPos.y = mScrHeight/2;
        mBallSpd.x = 0;
        mBallSpd.y = 0;


        
        //create initial ball
        mBallView = new BallView(this,mBallPos.x,mBallPos.y,5, 0xFF00FF00);
                
        mainView.addView(mBallView); //add ball to main screen
        mBallView.invalidate(); //call onDraw in BallView

        //listener for accelerometer, use anonymous class for simplicity
                ((SensorManager) getSystemService(Context.SENSOR_SERVICE)).registerListener(
                        new SensorEventListener() {
                            @Override
                            public void onSensorChanged(SensorEvent event) {
                                //set ball speed based on phone tilt (ignore Z axis)
                                mBallSpd.x = -event.values[0];
                                mBallSpd.y = event.values[1];

                                //timer event will redraw ball
                            }

                            @Override
                            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                            } //ignore this event
                        },
                        ((SensorManager) getSystemService(Context.SENSOR_SERVICE))
                                .getSensorList(Sensor.TYPE_ACCELEROMETER).get(0), SensorManager.SENSOR_DELAY_NORMAL
                );
        		
        //listener for touch event 
        mainView.setOnTouchListener(new android.view.View.OnTouchListener() {
	        public boolean onTouch(android.view.View v, android.view.MotionEvent e) {
	        	//set ball position based on screen touch
	        	mBallPos.x = e.getX();
	        	mBallPos.y = e.getY();
    			//timer event will redraw ball
	        	return true;
	        }}); 
    } //OnCreate


    //listener for menu button on phone
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Exit"); //only one menu item
        return super.onCreateOptionsMenu(menu);
    }
    
    //listener for menu item clicked
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	// Handle item selection    
    	if (item.getTitle() == "Exit") //user clicked Exit
    		finish(); //will call onPause
   		return super.onOptionsItemSelected(item);    
    }
    
    //For state flow see http://developer.android.com/reference/android/app/Activity.html
    @Override
    public void onPause() //app moved to background, stop background threads
    {
    	mTmr.cancel(); //kill\release timer (our only background thread)
    	mTmr = null;
    	mTsk = null;
    	super.onPause();
    }
    
    @Override
    public void onResume() //app moved to foreground (also occurs at app startup)
    {
        //create timer to move ball to new position
        mTmr = new Timer(); 
        mTsk = new TimerTask() {
			public void run() {
				//if debugging with external device, 
				//  a cat log viewer will be needed on the device
				android.util.Log.d(
				    "TiltBall","Timer Hit - " + mBallPos.x + ":" + mBallPos.y);
			    //move ball based on current speed
				mBallPos.x += mBallSpd.x;
				mBallPos.y += mBallSpd.y;

				//if ball goes off screen, reposition to opposite side of screen
				if (mBallPos.x > mScrWidth) mBallPos.x=0;
				if (mBallPos.y > mScrHeight) mBallPos.y=0;
				if (mBallPos.x < 0) mBallPos.x=mScrWidth;
				if (mBallPos.y < 0) mBallPos.y=mScrHeight;

				//update ball class instance
				mBallView.mX = mBallPos.x;
				mBallView.mY = mBallPos.y;




                // when you come back to looking at updating background color
                // you'll do it here.

                // Color
                //        *     hsv[0] is Hue [0 .. 360)
                //        *     hsv[1] is Saturation [0...1]
                //        *     hsv[2] is Value [0...1]
                // use this for reference if ^^ doesn't make sense http://hslpicker.com/#ff0a0a
                //  mainView.setBackgroundColor(0xFF00FF00);

//                float[] hsv = new float[3];
//                hsv[0] = 50f; // color
//                hsv[1] = 1f; // how close to grey, should probably be 1.0
//                hsv[2] = 0.5f; // how close to white(1) or black(0),  could perhaps
//                // calculate it based on proximity to ends. cen
//                int color = Color.HSVToColor(1, hsv);
//                mainView.setBackgroundColor(color);



				//redraw ball. Must run in background thread to prevent thread lock.
				RedrawHandler.post(new Runnable() {
				    public void run() {
                        mBallView.invalidate();

				  }});
			}}; // TimerTask

        mTmr.schedule(mTsk,10,10); //start timer
        super.onResume();
    } // onResume
    
    @Override
    public void onDestroy() //main thread stopped
    {
    	super.onDestroy();
    	System.runFinalizersOnExit(true); //wait for threads to exit before clearing app
    	android.os.Process.killProcess(android.os.Process.myPid());  //remove app from memory 
    }
    
    //listener for config change. 
    //This is called when user tilts phone enough to trigger landscape view
    //we want our app to stay in portrait view, so bypass event 
    @Override 
    public void onConfigurationChanged(Configuration newConfig)
	{
       super.onConfigurationChanged(newConfig);
	}

}