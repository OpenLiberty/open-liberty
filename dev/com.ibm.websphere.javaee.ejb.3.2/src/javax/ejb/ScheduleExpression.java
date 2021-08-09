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
package javax.ejb;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.Serializable;
import java.util.Date;

/**
 * This is a custom implementation of ScheduleExpression that handles a
 * serialization incompatibility between the RI implementation for EJB 3.2 and
 * the version used for EJB 3.1. The serial version identifier is the same for
 * both implementations, however the instance variables have different names,
 * so default serialization is not able to restore the values when deserializing
 * bytes from the older implementation using the RI implementation. <p>
 *
 * Since an instance of ScheduleExpression is serialized and stored in a
 * database for persistent calendar based EJB timers, supporting the
 * deserialization of both versions of the class is needed for applications
 * migrating from EJB 3.1 to EJB 3.2.
 */
public class ScheduleExpression implements Serializable {

    private static final long serialVersionUID = -3813254457230997879L;

    public ScheduleExpression() {
        second_ = "0";
        minute_ = "0";
        hour_ = "0";
        dayOfMonth_ = "*";
        month_ = "*";
        dayOfWeek_ = "*";
        year_ = "*";
        timezoneID_ = null;
        start_ = null;
        end_ = null;
    }

    public ScheduleExpression second(String s) {
        second_ = s;
        return this;
    }

    public ScheduleExpression second(int s) {
        second_ = s + "";
        return this;
    }

    public String getSecond() {
        return second_;
    }

    public ScheduleExpression minute(String m) {
        minute_ = m;
        return this;
    }

    public ScheduleExpression minute(int m) {
        minute_ = m + "";
        return this;
    }

    public String getMinute() {
        return minute_;
    }

    public ScheduleExpression hour(String h) {
        hour_ = h;
        return this;
    }

    public ScheduleExpression hour(int h) {
        hour_ = h + "";
        return this;
    }

    public String getHour() {
        return hour_;
    }

    public ScheduleExpression dayOfMonth(String d) {
        dayOfMonth_ = d;
        return this;
    }

    public ScheduleExpression dayOfMonth(int d) {
        dayOfMonth_ = d + "";
        return this;
    }

    public String getDayOfMonth() {
        return dayOfMonth_;
    }

    public ScheduleExpression month(String m) {
        month_ = m;
        return this;
    }

    public ScheduleExpression month(int m) {
        month_ = m + "";
        return this;
    }

    public String getMonth() {
        return month_;
    }

    public ScheduleExpression dayOfWeek(String d) {
        dayOfWeek_ = d;
        return this;
    }

    public ScheduleExpression dayOfWeek(int d) {
        dayOfWeek_ = d + "";
        return this;
    }

    public String getDayOfWeek() {
        return dayOfWeek_;
    }

    public ScheduleExpression year(String y) {
        year_ = y;
        return this;
    }

    public ScheduleExpression year(int y) {
        year_ = y + "";
        return this;
    }

    public String getYear() {
        return year_;
    }

    public ScheduleExpression timezone(String timezoneID) {
        timezoneID_ = timezoneID;
        return this;
    }

    public String getTimezone() {
        return timezoneID_;
    }

    public ScheduleExpression start(Date s) {
        start_ = (s == null) ? null : new Date(s.getTime());
        return this;
    }

    public Date getStart() {
        return (start_ == null) ? null : new Date(start_.getTime());
    }

    public ScheduleExpression end(Date e) {
        end_ = (e == null) ? null : new Date(e.getTime());
        return this;
    }

    public Date getEnd() {
        return (end_ == null) ? null : new Date(end_.getTime());
    }

    @Override
    public String toString() {
        return "ScheduleExpression [second=" + second_
               + ";minute=" + minute_
               + ";hour=" + hour_
               + ";dayOfMonth=" + dayOfMonth_
               + ";month=" + month_
               + ";dayOfWeek=" + dayOfWeek_
               + ";year=" + year_
               + ";timezoneID=" + timezoneID_
               + ";start=" + start_
               + ";end=" + end_
               + "]";
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {

        // Read the fields basically the same way as defaultReadObject
        GetField fields = in.readFields();

        // The month_ field must always have a value; so if it has not been defaulted
        // in the fields read, then this is EJB 3.2; otherwise it must be EJB 3.1.
        if (!fields.defaulted("month_")) {
            // Read the fields using the EJB 3.2 field names
            second_ = (String) fields.get("second_", "0");
            minute_ = (String) fields.get("minute_", "0");
            hour_ = (String) fields.get("hour_", "0");
            dayOfMonth_ = (String) fields.get("dayOfMonth_", "*");
            month_ = (String) fields.get("month_", "*");
            dayOfWeek_ = (String) fields.get("dayOfWeek_", "*");
            year_ = (String) fields.get("year_", "*");
            timezoneID_ = (String) fields.get("timezoneID_", null);
            start_ = (Date) fields.get("start_", null);
            end_ = (Date) fields.get("end_", null);
        } else {
            // Read the fields using the EJB 3.1 field names
            second_ = (String) fields.get("second", "0");
            minute_ = (String) fields.get("minute", "0");
            hour_ = (String) fields.get("hour", "0");
            dayOfMonth_ = (String) fields.get("dayOfMonth", "*");
            month_ = (String) fields.get("month", "*");
            dayOfWeek_ = (String) fields.get("dayOfWeek", "*");
            year_ = (String) fields.get("year", "*");
            timezoneID_ = (String) fields.get("timezone", null);
            start_ = (Date) fields.get("start", null);
            end_ = (Date) fields.get("end", null);
        }
    }

    private String second_;
    private String minute_;
    private String hour_;
    private String dayOfMonth_;
    private String month_;
    private String dayOfWeek_;
    private String year_;
    private String timezoneID_;
    private Date start_;
    private Date end_;
}
