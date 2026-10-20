package com.nttdata.mscustomer.model;

public record ContactUpdate(boolean provided, String value) {

    public static final ContactUpdate KEEP = new ContactUpdate(false, null);

    public static ContactUpdate set(String value) {
        return new ContactUpdate(true, value);
    }
}
