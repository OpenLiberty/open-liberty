package io.openliberty.jpa.data.tests.models;

import io.openliberty.jpa.data.tests.models.Business.Street;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

@Embeddable
public class Address1 {

    public String city;

    public int houseNum;

    public String state;

    @Embedded
    public Street1 street;

    @Convert(converter = ZipCodeConverter.class)
    public ZipCode zip;

    public Address1() {
    }

    public Address1(String city, String state, int zip, int houseNum, Street1 street) {
        this.city = city;
        this.state = state;
        this.zip = ZipCode.of(zip);
        this.houseNum = houseNum;
        this.street = street;
    }
}