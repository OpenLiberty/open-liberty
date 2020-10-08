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
package com.ibm.ws.microprofile.appConfig.cdi.beans;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.BitSet;
import java.util.Currency;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@RequestScoped
public class BuiltInConverterInjectionBean {

    @Inject
    @ConfigProperty(name = "INTEGER_KEY")
    Integer INTEGER_KEY;

    @Inject
    @ConfigProperty(name = "INTEGER_KEY")
    int INT_KEY;

    @Inject
    @ConfigProperty(name = "BOOLEAN_KEY")
    Boolean BOOLEAN_KEY;

    @Inject
    @ConfigProperty(name = "LONG_KEY")
    Long LONG_KEY;

    @Inject
    @ConfigProperty(name = "SHORT_KEY")
    Short SHORT_KEY;

    @Inject
    @ConfigProperty(name = "BYTE_KEY")
    Byte BYTE_KEY;

    @Inject
    @ConfigProperty(name = "DOUBLE_KEY")
    Double DOUBLE_KEY;

    @Inject
    @ConfigProperty(name = "FLOAT_KEY")
    Float FLOAT_KEY;

    @Inject
    @ConfigProperty(name = "BIG_INTEGER_KEY")
    BigInteger BIG_INTEGER_KEY;

    @Inject
    @ConfigProperty(name = "BIG_DECIMAL_KEY")
    BigDecimal BIG_DECIMAL_KEY;

    @Inject
    @ConfigProperty(name = "DURATION_KEY")
    Duration DURATION_KEY;

    @Inject
    @ConfigProperty(name = "LOCAL_DATE_TIME_KEY")
    LocalDateTime LOCAL_DATE_TIME_KEY;

    @Inject
    @ConfigProperty(name = "LOCAL_DATE_KEY")
    LocalDate LOCAL_DATE_KEY;

    @Inject
    @ConfigProperty(name = "LOCAL_TIME_KEY")
    LocalTime LOCAL_TIME_KEY;

    @Inject
    @ConfigProperty(name = "OFFSET_DATE_TIME_KEY")
    OffsetDateTime OFFSET_DATE_TIME_KEY;

    @Inject
    @ConfigProperty(name = "OFFSET_TIME_KEY")
    OffsetTime OFFSET_TIME_KEY;

    @Inject
    @ConfigProperty(name = "ZONED_DATE_TIME_KEY")
    ZonedDateTime ZONED_DATE_TIME_KEY;

    @Inject
    @ConfigProperty(name = "INSTANT_KEY")
    Instant INSTANT_KEY;

    @Inject
    @ConfigProperty(name = "CURRENCY_KEY")
    Currency CURRENCY_KEY;

    @Inject
    @ConfigProperty(name = "BIT_SET_KEY")
    BitSet BIT_SET_KEY;

    @Inject
    @ConfigProperty(name = "URI_KEY")
    URI URI_KEY;

    @Inject
    @ConfigProperty(name = "URL_KEY")
    URL URL_KEY;

    public Boolean getBOOLEAN_KEY() {
        return BOOLEAN_KEY;
    }

    /**
     * @return the iNTEGER_KEY
     */
    public Integer getINTEGER_KEY() {
        return INTEGER_KEY;
    }

    public int getINT_KEY() {
        return INT_KEY;
    }

    /**
     * @return the lONG_KEY
     */
    public Long getLONG_KEY() {
        return LONG_KEY;
    }

    /**
     * @return the sHORT_KEY
     */
    public Short getSHORT_KEY() {
        return SHORT_KEY;
    }

    /**
     * @return the bYTE_KEY
     */
    public Byte getBYTE_KEY() {
        return BYTE_KEY;
    }

    /**
     * @return the dOUBLE_KEY
     */
    public Double getDOUBLE_KEY() {
        return DOUBLE_KEY;
    }

    /**
     * @return the fLOAT_KEY
     */
    public Float getFLOAT_KEY() {
        return FLOAT_KEY;
    }

    /**
     * @return the bIG_INTEGER_KEY
     */
    public BigInteger getBIG_INTEGER_KEY() {
        return BIG_INTEGER_KEY;
    }

    /**
     * @return the bIG_DECIMAL_KEY
     */
    public BigDecimal getBIG_DECIMAL_KEY() {
        return BIG_DECIMAL_KEY;
    }

    /**
     * @return the dURATION_KEY
     */
    public Duration getDURATION_KEY() {
        return DURATION_KEY;
    }

    /**
     * @return the lOCAL_DATE_TIME_KEY
     */
    public LocalDateTime getLOCAL_DATE_TIME_KEY() {
        return LOCAL_DATE_TIME_KEY;
    }

    /**
     * @return the lOCAL_DATE_KEY
     */
    public LocalDate getLOCAL_DATE_KEY() {
        return LOCAL_DATE_KEY;
    }

    /**
     * @return the lOCAL_TIME_KEY
     */
    public LocalTime getLOCAL_TIME_KEY() {
        return LOCAL_TIME_KEY;
    }

    /**
     * @return the oFFSET_DATE_TIME_KEY
     */
    public OffsetDateTime getOFFSET_DATE_TIME_KEY() {
        return OFFSET_DATE_TIME_KEY;
    }

    /**
     * @return the oFFSET_TIME_KEY
     */
    public OffsetTime getOFFSET_TIME_KEY() {
        return OFFSET_TIME_KEY;
    }

    /**
     * @return the zONED_DATE_TIME_KEY
     */
    public ZonedDateTime getZONED_DATE_TIME_KEY() {
        return ZONED_DATE_TIME_KEY;
    }

    /**
     * @return the iNSTANT_KEY
     */
    public Instant getINSTANT_KEY() {
        return INSTANT_KEY;
    }

    /**
     * @return the cURRENCY_KEY
     */
    public Currency getCURRENCY_KEY() {
        return CURRENCY_KEY;
    }

    /**
     * @return the bIT_SET_KEY
     */
    public BitSet getBIT_SET_KEY() {
        return BIT_SET_KEY;
    }

    /**
     * @return the uRI_KEY
     */
    public URI getURI_KEY() {
        return URI_KEY;
    }

    /**
     * @return the uRL_KEY
     */
    public URL getURL_KEY() {
        return URL_KEY;
    }

    public AtomicInteger getATOMIC_INTEGER_KEY() {
        return null;
    }

    public AtomicLong getATOMIC_LONG_KEY() {
        return null;
    }
}