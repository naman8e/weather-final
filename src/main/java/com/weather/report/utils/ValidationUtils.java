package com.weather.report.utils;

import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.model.UserType;
import com.weather.report.model.entities.User;
import com.weather.report.repositories.CRUDRepository;

//utility class for common validation logic across the system
public class ValidationUtils {
    private ValidationUtils() {

    }

    public static void validateNotNullOrEmpty(String value, String fieldName)
            throws InvalidInputDataException {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputDataException(fieldName + " cannot be NULL or empty");

        }
    }

    public static void validateNotNull(Object value, String fieldName)
            throws InvalidInputDataException {
        if (value == null) {
            throw new InvalidInputDataException(fieldName + " cannot be null");
        }
    }

    public static void validateNetworkCode(String code) throws InvalidInputDataException {
        validateNotNullOrEmpty(code, "Network code");

        if (!code.matches("^NET_\\d{2}$")) {
            throw new InvalidInputDataException(
                    "Network code must follow format 'NET_##' where ## are two digits. Got: " + code);
        }
    }

    public static void validateGatewayCode(String code) throws InvalidInputDataException {
        validateNotNullOrEmpty(code, "Gateway code");

        if (!code.matches("^GW_\\d{4}$")) {
            throw new InvalidInputDataException(
                    "Gateway code must follow format 'GW_####' where #### are four digits. Got: " + code);
        }
    }

    public static void validateSensorCode(String code) throws InvalidInputDataException {
        validateNotNullOrEmpty(code, "Sensor code");

        if (!code.matches("^S_\\d{6}$")) {
            throw new InvalidInputDataException(
                    "Sensor code must follow format 'S_######' where ###### are six digits. Got: " + code);
        }
    }

    public static User validateMaintainerUser(String username) throws UnauthorizedException {
        if (username == null || username.trim().isEmpty()) {
            throw new UnauthorizedException("Username cannot be null or empty");
        }

        CRUDRepository<User, String> userRepo = new CRUDRepository<>(User.class);
        User user = userRepo.read(username);

        if (user == null) {
            throw new UnauthorizedException("User '" + username + "' not found");
        }

        if (user.getType() != UserType.MAINTAINER) {
            throw new UnauthorizedException(
                    "User" + username + " does not have MAINTAINER permissions. Current type:" + user.getType());

        }
        return user;
    }

}
