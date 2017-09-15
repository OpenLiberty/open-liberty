/*
 **********************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *               2017 Red Hat, Inc. and/or its affiliates
 *               and other contributors as indicated by the @author tags.
 *
 * See the NOTICES file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 **********************************************************************/
package org.eclipse.microprofile.metrics;

import java.util.EnumSet;

/**
 * An enumeration representing the different types of metrics.
 * 
 * @author hrupp, Raymond Lam, Ouyang Zhou
 */
public enum MetricType {
    /**
     * A Counter monotonically in-/decreases its values.
     * An example could be the number of Transactions committed.
     */
    COUNTER("counter", Counter.class),

    /**
     * A Gauge has values that 'arbitrarily' goes up/down at each
     * sampling. An example could be CPU load
     */
    GAUGE("gauge", Gauge.class),

    /**
     * A Meter measures the rate at which a set of events occur.
     * An example could be amount of Transactions per Hour.
     */
    METERED("meter", Meter.class),

    /**
     * A Histogram calculates the distribution of a value.
     */
    HISTOGRAM("histogram", Histogram.class),

    /**
     * A timer aggregates timing durations and provides duration 
     * statistics, plus throughput statistics
     */
    TIMER("timer", Timer.class),
    
    /**
     * Invalid - Placeholder
     */
    INVALID("invalid", null)
    ;


  private String type;
  private Class<?> classtype;

  MetricType(String type, Class<?> classtype) {
    this.type = type;
    this.classtype = classtype;
  }

  public String toString() {
    return type;
  }

  /**
   * Convert the string representation into an enum
   * @param in the String representation
   * @return the matching Enum
   * @throws IllegalArgumentException if in is not a valid enum value
   */
  public static MetricType from(String in) {
    EnumSet<MetricType> enumSet = EnumSet.allOf(MetricType.class);
    for (MetricType u : enumSet) {
      if (u.type.equals(in)) {
        return u;
      }
    }
    throw new IllegalArgumentException(in + " is not a valid MetricType");
  }

  /**
   * Convert the metric class type into an enum
   * @param in The metric class type
   * @return the matching Enum
   * @throws IllegalArgumentException if in is not a valid enum value
   */
  public static MetricType from(Class<?> in) {
    EnumSet<MetricType> enumSet = EnumSet.allOf(MetricType.class);
    for (MetricType u : enumSet) {
      if (u.classtype != null && u.classtype.equals(in)) {
        return u;
      }
    }
    return MetricType.INVALID;
  }
}