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

/**
 * Standard units constants for metric's {@link Metadata}.
 * This class provides a list of common metric units and is not meant to be a complete list of possible units.
 *
 * @see Metadata
 *
 * @author hrupp
 */
public final class MetricUnits {
  /** No unit */
  public static final String NONE = "none";

  /** Represents bits. Not defined by SI, but by IEC 60027 */
  public static final String BITS = "bits";
  /** 1000 {@link #BITS} */
  public static final String KILOBITS = "kilobits";
  /** 1000 {@link #KIBIBITS} */
  public static final String MEGABITS = "megabits";
  /** 1000 {@link #MEGABITS} */
  public static final String GIGABITS = "gigabits";
  /** 1024 {@link #BITS} */
  public static final String KIBIBITS = "kibibits";
  /** 1024 {@link #KIBIBITS}  */
  public static final String MEBIBITS = "mebibits";
  /** 1024 {@link #MEBIBITS} */
  public static final String GIBIBITS = "gibibits";

  /** 8 {@link #BITS} */
  public static final String BYTES = "bytes";
  /** 1000 {@link #BYTES} */
  public static final String KILOBYTES = "kilobytes";
  /** 1000 {@link #KILOBYTES} */
  public static final String MEGABYTES = "megabytes";
  /** 1000 {@link #MEGABYTES} */
  public static final String GIGABYTES = "gigabytes";

  /** 1/1000 {@link #MICROSECONDS} */
  public static final String NANOSECONDS = "nanoseconds";
  /** 1/1000 {@link #MILLISECONDS} */
  public static final String MICROSECONDS = "microseconds";
  /** 1/1000 {@link #SECONDS} */
  public static final String MILLISECONDS = "milliseconds";
  /** Represents seconds */
  public static final String SECONDS = "seconds";
  /** 60 {@link #SECONDS} */
  public static final String MINUTES = "minutes";
  /** 60 {@link #MINUTES} */
  public static final String HOURS = "hours";
  /** 24 {@link #HOURS} */
  public static final String DAYS = "days";

  /** Represents percentage */
  public static final String PERCENT = "percent";
  
  /** Represent per second  */
  public static final String PER_SECOND = "per_second";
  
  
  private MetricUnits() {}
}
