package com.example.myblelibrary;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements ASScannerCallback {

    private final String TAG = "resulty";

    private final String bOld = "D5:80:46:19:71:28";
    private final String bNew = "E1:9E:37:95:13:5F";
    private List<String> macAddresses = new ArrayList<String>();

    protected RecyclerView mRecyclerView;
    protected RecyclerView.LayoutManager mLayoutManager;
    protected static CustomAdapter mAdapter = new CustomAdapter();


    private Map<String, Beacon> inBeacon = new HashMap<>();

    private String pMaxMac;
    private String pMidMac;
    private String pMinMac;
    private double pMaxVal = -100;
    private double pMidVal = -100;
    private double pMinVal = -100;


    private final float mPower = -58.0f;
    private final int REQUEST_ENABLE_BT = 1;


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

    @Override
    public void scannedBleDevices(ScanResult result) {
        if (macAddresses.contains(result.getDevice().getAddress())) {
            if (inBeacon.containsKey(result.getDevice().getAddress())) {
                inBeacon.get(result.getDevice().getAddress()).setRssi(result.getRssi());
            }else {
                inBeacon.put(result.getDevice().getAddress(), new Beacon(result.getDevice().getAddress(), 0.0, 0.0, result.getRssi()));
            }
            if (inBeacon.get(result.getDevice().getAddress()).getRssi() >= 0.01
                    && inBeacon.get(result.getDevice().getAddress()).getRssi() <= 0.01) {
                if (inBeacon.get(result.getDevice().getAddress()).getRssi() > pMaxVal) {
                    pMidVal = pMaxVal;
                    pMidMac = pMaxMac;
                    pMinVal = pMidVal;
                    pMinMac = pMidMac;
                    pMaxVal = inBeacon.get(result.getDevice().getAddress()).getRssi();
                    pMaxMac = result.getDevice().getAddress();
                } else if (inBeacon.get(result.getDevice().getAddress()).getRssi() > pMidVal) {
                    pMinVal = pMidVal;
                    pMinMac = pMidMac;
                    pMidVal = inBeacon.get(result.getDevice().getAddress()).getRssi();
                    pMidMac = result.getDevice().getAddress();
                } else if (inBeacon.get(result.getDevice().getAddress()).getRssi() > pMinVal) {
                    pMinVal = inBeacon.get(result.getDevice().getAddress()).getRssi();
                    pMinMac = result.getDevice().getAddress();
                }
            }
        }
    }


    private void outCoordinates(){

    }

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