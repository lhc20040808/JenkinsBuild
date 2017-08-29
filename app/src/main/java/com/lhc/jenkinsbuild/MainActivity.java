package com.lhc.jenkinsbuild;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    TextView tv_test;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_test = (TextView) findViewById(R.id.tv_test);
        tv_test.setText(BuildConfig.MARKET_URL);
    }
}
