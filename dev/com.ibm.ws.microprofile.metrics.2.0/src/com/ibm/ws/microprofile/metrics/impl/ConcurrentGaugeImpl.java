/*******************************************************************************
* Copyright (c) 2019 IBM Corporation and others.
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
********************************************************************************
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * *******************************************************************************
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
 */
package com.ibm.ws.microprofile.metrics.impl;

import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.metrics.ConcurrentGauge;

/**
 * @author Jan Martiska
 */
public class ConcurrentGaugeImpl implements ConcurrentGauge {

    // current count of concurrent invocations
    private final AtomicLong count;

    // maximum count achieved in previous minute
    private final AtomicLong max_previousMinute;
    // minimum count achieved in previous minute
    private final AtomicLong min_previousMinute;

    // maximum count achieved in this minute
    private final AtomicLong max_thisMinute;
    // minimum count achieved in this minute
    private final AtomicLong min_thisMinute;

    // current timestamp rounded down to the last whole minute
    private final AtomicLong thisMinute;

    public ConcurrentGaugeImpl() {
        count = new AtomicLong(0);
        max_previousMinute = new AtomicLong(0);
        min_previousMinute = new AtomicLong(0);
        max_thisMinute = new AtomicLong(0);
        min_thisMinute = new AtomicLong(0);
        thisMinute = new AtomicLong(getCurrentMinuteFromSystem());
    }

    @Override
    public void inc() {
        maybeStartNewMinute();
        synchronized (this) {
            long newCount = count.incrementAndGet();
            if (newCount > max_thisMinute.get()) {
                max_thisMinute.set(newCount);
            }
        }
    }

    @Override
    public void dec() {
        maybeStartNewMinute();
        synchronized (this) {
            long newCount = count.decrementAndGet();
            if (newCount < min_thisMinute.get()) {
                min_thisMinute.set(newCount);
            }
        }
    }

    @Override
    public long getCount() {
        maybeStartNewMinute();
        return count.get();
    }

    @Override
    public long getMax() {
        maybeStartNewMinute();
        return max_previousMinute.get();
    }

    @Override
    public long getMin() {
        maybeStartNewMinute();
        return min_previousMinute.get();
    }

    /*
     * If a new minute has started, move the data for 'this' minute to 'previous' minute and start
     * collecting new data for the 'this' minute
     */
    private void maybeStartNewMinute() {
        long newMinute = getCurrentMinuteFromSystem();
        if (newMinute > thisMinute.get()) {
            synchronized (this) {
                if (newMinute > thisMinute.get()) {
                    thisMinute.set(newMinute);
                    max_previousMinute.set(max_thisMinute.get());
                    min_previousMinute.set(min_thisMinute.get());
                    max_thisMinute.set(count.get());
                    min_thisMinute.set(count.get());
                }
            }
        }
    }

    // Get the current system time in minutes, truncating. This number will increase by 1 every complete minute.
    private long getCurrentMinuteFromSystem() {
        return System.currentTimeMillis() / 60000;
    }
}