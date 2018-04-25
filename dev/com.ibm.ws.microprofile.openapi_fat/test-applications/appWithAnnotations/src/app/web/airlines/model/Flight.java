/**
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package app.web.airlines.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class Flight {

    @Schema(required = true)
    private Airline airline;

    @Schema(required = true, pattern = "dateTime", example = "2016-03-05 18:00")
    private String dateTime;

    @Schema(required = true, example = "AC190")
    private String number;

    @Schema(required = true, example = "On Schedule")
    private String status;

    @Schema(required = true, example = "YYZ")
    private String airportFrom;

    @Schema(required = true, example = "LAX")
    private String airportTo;

    @Schema(required = true, example = "US$350")
    private String price;

    private Flight() {}

    public Flight(Airline airline, String dateTime, String number, String status, String airportFrom, String airportTo, String price) {
        this.airline = airline;
        this.dateTime = dateTime;
        this.number = number;
        this.status = status;
        this.airportFrom = airportFrom;
        this.airportTo = airportTo;
        this.price = price;
    }

    public String getAirportFrom() {
        return airportFrom;
    }

    public void setAirportFrom(String airportFrom) {
        this.airportFrom = airportFrom;
    }

    public String getAirportTo() {
        return airportTo;
    }

    public void setAirportTo(String airportTo) {
        this.airportTo = airportTo;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public Airline getAirline() {
        return airline;
    }

    public void setAirline(Airline airline) {
        this.airline = airline;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}
