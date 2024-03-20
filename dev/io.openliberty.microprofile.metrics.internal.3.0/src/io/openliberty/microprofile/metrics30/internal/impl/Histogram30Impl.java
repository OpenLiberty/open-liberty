/*******************************************************************************
* Copyright (c) 2017 IBM Corporation and others.
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
*
*******************************************************************************
* Copyright 2010-2013 Coda Hale and Yammer, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
package io.openliberty.microprofile.metrics30.internal.impl;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Snapshot;

import com.ibm.ws.microprofile.metrics.impl.LongAdderAdapter;
import com.ibm.ws.microprofile.metrics.impl.LongAdderProxy;
import com.ibm.ws.microprofile.metrics.impl.Reservoir;

import io.openliberty.microprofile.metrics30.internal.helper.BucketManager;
import io.openliberty.microprofile.metrics30.internal.helper.BucketManager.BucketValue;
import io.openliberty.microprofile.metrics30.setup.config.MetricPercentileConfiguration;
import io.openliberty.microprofile.metrics30.setup.config.MetricsConfigurationManager;

/**
 * A metric which calculates the distribution of a value.
 *
 * @see <a href="http://www.johndcook.com/standard_deviation.html">Accurately computing running
 *      variance</a>
 */
public class Histogram30Impl implements Histogram {
    private final Reservoir reservoir;
    private final LongAdderAdapter count;
    private final LongAdderAdapter sum;
    private final BucketManager manager;
    private final double[] percentiles;

    /**
     * Creates a new {@link Histogram30Impl} with the given reservoir.
     *
     * @param reservoir the reservoir to create a histogram from
     * @param metadata
     */
    public Histogram30Impl(Reservoir reservoir, Metadata metadata) {

        //System.out.println("Current Metrics: " + metricName.getDisplayName() + " -- " + metricName.getName() + " -- " + metricID.getTagsAsArray() + " -- " + metricID.getName());

        this.reservoir = reservoir;
        this.count = LongAdderProxy.create();
        this.sum = LongAdderProxy.create();
        this.manager = new BucketManager(metadata); //read config here for buckets and perce
        this.percentiles = setConfiguredPercentiles(metadata);
    }

    /**
     * Adds a recorded value.
     *
     * @param value the length of the value
     */
    @Override
    public void update(int value) {
        update((long) value);
    }

    /**
     * Adds a recorded value.
     *
     * @param value the length of the value
     */
    @Override
    public void update(long value) {
        count.increment();
        sum.add(value);
        reservoir.update(value);
        manager.update(value);
    }

    /**
     * Returns the number of values recorded.
     *
     * @return the number of values recorded
     */
    @Override
    public long getCount() {
        return count.sum();
    }

    @Override
    public Snapshot getSnapshot() {

        return reservoir.getSnapshot();
    }

    public Map<String, Map<Double, BucketValue>> getBuckets() {
        return manager.getBuckets();
    }

    public BucketManager getManager() {
        return manager;
    }

    /** {@inheritDoc} */
    @Override
    public long getSum() {
        return sum.sum();
    }

    public double[] getConfiguredPercentiles() {
        return percentiles;
    }

    public double[] setConfiguredPercentiles(Metadata metadata) {
        Optional<String> percentileConfiguration = ConfigProvider.getConfig().getOptionalValue("mp.metrics.distribution.percentiles", String.class);
        String metricName = metadata.getName();
        if (percentileConfiguration.isPresent()) {

            MetricPercentileConfiguration percentileConfig = MetricsConfigurationManager.getInstance().getPercentilesConfiguration(metricName);
            System.out.println("MetricName: " + metricName);

            if (percentileConfig != null && percentileConfig.getValues() != null
                && percentileConfig.getValues().length > 0) {
                double[] vals = Stream.of(percentileConfig.getValues()).mapToDouble(Double::doubleValue).toArray();

                for (Double value : vals) {
                    System.out.println("Histogram Percentile Value: " + value + " -- " + metadata.getUnit());

                }
                return vals;
            } else if (percentileConfig == null) {
                return null;
            } else {
                System.out.println("Returning empty double for percentiles");
                return new double[0];
            }
        }

        System.out.println("Returning null for percentiles.");
        return null;

    }
}
