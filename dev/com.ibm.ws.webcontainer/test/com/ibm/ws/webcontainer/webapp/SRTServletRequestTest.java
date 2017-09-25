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

// This test class is in the com.ibm.ws.webcontainer.webapp package as teh WebApp class has protected methods that this
// test class needs to access.

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.util.Hashtable;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.ws.container.service.metadata.MetaDataService;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.osgi.collaborator.CollaboratorHelperImpl;
import com.ibm.ws.webcontainer.osgi.collaborator.CollaboratorServiceImpl;
import com.ibm.ws.webcontainer.osgi.srt.SRTConnectionContext;
import com.ibm.ws.webcontainer.osgi.webapp.WebApp;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppSecurityCollaborator;
import com.ibm.wsspi.webcontainer.util.URIMatcherFactory;

public class SRTServletRequestTest {

    private final Mockery context = new Mockery();
    final private IRequest request = context.mock(IRequest.class);
    private SRTConnectionContext connContext;
    final private URIMatcherFactory matcherFactory = context.mock(URIMatcherFactory.class);

    @Before
    public void setupURIMatcherFactory() {
        
        context.checking(new Expectations() {
            {
                one(matcherFactory).createURIMatcher(with(any(Boolean.class)));
            }
        });
        
        // Set the factory that is necessary for the creation of the objects used by this
        // unit test.
        SRTServletRequestTest.WebContainer webContainer = new WebContainer();
        webContainer.setURIMatcherFactory(matcherFactory);
    }

    @Test
    public void testEncoding() throws Exception {        
        connContext = new SRTConnectionContext();
        context.checking(new Expectations() {
            {
                atLeast(1).of(request).getContentType();
                will(onConsecutiveCalls(returnValue("text/html;charset='utf-8';action='foo'"), returnValue("text/html;charset='utf-8';action=foo"),
                                        returnValue("text/html;charset='utf-8'"), returnValue("text/html;charset=utf-8;action='foo'"),
                                        returnValue("text/html;action='utf-8';charset='foo'")));
                atLeast(1).of(request).getInputStream();
                atLeast(1).of(request).getContentLength();
            }
        });
        SRTServletRequest srtReq = new SRTServletRequest(connContext);
        srtReq.initForNextRequest(request);
        assertEquals("utf-8", srtReq.getCharacterEncoding());
        assertEquals("utf-8", srtReq.getCharacterEncoding());
        assertEquals("utf-8", srtReq.getCharacterEncoding());
        assertEquals("utf-8", srtReq.getCharacterEncoding());
        assertEquals("utf-8", srtReq.getCharacterEncoding());
    }

    /**
     * Verifies that if the IWebAppSecurityCollaborator is not set, we have no
     * security, so we have no Principal.
     */
    @Test
    public void getUserPrincipal_noIWebAppSecurityCollaborator() {
        SRTServletRequest srtReq = new SRTServletRequest(connContext);
        assertNull("No IWebAppSecurityCollaborator, so no Principal",
                   srtReq.getUserPrincipal());
    }

    /**
     * Verifies that if the IWebAppSecurityCollaborator is not set, we have no
     * security, so we have no Principal.
     */
    @Test
    public void getRemoteUser_noIWebAppSecurityCollaborator() throws Exception {        
        SRTServletRequest srtReq = new SRTServletRequest(connContext);
        context.checking(new Expectations() {
            {
                allowing(request).getRemoteUser();
                // If security is disabled the internal request will check the value of the
                // $WSRU header and return its value or null if it doesn't exist
                will(returnValue(null));
                atLeast(1).of(request).getInputStream();
                atLeast(1).of(request).getContentLength();
            }
        });
        // Need to init the internal _request object to avoid NPE.
        srtReq.initForNextRequest(request);
        // remote user will still be null since the WSRU header is not set in this test.

        assertNull("No IWebAppSecurityCollaborator, so no RemoteUser",
                   srtReq.getRemoteUser());
    }

    @Test
    public void getRemoteUser_noIWebAppSecurityCollaborator_mockPrivateHeader() throws Exception {        
        SRTServletRequest srtReq = new SRTServletRequest(connContext);
        context.checking(new Expectations() {
            {
                allowing(request).getRemoteUser();
                // The internal request will check the value of the $WSRU header and return its value
                will(returnValue("myUser"));
                atLeast(1).of(request).getInputStream();
                atLeast(1).of(request).getContentLength();
            }
        });
        srtReq.initForNextRequest(request);

        assertTrue("Header set, RemoteUser is \"myName\"",
                   srtReq.getRemoteUser().equals("myUser"));
    }

    /**
     * Verifies that if the IWebAppSecurityCollaborator is not set, we have no
     * security, so we have no Principal.
     */
    @Test
    public void isUserInRole_noIWebAppSecurityCollaborator() {
        SRTServletRequest srtReq = new SRTServletRequest(connContext);
        assertFalse("No IWebAppSecurityCollaborator, so no user to check is in role",
                    srtReq.isUserInRole("someRole"));
    }

    class MyCollaboratorServiceImpl extends CollaboratorServiceImpl {
        @Override
        protected void activate(ComponentContext cc) {
            super.activate(cc);
        }

        @Override
        protected void deactivate(ComponentContext cc) {
            super.deactivate(cc);
        }
    }

    /**
     * Verifies that if the IWebAppSecurityCollaborator is available, we delegate
     * the getRemoteUser logic to it.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getRemoteUser_withIWebAppSecurityCollaborator() {
        final ComponentContext cc = context.mock(ComponentContext.class);
        final ServiceReference<IWebAppSecurityCollaborator> ref = context.mock(ServiceReference.class);
        final IWebAppSecurityCollaborator newWebAppSecCollab = context.mock(IWebAppSecurityCollaborator.class);
        final Principal principal = context.mock(Principal.class);
        final String principalName = "bob";
        final MetaDataService mds = context.mock(MetaDataService.class);
        final J2EENameFactory jnf = context.mock(J2EENameFactory.class);

        context.checking(new Expectations() {
            {
                ignoring(mds);
                one(ref).getProperty("com.ibm.ws.security.type");
                will(returnValue("com.ibm.ws.management"));
                one(ref).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                one(ref).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(cc).locateService("webAppSecurityCollaborator", ref);
                will(returnValue(newWebAppSecCollab));
                one(newWebAppSecCollab).getUserPrincipal();
                will(returnValue(principal));
                one(principal).getName();
                will(returnValue(principalName));
            }
        });

        // set an active component that will be associated with the registered collaborator
        Hashtable bundleHeaders = new Hashtable(1);
        bundleHeaders.put("IBM-Authorization-Roles", "com.ibm.ws.management");
        WebAppConfiguration webAppConfig = new WebAppConfiguration(null, "name");
        WebApp webApp = new WebApp(webAppConfig, null, null, mds, jnf, null);
        webAppConfig.setWebApp(webApp);
        webAppConfig.setBundleHeaders(bundleHeaders);
        webApp.setCollaboratorHelper(new CollaboratorHelperImpl(webApp, null));
        ComponentMetaData cmd = webAppConfig.getMetaData().getCollaboratorComponentMetaData();
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().beginContext(cmd);

        MyCollaboratorServiceImpl collabService = new MyCollaboratorServiceImpl();
        collabService.setWebAppSecurityCollaborator(ref);
        collabService.activate(cc);
        assertNotNull("Should have set up the WebAppSecurityCollaborator",
                      CollaboratorServiceImpl.getWebAppSecurityCollaborator("com.ibm.ws.management"));

        SRTServletRequest srtReq = new SRTServletRequest(connContext);
        String returnedPrincipalName = srtReq.getRemoteUser();
        assertEquals("We have a IWebAppSecurityCollaborator, so expect the RemoteUser",
                     principalName, returnedPrincipalName);

        context.assertIsSatisfied();
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }

    /**
     * Verifies that if the IWebAppSecurityCollaborator is available, we delegate
     * the getUserPrincipal logic to it.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void isUserInRole_withIWebAppSecurityCollaborator() {
        final ComponentContext cc = context.mock(ComponentContext.class);
        final ServiceReference<IWebAppSecurityCollaborator> ref = context.mock(ServiceReference.class);
        final IWebAppSecurityCollaborator webAppSecCollab = context.mock(IWebAppSecurityCollaborator.class);
        final String roleName = "someRole";
        final SRTServletRequest srtReq = new SRTServletRequest(connContext);
        final MetaDataService mds = context.mock(MetaDataService.class);
        final J2EENameFactory jnf = context.mock(J2EENameFactory.class);

        context.checking(new Expectations() {
            {
                ignoring(mds);
                one(ref).getProperty("com.ibm.ws.security.type");
                will(returnValue(null));
                one(ref).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                one(ref).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(cc).locateService("webAppSecurityCollaborator", ref);
                will(returnValue(webAppSecCollab));
                one(webAppSecCollab).isUserInRole(roleName, srtReq);
                will(returnValue(true));
            }
        });
        // set an active component that will be associated with the registered collaborator
        Hashtable bundleHeaders = new Hashtable(1);
        bundleHeaders.put("IBM-Authorization-Roles", "default");
        WebAppConfiguration webAppConfig = new WebAppConfiguration(null, "name");
        WebApp webApp = new WebApp(webAppConfig, null, null, mds, jnf, null);
        webAppConfig.setWebApp(webApp);
        webAppConfig.setBundleHeaders(bundleHeaders);
        webApp.setCollaboratorHelper(new CollaboratorHelperImpl(webApp, null));
        ComponentMetaData cmd = webAppConfig.getMetaData().getCollaboratorComponentMetaData();
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().beginContext(cmd);

        MyCollaboratorServiceImpl collabService = new MyCollaboratorServiceImpl();
        collabService.setWebAppSecurityCollaborator(ref);
        collabService.activate(cc);
        assertNotNull("Should have set up the WebAppSecurityCollaborator",
                      CollaboratorServiceImpl.getWebAppSecurityCollaborator("default"));
        assertTrue("We have a IWebAppSecurityCollaborator, return what it returns",
                   srtReq.isUserInRole(roleName));

        context.assertIsSatisfied();
        // clean thread
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }

    /**
     * Verifies that if the IWebAppSecurityCollaborator is available, we delegate
     * the getUserPrincipal logic to it.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getUserPrincipal_withIWebAppSecurityCollaborator() {
        final ComponentContext cc = context.mock(ComponentContext.class);
        final ServiceReference<IWebAppSecurityCollaborator> ref = context.mock(ServiceReference.class);
        final IWebAppSecurityCollaborator webAppSecCollab = context.mock(IWebAppSecurityCollaborator.class);
        final Principal principal = context.mock(Principal.class);
        final MetaDataService mds = context.mock(MetaDataService.class);
        final J2EENameFactory jnf = context.mock(J2EENameFactory.class);

        context.checking(new Expectations() {
            {
                ignoring(mds);
                one(ref).getProperty("com.ibm.ws.security.type");
                will(returnValue("com.ibm.ws.management"));
                one(ref).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                one(ref).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(cc).locateService("webAppSecurityCollaborator", ref);
                will(returnValue(webAppSecCollab));
                one(webAppSecCollab).getUserPrincipal();
                will(returnValue(principal));
            }
        });
        // set an active component that will be associated with the registered collaborator
        Hashtable bundleHeaders = new Hashtable(1);
        bundleHeaders.put("IBM-Authorization-Roles", "com.ibm.ws.management");
        WebAppConfiguration webAppConfig = new WebAppConfiguration(null, "name");
        WebApp webApp = new WebApp(webAppConfig, null, null, mds, jnf, null);
        webAppConfig.setWebApp(webApp);
        webAppConfig.setBundleHeaders(bundleHeaders);
        webApp.setCollaboratorHelper(new CollaboratorHelperImpl(webApp, null));
        ComponentMetaData cmd = webAppConfig.getMetaData().getCollaboratorComponentMetaData();
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().beginContext(cmd);

        MyCollaboratorServiceImpl collabService = new MyCollaboratorServiceImpl();
        collabService.setWebAppSecurityCollaborator(ref);
        collabService.activate(cc);
        assertNotNull("Should have set up the WebAppSecurityCollaborator",
                      CollaboratorServiceImpl.getWebAppSecurityCollaborator("com.ibm.ws.management"));

        assertEquals("didn't get our own collaborator back", webAppSecCollab,
                     CollaboratorServiceImpl.getWebAppSecurityCollaborator("com.ibm.ws.management"));

        SRTServletRequest srtReq = new SRTServletRequest(connContext);
        assertEquals("We have a IWebAppSecurityCollaborator, so expect the Principal",
                     principal, srtReq.getUserPrincipal());

        context.assertIsSatisfied();
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }

    /*
     * This is an inner class for testing. We needed a way to set the factories necessary
     * to create the objects used by the unit tests. Since there is no runtime involved here
     * Declarative Services is never invoked to actually set the factories resulting in NPEs.
     *
     * Extending the actual com.ibm.ws.webcontainer.osgi.WebContainer allows us to create
     * public methods that just call through to the protected methods to set the factories.
     */
    private class WebContainer extends com.ibm.ws.webcontainer.osgi.WebContainer {

        @Override
        public void setURIMatcherFactory(URIMatcherFactory factory) {
            super.setURIMatcherFactory(factory);
        }
    }

}
