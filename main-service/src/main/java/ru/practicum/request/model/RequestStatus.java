package ru.practicum.request.model;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RequestStatus {
    public static final String PENDING_REQUEST = "PENDING";
    public static final String CONFIRMED_REQUEST = "CONFIRMED";
    public static final String REJECTED_REQUEST = "REJECTED";
    public static final String ACCEPTED_REQUEST = "ACCEPTED";
    public static final String CANCELED_REQUEST = "CANCELED";
}
