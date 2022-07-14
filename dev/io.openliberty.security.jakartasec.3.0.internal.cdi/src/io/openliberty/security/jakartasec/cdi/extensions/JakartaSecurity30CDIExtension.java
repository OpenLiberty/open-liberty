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
import java.util.Properties;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.inject.spi.WithAnnotations;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.javaeesec.cdi.beans.BasicHttpAuthenticationMechanism;
import com.ibm.ws.security.javaeesec.cdi.beans.CustomFormAuthenticationMechanism;
import com.ibm.ws.security.javaeesec.cdi.beans.FormAuthenticationMechanism;
import com.ibm.ws.security.javaeesec.cdi.extensions.CommonJavaEESecCDIExtension;

import io.openliberty.security.jakartasec.JakartaSec30Constants;
import io.openliberty.security.jakartasec.cdi.beans.OidcHttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;

public class JakartaSecurity30CDIExtension<T> extends CommonJavaEESecCDIExtension<T> implements Extension {

    private static final TraceComponent tc = Tr.register(JakartaSecurity30CDIExtension.class);

    public <T> void processAnnotatedOidc(@WithAnnotations({ OpenIdAuthenticationMechanismDefinition.class }) @Observes ProcessAnnotatedType<T> event, BeanManager beanManager) {
        processAnnotatedType(event, beanManager);
    }

    public void processOidcHttpAuthMechNeeded(@Observes ProcessBeanAttributes<OidcHttpAuthenticationMechanism> processBeanAttributes, BeanManager beanManager) {
        if (!existAuthMech(OidcHttpAuthenticationMechanism.class)) {
            processBeanAttributes.veto();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "OidcHttpAuthenticationMechanism is disabled since another HttpAuthorizationMechanism is registered.");
            }
        }
    }

    @Override
    protected boolean isApplicationAuthMech(Class<?> javaClass) {
        if (HttpAuthenticationMechanism.class.isAssignableFrom(javaClass)) {
            if (!OidcHttpAuthenticationMechanism.class.equals(javaClass) &&
                !BasicHttpAuthenticationMechanism.class.equals(javaClass) &&
                !FormAuthenticationMechanism.class.equals(javaClass) &&
                !CustomFormAuthenticationMechanism.class.equals(javaClass) &&
                !HttpAuthenticationMechanism.class.equals(javaClass)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isKnownType(Class<? extends Annotation> annotationType) {
        return OpenIdAuthenticationMechanismDefinition.class.equals(annotationType);
    }

    @Override
    public <T> void processKnownType(BeanManager beanManager, Annotation annotation, Class<? extends Annotation> annotationType, Class<?> annotatedClass) {
        try {
            addOidcHttpAuthenticationMechanismBean(annotation, annotationType, annotatedClass);
            addOidcIdentityStore();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addOidcHttpAuthenticationMechanismBean(Annotation annotation, Class<? extends Annotation> annotationType, Class<?> annotatedClass) {
        Properties props = new Properties();
        props.put(JakartaSec30Constants.OIDC_ANNOTATION, annotation);
        addAuthMech(annotatedClass, OidcHttpAuthenticationMechanism.class, props);
    }

    private void addOidcIdentityStore() {
        // TODO: Register the bean for the the OidcIdentityStore
    }

}
