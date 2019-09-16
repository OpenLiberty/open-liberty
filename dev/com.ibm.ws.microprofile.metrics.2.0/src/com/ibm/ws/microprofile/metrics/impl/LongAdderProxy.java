/*******************************************************************************
* Copyright (c) 2017 IBM Corporation and others.
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
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
package com.ibm.ws.microprofile.metrics.impl;

/**
 * Proxy for creating long adders depending on the runtime. By default it tries to
 * the JDK's implementation and fallbacks to the internal one if the JDK doesn't provide
 * any. The JDK's LongAdder and the internal one don't have a common interface, therefore
 * we adapten them to {@link InternalLongAdderProvider}, which serves as a common interface for
 * long adders.
 */
public class LongAdderProxy {

    private interface Provider {
        LongAdderAdapter get();
    }

    /**
     * To avoid NoClassDefFoundError during loading {@link LongAdderProxy}
     */
    private static class JdkProvider implements Provider {

        @Override
        public LongAdderAdapter get() {
            return new LongAdderAdapter() {
                private final java.util.concurrent.atomic.LongAdder longAdder = new java.util.concurrent.atomic.LongAdder();

                @Override
                public void add(long x) {
                    longAdder.add(x);
                }

                @Override
                public long sum() {
                    return longAdder.sum();
                }

                @Override
                public void increment() {
                    longAdder.increment();
                }

                @Override
                public void decrement() {
                    longAdder.decrement();
                }

                @Override
                public long sumThenReset() {
                    return longAdder.sumThenReset();
                }
            };
        }
    }

    /**
     * Backed by the internal LongAdder
     */
    private static class InternalLongAdderProvider implements Provider {

        @Override
        public LongAdderAdapter get() {
            return new LongAdderAdapter() {
                private final LongAdder longAdder = new LongAdder();

                @Override
                public void add(long x) {
                    longAdder.add(x);
                }

                @Override
                public long sum() {
                    return longAdder.sum();
                }

                @Override
                public void increment() {
                    longAdder.increment();
                }

                @Override
                public void decrement() {
                    longAdder.decrement();
                }

                @Override
                public long sumThenReset() {
                    return longAdder.sumThenReset();
                }
            };
        }

    }

    private static final Provider INSTANCE = getLongAdderProvider();

    private static Provider getLongAdderProvider() {
        try {
            final JdkProvider jdkProvider = new JdkProvider();
            jdkProvider.get(); // To trigger a possible `NoClassDefFoundError` exception
            return jdkProvider;
        } catch (Throwable e) {
            return new InternalLongAdderProvider();
        }
    }

    public static LongAdderAdapter create() {
        return INSTANCE.get();
    }
}
