/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.security.jakartasec.cdi.extensions;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.javaeesec.cdi.extensions.PrimarySecurityCDIExtension;

import io.openliberty.security.jakartasec.JakartaSec30Constants;
import io.openliberty.security.jakartasec.OpenIdAuthenticationMechanismDefinitionHolder;
import io.openliberty.security.jakartasec.cdi.beans.OidcHttpAuthenticationMechanism;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;

public class JakartaSecurity30CDIExtensionTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private PrimarySecurityCDIExtension primarySecurityCDIExtension;
    private JakartaSecurity30CDIExtension jakartaSecurity30CDIExtension;
    private ProcessAnnotatedType<?> event;
    private BeanManager beanManager;
    private AnnotatedType<?> annotatedType;
    private Annotation oidcAnnotation;
    private final Set<Annotation> annotations = new HashSet<Annotation>();
    private AfterBeanDiscovery afterBeanDiscovery;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        primarySecurityCDIExtension = mockery.mock(PrimarySecurityCDIExtension.class);
        beanManager = mockery.mock(BeanManager.class);
        cdiExtensionRegistersMechanismClass();
        jakartaSecurity30CDIExtension = new JakartaSecurity30CDIExtension();
        jakartaSecurity30CDIExtension.setPrimarySecurityCDIExtension(primarySecurityCDIExtension);
    }

    private void cdiExtensionRegistersMechanismClass() {
        mockery.checking(new Expectations() {
            {
                one(primarySecurityCDIExtension).registerMechanismClass(OidcHttpAuthenticationMechanism.class);
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testProcessAnnotatedOidc() {
        withAnnotatedClassEvent();
        cdiExtensionAddsAuthenticationMechanism();

        jakartaSecurity30CDIExtension.processAnnotatedOidc(event, beanManager);
    }

    @Test
    public void testAfterBeanDiscovery() {
        withAnnotatedClassEvent();
        cdiExtensionAddsAuthenticationMechanism();
        cdiExtensionAddsBeans();

        jakartaSecurity30CDIExtension.processAnnotatedOidc(event, beanManager);
        jakartaSecurity30CDIExtension.afterBeanDiscovery(afterBeanDiscovery, beanManager);

    }

    private void withAnnotatedClassEvent() {
        event = mockery.mock(ProcessAnnotatedType.class);
        annotatedType = mockery.mock(AnnotatedType.class);
        oidcAnnotation = mockery.mock(OpenIdAuthenticationMechanismDefinition.class);

        mockery.checking(new Expectations() {
            {
                allowing(event).getAnnotatedType();
                will(returnValue(annotatedType));
                allowing(annotatedType).getAnnotation(OpenIdAuthenticationMechanismDefinition.class);
                will(returnValue(oidcAnnotation));
                allowing(annotatedType).getJavaClass();
                will(returnValue(AnnotatedClass.class));
                allowing(annotatedType).getAnnotations();
            }
        });
    }

    private void cdiExtensionAddsAuthenticationMechanism() {
        mockery.checking(new Expectations() {
            {
                allowing(annotatedType).getAnnotations();
                one(primarySecurityCDIExtension).addAuthMech(with(any(String.class)), with(AnnotatedClass.class), with(OidcHttpAuthenticationMechanism.class),
                                                             with(annotations), with(anOidcAnnotationInProperties(oidcAnnotation)));
            }
        });
    }

    private void cdiExtensionAddsBeans() {
        afterBeanDiscovery = mockery.mock(AfterBeanDiscovery.class);
        mockery.checking(new Expectations() {
            {
                one(afterBeanDiscovery).addBean(with(aNonNull(OidcIdentityStoreBean.class)));
                one(afterBeanDiscovery).addBean(with(aNonNull(OpenIdContextBean.class)));
            }
        });
    }

    class AnnotatedClass {
    }

    private OidcAnnotationInPropertiesMatcher anOidcAnnotationInProperties(Annotation oidcAnnotation) {
        return new OidcAnnotationInPropertiesMatcher(oidcAnnotation);
    }

    class OidcAnnotationInPropertiesMatcher extends BaseMatcher<Properties> {

        private final Annotation oidcAnnotation;

        public OidcAnnotationInPropertiesMatcher(Annotation oidcAnnotation) {
            this.oidcAnnotation = oidcAnnotation;
        }

        @Override
        public boolean matches(Object arg0) {
            boolean result = false;
            if (arg0 instanceof Properties) {
                OpenIdAuthenticationMechanismDefinitionHolder holder = (OpenIdAuthenticationMechanismDefinitionHolder) ((Properties) arg0).get(JakartaSec30Constants.OIDC_ANNOTATION);
                Annotation annotation = holder.getOpenIdAuthenticationMechanismDefinition();
                result = oidcAnnotation == annotation;
            }
            return result;
        }

        @Override
        public void describeTo(Description arg0) {
        }

    }

}
