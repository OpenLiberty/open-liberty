/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.HttpConstraintElement;
import javax.servlet.HttpMethodConstraintElement;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.webcontainer.security.ServletStartedListener;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraint;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollection;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.ws.webcontainer.security.metadata.WebResourceCollection;
import com.ibm.ws.webcontainer.webapp.WebAppConfigExtended;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 *
 */
public class ServletStartedListenerTest {

    private final Mockery mock = new JUnit4Mockery();
    private ServletStartedListener servletStartedListener;
    private final WebModuleInfo deployedMod = mock.mock(WebModuleInfo.class);
    private final Container moduleContainer = mock.mock(Container.class);
    private final WebModuleMetaData webModuleMetaDataMock = mock.mock(WebModuleMetaData.class);
    private final SecurityMetadata securityMetadataMock = mock.mock(SecurityMetadata.class, "securityMetadataMock_createTestSecurityMetadata");

    private final static Collection<String> emptyList = new ArrayList<String>();

    private final static String ddRunAsRole = "ddRunAsRole";
    private final static String ddServlet = "ddServlet";
    private final static String annotatedRunAsRole = "annotatedRunAsRole";
    private final static String annotatedServlet = "annotatedServlet";
    private final static List<String> URL_PATTERN_DD_LIST = new ArrayList<String>();
    private final static String URL_PATTERN_DD = "urlPatternDD";
    private final static String URL_PATTERN_ANNO = "urlPatternAnno";
    private final static Collection<String> URL_PATTERN_ANNO_LIST = new ArrayList<String>();

    private final static String ROLE1_ALLOWED_ANNO = "AnnoRole1";
    private final static String ROLE2_ALLOWED_ANNO = "AnnoRole2";
    private final static List<String> ROLES_ALLOWED_ANNO = new ArrayList<String>();
    private final static String HTTP_METHOD_DD = "CUSTOM";
    private final static String HTTP_METHOD_ANNO = "POST";

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        URL_PATTERN_DD_LIST.add(URL_PATTERN_DD);
        ROLES_ALLOWED_ANNO.add(ROLE1_ALLOWED_ANNO);
        ROLES_ALLOWED_ANNO.add(ROLE2_ALLOWED_ANNO);
        URL_PATTERN_ANNO_LIST.add(URL_PATTERN_ANNO);
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        servletStartedListener = new ServletStartedListener();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {}

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.ServletStartedListener#started(com.ibm.ws.container.service.app.deploy.DeployedMod)}.
     * 
     * @throws UnableToAdaptException
     */
    @Test
    public void testStarted() throws UnableToAdaptException {

        final WebAppConfigExtended webAppConfig = createWebAppConfigMock();

        final Iterator<IServletConfig> servletConfigs = createServletConfigsMock();
        mock.checking(new Expectations() {
            {
                allowing(deployedMod).getContainer();
                will(returnValue(moduleContainer));
                allowing(moduleContainer).adapt(WebAppConfig.class);
                will(returnValue(webAppConfig));

                allowing(webAppConfig).getServletInfos();
                will(returnValue(servletConfigs));
                allowing(webAppConfig).getMetaData();
                will(returnValue(webModuleMetaDataMock));
                allowing(webModuleMetaDataMock).getSecurityMetaData();
                will(returnValue(securityMetadataMock));
            }
        });

        servletStartedListener.started(deployedMod.getContainer());
        mock.assertIsSatisfied();

    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.ServletStartedListener#updateSecurityMetadataWithRunAs()}.
     */
    @Test
    public void testUpdateSecurityMetadataWithRunAs() {
        final SecurityMetadata securityMetadataFromDD = createTestSecurityMetadata();
        final IServletConfig servletConfig = mock.mock(IServletConfig.class, "servletConfigMockForRunAs");

        final Map<String, String> runAsMapFromDD = new HashMap<String, String>();
        runAsMapFromDD.put(ddServlet, ddRunAsRole);
        final List<String> ddRoles = new ArrayList<String>();
        ddRoles.add(ddRunAsRole);

        mock.checking(new Expectations() {
            {
                allowing(servletConfig).getRunAsRole();
                will(returnValue(annotatedRunAsRole));
                allowing(servletConfig).getServletName();
                will(returnValue(annotatedServlet));

                allowing(securityMetadataFromDD).getRunAsMap();
                will(returnValue(runAsMapFromDD));
                allowing(securityMetadataFromDD).getRoles();
                will(returnValue(ddRoles));
            }
        });
        servletStartedListener.updateSecurityMetadataWithRunAs(securityMetadataFromDD, servletConfig);
        mock.assertIsSatisfied();

        List<String> mergedRoles = securityMetadataFromDD.getRoles();
        List<String> expectedRolesAfter = ddRoles;
        expectedRolesAfter.add(annotatedRunAsRole);
        assertEquals("The list of roles should now be updated with the runAsRole from the annotations.", expectedRolesAfter, mergedRoles);

        Map<String, String> mergedRunAsMap = securityMetadataFromDD.getRunAsMap();
        Map<String, String> expectedRunAsMap = runAsMapFromDD;
        expectedRunAsMap.put(annotatedServlet, annotatedRunAsRole);
        assertEquals("The run-as map should now be updated the annotated servlet name and run as role.", expectedRunAsMap, mergedRunAsMap);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.ServletStartedListener#updateSecurityMetadataWithRunAs()}.
     */
    @Test
    public void testUpdateSecurityMetadataWithRunAs_withConflicts() {
        final SecurityMetadata securityMetadataFromDD = createTestSecurityMetadata();
        final IServletConfig servletConfig = mock.mock(IServletConfig.class, "servletConfigMockForRunAsConflicts");

        final Map<String, String> runAsMapFromDD = new HashMap<String, String>();
        runAsMapFromDD.put(ddServlet, ddRunAsRole);
        final List<String> ddRoles = new ArrayList<String>();
        ddRoles.add(ddRunAsRole);

        mock.checking(new Expectations() {
            {
                allowing(servletConfig).getRunAsRole();
                will(returnValue(annotatedRunAsRole));
                allowing(servletConfig).getServletName();
                will(returnValue(ddServlet));

                allowing(securityMetadataFromDD).getRunAsMap();
                will(returnValue(runAsMapFromDD));
                allowing(securityMetadataFromDD).getRoles();
                will(returnValue(ddRoles));
            }
        });
        servletStartedListener.updateSecurityMetadataWithRunAs(securityMetadataFromDD, servletConfig);
        mock.assertIsSatisfied();

        List<String> mergedRoles = securityMetadataFromDD.getRoles();
        List<String> expectedRolesAfter = ddRoles;
        assertEquals("The list of roles should NOT be updated with the runAsRole from the annotations.", expectedRolesAfter, mergedRoles);

        Map<String, String> mergedRunAsMap = securityMetadataFromDD.getRunAsMap();
        Map<String, String> expectedRunAsMap = runAsMapFromDD;
        assertEquals("The run-as map should NOT be updated the annotated servlet name and run as role.", expectedRunAsMap, mergedRunAsMap);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.ServletStartedListener#updateSecurityMetadataWithSecurityConstraints()}.
     */
    @Test
    public void testUpdateSecurityMetadataWithSecurityConstraints() {
        final SecurityMetadata securityMetadataFromDD = createTestSecurityMetadata();
        final ServletSecurityElement servletSecurity = createServletSecurityMock();
        final IServletConfig servletConfig = mock.mock(IServletConfig.class, "servletConfigMockForSecConstr");
        final SecurityConstraintCollection secConstrCollection = mock.mock(SecurityConstraintCollection.class);
        final List<SecurityConstraint> secConstraints = new ArrayList<SecurityConstraint>();
        SecurityConstraint ddSecConstraint = createSecurityConstraint(URL_PATTERN_DD_LIST, HTTP_METHOD_DD, null, new ArrayList<String>(), false, false, false, false);
        secConstraints.add(ddSecConstraint);

        mock.checking(new Expectations() {
            {
                allowing(servletConfig).getMappings();
                will(returnValue(URL_PATTERN_ANNO_LIST));
                allowing(servletConfig).getServletSecurity();
                will(returnValue(servletSecurity));

                allowing(securityMetadataFromDD).getSecurityConstraintCollection();
                will(returnValue(secConstrCollection));
                allowing(securityMetadataFromDD).isDenyUncoveredHttpMethods();

                allowing(secConstrCollection).getSecurityConstraints();
                will(returnValue(secConstraints));
                allowing(securityMetadataFromDD).getRoles();
                will(returnValue(emptyList));

            }
        });
        servletStartedListener.updateSecurityMetadataWithSecurityConstraints(securityMetadataFromDD, servletConfig);
        mock.assertIsSatisfied();
        List<SecurityConstraint> mergedConstraints = securityMetadataFromDD.getSecurityConstraintCollection().getSecurityConstraints();
        SecurityConstraint expectedConstraintForAnno1 = createSecurityConstraint((List<String>) URL_PATTERN_ANNO_LIST, null, HTTP_METHOD_ANNO, ROLES_ALLOWED_ANNO, true, false,
                                                                                 false,
                                                                                 false);
        SecurityConstraint expectedConstraintForAnno2 = createSecurityConstraint((List<String>) URL_PATTERN_ANNO_LIST, HTTP_METHOD_ANNO, null, new ArrayList<String>(), true, true,
                                                                                 false,
                                                                                 false);

        //Check 1st constraint from annotations is correct
        assertEquals("The roles should contain " + expectedConstraintForAnno1.getRoles() + " for the first merged annotated security constraint.",
                     expectedConstraintForAnno1.getRoles(),
                     mergedConstraints.get(1).getRoles());
        assertEquals("isSSLRequired should be " + expectedConstraintForAnno1.isSSLRequired() + " for the first merged annotated security constraint.",
                     expectedConstraintForAnno1.isSSLRequired(), mergedConstraints.get(1).isSSLRequired());
        assertEquals("isAccessPrecluded should be " + expectedConstraintForAnno1.isAccessPrecluded() + " for the first merged annotated security constraint.",
                     expectedConstraintForAnno1.isAccessPrecluded(), mergedConstraints.get(1).isAccessPrecluded());
        assertEquals("The url pattern is not correct for the first merged annotated security constraint.",
                     expectedConstraintForAnno1.getWebResourceCollections().get(0).getUrlPatterns(), mergedConstraints.get(1).getWebResourceCollections().get(0).getUrlPatterns());

        //Check 2nd constraint from annotations is correct
        assertEquals("The roles should contain " + expectedConstraintForAnno2.getRoles() + " for the second merged annotated security constraint.",
                     expectedConstraintForAnno2.getRoles(),
                     mergedConstraints.get(2).getRoles());
        assertEquals("isSSLRequired should be " + expectedConstraintForAnno2.isSSLRequired() + " for the second merged annotated security constraint.",
                     expectedConstraintForAnno2.isSSLRequired(), mergedConstraints.get(2).isSSLRequired());
        assertEquals("isAccessPrecluded should be " + expectedConstraintForAnno2.isAccessPrecluded() + " for the second merged annotated security constraint.",
                     expectedConstraintForAnno2.isAccessPrecluded(), mergedConstraints.get(2).isAccessPrecluded());
        assertEquals("The url pattern is not correct for the second merged annotated security constraint.",
                     expectedConstraintForAnno2.getWebResourceCollections().get(0).getUrlPatterns(), mergedConstraints.get(2).getWebResourceCollections().get(0).getUrlPatterns());

    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.ServletStartedListener#updateSecurityMetadataWithSecurityConstraints()}.
     */
    @Test
    public void testUpdateSecurityMetadataWithSecurityConstraints_nullMappings() {
        final SecurityMetadata securityMetadataFromDD = createTestSecurityMetadata();
        final ServletSecurityElement servletSecurity = createServletSecurityMock();
        final IServletConfig servletConfig = mock.mock(IServletConfig.class, "servletConfigMockForSecConstr");
        final SecurityConstraintCollection secConstrCollection = mock.mock(SecurityConstraintCollection.class);
        final List<SecurityConstraint> secConstraints = new ArrayList<SecurityConstraint>();
        SecurityConstraint ddSecConstraint = createSecurityConstraint(URL_PATTERN_DD_LIST, HTTP_METHOD_DD, null, new ArrayList<String>(), false, false, false, false);
        secConstraints.add(ddSecConstraint);

        mock.checking(new Expectations() {
            {
                allowing(servletConfig).getServletSecurity();
                will(returnValue(servletSecurity));
                allowing(servletConfig).getMappings();
                will(returnValue(null));

                allowing(securityMetadataFromDD).getSecurityConstraintCollection();
                will(returnValue(secConstrCollection));
                allowing(secConstrCollection).getSecurityConstraints();
                will(returnValue(secConstraints));
            }
        });
        servletStartedListener.updateSecurityMetadataWithSecurityConstraints(securityMetadataFromDD, servletConfig);
        mock.assertIsSatisfied();
        List<SecurityConstraint> mergedConstraints = securityMetadataFromDD.getSecurityConstraintCollection().getSecurityConstraints();

        assertEquals("The security constraints after the merge should be the same as before the merge because there are no URL mappings for this servlet (so it's not added).",
                     secConstraints, mergedConstraints);
    }

    private WebAppConfigExtended createWebAppConfigMock() throws UnableToAdaptException {
        final WebAppConfigExtended webAppConfig = mock.mock(WebAppConfigExtended.class);
        final WebModuleMetaData wmmd = createWebModuleMetaDataMock();
        mock.checking(new Expectations() {
            {
                allowing(webAppConfig).getMetaData();
                will(returnValue(wmmd));

                allowing(deployedMod).getContainer();
                will(returnValue(moduleContainer));
                allowing(moduleContainer).adapt(WebModuleMetaData.class);
                will(returnValue(wmmd));
            }
        });
        return webAppConfig;
    }

    private WebModuleMetaData createWebModuleMetaDataMock() {
        final SecurityMetadata securityMetadata = createTestSecurityMetadata();

        final SecurityConstraintCollection secConstrCollection = mock.mock(SecurityConstraintCollection.class);
        final List<SecurityConstraint> secConstraints = new ArrayList<SecurityConstraint>();

        mock.checking(new Expectations() {
            {
                allowing(webModuleMetaDataMock).setSecurityMetaData(with(any(SecurityMetadata.class)));
                allowing(webModuleMetaDataMock).getSecurityMetaData();
                will(returnValue(securityMetadata));

                allowing(securityMetadata).getSecurityConstraintCollection();
                will(returnValue(secConstrCollection));

                allowing(secConstrCollection).getSecurityConstraints();
                will(returnValue(secConstraints));

            }
        });
        return webModuleMetaDataMock;
    }

    private SecurityMetadata createTestSecurityMetadata() {
        mock.checking(new Expectations() {
            {
            }
        });
        return securityMetadataMock;
    }

    private Iterator<IServletConfig> createServletConfigsMock() {
        List<IServletConfig> servletConfigs = new ArrayList<IServletConfig>();
        final IServletConfig servletConfig_noRunAsOrServletSecurity = mock.mock(IServletConfig.class, "servletConfigMock_NoRunAsOrServletSecurity");
        servletConfigs.add(servletConfig_noRunAsOrServletSecurity);

        mock.checking(new Expectations() {
            {
                allowing(servletConfig_noRunAsOrServletSecurity).getServletName();
                will(returnValue("noRunAsOrServletSecurityServlet"));
                allowing(servletConfig_noRunAsOrServletSecurity).getRunAsRole();
                will(returnValue(null));
                allowing(servletConfig_noRunAsOrServletSecurity).getServletSecurity();
                will(returnValue(null));
            }
        });
        return servletConfigs.iterator();
    }

    private ServletSecurityElement createServletSecurityMock() {
        Collection<HttpMethodConstraintElement> httpMethodConstraints = createHTTPMethodConstraints();
        String[] rolesAllowed = ROLES_ALLOWED_ANNO.toArray(new String[ROLES_ALLOWED_ANNO.size()]);
        HttpConstraintElement httpConstraintWithRoles = createHTTPConstraintElement(rolesAllowed);
        ServletSecurityElement servletSecurity = new ServletSecurityElement(httpConstraintWithRoles, httpMethodConstraints);

        return servletSecurity;
    }

    private Collection<HttpMethodConstraintElement> createHTTPMethodConstraints() {
        Collection<HttpMethodConstraintElement> httpMethodConstraints = new ArrayList<HttpMethodConstraintElement>();
        String[] emptyRoles = {};
        HttpConstraintElement httpConstraintWithoutRoles = createHTTPConstraintElement(emptyRoles);
        HttpMethodConstraintElement httpMethodConstraint = new HttpMethodConstraintElement(HTTP_METHOD_ANNO, httpConstraintWithoutRoles);
        httpMethodConstraints.add(httpMethodConstraint);
        return httpMethodConstraints;
    }

    private HttpConstraintElement createHTTPConstraintElement(String[] rolesAllowed) {
        return new HttpConstraintElement(EmptyRoleSemantic.DENY, TransportGuarantee.CONFIDENTIAL, rolesAllowed);
    }

    /**
     * 
     * @param urlPatterns
     * @param httpMethod
     * @param rolesAllowed
     * @param sslRequired
     * @param accessPrecluded
     * @return
     */
    private SecurityConstraint createSecurityConstraint(List<String> urlPatterns, String httpMethod, String omissionMethod, List<String> rolesAllowed, boolean sslRequired,
                                                        boolean accessPrecluded, boolean fromHttpConstraint, boolean accessUncovered) {
        List<WebResourceCollection> webResourceCollections = new ArrayList<WebResourceCollection>();
        List<String> httpMethods = new ArrayList<String>();
        if (httpMethod != null) {
            httpMethods.add(httpMethod);
        }
        List<String> omissionMethods = new ArrayList<String>();
        if (omissionMethod != null) {
            omissionMethods.add(omissionMethod);
        }
        WebResourceCollection webResCollection = new WebResourceCollection(urlPatterns, httpMethods, omissionMethods);
        webResourceCollections.add(webResCollection);
        return new SecurityConstraint(webResourceCollections, rolesAllowed, sslRequired, accessPrecluded, fromHttpConstraint, accessUncovered);
    }
}
