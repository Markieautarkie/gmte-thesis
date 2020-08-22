package com.thesis.mtbalance;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.charts.Scatter;
import com.anychart.core.annotations.Ellipse;
import com.anychart.core.annotations.PlotController;
import com.anychart.core.scatter.series.Marker;
import com.anychart.enums.MarkerType;
import com.anychart.graphics.vector.text.HAlign;

import java.util.ArrayList;

public class BothDirFragment extends Fragment {

    /* Variables */
    // The data points of the plot.
    private ArrayList<DataEntry> mPlotData;

    private View mView;

    public BothDirFragment() {
    }

    /**
     * Creates the layout of the fragment in the container.
     *
     * @param inflater           - helper to inflate the layout.
     * @param container          - the container to inflate to fragment in.
     * @param savedInstanceState - state holding data on the onPause callback.
     * @return a view element with the inflated fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_bothdir, container, false);
        return mView;
    }

    /**
     * Called after the view is created.
     * The plot is populated here since views in fragments can only be accessed after creation.
     *
     * @param view               - the current view of the fragment.
     * @param savedInstanceState - the saved instance of the fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the color for the background and markers
        String backgroundColor = "#2B2B2B";
        String markerColor = "#52B7F8";

        // Find the plotView element in the fragment
        AnyChartView plotView = view.findViewById(R.id.plot_bothdir);

        // Set the plotView background during loading
        plotView.setBackgroundColor(backgroundColor);
        // Todo: set up a loading bar to notify the app is loading

        // Create a scatter plot and set the background color
        Scatter scatter = AnyChart.scatter();
        scatter.background().fill(backgroundColor);

        // Set the min + max values for the scales of the plot
        scatter.xScale()
                .minimum(-100f)
                .maximum(100f);
        scatter.yScale()
                .minimum(-100f)
                .maximum(100f);

        // Enable gridlines on both axes
        scatter.xGrid(0).enabled(true);
        scatter.yGrid(0).enabled(true);
        scatter.xMinorGrid(0).enabled(true);
        scatter.yMinorGrid(0).enabled(true);

        // Set clearer lines for the cartesian coordinate system
        scatter.lineMarker(0)
                .axis(scatter.xAxis(0))
                .value(0)
                .stroke("2 white");
        scatter.lineMarker(1)
                .axis(scatter.yAxis(0))
                .value(0)
                .stroke("2 white");

        // Change the tooltip name
        scatter.tooltip().title("Balance");

        // Set the marker/tooltip and plot data to it
        Marker marker = scatter.marker(mPlotData);
        marker.type(MarkerType.CIRCLE)
                .size(4f)
                .color(markerColor);
        marker.tooltip()
                .hAlign(HAlign.CENTER)
                .format("front/back: {%Value} cm\\nleft/right: {%X} cm");

        // Create an annotation showing the optimal balance zone
        PlotController plotController = scatter.annotations();
        Ellipse balanceThreshold = plotController.ellipse("");
        balanceThreshold
                .xAnchor("-50")
                .secondXAnchor("50")
                .valueAnchor("-50")
                .secondValueAnchor("50")
                .fill("green", 0.5f)
                .stroke("2 green");

        // Todo - check github - ticks interval + allowedit ellips = false

        // Show the plot
        plotView.setChart(scatter);
    }

    /**
     * Called on the creation of the fragment (before onCreateView).
     * Used for initializing data.
     *
     * @param savedInstanceState - the saved instance of the fragment.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve the plot data from the PlotActivity bundle
        mPlotData = (ArrayList<DataEntry>) requireArguments()
                .getSerializable(PlotsActivity.BUNDLE_KEY_BOTHDIR);
    }
}
