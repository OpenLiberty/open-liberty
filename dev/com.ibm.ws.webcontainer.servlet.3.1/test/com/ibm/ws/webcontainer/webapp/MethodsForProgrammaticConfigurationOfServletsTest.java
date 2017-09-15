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
package com.ibm.ws.webcontainer.webapp;

/*
 * Tests this sentence from first paragraph of section 4.4 of Servlet 3.1 final spec:
 * If the ServletContext passed to the ServletContextListener’s contextInitialized method
 * where the ServletContextListener was neither declared in web.xml or web-fragment.xml nor
 * annotated with @WebListener then an UnsupportedOperationException MUST be thrown for all the
 * methods defined in ServletContext for programmatic configuration of servlets, filters and
 * listeners.
 *
 */

import static org.junit.Assert.assertTrue;

import org.jmock.Mockery;
import org.junit.Test;

import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.ws.container.service.metadata.MetaDataService;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.osgi.srt.SRTConnectionContext;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.ws.webcontainer31.osgi.webapp.WebApp31;

public class MethodsForProgrammaticConfigurationOfServletsTest {

    private final Mockery context = new Mockery();
    final private IRequest request = context.mock(IRequest.class);
    private SRTConnectionContext connContext;

    @Test
    public void testDeclareRoles() {

        final MetaDataService mds = context.mock(MetaDataService.class);
        final J2EENameFactory jnf = context.mock(J2EENameFactory.class);

        WebAppConfiguration webAppConfig = new WebAppConfiguration(null, "name");
        WebApp31 webApp = new WebApp31(webAppConfig, null, null, mds, jnf, null);

        webApp.withinContextInitOfProgAddListener = true;

        boolean caughtUnsupportedOperationException = false;
        try {
            webApp.declareRoles("someArg"); /**/
        } catch (UnsupportedOperationException uoe) {
            caughtUnsupportedOperationException = true;
        }

        assertTrue(caughtUnsupportedOperationException);
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }

    @Test
    public void testGetClassLoader() {

        final MetaDataService mds = context.mock(MetaDataService.class);
        final J2EENameFactory jnf = context.mock(J2EENameFactory.class);

        WebAppConfiguration webAppConfig = new WebAppConfiguration(null, "name");
        WebApp31 webApp = new WebApp31(webAppConfig, null, null, mds, jnf, null);

        webApp.withinContextInitOfProgAddListener = true;

        boolean caughtUnsupportedOperationException = false;
        try {
            webApp.getClassLoader(); /**/
        } catch (UnsupportedOperationException uoe) {
            caughtUnsupportedOperationException = true;
        }

        assertTrue(caughtUnsupportedOperationException);
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }

    @Test
    public void testGetEffectiveMajorVersion() {

        final MetaDataService mds = context.mock(MetaDataService.class);
        final J2EENameFactory jnf = context.mock(J2EENameFactory.class);

        WebAppConfiguration webAppConfig = new WebAppConfiguration(null, "name");
        WebApp31 webApp = new WebApp31(webAppConfig, null, null, mds, jnf, null);

        webApp.withinContextInitOfProgAddListener = true;

        boolean caughtUnsupportedOperationException = false;
        try {
            webApp.getEffectiveMajorVersion(); /**/
        } catch (UnsupportedOperationException uoe) {
            caughtUnsupportedOperationException = true;
        }

        assertTrue(caughtUnsupportedOperationException);
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }

    @Test
    public void testGetEffectiveMinorVersion() {

        final MetaDataService mds = context.mock(MetaDataService.class);
        final J2EENameFactory jnf = context.mock(J2EENameFactory.class);

        WebAppConfiguration webAppConfig = new WebAppConfiguration(null, "name");
        WebApp31 webApp = new WebApp31(webAppConfig, null, null, mds, jnf, null);

        webApp.withinContextInitOfProgAddListener = true;

        boolean caughtUnsupportedOperationException = false;
        try {
            webApp.getEffectiveMinorVersion(); /**/
        } catch (UnsupportedOperationException uoe) {
            caughtUnsupportedOperationException = true;
        }

        assertTrue(caughtUnsupportedOperationException);
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }

    @Test
    public void testGetFilterRegistration() {

        final MetaDataService mds = context.mock(MetaDataService.class);
        final J2EENameFactory jnf = context.mock(J2EENameFactory.class);

        WebAppConfiguration webAppConfig = new WebAppConfiguration(null, "name");
        WebApp31 webApp = new WebApp31(webAppConfig, null, null, mds, jnf, null);

        webApp.withinContextInitOfProgAddListener = true;

        boolean caughtUnsupportedOperationException = false;
        try {
            webApp.getFilterRegistration("some arg"); /**/
        } catch (UnsupportedOperationException uoe) {
            caughtUnsupportedOperationException = true;
        }

        assertTrue(caughtUnsupportedOperationException);
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }

    @Test
    public void testGetFilterRegistrations() {

        final MetaDataService mds = context.mock(MetaDataService.class);
        final J2EENameFactory jnf = context.mock(J2EENameFactory.class);

        WebAppConfiguration webAppConfig = new WebAppConfiguration(null, "name");
        WebApp31 webApp = new WebApp31(webAppConfig, null, null, mds, jnf, null);

        webApp.withinContextInitOfProgAddListener = true;

        boolean caughtUnsupportedOperationException = false;
        try {
            webApp.getFilterRegistrations(); /**/
        } catch (UnsupportedOperationException uoe) {
            caughtUnsupportedOperationException = true;
        }

        assertTrue(caughtUnsupportedOperationException);
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }

    @Test
    public void testGetServletRegistration() {

        final MetaDataService mds = context.mock(MetaDataService.class);
        final J2EENameFactory jnf = context.mock(J2EENameFactory.class);

        WebAppConfiguration webAppConfig = new WebAppConfiguration(null, "name");
        WebApp31 webApp = new WebApp31(webAppConfig, null, null, mds, jnf, null);

        webApp.withinContextInitOfProgAddListener = true;

        boolean caughtUnsupportedOperationException = false;
        try {
            webApp.getServletRegistration("some arg"); /**/
        } catch (UnsupportedOperationException uoe) {
            caughtUnsupportedOperationException = true;
        }

        assertTrue(caughtUnsupportedOperationException);
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }

    @Test
    public void testGetServletRegistrations() {

        final MetaDataService mds = context.mock(MetaDataService.class);
        final J2EENameFactory jnf = context.mock(J2EENameFactory.class);

        WebAppConfiguration webAppConfig = new WebAppConfiguration(null, "name");
        WebApp31 webApp = new WebApp31(webAppConfig, null, null, mds, jnf, null);

        webApp.withinContextInitOfProgAddListener = true;

        boolean caughtUnsupportedOperationException = false;
        try {
            webApp.getServletRegistrations(); /**/
        } catch (UnsupportedOperationException uoe) {
            caughtUnsupportedOperationException = true;
        }

        assertTrue(caughtUnsupportedOperationException);
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }

    @Test
    public void testGetVirtualServerName() {

        final MetaDataService mds = context.mock(MetaDataService.class);
        final J2EENameFactory jnf = context.mock(J2EENameFactory.class);

        WebAppConfiguration webAppConfig = new WebAppConfiguration(null, "name");
        WebApp31 webApp = new WebApp31(webAppConfig, null, null, mds, jnf, null);

        webApp.withinContextInitOfProgAddListener = true;

        boolean caughtUnsupportedOperationException = false;
        try {
            webApp.getVirtualServerName(); /**/
        } catch (UnsupportedOperationException uoe) {
            caughtUnsupportedOperationException = true;
        }

        assertTrue(caughtUnsupportedOperationException);
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }
}
