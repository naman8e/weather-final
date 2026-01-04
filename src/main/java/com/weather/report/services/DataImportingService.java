package com.weather.report.services;

import com.weather.report.model.ThresholdType;
import com.weather.report.model.entities.*;
import com.weather.report.repositories.CRUDRepository;
import com.weather.report.repositories.MeasurementRepository;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DataImportingService {

  private static final int CSV_COLUMNS = 5;
  private static final DateTimeFormatter CSV_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private DataImportingService() {}

  public static void storeMeasurements(String filePath) {
    if (filePath == null || filePath.isBlank()) return;

    MeasurementRepository repository = new MeasurementRepository();

    File file;
    try {
        String decodedPath = URLDecoder.decode(filePath, StandardCharsets.UTF_8.name());
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        if (decodedPath.startsWith("file:/")) {
            decodedPath = decodedPath.substring(6);
        }
        
        if (isWindows && decodedPath.startsWith("/")) {
            decodedPath = decodedPath.substring(1);
        }
        
        file = new File(decodedPath);
    } catch (Exception e) {
        file = new File(filePath);
    }

    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      reader.readLine();
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isBlank()) continue;
        String[] parts = line.split(",");
        if (parts.length != CSV_COLUMNS) continue;

        try {
          Measurement measurement = new Measurement(parts[1].trim(), parts[2].trim(), parts[3].trim(), 
                                        Double.parseDouble(parts[4].trim()), 
                                        LocalDateTime.parse(parts[0].trim(), CSV_TIME_FORMATTER));
          checkMeasurement(repository.create(measurement));
        } catch (Exception ex) { continue; }
      }
    } catch (IOException ex) {
      throw new IllegalArgumentException("Cannot read measurements file: " + filePath, ex);
    }
  }

  private static void checkMeasurement(Measurement measurement) {
    CRUDRepository<Sensor, String> sensorRepository = new CRUDRepository<>(Sensor.class);
    Sensor currentSensor = sensorRepository.read().stream()
        .filter(sensor -> measurement.getSensorCode().equals(sensor.getCode())).findFirst().orElse(null);

    if (currentSensor == null || currentSensor.getThreshold() == null) return;
    Threshold threshold = currentSensor.getThreshold();
    if (checkThresholdViolation(measurement.getValue(), threshold.getValue(), threshold.getType())) {
        CRUDRepository<Network, String> networkRepository = new CRUDRepository<>(Network.class);
        Network network = networkRepository.read(measurement.getNetworkCode());
        if (network != null) AlertingService.notifyThresholdViolation(network.getOperators(), currentSensor.getCode());
    }
  }

  private static boolean checkThresholdViolation(double measuredValue, double thresholdValue, ThresholdType type) {
    return switch (type) {
      case GREATER_THAN -> measuredValue > thresholdValue;
      case LESS_THAN -> measuredValue < thresholdValue;
      case GREATER_OR_EQUAL -> measuredValue >= thresholdValue;
      case LESS_OR_EQUAL -> measuredValue <= thresholdValue;
      case EQUAL -> Math.abs(measuredValue - thresholdValue) < 0.0001;
      case NOT_EQUAL -> Math.abs(measuredValue - thresholdValue) >= 0.0001;
    };
  }
}