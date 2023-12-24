package com.example.finalproject;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import android.os.Handler;
import android.os.Looper;

import com.example.finalproject.Utils.MyWebSocketClient;
import com.tencent.mmkv.MMKV;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;


public class frag_home extends Fragment {
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MyWebSocketClient wssClient = null;
    private MapView map = null;
    private MMKV kv = null;
    private long ZoomSpeed = 500;
    private double MinZoom = 18.0;
    private double MaxZoom = 21.0;
    private double ZoomLevel = 19.5;
    private GeoPoint UITLocation = new GeoPoint(10.870, 106.80324);
    private GeoPoint Station1 = new GeoPoint(10.869778736885038, 106.80280655508835);
    private GeoPoint Station2 = new GeoPoint(10.869778736885038, 106.80345028525176);
    private Button btn_fullScreen = null;
    private TextView timeTextView = null, dayOfWeekTextView = null, dateTextView = null;

    private static final long UPDATE_INTERVAL = 30000;
    private Handler handler = null;
    private Runnable updateTimeRunnable = null;

    public frag_home() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {super.onCreate(savedInstanceState);}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_home, container, false);

        HomeActivity homeActivity = (HomeActivity) getActivity();
        wssClient = homeActivity.getSocket();

        timeTextView = view.findViewById(R.id.tv_time);
        dayOfWeekTextView = view.findViewById(R.id.tv_dow);
        dateTextView = view.findViewById(R.id.tv_date);
        btn_fullScreen = view.findViewById(R.id.btn_fullscreen);

        btn_fullScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.map_container, new frag_map())
                        .addToBackStack(null)
                        .commit();
            }
        });

        OnBackPressedDispatcher onBackPressedDispatcher = requireActivity().getOnBackPressedDispatcher();
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        };
        onBackPressedDispatcher.addCallback(getViewLifecycleOwner(), callback);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context ctx = requireContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        displayDateTimeInfo(timeTextView, dayOfWeekTextView, dateTextView);

        map = view.findViewById(R.id.map);
        UpdateMark1(view);
        UpdateMark2(view);

        SetupMap(view,ctx);
        String[] Permission = {
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION};
        requestPermissionsIfNecessary(Permission);

        handler = new Handler(Looper.getMainLooper());
        updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                displayDateTimeInfo(timeTextView, dayOfWeekTextView, dateTextView);
                UpdateMark1(view);
                UpdateMark2(view);
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        // Bắt đầu cập nhật thời gian khi Fragment được hiển thị
        handler.post(updateTimeRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Dừng cập nhật thời gian khi Fragment không còn được hiển thị
        handler.removeCallbacks(updateTimeRunnable);
    }
    private void displayDateTimeInfo(TextView timeTextView, TextView dayOfWeekTextView, TextView dateTextView) {
        Calendar calendar = Calendar.getInstance(); // Tạo một instance của Calendar

        // Định dạng thời gian và hiển thị trong TextView thời gian
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String formattedTime = timeFormat.format(calendar.getTime());
        timeTextView.setText(formattedTime);

        // Định dạng thứ và hiển thị trong TextView thứ
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        String formattedDayOfWeek = dayFormat.format(calendar.getTime());
        dayOfWeekTextView.setText(formattedDayOfWeek);

        // Định dạng ngày tháng năm và hiển thị trong TextView ngày tháng năm
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String formattedDate = dateFormat.format(calendar.getTime());
        dateTextView.setText(formattedDate);
    }


    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }
    private void SetupMap(@NonNull View view, @NonNull Context ctx){
        ColorMatrix inverseMatrix = new ColorMatrix(new float[] {
                -1.0f, 0.0f, 0.0f, 0.0f, 255f,
                0.0f, -1.0f, 0.0f, 0.0f, 255f,
                0.0f, 0.0f, -1.0f, 0.0f, 255f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
        });

        int destinationColor = Color.parseColor("#1c2334");
        float lr = (255.0f - Color.red(destinationColor))/255.0f;
        float lg = (255.0f - Color.green(destinationColor))/255.0f;
        float lb = (255.0f - Color.blue(destinationColor))/255.0f;
        ColorMatrix grayscaleMatrix = new ColorMatrix(new float[] {
                lr, lg, lb, 0, 0,
                lr, lg, lb, 0, 0,
                lr, lg, lb, 0, 0,
                0, 0, 0, 0, 255,
        });
        grayscaleMatrix.preConcat(inverseMatrix);
        int dr = Color.red(destinationColor);
        int dg = Color.green(destinationColor);
        int db = Color.blue(destinationColor);
        float drf = dr / 255f;
        float dgf = dg / 255f;
        float dbf = db / 255f;
        ColorMatrix tintMatrix = new ColorMatrix(new float[] {
                drf, 0, 0, 0, 0,
                0, dgf, 0, 0, 0,
                0, 0, dbf, 0, 0,
                0, 0, 0, 1, 0,
        });
        tintMatrix.preConcat(grayscaleMatrix);
        float lDestination = drf * lr + dgf * lg + dbf * lb;
        float scale = 1f - lDestination;
        float translate = 1 - scale * 0.5f;
        ColorMatrix scaleMatrix = new ColorMatrix(new float[] {
                scale, 0, 0, 0, dr * translate,
                0, scale, 0, 0, dg * translate,
                0, 0, scale, 0, db * translate,
                0, 0, 0, 1, 0,
        });
        scaleMatrix.preConcat(tintMatrix);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(scaleMatrix);

        Marker Station1Marker = new Marker(map);
        Station1Marker.setPosition(Station1);
        Station1Marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        Station1Marker.setIcon(ctx.getDrawable(R.drawable.cloud));

        Marker Station2Marker = new Marker(map);
        Station2Marker.setPosition(Station2);
        Station2Marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        Station2Marker.setIcon(ctx.getDrawable(R.drawable.cloud));

        map.addOnFirstLayoutListener(new MapView.OnFirstLayoutListener() {
            @Override
            public void onFirstLayout(View v, int left, int top, int right, int bottom) {
                map.setTileSource(TileSourceFactory.MAPNIK );
                map.setMultiTouchControls(true);
                map.setMinZoomLevel(MinZoom);
                map.setMaxZoomLevel(MaxZoom);
                map.getController().setZoom(ZoomLevel);
                map.setScrollableAreaLimitLatitude(10.890, 10.865, 100);
                map.setScrollableAreaLimitLongitude(106.7, 106.806, 100);
                map.getController().setCenter(UITLocation);
                map.getOverlayManager().getTilesOverlay().setColorFilter(filter);
                map.getOverlays().add(Station1Marker);
                map.getOverlays().add(Station2Marker);
                map.invalidate();
            }
        });

        Station1Marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                map.getController().animateTo(Station1Marker.getPosition(), ZoomLevel, ZoomSpeed);
                UpdateMark1(view);
                return true;
            }
        });

        Station2Marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                map.getController().animateTo(Station2Marker.getPosition(), ZoomLevel, ZoomSpeed);
                UpdateMark2(view);
                return true;
            }
        });
    }
    private void UpdateMark1(@NonNull View view){
        try {
            wssClient.send("REQUESTRESPONSE:{\"messageId\":\"read-assets:5zI6XqkQVSfdgOrZ1MyWEf:AssetEvent2\",\"event\":{\"eventType\":\"read-assets\",\"assetQuery\":{\"ids\":[\"5zI6XqkQVSfdgOrZ1MyWEf\"]}}}");
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Log.d("Mark1", wssClient.uglyJson.substring(16,wssClient.uglyJson.length()));
        try{
            String formatString = wssClient.uglyJson.substring(16,wssClient.uglyJson.length());
            if (!formatString.equals("{}")) {
                JSONObject jsonObject = new JSONObject(formatString);
                String messageId = jsonObject.getString("messageId");
                if(messageId.equals("read-assets:5zI6XqkQVSfdgOrZ1MyWEf:AssetEvent2")){
                    JSONObject event = jsonObject.getJSONObject("event");
                    JSONArray assets = event.getJSONArray("assets");
                    JSONObject zero = assets.getJSONObject(0);
                    JSONObject attributes = zero.getJSONObject("attributes");
                    JSONObject rainfall = attributes.getJSONObject("rainfall");
                    JSONObject temperature = attributes.getJSONObject("temperature");
                    JSONObject humidity = attributes.getJSONObject("humidity");
                    JSONObject windDirection = attributes.getJSONObject("windDirection");
                    JSONObject windSpeed = attributes.getJSONObject("windSpeed");

                    TextView rf = view.findViewById(R.id.tv_rainfall);
                    TextView temp = view.findViewById(R.id.tv_temp);
                    TextView hm = view.findViewById(R.id.tv_humidity);
                    TextView wd = view.findViewById(R.id.tv_winddirect);
                    TextView ws = view.findViewById(R.id.tv_windspeed);

                    rf.setText(rainfall.getString("value").concat("mm"));
                    temp.setText(temperature.getString("value").concat("°C"));
                    hm.setText(humidity.getString("value").concat("%"));
                    wd.setText(windDirection.getString("value"));
                    ws.setText(windSpeed.getString("value").concat(" km/h"));
                }
            }
        } catch (Exception e) {
            Log.d("FragHome", e.toString());
        }
    }
    private void UpdateMark2(@NonNull View view){
        try {
            wssClient.send("REQUESTRESPONSE:{\"messageId\":\"read-assets:6iWtSbgqMQsVq8RPkJJ9vo:AssetEvent2\",\"event\":{\"eventType\":\"read-assets\",\"assetQuery\":{\"ids\":[\"6iWtSbgqMQsVq8RPkJJ9vo\"]}}}");
            Thread.sleep(100);
        } catch (Exception e) {
            Log.d("FragHome", e.toString());
        }

        Log.d("Mark2", wssClient.uglyJson.substring(16,wssClient.uglyJson.length()));
        try{
            String formatString = wssClient.uglyJson.substring(16,wssClient.uglyJson.length());
            if (!formatString.equals("{}")) {
                JSONObject jsonObject = new JSONObject(formatString);
                String messageId = jsonObject.getString("messageId");
                if(messageId.equals("read-assets:6iWtSbgqMQsVq8RPkJJ9vo:AssetEvent2")){
                    JSONObject event = jsonObject.getJSONObject("event");
                    JSONArray assets = event.getJSONArray("assets");
                    JSONObject zero = assets.getJSONObject(0);
                    JSONObject attributes = zero.getJSONObject("attributes");
                    JSONObject brightness = attributes.getJSONObject("brightness");
                    JSONObject colourTemperature = attributes.getJSONObject("colourTemperature");

                    TextView br = view.findViewById(R.id.tv_brightness);
                    TextView ct = view.findViewById(R.id.tv_colortemp);

                    br.setText(brightness.getString("value").concat("%"));
                    ct.setText(colourTemperature.getString("value").concat("°K"));
                }
            }
        } catch (Exception e) {
            Log.d("FragHome", e.toString());
        }
    }
}