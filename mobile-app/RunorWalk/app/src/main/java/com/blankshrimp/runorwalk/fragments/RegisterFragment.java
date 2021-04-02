package com.blankshrimp.runorwalk.fragments;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.blankshrimp.runorwalk.R;
import com.blankshrimp.runorwalk.utils.SwitchFragment;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Very similar to @LoginFragment
 */
public class RegisterFragment extends Fragment{

    private View view;
    private ProgressDialog waitingDialog;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.register, container, false);
        Button backButton = view.findViewById(R.id.back_to_login);
        backButton.setOnClickListener(l -> {
            if(getActivity() instanceof SwitchFragment){
                ((SwitchFragment)getActivity()).switchFragment("reg", "login");
            }
        });
        Button regButton = view.findViewById(R.id.btn_reg);
        regButton.setOnClickListener(l -> {
            EditText txtUsername = view.findViewById(R.id.reg_username);
            EditText txtPassword = view.findViewById(R.id.reg_password);
            EditText txtRePassword = view.findViewById(R.id.reg_re_password);
            EditText txtDeviceID = view.findViewById(R.id.reg_device_id);
            String username = txtUsername.getText().toString();
            String password = txtPassword.getText().toString();
            String rePassword = txtRePassword.getText().toString();
            String deviceID = txtDeviceID.getText().toString();

            if (!password.equals(rePassword)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.mismatch_passwd)
                        .setTitle(R.string.mismatch_passwd_title);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                waitingDialog = new ProgressDialog(getActivity());
                waitingDialog.setTitle(R.string.registering_title);
                waitingDialog.setMessage(getString(R.string.registering_msg));
                waitingDialog.setCancelable(false);
                waitingDialog.show();
                new RegisterTask().execute(username, password, deviceID);
            }
        });
        return view;
    }

    private class RegisterTask extends AsyncTask<String, Void, Void> {

        Response response;

        @Override
        protected Void doInBackground(String... strings) {
            try {
                String username = strings[0];
                String password = strings[1];
                String deviceID = strings[2];
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .build();
                MediaType mediaType = MediaType.parse("application/json");
                String content = String.format("{\r\n    \"account\": \"%s\"," +
                                "\r\n    \"passwd\":\"%s\"," +
                                "\r\n    \"device_id\":\"%s\"\r\n}",
                        username, password, deviceID);
                RequestBody body = RequestBody.create(mediaType, content);
                Request request = new Request.Builder()
                        .url("http://35.214.70.14/register")
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
                waitingDialog.cancel();
                Toast.makeText(view.getContext(), getString(R.string.register_success), Toast.LENGTH_SHORT).show();
                if(getActivity() instanceof SwitchFragment){
                    ((SwitchFragment)getActivity()).switchFragment("reg", "login");
                }
            } else {
                waitingDialog.cancel();
                Toast.makeText(view.getContext(), getString(R.string.register_failed), Toast.LENGTH_SHORT).show();
            }
            super.onPostExecute(aVoid);
        }
    }
}
