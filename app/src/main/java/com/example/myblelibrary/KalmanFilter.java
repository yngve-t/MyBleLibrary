package com.example.myblelibrary;

public class KalmanFilter {
    private double processNoise;//Process noise
    private double measurementNoise;//Measurement noise
    private double estimatedRSSI;//calculated rssi
    private double errorCovarianceRSSI;//calculated covariance
    private boolean isInitialized = false;//initialization flag

    public KalmanFilter() {
        this.processNoise = 0.125;
        this.measurementNoise = 0.5;
    }

    public KalmanFilter(double processNoise, double measurementNoise) {
        this.processNoise = processNoise;
        this.measurementNoise = measurementNoise;
    }

    public double applyFilter(double rssi) {
        double priorRSSI;
        double kalmanGain;
        double priorErrorCovarianceRSSI;
        if (!isInitialized) {
            priorRSSI = rssi;
            priorErrorCovarianceRSSI = 1;
            isInitialized = true;
        } else {
            priorRSSI = estimatedRSSI;
            priorErrorCovarianceRSSI = errorCovarianceRSSI + processNoise;
        }

        kalmanGain = priorErrorCovarianceRSSI / (priorErrorCovarianceRSSI + measurementNoise);
        estimatedRSSI = priorRSSI + (kalmanGain * (rssi - priorRSSI));
        errorCovarianceRSSI = (1 - kalmanGain) * priorErrorCovarianceRSSI;

        return estimatedRSSI;
    }
}

