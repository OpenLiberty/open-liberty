package io.openliberty.jpa.data.tests.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

@Embeddable
public class Location1 {

    @Embedded
    public Address1 address;

    @Column(columnDefinition = "DECIMAL(8,5) NOT NULL")
    public float latitude;

    @Column(columnDefinition = "DECIMAL(8,5) NOT NULL")
    public float longitude;

    public Location1() {
    }

    public Location1(Address1 address, float latitude, float longitude) {
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}