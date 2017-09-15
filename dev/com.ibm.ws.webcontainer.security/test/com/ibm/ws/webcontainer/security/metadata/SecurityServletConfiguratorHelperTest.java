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
package com.ibm.ws.webcontainer.security.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.security.DeclareRoles;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.container.service.annotations.FragmentAnnotations;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.config.ServletConfigurator;
import com.ibm.ws.container.service.config.ServletConfigurator.ConfigItem;
import com.ibm.ws.container.service.config.ServletConfigurator.ConfigSource;
import com.ibm.ws.container.service.config.WebFragmentInfo;
import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.javaee.dd.common.RunAs;
import com.ibm.ws.javaee.dd.common.SecurityRole;
import com.ibm.ws.javaee.dd.common.SecurityRoleRef;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.WebFragment;
import com.ibm.ws.javaee.dd.web.common.AuthConstraint;
import com.ibm.ws.javaee.dd.web.common.FormLoginConfig;
import com.ibm.ws.javaee.dd.web.common.LoginConfig;
import com.ibm.ws.javaee.dd.web.common.Servlet;
import com.ibm.ws.javaee.dd.web.common.ServletMapping;
import com.ibm.ws.javaee.dd.web.common.UserDataConstraint;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.AnnotationValue;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;

import junit.framework.Assert;
import test.common.SharedOutputManager;

/**
 *
 */
public class SecurityServletConfiguratorHelperTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final static String ONE_EXACT_URL = "/oneExactURL/oneExactURL.oneExactURL";
    private final static String WEBAPP_AUTH_METHOD = "FORM";
    private final static String WEBAPP_REALM_NAME = "WebRealmForWebApp";
    private final static String SERVLET_NAME = "ServletName";
    private final static String WEBAPP_RUNAS_ROLE = "RunAsRoleNameForWebApp";
    private final static String WEBFRAGMENT_RUNAS_ROLE = "RunAsRoleNameForWebFragment";
    private final static Map<String, String> RUNAS_MAP = new HashMap<String, String>();
    private final static List<String> WEBAPP_ROLES = new ArrayList<String>();
    private final static String WEBAPP_ROLE = "SecurityRoleInWebXML";
    private final static List<String> WEBFRAGMENT_ROLES = new ArrayList<String>();
    private final static String WEBFRAGMENT_ROLE = "SecurityRoleInWebFragment";

    private final List<String> urlPatterns = createTestUrlPatterns();
    private final List<String> httpMethods = createTestHttpMethods();

    private final static String staticAnnotationRole1 = "DECLAREROLE1";
    private final static String staticAnnotationRole2 = "DECLAREROLE2";
    private final static List<String> STATICANNOTATION_ROLES = new ArrayList<String>();
    private final static List<String> EMPTY_LIST = new ArrayList<String>();

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    final FormLoginConfig formLoginConfigFromMock = createFormLoginConfigMock("formLoginConfig");
    final Map<String, ConfigItem<RunAs>> emptyRunAsConfigMap = new HashMap<String, ServletConfigurator.ConfigItem<RunAs>>();
    final Map<String, ConfigItem<FormLoginConfig>> emptyFormLoginConfigMap = new HashMap<String, ServletConfigurator.ConfigItem<FormLoginConfig>>();
    final WebModuleMetaData wmmd = mockery.mock(WebModuleMetaData.class);
    ServletConfigurator configurator = mockery.mock(ServletConfigurator.class);
    final SecurityServletConfiguratorHelper configHelper = new SecurityServletConfiguratorHelper(configurator);
    final ConfigItem<List<String>> rolesConfigItemFromWebApp = mockery.mock(ConfigItem.class, "RoleConfigItemFromWebApp");
    final ConfigItem<String> transportGuaranteeConfigItemFromWebApp = mockery.mock(ConfigItem.class, "TransportGuaranteeConfigItemFromWebApp");
    LoginConfig loginConfig = mockery.mock(LoginConfig.class);

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        RUNAS_MAP.put(SERVLET_NAME, WEBAPP_RUNAS_ROLE);
        WEBAPP_ROLES.add(WEBAPP_ROLE);
        WEBFRAGMENT_ROLES.add(WEBFRAGMENT_ROLE);
        STATICANNOTATION_ROLES.add(staticAnnotationRole1);
        STATICANNOTATION_ROLES.add(staticAnnotationRole2);
    }

    private List<String> createTestUrlPatterns() {
        List<String> urlPatterns = new ArrayList<String>();
        urlPatterns.add("/oneExactURL/oneExactURL.oneExactURL");
        urlPatterns.add("/onePathURL/*");
        return urlPatterns;
    }

    private List<String> createTestHttpMethods() {
        List<String> httpMethods = new ArrayList<String>();
        httpMethods.add("GET");
        httpMethods.add("PUT");
        return httpMethods;
    }

    class SecurityServletConfiguratorHelperTestDouble extends SecurityServletConfiguratorHelper {
        public SecurityServletConfiguratorHelperTestDouble(ServletConfigurator configurator) {
            super(configurator);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.SecurityServletConfiguratorHelper#configureFromWebApp(com.ibm.ws.javaee.dd.web.WebApp)}.
     */
    @Test
    public void testConfigureFromWebApp() {
        WebApp webApp = createWebAppMockNoLoginConfig();
        mockery.checking(new Expectations() {
            {
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                will(returnValue(null));

            }
        });
        configHelper.configureFromWebApp(webApp);
        assertEquals("The configHelper should return the correct list of roles after processing the web.xml", WEBAPP_ROLES, configHelper.getRoles());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.SecurityServletConfiguratorHelper#configureFromWebFragment(com.ibm.ws.javaee.dd.web.WebFragment)}.
     */
    @Test
    public void testConfigureFromWebFragment() {
        WebFragment webFragment = createWebFragmentMock();
        WebFragmentInfo webFragmentItem = createWebFragmentItemMock(webFragment);

        mockery.checking(new Expectations() {
            {
                one(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                will(returnValue(null));
                one(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.RUN_AS_KEY);
                will(returnValue(emptyRunAsConfigMap));
                one(configurator).createConfigItem(WEBFRAGMENT_RUNAS_ROLE);

            }
        });
        configHelper.configureFromWebFragment(webFragmentItem);
        assertEquals("The configHelper should return the list of roles from the web-fragment.xml: " + WEBFRAGMENT_ROLES, WEBFRAGMENT_ROLES, configHelper.getRoles());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.SecurityServletConfiguratorHelper#testMpJwt}.
     */
    @Test
    public void testMpJwt() throws Exception {
        final String authMethod = "bogusAuthMethod";
        mockery.checking(new Expectations() {
            {
                com.ibm.wsspi.anno.info.InfoStore infoStore = mockery.mock(com.ibm.wsspi.anno.info.InfoStore.class);
                com.ibm.wsspi.anno.info.ClassInfo classInfo = mockery.mock(com.ibm.wsspi.anno.info.ClassInfo.class);
                com.ibm.wsspi.anno.info.AnnotationInfo annoInfo = mockery.mock(com.ibm.wsspi.anno.info.AnnotationInfo.class);
                com.ibm.wsspi.anno.info.AnnotationValue auth = mockery.mock(com.ibm.wsspi.anno.info.AnnotationValue.class);
                com.ibm.ws.container.service.annotations.WebAnnotations webAnnotations = mockery.mock(com.ibm.ws.container.service.annotations.WebAnnotations.class);
                com.ibm.wsspi.anno.targets.AnnotationTargets_Targets targets = mockery.mock(com.ibm.wsspi.anno.targets.AnnotationTargets_Targets.class);

                String annoName = "org.eclipse.microprofile.auth.LoginConfig";

                allowing(configurator).getWebAnnotations();
                will(returnValue(webAnnotations));

                one(webAnnotations).getAnnotationTargets();
                will(returnValue(targets));

                HashSet hs = new HashSet();
                hs.add("FooClass");
                one(targets).getAnnotatedClasses(annoName);
                will(returnValue(hs));

                one(webAnnotations).getInfoStore();
                will(returnValue(infoStore));

                one(infoStore).getDelayableClassInfo("FooClass");
                will(returnValue(classInfo));
                one(classInfo).getSuperclassName();
                will(returnValue("javax.ws.rs.core.Application"));
                one(classInfo).getAnnotation(annoName);
                will(returnValue(annoInfo));
                one(annoInfo).getValue("authMethod");
                will(returnValue(auth));
                one(annoInfo).getValue("realmName");
                will(returnValue(auth));
                allowing(auth).getStringValue();
                will(returnValue(authMethod));

                one(configurator).getWebAnnotations().getInfoStore();
                will(returnValue(infoStore));
            }
        });
        configHelper.configureMpJwt(false);
        assertNotNull("loginconfig should not be null", configHelper.getLoginConfiguration());
        String result = configHelper.getLoginConfiguration().getAuthenticationMethod();

        assertTrue("The Login Auth Method should have been updated to " + authMethod + " but was " + result,
                   result.equals("bogusAuthMethod"));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.SecurityServletConfiguratorHelper#configureFromWebFragment(com.ibm.ws.javaee.dd.web.WebFragment)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConfigureFromWebAppAndWebFragment() {
        WebApp webApp = createWebAppMockWithLoginConfigAndRunAs();
        WebFragment webFragment = createWebFragmentMockWithLoginConfig();
        WebFragmentInfo webFragmentItem = createWebFragmentItemMock(webFragment);

        final ConfigItem<FormLoginConfig> loginConfigItem = mockery.mock(ServletConfigurator.ConfigItem.class, "FormLoginConfigItem");
        final Map<String, ConfigItem<FormLoginConfig>> formLoginConfigMapAfterWebAppProcessing = new HashMap<String, ServletConfigurator.ConfigItem<FormLoginConfig>>();
        formLoginConfigMapAfterWebAppProcessing.put(SecurityServletConfiguratorHelper.LOGIN_CONFIG_KEY, loginConfigItem);

        final Map<String, ConfigItem<String>> runAsConfigMapAfterWebAppProcessing = new HashMap<String, ServletConfigurator.ConfigItem<String>>();
        final ConfigItem<String> configItem = mockery.mock(ServletConfigurator.ConfigItem.class, "RunAsConfigItem");
        runAsConfigMapAfterWebAppProcessing.put(SERVLET_NAME, configItem);

        mockery.checking(new Expectations() {
            {
                //for web.xml processing
                one(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                will(returnValue(emptyFormLoginConfigMap));
                one(configurator).createConfigItem(formLoginConfigFromMock);
                will(returnValue(loginConfigItem));
                one(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.RUN_AS_KEY);
                will(returnValue(emptyRunAsConfigMap));
                one(configurator).createConfigItem(WEBAPP_RUNAS_ROLE);
                will(returnValue(configItem));
                allowing(configurator).createConfigItem(WEBAPP_AUTH_METHOD);
                will(returnValue(configItem));
                allowing(configurator).createConfigItem(WEBAPP_REALM_NAME);
                will(returnValue(configItem));

                //for web-fragment.xml processing
                one(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.RUN_AS_KEY);
                will(returnValue(runAsConfigMapAfterWebAppProcessing));
                one(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                will(returnValue(formLoginConfigMapAfterWebAppProcessing));
                one(configurator).validateDuplicateKeyValueConfiguration(SecurityServletConfiguratorHelper.SERVLET_KEY, SecurityServletConfiguratorHelper.SERVLET_NAME_KEY,
                                                                         SERVLET_NAME, SecurityServletConfiguratorHelper.RUN_AS_KEY,
                                                                         WEBFRAGMENT_RUNAS_ROLE, configItem);
            }
        });

        configHelper.configureFromWebApp(webApp);
        configHelper.configureFromWebFragment(webFragmentItem);

        List<String> expectedSecurityRoles = new ArrayList<String>();
        expectedSecurityRoles.add(WEBAPP_ROLE);
        expectedSecurityRoles.add(WEBFRAGMENT_ROLE);

        assertEquals("The getRoles() should return roles defined in both web.xml and web-fragment.xml: " + expectedSecurityRoles, expectedSecurityRoles,
                     configHelper.getRoles());
        assertEquals("The runAs role should be set only to the web.xml runas role: " + WEBAPP_RUNAS_ROLE, WEBAPP_RUNAS_ROLE, configHelper.getRunAsRoleForServlet(SERVLET_NAME));
        assertEquals("The authentication method should be set only to the web.xml auth method: " + WEBAPP_AUTH_METHOD, WEBAPP_AUTH_METHOD,
                     configHelper.getLoginConfiguration().getAuthenticationMethod());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.SecurityServletConfiguratorHelper#configureFromWebFragment(com.ibm.ws.javaee.dd.web.WebFragment)}.
     */
    @Test
    public void testConfigureFromWebAppAndWebFragment_ignoreWebFragmentRole() {
        WebApp webApp = createWebAppMockNoLoginConfig();
        WebFragment webFragment = createWebFragmentMockWithConflict();
        WebFragmentInfo webFragmentItem = createWebFragmentItemMock(webFragment);

        final Map<String, ConfigItem<String>> runAsConfigMapAfterWebAppProcessing = new HashMap<String, ServletConfigurator.ConfigItem<String>>();
        final ConfigItem<String> configItem = mockery.mock(ServletConfigurator.ConfigItem.class, "RunAsConfigItem");
        runAsConfigMapAfterWebAppProcessing.put(SERVLET_NAME, configItem);

        mockery.checking(new Expectations() {
            {
                //validate constraint in web fragment is ignored
                one(configurator).validateDuplicateConfiguration(SecurityServletConfiguratorHelper.SECURITY_CONSTRAINT_KEY,
                                                                 SecurityServletConfiguratorHelper.AUTH_CONSTRAINT_KEY, WEBFRAGMENT_ROLES, rolesConfigItemFromWebApp);
                one(configurator).validateDuplicateConfiguration(SecurityServletConfiguratorHelper.SECURITY_CONSTRAINT_KEY,
                                                                 SecurityServletConfiguratorHelper.USER_DATA_CONSTRAINT_KEY, String.valueOf(2),
                                                                 transportGuaranteeConfigItemFromWebApp);

                //for web-fragment.xml processing
                one(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.RUN_AS_KEY);
                will(returnValue(runAsConfigMapAfterWebAppProcessing));

                one(configurator).validateDuplicateKeyValueConfiguration(SecurityServletConfiguratorHelper.SERVLET_KEY, SecurityServletConfiguratorHelper.SERVLET_NAME_KEY,
                                                                         SERVLET_NAME, SecurityServletConfiguratorHelper.RUN_AS_KEY,
                                                                         WEBFRAGMENT_RUNAS_ROLE, configItem);

                exactly(2).of(configurator).getConfigSource();
                will(returnValue(ConfigSource.WEB_FRAGMENT));

                one(rolesConfigItemFromWebApp).getSource();
                will(returnValue(ConfigSource.WEB_XML));

                one(transportGuaranteeConfigItemFromWebApp).getSource();
                will(returnValue(ConfigSource.WEB_XML));
            }
        });

        configHelper.configureFromWebApp(webApp);
        configHelper.configureFromWebFragment(webFragmentItem);

        List<SecurityConstraint> securityConstraints = configHelper.getSecurityConstraintCollection().getSecurityConstraints();
        List<String> actualRoles = securityConstraints.get(1).getRoles();
        boolean actualSSLRequired = securityConstraints.get(1).isSSLRequired();
        assertEquals("The roles of the web-fragment auth-constraint should be ignored, so getRoles() should return an empty list: " + EMPTY_LIST, EMPTY_LIST,
                     actualRoles);
        assertEquals("The transport guarantee of the web-fragment user-data-constraint should be ignored, so isSSLRequired should return false: " + false, false,
                     actualSSLRequired);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.SecurityServletConfiguratorHelper#configureFromAnnotations(WebFragmentInfo)}.
     *
     * @throws UnableToAdaptException
     */
    @Test
    public void testConfigureFromAnnotations() throws UnableToAdaptException {
        WebFragment webFragment = createWebFragmentMock();
        WebFragmentInfo webFragmentItem = createWebFragmentItemMock(webFragment);
        final WebAnnotations webAnnotations = createWebAnnotationMock(webFragmentItem);

        mockery.checking(new Expectations() {
            {
                one(configurator).getWebAnnotations();
                will(returnValue(webAnnotations));
            }
        });
        configHelper.configureFromAnnotations(webFragmentItem);
        assertEquals("The configHelper should return the correct list of roles after processing the static annotations", STATICANNOTATION_ROLES, configHelper.getRoles());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.SecurityServletConfiguratorHelper#configureFromAnnotations(WebFragmentInfo)}.
     *
     * @throws UnableToAdaptException
     */
    @Test
    public void testConfigureFromAnnotations_noRoles() throws UnableToAdaptException {
        WebFragment webFragment = createWebFragmentMock();
        WebFragmentInfo webFragmentItem = createWebFragmentItemMock(webFragment);
        final WebAnnotations webAnnotations = createNullWebAnnotationMock(webFragmentItem);

        mockery.checking(new Expectations() {
            {
                one(configurator).getWebAnnotations();
                will(returnValue(webAnnotations));
            }
        });
        configHelper.configureFromAnnotations(webFragmentItem);
        assertEquals("The configHelper should return an empty list after processing the static annotations where no roles are declared", EMPTY_LIST, configHelper.getRoles());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.SecurityServletConfiguratorHelper#getSecurityConstraintCollection()}.
     */
    @Test
    public void testGetSecurityConstraintCollection() {
        WebApp webApp = createWebAppMockNoLoginConfig();
        mockery.checking(new Expectations() {
            {
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                will(returnValue(null));

            }
        });
        configHelper.configureFromWebApp(webApp);
        SecurityConstraintCollection securityConstraintCollection = configHelper.getSecurityConstraintCollection();
        assertNotNull("There must be a security constraint collection.", securityConstraintCollection);
    }

    @Test
    public void testGetSecurityConstraintCollectionInitialized() {
        final String methodName = "testGetSecurityConstraintCollectionInitialized";
        try {
            WebApp webApp = createWebAppMockNoLoginConfig();
            mockery.checking(new Expectations() {
                {
                    allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                    will(returnValue(null));

                }
            });
            configHelper.configureFromWebApp(webApp);
            SecurityConstraintCollection securityConstraintCollection = configHelper.getSecurityConstraintCollection();
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(ONE_EXACT_URL, "GET");
            assertNotNull("There must be a match response.", matchResponse);
            assertEquals("There must be roles.", WEBAPP_ROLES, matchResponse.getRoles());
            assertTrue("SSL must be required.", matchResponse.isSSLRequired());
            assertFalse("The access must not be precluded.", matchResponse.isAccessPrecluded());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    public void testGetSecurityConstraintCollectionInitializedPrecluded() {
        final String methodName = "testGetSecurityConstraintCollectionInitializedPrecluded";
        try {
            WebApp webApp = createWebAppMockPrecluded();
            mockery.checking(new Expectations() {
                {
                    allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                    will(returnValue(null));

                }
            });
            configHelper.configureFromWebApp(webApp);
            SecurityConstraintCollection securityConstraintCollection = configHelper.getSecurityConstraintCollection();
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(ONE_EXACT_URL, "GET");
            assertTrue("The access must be precluded.", matchResponse.isAccessPrecluded());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.SecurityServletConfiguratorHelper#getLoginConfiguration()}.
     */
    @Test
    public void testGetLoginConfigurationDefault() {
        final String methodName = "testGetLoginConfigurationDefault";
        try {
            WebApp webApp = createWebAppMockNoLoginConfig();
            mockery.checking(new Expectations() {
                {
                    allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                    will(returnValue(null));
                    allowing(configurator).getFromModuleCache(WebModuleMetaData.class);
                    will(returnValue(wmmd));
                    allowing(wmmd).setSecurityMetaData(configHelper);
                }
            });
            configHelper.configureFromWebApp(webApp);
            configHelper.configureDefaults();
            configHelper.finish();
            LoginConfiguration loginConfiguration = configHelper.getLoginConfiguration();
            //when there is no login config defined, we return a default basicAuth configuration
            assertEquals("There must be a default login configuration", LoginConfiguration.BASIC, loginConfiguration.getAuthenticationMethod());
            assertNull("There must be a default login configuration.", loginConfiguration.getFormLoginConfiguration());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetLoginConfigurationFromWebAppContainingLoginConfig() {
        final String methodName = "testGetLoginConfigurationFromWebAppContainingLoginConfig";
        try {
            WebApp webApp = createWebAppMockWithFullLoginConfig();

            mockery.checking(new Expectations() {
                {
                    allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                    will(returnValue(emptyFormLoginConfigMap));
                    allowing(configurator).createConfigItem(formLoginConfigFromMock);

                }
            });
            configHelper.configureFromWebApp(webApp);
            LoginConfiguration loginConfiguration = configHelper.getLoginConfiguration();
            assertNotNull("There must be a login configuration.", loginConfiguration);
            assertEquals("The authentication method must be set.", WEBAPP_AUTH_METHOD, loginConfiguration.getAuthenticationMethod());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Validate that we can properly handle a WebArchive that has a incomplete
     * or invalid security role ref elements.
     */
    @Test
    public void constructorInvalidSecurityRoleRefs() {
        final WebApp webApp = mockery.mock(WebApp.class);

        final List<SecurityRole> securityRoles = new ArrayList<SecurityRole>();
        final SecurityRole securityRole = mockery.mock(SecurityRole.class);
        securityRoles.add(securityRole);

        final List<Servlet> servlets = new ArrayList<Servlet>();
        final Servlet servlet = mockery.mock(Servlet.class);
        servlets.add(servlet);

        final List<SecurityRoleRef> securityRoleRefs = new ArrayList<SecurityRoleRef>();
        final SecurityRoleRef complete = mockery.mock(SecurityRoleRef.class, "completeRoleRef");
        final SecurityRoleRef incomplete = mockery.mock(SecurityRoleRef.class, "incompleteRoleRef");
        final SecurityRoleRef unresolved = mockery.mock(SecurityRoleRef.class, "unresolvedRoleRef");
        securityRoleRefs.add(complete);
        securityRoleRefs.add(incomplete);
        securityRoleRefs.add(unresolved);

        final List<EnvEntry> envEntries = new ArrayList<EnvEntry>();

        mockery.checking(new Expectations() {
            {
                allowing(securityRole).getRoleName();
                will(returnValue("SecurityRole"));

                allowing(servlet).getServletName();
                will(returnValue("ServletName"));
                allowing(servlet).getSecurityRoleRefs();
                will(returnValue(securityRoleRefs));
                allowing(servlet).getRunAs();
                will(returnValue(null));

                allowing(complete).getName();
                will(returnValue("CompleteRef"));
                allowing(complete).getLink();
                will(returnValue("SecurityRole"));

                allowing(incomplete).getName();
                will(returnValue("IncompleteRef"));
                allowing(incomplete).getLink();
                will(returnValue(null));

                allowing(unresolved).getName();
                will(returnValue("UnresolvedRef"));
                allowing(unresolved).getLink();
                will(returnValue("DoesntExist"));

                allowing(webApp).getSecurityRoles();
                will(returnValue(securityRoles));
                allowing(webApp).getSecurityConstraints();
                allowing(webApp).getLoginConfig();
                will(returnValue(null));
                allowing(webApp).getServlets();
                will(returnValue(servlets));
                allowing(webApp).getServletMappings();
                allowing(webApp).getEnvEntries();
                will(returnValue(envEntries));
                allowing(webApp).isSetDenyUncoveredHttpMethods();
            }
        });
        mockery.checking(new Expectations() {
            {
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                will(returnValue(null));
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.RUN_AS_KEY);
                will(returnValue(emptyRunAsConfigMap));
                allowing(configurator).createConfigItem(null);
            }
        });
        configHelper.configureFromWebApp(webApp);

        assertTrue(
                   "Expected CWWKS9100W",
                   outputMgr.checkForStandardOut("CWWKS9100W: In servlet ServletName, <security-role-ref> element for <role-name>IncompleteRef</role-name> is missing corresponding <role-link> element."));
        assertTrue(
                   "Expected CWWKS9101W",
                   outputMgr.checkForStandardOut("CWWKS9101W: In servlet ServletName, <role-link>DoesntExist</role-link> for <role-name>UnresolvedRef</role-name> is not a defined <security-role>."));
    }

    /**
     * Validate that we can properly handle a WebArchive that has duplicate
     * URL pattern mappings in the servlet mapping.
     */
    @Test
    public void constructorDuplicateServletMapping() {
        final WebApp webApp = mockery.mock(WebApp.class);
        final List<ServletMapping> servletMappings = new ArrayList<ServletMapping>();
        final ServletMapping dup1 = mockery.mock(ServletMapping.class, "ServletMappingDup1");
        final ServletMapping dup2 = mockery.mock(ServletMapping.class, "ServletMappingDup2");
        servletMappings.add(dup1);
        servletMappings.add(dup2);
        final List<EnvEntry> envEntries = new ArrayList<EnvEntry>();
        mockery.checking(new Expectations() {
            {
                allowing(dup1).getServletName();
                will(returnValue("FirstServletName"));
                allowing(dup1).getURLPatterns();
                will(returnValue(urlPatterns));
                allowing(dup2).getServletName();
                will(returnValue("SecondServletName"));
                allowing(dup2).getURLPatterns();
                will(returnValue(urlPatterns));

                allowing(webApp).getSecurityRoles();
                allowing(webApp).getSecurityConstraints();
                allowing(webApp).getLoginConfig();
                will(returnValue(null));
                allowing(webApp).getServlets();
                allowing(webApp).getServletMappings();
                will(returnValue(servletMappings));
                allowing(webApp).getEnvEntries();
                will(returnValue(envEntries));
                allowing(webApp).isSetDenyUncoveredHttpMethods();
            }
        });
        mockery.checking(new Expectations() {
            {
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                will(returnValue(null));

            }
        });
        configHelper.configureFromWebApp(webApp);

        //test the expectations were met
        mockery.assertIsSatisfied();
    }

    /**
     * getSecurityRoleReferenced does not support a null URI.
     */
    public void getSecurityRoleReferenced_nullUriIsNotSupported() {
        final WebApp webApp = createMockWebAppWithSecurityRoleRef();

        mockery.checking(new Expectations() {
            {
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                will(returnValue(null));
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.RUN_AS_KEY);
                will(returnValue(emptyRunAsConfigMap));
                allowing(configurator).createConfigItem(null);
            }
        });
        configHelper.configureFromWebApp(webApp);
        String role = configHelper.getSecurityRoleReferenced(null, "RoleName");
        Assert.assertNull("Null servlet -> null role", role);
    }

    /**
     * getSecurityRoleReferenced will return null if the roleName specified was null.
     */
    @Test
    public void getSecurityRoleReferenced_nullRoleName() {
        final WebApp webApp = createMockWebAppWithSecurityRoleRef();

        mockery.checking(new Expectations() {
            {
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                will(returnValue(null));
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.RUN_AS_KEY);
                will(returnValue(emptyRunAsConfigMap));
                allowing(configurator).createConfigItem(null);
            }
        });
        configHelper.configureFromWebApp(webApp);
        assertNull("Return null if the roleName specified was null",
                   configHelper.getSecurityRoleReferenced("uri", null));
    }

    /**
     * If the URI can not be matched, return null as we can not determine which
     * servlet this is, so we can't check the role-refs.
     */
    @Test
    public void getSecurityRoleReferenced_unmatchedUri() {
        final WebApp webApp = createMockWebAppWithSecurityRoleRef();

        mockery.checking(new Expectations() {
            {
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                will(returnValue(null));
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.RUN_AS_KEY);
                will(returnValue(emptyRunAsConfigMap));
                allowing(configurator).createConfigItem(null);
            }
        });
        configHelper.configureFromWebApp(webApp);
        assertEquals("When an unmatched URI is specified, check the global roles",
                     "SecurityRole", configHelper.getSecurityRoleReferenced("/iDontMatch/", "SecurityRole"));
    }

    /**
     * If the role ref name is provided, return the real name.
     */
    @Test
    public void getSecurityRoleReferenced_roleRef() {
        final WebApp webApp = createMockWebAppWithSecurityRoleRef();

        mockery.checking(new Expectations() {
            {
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                will(returnValue(null));
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.RUN_AS_KEY);
                will(returnValue(emptyRunAsConfigMap));
                allowing(configurator).createConfigItem(null);
            }
        });
        configHelper.configureFromWebApp(webApp);
        assertEquals("A ref role name should be mapped back to the real name",
                     "SecurityRole", configHelper.getSecurityRoleReferenced("ServletName", "RoleRef"));
    }

    /**
     * If the real role name name is provided, return the real name.
     */
    @Test
    public void getSecurityRoleReferenced_realRole() {
        final WebApp webApp = createMockWebAppWithSecurityRoleRef();

        mockery.checking(new Expectations() {
            {
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                will(returnValue(null));
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.RUN_AS_KEY);
                will(returnValue(emptyRunAsConfigMap));
                allowing(configurator).createConfigItem(null);
            }
        });
        configHelper.configureFromWebApp(webApp);
        assertEquals("When the role name specified is a real role name, return it",
                     "SecurityRole", configHelper.getSecurityRoleReferenced("/onePathURL/matchMe", "SecurityRole"));
    }

    /**
     * If the real role name name is provided, return the real name.
     */
    @Test
    public void getSecurityRoleReferenced_realRoleNoRefs() {
        final WebApp webApp = createMockWebAppWithNoSecurityRoleRef();

        mockery.checking(new Expectations() {
            {
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                will(returnValue(null));
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.RUN_AS_KEY);
                will(returnValue(emptyRunAsConfigMap));
                allowing(configurator).createConfigItem(null);
            }
        });
        configHelper.configureFromWebApp(webApp);
        assertEquals("When the role name specified is a real role name, return it",
                     "SecurityRole", configHelper.getSecurityRoleReferenced("/onePathURL/matchMe", "SecurityRole"));
    }

    /**
     * If the real role name name is provided, return the real name.
     */
    @Test
    public void getSecurityRoleReferenced_nonExistentRoleName() {
        final WebApp webApp = createMockWebAppWithSecurityRoleRef();

        mockery.checking(new Expectations() {
            {
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                will(returnValue(null));
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.RUN_AS_KEY);
                will(returnValue(emptyRunAsConfigMap));
                allowing(configurator).createConfigItem(null);
            }
        });
        configHelper.configureFromWebApp(webApp);
        assertNull("A non-existing role name should result in null",
                   configHelper.getSecurityRoleReferenced("/onePathURL/matchMe", "IDontExist"));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.SecurityServletConfiguratorHelper#getRunAsRoleForServlet(java.lang.String)}.
     */
    @Test
    public void testGetRunAsRoleForServlet() {
        final WebApp webApp = createMockWebAppWithRunAs();
        mockery.checking(new Expectations() {
            {
                allowing(webApp).getLoginConfig();
                will(returnValue(null));
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                will(returnValue(null));
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.RUN_AS_KEY);
                will(returnValue(emptyRunAsConfigMap));
                allowing(configurator).createConfigItem(WEBAPP_RUNAS_ROLE);
            }
        });
        configHelper.configureFromWebApp(webApp);
        assertEquals("The runAs role should be set to " + WEBAPP_RUNAS_ROLE, WEBAPP_RUNAS_ROLE, configHelper.getRunAsRoleForServlet(SERVLET_NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.SecurityServletConfiguratorHelper#getRunAsMap()}.
     */
    @Test
    public void testGetRunAsMap() {
        final WebApp webApp = createMockWebAppWithRunAs();
        mockery.checking(new Expectations() {
            {
                allowing(webApp).getLoginConfig();
                will(returnValue(null));
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.FORM_LOGIN_CONFIG_KEY);
                will(returnValue(null));
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.RUN_AS_KEY);
                will(returnValue(emptyRunAsConfigMap));
                allowing(configurator).createConfigItem(WEBAPP_RUNAS_ROLE);
            }
        });
        configHelper.configureFromWebApp(webApp);
        assertEquals("The runAs map should be set to " + RUNAS_MAP, RUNAS_MAP, configHelper.getRunAsMap());
    }

    /**
     * Create a WebArchive which has all the necessary mocks for getSecurityRoleReferenced
     *
     * @return
     */
    private WebApp createMockWebAppWithSecurityRoleRef() {
        final WebApp webArchive = mockery.mock(WebApp.class);

        final List<SecurityRole> securityRoles = new ArrayList<SecurityRole>();
        final SecurityRole securityRole = mockery.mock(SecurityRole.class);
        securityRoles.add(securityRole);

        final List<Servlet> servlets = new ArrayList<Servlet>();
        final Servlet servlet = mockery.mock(Servlet.class);
        servlets.add(servlet);

        final List<SecurityRoleRef> securityRoleRefs = new ArrayList<SecurityRoleRef>();
        final SecurityRoleRef roleRef = mockery.mock(SecurityRoleRef.class, "RoleRef");
        securityRoleRefs.add(roleRef);

        final List<ServletMapping> servletMappings = new ArrayList<ServletMapping>();
        final ServletMapping mapping = mockery.mock(ServletMapping.class, "ServletMapping");
        servletMappings.add(mapping);

        final List<EnvEntry> envEntries = new ArrayList<EnvEntry>();

        mockery.checking(new Expectations() {
            {
                allowing(securityRole).getRoleName();
                will(returnValue("SecurityRole"));

                allowing(servlet).getServletName();
                will(returnValue("ServletName"));
                allowing(servlet).getSecurityRoleRefs();
                will(returnValue(securityRoleRefs));
                allowing(servlet).getRunAs();
                will(returnValue(null));

                allowing(roleRef).getName();
                will(returnValue("RoleRef"));
                allowing(roleRef).getLink();
                will(returnValue("SecurityRole"));

                allowing(mapping).getServletName();
                will(returnValue("ServletName"));
                allowing(mapping).getURLPatterns();
                will(returnValue(urlPatterns));

                allowing(webArchive).getSecurityRoles();
                will(returnValue(securityRoles));
                allowing(webArchive).getSecurityConstraints();
                allowing(webArchive).getLoginConfig();
                will(returnValue(null));
                allowing(webArchive).getServlets();
                will(returnValue(servlets));
                allowing(webArchive).getServletMappings();
                will(returnValue(servletMappings));
                allowing(webArchive).getEnvEntries();
                will(returnValue(envEntries));
                allowing(webArchive).isSetDenyUncoveredHttpMethods();
            }
        });
        return webArchive;
    }

    /**
     * Create a WebArchive which has all the necessary mocks for getSecurityRoleReferenced
     *
     * @return
     */
    private WebApp createMockWebAppWithNoSecurityRoleRef() {
        final WebApp webArchive = mockery.mock(WebApp.class);

        final List<SecurityRole> securityRoles = new ArrayList<SecurityRole>();
        final SecurityRole securityRole = mockery.mock(SecurityRole.class);
        securityRoles.add(securityRole);

        final List<Servlet> servlets = new ArrayList<Servlet>();
        final Servlet servlet = mockery.mock(Servlet.class);
        servlets.add(servlet);

        final List<SecurityRoleRef> securityRoleRefs = new ArrayList<SecurityRoleRef>();

        final List<ServletMapping> servletMappings = new ArrayList<ServletMapping>();
        final ServletMapping mapping = mockery.mock(ServletMapping.class, "ServletMapping");
        servletMappings.add(mapping);

        final List<EnvEntry> envEntries = new ArrayList<EnvEntry>();

        mockery.checking(new Expectations() {
            {
                allowing(securityRole).getRoleName();
                will(returnValue("SecurityRole"));

                allowing(servlet).getServletName();
                will(returnValue("ServletName"));
                allowing(servlet).getSecurityRoleRefs();
                will(returnValue(securityRoleRefs));
                allowing(servlet).getRunAs();
                will(returnValue(null));

                allowing(mapping).getServletName();
                will(returnValue("ServletName"));
                allowing(mapping).getURLPatterns();
                will(returnValue(urlPatterns));

                allowing(webArchive).getSecurityRoles();
                will(returnValue(securityRoles));
                allowing(webArchive).getSecurityConstraints();
                allowing(webArchive).getLoginConfig();
                will(returnValue(null));
                allowing(webArchive).getServlets();
                will(returnValue(servlets));
                allowing(webArchive).getServletMappings();
                will(returnValue(servletMappings));
                allowing(webArchive).getEnvEntries();
                will(returnValue(envEntries));
                allowing(webArchive).isSetDenyUncoveredHttpMethods();
            }
        });
        return webArchive;
    }

    private WebApp createWebAppMockNoLoginConfig() {
        final WebApp webArchiveMock = createWebAppMock();
        mockery.checking(new Expectations() {
            {
                allowing(webArchiveMock).getLoginConfig();
                will(returnValue(null));
            }
        });
        return webArchiveMock;
    }

    private WebApp createWebAppMock() {
        final List<com.ibm.ws.javaee.dd.web.common.SecurityConstraint> archiveSecurityConstraints = createSecurityConstraintsForWebApp();
        final List<SecurityRole> securityRoles = createSecurityRolesForWebAppMock();
        final List<EnvEntry> envEntries = new ArrayList<EnvEntry>();
        final WebApp webArchiveMock = mockery.mock(WebApp.class);
        mockery.checking(new Expectations() {
            {
                allowing(webArchiveMock).getSecurityConstraints();
                will(returnValue(archiveSecurityConstraints));
                allowing(webArchiveMock).getSecurityRoles();
                will(returnValue(securityRoles));
                allowing(webArchiveMock).getServlets();
                allowing(webArchiveMock).getServletMappings();
                allowing(webArchiveMock).getEnvEntries();
                will(returnValue(envEntries));
                allowing(webArchiveMock).isSetDenyUncoveredHttpMethods();
            }
        });
        return webArchiveMock;
    }

    /**
     * Create a WebArchive which has all the necessary mocks for runAs test
     *
     * @return
     */
    private WebApp createMockWebAppWithRunAs() {
        final WebApp webArchive = mockery.mock(WebApp.class);

        final List<SecurityRole> securityRoles = new ArrayList<SecurityRole>();
        final SecurityRole securityRole = mockery.mock(SecurityRole.class);
        securityRoles.add(securityRole);

        final List<Servlet> servlets = new ArrayList<Servlet>();
        final Servlet servlet = mockery.mock(Servlet.class);
        servlets.add(servlet);

        final List<ServletMapping> servletMappings = new ArrayList<ServletMapping>();
        final ServletMapping mapping = mockery.mock(ServletMapping.class, "ServletMapping");
        servletMappings.add(mapping);
        final List<SecurityRoleRef> securityRoleRefs = new ArrayList<SecurityRoleRef>();
        final List<EnvEntry> envEntries = new ArrayList<EnvEntry>();

        final RunAs runAs = mockery.mock(RunAs.class, "WebAppRunAs");

        mockery.checking(new Expectations() {
            {
                allowing(securityRole).getRoleName();
                will(returnValue(WEBAPP_ROLE));

                allowing(servlet).getServletName();
                will(returnValue(SERVLET_NAME));
                allowing(servlet).getSecurityRoleRefs();
                will(returnValue(securityRoleRefs));
                allowing(servlet).getRunAs();
                will(returnValue(runAs));
                allowing(runAs).getRoleName();
                will(returnValue(WEBAPP_RUNAS_ROLE));

                allowing(mapping).getServletName();
                will(returnValue("ServletName"));
                allowing(mapping).getURLPatterns();
                will(returnValue(urlPatterns));

                allowing(webArchive).getSecurityRoles();
                will(returnValue(securityRoles));
                allowing(webArchive).getSecurityConstraints();
                allowing(webArchive).getServlets();
                will(returnValue(servlets));
                allowing(webArchive).getServletMappings();
                will(returnValue(servletMappings));
                allowing(webArchive).getEnvEntries();
                will(returnValue(envEntries));
                allowing(webArchive).isSetDenyUncoveredHttpMethods();
            }
        });
        return webArchive;
    }

    private List<com.ibm.ws.javaee.dd.web.common.SecurityConstraint> createSecurityConstraintsForWebApp() {
        final List<com.ibm.ws.javaee.dd.web.common.WebResourceCollection> archiveWebResourceCollections = createWebResourceCollectionsForWebApp();
        final AuthConstraint authConstraint = createAuthConstraintForWebApp();
        final com.ibm.ws.javaee.dd.web.common.SecurityConstraint archiveSecurityConstraintMock = mockery.mock(com.ibm.ws.javaee.dd.web.common.SecurityConstraint.class,
                                                                                                              "SecurityConstraintsForWebApp");
        final UserDataConstraint userDataConstraint = createUserDataConstraintForWebApp();
        mockery.checking(new Expectations() {
            {
                allowing(archiveSecurityConstraintMock).getWebResourceCollections();
                will(returnValue(archiveWebResourceCollections));
                allowing(archiveSecurityConstraintMock).getAuthConstraint();
                will(returnValue(authConstraint));
                allowing(archiveSecurityConstraintMock).getUserDataConstraint();
                will(returnValue(userDataConstraint));
            }
        });
        List<com.ibm.ws.javaee.dd.web.common.SecurityConstraint> archiveSecurityConstraints = new ArrayList<com.ibm.ws.javaee.dd.web.common.SecurityConstraint>();
        archiveSecurityConstraints.add(archiveSecurityConstraintMock);
        return archiveSecurityConstraints;
    }

    private List<com.ibm.ws.javaee.dd.web.common.SecurityConstraint> createSecurityConstraintsForWebFragment() {
        final List<com.ibm.ws.javaee.dd.web.common.WebResourceCollection> archiveWebResourceCollections = createWebResourceCollectionsForWebFragment();
        final com.ibm.ws.javaee.dd.web.common.SecurityConstraint archiveSecurityConstraintMock = createCommonExpectationsForSecurityConstraintsForWebFragment(archiveWebResourceCollections);
        mockery.checking(new Expectations() {
            {
                allowing(archiveSecurityConstraintMock).getWebResourceCollections();
                will(returnValue(archiveWebResourceCollections));
            }
        });
        List<com.ibm.ws.javaee.dd.web.common.SecurityConstraint> archiveSecurityConstraints = new ArrayList<com.ibm.ws.javaee.dd.web.common.SecurityConstraint>();
        archiveSecurityConstraints.add(archiveSecurityConstraintMock);
        return archiveSecurityConstraints;
    }

    private List<com.ibm.ws.javaee.dd.web.common.SecurityConstraint> createSecurityConstraintsWithConflictForWebFragment() {
        final List<com.ibm.ws.javaee.dd.web.common.WebResourceCollection> archiveWebResourceCollections = createWebResourceCollectionsWithConflictForWebFragment();
        final com.ibm.ws.javaee.dd.web.common.SecurityConstraint archiveSecurityConstraintMock = createCommonExpectationsForSecurityConstraintsForWebFragment(archiveWebResourceCollections);
        mockery.checking(new Expectations() {
            {
                allowing(archiveSecurityConstraintMock).getWebResourceCollections();
                will(returnValue(archiveWebResourceCollections));
            }
        });
        List<com.ibm.ws.javaee.dd.web.common.SecurityConstraint> archiveSecurityConstraints = new ArrayList<com.ibm.ws.javaee.dd.web.common.SecurityConstraint>();
        archiveSecurityConstraints.add(archiveSecurityConstraintMock);
        return archiveSecurityConstraints;
    }

    private com.ibm.ws.javaee.dd.web.common.SecurityConstraint createCommonExpectationsForSecurityConstraintsForWebFragment(final List<com.ibm.ws.javaee.dd.web.common.WebResourceCollection> archiveWebResourceCollections) {
        final AuthConstraint authConstraint = createAuthConstraintForWebFragment();
        final com.ibm.ws.javaee.dd.web.common.SecurityConstraint archiveSecurityConstraintMock = mockery.mock(com.ibm.ws.javaee.dd.web.common.SecurityConstraint.class,
                                                                                                              "SecurityConstraintsForWebFragment");
        final UserDataConstraint userDataConstraint = createUserDataConstraintForWebFragment();
        mockery.checking(new Expectations() {
            {
                allowing(archiveSecurityConstraintMock).getAuthConstraint();
                will(returnValue(authConstraint));
                allowing(archiveSecurityConstraintMock).getUserDataConstraint();
                will(returnValue(userDataConstraint));
            }
        });

        return archiveSecurityConstraintMock;
    }

    private List<com.ibm.ws.javaee.dd.web.common.WebResourceCollection> createWebResourceCollectionsForWebApp() {
        final com.ibm.ws.javaee.dd.web.common.WebResourceCollection archiveWebResourceCollectionMock = mockery.mock(com.ibm.ws.javaee.dd.web.common.WebResourceCollection.class,
                                                                                                                    "WebResourceCollectionsForWebApp");
        mockery.checking(new Expectations() {
            {
                allowing(archiveWebResourceCollectionMock).getURLPatterns();
                will(returnValue(urlPatterns));
                allowing(archiveWebResourceCollectionMock).getHTTPMethods();
                will(returnValue(httpMethods));
                allowing(archiveWebResourceCollectionMock).getHTTPMethodOmissions();
                will(returnValue(Collections.EMPTY_LIST));
                allowing(archiveWebResourceCollectionMock).getWebResourceName();
                will(returnValue("WebResourceNameForWebApp"));
            }
        });
        List<com.ibm.ws.javaee.dd.web.common.WebResourceCollection> archiveWebResourceCollections = new ArrayList<com.ibm.ws.javaee.dd.web.common.WebResourceCollection>();
        archiveWebResourceCollections.add(archiveWebResourceCollectionMock);
        return archiveWebResourceCollections;
    }

    private List<com.ibm.ws.javaee.dd.web.common.WebResourceCollection> createWebResourceCollectionsForWebFragment() {
        final com.ibm.ws.javaee.dd.web.common.WebResourceCollection archiveWebResourceCollectionMock = mockery.mock(com.ibm.ws.javaee.dd.web.common.WebResourceCollection.class,
                                                                                                                    "WebResourceCollectionsForWebFragment");
        createCommonWebResourceCollectionsExpectationsForWebFragment(archiveWebResourceCollectionMock);
        mockery.checking(new Expectations() {
            {
                allowing(archiveWebResourceCollectionMock).getWebResourceName();
                will(returnValue("WebResourceNameForWebFragment"));
            }
        });
        List<com.ibm.ws.javaee.dd.web.common.WebResourceCollection> archiveWebResourceCollections = new ArrayList<com.ibm.ws.javaee.dd.web.common.WebResourceCollection>();
        archiveWebResourceCollections.add(archiveWebResourceCollectionMock);
        return archiveWebResourceCollections;
    }

    private List<com.ibm.ws.javaee.dd.web.common.WebResourceCollection> createWebResourceCollectionsWithConflictForWebFragment() {
        final com.ibm.ws.javaee.dd.web.common.WebResourceCollection archiveWebResourceCollectionMock = mockery.mock(com.ibm.ws.javaee.dd.web.common.WebResourceCollection.class,
                                                                                                                    "WebResourceCollectionsForWebFragment");
        createCommonWebResourceCollectionsExpectationsForWebFragment(archiveWebResourceCollectionMock);
        mockery.checking(new Expectations() {
            {
                allowing(archiveWebResourceCollectionMock).getWebResourceName();
                will(returnValue("WebResourceNameForWebApp"));
            }
        });
        List<com.ibm.ws.javaee.dd.web.common.WebResourceCollection> archiveWebResourceCollections = new ArrayList<com.ibm.ws.javaee.dd.web.common.WebResourceCollection>();
        archiveWebResourceCollections.add(archiveWebResourceCollectionMock);
        return archiveWebResourceCollections;
    }

    private void createCommonWebResourceCollectionsExpectationsForWebFragment(final com.ibm.ws.javaee.dd.web.common.WebResourceCollection archiveWebResourceCollectionMock) {
        mockery.checking(new Expectations() {
            {
                allowing(archiveWebResourceCollectionMock).getURLPatterns();
                will(returnValue(urlPatterns));
                allowing(archiveWebResourceCollectionMock).getHTTPMethods();
                will(returnValue(httpMethods));
                allowing(archiveWebResourceCollectionMock).getHTTPMethodOmissions();
                will(returnValue(Collections.EMPTY_LIST));
            }
        });
    }

    private AuthConstraint createAuthConstraintForWebApp() {
        final AuthConstraint authConstraintMock = mockery.mock(AuthConstraint.class, "AuthConstraintForWebApp");
        //final <List<String>> ConfigItem<List<String>> rolesConfigItem = mockery.mock(<List<String>>ConfigItem.class);
        mockery.checking(new Expectations() {
            {
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.AUTH_CONSTRAINT_KEY);
                will(returnValue(new HashMap<String, ConfigItem<List<String>>>()));
                allowing(authConstraintMock).getRoleNames();
                will(returnValue(WEBAPP_ROLES));
                allowing(configurator).createConfigItem(WEBAPP_ROLES);
                will(returnValue(rolesConfigItemFromWebApp));
            }
        });
        return authConstraintMock;
    }

    private AuthConstraint createAuthConstraintForWebFragment() {
        final AuthConstraint authConstraintMock = mockery.mock(AuthConstraint.class, "AuthConstraintForWebFragment");
        mockery.checking(new Expectations() {
            {
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.AUTH_CONSTRAINT_KEY);
                will(returnValue(new HashMap<String, ConfigItem<List<String>>>()));
                allowing(authConstraintMock).getRoleNames();
                will(returnValue(WEBFRAGMENT_ROLES));
                allowing(configurator).createConfigItem(WEBFRAGMENT_ROLES);
            }
        });
        return authConstraintMock;
    }

    private UserDataConstraint createUserDataConstraintForWebApp() {
        final int confidentialTransportGuarantee = 2; //CONFIDENTIAL
        final UserDataConstraint userDataConstraintMock = mockery.mock(UserDataConstraint.class, "UserDataConstraintForWebApp");
        mockery.checking(new Expectations() {
            {
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.USER_DATA_CONSTRAINT_KEY);
                will(returnValue(new HashMap<String, ConfigItem<String>>()));
                allowing(userDataConstraintMock).getTransportGuarantee();
                will(returnValue(confidentialTransportGuarantee));
                allowing(configurator).createConfigItem(String.valueOf(confidentialTransportGuarantee));
                will(returnValue(transportGuaranteeConfigItemFromWebApp));
            }
        });
        return userDataConstraintMock;
    }

    private UserDataConstraint createUserDataConstraintForWebFragment() {
        final int confidentialTransportGuarantee = 2; //CONFIDENTIAL
        final UserDataConstraint userDataConstraintMock = mockery.mock(UserDataConstraint.class, "UserDataConstraintForWebFragment");
        mockery.checking(new Expectations() {
            {
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.USER_DATA_CONSTRAINT_KEY);
                will(returnValue(new HashMap<String, ConfigItem<String>>()));
                allowing(userDataConstraintMock).getTransportGuarantee();
                will(returnValue(confidentialTransportGuarantee));
                allowing(configurator).createConfigItem(String.valueOf(confidentialTransportGuarantee));
            }
        });
        return userDataConstraintMock;
    }

    private List<SecurityRole> createSecurityRolesForWebAppMock() {
        List<SecurityRole> securityRoles = new ArrayList<SecurityRole>();
        final SecurityRole securityRoleMock = mockery.mock(SecurityRole.class, "SecurityRolesForWebAppMock");
        mockery.checking(new Expectations() {
            {
                allowing(securityRoleMock).getRoleName();
                will(returnValue(WEBAPP_ROLE));
            }
        });
        securityRoles.add(securityRoleMock);
        return securityRoles;
    }

    private List<SecurityRole> createSecurityRolesForWebFragmentMock() {
        List<SecurityRole> securityRoles = new ArrayList<SecurityRole>();
        final SecurityRole securityRoleMock = mockery.mock(SecurityRole.class, "SecurityRolesForWebFragmentMock");
        mockery.checking(new Expectations() {
            {
                allowing(securityRoleMock).getRoleName();
                will(returnValue(WEBFRAGMENT_ROLE));
            }
        });
        securityRoles.add(securityRoleMock);
        return securityRoles;
    }

    private WebApp createWebAppMockWithLoginConfigAndRunAs() {
        final com.ibm.ws.javaee.dd.web.common.LoginConfig loginConfig = createLoginConfigForWebAppMock();
        final WebApp webArchiveMock = createMockWebAppWithRunAs();
        mockery.checking(new Expectations() {
            {
                allowing(webArchiveMock).getLoginConfig();
                will(returnValue(loginConfig));
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.AUTH_METHOD_KEY);
                will(returnValue(new HashMap<String, ConfigItem<String>>()));

                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.REALM_NAME_KEY);
                will(returnValue(new HashMap<String, ConfigItem<String>>()));

            }
        });
        return webArchiveMock;
    }

    private WebApp createWebAppMockWithFullLoginConfig() {
        final com.ibm.ws.javaee.dd.web.common.LoginConfig loginConfig = createLoginConfigForWebAppMock();
        final WebApp webArchiveMock = createWebAppMock();
        mockery.checking(new Expectations() {
            {
                allowing(webArchiveMock).getLoginConfig();
                will(returnValue(loginConfig));
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.AUTH_METHOD_KEY);
                will(returnValue(new HashMap<String, ConfigItem<String>>()));
                allowing(configurator).createConfigItem(WEBAPP_AUTH_METHOD);
                allowing(configurator).getConfigItemMap(SecurityServletConfiguratorHelper.REALM_NAME_KEY);
                will(returnValue(new HashMap<String, ConfigItem<String>>()));
                allowing(configurator).createConfigItem(WEBAPP_REALM_NAME);
            }
        });
        return webArchiveMock;
    }

    private LoginConfig createLoginConfigForWebAppMock() {
        final LoginConfig loginConfigMock = mockery.mock(LoginConfig.class, "LoginConfigForWebAppMock");
        mockery.checking(new Expectations() {
            {
                allowing(loginConfigMock).getAuthMethod();
                will(returnValue(WEBAPP_AUTH_METHOD));
                allowing(loginConfigMock).getRealmName();
                will(returnValue(WEBAPP_REALM_NAME));
                allowing(loginConfigMock).getFormLoginConfig();
                will(returnValue(formLoginConfigFromMock));
            }
        });
        return loginConfigMock;
    }

    private LoginConfig createLoginConfigForWebFragmentMock() {
        final LoginConfig loginConfigMock = mockery.mock(LoginConfig.class, "LoginConfigForWebFragmentMock");
        mockery.checking(new Expectations() {
            {
                allowing(loginConfigMock).getAuthMethod();
                will(returnValue(WEBAPP_AUTH_METHOD));
                allowing(loginConfigMock).getRealmName();
                will(returnValue(WEBAPP_REALM_NAME));
                allowing(loginConfigMock).getFormLoginConfig();
                will(returnValue(formLoginConfigFromMock));
            }
        });
        return loginConfigMock;
    }

    private FormLoginConfig createFormLoginConfigMock(String mockName) {
        final FormLoginConfig formLoginConfigMock = mockery.mock(FormLoginConfig.class, mockName);
        mockery.checking(new Expectations() {
            {
                allowing(formLoginConfigMock).getFormLoginPage();
                will(returnValue("/loginPage.jsp"));
                allowing(formLoginConfigMock).getFormErrorPage();
                will(returnValue("/errorPage.jsp"));
            }
        });
        return formLoginConfigMock;
    }

    private WebApp createWebAppMockPrecluded() {
        final List<com.ibm.ws.javaee.dd.web.common.SecurityConstraint> archiveSecurityConstraintsPrecluded = createTestArchiveSecurityConstraintsPrecluded();
        final com.ibm.ws.javaee.dd.web.common.LoginConfig loginConfig = createLoginConfigForWebAppMock();
        final List<SecurityRole> securityRoles = createSecurityRolesForWebAppMock();
        final List<EnvEntry> envEntries = new ArrayList<EnvEntry>();
        final WebApp webArchiveMock = mockery.mock(WebApp.class);
        mockery.checking(new Expectations() {
            {
                allowing(webArchiveMock).getSecurityConstraints();
                will(returnValue(archiveSecurityConstraintsPrecluded));
                allowing(webArchiveMock).getLoginConfig();
                will(returnValue(loginConfig));
                allowing(webArchiveMock).getSecurityRoles();
                will(returnValue(securityRoles));
                allowing(webArchiveMock).getServlets();
                allowing(webArchiveMock).getServletMappings();
                allowing(webArchiveMock).getEnvEntries();
                will(returnValue(envEntries));
                allowing(webArchiveMock).isSetDenyUncoveredHttpMethods();
            }
        });
        return webArchiveMock;
    }

    private List<com.ibm.ws.javaee.dd.web.common.SecurityConstraint> createTestArchiveSecurityConstraintsPrecluded() {
        final List<com.ibm.ws.javaee.dd.web.common.WebResourceCollection> archiveWebResourceCollections = createWebResourceCollectionsForWebApp();
        final AuthConstraint authConstraint = createTestAuthConstraintPrecluded();
        final com.ibm.ws.javaee.dd.web.common.SecurityConstraint archiveSecurityConstraintMock = mockery.mock(com.ibm.ws.javaee.dd.web.common.SecurityConstraint.class);
        final UserDataConstraint userDataConstraint = createUserDataConstraintForWebApp();
        mockery.checking(new Expectations() {
            {
                allowing(archiveSecurityConstraintMock).getWebResourceCollections();
                will(returnValue(archiveWebResourceCollections));
                allowing(archiveSecurityConstraintMock).getAuthConstraint();
                will(returnValue(authConstraint));
                allowing(archiveSecurityConstraintMock).getUserDataConstraint();
                will(returnValue(userDataConstraint));
            }
        });
        List<com.ibm.ws.javaee.dd.web.common.SecurityConstraint> archiveSecurityConstraints = new ArrayList<com.ibm.ws.javaee.dd.web.common.SecurityConstraint>();
        archiveSecurityConstraints.add(archiveSecurityConstraintMock);
        return archiveSecurityConstraints;
    }

    private AuthConstraint createTestAuthConstraintPrecluded() {
        final AuthConstraint authConstraintMock = mockery.mock(AuthConstraint.class);
        mockery.checking(new Expectations() {
            {
                allowing(authConstraintMock).getRoleNames();
                will(returnValue(Collections.EMPTY_LIST));
            }
        });
        return authConstraintMock;
    }

    private WebAnnotations createNullWebAnnotationMock(final WebFragmentInfo webFragmentItem) throws UnableToAdaptException {
        final WebAnnotations webAnnotationsMock = mockery.mock(WebAnnotations.class);
        final FragmentAnnotations fragmentAnnotationsMock = mockery.mock(FragmentAnnotations.class);
        final ClassInfo classInfo = mockery.mock(ClassInfo.class);

        final Set<String> annotatedClasses = new HashSet<String>();
        final String className = "myClass.ibm.com";
        annotatedClasses.add(className);

        mockery.checking(new Expectations() {
            {
                one(webAnnotationsMock).getFragmentAnnotations(webFragmentItem);
                will(returnValue(fragmentAnnotationsMock));
                one(fragmentAnnotationsMock).selectAnnotatedClasses(DeclareRoles.class);
                will(returnValue(annotatedClasses));
                one(webAnnotationsMock).getClassInfo(className);
                will(returnValue(classInfo));
                one(classInfo).getAnnotation(DeclareRoles.class);
                will(returnValue(null));
            }
        });
        return webAnnotationsMock;
    }

    private WebAnnotations createWebAnnotationMock(final WebFragmentInfo webFragmentItem) throws UnableToAdaptException {
        final WebAnnotations webAnnotationsMock = mockery.mock(WebAnnotations.class);
        final FragmentAnnotations fragmentAnnotationsMock = mockery.mock(FragmentAnnotations.class);
        final ClassInfo classInfo = mockery.mock(ClassInfo.class);
        final AnnotationInfo declareRolesAnnotationInfo = mockery.mock(AnnotationInfo.class);
        final AnnotationValue annotationValue = mockery.mock(AnnotationValue.class);
        final List<AnnotationValue> roleValues = mockery.mock(List.class);

        final Set<String> annotatedClasses = new HashSet<String>();
        final String className = "myClass.ibm.com";
        annotatedClasses.add(className);

        createAnnotationValueIteratorMock(roleValues);

        mockery.checking(new Expectations() {
            {
                one(webAnnotationsMock).getFragmentAnnotations(webFragmentItem);
                will(returnValue(fragmentAnnotationsMock));
                one(fragmentAnnotationsMock).selectAnnotatedClasses(DeclareRoles.class);
                will(returnValue(annotatedClasses));
                one(webAnnotationsMock).getClassInfo(className);
                will(returnValue(classInfo));
                one(classInfo).getAnnotation(DeclareRoles.class);
                will(returnValue(declareRolesAnnotationInfo));
                one(declareRolesAnnotationInfo).getValue("value");
                will(returnValue(annotationValue));
                one(annotationValue).getArrayValue();
                will(returnValue(roleValues));
            }
        });
        return webAnnotationsMock;
    }

    private void createAnnotationValueIteratorMock(final List<AnnotationValue> roleValues) {
        final Iterator<AnnotationValue> iterator = mockery.mock(Iterator.class);
        final AnnotationValue roleValue1 = mockery.mock(AnnotationValue.class, "roleValue1");
        final AnnotationValue roleValue2 = mockery.mock(AnnotationValue.class, "rolevalue2");

        mockery.checking(new Expectations() {
            {
                one(roleValues).iterator();
                will(returnValue(iterator));
                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(roleValue1));
                one(roleValue1).getStringValue();
                will(returnValue(staticAnnotationRole1));
                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(roleValue2));
                one(roleValue2).getStringValue();
                will(returnValue(staticAnnotationRole2));
                one(iterator).hasNext();
                will(returnValue(false));
            }
        });
    }

    private WebFragment createWebFragmentMock() {
        final List<com.ibm.ws.javaee.dd.web.common.SecurityConstraint> archiveSecurityConstraints = createSecurityConstraintsForWebFragment();
        return createCommonExpectationsForWebFragmentMock(archiveSecurityConstraints);
    }

    private WebFragment createWebFragmentMockWithConflict() {
        final List<com.ibm.ws.javaee.dd.web.common.SecurityConstraint> archiveSecurityConstraints = createSecurityConstraintsWithConflictForWebFragment();
        return createCommonExpectationsForWebFragmentMock(archiveSecurityConstraints);
    }

    private WebFragment createCommonExpectationsForWebFragmentMock(final List<com.ibm.ws.javaee.dd.web.common.SecurityConstraint> archiveSecurityConstraints) {
        final List<Servlet> servlets = new ArrayList<Servlet>();
        final Servlet servlet = mockery.mock(Servlet.class, "WebFragmentServlet");
        servlets.add(servlet);

        final List<ServletMapping> servletMappings = new ArrayList<ServletMapping>();
        final ServletMapping mapping = mockery.mock(ServletMapping.class, "ServletMappingInWebFragment");
        servletMappings.add(mapping);
        final List<SecurityRoleRef> securityRoleRefs = new ArrayList<SecurityRoleRef>();
        final List<EnvEntry> envEntries = new ArrayList<EnvEntry>();
        final List<SecurityRole> securityRoles = createSecurityRolesForWebFragmentMock();
        final WebFragment webFragmentMock = mockery.mock(WebFragment.class);

        final RunAs runAs = mockery.mock(RunAs.class, "WebFragmentRunAs");

        mockery.checking(new Expectations() {
            {
                allowing(webFragmentMock).getSecurityConstraints();
                will(returnValue(archiveSecurityConstraints));
                allowing(webFragmentMock).getSecurityRoles();
                will(returnValue(securityRoles));
                allowing(webFragmentMock).getServlets();
                will(returnValue(servlets));
                allowing(webFragmentMock).getServletMappings();
                will(returnValue(servletMappings));
                allowing(webFragmentMock).getEnvEntries();
                will(returnValue(envEntries));
                allowing(servlet).getServletName();
                will(returnValue(SERVLET_NAME));
                allowing(mapping).getServletName();
                will(returnValue(SERVLET_NAME));
                allowing(mapping).getURLPatterns();
                will(returnValue(urlPatterns));
                allowing(servlet).getSecurityRoleRefs();
                will(returnValue(securityRoleRefs));
                allowing(webFragmentMock).getLoginConfig();
                will(returnValue(null));
                allowing(servlet).getRunAs();
                will(returnValue(runAs));
                allowing(runAs).getRoleName();
                will(returnValue(WEBFRAGMENT_RUNAS_ROLE));
            }
        });
        return webFragmentMock;
    }

    private WebFragment createWebFragmentMockWithLoginConfig() {
        final WebFragment webFragmentMock = createWebFragmentMock();
        final com.ibm.ws.javaee.dd.web.common.LoginConfig loginConfig = createLoginConfigForWebFragmentMock();

        mockery.checking(new Expectations() {
            {
                allowing(webFragmentMock).getLoginConfig();
                will(returnValue(loginConfig));
            }
        });
        return webFragmentMock;
    }

    private WebFragmentInfo createWebFragmentItemMock(final WebFragment webFragment) {
        final WebFragmentInfo webFragmentItemMock = mockery.mock(WebFragmentInfo.class);

        mockery.checking(new Expectations() {
            {
                allowing(webFragmentItemMock).getWebFragment();
                will(returnValue(webFragment));
            }
        });
        return webFragmentItemMock;
    }

    /**
     * This will pass an empty LoginConfig and assure that SecurityServletConfiguratorHelper.processLoginConfig(LoginConfig) set a BasicAuth login.
     */
    @Test
    public void testProcessLoginConfig() {

        mockery.checking(new Expectations() {
            {
                allowing(loginConfig).getAuthMethod();
                will(returnValue(null));
                allowing(loginConfig).getRealmName();
                will(returnValue(null));
                allowing(loginConfig).getFormLoginConfig();
                will(returnValue(null));
            }
        });
        configHelper.processLoginConfig(loginConfig);
        assertEquals("LoginConfiguration AuthenticationMethod should default to BASIC if not specified", configHelper.loginConfiguration.getAuthenticationMethod(),
                     LoginConfiguration.BASIC);
    }

}
