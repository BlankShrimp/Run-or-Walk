package com.blankshrimp.runorwalk;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;

import com.blankshrimp.runorwalk.fragments.LoginFragment;
import com.blankshrimp.runorwalk.fragments.RealTimeFragment;
import com.blankshrimp.runorwalk.fragments.RegisterFragment;
import com.blankshrimp.runorwalk.fragments.StatisticsFragment;
import com.blankshrimp.runorwalk.utils.Notify;
import com.blankshrimp.runorwalk.utils.SwitchFragment;
import com.blankshrimp.runorwalk.utils.UpdateDate;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements SwitchFragment {

    FragmentManager fm;
    Fragment regFragment;
    Fragment loginFragment;
    Fragment statisticsFragment;
    Fragment realTimeFragment;
    int mYear;
    int mMonthOfYear;
    int mDayOfMonth;

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.select_date:
                // Open date selector.
                if (!statisticsFragment.isHidden() && regFragment.isHidden() && loginFragment.isHidden()) {
                    showDatePicker(this);
                }
                break;
            case R.id.real_time:
                // Switch fragment to real time predication.
                if (!statisticsFragment.isHidden() && regFragment.isHidden() && loginFragment.isHidden()) {
                    Fragment from = fm.findFragmentByTag("stat");
                    Fragment to = fm.findFragmentByTag("real");
                    FragmentTransaction transaction = fm.beginTransaction();
                    transaction.hide(from).show(to).commit();
                }
                break;
            case R.id.daily_view:
                // Switch fragment to daily statistics.
                if (statisticsFragment.isHidden() && regFragment.isHidden() && loginFragment.isHidden()) {
                    Fragment to = fm.findFragmentByTag("stat");
                    Fragment from = fm.findFragmentByTag("real");
                    FragmentTransaction transaction = fm.beginTransaction();
                    transaction.hide(from).show(to).commit();
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create all fragments and init.
        fm = getSupportFragmentManager();
        loginFragment = new LoginFragment();
        regFragment = new RegisterFragment();
        statisticsFragment = new StatisticsFragment();
        realTimeFragment = new RealTimeFragment();
        fm.beginTransaction()
                .add(R.id.main_container, loginFragment, "login")
                .add(R.id.main_container, regFragment, "reg")
                .add(R.id.main_container, statisticsFragment, "stat")
                .add(R.id.main_container, realTimeFragment, "real")
                .hide(loginFragment)
                .hide(regFragment)
                .hide(statisticsFragment)
                .hide(realTimeFragment)
                .commit();

        final Calendar calendar = Calendar.getInstance();
        mYear = calendar.get(Calendar.YEAR);
        mMonthOfYear = calendar.get(Calendar.MONTH);
        mDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        // If logged in, jump to daily stat, otherwise login.
        SharedPreferences preferences = this.getSharedPreferences("account", Context.MODE_PRIVATE);
        String deviceID = preferences.getString("device_id", "");
        if (!deviceID.equals("")) {
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.show(statisticsFragment).commit();
        } else {
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.show(loginFragment).commit();
        }

        Notify.createNotificationChannels(this);
    }

    /**
     * A globally called function to switch fragments.
     * @param fromTag
     * @param toTag
     */
    @Override
    public void switchFragment(String fromTag, String toTag) {
        Fragment from = fm.findFragmentByTag(fromTag);
        Fragment to = fm.findFragmentByTag(toTag);
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.hide(from).show(to).commit();
    }

    // Date selector helper
    private void showDatePicker(Context context) {
        // Init to current date.
        final Calendar calendar = Calendar.getInstance();
        View datePickerView = LayoutInflater.from(context).inflate(R.layout.date_picker, null);
        DatePicker datePicker = datePickerView.findViewById(R.id.date_picker);
        datePicker.init(mYear, mMonthOfYear, mDayOfMonth,
                new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mYear = year;
                mMonthOfYear = monthOfYear;
                mDayOfMonth = dayOfMonth;
                calendar.set(year, monthOfYear, dayOfMonth);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                if(statisticsFragment instanceof UpdateDate){
                    // Call UI update.
                    ((UpdateDate)statisticsFragment).updateDate(format.format(calendar.getTime()));
                }
            }
        });
        final AlertDialog dialog = new AlertDialog.Builder(context).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.setView(datePickerView);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }


}