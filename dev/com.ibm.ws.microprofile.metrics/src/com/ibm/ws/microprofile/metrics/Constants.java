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
package com.ibm.ws.microprofile.metrics;

import java.util.Arrays;
import java.util.List;

import org.eclipse.microprofile.metrics.MetricRegistry;

/**
 *
 */
public class Constants {
    // HTTP Headers
    public final static String ACCEPT_HEADER = "Accept";
    public final static String ACCEPT_HEADER_JSON = "application/json";
    public final static String ACCEPT_HEADER_TEXT = "text/plain";

    // HTTP Methods
    public final static String METHOD_GET = "GET";
    public final static String METHOD_OPTIONS = "OPTIONS";

    // Paths
    public final static String PATH_METRICS = "/metrics";
    public final static String PATH_ROOT = "/";
    public final static String PATH_SUB = "/{sub}";
    public final static String PATH_SUB_ATTRIBUTE = "/{sub}/{attribute}";

    // Path Variables
    public final static String SUB = "sub";
    public final static String ATTRIBUTE = "attribute";

    // Registry Names
    private final static String[] REGISTRY_NAMES_ARRAY = { MetricRegistry.Type.BASE.getName(), MetricRegistry.Type.VENDOR.getName(), MetricRegistry.Type.APPLICATION.getName() };
    public final static List<String> REGISTRY_NAMES_LIST = Arrays.asList(REGISTRY_NAMES_ARRAY);

    // Dropwizard Histogram, Meter, or Timer Constants
    public final static String COUNT = "count";
    public final static String MEAN_RATE = "meanRate";
    public final static String ONE_MINUTE_RATE = "oneMinRate";
    public final static String FIVE_MINUTE_RATE = "fiveMinRate";
    public final static String FIFTEEN_MINUTE_RATE = "fifteenMinRate";
    public final static String MAX = "max";
    public final static String MEAN = "mean";
    public final static String MIN = "min";
    public final static String STD_DEV = "stddev";
    public final static String MEDIAN = "p50";
    public final static String PERCENTILE_75TH = "p75";
    public final static String PERCENTILE_95TH = "p95";
    public final static String PERCENTILE_98TH = "p98";
    public final static String PERCENTILE_99TH = "p99";
    public final static String PERCENTILE_999TH = "p999";

    //Content Types
    public final static String TEXTCONTENTTYPE = "text/plain; charset=utf-8";
    public final static String JSONCONTENTTYPE = "application/json; charset=utf-8";

    //Appended Units for prometheus

    public final static String APPENDEDSECONDS = "_seconds";
    public final static String APPENDEDBYTES = "_bytes";
    public final static String APPENDEDPERCENT = "_percent";

    //Conversion factors
    public final static double NANOSECONDCONVERSION = 0.000000001;
    public final static double MICROSECONDCONVERSION = 0.000001;
    public final static double MILLISECONDCONVERSION = 0.001;
    public final static double SECONDCONVERSION = 1;
    public final static double MINUTECONVERSION = 60;
    public final static double HOURCONVERSION = 3600;
    public final static double DAYCONVERSION = 86400;
    public final static double BYTECONVERSION = 1;
    public final static double KILOBYTECONVERSION = 1024;
    public final static double MEGABYTECONVERSION = 1048576;
    public final static double GIGABYTECONVERSION = 1073741824;
    public final static double BITCONVERSION = 0.125;
    public final static double KILOBITCONVERSION = 125;
    public final static double MEGABITCONVERSION = 125000;
    public final static double GIGABITCONVERSION = 1.25e+8;
    public final static double KIBIBITCONVERSION = 128;
    public final static double MEBIBITCONVERSION = 131072;
    public final static double GIBIBITCONVERSION = 1.342e+8;
}
