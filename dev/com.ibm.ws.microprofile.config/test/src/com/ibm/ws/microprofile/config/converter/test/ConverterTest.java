/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.converter.test;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.BitSet;
import java.util.Currency;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.bind.DatatypeConverter;

import org.junit.Test;

import com.ibm.ws.microprofile.config.converters.DefaultConverters;

public class ConverterTest {

    private static <T> T defaultConversion(Class<T> type, String value) {
        return (T) DefaultConverters.getDefaultConverters().getConverter(type).convert(value);
    }

    @Test
    public void testString() {
        String value = "TEST";
        System.out.println("String :" + value);
        String converted = defaultConversion(String.class, value);
        assertEquals(value, converted);

        value = null;
        System.out.println("String :" + value);
        converted = defaultConversion(String.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testBoolean() {
        String value = "true";
        System.out.println("Boolean :" + value);
        Boolean converted = defaultConversion(Boolean.class, value);
        assertEquals(Boolean.TRUE, converted);

        value = "yEs";
        converted = defaultConversion(Boolean.class, value);
        assertEquals(Boolean.TRUE, converted);

        value = "on";
        converted = defaultConversion(Boolean.class, value);
        assertEquals(Boolean.TRUE, converted);

        value = "Y";
        converted = defaultConversion(Boolean.class, value);
        assertEquals(Boolean.TRUE, converted);

        value = "1";
        converted = defaultConversion(Boolean.class, value);
        assertEquals(Boolean.TRUE, converted);

        value = "FALSe";
        converted = defaultConversion(Boolean.class, value);
        assertEquals(Boolean.FALSE, converted);

        value = "nO";
        converted = defaultConversion(Boolean.class, value);
        assertEquals(Boolean.FALSE, converted);

        value = "off";
        converted = defaultConversion(Boolean.class, value);
        assertEquals(Boolean.FALSE, converted);

        value = "17";
        converted = defaultConversion(Boolean.class, value);
        assertEquals(Boolean.FALSE, converted);

        value = null;
        converted = defaultConversion(Boolean.class, value);
        assertEquals(null, converted);
    }

    @Test
    public void testInteger() {
        String value = "0";
        Integer converted = defaultConversion(Integer.class, value);
        assertEquals(new Integer(0), converted);

        value = "" + Integer.MAX_VALUE;
        System.out.println("Integer :" + value);
        converted = defaultConversion(Integer.class, value);
        assertEquals(new Integer(Integer.MAX_VALUE), converted);

        value = "" + Integer.MIN_VALUE;
        converted = defaultConversion(Integer.class, value);
        assertEquals(new Integer(Integer.MIN_VALUE), converted);

        value = null;
        converted = defaultConversion(Integer.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testLong() {
        String value = "0";
        Long converted = defaultConversion(Long.class, value);
        assertEquals(new Long(0), converted);

        value = "" + Long.MAX_VALUE;
        converted = defaultConversion(Long.class, value);
        assertEquals(new Long(Long.MAX_VALUE), converted);

        value = "" + Long.MIN_VALUE;
        System.out.println("Long :" + value);
        converted = defaultConversion(Long.class, value);
        assertEquals(new Long(Long.MIN_VALUE), converted);

        value = null;
        System.out.println("Long :" + value);
        converted = defaultConversion(Long.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testShort() {
        String value = "0";
        Short converted = defaultConversion(Short.class, value);
        assertEquals(new Short((short) 0), converted);

        value = "" + Short.MAX_VALUE;
        System.out.println("Short :" + value);
        converted = defaultConversion(Short.class, value);
        assertEquals(new Short(Short.MAX_VALUE), converted);

        value = "" + Short.MIN_VALUE;
        converted = defaultConversion(Short.class, value);
        assertEquals(new Short(Short.MIN_VALUE), converted);

        value = null;
        converted = defaultConversion(Short.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testByte() {
        String value = "0";
        Byte converted = defaultConversion(Byte.class, value);
        assertEquals(new Byte((byte) 0), converted);

        value = "" + Byte.MAX_VALUE;
        converted = defaultConversion(Byte.class, value);
        assertEquals(new Byte(Byte.MAX_VALUE), converted);

        value = "" + Byte.MIN_VALUE;
        System.out.println("Byte :" + value);
        converted = defaultConversion(Byte.class, value);
        assertEquals(new Byte(Byte.MIN_VALUE), converted);

        value = null;
        System.out.println("Byte :" + value);
        converted = defaultConversion(Byte.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testDouble() {
        String value = "0.0";
        Double converted = defaultConversion(Double.class, value);
        assertEquals(new Double(0.0), converted);

        value = "" + Double.MAX_VALUE;
        System.out.println("Double :" + value);
        converted = defaultConversion(Double.class, value);
        assertEquals(new Double(Double.MAX_VALUE), converted);

        value = "" + Double.MIN_VALUE;
        converted = defaultConversion(Double.class, value);
        assertEquals(new Double(Double.MIN_VALUE), converted);

        value = null;
        converted = defaultConversion(Double.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testFloat() {
        String value = "0.0";
        Float converted = defaultConversion(Float.class, value);
        assertEquals(new Float(0.0), converted);

        value = "" + Float.MAX_VALUE;
        converted = defaultConversion(Float.class, value);
        assertEquals(new Float(Float.MAX_VALUE), converted);

        value = "" + Float.MIN_VALUE;
        System.out.println("Float :" + value);
        converted = defaultConversion(Float.class, value);
        assertEquals(new Float(Float.MIN_VALUE), converted);

        value = null;
        System.out.println("Float :" + value);
        converted = defaultConversion(Float.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testBigInteger() {
        String value = "0";
        BigInteger converted = defaultConversion(BigInteger.class, value);
        assertEquals(BigInteger.ZERO, converted);

        Random rnd = new Random();
        BigInteger bigInt = new BigInteger(70, rnd);
        value = "" + bigInt;
        System.out.println("BigInt :" + value);
        converted = defaultConversion(BigInteger.class, value);
        assertEquals(bigInt, converted);

        value = null;
        System.out.println("BigInt :" + value);
        converted = defaultConversion(BigInteger.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testBigDecimal() {
        String value = "0";
        BigDecimal converted = defaultConversion(BigDecimal.class, value);
        assertEquals(BigDecimal.ZERO, converted);

        Random rnd = new Random();
        BigInteger bigInt = new BigInteger(70, rnd);
        BigDecimal bigDec = new BigDecimal(bigInt);
        value = "" + bigDec;
        System.out.println("BigDec :" + value);
        converted = defaultConversion(BigDecimal.class, value);
        assertEquals(bigDec, converted);

        value = null;
        System.out.println("BigDec :" + value);
        converted = defaultConversion(BigDecimal.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testAtomicInteger() {
        String value = "0";
        AtomicInteger converted = defaultConversion(AtomicInteger.class, value);
        assertEquals(0, converted.get());

        value = "" + Integer.MAX_VALUE;
        converted = defaultConversion(AtomicInteger.class, value);
        assertEquals(Integer.MAX_VALUE, converted.get());

        value = "" + Integer.MIN_VALUE;
        converted = defaultConversion(AtomicInteger.class, value);
        assertEquals(Integer.MIN_VALUE, converted.get());

        value = null;
        converted = defaultConversion(AtomicInteger.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testAtomicLong() {
        String value = "0";
        AtomicLong converted = defaultConversion(AtomicLong.class, value);
        assertEquals(0l, converted.get());

        value = "" + Long.MAX_VALUE;
        converted = defaultConversion(AtomicLong.class, value);
        assertEquals(Long.MAX_VALUE, converted.get());

        value = "" + Long.MIN_VALUE;
        converted = defaultConversion(AtomicLong.class, value);
        assertEquals(Long.MIN_VALUE, converted.get());

        value = null;
        converted = defaultConversion(AtomicLong.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testDuration() {
        String value = Duration.ZERO.toString();
        Duration converted = defaultConversion(Duration.class, value);
        assertEquals(Duration.ZERO, converted);

        Duration duration = Duration.between(Instant.EPOCH, Instant.EPOCH.plusMillis(1000));
        value = duration.toString();
        System.out.println("Duration :" + value);
        converted = defaultConversion(Duration.class, value);
        assertEquals(duration, converted);

        value = null;
        System.out.println("Duration :" + value);
        converted = defaultConversion(Duration.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testPeriod() {
        String value = Period.ZERO.toString();
        Period converted = defaultConversion(Period.class, value);
        assertEquals(Period.ZERO, converted);

        Period period = Period.between(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(100));
        value = period.toString();
        System.out.println("Period :" + value);
        converted = defaultConversion(Period.class, value);
        assertEquals(period, converted);

        value = null;
        System.out.println("Period :" + value);
        converted = defaultConversion(Period.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testLocalDateTime() {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
        String value = localDateTime.toString();
        System.out.println("LocalDateTime :" + value);
        LocalDateTime converted = defaultConversion(LocalDateTime.class, value);
        assertEquals(localDateTime, converted);

        value = null;
        System.out.println("LocalDateTime :" + value);
        converted = defaultConversion(LocalDateTime.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testLocalDate() {
        LocalDate localDate = LocalDate.ofEpochDay(0);
        String value = localDate.toString();
        System.out.println("LocalDate :" + value);
        LocalDate converted = defaultConversion(LocalDate.class, value);
        assertEquals(localDate, converted);

        value = null;
        System.out.println("LocalDate :" + value);
        converted = defaultConversion(LocalDate.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testLocalTime() {
        LocalTime localTime = LocalTime.MIDNIGHT;
        String value = localTime.toString();
        System.out.println("LocalTime :" + value);
        LocalTime converted = defaultConversion(LocalTime.class, value);
        assertEquals(localTime, converted);

        value = null;
        System.out.println("LocalTime :" + value);
        converted = defaultConversion(LocalTime.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testOffsetDateTime() {
        OffsetDateTime offsetDateTime = OffsetDateTime.MIN;
        String value = offsetDateTime.toString();
        System.out.println("OffsetDateTime :" + value);
        OffsetDateTime converted = defaultConversion(OffsetDateTime.class, value);
        assertEquals(offsetDateTime, converted);

        offsetDateTime = OffsetDateTime.MAX;
        value = offsetDateTime.toString();
        converted = defaultConversion(OffsetDateTime.class, value);
        assertEquals(offsetDateTime, converted);

        value = null;
        converted = defaultConversion(OffsetDateTime.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testOffsetTime() {
        OffsetTime offsetTime = OffsetTime.MIN;
        String value = offsetTime.toString();
        OffsetTime converted = defaultConversion(OffsetTime.class, value);
        assertEquals(offsetTime, converted);

        offsetTime = OffsetTime.MAX;
        value = offsetTime.toString();
        System.out.println("OffsetTime :" + value);
        converted = defaultConversion(OffsetTime.class, value);
        assertEquals(offsetTime, converted);

        value = null;
        System.out.println("OffsetTime :" + value);
        converted = defaultConversion(OffsetTime.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testZonedDateTime() {
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
        String value = zonedDateTime.toString();
        System.out.println("ZonedDateTime :" + value);
        ZonedDateTime converted = defaultConversion(ZonedDateTime.class, value);
        assertEquals(zonedDateTime, converted);

        value = null;
        System.out.println("ZonedDateTime :" + value);
        converted = defaultConversion(ZonedDateTime.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testInstant() {
        Instant instant = Instant.EPOCH;
        String value = instant.toString();
        System.out.println("Instant :" + value);
        Instant converted = defaultConversion(Instant.class, value);
        assertEquals(instant, converted);

        value = null;
        System.out.println("Instant :" + value);
        converted = defaultConversion(Instant.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testCurrency() {
        Currency currency = Currency.getInstance(Locale.UK);
        String value = "" + currency;
        System.out.println("Currency :" + value);
        Currency converted = defaultConversion(Currency.class, value);
        assertEquals(currency, converted);

        value = null;
        System.out.println("Currency :" + value);
        converted = defaultConversion(Currency.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testBitSet() {
        BitSet bitSet = BitSet.valueOf(new long[] { 5, 5 });
        String value = "" + DatatypeConverter.printHexBinary(bitSet.toByteArray());
        System.out.println("BitSet :" + value);
        BitSet converted = defaultConversion(BitSet.class, value);
        assertEquals(bitSet, converted);

        bitSet = BitSet.valueOf(new long[] { Long.MAX_VALUE, Long.MAX_VALUE });
        value = "" + DatatypeConverter.printHexBinary(bitSet.toByteArray());
        converted = defaultConversion(BitSet.class, value);
        assertEquals(bitSet, converted);

        value = null;
        converted = defaultConversion(BitSet.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testURI() throws URISyntaxException {
        URI uri = new URI("../../resource.txt");
        String value = "" + uri;
        System.out.println("URI :" + uri);
        URI converted = defaultConversion(URI.class, value);
        assertEquals(uri, converted);

        value = null;
        System.out.println("URI :" + uri);
        converted = defaultConversion(URI.class, value);
        assertEquals(value, converted);
    }

    @Test
    public void testURL() throws MalformedURLException {
        URL url = new URL("http://www.ibm.com");
        String value = "" + url;
        System.out.println("URL :" + url);
        URL converted = defaultConversion(URL.class, value);
        assertEquals(url, converted);

        value = null;
        System.out.println("URL :" + url);
        converted = defaultConversion(URL.class, value);
        assertEquals(value, converted);
    }
}
