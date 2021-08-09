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

public class Booking {

    @Schema(required = true)
    private Flight departtureFlight;

    @Schema(required = true)
    private Flight returningFlight;

    @Schema(required = true)
    private CreditCard creditCard;

    @Schema(required = true, example = "32126319")
    private String airMiles;

    @Schema(required = true, example = "window")
    private String seatPreference;

    private Booking() {}

    public Flight getDeparttureFlight() {
        return departtureFlight;
    }

    public void setDeparttureFlight(Flight departtureFlight) {
        this.departtureFlight = departtureFlight;
    }

    public Flight getReturningFlight() {
        return returningFlight;
    }

    public void setReturningFlight(Flight returningFlight) {
        this.returningFlight = returningFlight;
    }

    public CreditCard getCreditCard() {
        return creditCard;
    }

    public void setCreditCard(CreditCard creditCard) {
        this.creditCard = creditCard;
    }

    public String getAirMiles() {
        return airMiles;
    }

    public void setAirMiles(String airMiles) {
        this.airMiles = airMiles;
    }

    public String getSeatPreference() {
        return seatPreference;
    }

    public void setSeatPreference(String seatPreference) {
        this.seatPreference = seatPreference;
    }

}
