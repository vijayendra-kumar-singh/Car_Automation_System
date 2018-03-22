package com.example.Car_Automation_System;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    int n, s, w;
    TextView g1, g2, g3, sensor;

    Switch system, window, notification;

    boolean setup = false;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        g1 = findViewById(R.id.g1);
        g2 = findViewById(R.id.g2);
        g3 = findViewById(R.id.g3);
        sensor = findViewById(R.id.sensor);

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Loading ... ");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(false);

        system = findViewById(R.id.switch1);
        window = findViewById(R.id.switch2);
        notification = findViewById(R.id.switch3);

        httpRequest(1);

        g1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Graph.class);
                intent.putExtra("id", 2);
                startActivity(intent);
            }
        });
        g2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Graph.class);
                intent.putExtra("id", 3);
                startActivity(intent);
            }
        });
        g3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Graph.class);
                intent.putExtra("id", 4);
                startActivity(intent);
            }
        });
        sensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Graph.class);
                intent.putExtra("id", 1);
                startActivity(intent);
            }
        });

        system.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (setup) {
//                    Toast.makeText(MainActivity.this, "system", Toast.LENGTH_SHORT).show();
                    if (isChecked) {
                        s = 1;
                    } else {
                        s = 0;
                    }
                    httpRequest(3);
                }
            }
        });

        window.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (setup) {
//                    Toast.makeText(MainActivity.this, "window", Toast.LENGTH_SHORT).show();
                    if (isChecked) {
                        w = 1;
                    } else {
                        w = 0;
                    }
                    httpRequest(4);
                }
            }
        });

        notification.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (setup) {
//                    Toast.makeText(MainActivity.this, "notify", Toast.LENGTH_SHORT).show();
                    if (isChecked) {
                        n = 1;
                    } else {
                        n = 0;
                    }
                    httpRequest(5);
                }
            }
        });
    }

    private void disp() {

        String s_status, w_status, n_status;
        if (s == 1) {
            s_status = "ON";
        } else {
            s_status = "OFF";
        }

        if (w == 1) {
            w_status = "OPEN";
        } else {
            w_status = "CLOSED";
        }

        if (n == 1) {
            n_status = "ON";
        } else {
            n_status = "OFF";
        }

        Toast.makeText(this, "System is " + s_status + "\nWindow is " + w_status + "\nBuzzer is " + n_status, Toast.LENGTH_SHORT).show();
    }

    private void httpRequest(final int i) {

        AsyncHttpClient client = new AsyncHttpClient();

        String url;
        RequestParams params = new RequestParams();
        if (i == 1) {
            url = "https://api.thingspeak.com/channels/453839/feeds.json";
            params.put("results", 1);
        } else if (i == 2) {
            url = "https://api.thingspeak.com/channels/454941/feeds.json";
            params.put("results", 1);
        } else {
            url = "https://api.thingspeak.com/update";
            params.put("api_key", "T0FAKSVRQKPXTHLB");
            params.put("field1", s);
            params.put("field2", w);
            params.put("field3", n);
        }

        progressDialog.show();

        client.get(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
//                Toast.makeText(MainActivity.this, "Success " + i, Toast.LENGTH_SHORT).show();
                actions(i, response);
                if (i == 1) {
                    progressDialog.dismiss();
                    httpRequest(2);
                }
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
//                Toast.makeText(MainActivity.this, "success " + responseString, Toast.LENGTH_SHORT).show();
            }

            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                if (Objects.equals(responseString, "0")) {
                    setup = false;
                    Toast.makeText(MainActivity.this, "Network Error!", Toast.LENGTH_SHORT).show();
                    if (i == 3) {
//                        Toast.makeText(MainActivity.this, "in 30", Toast.LENGTH_SHORT).show();
                        if (s == 0) {
                            s = 1;
                            system.setChecked(true);
                        } else {
                            s = 0;
                            system.setChecked(false);
                        }
                    } else if (i == 4) {
//                        Toast.makeText(MainActivity.this, "in 40", Toast.LENGTH_SHORT).show();
                        if (w == 0) {
                            w = 1;
                            window.setChecked(true);
                        } else {
                            w = 0;
                            window.setChecked(false);
                        }
                    } else if (i == 5) {
//                        Toast.makeText(MainActivity.this, "in 50" + n, Toast.LENGTH_SHORT).show();
                        if (n == 0) {
                            n = 1;
                            notification.setChecked(true);
                        } else {
                            n = 0;
                            notification.setChecked(false);
                        }
                    }
                    setup = true;
                } else {
//                    Toast.makeText(MainActivity.this, "success " + responseString, Toast.LENGTH_SHORT).show();
                }
                disp();
                progressDialog.dismiss();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                if (i == 1) {
                    httpRequest(2);
                } else {
                    s = 0;
                    w = 0;
                    n = 0;
                    setup = true;
                }
                progressDialog.dismiss();
            }
        });

    }

    private void actions(int i, JSONObject response) {
        Log.v("--------------", String.valueOf(response));
        if (i == 1) {
            try {
                JSONArray feeds = response.getJSONArray("feeds");
                JSONObject latestFeed = feeds.getJSONObject(0);
                g1.setText("LPG Concentration = " + latestFeed.getString("field2").trim() + " ppm");
                g2.setText("CO Concentration = " + latestFeed.getString("field3").trim() + " ppm");
                g3.setText("Smoke Concentration = " + latestFeed.getString("field4").trim() + " ppm");
                sensor.setText("Sensor reading = " + latestFeed.getString("field1").trim());
            } catch (JSONException e) {
                Log.e("--------------", e.getMessage());
            }
        } else {
            try {
                JSONArray feeds = response.getJSONArray("feeds");
                JSONObject latestFeed = feeds.getJSONObject(0);
                if (latestFeed.getString("field1") != null && latestFeed.getString("field2") != null && latestFeed.getString("field3") != null) {
                    s = Integer.parseInt(latestFeed.getString("field1").trim());
                    w = Integer.parseInt(latestFeed.getString("field2").trim());
                    n = Integer.parseInt(latestFeed.getString("field3").trim());
                } else {
                    s = 0;
                    w = 0;
                    n = 0;
                }

                if (s == 1) {
                    system.setChecked(true);
                }
                if (w == 1) {
                    window.setChecked(true);
                }
                if (n == 1) {
                    notification.setChecked(true);
                }
                setup = true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            progressDialog.dismiss();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            setup = false;
            httpRequest(1);
        }
        return super.onOptionsItemSelected(item);
    }
}
