package com.example.myblelibrary;

import java.util.ArrayList;
import java.util.List;

public class Beacon {
    private final List<Double> list1 = new ArrayList<>();
    private final List<Double> list2 = new ArrayList<>();
    private KalmanFilter kalmanFilter = new KalmanFilter();
    private final int N = 10;


    private String macAddress;
    private double[] coordinates;
    private double rssi;

    public Beacon(String macAddress, double[] coordinates, double rssi) {
        this.macAddress = macAddress;
        this.coordinates = coordinates;
        this.rssi = 0.0;
        list1.add(rssi);
    }


    public String getMacAddress() {
        return macAddress;
    }

    public double getX() {
        return coordinates[0];
    }

    public double getY() {
        return coordinates[1];
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public double getRssi() {return rssi;}

    public void setRssi(double rssi) {
        list1.add(kalmanFilter.applyFilter(rssi));
        if (list1.size() >= N) {
            list2.add(sma(list1));
        }
        if (list2.size() >= N) {
            this.rssi = sma(list2);
        }
    }

    private double sma(List<Double> list) {
        int s = 0;
        for (int i = list.size() - 1, k = 0; k < N; i--, k++) {
            s += list.get(i);
        }

        return (double)s / N;
    }
}
