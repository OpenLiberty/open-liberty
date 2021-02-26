/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.krb5.Krb5Common;
import com.ibm.ws.security.spnego.SpnegoConfig;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

import test.common.SharedOutputManager;

public class Krb5UtilTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final SpnegoConfig spnegoConfig = mock.mock(SpnegoConfig.class);

    private final WsLocationAdmin locationAdmin = mock.mock(WsLocationAdmin.class);
    private final WsResource wsResource = mock.mock(WsResource.class);
    private static final String KRB5_CONF = "/myKrb5.conf";
    private static final String KRB5_KEYTAB = "/myKrb5.keytab";

    private final HttpServletResponse resp = mock.mock(HttpServletResponse.class);

    @Before
    public void setUp() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(locationAdmin).resolveResource(KRB5_CONF);
                will(returnValue(wsResource));
                allowing(spnegoConfig).getKrb5Config();
                will(returnValue(KRB5_CONF));

                allowing(locationAdmin).resolveResource(KRB5_KEYTAB);
                will(returnValue(wsResource));
                allowing(spnegoConfig).getKrb5Keytab();
                will(returnValue(KRB5_KEYTAB));
            }
        });
    }

    @Test
    public void testSetServerResponseToken() throws Exception {
        final String methodName = "testSetServerResponseToken";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(resp).setHeader("WWW-Authenticate", "Negotiate cmVzcG9uc2VUb2tlbg==");
                    allowing(resp).getHeader("WWW-Authenticate");
                }
            });
            Krb5Util krb5Util = new Krb5Util();
            String token = "responseToken";
            byte[] respToken = token.getBytes();
            krb5Util.setServerResponseToken(resp, respToken);
            assertNotNull("response should not be null", resp.getHeader("WWW-Authenticate"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testTrimUserName() {
        final String methodName = "testTrimUserName";
        try {
            Krb5Util krb5Util = new Krb5Util();
            assertEquals("User name should be utle", "utle", krb5Util.trimUsername("utle@KerberosRealm"));
            assertEquals("User name should be utle", "utle", krb5Util.trimUsername("utle"));
            assertNull("User name should be null", krb5Util.trimUsername(null));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testSetKrb5ConfigAndKeytabProps() throws Exception {
        final String methodName = "testSetKrb5ConfigAndKeytabProps";
        try {
            Krb5Util krb5Util = new Krb5Util();
            krb5Util.setKrb5ConfigAndKeytabProps(spnegoConfig);
            assertEquals("Keberos configuration should be " + KRB5_CONF, KRB5_CONF, Krb5Common.getSystemProperty(Krb5Common.KRB5_CONF));
            assertEquals("Keberos keytab should be " + KRB5_KEYTAB, KRB5_KEYTAB, Krb5Common.getSystemProperty(Krb5Common.KRB5_KTNAME));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testSetSystemProperty() throws Exception {
        final String methodName = "testSetSystemProperty";
        try {
            Krb5Common.setSystemProperty("propA", "valueA");
            assertEquals("property value should be ", "valueA", Krb5Common.getSystemProperty("propA"));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
