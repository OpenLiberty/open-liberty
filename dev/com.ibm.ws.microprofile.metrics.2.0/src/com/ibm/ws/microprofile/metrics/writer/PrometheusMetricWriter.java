/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.writer;

import java.io.IOException;
import java.io.Writer;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.metrics.Constants;
import com.ibm.ws.microprofile.metrics.exceptions.EmptyRegistryException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchMetricException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchRegistryException;
import com.ibm.ws.microprofile.metrics.helper.PrometheusBuilder;
import com.ibm.ws.microprofile.metrics.helper.Util;

/**
 *
 */
public class PrometheusMetricWriter implements OutputWriter {

    private static final TraceComponent tc = Tr.register(PrometheusMetricWriter.class);

    private final Writer writer;
    protected final Locale locale;

    protected final Set<Metadata> improperMetadataSet = new HashSet<Metadata>();

    public PrometheusMetricWriter(Writer writer, Locale locale) {
        this.writer = writer;
        this.locale = locale;
    }

    /**
     * {@inheritDoc}
     *
     * @throws EmptyRegistryException
     */
    @Override
    public void write(String registryName, String metricName) throws NoSuchMetricException, NoSuchRegistryException, IOException, EmptyRegistryException {
        StringBuilder builder = new StringBuilder();
        writeMetricsAsPrometheus(builder, registryName, metricName);
        serialize(builder);
    }

    /** {@inheritDoc} */
    @Override
    public void write(String registryName) throws NoSuchRegistryException, EmptyRegistryException, IOException {
        StringBuilder builder = new StringBuilder();
        writeMetricsAsPrometheus(builder, registryName);
        serialize(builder);
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore({ EmptyRegistryException.class, NoSuchRegistryException.class })
    public void write() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String registryName : Constants.REGISTRY_NAMES_LIST) {
            try {
                writeMetricsAsPrometheus(builder, registryName);
            } catch (NoSuchRegistryException e) { // Ignore
            } catch (EmptyRegistryException e) { // Ignore
            }
        }
        serialize(builder);
    }

    protected void writeMetricsAsPrometheus(StringBuilder builder, String registryName) throws NoSuchRegistryException, EmptyRegistryException {
        writeMetricMapAsPrometheus(builder, registryName, Util.getMetricsAsMap(registryName), Util.getMetricsMetadataAsMap(registryName));
    }

    protected void writeMetricsAsPrometheus(StringBuilder builder, String registryName,
                                            String metricName) throws NoSuchRegistryException, NoSuchMetricException, EmptyRegistryException {
        writeMetricMapAsPrometheus(builder, registryName, Util.getMetricsAsMap(registryName, metricName), Util.getMetricsMetadataAsMap(registryName));
    }

    protected void writeMetricMapAsPrometheus(StringBuilder builder, String registryName, Map<MetricID, Metric> metricMap, Map<String, Metadata> metricMetadataMap) {
        for (Entry<String, Metadata> metadataEntry : metricMetadataMap.entrySet()) {

            String metricName = metadataEntry.getKey();
            String metricNamePrometheus = registryName + "_" + metricName;
            Metadata metricMetadata = metadataEntry.getValue();

            Map<MetricID, Metric> currentMetricMap = new HashMap<MetricID, Metric>();

            for (Entry<MetricID, Metric> metricEntry : metricMap.entrySet()) {
                if (metricEntry.getKey().getName().equals(metricName)) {
                    currentMetricMap.put(metricEntry.getKey(), metricEntry.getValue());
                }
            }

            String description = (!metricMetadata.getDescription().isPresent()
                                  || metricMetadata.getDescription().get().trim().isEmpty()) ? "" : Tr.formatMessage(tc, locale, metricMetadata.getDescription().get());

            //Get Unit
            String unit = metricMetadata.getUnit().get();

            //Unit determination / translation
            Map.Entry<String, Double> conversionAppendEntry = resolveConversionFactorXappendUnitEntry(unit);
            double conversionFactor = conversionAppendEntry.getValue();
            String appendUnit = conversionAppendEntry.getKey();

            if (metricMetadata.getTypeRaw().equals(MetricType.COUNTER)) {
                PrometheusBuilder.buildCounter(builder, metricNamePrometheus, description, currentMetricMap);
            } else if (metricMetadata.getTypeRaw().equals(MetricType.CONCURRENT_GAUGE)) {
                PrometheusBuilder.buildConcurrentGauge(builder, metricNamePrometheus, description, currentMetricMap);
            } else if (metricMetadata.getTypeRaw().equals(MetricType.GAUGE)) {
                PrometheusBuilder.buildGauge(builder, metricNamePrometheus, description, currentMetricMap, conversionFactor, appendUnit);
            } else if (metricMetadata.getTypeRaw().equals(MetricType.TIMER)) {
                PrometheusBuilder.buildTimer(builder, metricNamePrometheus, description, currentMetricMap);
            } else if (metricMetadata.getTypeRaw().equals(MetricType.HISTOGRAM)) {
                PrometheusBuilder.buildHistogram(builder, metricNamePrometheus, description, currentMetricMap, conversionFactor, appendUnit);
            } else if (metricMetadata.getTypeRaw().equals(MetricType.METERED)) {
                PrometheusBuilder.buildMeter(builder, metricNamePrometheus, description, currentMetricMap);
            } else if (!improperMetadataSet.contains(metricMetadata)) {
                Tr.event(tc, "Metadata " + metricMetadata.toString() + " does not have an appropriate Metric Type");
                improperMetadataSet.add(metricMetadata);
            }

        }
    }

    /**
     * Calculates the unit String suffix and conversion factor used for later calculations
     *
     * @param unit String that encompasses the unit needed to calculate appropriate conversion factor and value to append
     * @return Map.Entry<String, Double> that contains the unit string suffix and conversion factor
     */
    protected Map.Entry<String, Double> resolveConversionFactorXappendUnitEntry(String unit) {

        if (unit == null || unit.trim().isEmpty() || unit.equals(MetricUnits.NONE)) {
            return new AbstractMap.SimpleEntry<String, Double>(null, Double.NaN);

        } else if (unit.equals(MetricUnits.NANOSECONDS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDSECONDS, Constants.NANOSECONDCONVERSION);

        } else if (unit.equals(MetricUnits.MICROSECONDS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDSECONDS, Constants.MICROSECONDCONVERSION);
        } else if (unit.equals(MetricUnits.SECONDS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDSECONDS, Constants.SECONDCONVERSION);

        } else if (unit.equals(MetricUnits.MINUTES)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDSECONDS, Constants.MINUTECONVERSION);

        } else if (unit.equals(MetricUnits.HOURS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDSECONDS, Constants.HOURCONVERSION);

        } else if (unit.equals(MetricUnits.DAYS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDSECONDS, Constants.DAYCONVERSION);

        } else if (unit.equals(MetricUnits.PERCENT)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDPERCENT, Double.NaN);

        } else if (unit.equals(MetricUnits.BYTES)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.BYTECONVERSION);

        } else if (unit.equals(MetricUnits.KILOBYTES)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.KILOBYTECONVERSION);

        } else if (unit.equals(MetricUnits.MEGABYTES)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.MEGABYTECONVERSION);

        } else if (unit.equals(MetricUnits.GIGABYTES)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.GIGABYTECONVERSION);

        } else if (unit.equals(MetricUnits.KILOBITS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.KILOBITCONVERSION);

        } else if (unit.equals(MetricUnits.MEGABITS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.MEGABITCONVERSION);
        } else if (unit.equals(MetricUnits.GIGABITS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.GIGABITCONVERSION);

        } else if (unit.equals(MetricUnits.KIBIBITS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.KIBIBITCONVERSION);
        } else if (unit.equals(MetricUnits.MEBIBITS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.MEBIBITCONVERSION);
        } else if (unit.equals(MetricUnits.GIBIBITS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.GIBIBITCONVERSION);
        } else if (unit.equals(MetricUnits.MILLISECONDS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDSECONDS, Constants.MILLISECONDCONVERSION);
        } else {
            return new AbstractMap.SimpleEntry<String, Double>("_" + unit, Double.NaN);
        }
    }

    protected void serialize(StringBuilder builder) throws IOException {
        try {
            writer.write(builder.toString());
        } finally {
            writer.flush();
        }
    }
}
