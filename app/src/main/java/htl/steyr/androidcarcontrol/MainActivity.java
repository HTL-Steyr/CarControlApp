package htl.steyr.androidcarcontrol;

import android.content.Intent;
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
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import htl.steyr.androidcarcontrol.socket.CarSocketConnection;
import htl.steyr.androidcarcontrol.socket.ICarControlSubscriber;
import htl.steyr.androidcarcontrol.socket.ICarMessage;

public class MainActivity extends AppCompatActivity implements ICarControlSubscriber {

    Thread myThread = null;
    public static CarSocketConnection carSocket = null;
    private long lastUpdate = 0;
    private long lastUpdate_rotation = 0;

    SensorManager sensorManager;

    Sensor rotationVectorSensor;

    SensorEventListener rvListener;

    boolean switchChecked = false;
    boolean movementButtonPressed = false;

    boolean orientationNegative;
    boolean orientationSet = false;


    String state = "S";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        rotationVectorSensor =
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        View.OnClickListener startListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                myThread = new Thread() {
                    @Override
                    public void run() {
                        EditText hostTextField = findViewById(R.id.hostTextField);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hostTextField.setEnabled(false);
                            }
                        });

                        carSocket = new CarSocketConnection(hostTextField.getText().toString(), 2612);

                        ICarControlSubscriber sub = new ICarControlSubscriber() {
                            @Override
                            public void messageReceived(ICarMessage msg) {
                                System.out.println(msg.getMessage());
                            }
                        };

                        carSocket.addSubscriber(sub);

                        findViewById(R.id.goToGyroButton).setEnabled(true);
                    }
                };

                myThread.start();
            }
        };

        Button connectButton = findViewById(R.id.connectButton);
        connectButton.setOnClickListener(startListener);

        View.OnTouchListener touch = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (System.currentTimeMillis() - lastUpdate > 100) {
                    lastUpdate = System.currentTimeMillis();
                    SeekBar right = findViewById(R.id.rightSeekBar);
                    SeekBar left = findViewById(R.id.leftSeekBar);

                    if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        int rightVal = right.getProgress() - 100;
                        int leftVal = left.getProgress() - 100;

                        leftVal = leftVal / 10;
                        leftVal = leftVal * 10;
                        rightVal = rightVal / 10;
                        rightVal = rightVal * 10;

                        // System.out.println(command);
                        if (carSocket != null) {
                            carSocket.sendMessage("D;" + rightVal + ";" + leftVal);
                        }
                    }
                }
                return false;
            }
        };

        SeekBar s = findViewById(R.id.rightSeekBar);
        s.setOnTouchListener(touch);
        s = findViewById(R.id.leftSeekBar);
        s.setOnTouchListener(touch);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String command = "";
                switch (v.getId()) {
                    // @ToDo: Um zur funkionierenden Originalfunktionalität zurückzukehren,
                    // den folgenden Kommentar auflösen.
                    // Dieser Code enthält die Reaktion auf Button-Presses

                    case R.id.forwardButton:
                        command = "F";
                        break;
                    case R.id.backwardButton:
                        command = "B";
                        break;
                    case R.id.leftButton:
                        command = "L";
                        break;
                    case R.id.rightButton:
                        command = "R";
                        break;
                    case R.id.stopButton:
                        command = "S";
                        SeekBar right = findViewById(R.id.rightSeekBar);
                        SeekBar left = findViewById(R.id.leftSeekBar);
                        right.setProgress(100);
                        left.setProgress(100);
                        break;
                }

                // System.out.println(command);
                if (carSocket != null) {
                    carSocket.sendMessage(command);
                }
            }
        };

        // @ToDo: Um zur funkionierenden Originalfunktionalität zurückzukehren, den folgenden Code
        // auskommentieren.
        // Dieser Code regelt aufkommende Touch-Events, es kann erkannt werden, ob der Finger
        // angesetzt wurde oder wieder aufgehoben wurde

        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent motionEvent) {
                movementButtonPressed = true;
                if (motionEvent.getAction() == MotionEvent.ACTION_UP){
                    movementButtonPressed = false;
                    state = "S";

                    if (carSocket != null) {
                        carSocket.sendMessage("S");
                    }

                    return true;
                } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    switch (v.getId()) {
                        case R.id.rightButton:
                            state = "F";
                            break;
                        case R.id.leftButton:
                            state = "B";
                            break;
                    }

                    orientationSet = false;

                    return true;
                }

                return false;
            }
        };

        Button btn = findViewById(R.id.rightButton);
        btn.setOnClickListener(listener);
        btn = findViewById(R.id.leftButton);
        btn.setOnClickListener(listener);
        btn = findViewById(R.id.forwardButton);
        btn.setOnClickListener(listener);
        btn = findViewById(R.id.backwardButton);
        btn.setOnClickListener(listener);
        btn = findViewById(R.id.stopButton);
        btn.setOnClickListener(listener);


        Switch modeSwitch = findViewById(R.id.modeSwitch);

        if (modeSwitch != null) {
            modeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                    switchChecked = isChecked;

                    if (carSocket != null) {
                        carSocket.sendMessage("S");
                    }
                }
            });
        }

        View.OnClickListener goToGyroListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent switchIntent = new Intent(MainActivity.this, GyroControlActivity.class);

                EditText hostTextField = findViewById(R.id.hostTextField);
                switchIntent.putExtra("carSocket", hostTextField.getText().toString());

                startActivity(switchIntent);
            }
        };

        findViewById(R.id.goToGyroButton).setOnClickListener(goToGyroListener);
    }

    @Override
    public void messageReceived(ICarMessage msg) {
        System.out.println(msg.getMessage());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Create a listener
        SensorEventListener rvListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (switchChecked && System.currentTimeMillis() - lastUpdate_rotation > 100) {
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


                    // Regelung der Bewegungssteuerung

                    // @ToDo: Um zur funkionierenden Originalfunktionalität zurückzukehren,
                    // den folgenden Kommentar auflösen.

                    /*float tilt = orientations[1];

                    if (tilt >= 0 && tilt <= 90) {
                        int forward_value = (int) (((tilt - 45) / 45) * 100);

                        if (carSocket != null) {
                            carSocket.sendMessage("D;" + forward_value + ";" + forward_value);
                        }
                    }*/



                    // @ToDo: Um zur funkionierenden Originalfunktionalität zurückzukehren,
                    // den folgenden Code auskommentieren

                    /*
                    float tilt_left_right = orientations[2];

                    if (tilt_left_right >= -45 && tilt_left_right <= 45 && !state.equals("S")) {
                        float percentage = (100 - ((Math.abs(tilt_left_right)) / 45) * 100) / 100;

                        int right = 100;
                        int left = 100;
                        if (state.equals("B")) {
                            right *= (-1);
                            left *= (-1);
                        }

                        if (tilt_left_right < 0) {
                            left *= percentage;
                        } else {
                            right *= percentage;
                        }

                        if (carSocket != null) {
                            carSocket.sendMessage("D;" + right + ";" + left);
                        }

                    }
                     */

                    float horizontal_orientation = orientations[2];

                    if(!orientationSet){
                        orientationNegative = orientations[2] < 0;
                        orientationSet = true;

                        /*
                            Wenn Button wieder losgelassen wird soll orientationSet
                            wieder auf false gesetzt werden
                         */
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

                    /**
                     * Ansteuerung an Server übermitteln
                     * Jetzt in richtigen Format
                     */
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


    }

    @Override
    protected void onPause() {
        super.onPause();

        sensorManager.unregisterListener(rvListener);
    }

}
