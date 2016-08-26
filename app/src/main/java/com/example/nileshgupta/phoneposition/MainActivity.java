package com.example.nileshgupta.phoneposition;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import weka.classifiers.Evaluation;



public class MainActivity extends Activity implements SensorEventListener{

    SensorManager mSensorManager;
    phone_position  mConnectedThread;
    Sensor proximity,light, acceleromerer, mMagnetometer;
    TextView t, t1;
    Vibrator v;
    boolean isSensorPresent;
    public static float vProximity, vLight, vAccelerometer,vGravity ,deltaX, deltaY, deltaZ, lastX, lastY, lastZ;
    final float alpha = (float) 0.8;
    float[] gravity=new float[]{0.0f,0.0f,0.0f};
    PowerManager pm ;
    PowerManager.WakeLock wl;

    float[] inR = new float[16];
    float[] I = new float[16];
    float[] gravity1 = new float[3];
    float[] geomag = new float[3];
    float[] orientVals = new float[3];
    double azimuth = 0;
    double pitch = 0;
    double roll = 0;

    private float   mLimit = (float)10.5;
    private float   mLastValues[] = new float[3*2];
    private float   mScale[] = new float[2];
    private float   mYOffset;

    private float   mLastDirections[] = new float[3*2];
    private float   mLastExtremes[][] = { new float[3*2], new float[3*2] };
    private float   mLastDiff[] = new float[3*2];
    private int     mLastMatch = -1, StepCount=0, LStepCount=0;
    TelephonyManager call;

    public static int phone_state=0;// 1 for ringing and 0 for ideal
    public static int orientation=1; // 1 for portrait and 0 for landscape
    public static int is_step=0; // 1 for step and 0 for not
    public static int counter=0, counter1, State=5, Lstate=5;
    public static int in_Proximity=0;// 0 for false and 1 for near or true





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        call = ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE));
        call.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        PackageManager PM = this.getPackageManager();
        isSensorPresent = PM.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS) ;
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            if(!isSensorPresent){
               Toast.makeText(getApplicationContext(), "Sensor Not Present", Toast.LENGTH_SHORT).show();
            }

        proximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        t = (TextView) findViewById(R.id.Input);
        t1 = (TextView) findViewById(R.id.Display);
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        light = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        acceleromerer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Tag");
        wl.acquire();
        //step count
        int h = 480; // TODO: remove this constant
        mYOffset = h * 0.5f;
        mScale[0] = -(h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
        mScale[1] = -(h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
        phone_position  mConnectedThread = new phone_position();
        mConnectedThread.start();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, acceleromerer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mSensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, acceleromerer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
        wl.acquire();
        if(mConnectedThread!=null)
        mConnectedThread.interrupt();


    }


    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();

        switch (sensorType) {
            case Sensor.TYPE_LIGHT:
                processLightSensorEvent(event.values[0]);
                break;

            case Sensor.TYPE_PROXIMITY:
                processProximitySensorEvent(event.values[0]);
                break;

            case Sensor.TYPE_ACCELEROMETER:
                gravity1 = event.values.clone();
                processAccelerometerSensorEvent(event.values[0], event.values[1], event.values[2], event);

                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                geomag = event.values.clone();
                break;


            default:
			Log.e("TAG", "OTHER SENSOR: " + sensorType);

        }
        postSensorEvent();

    }

    public void processLightSensorEvent(float value) {
        vLight = value;
    }

    public void processProximitySensorEvent(float value) {

       if(value==0){
           in_Proximity=0;

       }else
           in_Proximity=1;
    }

    public void processAccelerometerSensorEvent(float x, float y, float z, SensorEvent event) {

       // gravity
        gravity[0] = alpha * gravity[0] + (1 - alpha) * x;
        gravity[1] = alpha * gravity[1] + (1 - alpha) * y;
        gravity[2] = alpha * gravity[2] + (1 - alpha) * z;
        deltaX = Math.abs(lastX - x);
        deltaY = Math.abs(lastY - y);
        deltaZ = Math.abs(lastZ - z);
        float a = (float) Math.sqrt(Math.pow(deltaX,2)+Math.pow(deltaY,2)+Math.pow(deltaZ,2));
        if(a>0.2)
            vAccelerometer =a;
        else
            vAccelerometer=0;
        vGravity = (float) Math.sqrt(Math.pow(gravity[0], 2) + Math.pow(gravity[1], 2) + Math.pow(gravity[2], 2));
        lastX = x;
        lastY = y;
        lastZ = z;


       //step detector
        synchronized (this) {
            int j = 1;
            if (j == 1) {
                float vSum = 0;
                for (int i = 0; i < 3; i++) {
                    final float v = mYOffset + event.values[i] * mScale[j];
                    vSum += v;
                }

                int k = 0;
                float v = vSum / 3;

                float direction = (v > mLastValues[k] ? 1 : (v < mLastValues[k] ? -1 : 0));
                if (direction == -mLastDirections[k]) {
                    // Direction changed
                    int extType = (direction > 0 ? 0 : 1); // minumum or maximum?
                    mLastExtremes[extType][k] = mLastValues[k];
                    float diff = Math.abs(mLastExtremes[extType][k] - mLastExtremes[1 - extType][k]);

                    if (diff > mLimit) {

                        boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k] * 2 / 3);
                        boolean isPreviousLargeEnough = mLastDiff[k] > (diff / 3);
                        boolean isNotContra = (mLastMatch != 1 - extType);

                        if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra) {
                            StepCount++;

                            mLastMatch = extType;
                        } else {
                            mLastMatch = -1;
                        }
                    }
                    mLastDiff[k] = diff;
                }
                mLastDirections[k] = direction;
                mLastValues[k] = v;
            }

        }
    }


    public void postSensorEvent() {

      // step detect
        if(StepCount>LStepCount)
        {
            LStepCount=StepCount;
            is_step=1;
            counter=100;
        }
        else{
            counter--;
            if(counter<0)
                is_step=0;
        }


       //orientation
        if (gravity1 != null && geomag != null) {
            // checks that the rotation matrix is found
            boolean success = SensorManager.getRotationMatrix(inR, I,
                    gravity1, geomag);
            if (success) {
                SensorManager.getOrientation(inR, orientVals);
                azimuth = Math.abs(Math.toDegrees(orientVals[0]));
                pitch = Math.abs(Math.toDegrees(orientVals[1]));
                roll = Math.abs(Math.toDegrees(orientVals[2]));
            }
        }
        if(pitch>40){
            orientation = 1;
        }else if(roll>pitch&&roll>50){
            orientation = 0;
        }else if(pitch<10&&roll<10){
                orientation = 2;

        }


        t.setText("Proximity: " + Integer.toString(in_Proximity) + "\n" + "Gravity: "
                + Float.toString(vGravity) + "\n" + "Accelerometer: " +
                Float.toString(vAccelerometer) + "\n" + "Light: " + Float.toString(vLight) + "\n" +"Pitch : "+
                pitch + "\n" + "Roll : "+roll );

        if(Lstate!=State){
            Lstate=State;
            v.vibrate(200);
        }


        if(State==0){
            t1.setText("On Call");
        }else if(State==1){
            t1.setText("Ideal");
        }else if(State==2){
            t1.setText("In Car");
        }else if(State==3){
            t1.setText("Pocket");
        }else if(State==4){
            t1.setText("In Hand");
        }else if(State==5){
            t1.setText("In hand/bag");
        }else if(State==6){
            t1.setText("Random");
        }

    }

    PhoneStateListener callStateListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber)
        {
            if(state==TelephonyManager.CALL_STATE_RINGING){
            }
            if(state==TelephonyManager.CALL_STATE_OFFHOOK){

            phone_state = 1;
            }

            if(state==TelephonyManager.CALL_STATE_IDLE){
                phone_state=0;
                Log.e("TAG", "ttttttttttttttttttttttttttttttttttttttt:      7 ");

            }
        }
    };

    public class phone_position extends Thread{

        public void run() {

            while (!Thread.currentThread().isInterrupted()) {


                if (phone_state == 1) {
                    State = 0;  // on call
                    SystemClock.sleep(5000);
                } else if (vAccelerometer == 0) {
                    State = 1;  // ideal
                    SystemClock.sleep(5000);

                }else if(vAccelerometer>20){
                    State = 2;  // car
                    SystemClock.sleep(5000);

                }else if(orientation==1 && is_step==1&&in_Proximity==0){
                    State = 3;  // in Pocket
                    SystemClock.sleep(5000);
                }else if(orientation==0&&is_step==1){
                    if(in_Proximity==0){
                        State = 4;  // in Hand
                        SystemClock.sleep(5000);
                    }else{
                        State = 5; // in hand or bag
                        SystemClock.sleep(5000);
                    }
                }else if(vAccelerometer<15&&vAccelerometer>0 && is_step==0) {
                    State = 6; //random
                    SystemClock.sleep(1000);
                    Log.e("TAG", "ttttttttttttttttttttttttttttttttttttttt: "+ State);

                }
            }

      /* Allow thread to exit */
                try {
                    Thread.currentThread().wait(100);
                    SystemClock.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
            }
        }

        public void cancel() { interrupt(); }


    }

}
















