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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.javaeesec.cdi.extensions.HttpAuthenticationMechanismsTracker;
import com.ibm.ws.security.javaeesec.cdi.extensions.PrimarySecurityCDIExtension;

import io.openliberty.security.jakartasec.JakartaSec30Constants;
import io.openliberty.security.jakartasec.OpenIdAuthenticationMechanismDefinitionHolder;
import io.openliberty.security.jakartasec.cdi.beans.OidcHttpAuthenticationMechanism;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;

/**
 * CDI Extension to process the {@link OpenIdAuthenticationMechanismDefinition} annotation
 * and register beans required for Jakarta Security 3.0.
 */
@Component(service = {},
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "service.vendor=IBM")
public class JakartaSecurity30CDIExtension implements Extension {

    private static final TraceComponent tc = Tr.register(JakartaSecurity30CDIExtension.class);

    private static PrimarySecurityCDIExtension primarySecurityCDIExtension;

    private final Set<Bean> beansToAdd = new HashSet<Bean>();
    private final String applicationName;

    public JakartaSecurity30CDIExtension() {
        applicationName = HttpAuthenticationMechanismsTracker.getApplicationName();
    }

    @SuppressWarnings("static-access")
    @Reference
    protected void setPrimarySecurityCDIExtension(PrimarySecurityCDIExtension primarySecurityCDIExtension) {
        this.primarySecurityCDIExtension = primarySecurityCDIExtension;
        primarySecurityCDIExtension.registerMechanismClass(OidcHttpAuthenticationMechanism.class);
    }

    @Deactivate
    protected void deactivate() {
        primarySecurityCDIExtension.deregisterMechanismClass(OidcHttpAuthenticationMechanism.class);
    }

    public <T> void processAnnotatedOidc(@WithAnnotations({ OpenIdAuthenticationMechanismDefinition.class }) @Observes ProcessAnnotatedType<T> event, BeanManager beanManager) {
        AnnotatedType<T> annotatedType = event.getAnnotatedType();
        Annotation oidcAnnotation = annotatedType.getAnnotation(OpenIdAuthenticationMechanismDefinition.class);
        Class<?> annotatedClass = annotatedType.getJavaClass();
        addOidcHttpAuthenticationMechanismBean(oidcAnnotation, annotatedClass, annotatedType);
        addOidcIdentityStore(beanManager);
        addOpenIdContext(beanManager);
    }

    private <T> void addOidcHttpAuthenticationMechanismBean(Annotation annotation, Class<?> annotatedClass, AnnotatedType<T> annotatedType) {
        Properties props = new Properties();
        props.put(JakartaSec30Constants.OIDC_ANNOTATION, new OpenIdAuthenticationMechanismDefinitionHolder((OpenIdAuthenticationMechanismDefinition) annotation));
        Set<Annotation> annotations = annotatedType.getAnnotations();
        primarySecurityCDIExtension.addAuthMech(applicationName, annotatedClass, OidcHttpAuthenticationMechanism.class, annotations, props);
    }

    private void addOidcIdentityStore(BeanManager beanManager) {
        //TODO: look for better way to check for duplicates
        for (Bean b : beansToAdd) {
            if (OidcIdentityStoreBean.class.equals(b.getClass())) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "OidcIdentityStoreBean already registered.");
                return;
            }
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "adding OidcIdentityStoreBean.");
        beansToAdd.add(new OidcIdentityStoreBean(beanManager));
    }

    private void addOpenIdContext(BeanManager beanManager) {
        //TODO: look for better way to check for duplicates
        for (Bean b : beansToAdd) {
            if (OpenIdContextBean.class.equals(b.getClass())) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "OpenIdContextBean already registered.");
                return;
            }
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "adding OpenIdContextBean.");
        beansToAdd.add(new OpenIdContextBean(beanManager));
    }

    public void processOidcHttpAuthMechNeeded(@Observes ProcessBeanAttributes<OidcHttpAuthenticationMechanism> processBeanAttributes, BeanManager beanManager) {
        // Veto bean if it was registered already
        if (!primarySecurityCDIExtension.existAuthMech(applicationName, OidcHttpAuthenticationMechanism.class)) {
            processBeanAttributes.veto();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "OidcHttpAuthenticationMechanism is disabled since another HttpAuthorizationMechanism is registered.");
            }
        }
    }

    public <T> void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "afterBeanDiscovery : instance : " + Integer.toHexString(this.hashCode()) + " BeanManager : " + Integer.toHexString(beanManager.hashCode()));
        }

        // Verification of mechanisms and registration of ModulePropertiesProviderBean performed in JavaEESecCDIExtension's afterBeanDiscovery()
        for (Bean bean : beansToAdd) {
            afterBeanDiscovery.addBean(bean);
        }
    }
}
