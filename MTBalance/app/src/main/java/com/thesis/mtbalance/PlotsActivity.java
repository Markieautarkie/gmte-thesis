package com.thesis.mtbalance;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.google.android.material.tabs.TabLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

public class PlotsActivity extends AppCompatActivity {

    // Public keys used by fragments to get access to the plots data
    public static final String BUNDLE_KEY_BOTHDIR =
            "com.thesis.mtbalance.plotsactivity.bundle.KEY.BOTHDIR";
    public static final String BUNDLE_KEY_XDIR =
            "com.thesis.mtbalance.plotsactivity.bundle.KEY.XDIR";
    public static final String BUNDLE_KEY_YDIR =
            "com.thesis.mtbalance.plotsactivity.bundle.KEY.YDIR";

    /* Variables */
    // ArrayLists of data entry objects for the plots
    private ArrayList<DataEntry> mBothDirData = new ArrayList<>();
    private ArrayList<DataEntry> mXDirData = new ArrayList<>();
    private ArrayList<DataEntry> mYDirData = new ArrayList<>();

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private ViewPagerAdapter mVPAdapter;

    /**
     * Called on activity creation.
     *
     * @param savedInstanceState - state holding saved info from onPause callback.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plots);

        // Get the plot data from the passed intent
        getPlotData();

        // Set the tablayout
        setTabLayout();
    }

    /**
     * Gets the data for the plots.
     */
    private void getPlotData() {
        // Get the intent data from the card view click
        Intent intent = getIntent();
        String fileDir = intent.getStringExtra(RecyclerViewAdapter.EXTRA_FILEDIR);

        // Create a new fileHelper object
        FileHelper fileHelper = new FileHelper();

        // Retrieve the plot data from the file directory
        ArrayList<String> rideData = fileHelper.loadArrayData(fileDir, this);

        // Loop over all the strings in the rideData and turn them into data entry elements
        for (String ride : rideData) {
            float[] data = fileHelper.stringToFloatArray(ride);
            mBothDirData.add(new ValueDataEntry(data[1], data[2]));     // X and Y
            mXDirData.add(new ValueDataEntry(data[0], data[1]));        // Time and X
            mYDirData.add(new ValueDataEntry(data[0], data[2]));        // Time and Y
        }
    }

    /**
     * Sets the tablayout in the main activity.
     */
    private void setTabLayout() {
        // Find the elements needed for setup
        mTabLayout = findViewById(R.id.tablayout_plots);
        mViewPager = findViewById(R.id.viewpager_plots);
        mVPAdapter = new ViewPagerAdapter(getSupportFragmentManager());

        // Create a bundle holding the plot data
        Bundle bundle = new Bundle();
        bundle.putSerializable(BUNDLE_KEY_BOTHDIR, mBothDirData);
        bundle.putSerializable(BUNDLE_KEY_XDIR, mXDirData);
        bundle.putSerializable(BUNDLE_KEY_YDIR, mYDirData);

        // Create the fragments and add the bundle data
        BothDirFragment bothDirFragment = new BothDirFragment();
        XDirFragment xDirFragment = new XDirFragment();
        YDirFragment yDirFragment = new YDirFragment();

        bothDirFragment.setArguments(bundle);
        xDirFragment.setArguments(bundle);
        yDirFragment.setArguments(bundle);

        // Add the desired fragments, without title to show icon
        mVPAdapter.addFragment(bothDirFragment, "");
        mVPAdapter.addFragment(xDirFragment, "");
        mVPAdapter.addFragment(yDirFragment, "");

        // Setup the adapter and viewpager
        mViewPager.setAdapter(mVPAdapter);
        mTabLayout.setupWithViewPager(mViewPager);

        // Add the icons and remove elevation from the actionbar
        Objects.requireNonNull(mTabLayout.getTabAt(0)).setIcon(R.drawable.ic_directions);
        Objects.requireNonNull(mTabLayout.getTabAt(1)).setIcon(R.drawable.ic_horizontal);
        Objects.requireNonNull(mTabLayout.getTabAt(2)).setIcon(R.drawable.ic_vertical);
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setElevation(0);
    }
}