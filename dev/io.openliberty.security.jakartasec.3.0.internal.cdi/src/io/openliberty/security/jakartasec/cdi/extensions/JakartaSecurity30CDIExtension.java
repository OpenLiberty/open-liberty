/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import com.ibm.ws.cdi.CDIServiceUtils;
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
        addOidcHttpAuthenticationMechanismBean(oidcAnnotation, annotatedClass);
        addOidcIdentityStore(beanManager);
        addOpenIdContext(beanManager);
    }

    private void addOidcHttpAuthenticationMechanismBean(Annotation annotation, Class<?> annotatedClass) {
        Properties props = new Properties();
        props.put(JakartaSec30Constants.OIDC_ANNOTATION, new OpenIdAuthenticationMechanismDefinitionHolder((OpenIdAuthenticationMechanismDefinition) annotation));
        primarySecurityCDIExtension.addAuthMech(applicationName, annotatedClass, OidcHttpAuthenticationMechanism.class, props);
    }

    private void addOidcIdentityStore(BeanManager beanManager) {
        // TODO: Check for duplicates
        beansToAdd.add(new OidcIdentityStoreBean(beanManager));
    }

    private void addOpenIdContext(BeanManager beanManager) {
        // TODO: Check for duplicates
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
