package com.mediatek.mcstutorial;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.mediatek.mcs.Utils.UIUtils;
import com.mediatek.mcs.domain.McsDataChannel;
import com.mediatek.mcs.domain.McsResponse;
import com.mediatek.mcs.entity.DataChannelEntity;
import com.mediatek.mcs.entity.DataPointEntity;
import com.mediatek.mcs.entity.api.DeviceInfoEntity;
import com.mediatek.mcs.entity.api.DeviceSummaryEntity;
import com.mediatek.mcs.net.McsJsonRequest;
import com.mediatek.mcs.net.RequestApi;
import com.mediatek.mcs.net.RequestManager;
import com.mediatek.mcs.socket.McsSocketListener;
import com.mediatek.mcs.socket.SocketManager;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    TextView comfort_tude;
    TextView pressure_tude;
    TextView allComfort_tude;

    ImageButton ibtn;

    String mDeviceId = "";
    DeviceInfoEntity mDeviceInfo;
    McsDataChannel mDataChannel;
    Handler handler = new Handler();
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        this.comfort_tude = (TextView) findViewById((R.id.comfort));
        this.pressure_tude = (TextView) findViewById((R.id.pressure));
        this.allComfort_tude = (TextView) findViewById((R.id.all));
        this.ibtn = (ImageButton) findViewById(R.id.btn);

        final Intent intent = new Intent(MainActivity.this, SessionActivity.class);
        startActivity(intent);

        requestDevices();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                requestDeviceInfo(mDeviceId);
            }
        },2000);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                showDataChannel(mDeviceInfo);
            }
        },4000);
       ibtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                allComfort_tude.setText("");
                start();
            }
        });

    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * GET device list.
     */
    private void requestDevices() {
        // Default method is GET
        int method = McsJsonRequest.Method.GET;
        String url = RequestApi.DEVICES;
        McsResponse.SuccessListener<JSONObject> successListener =
                new McsResponse.SuccessListener<JSONObject>() {
                    @Override public void onSuccess(JSONObject response) {
                        List<DeviceSummaryEntity> summary = new Gson().fromJson(
                                response.toString(), DeviceSummaryEntity.class).getResults();

                        if (summary.size() > 0) {
                            mDeviceId = summary.get(0).getDeviceId();
//                            btn_req_device_detail.setVisibility(View.VISIBLE);
                        }

                       printJson(response);
                    }
                };

        /**
         * Optional.
         * Default error message would be shown in logcat.
         */
        McsResponse.ErrorListener errorListener = new McsResponse.ErrorListener() {
            @Override public void onError(Exception e) {
//                tv_info.setText(e.toString());
            }
        };

        McsJsonRequest request = new McsJsonRequest(method, url, successListener, errorListener);
        RequestManager.sendInBackground(request);
    }

    /**
     * GET device info.
     */
    private void requestDeviceInfo(String deviceId) {
        McsJsonRequest request = new McsJsonRequest(
                RequestApi.DEVICE
                        .replace("{deviceId}", deviceId),
                new McsResponse.SuccessListener<JSONObject>() {
                    @Override public void onSuccess(JSONObject response) {
                        mDeviceInfo = UIUtils.getFormattedGson()
                                .fromJson(response.toString(), DeviceInfoEntity.class)
                                .getResults().get(0);

                        printJson(response);
                    }
                }
        );

        RequestManager.sendInBackground(request);
    }

    /**
     * GET data channel
     */
    private void showDataChannel(DeviceInfoEntity deviceInfo) {
        if (deviceInfo.getDataChannels().size() == 0) {
//            tv_info.setText("data channel is empty, please create one");
            return ;
        }

        /**
         * Optional.
         * Default message of socket update shows in log.
         */
        McsSocketListener socketListener = new McsSocketListener(
                new McsSocketListener.OnUpdateListener() {
                    @Override public void onUpdate(JSONObject data) {
                        printJson(data);
                    }
                }
        );
        DataChannelEntity channelEntity = deviceInfo.getDataChannels().get(0);

        mDataChannel = new McsDataChannel(deviceInfo, channelEntity, socketListener);
        allComfort_tude.setText(mDataChannel.getDataPointEntity().getValues().getValue());
    }

    /**
     * Socket control of single data channel
     */
    private void turnOnSocket() {
        SocketManager.connectSocket();
        SocketManager.registerSocket(mDataChannel, mDataChannel.getMcsSocketListener());
    }

    private void turnOffSocket() {
        SocketManager.unregisterSocket(mDataChannel, mDataChannel.getMcsSocketListener());
        SocketManager.disconnectSocket();
    }

    private void start(){

        requestDevices();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                requestDeviceInfo(mDeviceId);
            }
        },1000);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                showDataChannel(mDeviceInfo);
            }
        },1000);
    }


    private void printJson(JSONObject jsonObject) {
        try {
            allComfort_tude.setText(jsonObject.toString(2));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


}