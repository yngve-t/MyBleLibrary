package com.example.myblelibrary;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.accent_systems.ibks_sdk.scanner.ASBleScanner;
import com.accent_systems.ibks_sdk.scanner.ASScannerCallback;
import com.accent_systems.ibks_sdk.utils.ASUtils;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


public class MainActivity extends AppCompatActivity implements ASScannerCallback {

    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private static CustomAdapter mAdapter = new CustomAdapter();

    private final String TAG = "resulty";

    private final String bOld = "D5:80:46:19:71:28";
    private final String bNew = "E1:9E:37:95:13:5F";
    private List<String> macAddresses = new ArrayList<String>();

    private Map<String, Beacon> inBeacon = new HashMap<>();

    private Map<String, double[]> coordinates = new HashMap<>();


    private final float mPower = -58.0f;
    private final int REQUEST_ENABLE_BT = 1;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static <K, V extends Comparable<? super V>> Map<String, com.example.myblelibrary.Beacon> sortByValue(Map<String, com.example.myblelibrary.Beacon> map) {
        Map<String, com.example.myblelibrary.Beacon> result = new LinkedHashMap<>();
        Stream <Map.Entry<String, com.example.myblelibrary.Beacon>> stream = map.entrySet().stream();
        stream.sorted(Comparator.comparing(e -> e.getValue().getRssi())).forEach(e -> result.put(e.getKey(), e.getValue()));

        return result;
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(getApplicationContext(), "Hello", Toast.LENGTH_LONG).show();

            }else {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,}, 1);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(getApplicationContext(), "qwerty", Toast.LENGTH_LONG).show();

        } else {
            checkPermission();
        }
    }


    private double calculateDistance(float txPower, double rssi) {
        return Math.pow(10d, (txPower - rssi)/ (2 * 10));
    }

    private double[] applyTrilateration(double[][] positions, double[] distances) {
        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(
                new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
        LeastSquaresOptimizer.Optimum optimum = solver.solve();

        return optimum.getPoint().toArray();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void scannedBleDevices(ScanResult result) {
        Log.i(TAG, result.getDevice().getAddress());
        if (macAddresses.contains(result.getDevice().getAddress())) {
            Log.i(TAG, result.getDevice().getAddress());
            if (!inBeacon.containsKey(result.getDevice().getAddress())) {
                inBeacon.put(result.getDevice().getAddress(), new Beacon(result.getDevice().getAddress(),
                        coordinates.get(result.getDevice().getAddress()), result.getRssi()));
            }else {
                inBeacon.get(result.getDevice().getAddress()).setRssi(result.getRssi());
            }
            inBeacon = sortByValue(inBeacon);
            if (inBeacon.size() >= 3) {
                Log.i("inBeacon", String.valueOf(inBeacon.get(inBeacon.keySet().toArray()[inBeacon.size() - 1]).getRssi()));
                Log.i("inBeacon", String.valueOf(inBeacon.get(inBeacon.keySet().toArray()[inBeacon.size() - 2]).getRssi()));
                Log.i("inBeacon", String.valueOf(inBeacon.get(inBeacon.keySet().toArray()[inBeacon.size() - 3]).getRssi()));
                outCoordinates();
            }
        }
    }


    private void outCoordinates() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Wait a minute...");

        if (inBeacon.get(inBeacon.keySet().toArray()[inBeacon.size() - 1]).getRssi() >= 0
                || inBeacon.get(inBeacon.keySet().toArray()[inBeacon.size() - 2]).getRssi() >= 0
                || inBeacon.get(inBeacon.keySet().toArray()[inBeacon.size() - 3]).getRssi() >= 0) {

        }else {
            double[][] positions = {inBeacon.get(inBeacon.keySet().toArray()[inBeacon.size() - 1]).getCoordinates(),
                    inBeacon.get(inBeacon.keySet().toArray()[inBeacon.size() - 2]).getCoordinates(),
                    inBeacon.get(inBeacon.keySet().toArray()[inBeacon.size() - 3]).getCoordinates()};
            double[] distances = {calculateDistance(mPower, inBeacon.get(inBeacon.keySet().toArray()[inBeacon.size() - 1]).getRssi()),
                    calculateDistance(mPower, inBeacon.get(inBeacon.keySet().toArray()[inBeacon.size() - 2]).getRssi()),
                    calculateDistance(mPower, inBeacon.get(inBeacon.keySet().toArray()[inBeacon.size() - 3]).getRssi())};

            double[] coordinates = applyTrilateration(positions, distances);

            mAdapter.addItem(coordinates[0] + " " + coordinates[1]);
            mAdapter.notifyDataSetChanged();
            mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount()-1);

            Log.i(TAG, "item is added");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        macAddresses.add(bOld);
        macAddresses.add(bNew);
        macAddresses.add("C7:1E:1E:D1:AC:16");


        coordinates.put(bNew, new double[]{0.0, 0.0});
        coordinates.put(bOld, new double[]{9.0, 0.0});
        coordinates.put("C7:1E:1E:D1:AC:16", new double[]{10.0, 2.0});


        new ASBleScanner(this, this);
        BluetoothAdapter mBluetoothAdapter = ASBleScanner.getmBluetoothAdapter();

        if (mBluetoothAdapter == null) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        int e;
        ASBleScanner.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        e = ASBleScanner.startScan();
        if(e != ASUtils.TASK_OK) {
            Log.i(TAG, "startScan - Error (" + e + ")");
        }
    }
}