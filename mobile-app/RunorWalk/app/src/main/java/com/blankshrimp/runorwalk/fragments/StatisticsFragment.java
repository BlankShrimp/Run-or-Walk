package com.blankshrimp.runorwalk.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.charts.Pie;
import com.anychart.core.axes.Linear;
import com.anychart.core.cartesian.series.Bar;
import com.anychart.data.Mapping;
import com.anychart.data.Set;
import com.anychart.enums.Anchor;
import com.anychart.enums.HoverMode;
import com.anychart.enums.LabelsOverlapMode;
import com.anychart.enums.Orientation;
import com.anychart.enums.ScaleStackMode;
import com.anychart.enums.TooltipDisplayMode;
import com.anychart.enums.TooltipPositionMode;
import com.blankshrimp.runorwalk.R;
import com.blankshrimp.runorwalk.utils.DataAccessObject;
import com.blankshrimp.runorwalk.utils.Notify;
import com.blankshrimp.runorwalk.utils.UpdateDate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class StatisticsFragment extends Fragment implements UpdateDate {

    private View view;
    DataAccessObject object;
    Cartesian barChart;
    Set set;
    private SwipeRefreshLayout layout;
    private long lastPull;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.statistics, container, false);

        layout = view.findViewById(R.id.stat_swipe_layout);
        layout.setColorSchemeColors(ContextCompat.getColor(view.getContext(), R.color.dark_violet),
                ContextCompat.getColor(view.getContext(), R.color.dark_turquoise),
                ContextCompat.getColor(view.getContext(), R.color.colorPrimary));
        layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new StatisticsFragment.UpdateTask().execute();
            }
        });
        AnyChartView anyChartView = view.findViewById(R.id.chart_view);
        anyChartView.setProgressBar(view.findViewById(R.id.chart_bar));

        // Read last pulling time from sharedpref. With this value, program will only need to
        // query newly uploaded data from server instead of pulling all historical data.
        SharedPreferences preferences = view.getContext().getSharedPreferences("account", Context.MODE_PRIVATE);
        lastPull = preferences.getLong("lastpull", 0L);

        // Very lengthy chunk of code that construct the barchart.
        barChart = AnyChart.bar();
        barChart.animation(true);
        barChart.padding(10d, 20d, 5d, 20d);
        barChart.yScale().stackMode(ScaleStackMode.VALUE);
        barChart.yAxis(0).labels().format(
                "function() {\n" +
                        "    return Math.abs(this.value).toLocaleString();\n" +
                        "  }");
        barChart.yAxis(0d).title("Activities minutes");
        barChart.xAxis(0d).overlapMode(LabelsOverlapMode.ALLOW_OVERLAP);
        Linear xAxis1 = barChart.xAxis(1d);
        xAxis1.enabled(true);
        xAxis1.orientation(Orientation.RIGHT);
        xAxis1.overlapMode(LabelsOverlapMode.ALLOW_OVERLAP);
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int monthOfYear = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.set(year, monthOfYear, dayOfMonth);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String date = String.format("%s Move Stat", format.format(calendar.getTime()));
        barChart.title(date);
        barChart.interactivity().hoverMode(HoverMode.BY_X);
        barChart.tooltip().title(false).separator(false)
                .displayMode(TooltipDisplayMode.SEPARATED)
                .positionMode(TooltipPositionMode.POINT)
                .useHtml(true).fontSize(12d).offsetX(5d).offsetY(0d)
                .format("function() {\n" +
                                "      return Math.abs(this.value).toLocaleString() + '<span style=\"color: #D9D9D9\"> min(s)</span>';\n" +
                                "    }");

        // Read existed data from local database. Data structure is written in @DBHelper.
        // Principle is written in @DataAccessObject.
        List<DataEntry> seriesData = new ArrayList<>();
        object = new DataAccessObject(view.getContext());
        List<Integer> moves = object.queryMovements(date);
        int totalMove = 0;
        for (int i = 0; i < 24; i++) {
            seriesData.add(new CustomDataEntry(
                    String.format("%s:00-%s:00",
                            String.valueOf(i).length()<2?"0"+i:i,
                            String.valueOf(i+1).length()<2?"0"+(i+1):i+1),
                    (int)(moves.get(i)/60),
                    -(int)(60-moves.get(i)/60)));
            totalMove += (int)(moves.get(i)/60);
        }
        set = Set.instantiate();
        set.data(seriesData);
        Mapping series1Data = set.mapAs("{ x: 'x', value: 'value' }");
        Mapping series2Data = set.mapAs("{ x: 'x', value: 'value2' }");

        Bar series1 = barChart.bar(series1Data);
        series1.name("Run")
                .color("DarkViolet");
        series1.tooltip()
                .position("right")
                .anchor(Anchor.LEFT_CENTER);
        Bar series2 = barChart.bar(series2Data);
        series2.name("Walk")
                .color("DarkTurquoise");
        series2.tooltip()
                .position("left")
                .anchor(Anchor.RIGHT_CENTER);
        barChart.legend().enabled(true);
        barChart.legend().inverted(true);
        barChart.legend().fontSize(13d);
        barChart.legend().padding(0d, 0d, 20d, 0d);

        anyChartView.setChart(barChart);

        // Pull new data from server.
        new StatisticsFragment.UpdateTask().execute();
        // If goal met, send a notification.
        if (totalMove >= 30) {
            Notify.displayNotification(view.getContext(), 1, false, "Milestone achieved. ", "Congrats! You've runned for above 30mins! ");
        }

        return view;
    }

    /**
     * Reload the chart with a given date.
     * @param newDate Formatted String (yyyy-MM-dd) of date.
     */
    @Override
    public void updateDate(String newDate) {
        List<Integer> moves = object.queryMovements(newDate);
        List<DataEntry> seriesData = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            seriesData.add(new CustomDataEntry(
                    String.format("%s:00-%s:00",
                            String.valueOf(i).length()<2?"0"+i:i,
                            String.valueOf(i+1).length()<2?"0"+(i+1):i+1),
                    (int)(moves.get(i)/60),
                    -(int)(60-moves.get(i)/60)));
        }
        set.data(seriesData);
        barChart.title(String.format("%s Move Stat", newDate));
    }

    private class CustomDataEntry extends ValueDataEntry {
        CustomDataEntry(String x, Number value, Number value2) {
            super(x, value);
            setValue("value2", value2);
        }
    }


    private class UpdateTask extends AsyncTask<Void, Void, Void> {

        Response response;

        @Override
        protected Void doInBackground(Void... voidss) {
            try {
                SharedPreferences preferences = view.getContext().getSharedPreferences("account", Context.MODE_PRIVATE);
                String deviceID = preferences.getString("device_id", "");
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .build();
                MediaType mediaType = MediaType.parse("application/json");
                String content = "{\r\n    \"device_id\": \""+deviceID+"\", \r\n    \"from_time\":"+lastPull+"\r\n}";
                RequestBody body = RequestBody.create(mediaType, content);
                Request request = new Request.Builder()
                        .url("http://35.214.70.14/mobile")
                        .method("PUT", body)
                        .addHeader("Content-Type", "application/json")
                        .build();
                response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    // Get a list of historical data from JSON.
                    JsonObject jsonObject = new JsonParser().parse(response.body().string()).getAsJsonObject();
                    final JsonArray data = jsonObject.getAsJsonArray("data");
                    for (JsonElement element: data) {
                        Gson gson = new Gson();
                        QuickObj temp = gson.fromJson(element.toString(), QuickObj.class);
                        Date date1 = new Date(temp.timestamp);
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(date1);
                        DataAccessObject object = new DataAccessObject(view.getContext());
                        // Addup to database
                        if (temp.type != 0) {
                            object.updateEntry(new SimpleDateFormat("yyyy-MM-dd").format(temp.timestamp),
                                    String.valueOf(calendar.get(Calendar.HOUR_OF_DAY)),
                                    String.valueOf((int)(temp.timestamp-lastPull)/1000));
                        }
                        lastPull = temp.timestamp;
                        System.out.println(lastPull);
                    }
                    // Update lastpull value
                    preferences.edit().putLong("lastpull", lastPull).apply();
                    System.out.println(lastPull);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // Update UI elements
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            String sd = format.format(new Date(Long.parseLong(String.valueOf(new Date().getTime()))));
            updateDate(sd);
            layout.setRefreshing(false);
            super.onPostExecute(aVoid);
        }

        private class QuickObj {
            long timestamp;
            int type;
        }
    }
}
