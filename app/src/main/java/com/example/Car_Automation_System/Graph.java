package com.example.Car_Automation_System;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.macroyau.thingspeakandroid.ThingSpeakChannel;
import com.macroyau.thingspeakandroid.ThingSpeakLineChart;
import com.macroyau.thingspeakandroid.model.ChannelFeed;

import java.util.Calendar;
import java.util.Date;

import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

public class Graph extends AppCompatActivity {

    private ThingSpeakChannel tsChannel;
    private ThingSpeakLineChart tsChart;
    private LineChartView chartView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        // Connect to ThinkSpeak Channel 9
        tsChannel = new ThingSpeakChannel(453839);
        // Set listener for Channel feed update events
        tsChannel.setChannelFeedUpdateListener(new ThingSpeakChannel.ChannelFeedUpdateListener() {
            @Override
            public void onChannelFeedUpdated(long channelId, String channelName, ChannelFeed channelFeed) {
                Date lastUpdate = channelFeed.getChannel().getUpdatedAt();
                Toast.makeText(Graph.this,"Last update at " + lastUpdate.toString(), Toast.LENGTH_LONG).show();
            }
        });
        tsChannel.loadChannelFeed();

        // Create a Calendar object dated 5 minutes ago
        Calendar calendar = Calendar.getInstance();
//        calendar.add(Calendar.SECOND, -60);

        // Configure LineChartView
        chartView = findViewById(R.id.chart);
        chartView.setZoomEnabled(true);
        chartView.setValueSelectionEnabled(true);

        // Create a line chart from Field1 of ThinkSpeak Channel 9
        tsChart = new ThingSpeakLineChart(453839, getIntent().getIntExtra("id", 1));
        // Get 200 entries at maximum
        tsChart.setNumberOfEntries(50);
        // Set value axis labels on 10-unit interval
        tsChart.setValueAxisLabelInterval(25);
        // Set date axis labels on 5-minute interval
        tsChart.setDateAxisLabelInterval(10);
        // Show the line as a cubic spline
        tsChart.useSpline(true);

        // Set the line color
        tsChart.setLineColor(Color.parseColor("#FF5722"));
        // Set the axis color
        tsChart.setAxisColor(Color.parseColor("#A9A9A9"));
        // Set the starting date (5 minutes ago) for the default viewport of the chart
        tsChart.setChartStartDate(calendar.getTime());
        // Set listener for chart data update
        tsChart.setListener(new ThingSpeakLineChart.ChartDataUpdateListener() {
            @Override
            public void onChartDataUpdated(long channelId, int fieldId, String title, LineChartData lineChartData, Viewport maxViewport, Viewport initialViewport) {
                // Set chart data to the LineChartView
                chartView.setLineChartData(lineChartData);
                // Set scrolling bounds of the chart
                chartView.setMaximumViewport(maxViewport);
                // Set the initial chart bounds
                chartView.setCurrentViewport(initialViewport);
            }
        });
        // Load chart data asynchronously
        tsChart.loadChartData();
    }

}