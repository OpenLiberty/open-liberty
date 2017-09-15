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

import java.util.Random;

/**
 * Proxy for creating thread local {@link Random} instances depending on the runtime.
 * By default it tries to use the JDK's implementation and fallbacks to the internal
 * one if the JDK doesn't provide any.
 */
class ThreadLocalRandomProxy {

    private interface Provider {
        Random current();
    }

    /**
     * To avoid NoClassDefFoundError during loading {@link ThreadLocalRandomProxy}
     */
    private static class JdkProvider implements Provider {

        @Override
        public Random current() {
            return java.util.concurrent.ThreadLocalRandom.current();
        }
    }

    private static class InternalProvider implements Provider {

        @Override
        public Random current() {
            return ThreadLocalRandom.current();
        }
    }

    private static final Provider INSTANCE = getThreadLocalProvider();

    private static Provider getThreadLocalProvider() {
        try {
            final JdkProvider jdkProvider = new JdkProvider();
            jdkProvider.current(); //  To make sure that ThreadLocalRandom actually exists in the JDK
            return jdkProvider;
        } catch (Throwable e) {
            return new InternalProvider();
        }
    }

    public static Random current() {
        return INSTANCE.current();
    }

}
