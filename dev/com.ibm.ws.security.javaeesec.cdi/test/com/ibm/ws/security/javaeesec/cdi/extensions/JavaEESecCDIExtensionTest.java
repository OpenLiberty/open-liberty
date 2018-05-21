/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.javaeesec.cdi.extensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.util.TypeLiteral;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;
import javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStore.ValidationType;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition.LdapSearchScope;
import javax.security.enterprise.identitystore.PasswordHash;
import javax.security.enterprise.identitystore.Pbkdf2PasswordHash;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.ws.security.javaeesec.properties.ModuleProperties;
import com.ibm.ws.security.javaeesec.cdi.beans.BasicHttpAuthenticationMechanism;
import com.ibm.ws.security.javaeesec.cdi.beans.CustomFormAuthenticationMechanism;
import com.ibm.ws.security.javaeesec.cdi.beans.FormAuthenticationMechanism;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;

public class JavaEESecCDIExtensionTest {

    private final Mockery context = new JUnit4Mockery();
    private final ProcessAnnotatedType pat1 = context.mock(ProcessAnnotatedType.class, "pat1");
    private final ProcessAnnotatedType pat2 = context.mock(ProcessAnnotatedType.class, "pat2");
    private final BeanManager bm = context.mock(BeanManager.class, "bm1");
    private final AnnotatedType at1 = context.mock(AnnotatedType.class, "at1");
    private final AnnotatedType at2 = context.mock(AnnotatedType.class, "at2");
//    private final LdapIdentityStoreDefinition lisd = context.mock(LdapIdentityStoreDefinition.class, "lisd1");
//    private final DatabaseIdentityStoreDefinition disd = context.mock(DatabaseIdentityStoreDefinition.class, "disd1");
    private final AfterBeanDiscovery abd = context.mock(AfterBeanDiscovery.class, "abd1");
    private final ProcessBean<?> pb = context.mock(ProcessBean.class, "pb1");
    private final ProcessBean<?> pb2 = context.mock(ProcessBean.class, "pb2");
    private final ProcessBean<?> pb3 = context.mock(ProcessBean.class, "pb3");
    private final Bean<?> bn = context.mock(Bean.class, "bn1");
    private final Bean<?> bn2 = context.mock(Bean.class, "bn2");
    @SuppressWarnings("unchecked")
    private final CreationalContext<IdentityStoreHandler> cc = context.mock(CreationalContext.class, "cc1");
    private final WebAppSecurityConfig wasc  = context.mock(WebAppSecurityConfig.class);
    private final WebModuleMetaData wmmd1 = context.mock(WebModuleMetaData.class, "wmmd1");
    private final WebModuleMetaData wmmd2 = context.mock(WebModuleMetaData.class, "wmmd2");
    private final J2EEName j2en1 = context.mock(J2EEName.class, "j2en1");
    private final J2EEName j2en2 = context.mock(J2EEName.class, "j2en2");
    private final ProcessBeanAttributes pba = context.mock(ProcessBeanAttributes.class, "pba1");
    

    private final static String MODULE_NAME1 = "module1.war";
    private final static String MODULE_NAME2 = "module2.war";
    private final static String MODULE_PATH_NAME1 = "/wlp/usr/servers/apps/" + MODULE_NAME1;
    private final static String MODULE_PATH_NAME2 = "/wlp/usr/servers/apps/" + MODULE_NAME2;

    private final static String MODULE_LOCATION_NAME1 = "file:" + MODULE_PATH_NAME1;
    private final static String MODULE_LOCATION_NAME2 = "file:" + MODULE_PATH_NAME2;
    private final static String CLASS_LOCATION1 = MODULE_PATH_NAME1 + "/foo/bar/HAMClass1";
    private final static String CLASS_LOCATION2 = MODULE_PATH_NAME2 + "/foo1/bar1/HAMClass2";
    private final static String REALM_NAME1 = "realmName1";

    private final static String LTC_FORM_LOGIN_PAGE = "/formLogin.jsp";
    private final static String LTC_FORM_ERROR_PAGE = "/formError.jsp";
    private final static String LTC_FORM_EL = "${el_expression}";
    private final static boolean LTC_FORM_USEFORWARD = false;

    private final static String LTC_CUSTOM_FORM_LOGIN_PAGE = "/customFormLogin.xhtml";
    private final static String LTC_CUSTOM_FORM_ERROR_PAGE = "/customFormError.xhtml";
    private final static String LTC_CUSTOM_FORM_EL = "${el_expression_for_custom}";
    private final static boolean LTC_CUSTOM_FORM_USEFORWARD = true;
    
    private final static String GLOBAL_FORM = "FORM";
    private final static String GLOBAL_BASIC = "BASIC";
    private final static String GLOBAL_FORM_CONTEXT_ROOT = "/global";
    private final static String GLOBAL_FORM_LOGIN_PAGE = "/GlobalFormLogin.jsp";
    private final static String GLOBAL_FORM_ERROR_PAGE = "/GlobalFormError.jsp";
    private final static String GLOBAL_FORM_LOGIN_URL = GLOBAL_FORM_CONTEXT_ROOT + GLOBAL_FORM_LOGIN_PAGE;
    private final static String GLOBAL_FORM_ERROR_URL = GLOBAL_FORM_CONTEXT_ROOT + GLOBAL_FORM_ERROR_PAGE;
    private final static String GLOBAL_BASIC_REALM_NAME = "globalRealm";
    

    private URL url1;
    private URL url2;

    @Rule
    public final TestName testName = new TestName();

    @Before
    public void setUp() {
        try {
            url1 = new URL(MODULE_LOCATION_NAME1);
            url2 = new URL(MODULE_LOCATION_NAME2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() throws Exception {
        context.assertIsSatisfied();
    }

    @Test
    public void processAnnotatedTypeNA() {
        final Set<Annotation> aset = new HashSet<Annotation>();
        final InvalidAnnotation ia = getIAInstance();
        aset.add(ia);
        context.checking(new Expectations() {
            {
                one(pat1).getAnnotatedType();
                will(returnValue(at1));
                one(at1).getJavaClass();
                will(returnValue(String.class));
                one(at1).getAnnotations();
                will(returnValue(aset));
                one(wasc).getOverrideHttpAuthMethod();
                will(returnValue(null));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension() {
            @Override
            protected WebAppSecurityConfig getWebAppSecurityConfig() {
                return wasc;
            }
        };
        j3ce.processAnnotatedType(pat1, bm);
        Set<Bean> beans = j3ce.getBeansToAdd();
        assertTrue("beansToAdd should be empty after processAnnotatedType", beans.isEmpty());
    }

    @Test
    public void processAnnotatedTypeLdapIdentityStore() {
        final Set<Annotation> aset = new HashSet<Annotation>();
        final Properties props = new Properties();
        final LdapIdentityStoreDefinition lisd = getLISDInstance(props);
        aset.add(lisd);
        context.checking(new Expectations() {
            {
                one(at1).getJavaClass();
                will(returnValue(String.class));
                one(pat1).getAnnotatedType();
                will(returnValue(at1));
                one(at1).getAnnotations();
                will(returnValue(aset));
                one(wasc).getOverrideHttpAuthMethod();
                will(returnValue(null));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension() {
            @Override
            protected WebAppSecurityConfig getWebAppSecurityConfig() {
                return wasc;
            }
        };
        j3ce.processAnnotatedType(pat1, bm);
        Set<Bean> beans = j3ce.getBeansToAdd();
        assertEquals("incorrect number of beans in beansToAdd after processAnnotatedType", 1, beans.size());
        assertTrue("incorrect beansToAdd value after processAnnotatedType", beans.iterator().next().getClass().equals(LdapIdentityStoreBean.class));
    }

    @Test
    public void processAnnotatedTypeBasicHAM() {
        final Set<Annotation> aset = new HashSet<Annotation>();
        final BasicAuthenticationMechanismDefinition bamd = getBAMDInstance(REALM_NAME1);
        final Map<URL, ModuleMetaData> mmds = new HashMap<URL, ModuleMetaData>();
        aset.add(bamd);
        context.checking(new Expectations() {
            {
                one(at1).getJavaClass();
                will(returnValue(HAMClass1.class));
                one(pat1).getAnnotatedType();
                will(returnValue(at1));
                one(at1).getAnnotations();
                will(returnValue(aset));
                one(wasc).getOverrideHttpAuthMethod();
                will(returnValue(null));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension() {
            @Override
            protected WebAppSecurityConfig getWebAppSecurityConfig() {
                return wasc;
            }
            @Override
            protected Map<URL, ModuleMetaData> getModuleMetaDataMap() {
                return mmds;
            }

            @Override
            protected String getClassFileLocation(Class klass) {
                if (klass.equals(HAMClass1.class)) {
                    return CLASS_LOCATION1;
                } else {
                    return CLASS_LOCATION2;
                }
            }
        };
        createMMDs(mmds);
        j3ce.processAnnotatedType(pat1, bm);
        Map<String, ModuleProperties> moduleMap = j3ce.getModuleMap();
        // check ModuleProperties object.
        ModuleProperties moduleProps = moduleMap.get(MODULE_NAME1);
        assertNotNull("The moduleMap should contain an element of module " + MODULE_NAME1, moduleProps);
        Properties props = moduleProps.getFromAuthMechMap(BasicHttpAuthenticationMechanism.class);
        assertNotNull("Properties for the HAM implementation class BasicHttpAuthenticationMechanism.class should not be null.", props);
        assertEquals("realm name should be " + REALM_NAME1, props.get(JavaEESecConstants.REALM_NAME), REALM_NAME1);
    }

    @Test
    public void processAnnotatedTypeFormHAM() {
        final Set<Annotation> aset = new HashSet<Annotation>();
        Properties props = new Properties();
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, LTC_FORM_LOGIN_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, LTC_FORM_ERROR_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION, LTC_FORM_EL);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN, LTC_FORM_USEFORWARD);
        final LoginToContinue ltc = getLTCInstance(props);
        final FormAuthenticationMechanismDefinition famd = getFAMDInstance(ltc);

        final Map<URL, ModuleMetaData> mmds = new HashMap<URL, ModuleMetaData>();
        aset.add(famd);
        context.checking(new Expectations() {
            {
                one(at1).getJavaClass();
                will(returnValue(HAMClass1.class));
                one(pat1).getAnnotatedType();
                will(returnValue(at1));
                one(at1).getAnnotations();
                will(returnValue(aset));
                one(wasc).getOverrideHttpAuthMethod();
                will(returnValue(null));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension() {
            @Override
            protected WebAppSecurityConfig getWebAppSecurityConfig() {
                return wasc;
            }
            @Override
            protected Map<URL, ModuleMetaData> getModuleMetaDataMap() {
                return mmds;
            }

            @Override
            protected String getClassFileLocation(Class klass) {
                if (klass.equals(HAMClass1.class)) {
                    return CLASS_LOCATION1;
                } else {
                    return CLASS_LOCATION2;
                }
            }
        };
        createMMDs(mmds);
        j3ce.processAnnotatedType(pat1, bm);
        Map<String, ModuleProperties> moduleMap = j3ce.getModuleMap();
        // check ModuleProperties object.
        ModuleProperties moduleProps = moduleMap.get(MODULE_NAME1);
        assertNotNull("The moduleMap should contain an element of module " + MODULE_NAME1, moduleProps);
        Properties resultProps = moduleProps.getFromAuthMechMap(FormAuthenticationMechanism.class);
        assertNotNull("Properties for the HAM implementation class FormAuthenticationMechanism.class should not be null.", resultProps);
        System.out.println("resultProps : " + resultProps);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE) , LTC_FORM_LOGIN_PAGE);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE), LTC_FORM_ERROR_PAGE);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION), LTC_FORM_EL);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN), LTC_FORM_USEFORWARD);
    }

    @Test
    public void processAnnotatedTypeApplicationHAM() {
        final Set<Annotation> aset = new HashSet<Annotation>();
        Properties props = new Properties();
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, LTC_FORM_LOGIN_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, LTC_FORM_ERROR_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION, LTC_FORM_EL);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN, LTC_FORM_USEFORWARD);
        final LoginToContinue ltc = getLTCInstance(props);

        final Map<URL, ModuleMetaData> mmds = new HashMap<URL, ModuleMetaData>();
        context.checking(new Expectations() {
            {
                one(at1).getJavaClass();
                will(returnValue(ApplicationHAM.class));
                one(pat1).getAnnotatedType();
                will(returnValue(at1));
                one(at1).getAnnotation(LoginToContinue.class);
                will(returnValue(ltc));
                one(wasc).getOverrideHttpAuthMethod();
                will(returnValue(null));
                one(at1).getAnnotations();
                will(returnValue(aset));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension() {
            @Override
            protected WebAppSecurityConfig getWebAppSecurityConfig() {
                return wasc;
            }
            @Override
            protected Map<URL, ModuleMetaData> getModuleMetaDataMap() {
                return mmds;
            }

            @Override
            protected String getClassFileLocation(Class klass) {
                if (klass.equals(ApplicationHAM.class)) {
                    return CLASS_LOCATION1;
                } else {
                    return CLASS_LOCATION2;
                }
            }
        };
        createMMDs(mmds);
        j3ce.processAnnotatedType(pat1, bm);
        Map<String, ModuleProperties> moduleMap = j3ce.getModuleMap();
        // check ModuleProperties object.
        ModuleProperties moduleProps = moduleMap.get(MODULE_NAME1);
        assertNotNull("The moduleMap should contain an element of module " + MODULE_NAME1, moduleProps);
        Properties resultProps = moduleProps.getFromAuthMechMap(ApplicationHAM.class);
        assertNotNull("Properties for the application provided HAM with LoginToContinue annotation should not be null.", resultProps);
        System.out.println("resultProps : " + resultProps);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE) , LTC_FORM_LOGIN_PAGE);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE), LTC_FORM_ERROR_PAGE);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION), LTC_FORM_EL);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN), LTC_FORM_USEFORWARD);
    }

    @Test
    public void processAnnotatedTypeFormAndCustomFormHAM() {
        final Set<Annotation> aset1 = new HashSet<Annotation>();
        Properties propsForm = new Properties();
        propsForm.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, LTC_FORM_LOGIN_PAGE);
        propsForm.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, LTC_FORM_ERROR_PAGE);
        propsForm.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION, LTC_FORM_EL);
        propsForm.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN, LTC_FORM_USEFORWARD);
        final LoginToContinue ltcForm = getLTCInstance(propsForm);
        final FormAuthenticationMechanismDefinition famd = getFAMDInstance(ltcForm);
        aset1.add(famd);

        final Set<Annotation> aset2 = new HashSet<Annotation>();
        Properties propsCustom = new Properties();
        propsCustom.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, LTC_CUSTOM_FORM_LOGIN_PAGE);
        propsCustom.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, LTC_CUSTOM_FORM_ERROR_PAGE);
        propsCustom.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION, LTC_CUSTOM_FORM_EL);
        propsCustom.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN, LTC_CUSTOM_FORM_USEFORWARD);
        final LoginToContinue ltcCustom = getLTCInstance(propsCustom);
        final CustomFormAuthenticationMechanismDefinition cfamd = getCFAMDInstance(ltcCustom);

        final Map<URL, ModuleMetaData> mmds = new HashMap<URL, ModuleMetaData>();
        aset2.add(cfamd);

        context.checking(new Expectations() {
            {
                one(pat1).getAnnotatedType();
                will(returnValue(at1));
                one(at1).getJavaClass();
                will(returnValue(HAMClass1.class));
                one(at1).getAnnotations();
                will(returnValue(aset1));

                one(pat2).getAnnotatedType();
                will(returnValue(at2));
                one(at2).getJavaClass();
                will(returnValue(HAMClass2.class));
                one(at2).getAnnotations();
                will(returnValue(aset2));

                exactly(2).of(wasc).getOverrideHttpAuthMethod();
                will(returnValue(null));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension() {
            @Override
            protected WebAppSecurityConfig getWebAppSecurityConfig() {
                return wasc;
            }
            @Override
            protected Map<URL, ModuleMetaData> getModuleMetaDataMap() {
                return mmds;
            }

            @Override
            protected String getClassFileLocation(Class klass) {
                if (klass.equals(HAMClass1.class)) {
                    return CLASS_LOCATION1;
                } else {
                    return CLASS_LOCATION2;
                }
            }
        };
        createMMDs(mmds);

        j3ce.processAnnotatedType(pat1, bm);
        j3ce.processAnnotatedType(pat2, bm);

        Map<String, ModuleProperties> moduleMap = j3ce.getModuleMap();
        // check ModuleProperties object for Form.
        ModuleProperties moduleProps = moduleMap.get(MODULE_NAME1);
        assertNotNull("The moduleMap should contain an element of module " + MODULE_NAME1, moduleProps);
        Properties resultProps = moduleProps.getFromAuthMechMap(FormAuthenticationMechanism.class);
        assertNotNull("Properties for the HAM implementation class FormAuthenticationMechanism.class should not be null.", resultProps);
        System.out.println("resultProps for Form: " + resultProps);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE) , LTC_FORM_LOGIN_PAGE);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE), LTC_FORM_ERROR_PAGE);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION), LTC_FORM_EL);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN), LTC_FORM_USEFORWARD);

        // check ModuleProperties object for Custom Form.
        moduleProps = moduleMap.get(MODULE_NAME2);
        assertNotNull("The moduleMap should contain an element of module " + MODULE_NAME2, moduleProps);
        resultProps = moduleProps.getFromAuthMechMap(CustomFormAuthenticationMechanism.class);
        assertNotNull("Properties for the HAM implementation class CustomFormAuthenticationMechanism.class should not be null.", resultProps);
        System.out.println("resultProps for CustomForm: " + resultProps);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE) , LTC_CUSTOM_FORM_LOGIN_PAGE);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE), LTC_CUSTOM_FORM_ERROR_PAGE);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION), LTC_CUSTOM_FORM_EL);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN), LTC_CUSTOM_FORM_USEFORWARD);
    }

    @Test
    public void processAnnotatedTypeBasicAndCustomFormHAMOverriddenByGlobalForm() {
        final Set<Annotation> aset1 = new HashSet<Annotation>();

        final BasicAuthenticationMechanismDefinition bamd = getBAMDInstance(REALM_NAME1);
        aset1.add(bamd);

        final Set<Annotation> aset2 = new HashSet<Annotation>();
        Properties propsCustom = new Properties();
        propsCustom.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, LTC_CUSTOM_FORM_LOGIN_PAGE);
        propsCustom.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, LTC_CUSTOM_FORM_ERROR_PAGE);
        propsCustom.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION, LTC_CUSTOM_FORM_EL);
        propsCustom.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN, LTC_CUSTOM_FORM_USEFORWARD);
        final LoginToContinue ltcCustom = getLTCInstance(propsCustom);
        final CustomFormAuthenticationMechanismDefinition cfamd = getCFAMDInstance(ltcCustom);

        final Map<URL, ModuleMetaData> mmds = new HashMap<URL, ModuleMetaData>();
        aset2.add(cfamd);

        context.checking(new Expectations() {
            {
                one(pat1).getAnnotatedType();
                will(returnValue(at1));
                one(at1).getJavaClass();
                will(returnValue(HAMClass1.class));
                one(at1).getAnnotations();
                will(returnValue(aset1));

                one(pat2).getAnnotatedType();
                will(returnValue(at2));
                one(at2).getJavaClass();
                will(returnValue(HAMClass2.class));
                one(at2).getAnnotations();
                will(returnValue(aset2));

                allowing(wasc).getOverrideHttpAuthMethod();
                will(returnValue(GLOBAL_FORM));
                exactly(2).of(wasc).getLoginFormURL();
                will(returnValue(GLOBAL_FORM_LOGIN_URL));
                exactly(2).of(wasc).getLoginErrorURL();
                will(returnValue(GLOBAL_FORM_ERROR_URL));
                exactly(2).of(wasc).getLoginFormContextRoot();
                will(returnValue(GLOBAL_FORM_CONTEXT_ROOT));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension() {
            @Override
            protected WebAppSecurityConfig getWebAppSecurityConfig() {
                return wasc;
            }
            @Override
            protected Map<URL, ModuleMetaData> getModuleMetaDataMap() {
                return mmds;
            }

            @Override
            protected String getClassFileLocation(Class klass) {
                if (klass.equals(HAMClass1.class)) {
                    return CLASS_LOCATION1;
                } else {
                    return CLASS_LOCATION2;
                }
            }
        };
        createMMDs(mmds);

        j3ce.processAnnotatedType(pat1, bm);
        j3ce.processAnnotatedType(pat2, bm);

        Map<String, ModuleProperties> moduleMap = j3ce.getModuleMap();
        // check ModuleProperties object for Form.
        ModuleProperties moduleProps = moduleMap.get(MODULE_NAME1);
        assertNotNull("The moduleMap should contain an element of module " + MODULE_NAME1, moduleProps);
        Properties resultProps = moduleProps.getFromAuthMechMap(FormAuthenticationMechanism.class);
        assertNotNull("Properties for the Global Login FormAuthenticationMechanism.class should not be null.", resultProps);
        System.out.println("resultProps for Form: " + resultProps);

        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE) , GLOBAL_FORM_LOGIN_PAGE);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE), GLOBAL_FORM_ERROR_PAGE);
        assertNull("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION));
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN), true);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_USE_GLOBAL_LOGIN + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_USE_GLOBAL_LOGIN), true);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_LOGIN_FORM_CONTEXT_ROOT + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGIN_FORM_CONTEXT_ROOT), GLOBAL_FORM_CONTEXT_ROOT);

        // check ModuleProperties object for Custom Form.
        moduleProps = moduleMap.get(MODULE_NAME2);
        assertNotNull("The moduleMap should contain an element of module " + MODULE_NAME2, moduleProps);
        resultProps = moduleProps.getFromAuthMechMap(FormAuthenticationMechanism.class);
        assertNotNull("Properties for the Global Login FormAuthenticationMechanism.class should not be null.", resultProps);
        System.out.println("resultProps for CustomForm: " + resultProps);

        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE) , GLOBAL_FORM_LOGIN_PAGE);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE), GLOBAL_FORM_ERROR_PAGE);
        assertNull("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION));
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN), true);
        assertEquals("the property value of " + JavaEESecConstants.LOGIN_TO_CONTINUE_USE_GLOBAL_LOGIN + " is not valid.", resultProps.get(JavaEESecConstants.LOGIN_TO_CONTINUE_USE_GLOBAL_LOGIN), true);
    }

    @Test
    public void processAnnotatedTypeApplicationAndFormHAMNOverriddenByGlobalBasic() {
        final Set<Annotation> aset1 = new HashSet<Annotation>();
        Properties propsAppl = new Properties();
        propsAppl.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, LTC_FORM_LOGIN_PAGE);
        propsAppl.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, LTC_FORM_ERROR_PAGE);
        propsAppl.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION, LTC_FORM_EL);
        propsAppl.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN, LTC_FORM_USEFORWARD);
        final LoginToContinue ltc1 = getLTCInstance(propsAppl);

        final Set<Annotation> aset2 = new HashSet<Annotation>();
        Properties propsForm = new Properties();
        propsForm.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, LTC_FORM_LOGIN_PAGE);
        propsForm.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, LTC_FORM_ERROR_PAGE);
        propsForm.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION, LTC_FORM_EL);
        propsForm.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN, LTC_FORM_USEFORWARD);
        final LoginToContinue ltc2 = getLTCInstance(propsForm);
        final FormAuthenticationMechanismDefinition famd = getFAMDInstance(ltc2);

        final Map<URL, ModuleMetaData> mmds = new HashMap<URL, ModuleMetaData>();
        aset2.add(famd);

        context.checking(new Expectations() {
            {
                one(at1).getJavaClass();
                will(returnValue(ApplicationHAM.class));
                one(pat1).getAnnotatedType();
                will(returnValue(at1));
                one(at1).getAnnotations();
                will(returnValue(aset1));

                one(pat2).getAnnotatedType();
                will(returnValue(at2));
                one(at2).getJavaClass();
                will(returnValue(HAMClass2.class));
                one(at2).getAnnotations();
                will(returnValue(aset2));

                allowing(wasc).getOverrideHttpAuthMethod();
                will(returnValue(GLOBAL_BASIC));
                exactly(2).of(wasc).getBasicAuthRealmName();
                will(returnValue(GLOBAL_BASIC_REALM_NAME));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension() {
            @Override
            protected WebAppSecurityConfig getWebAppSecurityConfig() {
                return wasc;
            }
            @Override
            protected Map<URL, ModuleMetaData> getModuleMetaDataMap() {
                return mmds;
            }

            @Override
            protected String getClassFileLocation(Class klass) {
                if (klass.equals(ApplicationHAM.class)) {
                    return CLASS_LOCATION1;
                } else if (klass.equals(HAMClass2.class)) {
                    return CLASS_LOCATION2;
                }
                return null;
            }
        };
        createMMDs(mmds);
        j3ce.processAnnotatedType(pat1, bm);
        j3ce.processAnnotatedType(pat2, bm);
        Map<String, ModuleProperties> moduleMap = j3ce.getModuleMap();
        // check ModuleProperties object.
        ModuleProperties moduleProps = moduleMap.get(MODULE_NAME1);
        assertNotNull("The moduleMap should contain an element of module " + MODULE_NAME1, moduleProps);
        Properties resultProps = moduleProps.getFromAuthMechMap(BasicHttpAuthenticationMechanism.class);
        assertNotNull("Properties for Global Basic Login should not be null.", resultProps);
        System.out.println("resultProps : " + resultProps);
        assertEquals("realm name should be " + GLOBAL_BASIC_REALM_NAME, resultProps.get(JavaEESecConstants.REALM_NAME), GLOBAL_BASIC_REALM_NAME);

        moduleProps = moduleMap.get(MODULE_NAME2);
        System.out.println("moduleProps2 : " + moduleProps);
        assertNotNull("The moduleMap should contain an element of module " + MODULE_NAME2, moduleProps);
        resultProps = moduleProps.getFromAuthMechMap(BasicHttpAuthenticationMechanism.class);
        assertNotNull("Properties for Global Basic Login should not be null.", resultProps);
        System.out.println("resultProps : " + resultProps);
        assertEquals("realm name should be " + GLOBAL_BASIC_REALM_NAME, resultProps.get(JavaEESecConstants.REALM_NAME), GLOBAL_BASIC_REALM_NAME);
    }

    @Test
    public void afterBeanDiscoveryNoCustomIdentityStoreHandlerOneIdentityStore() {
        final Set<Type> types = new HashSet<Type>();
        types.add(new TypeLiteral<Bean>() {}.getType());
        types.add(new TypeLiteral<IdentityStore>() {}.getType());
        context.checking(new Expectations() {
            {
                exactly(2).of(pb).getBean();
                will(returnValue(bn));
                between(2, 3).of(bn).getBeanClass();
                will(returnValue(TestIdentityStore.class));
                exactly(2).of(bn).getTypes();
                will(returnValue(types));
                one(abd).addBean(with(any(IdentityStoreHandlerBean.class)));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension();
        j3ce.processBean(pb, bm);
        j3ce.afterBeanDiscovery(abd, bm);
        assertTrue("incorrect IentityStoreRegistered value after afterBeanDiscovery", j3ce.getIdentityStoreRegistered());
        assertFalse("incorrect IentityStoreHandlerRegistered value after afterBeanDiscovery", j3ce.getIdentityStoreHandlerRegistered());
        assertFalse("incorrect beansToAdd value after afterBeanDiscovery", j3ce.getBeansToAdd().isEmpty());
    }

    @Test
    public void afterBeanDiscoveryCustomIdentityStoreHandlerExists() {
        final Set<Type> types = new HashSet<Type>();
        types.add(new TypeLiteral<Bean>() {}.getType());
        types.add(new TypeLiteral<IdentityStoreHandler>() {}.getType());
        context.checking(new Expectations() {
            {
                exactly(2).of(pb).getBean();
                will(returnValue(bn));
                between(2, 3).of(bn).getBeanClass();
                will(returnValue(Object.class));
                exactly(2).of(bn).getTypes();
                will(returnValue(types));
                never(abd).addBean(with(any(IdentityStoreHandlerBean.class)));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension();
        assertFalse("incorrect identityStoreHandlerRegistered value", j3ce.getIdentityStoreHandlerRegistered());
        j3ce.processBean(pb, bm);
        assertTrue("incorrect IdentityStoreHandlerRegistered value after processBean", j3ce.getIdentityStoreHandlerRegistered());
        j3ce.afterBeanDiscovery(abd, bm);
        assertTrue("incorrect beansToAdd value after afterBeanDiscovery", j3ce.getBeansToAdd().isEmpty());
    }

    @Test
    public void processBeanIdentityStoreHandlerTrueIdentityStoreTrue() {
        final Set<Type> types = new HashSet<Type>();
        types.add(new TypeLiteral<Bean>() {}.getType());
        types.add(new TypeLiteral<IdentityStoreHandler>() {}.getType());

        final Set<Type> types2 = new HashSet<Type>();
        types2.add(new TypeLiteral<Bean>() {}.getType());
        types2.add(new TypeLiteral<IdentityStore>() {}.getType());

        context.checking(new Expectations() {
            {
                exactly(2).of(pb).getBean();
                will(returnValue(bn));
                between(2, 3).of(bn).getBeanClass();
                will(returnValue(Object.class));
                exactly(2).of(bn).getTypes();
                will(returnValue(types));
                one(pb2).getBean();
                will(returnValue(bn2));
                between(1, 2).of(bn2).getBeanClass();
                will(returnValue(Object.class));
                one(bn2).getTypes();
                will(returnValue(types2));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension();
        assertFalse("incorrect identityStoreHandlerRegistered value", j3ce.getIdentityStoreHandlerRegistered());
        assertFalse("incorrect identityStoreRegistered value", j3ce.getIdentityStoreRegistered());
        j3ce.processBean(pb, bm);
        j3ce.processBean(pb2, bm);
        assertTrue("incorrect identityStoreHandlerRegistered value after processBean", j3ce.getIdentityStoreHandlerRegistered());
        assertTrue("incorrect identityStoreRegistered value after processBean", j3ce.getIdentityStoreRegistered());
    }

    @Test
    public void processBeanIdentityStoreHandlerFalseIdentityStoreFalse() {
        final Set<Type> types = new HashSet<Type>();
        types.add(new TypeLiteral<Bean>() {}.getType());
        types.add(new TypeLiteral<IdentityStoreHandler>() {}.getType());
        final Set<Type> types2 = new HashSet<Type>();
        types2.add(new TypeLiteral<Bean>() {}.getType());
        types2.add(new TypeLiteral<IdentityStore>() {}.getType());

        context.checking(new Expectations() {
            {
                exactly(2).of(pb).getBean();
                will(returnValue(bn));
                exactly(2).of(bn).getBeanClass();
                will(returnValue(IdentityStoreHandler.class));
                one(bn).getTypes();
                will(returnValue(types));
                exactly(2).of(pb2).getBean();
                will(returnValue(bn2));
                exactly(2).of(bn2).getBeanClass();
                will(returnValue(IdentityStore.class));
                one(bn2).getTypes();
                will(returnValue(types2));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension();
        assertFalse("incorrect identityStoreHandlerRegistered value", j3ce.getIdentityStoreHandlerRegistered());
        assertFalse("incorrect identityStoreRegistered value", j3ce.getIdentityStoreRegistered());
        j3ce.processBean(pb, bm);
        j3ce.processBean(pb2, bm);
        assertFalse("incorrect identityStoreHandlerRegistered value after processBean", j3ce.getIdentityStoreHandlerRegistered());
        assertFalse("incorrect identityStoreRegistered value after processBean", j3ce.getIdentityStoreRegistered());
    }

    @Test
    public void processBeanIdentityStoreHandlerStaysTrueIdentityStoreStaysTrue() {
        final Set<Type> types = new HashSet<Type>();
        types.add(new TypeLiteral<Bean>() {}.getType());
        types.add(new TypeLiteral<IdentityStoreHandler>() {}.getType());
        final Set<Type> types2 = new HashSet<Type>();
        types2.add(new TypeLiteral<Bean>() {}.getType());
        types2.add(new TypeLiteral<IdentityStore>() {}.getType());

        context.checking(new Expectations() {
            {
                exactly(2).of(pb).getBean();
                will(returnValue(bn));
                between(2, 3).of(bn).getBeanClass();
                will(returnValue(Object.class));
                exactly(2).of(bn).getTypes();
                will(returnValue(types));
                one(pb2).getBean();
                will(returnValue(bn2));
                between(1, 2).of(bn2).getBeanClass();
                will(returnValue(Object.class));
                one(bn2).getTypes();
                will(returnValue(types2));
                never(pb3).getBean();
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension();
        assertFalse("incorrect identityStoreHandlerRegistered value", j3ce.getIdentityStoreHandlerRegistered());
        assertFalse("incorrect identityStoreRegistered value", j3ce.getIdentityStoreRegistered());
        j3ce.processBean(pb, bm);
        assertTrue("incorrect identityStoreHandlerRegistered value after processBean", j3ce.getIdentityStoreHandlerRegistered());
        assertFalse("incorrect identityStoreRegistered value", j3ce.getIdentityStoreRegistered());
        j3ce.processBean(pb2, bm);
        assertTrue("incorrect identityStoreHandlerRegistered value after 2nd  processBean", j3ce.getIdentityStoreHandlerRegistered());
        assertTrue("incorrect identityStoreRegistered value after 2nd processBean", j3ce.getIdentityStoreRegistered());
        j3ce.processBean(pb3, bm);
        assertTrue("incorrect identityStoreHandlerRegistered value after 3rd  processBean", j3ce.getIdentityStoreHandlerRegistered());
        assertTrue("incorrect identityStoreRegistered value after 3rd  processBean", j3ce.getIdentityStoreRegistered());
    }

    @Test
    public void isApplicationIdentityStoreHanderTrue() {
        final Set<Type> types = new HashSet<Type>();
        types.add(new TypeLiteral<Bean>() {}.getType());
        types.add(new TypeLiteral<IdentityStoreHandler>() {}.getType());

        context.checking(new Expectations() {
            {
                one(pb).getBean();
                will(returnValue(bn));
                between(1, 2).of(bn).getBeanClass();
                will(returnValue(Object.class));
                one(bn).getTypes();
                will(returnValue(types));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension();
        assertTrue("incorrect result.", j3ce.isIdentityStoreHandler(pb));
    }

    @Test
    public void isApplicationIdentityStoreHanderFalseBecauseInterface() {

        context.checking(new Expectations() {
            {
                one(pb).getBean();
                will(returnValue(bn));
                one(bn).getBeanClass();
                will(returnValue(IdentityStoreHandler.class));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension();
        assertFalse("incorrect result.", j3ce.isIdentityStoreHandler(pb));
    }

    @Test
    public void isApplicationIdentityStoreHanderFalseBecauseNotHandlerClass() {
        final Set<Type> types = new HashSet<Type>();
        types.add(new TypeLiteral<Bean>() {}.getType());
        types.add(new TypeLiteral<IdentityStore>() {}.getType());

        context.checking(new Expectations() {
            {
                one(pb).getBean();
                will(returnValue(bn));
                one(bn).getBeanClass();
                will(returnValue(Object.class));
                one(bn).getTypes();
                will(returnValue(types));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension();
        assertFalse("incorrect result.", j3ce.isIdentityStoreHandler(pb));
    }

//    @Test
    public void isApplicationIdentityStoreTrue() {
        final Set<Type> types = new HashSet<Type>();
        types.add(new TypeLiteral<Bean>() {}.getType());
        types.add(new TypeLiteral<IdentityStore>() {}.getType());

        context.checking(new Expectations() {
            {
                one(pb).getBean();
                will(returnValue(bn));
                between(1, 2).of(bn).getBeanClass();
                will(returnValue(Object.class));
                one(bn).getTypes();
                will(returnValue(types));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension();
        assertTrue("incorrect result.", j3ce.isIdentityStore(pb));
    }

    @Test
    public void isApplicationIdentityStoreFalseBecauseInterface() {

        context.checking(new Expectations() {
            {
                one(pb).getBean();
                will(returnValue(bn));
                one(bn).getBeanClass();
                will(returnValue(IdentityStore.class));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension();
        assertFalse("incorrect result.", j3ce.isIdentityStore(pb));
    }

    @Test
    public void isApplicationIdentityStoreFalseBecauseNotClass() {
        final Set<Type> types = new HashSet<Type>();
        types.add(new TypeLiteral<Bean>() {}.getType());
        types.add(new TypeLiteral<IdentityStoreHandler>() {}.getType());

        context.checking(new Expectations() {
            {
                one(pb).getBean();
                will(returnValue(bn));
                one(bn).getBeanClass();
                will(returnValue(Object.class));
                one(bn).getTypes();
                will(returnValue(types));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension();
        assertFalse("incorrect result.", j3ce.isIdentityStore(pb));
    }


// TODO: need to add tests for equalsLdapDefinition params.
    @Test
    public void equalsLdapDefinitionStrings() {
        String KEY[] = {"bindDn", "bindDnPassword", "callerBaseDn", "callerNameAttribute", "callerSearchBase", "callerSearchFilter", "callerSearchScopeExpression", "groupMemberAttribute", "groupMemberOfAttribute", "groupNameAttribute", "groupSearchBase", "groupSearchFilter", "groupSearchScopeExpression", "maxResultsExpression", JavaEESecConstants.PRIORITY_EXPRESSION, "readTimeoutExpression", "url", JavaEESecConstants.USE_FOR_EXPRESSION};
        List<String> KEYS = Arrays.asList(KEY);
        String VALUE1="value1";
        String VALUE2="value2";
        for (String key : KEYS) {
            equalsLdapDefinitionTest(key, VALUE1, VALUE2);
        }
    }

    @Test
    public void equalsLdapDefinitionIntegers() {
        String KEY[] = {"maxResults", JavaEESecConstants.PRIORITY, "readTimeout"};
        List<String> KEYS = Arrays.asList(KEY);
        Integer VALUE1= new Integer(10);
        Integer VALUE2= new Integer(20);
        for (String key : KEYS) {
            equalsLdapDefinitionTest(key, VALUE1, VALUE2);
        }
    }

    @Test
    public void equalsLdapDefinitionSearchScope() {
        String KEY[] = {"callerSearchScope", "groupSearchScope"};
        List<String> KEYS = Arrays.asList(KEY);
        LdapSearchScope VALUE1= LdapSearchScope.SUBTREE;
        LdapSearchScope VALUE2= LdapSearchScope.ONE_LEVEL;
        for (String key : KEYS) {
            equalsLdapDefinitionTest(key, VALUE1, VALUE2);
        }
    }

    @Test
    public void equalsLdapDefinitionUseFor() {
        String key = JavaEESecConstants.USE_FOR;

        equalsLdapDefinitionTest(key, new ValidationType[] { ValidationType.PROVIDE_GROUPS }, new ValidationType[] { ValidationType.VALIDATE });
        equalsLdapDefinitionTest(key, new ValidationType[] { ValidationType.PROVIDE_GROUPS, ValidationType.VALIDATE }, new ValidationType[] { ValidationType.PROVIDE_GROUPS });
        equalsLdapDefinitionTest(key, new ValidationType[] { ValidationType.PROVIDE_GROUPS, ValidationType.VALIDATE, ValidationType.PROVIDE_GROUPS, ValidationType.VALIDATE }, new ValidationType[] { ValidationType.PROVIDE_GROUPS});

        Map map1 = new HashMap<String, Object>();
        map1.put(key, new ValidationType[] { ValidationType.PROVIDE_GROUPS, ValidationType.VALIDATE });
        Map map2 = new HashMap<String, Object>();
        map2.put(key, new ValidationType[] { ValidationType.VALIDATE, ValidationType.PROVIDE_GROUPS });
        LdapIdentityStoreDefinition lisd1 = getLdapDefinitionForEqualsTest(map1);
        LdapIdentityStoreDefinition lisd2 = getLdapDefinitionForEqualsTest(map2);
        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension();
        assertTrue("the result should be true.", j3ce.equalsLdapDefinition(lisd1, lisd2));
    }

    @Test
    public void processBasicHttpAuthMechNeededVeto() {
        List<Class> cls = new ArrayList<Class>();
        cls.add(ApplicationHAM.class);
        cls.add(CustomFormAuthenticationMechanism.class);
        cls.add(FormAuthenticationMechanism.class);

        final Map<String, ModuleProperties> mm = createModuleMap(cls);

        context.checking(new Expectations() {
            {
                one(pba).veto();
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension() {
            @Override
            protected Map<String, ModuleProperties> getModuleMap() {
                return mm;
            }
        };
        
        j3ce.processBasicHttpAuthMechNeeded(pba, bm);
    }

    @Test
    public void processBasicHttpAuthMechNeededNoVeto() {
        List<Class> cls = new ArrayList<Class>();
        cls.add(ApplicationHAM.class);
        cls.add(CustomFormAuthenticationMechanism.class);
        cls.add(FormAuthenticationMechanism.class);
        cls.add(BasicHttpAuthenticationMechanism.class);

        final Map<String, ModuleProperties> mm = createModuleMap(cls);

        context.checking(new Expectations() {
            {
                never(pba).veto();
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension() {
            @Override
            protected Map<String, ModuleProperties> getModuleMap() {
                return mm;
            }
        };
        
        j3ce.processBasicHttpAuthMechNeeded(pba, bm);
    }

    @Test
    public void processFormAuthMechNeededVeto() {
        List<Class> cls = new ArrayList<Class>();
        cls.add(ApplicationHAM.class);
        cls.add(CustomFormAuthenticationMechanism.class);
        cls.add(BasicHttpAuthenticationMechanism.class);

        final Map<String, ModuleProperties> mm = createModuleMap(cls);

        context.checking(new Expectations() {
            {
                one(pba).veto();
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension() {
            @Override
            protected Map<String, ModuleProperties> getModuleMap() {
                return mm;
            }
        };
        
        j3ce.processFormAuthMechNeeded(pba, bm);
    }

    @Test
    public void processFormAuthMechNeededNoVeto() {
        List<Class> cls = new ArrayList<Class>();
        cls.add(ApplicationHAM.class);
        cls.add(CustomFormAuthenticationMechanism.class);
        cls.add(BasicHttpAuthenticationMechanism.class);
        cls.add(FormAuthenticationMechanism.class);

        final Map<String, ModuleProperties> mm = createModuleMap(cls);

        context.checking(new Expectations() {
            {
                never(pba).veto();
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension() {
            @Override
            protected Map<String, ModuleProperties> getModuleMap() {
                return mm;
            }
        };
        
        j3ce.processFormAuthMechNeeded(pba, bm);
    }

    @Test
    public void processCustomFormAuthMechNeededVeto() {
        List<Class> cls = new ArrayList<Class>();
        cls.add(ApplicationHAM.class);
        cls.add(FormAuthenticationMechanism.class);
        cls.add(BasicHttpAuthenticationMechanism.class);

        final Map<String, ModuleProperties> mm = createModuleMap(cls);

        context.checking(new Expectations() {
            {
                one(pba).veto();
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension() {
            @Override
            protected Map<String, ModuleProperties> getModuleMap() {
                return mm;
            }
        };
        
        j3ce.processCustomFormAuthMechNeeded(pba, bm);
    }

    @Test
    public void processCustomFormAuthMechNeededNoVeto() {
        List<Class> cls = new ArrayList<Class>();
        cls.add(ApplicationHAM.class);
        cls.add(BasicHttpAuthenticationMechanism.class);
        cls.add(FormAuthenticationMechanism.class);
        cls.add(CustomFormAuthenticationMechanism.class);

        final Map<String, ModuleProperties> mm = createModuleMap(cls);

        context.checking(new Expectations() {
            {
                never(pba).veto();
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension() {
            @Override
            protected Map<String, ModuleProperties> getModuleMap() {
                return mm;
            }
        };
        
        j3ce.processCustomFormAuthMechNeeded(pba, bm);
    }

    @Test
    public void equalsDatabaseDefinitionStrings() {
        String KEY[] = {"callerQuery", "dataSourceLookup", "groupsQuery",  JavaEESecConstants.PRIORITY_EXPRESSION, JavaEESecConstants.USE_FOR_EXPRESSION};
        List<String> KEYS = Arrays.asList(KEY);
        String VALUE1="value1";
        String VALUE2="value2";
        for (String key : KEYS) {
            equalsDatabaseDefinitionTest(key, VALUE1, VALUE2);
        }
    }

    @Test
    public void equalsDatabaseDefinitionClasses() {
        equalsDatabaseDefinitionTest("hashAlgorithm", CustomPasswordHash1.class, CustomPasswordHash2.class);
    }

    @Test
    public void equalsDatabaseDefinitionIntegers() {
        equalsDatabaseDefinitionTest(JavaEESecConstants.PRIORITY, new Integer(10), new Integer(20));
    }

    @Test
    public void equalsDatabaseDefinitionHashAlgorithmParameters() {
        String param1[] = {"key1=value1", "key2=value2", "key3=value3"};
        String param2[] = {"key1=value3", "key2=value2", "key3=value1"};

        equalsDatabaseDefinitionTest("hashAlgorithmParameters", param1, param2);
    }

    @Test
    public void equalsDatabaseDefinitionUseFor() {
        String key = JavaEESecConstants.USE_FOR;

        equalsDatabaseDefinitionTest(key, new ValidationType[] { ValidationType.PROVIDE_GROUPS }, new ValidationType[] { ValidationType.VALIDATE });
        equalsDatabaseDefinitionTest(key, new ValidationType[] { ValidationType.PROVIDE_GROUPS, ValidationType.VALIDATE }, new ValidationType[] { ValidationType.PROVIDE_GROUPS });
        equalsDatabaseDefinitionTest(key, new ValidationType[] { ValidationType.PROVIDE_GROUPS, ValidationType.VALIDATE, ValidationType.PROVIDE_GROUPS, ValidationType.VALIDATE }, new ValidationType[] { ValidationType.PROVIDE_GROUPS});

        Map map1 = new HashMap<String, Object>();
        map1.put(key, new ValidationType[] { ValidationType.PROVIDE_GROUPS, ValidationType.VALIDATE });
        Map map2 = new HashMap<String, Object>();
        map2.put(key, new ValidationType[] { ValidationType.VALIDATE, ValidationType.PROVIDE_GROUPS });
        DatabaseIdentityStoreDefinition disd1 = getDatabaseDefinitionForEqualsTest(map1);
        DatabaseIdentityStoreDefinition disd2 = getDatabaseDefinitionForEqualsTest(map2);
        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension();
        assertTrue("the result should be true.", j3ce.equalsDatabaseDefinition(disd1, disd2));
    }

    private void equalsLdapDefinitionTest(String key, Object value1, Object value2) {
        LdapIdentityStoreDefinition lisd1, lisd2;
        Map map1 = new HashMap<String, Object>();
        map1.put(key, value1);
        Map map2 = new HashMap<String, Object>();
        map2.put(key, value2);
        lisd1 = getLdapDefinitionForEqualsTest(map1);
        lisd2 = getLdapDefinitionForEqualsTest(map2);
        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension();
        assertTrue("the result should be true.", j3ce.equalsLdapDefinition(lisd1, lisd1));
        assertFalse("the result should be false.", j3ce.equalsLdapDefinition(lisd1, lisd2));
    }

    private void equalsDatabaseDefinitionTest(String key, Object value1, Object value2) {
        DatabaseIdentityStoreDefinition disd1, disd2;
        Map map1 = new HashMap<String, Object>();
        map1.put(key, value1);
        Map map2 = new HashMap<String, Object>();
        map2.put(key, value2);
        disd1 = getDatabaseDefinitionForEqualsTest(map1);
        disd2 = getDatabaseDefinitionForEqualsTest(map2);
        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension();
        assertTrue("the result should be true.", j3ce.equalsDatabaseDefinition(disd1, disd1));
        assertFalse("the result should be false.", j3ce.equalsDatabaseDefinition(disd1, disd2));
    }

    public @interface InvalidAnnotation {}

    private InvalidAnnotation getIAInstance() {
        InvalidAnnotation ann = new InvalidAnnotation() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return InvalidAnnotation.class;
            }
        };
        return ann;
    }

    private BasicAuthenticationMechanismDefinition getBAMDInstance(final String realmName) {
        BasicAuthenticationMechanismDefinition ann = new BasicAuthenticationMechanismDefinition() {
            @Override
            public String realmName() {
                return realmName;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return BasicAuthenticationMechanismDefinition.class;
            }
        };
        return ann;
    }

    private FormAuthenticationMechanismDefinition getFAMDInstance(final LoginToContinue ltc) {
        FormAuthenticationMechanismDefinition ann = new FormAuthenticationMechanismDefinition() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return FormAuthenticationMechanismDefinition.class;
            }

            @Override
            public LoginToContinue loginToContinue() {
                return ltc;
            }
        };
        return ann;
    }

    private CustomFormAuthenticationMechanismDefinition getCFAMDInstance(final LoginToContinue ltc) {
        CustomFormAuthenticationMechanismDefinition ann = new CustomFormAuthenticationMechanismDefinition() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return CustomFormAuthenticationMechanismDefinition.class;
            }

            @Override
            public LoginToContinue loginToContinue() {
                return ltc;
            }
        };
        return ann;
    }

    private LoginToContinue getLTCInstance(Properties props) {
        LoginToContinue ann = new LoginToContinue() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return LoginToContinue.class;
            }

            @Override
            public String errorPage() {
                // TODO Auto-generated method stub
                return (String)props.get(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE);
            }

            @Override
            public String loginPage() {
                return (String)props.get(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE);
            }

            @Override
            public boolean useForwardToLogin() {
                return (boolean)props.get(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN);
            }

            @Override
            public String useForwardToLoginExpression() {
                return (String)props.get(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION);
            }
        };
        return ann;
    }

    private LdapIdentityStoreDefinition getLISDInstance(final Properties props) {
        LdapIdentityStoreDefinition ann = new LdapIdentityStoreDefinition() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return LdapIdentityStoreDefinition.class;
            }

            @Override
            public String bindDn() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String bindDnPassword() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String callerBaseDn() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String callerNameAttribute() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String callerSearchBase() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String callerSearchFilter() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public LdapSearchScope callerSearchScope() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String callerSearchScopeExpression() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String groupMemberAttribute() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String groupMemberOfAttribute() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String groupNameAttribute() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String groupSearchBase() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String groupSearchFilter() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public LdapSearchScope groupSearchScope() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String groupSearchScopeExpression() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int maxResults() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public String maxResultsExpression() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int priority() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public String priorityExpression() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int readTimeout() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public String readTimeoutExpression() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String url() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public ValidationType[] useFor() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String useForExpression() {
                // TODO Auto-generated method stub
                return null;
            }
        };
        return ann;
    }

    private DatabaseIdentityStoreDefinition getDISDInstance(final String realmName) {
        DatabaseIdentityStoreDefinition ann = new DatabaseIdentityStoreDefinition() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return DatabaseIdentityStoreDefinition.class;
            }

            @Override
            public String callerQuery() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String dataSourceLookup() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String groupsQuery() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Class<? extends PasswordHash> hashAlgorithm() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String[] hashAlgorithmParameters() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int priority() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public String priorityExpression() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public ValidationType[] useFor() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String useForExpression() {
                // TODO Auto-generated method stub
                return null;
            }
        };
        return ann;
    }

    private LdapIdentityStoreDefinition getLdapDefinitionForEqualsTest(final Map<String, Object> overrides) {
        LdapIdentityStoreDefinition annotation = new LdapIdentityStoreDefinition() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String bindDn() {
                return (overrides != null && overrides.containsKey("bindDn")) ? (String) overrides.get("bindDn") : "";
            }

            @Override
            public String bindDnPassword() {
                return (overrides != null && overrides.containsKey("bindDnPassword")) ? (String) overrides.get("bindDnPassword") : "";
            }

            @Override
            public String callerBaseDn() {
                return (overrides != null && overrides.containsKey("callerBaseDn")) ? (String) overrides.get("callerBaseDn") : "";
            }

            @Override
            public String callerNameAttribute() {
                return (overrides != null && overrides.containsKey("callerNameAttribute")) ? (String) overrides.get("callerNameAttribute") : "uid";
            }

            @Override
            public String callerSearchBase() {
                return (overrides != null && overrides.containsKey("callerSearchBase")) ? (String) overrides.get("callerSearchBase") : "";
            }

            @Override
            public String callerSearchFilter() {
                return (overrides != null && overrides.containsKey("callerSearchFilter")) ? (String) overrides.get("callerSearchFilter") : "";

            }

            @Override
            public LdapSearchScope callerSearchScope() {
                return (overrides != null && overrides.containsKey("callerSearchScope")) ? (LdapSearchScope) overrides.get("callerSearchScope") : LdapSearchScope.SUBTREE;
            }

            @Override
            public String callerSearchScopeExpression() {
                return (overrides != null && overrides.containsKey("callerSearchScopeExpression")) ? (String) overrides.get("callerSearchScopeExpression") : "";
            }

            @Override
            public String groupMemberAttribute() {
                return (overrides != null && overrides.containsKey("groupMemberAttribute")) ? (String) overrides.get("groupMemberAttribute") : "member";
            }

            @Override
            public String groupMemberOfAttribute() {
                return (overrides != null && overrides.containsKey("groupMemberOfAttribute")) ? (String) overrides.get("groupMemberOfAttribute") : "memberOf";
            }

            @Override
            public String groupNameAttribute() {
                return (overrides != null && overrides.containsKey("groupNameAttribute")) ? (String) overrides.get("groupNameAttribute") : "cn";
            }

            @Override
            public String groupSearchBase() {
                return (overrides != null && overrides.containsKey("groupSearchBase")) ? (String) overrides.get("groupSearchBase") : "";
            }

            @Override
            public String groupSearchFilter() {
                return (overrides != null && overrides.containsKey("groupSearchFilter")) ? (String) overrides.get("groupSearchFilter") : "";
            }

            @Override
            public LdapSearchScope groupSearchScope() {
                return (overrides != null && overrides.containsKey("groupSearchScope")) ? (LdapSearchScope) overrides.get("groupSearchScope") : LdapSearchScope.SUBTREE;
            }

            @Override
            public String groupSearchScopeExpression() {
                return (overrides != null && overrides.containsKey("groupSearchScopeExpression")) ? (String) overrides.get("groupSearchScopeExpression") : "";
            }

            @Override
            public int maxResults() {
                return (overrides != null && overrides.containsKey("maxResults")) ? (Integer) overrides.get("maxResults") : 1000;
            }

            @Override
            public String maxResultsExpression() {
                return (overrides != null && overrides.containsKey("maxResultsExpression")) ? (String) overrides.get("maxResultsExpression") : "";
            }

            @Override
            public int priority() {
                return (overrides != null && overrides.containsKey(JavaEESecConstants.PRIORITY)) ? (Integer) overrides.get(JavaEESecConstants.PRIORITY) : 80;
            }

            @Override
            public String priorityExpression() {
                return (overrides != null && overrides.containsKey(JavaEESecConstants.PRIORITY_EXPRESSION)) ? (String) overrides.get(JavaEESecConstants.PRIORITY_EXPRESSION) : "";
            }

            @Override
            public int readTimeout() {
                return (overrides != null && overrides.containsKey("readTimeout")) ? (Integer) overrides.get("readTimeout") : 0;
            }

            @Override
            public String readTimeoutExpression() {
                return (overrides != null && overrides.containsKey("readTimeoutExpression")) ? (String) overrides.get("readTimeoutExpression") : "";
            }

            @Override
            public String url() {
                return (overrides != null && overrides.containsKey("url")) ? (String) overrides.get("url") : "";
            }

            @Override
            public ValidationType[] useFor() {
                return (overrides != null
                        && overrides.containsKey(JavaEESecConstants.USE_FOR)) ? (ValidationType[]) overrides.get(JavaEESecConstants.USE_FOR) : new ValidationType[] { ValidationType.PROVIDE_GROUPS,
                                                                                                                                                                      ValidationType.VALIDATE };
            }

            @Override
            public String useForExpression() {
                return (overrides != null && overrides.containsKey(JavaEESecConstants.USE_FOR_EXPRESSION)) ? (String) overrides.get(JavaEESecConstants.USE_FOR_EXPRESSION) : "";
            }

        };

        return annotation;
    }

    private DatabaseIdentityStoreDefinition getDatabaseDefinitionForEqualsTest(final Map<String, Object> overrides) {
        DatabaseIdentityStoreDefinition annotation = new DatabaseIdentityStoreDefinition() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String callerQuery() {
                return (overrides != null && overrides.containsKey(JavaEESecConstants.CALLER_QUERY)) ? (String) overrides.get(JavaEESecConstants.CALLER_QUERY) : "";
            }

            @Override
            public String dataSourceLookup() {
                return (overrides != null
                        && overrides.containsKey(JavaEESecConstants.DS_LOOKUP)) ? (String) overrides.get(JavaEESecConstants.DS_LOOKUP) : JavaEESecConstants.DEFAULT_DS_NAME;
            }

            @Override
            public String groupsQuery() {
                return (overrides != null && overrides.containsKey(JavaEESecConstants.GROUPS_QUERY)) ? (String) overrides.get(JavaEESecConstants.GROUPS_QUERY) : "";
            }

            @Override
            public Class<? extends PasswordHash> hashAlgorithm() {
                return (overrides != null
                        && overrides.containsKey(JavaEESecConstants.PWD_HASH_ALGORITHM)) ? (Class<? extends PasswordHash>) overrides.get(JavaEESecConstants.PWD_HASH_ALGORITHM) : Pbkdf2PasswordHash.class;
            }

            @Override
            public String[] hashAlgorithmParameters() {
                return (overrides != null
                        && overrides.containsKey(JavaEESecConstants.PWD_HASH_PARAMETERS)) ? (String[]) overrides.get(JavaEESecConstants.PWD_HASH_PARAMETERS) : new String[] {};
            }

            @Override
            public int priority() {
                return (overrides != null && overrides.containsKey(JavaEESecConstants.PRIORITY)) ? (Integer) overrides.get(JavaEESecConstants.PRIORITY) : 70;
            }

            @Override
            public String priorityExpression() {
                return (overrides != null && overrides.containsKey(JavaEESecConstants.PRIORITY_EXPRESSION)) ? (String) overrides.get(JavaEESecConstants.PRIORITY_EXPRESSION) : "";
            }

            @Override
            public ValidationType[] useFor() {
                return (overrides != null
                        && overrides.containsKey(JavaEESecConstants.USE_FOR)) ? (ValidationType[]) overrides.get(JavaEESecConstants.USE_FOR) : new ValidationType[] { ValidationType.PROVIDE_GROUPS,
                                                                                                                                                                      ValidationType.VALIDATE };
            }

            @Override
            public String useForExpression() {
                return (overrides != null && overrides.containsKey(JavaEESecConstants.USE_FOR_EXPRESSION)) ? (String) overrides.get(JavaEESecConstants.USE_FOR_EXPRESSION) : "";
            }
        };
        return annotation;
    }

    /**
     *
     *
     *
     **/
    private void createMMDs(Map<URL, ModuleMetaData> mmds) {
        context.checking(new Expectations() {
            {
                one(wmmd1).getJ2EEName();
                will(returnValue(j2en1));
                one(j2en1).getModule();
                will(returnValue(MODULE_NAME1));
                one(wmmd2).getJ2EEName();
                will(returnValue(j2en2));
                one(j2en2).getModule();
                will(returnValue(MODULE_NAME2));
            }
        });
        mmds.put(url1, wmmd1);
        mmds.put(url2, wmmd2);
    }

    private Map<String, ModuleProperties> createModuleMap(List<Class> cls) {
        Map<String, ModuleProperties> mm = new LinkedHashMap<String, ModuleProperties>();
        Iterator<Class> it = cls.iterator();
        int i = 1;
        while(it.hasNext()) {
            Class cl = it.next();
            Map<Class<?>, Properties> amm = new HashMap<Class<?>, Properties>();
            amm.put(cl, new Properties());
            // since location is not used, set null.
            ModuleProperties mp = new ModuleProperties(amm);
            // since module name does not matter, just put some dummy string.
            mm.put(("item-" + Integer.toString(i)), mp);
            i++;
        }
        return mm;
    }

    class HAMClass1 {};
    class HAMClass2 {};
    class ApplicationHAM implements HttpAuthenticationMechanism {
        @Override
        public AuthenticationStatus validateRequest(HttpServletRequest request,
                                             HttpServletResponse response,
                                             HttpMessageContext httpMessageContext) throws AuthenticationException {
            return AuthenticationStatus.SEND_FAILURE;
        }
        @Override
        public AuthenticationStatus secureResponse(HttpServletRequest request,
                                                   HttpServletResponse response,
                                                   HttpMessageContext httpMessageContext) throws AuthenticationException {
            return AuthenticationStatus.SUCCESS;
        }

        @Override
        public void cleanSubject(HttpServletRequest request,
                                 HttpServletResponse response,
                                 HttpMessageContext httpMessageContext) {
        }
    }

    class CustomPasswordHash1 implements PasswordHash {
        @Override
        public  void initialize(Map<String,String> parameters) {
        }
        @Override
        public String generate(char[] password) {
            return null;
        }
        @Override
        public boolean verify(char[] password, String hashedPassword) {
            return true;
        }
    }

    class CustomPasswordHash2 implements PasswordHash {
        @Override
        public  void initialize(Map<String,String> parameters) {
        }
        @Override
        public String generate(char[] password) {
            return null;
        }
        @Override
        public boolean verify(char[] password, String hashedPassword) {
            return false;
        }
    }
}
