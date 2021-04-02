package com.blankshrimp.runorwalk.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.blankshrimp.runorwalk.R;
import com.blankshrimp.runorwalk.utils.SwitchFragment;
import com.google.gson.Gson;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RealTimeFragment extends Fragment {

    private View view;
    private TextView status;
    private TextView latestTime;
    private SwipeRefreshLayout layout;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.real_time_prediction, container, false);
        layout = view.findViewById(R.id.swipe_layout);
        layout.setColorSchemeColors(ContextCompat.getColor(view.getContext(), R.color.dark_violet),
                ContextCompat.getColor(view.getContext(), R.color.dark_turquoise),
                ContextCompat.getColor(view.getContext(), R.color.colorPrimary));
        layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new UpdateTask().execute();
            }
        });
        status = view.findViewById(R.id.status);
        latestTime = view.findViewById(R.id.latest_time);

        return view;
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
                // Get the latest data from 4.2 seconds ago
                // The reason for doing this is to reduce server pressure.
                long time = (new Date().getTime())-420000L;
                String content = "{\r\n    \"device_id\": \""+deviceID+"\", \r\n    \"from_time\":"+time+"\r\n}";
                System.out.println(content);
                RequestBody body = RequestBody.create(mediaType, content);
                Request request = new Request.Builder()
                        .url("http://35.214.70.14/mobilelatest")
                        .method("PUT", body)
                        .addHeader("Content-Type", "application/json")
                        .build();
                response = client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (response.isSuccessful()) {
                // Update UI elements
                try {
                    Gson gson = new Gson();
                    QuickObj entity = gson.fromJson(response.body().string(), QuickObj.class);
                    if (entity.type == 1) {
                        status.setTextColor(ContextCompat.getColor(view.getContext(), R.color.dark_violet));
                        status.setText(getString(R.string.running));
                    }
                    else {
                        status.setTextColor(ContextCompat.getColor(view.getContext(), R.color.dark_turquoise));
                        status.setText(getString(R.string.walking));
                    }
                    latestTime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(entity.timestamp));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            layout.setRefreshing(false);
            super.onPostExecute(aVoid);
        }

        private class QuickObj {
            long timestamp;
            int type;
        }
    }
}
