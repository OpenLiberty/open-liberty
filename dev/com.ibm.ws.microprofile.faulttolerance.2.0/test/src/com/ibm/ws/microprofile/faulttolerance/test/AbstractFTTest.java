/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.test;

import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.ibm.ws.microprofile.faulttolerance.impl.FTConstants;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProviderResolver;
import com.ibm.ws.microprofile.faulttolerance.test.util.MockContextService;
import com.ibm.ws.microprofile.faulttolerance20.impl.ProviderResolverImpl20;

/**
 *
 */
public abstract class AbstractFTTest {

    // This is a static because it can only be set once because FaultToleranceProvider will cache the first FaultToleranceProviderResolver that it uses.
    private static ProviderResolverImpl20 providerResolver = new ProviderResolverImpl20();

    private MockContextService contextService;

    @BeforeClass
    public static void initializeResolver() {
        FaultToleranceProviderResolver.setInstance(providerResolver);
    }

    @Before
    public void before() {
        System.setProperty(FTConstants.JSE_FLAG, "true");
        contextService = new MockContextService();
        setField(ProviderResolverImpl20.class, "contextService", providerResolver, contextService);
    }

    @After
    public void after() {
        contextService = null;
        setField(ProviderResolverImpl20.class, "contextService", providerResolver, null);
        System.setProperty(FTConstants.JSE_FLAG, "false");
    }

    private <T> void setField(Class<T> clazz, String fieldName, T instance, Object value) {
        Field field;
        try {
            field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException("Unable to set field", e);
        }
    }

    protected MockContextService getMockContextService() {
        return contextService;
    }

}
