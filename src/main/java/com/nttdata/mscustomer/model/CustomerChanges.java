package com.nttdata.mscustomer.model;

public record CustomerChanges(
        ContactUpdate email,
        ContactUpdate phone,
        PersonalDetails personalDetails,
        BusinessDetails businessDetails
) {
}
