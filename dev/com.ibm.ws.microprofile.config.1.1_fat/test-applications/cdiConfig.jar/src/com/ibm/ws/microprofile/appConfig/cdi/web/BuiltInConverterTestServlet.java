/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.cdi.web;

import java.util.BitSet;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.microprofile.appConfig.cdi.beans.BuiltInConverterInjectionBean;

@SuppressWarnings("serial")
@WebServlet("/builtIn")
public class BuiltInConverterTestServlet extends AbstractBeanServlet {

    @Inject
    BuiltInConverterInjectionBean configBean;

    @Test
    public void testBoolean() throws Exception {
        test("BOOLEAN_KEY", "true");
    }

    @Test
    public void testInteger() throws Exception {
        test("INTEGER_KEY", "2147483647");
    }

    @Test
    public void testInt() throws Exception {
        test("INT_KEY", "2147483647");
    }

    @Test
    public void testLong() throws Exception {
        test("LONG_KEY", "-9223372036854775808");
    }

    @Test
    public void testShort() throws Exception {
        test("SHORT_KEY", "32767");
    }

    @Test
    public void testByte() throws Exception {
        test("BYTE_KEY", "-128");
    }

    @Test
    public void testDouble() throws Exception {
        test("DOUBLE_KEY", "1.7976931348623157E308");
    }

    @Test
    public void testFloat() throws Exception {
        test("FLOAT_KEY", "1.4E-45");
    }

    @Test
    public void testBigInteger() throws Exception {
        test("BIG_INTEGER_KEY", "697627659869078390664");
    }

    @Test
    public void testBigDecimal() throws Exception {
        test("BIG_DECIMAL_KEY", "899559405265203640297");
    }

    @Test
    public void testDuration() throws Exception {
        test("DURATION_KEY", "PT1S");
    }

    @Test
    public void testLocalDateTime() throws Exception {
        test("LOCAL_DATE_TIME_KEY", "1970-01-01T01:00");
    }

    @Test
    public void testLocalDate() throws Exception {
        test("LOCAL_DATE_KEY", "1970-01-01");
    }

    @Test
    public void testLocalTime() throws Exception {
        test("LOCAL_TIME_KEY", "00:00");
    }

    @Test
    public void testOffsetDateTime() throws Exception {
        test("OFFSET_DATE_TIME_KEY", "-999999999-01-01T00:00+18:00");
    }

    @Test
    public void testOffsetTime() throws Exception {
        test("OFFSET_TIME_KEY", "23:59:59.999999999-18:00");
    }

    @Test
    public void testZonedDateTime() throws Exception {
        test("ZONED_DATE_TIME_KEY", "1970-01-01T01:00+01:00[Europe/London]");
    }

    @Test
    public void testInstant() throws Exception {
        test("INSTANT_KEY", "1970-01-01T00:00:00Z");
    }

    @Test
    public void testCurrency() throws Exception {
        test("CURRENCY_KEY", "GBP");
    }

    @Test
    public void testBitSet() throws Exception {
        BitSet expected = new BitSet(8);
        expected.set(1);
        expected.set(3);
        expected.set(5);
        expected.set(7);
        test("BIT_SET_KEY", expected.toString());
    }

    @Test
    public void testURI() throws Exception {
        test("URI_KEY", "../../resource.txt");
    }

    @Test
    public void testURL() throws Exception {
        test("URL_KEY", "http://www.ibm.com");
    }

    /** {@inheritDoc} */
    @Override
    public Object getBean() {
        return configBean;
    }

}
