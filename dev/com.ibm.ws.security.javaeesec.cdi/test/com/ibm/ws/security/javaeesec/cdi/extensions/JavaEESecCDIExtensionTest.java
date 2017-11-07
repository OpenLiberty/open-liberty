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

package com.ibm.ws.security.javaeesec.cdi.extensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
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
import javax.enterprise.util.TypeLiteral;
import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;
import javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStore.ValidationType;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;
import javax.security.enterprise.identitystore.PasswordHash;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import test.common.SharedOutputManager;

public class JavaEESecCDIExtensionTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery context = new JUnit4Mockery();
    private final ProcessAnnotatedType pat = context.mock(ProcessAnnotatedType.class, "pat1");
    private final BeanManager bm = context.mock(BeanManager.class, "bm1");
    private final AnnotatedType at = context.mock(AnnotatedType.class, "at1");
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

    @Before
    public void setUp() {}

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
                one(pat).getAnnotatedType();
                will(returnValue(at));
                one(at).getJavaClass();
                will(returnValue(String.class));
                one(at).getAnnotations();
                will(returnValue(aset));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension();
        j3ce.processAnnotatedType(pat, bm);
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
                one(at).getJavaClass();
                will(returnValue(String.class));
                one(pat).getAnnotatedType();
                will(returnValue(at));
                one(at).getAnnotations();
                will(returnValue(aset));
            }
        });

        JavaEESecCDIExtension j3ce = new JavaEESecCDIExtension();
        j3ce.processAnnotatedType(pat, bm);
        Set<Bean> beans = j3ce.getBeansToAdd();
        assertEquals("incorrect number of beans in beansToAdd after processAnnotatedType", 1, beans.size());
        assertTrue("incorrect beansToAdd value after processAnnotatedType", beans.iterator().next().getClass().equals(LdapIdentityStoreBean.class));
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
                between(2,3).of(bn).getBeanClass();
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
                between(2,3).of(bn).getBeanClass();
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
                between(2,3).of(bn).getBeanClass();
                will(returnValue(Object.class));
                exactly(2).of(bn).getTypes();
                will(returnValue(types));
                one(pb2).getBean();
                will(returnValue(bn2));
                between(1,2).of(bn2).getBeanClass();
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
                between(2,3).of(bn).getBeanClass();
                will(returnValue(Object.class));
                exactly(2).of(bn).getTypes();
                will(returnValue(types));
                one(pb2).getBean();
                will(returnValue(bn2));
                between(1,2).of(bn2).getBeanClass();
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
                between(1,2).of(bn).getBeanClass();
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

    @Test
    public void isApplicationIdentityStoreTrue() {
        final Set<Type> types = new HashSet<Type>();
        types.add(new TypeLiteral<Bean>() {}.getType());
        types.add(new TypeLiteral<IdentityStore>() {}.getType());

        context.checking(new Expectations() {
            {
                one(pb).getBean();
                will(returnValue(bn));
                between(1,2).of(bn).getBeanClass();
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
                return null;
            }

            @Override
            public String loginPage() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public boolean useForwardToLogin() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public String useForwardToLoginExpression() {
                // TODO Auto-generated method stub
                return null;
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


}
