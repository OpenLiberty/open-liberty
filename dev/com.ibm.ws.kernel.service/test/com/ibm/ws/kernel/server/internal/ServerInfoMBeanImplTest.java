/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.server.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.util.Map;

import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.kernel.server.ServerInfoMBean;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

/**
 * Extremely simple unit tests to make sure we call the right SPIs and
 * interpret the hostname the right way.
 */
@RunWith(JMockit.class)
public class ServerInfoMBeanImplTest {
    private final Mockery mock = new JUnit4Mockery();
    private final WsLocationAdmin mockLocAdmin = mock.mock(WsLocationAdmin.class);
    private final VariableRegistry mockVarReg = mock.mock(VariableRegistry.class);
    private ServerInfoMBean mbean;

    @Before
    public void setup() throws Exception {
        ServerInfoMBeanImpl mbean = new ServerInfoMBeanImpl();
        mbean.setVariableRegistry(mockVarReg);
        mbean.setWsLocationAdmin(mockLocAdmin);
        this.mbean = mbean;
    }

    @After
    public void tearDown() {
        ServerInfoMBeanImpl mbean = (ServerInfoMBeanImpl) this.mbean;
        mbean.unsetVariableRegistry(mockVarReg);
        mbean.unsetWsLocationAdmin(mockLocAdmin);
        mbean = null;
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.server.internal.ServerInfoMBeanImpl#getDefaultHostname()}.
     */
    @Test
    public void getHostname() {
        final String hostname = "abc.ibm.com";
        mock.checking(new Expectations() {
            {
                one(mockVarReg).resolveString("${defaultHostName}");
                will(returnValue(hostname));
            }
        });
        assertEquals("The default hostname should always be returned in lower case and complete",
                     "abc.ibm.com", mbean.getDefaultHostname());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.server.internal.ServerInfoMBeanImpl#getDefaultHostname()}.
     */
    @Test
    public void getHostname_normalizedToLowercase() {
        final String hostname = "myHost";
        mock.checking(new Expectations() {
            {
                one(mockVarReg).resolveString("${defaultHostName}");
                will(returnValue(hostname));
            }
        });
        assertEquals("The default hostname should always be returned in lower case",
                     "myhost", mbean.getDefaultHostname());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.server.internal.ServerInfoMBeanImpl#getDefaultHostname()}.
     */
    @Test
    public void getHostname_notDefined() {
        mock.checking(new Expectations() {
            {
                one(mockVarReg).resolveString("${defaultHostName}");
                will(returnValue("${defaultHostName}"));
            }
        });
        assertEquals("If the defaultHostName variable is not defined, localhost should be returned",
                     "localhost", mbean.getDefaultHostname());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.server.internal.ServerInfoMBeanImpl#getDefaultHostname()}.
     */
    @Test
    public void getHostname_emptyValue() {
        mock.checking(new Expectations() {
            {
                one(mockVarReg).resolveString("${defaultHostName}");
                will(returnValue(" "));
            }
        });
        assertEquals("If the defaultHostName variable is empty, localhost should be returned",
                     "localhost", mbean.getDefaultHostname());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.server.internal.ServerInfoMBeanImpl#getDefaultHostname()}.
     */
    @Test
    public void getHostname_splatValue() {
        mock.checking(new Expectations() {
            {
                one(mockVarReg).resolveString("${defaultHostName}");
                will(returnValue("*"));
            }
        });
        assertNull("If the defaultHostName variable is defined to be '*', null should be returned to preserve pre-existing behaviour",
                   mbean.getDefaultHostname());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.server.internal.ServerInfoMBeanImpl#getUserDirectory()}.
     */
    @Test
    public void getUserDirectory() {
        final String userDir = "/wlp/usr";
        mock.checking(new Expectations() {
            {
                one(mockLocAdmin).resolveString(WsLocationConstants.SYMBOL_USER_DIR);
                will(returnValue(userDir));
            }
        });
        assertSame(userDir, mbean.getUserDirectory());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.server.internal.ServerInfoMBeanImpl#getName()}.
     */
    @Test
    public void getName() {
        final String serverName = "myServer";
        mock.checking(new Expectations() {
            {
                one(mockLocAdmin).getServerName();
                will(returnValue(serverName));
            }
        });
        assertSame(serverName, mbean.getName());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.server.internal.ServerInfoMBeanImpl#getJavaRuntimeVersion() and getJavaSpecVersion()}.
     */
    @Test
    public void getJavaVersion() {
        final String javaRuntimeVersion = System.getProperty("java.runtime.version");
        assertSame(javaRuntimeVersion, mbean.getJavaRuntimeVersion());
        final String javaSpecVersion = System.getProperty("java.specification.version");
        assertSame(javaSpecVersion, mbean.getJavaSpecVersion());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.server.internal.ServerInfoMBeanImpl#getLibertyVersion()}.
     * Uses jmockit for static method mocking
     * Uses fake product info properties file in test-files/lib/versions dir for mocked ProductInfo class
     */
    @Test
    public void getLibertyVersion() {
        new MockUp<ProductInfo>() {

            @Mock
            public Map<String, ProductInfo> getAllProductInfo() {
                Map<String, ProductInfo> testPIMap = null;
                try {
                    String testClassesDir = System.getProperty("test.classesDir", "bin_test");
                    testPIMap = ProductInfo.getAllProductInfo(new File(testClassesDir + "/test-files"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return testPIMap;
            }
        };

        final String libertyVersion = mbean.getLibertyVersion();
        assertEquals("9.9.9.9", libertyVersion); // using version 9.9.9.9 to clarify that this does not need to be updated.

    }

}
