/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.convertdatetime.beans;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 * RequestScoped bean to test Java time support
 */
@Named
@RequestScoped
public class DateTimeBean implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    private Date date;
    private final LocalDate localDate;
    private final LocalTime localTime;
    private final LocalDateTime localDateTime;
    private final OffsetTime offsetTime;
    private final OffsetDateTime offsetDateTime;
    private final ZonedDateTime zonedDateTime;

    public DateTimeBean() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String testDate = "2017-06-01 10:30:45";

        try {
            date = sdf.parse(testDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        localDate = LocalDate.of(2017, 06, 01);
        localTime = LocalTime.of(10, 35, 45, 500000000);
        localDateTime = LocalDateTime.of(2017, 06, 01, 10, 30, 45);
        offsetTime = OffsetTime.of(10, 30, 45, 500000000, ZoneOffset.ofHours(-7));
        offsetDateTime = OffsetDateTime.of(2017, 06, 01, 10, 30, 45, 500000000, ZoneOffset.ofHours(-7));
        zonedDateTime = ZonedDateTime.of(2017, 06, 01, 10, 30, 45, 500000000, ZoneId.of("America/Los_Angeles"));
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public LocalTime getLocalTime() {
        return localTime;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public OffsetTime getOffsetTime() {
        return offsetTime;
    }

    public OffsetDateTime getOffsetDateTime() {
        return offsetDateTime;
    }

    public ZonedDateTime getZonedDateTime() {
        return zonedDateTime;
    }

}
