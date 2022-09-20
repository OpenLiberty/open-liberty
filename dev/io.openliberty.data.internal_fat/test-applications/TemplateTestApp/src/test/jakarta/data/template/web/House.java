/**
 *
 */
package test.jakarta.data.template.web;

import java.time.Year;

import jakarta.data.Id;

/**
 *
 */
public class House {
    public int area;

    public float lotSize;

    public int numBedrooms;

    @Id("parcelId")
    public String parcel;

    public float purchasePrice;

    public Year sold;
}
