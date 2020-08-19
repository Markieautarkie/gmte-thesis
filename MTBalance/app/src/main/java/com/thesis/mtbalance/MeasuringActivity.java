package com.thesis.mtbalance;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanSettings;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
import com.xsens.dot.android.sdk.XsensDotSdk;
import com.xsens.dot.android.sdk.events.XsensDotData;
import com.xsens.dot.android.sdk.interfaces.XsensDotDeviceCb;
import com.xsens.dot.android.sdk.interfaces.XsensDotScannerCb;
import com.xsens.dot.android.sdk.models.XsPayload;
import com.xsens.dot.android.sdk.models.XsensDotDevice;
import com.xsens.dot.android.sdk.utils.XsensDotScanner;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class MeasuringActivity extends AppCompatActivity
        implements XsensDotDeviceCb, XsensDotScannerCb {

    // region Variables
    // Finals
    private final int ALL_DOTS = 3;

    // Numericals
    private int mIteration = 0;
    private int mFeedbackMethod;
    private float mThresholdLeniency;
    private float mAnkleLength, mKneeLength;

    // DVs
    private float mBalancePerformance, mCompletionTime;
    private float mBalanceDeviation = 0f;
    private float mResponseTime = 0f;

    // Booleans
    private boolean mMeasuring = false;
    private boolean mBalanced = true;

    // Views
    private LinearLayout mMeasuringLayout;
    private Chronometer mChronometer;

    // Instants
    Instant mStartTime, mStartBalanceTime, mEndBalanceTime;

    // Helpers
    private VecHelper mVecHelper;

    // Xsens
    private XsensDotScanner mDotScanner;
    private ArrayList<XsensDotDevice> mDotList = new ArrayList<>();

    // Hashmaps
    private HashMap<String, String> mAddressTagMap = new HashMap<>();
    private HashMap<String, float[]> mTagQuatMap = new HashMap<>();
    // endregion

    /**
     * Called on activity creation.
     *
     * @param savedInstanceState - state holding saved info from onPause callback.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measuring);

        // Initialize helpers
        mVecHelper = new VecHelper(this);

        // Get the shared preferences
        retrieveSharedPreferences();

        // Initialize the SDK
        initXsensSdk();
    }

    /**
     * Retrieves the used shared preferences.
     */
    private void retrieveSharedPreferences() {
        // Create a shared preferences object
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // Get the preferred feedback method
        mFeedbackMethod = Integer.parseInt(Objects.requireNonNull(sharedPref.getString
                (SettingsActivity.KEY_PREFERRED_FEEDBACK, "0")));

        // Get the preferred threshold leniency
        mThresholdLeniency = Float.parseFloat(Objects.requireNonNull(sharedPref.getString
                (SettingsActivity.KEY_THRESHOLD_LENIENCY, "50")));

        // Get the leg lengths
        mAnkleLength = Float.parseFloat(Objects.requireNonNull(sharedPref.getString
                (SettingsActivity.KEY_LOWER_LEG_LENGTH, "0")));
        mKneeLength = Float.parseFloat(Objects.requireNonNull(sharedPref.getString
                (SettingsActivity.KEY_UPPER_LEG_LENGTH, "0")));
    }

    /**
     * Initializes the DOT SDK and starts a scan.
     */
    private void initXsensSdk() {
        // Set global SDK options
        XsensDotSdk.setDebugEnabled(true);  // Todo: remove debugger when application is finished.
        XsensDotSdk.setReconnectEnabled(true);

        // Initialize views
        mMeasuringLayout = findViewById(R.id.measuring_layout);
        mChronometer = findViewById(R.id.chronometer);

        // Initialize scanner object and start scan
        mDotScanner = new XsensDotScanner(getApplicationContext(), this);
        mDotScanner.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        mDotScanner.startScan();
    }

    /**
     * Callback function which triggers when a DOT is scanned.
     *
     * @param bluetoothDevice - the currently scanned DOT.
     */
    @Override
    public void onXsensDotScanned(BluetoothDevice bluetoothDevice) {
        // Get the address, return if the device is already initialized
        String address = bluetoothDevice.getAddress();
        if (mAddressTagMap.containsKey(address))
            return;

        // Add the current address to the address-tag hashmap, depending on MAC-address
        switch (address) {
            case "D4:CA:6E:F1:7D:D9":   // Bike
                mAddressTagMap.put(address, "Bike DOT");
                break;
            case "D4:CA:6E:F1:66:AA":   // Ankle
                mAddressTagMap.put(address, "Ankle DOT");
                break;
            case "D4:CA:6E:F1:63:D0":   // Knee
                mAddressTagMap.put(address, "Knee DOT");
                break;
            default:
                return;
        }

        // Initialize address with a base quaternion
        mTagQuatMap.put(mAddressTagMap.get(address), new float[]{1f, 0f, 0f, 0f});

        // Create a new DOT object and add it to the list of DOT objects
        XsensDotDevice dot = new XsensDotDevice(getApplicationContext(),
                bluetoothDevice, this);
        dot.connect();
        mDotList.add(dot);

        // Stop the scan if all the DOTs are initialized
        if (mDotList.size() == ALL_DOTS) {
            mDotScanner.stopScan();

            // Notify user
            Snackbar.make(mMeasuringLayout, "Connected all DOTs.",
                    Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Toggles the measuring state of the DOTs.
     *
     * @param view - the button calling the function.
     */
    public void toggleMeasuring(View view) {
        // Return if not all the DOTs are initialized.
        if (mDotList.size() != ALL_DOTS)
            return;

        // If not measuring, start measuring
        if (!mMeasuring) {
            // Set measuring to true and change the icon of the view
            mMeasuring = true;
            ((ImageButton) view).setImageResource(R.drawable.ic_stop);

            // Start the clocks and the chronometer
            mStartTime = Instant.now();
            mStartBalanceTime = Instant.now();
            mChronometer.setBase(SystemClock.elapsedRealtime());
            mChronometer.start();

            // Initialize every dot with the quaternion measurement mode
            // Start measuring and calibrate the sensors
            for (XsensDotDevice dot : mDotList) {
                dot.setMeasurementMode(XsPayload.PAYLOAD_TYPE_ORIENTATION_QUATERNION);
                dot.startMeasuring();
                dot.resetHeading();
            }

            // Notify user
            Snackbar.make(mMeasuringLayout, "Started measuring.",
                    Snackbar.LENGTH_LONG).show();
        }

        // If measuring, stop measuring
        else {
            // Set measuring to false and change the icon of the view
            mMeasuring = false;
            ((ImageButton) view).setImageResource(R.drawable.ic_play);

            // Stop the clock and the chronometer
            Instant endTime = Instant.now();
            mChronometer.stop();

            // Stop the measuring for every DOT
            for (XsensDotDevice dot : mDotList)
                dot.stopMeasuring();

            // Calculate the completion time
            mCompletionTime = Duration.between(mStartTime, endTime).toMillis();

            // Todo: save DVs to rides.txt and measurement data to startTime.txt
            // Todo: notify user
        }
    }

    /**
     * Starts the calibration of the DOTs.
     *
     * @param view - the button calling the function.
     */
    public void startCalibration(View view) {
        // Return if not all the DOTs are initialized or not currently measuring.
        if (mDotList.size() != ALL_DOTS || !mMeasuring)
            return;

        // Calibrate all DOTs
        for (XsensDotDevice dot : mDotList)
            dot.resetHeading();

        // Notify user
        Snackbar.make(mMeasuringLayout, "Calibrated DOTs.",
                Snackbar.LENGTH_LONG).show();
    }

    /**
     * Callback function which is triggered when values in a DOT change.
     *
     * @param address      - the MAC address of the DOT.
     * @param xsensDotData - the data object tied to the DOT.
     */
    @Override
    public void onXsensDotDataChanged(String address, XsensDotData xsensDotData) {
        // Get the current tag and map the current quaternion to it
        String currTag = mAddressTagMap.get(address);
        mTagQuatMap.put(currTag, xsensDotData.getQuat());

        // If the current tag is the Bike DOT, calculate the balance
        if (currTag != null && currTag.equals("Bike DOT"))
            calculateBalance();
    }

    /**
     * Calculates the balance given the current quaternion values.
     */
    private void calculateBalance() {
        // Calculate the bike vector and mirror it to get the optimal balance direction
        float[] bikeVector = mVecHelper.quatRotation
                (Objects.requireNonNull(mTagQuatMap.get("Bike DOT")), 100f);
        bikeVector = mVecHelper.mirrorVector(bikeVector, false, 0f);

        // Calculate the ankle vector and knee vector
        float[] ankleVector = mVecHelper.quatRotation
                (Objects.requireNonNull(mTagQuatMap.get("Ankle DOT")), mAnkleLength);
        float[] kneeVector = mVecHelper.quatRotation
                (Objects.requireNonNull(mTagQuatMap.get("Knee DOT")), mKneeLength);

        // Calculate the position of the current balance (end effector)
        float[] endEffector = mVecHelper.getEndEffector(ankleVector, kneeVector);

        // Get the intersection between current and optimal balance
        float[] intersection = mVecHelper.getIntersection(bikeVector, endEffector);

        // Get the flattened balance difference between the current and optimal balance
        float[] balanceDifference = mVecHelper.getBalanceDifference(endEffector, intersection);

        // Get the distance between the intersection and end effector
        float distance = mVecHelper.getDistance(endEffector, intersection);

        // Update the average balance deviation
        float currDeviation = (distance - mThresholdLeniency > 0) ?
                distance - mThresholdLeniency : 0f;
        mBalanceDeviation = getCMA(mBalanceDeviation, currDeviation);

        // Execute if the previous state was unbalanced and the user is currently balanced
        if (!mBalanced && distance <= mThresholdLeniency) {
            // Set the balance flag to true, and start the balance time
            mBalanced = true;
            mStartBalanceTime = Instant.now();

            // Calculate the current response time and update the average response time
            float currResponseTime = Duration.between
                    (mEndBalanceTime, mStartBalanceTime).toMillis();
            mResponseTime = getCMA(mResponseTime, currResponseTime);
        }

        // Execute if the previous state was balanced and the user is currently unbalanced
        if (mBalanced && distance > mThresholdLeniency) {
            // Set the balance flag to false, and start the end balance time
            mBalanced = false;
            mEndBalanceTime = Instant.now();

            // Calculate the current balance time and update the total balance time.
            float currBalanceTime = Duration.between
                    (mStartBalanceTime, mEndBalanceTime).toMillis();
            mBalancePerformance += currBalanceTime;
        }
    }

    /**
     * Calculates the cumulative moving average.
     *
     * @param currAverage - the current average.
     * @param val         - the new value to add to the average.
     * @return an updated moving average.
     */
    private float getCMA(float currAverage, float val) {
        // Calculates the cumulative moving average - https://en.wikipedia.org/wiki/Moving_average
        float numerator = mIteration * currAverage + val;
        return numerator / (mIteration + 1);
    }

    // region Unused
    @Override
    public void onXsensDotConnectionChanged(String s, int i) {
    }

    @Override
    public void onXsensDotServicesDiscovered(String s, int i) {
    }

    @Override
    public void onXsensDotFirmwareVersionRead(String s, String s1) {
    }

    @Override
    public void onXsensDotTagChanged(String s, String s1) {
    }

    @Override
    public void onXsensDotBatteryChanged(String s, int i, int i1) {
    }

    @Override
    public void onXsensDotCalibrationResult(String s, int i, int i1, int i2) {
    }

    @Override
    public void onXsensDotOtaChecked(String s, boolean b, String s1, String s2) {
    }

    @Override
    public void onXsensDotOtaRollback(String s, boolean b, String s1, String s2) {
    }

    @Override
    public void onXsensDotOtaFileMismatch(String s) {
    }

    @Override
    public void onXsensDotOtaDownloaded(String s, int i) {
    }

    @Override
    public void onXsensDotOtaUpdated(String s, int i, int i1, int i2, int i3, int i4) {
    }

    @Override
    public void onXsensDotNewFirmwareVersion(String s, boolean b, String s1, String s2) {
    }

    @Override
    public void onXsensDotOtaDischarge(String s) {
    }
    // endregion
}