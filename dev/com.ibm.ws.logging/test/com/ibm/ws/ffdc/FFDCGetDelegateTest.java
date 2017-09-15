/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ffdc;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import test.TestConstants;
import test.common.SharedOutputManager;

import com.ibm.websphere.ras.SharedTr;
import com.ibm.ws.logging.internal.DisabledFFDCService;
import com.ibm.ws.logging.internal.impl.BaseFFDCService;
import com.ibm.wsspi.logprovider.FFDCFilterService;
import com.ibm.wsspi.logprovider.LogProviderConfig;

/**
 * Test/Verify basic delegate functionality
 */
public class FFDCGetDelegateTest {
    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance().logTo(TestConstants.BUILD_TMP);

    @Before
    public void setUp() throws Exception {
        SharedFFDCConfigurator.clearDelegates();
    }

    @After
    public void tearDown() throws Exception {
        SharedFFDCConfigurator.clearDelegates();
    }

    /**
     * DisabledFFDCFilter: Disabled delegate is used when FFDC is not
     * configured
     * 
     * @throws ConfigurationException
     */
    @Test
    public void testUseDisabledFFDCFilter() throws ConfigurationException {
        final String m = "testUseDisabledFFDCFilter";
        try {
            FFDCFilterService ffdc = FFDCConfigurator.getDelegate();
            assertThat(ffdc, instanceOf(DisabledFFDCService.class));

            ffdc.processException(null, null, null);
            ffdc.processException(null, null, null, (Object) null);
            ffdc.processException(null, null, null, (Object[]) null);
            ffdc.processException(null, null, null, null, null);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testUseDefaultFFDCFilter() throws ConfigurationException {
        final String m = "testUseDefaultFFDCFilter";
        try {
            LogProviderConfig config = SharedTr.getDefaultConfig();
            FFDCConfigurator.init(config);

            FFDCFilterService ffdc = FFDCConfigurator.getDelegate();
            assertThat("Basic FFDC service created by default", ffdc, instanceOf(BaseFFDCService.class));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}
