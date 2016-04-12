package com.example.clement.politico;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

/* Activity to display article summary
 */
public class SingleSummaryActivity extends AppCompatActivity {
    // defining colors for the chart
    private final int COLOR_GREEN_LIGHT = Color.rgb(192, 255, 133);
    private final int COLOR_BLUE_LIGHT = Color.rgb(142, 237, 255);
    private final int COLOR_RED_LIGHT = Color.rgb(254, 130, 133);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_summary);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        */
        Intent intent = getIntent();

        String title = getIntent().getExtras().getString(MainActivity.EXTRA_TITLE);
        String description = getIntent().getExtras().getString(MainActivity.EXTRA_DESCRIPTION);
        List<String> summaryText = getIntent().getExtras().getStringArrayList(MainActivity.EXTRA_SUMMARY);
        List<Integer> sentiment = getIntent().getExtras().getIntegerArrayList(MainActivity.EXTRA_SENTIMENT);

        /*TextView textview = new TextView(this);
        textview.setTextSize(20);
        textview.setText(summary);

        RelativeLayout layout = (RelativeLayout) findViewById(R.id.content);
        layout.addView(textview);*/

        loadArticlePage(title, description, summaryText);
        updatePie(sentiment.get(0), sentiment.get(1), sentiment.get(2));
    }

    private void updatePie(int positive, int neutral, int negative) {
        String sChartDescription = "Article's sentiment analysis";
        PieChart chart = (PieChart) findViewById(R.id.piegraph);

        String[] strSentiments = {"positive", "neutral", "negative"};

        ArrayList<Entry> weights = new ArrayList<>();
        weights.add(new Entry(positive, 0));
        weights.add(new Entry(neutral, 1));
        weights.add(new Entry(negative, 2));

        PieDataSet dataset = new PieDataSet(weights, "");
        dataset.setSliceSpace(3);
        dataset.setSelectionShift(5);

        PieData piedata = new PieData(strSentiments, dataset);
        piedata.setValueFormatter(new PercentFormatter());
        piedata.setValueTextSize(11f);
        piedata.setValueTextColor(Color.GRAY);

        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(COLOR_GREEN_LIGHT);
        colors.add(COLOR_BLUE_LIGHT);
        colors.add(COLOR_RED_LIGHT);
        dataset.setColors(colors);

        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(7);
        chart.setTransparentCircleRadius(10);
        chart.setData(piedata);
        chart.setBackgroundColor(Color.WHITE);

        chart.setDescription("");
        chart.setDescriptionTextSize(14);

        // disabling legend
        /*Legend l = chart.getLegend();
        l.setEnabled(false);*/

        chart.invalidate();
    }

    private void loadArticlePage(String title, String description, List<String> summaryText) {
        String summary = "<body><h2>" + title + "</h2>" +
                "<h4>" + description + "</h4>";
        for (int i = 0; i < summaryText.size(); ++i) {
            summary += "<p style=\"font-family:times, serif;\">" + summaryText.get(i) + "</p>";
        }

        WebView webview = (WebView) findViewById(R.id.webview);
        webview.loadData(summary,  "text/html", "UTF-8");
    }
}
