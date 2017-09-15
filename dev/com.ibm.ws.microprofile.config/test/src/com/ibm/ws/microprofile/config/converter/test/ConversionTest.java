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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import java.time.temporal.ChronoUnit;
import java.util.BitSet;
import java.util.Currency;
import java.util.Locale;

import javax.xml.bind.DatatypeConverter;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.ws.microprofile.config.dynamic.test.TestDynamicConfigSource;
import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;
import com.ibm.ws.microprofile.config.interfaces.DefaultConverters;

public class ConversionTest {

    @Test
    public void testString() {
        String value = "TEST";
        System.out.println("String :" + value);
        String converted = DefaultConverters.STRING_CONVERTER.convert(value);
        assertEquals(value, converted);
    }

    @Test
    public void testBooleanTrue() {
        Boolean booleanValue = Boolean.TRUE;
        String raw = "1";
        assertConversion(raw, booleanValue, Boolean.class);
    }

    @Test
    public void testBooleanFalse() {
        Boolean booleanValue = Boolean.FALSE;
        String raw = "gibberish";
        assertConversion(raw, booleanValue, Boolean.class);
    }

    @Test
    public void testLongZero() {
        Long longValue = 0l;
        String raw = "" + longValue;
        assertConversion(raw, longValue, Long.class);
    }

    @Test
    public void testLongMin() {
        Long longValue = Long.MIN_VALUE;
        String raw = "" + longValue;
        assertConversion(raw, longValue, Long.class);
    }

    @Test
    public void testLongMax() {
        Long longValue = Long.MAX_VALUE;
        String raw = "" + longValue;
        assertConversion(raw, longValue, Long.class);
    }

    @Test
    public void testLongCache() {
        Long longValue = Long.MAX_VALUE;
        String raw = "" + longValue;
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        TestDynamicConfigSource source = new TestDynamicConfigSource();
        source.put("key1", raw);
        builder.withSources(source);
        Config config = builder.build();

        Long long1 = config.getValue("key1", Long.class);
        Long long2 = config.getValue("key1", Long.class);
        assertTrue(long1 == long2);

    }

    @Test
    public void testBadDuration() {
        String raw = "There is no spoon";
        assertBadConversion(raw, Duration.class);
    }

    @Test
    public void testPeriodZero() {
        Period period = Period.ZERO;
        String raw = period.toString();

        assertConversion(raw, period, Period.class);
    }

    @Test
    public void testPeriod100days() {
        Period period = Period.between(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(100));
        String raw = period.toString();

        assertConversion(raw, period, Period.class);
    }

    @Test
    public void testLocalDateTime() {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
        String raw = localDateTime.toString();

        assertConversion(raw, localDateTime, LocalDateTime.class);
    }

    @Test
    public void testLocalDate() {
        LocalDate localDate = LocalDate.ofEpochDay(0);
        String raw = localDate.toString();
        assertConversion(raw, localDate, LocalDate.class);
    }

    @Test
    public void testLocalTime() {
        LocalTime localTime = LocalTime.MIDNIGHT;
        String raw = localTime.toString();
        assertConversion(raw, localTime, LocalTime.class);
    }

    @Test
    public void testOffsetDateTimeMin() {
        OffsetDateTime offsetDateTime = OffsetDateTime.MIN;
        String raw = offsetDateTime.toString();

        assertConversion(raw, offsetDateTime, OffsetDateTime.class);
    }

    @Test
    public void testOffsetDateTimeMax() {
        OffsetDateTime offsetDateTime = OffsetDateTime.MAX;
        String raw = offsetDateTime.toString();

        assertConversion(raw, offsetDateTime, OffsetDateTime.class);
    }

    @Test
    public void testOffsetTimeMin() {
        OffsetTime offsetTime = OffsetTime.MIN;
        String raw = offsetTime.toString();

        assertConversion(raw, offsetTime, OffsetTime.class);
    }

    @Test
    public void testOffsetTimeMax() {
        OffsetTime offsetTime = OffsetTime.MAX;
        String raw = offsetTime.toString();

        assertConversion(raw, offsetTime, OffsetTime.class);
    }

    @Test
    public void testZonedDateTime() {
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
        String raw = zonedDateTime.toString();

        assertConversion(raw, zonedDateTime, ZonedDateTime.class);
    }

    @Test
    public void testInstant() {
        Instant instant = Instant.EPOCH;
        String raw = instant.toString();

        assertConversion(raw, instant, Instant.class);
    }

    @Test
    public void testCurrency() {
        Currency currency = Currency.getInstance(Locale.UK);
        String raw = "" + currency;

        assertConversion(raw, currency, Currency.class);
    }

    @Test
    public void testBitSet() {
        BitSet bitSet = BitSet.valueOf(new long[] { 5, 5 });
        String raw = "" + DatatypeConverter.printHexBinary(bitSet.toByteArray());

        assertConversion(raw, bitSet, BitSet.class);
    }

    @Test
    public void testBitSetMax() {
        BitSet bitSet = BitSet.valueOf(new long[] { Long.MAX_VALUE, Long.MAX_VALUE });
        String raw = "" + DatatypeConverter.printHexBinary(bitSet.toByteArray());

        assertConversion(raw, bitSet, BitSet.class);
    }

    @Test
    public void testURI() throws URISyntaxException {
        String raw = "../../resource.txt";
        URI uri = new URI(raw);

        assertConversion(raw, uri, URI.class);
    }

    @Test
    public void testURL() throws MalformedURLException {
        String raw = "http://www.ibm.com";
        URL url = new URL(raw);

        assertConversion(raw, url, URL.class);
    }

    @Test
    public void testChronoUnit() {
        assertConversion("SECONDS", ChronoUnit.SECONDS, ChronoUnit.class);
    }

    public <T> void assertConversion(String rawString, T expected, Class<T> type) {
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        TestDynamicConfigSource source = new TestDynamicConfigSource();
        source.put("key1", rawString);

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDiscoveredConverters();
        builder.withSources(source);
        Config config = builder.build();

        T value = config.getValue("key1", type);
        assertEquals(expected, value);
    }

    public <T> void assertBadConversion(String rawString, Class<T> type) {
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        assertBadConversion(builder, rawString, type);
    }

    public <T> void assertBadConversion(ConfigBuilder builder, String rawString, Class<T> type) {
        TestDynamicConfigSource source = new TestDynamicConfigSource();
        builder.withSources(source);
        builder.addDiscoveredConverters();

        source.put("key1", rawString);
        Config config = builder.build();

        try {
            config.getValue("key1", type);
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void testCustomConverter() {
        ConverterA<ClassB> converter = new ConverterA<>();
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withConverters(converter);
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        TestDynamicConfigSource source = new TestDynamicConfigSource();
        source.put("key1", "value1");
        builder.withSources(source);
        Config config = builder.build();
        ClassB classB = config.getValue("key1", ClassB.class);
        assertEquals("value1", classB.getValue());
    }

    @Test
    public void testConverterException() {
        BadConverter converter = new BadConverter();
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withConverters(converter);
        assertBadConversion(builder, "value1", String.class);
    }

    @Test
    public void testCustomConverterCaching() {
        ConverterA<ClassB> converter = new ConverterA<>();
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withConverters(converter);
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        TestDynamicConfigSource source = new TestDynamicConfigSource();
        source.put("key1", "value1");
        builder.withSources(source);
        Config config = builder.build();

        ClassB classB = config.getValue("key1", ClassB.class);
        ClassB classB2 = config.getValue("key1", ClassB.class);
        assertEquals(classB, classB2);
        assertEquals(1, converter.getConversionCount());
    }

    @Test
    @Ignore
    public void testCustomArray() {
        ConverterA<ClassB> converter = new ConverterA<>();
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withConverters(converter);
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        TestDynamicConfigSource source = new TestDynamicConfigSource();
        source.put("key1", "value1,value2,value3,value4");
        builder.withSources(source);
        Config config = builder.build();
        ClassB[] classB = config.getValue("key1", ClassB[].class);
        assertEquals("value1", classB[0].getValue());
        assertEquals("value2", classB[1].getValue());
        assertEquals("value3", classB[2].getValue());
        assertEquals("value4", classB[3].getValue());
    }

    @Test
    public void testPrimitiveInt() {
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        TestDynamicConfigSource source = new TestDynamicConfigSource();
        source.put("key1", "1");
        builder.withSources(source);
        Config config = builder.build();
        int key1 = config.getValue("key1", int.class);
        assertEquals(1, key1);
    }

    @Test
    @Ignore //creating a primitive array doesn't work at the moment
    public void testIntArray() {
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        TestDynamicConfigSource source = new TestDynamicConfigSource();
        source.put("key1", "1,2,3,4");
        builder.withSources(source);
        Config config = builder.build();
        int[] key1 = config.getValue("key1", int[].class);
        assertEquals(1, key1[0]);
        assertEquals(2, key1[1]);
        assertEquals(3, key1[2]);
        assertEquals(4, key1[3]);

    }

    @Test
    @Ignore
    public void testIntegerArray() {
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        TestDynamicConfigSource source = new TestDynamicConfigSource();
        source.put("key1", "1,2,3,4");
        builder.withSources(source);
        Config config = builder.build();
        Integer[] key1 = config.getValue("key1", Integer[].class);
        assertEquals(new Integer(1), key1[0]);
        assertEquals(new Integer(2), key1[1]);
        assertEquals(new Integer(3), key1[2]);
        assertEquals(new Integer(4), key1[3]);

    }

    //Test to verify a null is a permitted return value from a converter
    @Test
    public void testNullAllowed() {
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        SimpleConfigSource source = new SimpleConfigSource();
        source.put("key1", "");

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDiscoveredConverters();
        builder.withSources(source);
        builder.withConverters(new ConverterD());
        Config config = builder.build();

        ClassD value = config.getValue("key1", ClassD.class);
        assertNull("The converted value should be null", value);
    }

    @Test
    public void testDiscoveredConverter() {
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        TestDynamicConfigSource source = new TestDynamicConfigSource();
        source.put("key1", "value1");
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDiscoveredConverters();
        builder.withSources(source);
        Config config = builder.build();
        ClassC classC = config.getValue("key1", ClassC.class);
        assertEquals("value1", classC.getValue());
    }

    @Test
    public void testConverterPriority() {
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        TestDynamicConfigSource source = new TestDynamicConfigSource();
        source.put("key1", "value1");
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(source);
        builder.withConverters(new StringConverter101());
        builder.withConverters(new StringConverter103());
        builder.withConverters(new StringConverter102());
        Config config = builder.build();
        String result = config.getValue("key1", String.class);
        assertEquals("103=value1", result);
    }

    @Test
    public void testConverterEqualPriority() {
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        TestDynamicConfigSource source = new TestDynamicConfigSource();
        source.put("key1", "value1");
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(source);
        builder.withConverters(new StringConverter101b());
        builder.withConverters(new StringConverter101());
        builder.withConverters(new StringConverter101c());//this one has same priority as previous two but is added last, should be used
        builder.withConverters(new StringConverterDefault());//this one does not have a @Priority, should default to 100 so should not be used

        Config config = builder.build();
        String result = config.getValue("key1", String.class);
        assertEquals("101c=value1", result);
    }

    @After
    public void resetRefresh() {
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "");
    }

}
