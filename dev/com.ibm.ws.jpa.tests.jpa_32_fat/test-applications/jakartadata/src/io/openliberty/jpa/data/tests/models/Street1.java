package io.openliberty.jpa.data.tests.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Street1 {

    public String direction;

    @Column(name = "STREETNAME")
    public String name;

    public Street1() {
    }

    public Street1(String name, String direction) {
        this.name = name;
        this.direction = direction;
    }
}