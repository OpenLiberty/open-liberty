/*******************************************************************************
 * Copyright (c) 2015, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer.security.jacc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.WebJaccService;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.internal.DenyReply;
import com.ibm.ws.webcontainer.security.internal.WebReply;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.webapp.IWebAppDispatcherContext;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

import test.common.SharedOutputManager;

public class WebAppJaccAuthorizationHelperTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    static final String KEY_WEB_JACC_SERVICE = "webJaccService";

    private final Mockery context = new JUnit4Mockery();
    private final IExtendedRequest ier = context.mock(IExtendedRequest.class);
    private final IWebAppDispatcherContext wadc = context.mock(IWebAppDispatcherContext.class);
    private final RequestProcessor rp = context.mock(RequestProcessor.class);
    private final WebModuleMetaData wmmd = context.mock(WebModuleMetaData.class);
    private final WebComponentMetaData wcmd = context.mock(WebComponentMetaData.class);
    private final WebAppConfig wac = context.mock(WebAppConfig.class);
    private final WebRequest wr = context.mock(WebRequest.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<WebJaccService> jsr = context.mock(ServiceReference.class, "jaccServiceRef");
    private final WebJaccService js = context.mock(WebJaccService.class);
    private final ComponentContext cc = context.mock(ComponentContext.class);
    private final WSPrincipal wp = new WSPrincipal("securityName", "accessId", "BASIC");
    private final AtomicServiceReference<WebJaccService> ajsr = new AtomicServiceReference<WebJaccService>(KEY_WEB_JACC_SERVICE);

    /**
     * Tests isUserInRole method
     * Expected result: valid output
     */
    @Test
    public void isUserInRoleTrue() {
        final String ROLE = "Role";
        final String SERVLET_NAME = "ServletName";
        final String APP_NAME = "ApplicationName";
        final String MODULE_NAME = "ModuleName";
        final Set<Principal> principals = new HashSet<Principal>();
        final Set<?> credentials = new HashSet<String>();
        principals.add(wp);
        final Subject SUBJECT = new Subject(false, principals, credentials, credentials);

        context.checking(new Expectations() {
            {
                one(ier).getWebAppDispatcherContext();
                will(returnValue(wadc));
                one(wadc).getCurrentServletReference();
                will(returnValue(rp));
                one(rp).getName();
                will(returnValue(SERVLET_NAME));
                allowing(wcmd).getModuleMetaData();
                will(returnValue(wmmd));
                allowing(wmmd).getConfiguration();
                will(returnValue(wac));
                allowing(wac).getApplicationName();
                will(returnValue(APP_NAME));
                allowing(wac).getModuleName();
                will(returnValue(MODULE_NAME));

                allowing(jsr).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(jsr).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                one(cc).locateService("webJaccService", jsr);
                will(returnValue(js));
                one(js).isSubjectInRole(APP_NAME, MODULE_NAME, SERVLET_NAME, ROLE, ier, SUBJECT);
                will(returnValue(true));
            }
        });
        ComponentMetaDataAccessorImpl cmda = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        cmda.beginContext(wcmd);

        ajsr.setReference(jsr);
        ajsr.activate(cc);
        WebAppJaccAuthorizationHelper wjah = new WebAppJaccAuthorizationHelper(ajsr);
        assertTrue(wjah.isUserInRole(ROLE, ier, SUBJECT));
    }

    /**
     * Tests isUserInRole method. reqProc is null.
     * Expected result: no NPE
     */
    @Test
    public void isUserInRoleNoReqProc() {
        final String ROLE = "Role";
        final String APP_NAME = "ApplicationName";
        final String MODULE_NAME = "ModuleName";
        final Set<Principal> principals = new HashSet<Principal>();
        final Set<?> credentials = new HashSet<String>();
        principals.add(wp);
        final Subject SUBJECT = new Subject(false, principals, credentials, credentials);

        context.checking(new Expectations() {
            {
                one(ier).getWebAppDispatcherContext();
                will(returnValue(wadc));
                one(wadc).getCurrentServletReference();
                will(returnValue(null));
                allowing(wcmd).getModuleMetaData();
                will(returnValue(wmmd));
                allowing(wmmd).getConfiguration();
                will(returnValue(wac));
                allowing(wac).getApplicationName();
                will(returnValue(APP_NAME));
                allowing(wac).getModuleName();
                will(returnValue(MODULE_NAME));

                allowing(jsr).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(jsr).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                one(cc).locateService("webJaccService", jsr);
                will(returnValue(js));
                one(js).isSubjectInRole(APP_NAME, MODULE_NAME, null, ROLE, ier, SUBJECT);
                will(returnValue(false));
            }
        });
        ComponentMetaDataAccessorImpl cmda = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        cmda.beginContext(wcmd);

        ajsr.setReference(jsr);
        ajsr.activate(cc);
        WebAppJaccAuthorizationHelper wjah = new WebAppJaccAuthorizationHelper(ajsr);
        assertFalse(wjah.isUserInRole(ROLE, ier, SUBJECT));
    }

    /**
     * Tests authorize method.
     * Expected result: valid output
     */
    @Test
    public void authorizeTrue() {

        final String APP_NAME = "ApplicationName";
        final String MODULE_NAME = "ModuleName";
        final String URI_NAME = "/test/go.html";
        final String METHOD_NAME = "GET";
        final Set<Principal> principals = new HashSet<Principal>();
        final Set<?> credentials = new HashSet<String>();
        principals.add(wp);
        final Subject SUBJECT = new Subject(false, principals, credentials, credentials);
        AuthenticationResult AR = new AuthenticationResult(AuthResult.SUCCESS, SUBJECT);

        context.checking(new Expectations() {
            {
                one(wr).getHttpServletRequest();
                will(returnValue(ier));
                one(ier).getMethod();
                will(returnValue(METHOD_NAME));
                allowing(wcmd).getModuleMetaData();
                will(returnValue(wmmd));
                allowing(wmmd).getConfiguration();
                will(returnValue(wac));
                allowing(wac).getApplicationName();
                will(returnValue(APP_NAME));
                allowing(wac).getModuleName();
                will(returnValue(MODULE_NAME));
                allowing(jsr).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(jsr).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                one(cc).locateService("webJaccService", jsr);
                will(returnValue(js));
                one(js).isAuthorized(APP_NAME, MODULE_NAME, URI_NAME, METHOD_NAME, ier, SUBJECT);
                will(returnValue(true));
            }
        });
        ComponentMetaDataAccessorImpl cmda = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        cmda.beginContext(wcmd);

        ajsr.setReference(jsr);
        ajsr.activate(cc);
        WebAppJaccAuthorizationHelper wjah = new WebAppJaccAuthorizationHelper(ajsr);
        assertTrue(wjah.authorize(AR, wr, URI_NAME));
    }

    /**
     * Tests isSSLRequired method. HTTP connection
     * Expected result: valid output
     */
    @Test
    public void isSSLRequiredTrue() {
        final String APP_NAME = "ApplicationName";
        final String MODULE_NAME = "ModuleName";
        final String URI_NAME = "/test/go.html";
        final String METHOD_NAME = "GET";

        context.checking(new Expectations() {
            {
                one(wr).getHttpServletRequest();
                will(returnValue(ier));
                one(ier).isSecure();
                will(returnValue(false));
                one(ier).getMethod();
                will(returnValue(METHOD_NAME));
                allowing(wcmd).getModuleMetaData();
                will(returnValue(wmmd));
                allowing(wmmd).getConfiguration();
                will(returnValue(wac));
                allowing(wac).getApplicationName();
                will(returnValue(APP_NAME));
                allowing(wac).getModuleName();
                will(returnValue(MODULE_NAME));
                allowing(jsr).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(jsr).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                one(cc).locateService("webJaccService", jsr);
                will(returnValue(js));
                one(js).isSSLRequired(APP_NAME, MODULE_NAME, URI_NAME, METHOD_NAME, ier);
                will(returnValue(true));
            }
        });
        ComponentMetaDataAccessorImpl cmda = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        cmda.beginContext(wcmd);

        ajsr.setReference(jsr);
        ajsr.activate(cc);
        WebAppJaccAuthorizationHelper wjah = new WebAppJaccAuthorizationHelper(ajsr);
        assertTrue(wjah.isSSLRequired(wr, URI_NAME));
    }

    /**
     * Tests isSSLRequired method. SSL connection
     * Expected result: valid output
     */
    @Test
    public void isSSLRequiredAlreadySSL() {
        final String APP_NAME = "ApplicationName";
        final String MODULE_NAME = "ModuleName";
        final String URI_NAME = "/test/go.html";
        final String METHOD_NAME = "GET";

        context.checking(new Expectations() {
            {
                one(wr).getHttpServletRequest();
                will(returnValue(ier));
                one(ier).isSecure();
                will(returnValue(true));
                one(ier).getMethod();
                will(returnValue(METHOD_NAME));
                allowing(jsr).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(jsr).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                one(cc).locateService("webJaccService", jsr);
                will(returnValue(js));
                one(js).isSSLRequired(APP_NAME, MODULE_NAME, URI_NAME, METHOD_NAME, ier);
                will(returnValue(false));
            }
        });
        ajsr.setReference(jsr);
        ajsr.activate(cc);
        WebAppJaccAuthorizationHelper wjah = new WebAppJaccAuthorizationHelper(ajsr);
        assertFalse(wjah.isSSLRequired(wr, URI_NAME));
    }

    /**
     * Tests checkPrecludedAccess method.
     * Expected result: False
     */
    @Test
    public void checkPrecludedAccessFalse() {
        final String APP_NAME = "ApplicationName";
        final String MODULE_NAME = "ModuleName";
        final String URI_NAME = "/test/go.html";
        final String METHOD_NAME = "GET";

        context.checking(new Expectations() {
            {
                one(wr).getHttpServletRequest();
                will(returnValue(ier));
                one(ier).getMethod();
                will(returnValue(METHOD_NAME));
                allowing(wcmd).getModuleMetaData();
                will(returnValue(wmmd));
                allowing(wmmd).getConfiguration();
                will(returnValue(wac));
                allowing(wac).getApplicationName();
                will(returnValue(APP_NAME));
                allowing(wac).getModuleName();
                will(returnValue(MODULE_NAME));
                allowing(jsr).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(jsr).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                one(cc).locateService("webJaccService", jsr);
                will(returnValue(js));
                one(js).isAccessExcluded(APP_NAME, MODULE_NAME, URI_NAME, METHOD_NAME, ier);
                will(returnValue(false));
            }
        });
        ComponentMetaDataAccessorImpl cmda = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        cmda.beginContext(wcmd);

        ajsr.setReference(jsr);
        ajsr.activate(cc);
        WebAppJaccAuthorizationHelper wjah = new WebAppJaccAuthorizationHelper(ajsr);
        assertNull(wjah.checkPrecludedAccess(wr, URI_NAME));
    }

    /**
     * Tests checkPrecludedAccess method.
     * Expected result: True
     */
    @Test
    public void checkPrecludedAccessTrue() {
        final String APP_NAME = "ApplicationName";
        final String MODULE_NAME = "ModuleName";
        final String URI_NAME = "/test/go.html";
        final String METHOD_NAME = "GET";

        context.checking(new Expectations() {
            {
                one(wr).getHttpServletRequest();
                will(returnValue(ier));
                one(ier).getMethod();
                will(returnValue(METHOD_NAME));
                allowing(wcmd).getModuleMetaData();
                will(returnValue(wmmd));
                allowing(wmmd).getConfiguration();
                will(returnValue(wac));
                allowing(wac).getApplicationName();
                will(returnValue(APP_NAME));
                allowing(wac).getModuleName();
                will(returnValue(MODULE_NAME));
                allowing(jsr).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(jsr).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                one(cc).locateService("webJaccService", jsr);
                will(returnValue(js));
                one(js).isAccessExcluded(APP_NAME, MODULE_NAME, URI_NAME, METHOD_NAME, ier);
                will(returnValue(true));
            }
        });
        ComponentMetaDataAccessorImpl cmda = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        cmda.beginContext(wcmd);

        ajsr.setReference(jsr);
        ajsr.activate(cc);
        WebAppJaccAuthorizationHelper wjah = new WebAppJaccAuthorizationHelper(ajsr);
        WebReply output = wjah.checkPrecludedAccess(wr, URI_NAME);
        assertTrue(output instanceof DenyReply);
    }
}
