/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpGraphQL10.metrics;

import javax.json.bind.annotation.JsonbProperty;

public class Stats {

    public static class Time {
        private int count;
        private double mean;
        private double max;
        private double min;

        public int getCount() {
            return count;
        }
        public void setCount(int count) {
            this.count = count;
        }
        public double getMean() {
            return mean;
        }
        public void setMean(double mean) {
            this.mean = mean;
        }
        public double getMax() {
            return max;
        }
        public void setMax(double max) {
            this.max = max;
        }
        public double getMin() {
            return min;
        }
        public void setMin(double min) {
            this.min = min;
        }
    }

    @JsonbProperty("mp_graphql_mpGraphQL10.metrics.MyGraphQLEndpoint.getCountWidget.count")
    private int count;
    @JsonbProperty("mp_graphql_mpGraphQL10.metrics.MyGraphQLEndpoint.getTimeWidget.time")
    private Time time;

    public int getCount() {
        return count;
    }
    public void setCount(int count) {
        this.count = count;
    }
    public Time getTime() {
        return time;
    }
    public void setTime(Time time) {
        this.time = time;
    }
}
