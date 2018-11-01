/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.test;

import org.junit.After;
import org.junit.Before;

import com.ibm.ws.microprofile.faulttolerance.impl.FTConstants;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProviderResolver;
import com.ibm.ws.microprofile.faulttolerance20.impl.ProviderResolverImpl20;

/**
 *
 */
public abstract class AbstractFTTest {

    @Before
    public void before() {
        System.setProperty(FTConstants.JSE_FLAG, "true");
        FaultToleranceProviderResolver.setInstance(new ProviderResolverImpl20());
    }

    @After
    public void after() {
        FaultToleranceProviderResolver.setInstance(null);
        System.setProperty(FTConstants.JSE_FLAG, "false");
    }

}
