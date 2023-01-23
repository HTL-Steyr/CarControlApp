package htl.steyr.androidcarcontrol;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import htl.steyr.androidcarcontrol.socket.CarSocketConnection;
import htl.steyr.androidcarcontrol.socket.ICarControlSubscriber;
import htl.steyr.androidcarcontrol.socket.ICarMessage;

public class GyroControlActivity extends AppCompatActivity implements ICarControlSubscriber {

    Thread myThread = null;
    CarSocketConnection carSocket = null;
    private long lastUpdate = 0;
    private long lastUpdate_rotation = 0;

    SensorManager sensorManager;

    Sensor rotationVectorSensor;

    SensorEventListener rvListener;

    boolean orientationNegative;
    boolean orientationSet = false;


    String state = "S";

    double speed = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gyro_control);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        rotationVectorSensor =
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        // Sollte eigentlich auch funktionieren, tuts aber nicht
        /*
        String host = "";
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            host = extras.getString("carSocket");
        }else {
            host = (String) savedInstanceState.getSerializable("carSocket");
        }

        System.out.println(host);
        carSocket = new CarSocketConnection(host, 2612);
        */

        carSocket = MainActivity.carSocket;

        ICarControlSubscriber sub = new ICarControlSubscriber() {
            @Override
            public void messageReceived(ICarMessage msg) {
                // System.out.println(msg.getMessage());
            }
        };

        carSocket.addSubscriber(sub);
    }

    @Override
    public void messageReceived(ICarMessage msg) {
        System.out.println(msg.getMessage());
    }

    @Override
    protected void onResume() {
        super.onResume();

        View.OnTouchListener touch = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (System.currentTimeMillis() - lastUpdate > 100) {
                    lastUpdate = System.currentTimeMillis();
                    SeekBar speedSeekBar = findViewById(R.id.speedSeekBar);

                    if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        speed = (speedSeekBar.getProgress() - 100) / (double) 100;
                    }
                }
                return false;
            }
        };

        SeekBar s = findViewById(R.id.speedSeekBar);
        s.setOnTouchListener(touch);

        /**
         * Im Moment kann das Auto noch nicht nur vorwärts fahren, 100 - orientation
         */
        // Create a listener
        SensorEventListener rvListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (System.currentTimeMillis() - lastUpdate_rotation > 50) {
                    lastUpdate_rotation = System.currentTimeMillis();

                    float[] rotationMatrix = new float[16];
                    SensorManager.getRotationMatrixFromVector(
                            rotationMatrix, sensorEvent.values);

                    // Remap coordinate system
                    float[] remappedRotationMatrix = new float[16];
                    SensorManager.remapCoordinateSystem(rotationMatrix,
                            SensorManager.AXIS_X,
                            SensorManager.AXIS_Z,
                            remappedRotationMatrix);

                    // Convert to orientations
                    float[] orientations = new float[3];
                    SensorManager.getOrientation(remappedRotationMatrix, orientations);

                    // convert radians to degree
                    for (int i = 0; i < 3; i++) {
                        orientations[i] = (float) (Math.toDegrees(orientations[i]));
                    }

                    float horizontal_orientation = orientations[2];

                    if(!orientationSet){
                        orientationNegative = orientations[2] < 0;
                        orientationSet = true;
                    }

                    if(orientationNegative){
                        if(horizontal_orientation > 0){
                            if(horizontal_orientation < 90){
                                horizontal_orientation = 0;
                            }else{
                                horizontal_orientation = -180;
                            }
                        }
                        horizontal_orientation += 180;
                    }else{
                        if(horizontal_orientation < 0){
                            if(horizontal_orientation > -90){
                                horizontal_orientation = 0;
                            }else{
                                horizontal_orientation = 180;
                            }
                        }
                    }

                    /**
                     * Egal wie handy gedreht ist:
                     * voller links-Einschlag ist 0
                     * voller rechts-Einschlag ist 180
                     */

                    float left = 0;
                    float right = 0;

                    if(horizontal_orientation < 90){
                         left = (horizontal_orientation/90)*100;
                         right = 100;
                    }else{
                         left = 100;
                         right = 100 - ((horizontal_orientation)/180)*100;
                    }

                    left = (float) (left * speed);
                    right = (float) (right * speed);

                    System.out.println("left: " + left + ", right: " + right);

                     if (carSocket != null) {
                        carSocket.sendMessage("D;" + (int) left + ";" + (int) right);
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };

        // Register it
        sensorManager.registerListener(rvListener,
                rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);

        System.out.println(orientationNegative);

        View.OnClickListener stopListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                carSocket.sendMessage("S");
                SeekBar speedSeekBar = findViewById(R.id.speedSeekBar);
                speedSeekBar.setProgress(100);
                speed = 0;
            }
        };

        Button stopButton = findViewById(R.id.stopGyroButton);
        stopButton.setOnClickListener(stopListener);
    }

    @Override
    protected void onPause() {
        super.onPause();

        sensorManager.unregisterListener(rvListener);
    }
}
