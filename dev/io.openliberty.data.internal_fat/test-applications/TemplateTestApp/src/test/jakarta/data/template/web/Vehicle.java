/**
 *
 */
package test.jakarta.data.template.web;

import io.openliberty.data.Id;

/**
 *
 */
public class Vehicle {
    public String make;

    public String model;

    public int numSeats;

    public float price;

    @Id("VIN")
    public String vin;
}
