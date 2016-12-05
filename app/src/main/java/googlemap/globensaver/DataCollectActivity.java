package googlemap.globensaver;

import android.graphics.Color;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;

import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

public class DataCollectActivity extends AppCompatActivity implements SensorEventListener {

    SensorManager mSensorManager;
    Sensor mAccelerometer;
    FileOutputStream writer;
    BufferedReader reader;
    LinkedList<Double> magnitudeList;
    int index = 0;
    String transport = "";

    Button walk_button;
    Button run_button;
    Button bike_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collect);

        walk_button = (Button) findViewById(R.id.walk_button);
        run_button = (Button) findViewById(R.id.run_button);
        bike_button = (Button) findViewById(R.id.bike_button);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnitudeList = new LinkedList<>();

        // keeps on CPU but allows screen to turnoff.
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();
    }


    public void collectWalkData(View view){
        walk_button.setBackgroundColor(Color.GREEN);
        collectTransportData(view, "walk");
    }

    public void collectRunData(View view){
        run_button.setBackgroundColor(Color.GREEN);
        collectTransportData(view, "run");
    }

    public void collectBikeData(View view){
        bike_button.setBackgroundColor(Color.GREEN);
        collectTransportData(view, "bike");
    }

    public void collectTransportData(View view, String transportMode){
        transport = transportMode;
        collectData("transport.csv");
    }

    public void stopCollectData(View view){
        walk_button.setBackgroundColor(Color.LTGRAY);
        run_button.setBackgroundColor(Color.LTGRAY);
        bike_button.setBackgroundColor(Color.LTGRAY);

        mSensorManager.unregisterListener(this);
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void collectData(String name){
        try {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            if (writer != null) {
                writer.close();
            }
            if (isExternalStorageWritable()) {
                File ourFile = getStorageFile(getApplicationContext(), name);
                writer = new FileOutputStream(ourFile, true);
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(ourFile)));
            } else Log.e("GlobeSaver", "Could not write to external storage");

        } catch (IOException e){
            e.printStackTrace();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double x = sensorEvent.values[0];
        double y = sensorEvent.values[1];
        double z = sensorEvent.values[2];
        magnitudeList.add(Math.sqrt(x*x + y*y + z*z));
        index += 1;
        if (index >= 128) {
            double min = Collections.min(magnitudeList);
            double max = Collections.max(magnitudeList);
            double stdDev = stdDev(magnitudeList);
            index = 64;

            for (double i: magnitudeList){
                Log.d("GlobeSaver", "value: " + i);
            }

            for (int i = 0; i < index; i++){
                magnitudeList.remove();
            }

            try  {
                String output = stdDev + "," + min + "," + max + "," + transport + "\n";
                writer.write(output.getBytes());
                Log.d("GlobeSaver", reader.readLine());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    private double stdDev(LinkedList<Double> list) {
        double mean = mean(list);
        double sumOfDiffToMeanSq = 0;
        double length = list.size();

        for(double i: list){
            double diffToMeanSq = Math.pow(i - mean, 2);
            sumOfDiffToMeanSq += diffToMeanSq;
        }
        double meanOfDiffs = sumOfDiffToMeanSq / length;
        return Math.sqrt(meanOfDiffs);
    }

    private double mean(LinkedList<Double> array) {

        double sum = 0;
        int length = array.size();

        for (double i: array) {
            sum += i;
        }

        return sum/length;
    }
    /* taken from android tutorials */
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* taken from android tutorials */
    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    /* taken from android tutorials */
    public File getStorageFile(Context context, String name) {
        // Get the directory for the app's private pictures directory.
        File directory = new File(context.getExternalFilesDir(
                null), "files");
        Log.e("GlobeSaver", directory.getAbsolutePath());
        if (!directory.mkdirs()) {
            Log.e("GlobeSaver", "Directory not created");
        }
        return new File(directory, name);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
