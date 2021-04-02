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
import android.widget.Toast;

import com.blankshrimp.runorwalk.R;
import com.blankshrimp.runorwalk.utils.Notify;
import com.blankshrimp.runorwalk.utils.SwitchFragment;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginFragment extends Fragment {

    private View view;
    private ProgressDialog waitingDialog;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.login, container, false);
        Button regButton = view.findViewById(R.id.turn_to_reg);
        regButton.setOnClickListener(l -> {
            if(getActivity() instanceof SwitchFragment){
                ((SwitchFragment)getActivity()).switchFragment("login", "reg");
            }
        });
        Button loginButtion = view.findViewById(R.id.btn_login);
        loginButtion.setOnClickListener(l -> {
            EditText txtUsername = view.findViewById(R.id.txt_username);
            EditText txtPassword = view.findViewById(R.id.txt_password);
            String username = txtUsername.getText().toString();
            String password = txtPassword.getText().toString();
            waitingDialog = new ProgressDialog(getActivity());
            waitingDialog.setTitle(R.string.logging_title);
            waitingDialog.setMessage(getString(R.string.logging_msg));
            waitingDialog.setCancelable(false);
            waitingDialog.show();
            new LoginTask().execute(username, password);
        });
        return view;
    }


    /**
     * This class handle the network in background thread as required by Android that
     * time consuming process should not be in main thread otherwise would cause UI stuck.
     */
    private class LoginTask extends AsyncTask<String, Void, Void> {

        Response response;
        String username;
        String password;

        @Override
        protected Void doInBackground(String... strings) {
            try {
                username = strings[0];
                password = strings[1];
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .build();
                MediaType mediaType = MediaType.parse("application/json");
                String content = String.format("{\r\n    \"account\": \"%s\"," +
                        "\r\n    \"passwd\":\"%s\"\r\n}",
                        username, password);
                RequestBody body = RequestBody.create(mediaType, content);
                Request request = new Request.Builder()
                        .url("http://35.214.70.14/mobilelogin")
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
                try {
                    // If logged in, store data in Sharedpref.
                    waitingDialog.cancel();
                    String device_id = response.body().string();
                    Toast.makeText(view.getContext(), getString(R.string.login_success), Toast.LENGTH_SHORT).show();
                    SharedPreferences preferences = view.getContext().getSharedPreferences("account", Context.MODE_PRIVATE);
                    preferences.edit().putString("account", username)
                            .putString("passwd", password)
                            .putString("device_id", device_id)
                            .apply();
                    // Then shift to stat view.
                    if(getActivity() instanceof SwitchFragment){
                        ((SwitchFragment)getActivity()).switchFragment("login", "stat");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                waitingDialog.cancel();
                Toast.makeText(view.getContext(), getString(R.string.login_failed), Toast.LENGTH_SHORT).show();
            }
            super.onPostExecute(aVoid);
        }
    }
}
