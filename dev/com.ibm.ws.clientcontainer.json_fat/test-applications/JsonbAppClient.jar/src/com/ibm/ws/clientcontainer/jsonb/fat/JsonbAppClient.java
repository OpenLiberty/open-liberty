/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.jsonb.fat;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.adapter.JsonbAdapter;

import com.ibm.ws.clientcontainer.jsonb.fat.OneDayWeatherForecast.HourlyWeatherForecast;

public class JsonbAppClient {

    public static void main(String[] args) {
        System.out.println("\nEntering JSON-B Application Client.");

        JsonbAdapter<LocalDate, Map<String, Object>> dateAdapter = new JsonbAdapter<LocalDate, Map<String, Object>>() {
            @Override
            public LocalDate adaptFromJson(Map<String, Object> map) throws Exception {
                return LocalDate.of(
                        ((Number) map.get("year")).intValue(),
                        Month.valueOf(((String) map.get("month")).toUpperCase(Locale.ENGLISH)),
                        ((Number) map.get("day")).intValue());
            }

            @Override
            public Map<String, Object> adaptToJson(LocalDate date) throws Exception {
                LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
                map.put("year", date.getYear());
                map.put("month", date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
                map.put("day", date.getDayOfMonth());
                map.put("dayOfWeek", date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
                return map;
            }
        };

        JsonbConfig config = new JsonbConfig().withAdapters(dateAdapter);
        Jsonb jsonb = JsonbBuilder.newBuilder().withConfig(config).build();
        System.out.println("Jsonb implementation is " + jsonb);

        String json;

        // Write Java Object to JSON
        Location swRochesterMN = new Location();
        swRochesterMN.setCity("Rochester");
        swRochesterMN.setCounty("Olmsted");
        swRochesterMN.setState("Minnesota");
        swRochesterMN.setZipCode(55902);
        json = jsonb.toJson(swRochesterMN);
        System.out.println("Location written to JSON as " + json);

        // Unmarshall JSON data into Java Object
        InputStream in = JsonbAppClient.class.getResourceAsStream("/META-INF/json_weather_data.js");
        OneDayWeatherForecast forecast = jsonb.fromJson(in, OneDayWeatherForecast.class);
        System.out.println("Forecast for " + forecast.date + " in " + forecast.location.getZipCode());
        System.out.println("Chance of snow at 1am is " + forecast.hourlyForecast[1].chanceOfSnow * 100.0f + "%");
        System.out.println("Expected wind chill at 10am is " + forecast.hourlyForecast[10].windchill);
        System.out.println("Expected temperature at 10pm is " + forecast.hourlyForecast[22].temp);

        // Modify the data and write it back to JSON
        forecast.date = forecast.date.plusDays(2);
        forecast.location = swRochesterMN;
        for (HourlyWeatherForecast h : forecast.hourlyForecast) {
            h.temp += 15;
            h.windchill += 15;
        }
        json = jsonb.toJson(forecast);
        System.out.println("Thursday's forecast in JSON " + json);

        System.out.println("\nJSON-B Application Client Completed.");
    }
}
