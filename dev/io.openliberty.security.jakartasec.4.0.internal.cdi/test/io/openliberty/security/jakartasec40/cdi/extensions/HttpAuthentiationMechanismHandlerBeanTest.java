/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec40.cdi.extensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;


//import io.openliberty.security.jakartasec.handlers.HttpAuthenticationMechanismHandlerImpl;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanismHandler;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openliberty.security.jakartasec.cdi.extensions.OidcIdentityStoreBean;
import io.openliberty.security.jakartasec.handlers.HttpAuthenticationMechanismHandlerImpl;
import io.openliberty.security.jakartasec.identitystore.OidcIdentityStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.security.enterprise.identitystore.IdentityStore;

/**
 * Unit tests for the HttpAuthentiationMechanismHandlerBean class.
 */
public class HttpAuthentiationMechanismHandlerBeanTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private HttpAuthenticationMechanismHandlerBean httpAuthenticationMechanismHandlerBean;
    private BeanManager beanManager;
    private OidcIdentityStoreBean oidcIdentityStoreBean;
    
    private HttpAuthenticationMechanismHandlerBean bean;
    
    @Before
    public void setUp() throws Exception {
        beanManager = mockery.mock(BeanManager.class);
        httpAuthenticationMechanismHandlerBean = new HttpAuthenticationMechanismHandlerBean(beanManager);
        bean = new HttpAuthenticationMechanismHandlerBean(beanManager);        
        oidcIdentityStoreBean = new OidcIdentityStoreBean(beanManager);        
    }
    
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testCreate() {
    	// given ...
        @SuppressWarnings("unchecked")
		CreationalContext<HttpAuthenticationMechanismHandler> creationalContext = 
            mockery.mock(CreationalContext.class);

        // when ...
        HttpAuthenticationMechanismHandler handler = httpAuthenticationMechanismHandlerBean.create(creationalContext);

        // then ..
        assertNotNull("Created handler should not be null", handler);
        assertTrue("Created handler should be an instance of HttpAuthenticationMechanismHandlerImpl",
                  handler instanceof HttpAuthenticationMechanismHandlerImpl);
    }

    @Test
    public void testDestroy() {
    	// just verifying that this method doesn't thrown an exception
    	
        // given ...
        @SuppressWarnings("unchecked")    	
        CreationalContext<HttpAuthenticationMechanismHandler> creationalContext = 
            mockery.mock(CreationalContext.class);
        HttpAuthenticationMechanismHandler handler = 
            mockery.mock(HttpAuthenticationMechanismHandler.class);
        
        // when/then ...
        bean.destroy(handler, creationalContext);
    }

    @Test
    public void testGetName() {
    	// given/when ...
        String name = bean.getName();
        
        // then ...
        assertNotNull("Bean name should not be null", name);
        assertTrue("Bean name should contain the class name", 
                  name.contains(HttpAuthenticationMechanismHandlerBean.class.getName()));
        assertTrue("Bean name should contain the type", 
                  name.contains(HttpAuthenticationMechanismHandler.class.getName()));
    }

    @Test
    public void testGetQualifiers() {
    	// given/when ...
        Set<Annotation> qualifiers = bean.getQualifiers();

        // then ...
        assertNotNull("Qualifiers set should not be null", qualifiers);
        assertEquals("Qualifiers set should contain exactly two entries", 2, qualifiers.size());
        
        boolean hasDefault = false;
        boolean hasAny = false;
        
        for (Annotation annotation : qualifiers) {
            if (annotation.annotationType().getSimpleName().equals("Default")) {
                hasDefault = true;
            } else if (annotation.annotationType().getSimpleName().equals("Any")) {
                hasAny = true;
            }
        }
        
        assertTrue("Qualifiers should include @Default", hasDefault);
        assertTrue("Qualifiers should include @Any", hasAny);
    }

    @Test
    public void testGetScope() {
    	// given/when ...
        Class<?> scope = bean.getScope();

        // then ...        
        assertEquals("Scope should be ApplicationScoped", ApplicationScoped.class, scope);
    }

    @Test
    public void testGetStereotypes() {
    	// given/when ...
        Set<Class<? extends Annotation>> stereotypes = bean.getStereotypes();
        
        // then ...
        assertNotNull("Stereotypes set should not be null", stereotypes);
        assertTrue("Stereotypes set should be empty", stereotypes.isEmpty());
    }

    @Test
    public void testGetTypes() {
    	// given/when ...
        Set<Type> types = bean.getTypes();
        
        // then ...
        assertNotNull("Types set should not be null", types);
        assertEquals("Types set should contain exactly one entry", 1, types.size());
        assertEquals("Type should be HttpAuthenticationMechanismHandler", 
                    HttpAuthenticationMechanismHandler.class, types.iterator().next());
    }


    @Test
    public void testIsAlternative() {
    	// given/when/then ...
        assertFalse("Bean should not be an alternative", bean.isAlternative());
    }

    @Test
    public void testGetBeanClass() {
    	//given/when ...
        Class<?> beanClass = bean.getBeanClass();
        
        // then ...
        assertEquals("Bean class should be HttpAuthentiationMechanismHandlerBean", 
                    HttpAuthenticationMechanismHandlerBean.class, beanClass);
    }

    @Test
    public void testGetInjectionPoints() {
    	// given/when ...
        Set<InjectionPoint> injectionPoints = bean.getInjectionPoints();
        
        // then ...
        assertNotNull("Injection points set should not be null", injectionPoints);
        assertTrue("Injection points set should be empty", injectionPoints.isEmpty());
    }

    @Test
    public void testGetId() {
    	// given/when ...
        String id = bean.getId();
        
        // then ...
        assertNotNull("Bean ID should not be null", id);
        assertTrue("Bean ID should contain the bean manager hashcode", 
                  id.contains(String.valueOf(beanManager.hashCode())));
        assertTrue("Bean ID should contain the bean name", 
                  id.contains(bean.getName()));
    }
}    

