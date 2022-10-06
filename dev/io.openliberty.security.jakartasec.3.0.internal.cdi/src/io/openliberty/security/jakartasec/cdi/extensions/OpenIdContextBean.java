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
import java.lang.reflect.Type;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.context.SubjectManager;

import io.openliberty.security.jakartasec.identitystore.OpenIdContextImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.PassivationCapable;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;

@SessionScoped
public class OpenIdContextBean implements Bean<OpenIdContext>, PassivationCapable {

    private static final TraceComponent tc = Tr.register(OpenIdContextBean.class);

    private final Set<Annotation> qualifiers;
    private final Type type;
    private final Set<Type> types;
    private final String name;
    private final String id;

    public OpenIdContextBean(BeanManager beanManager) {
        qualifiers = new HashSet<Annotation>();
        qualifiers.add(new AnnotationLiteral<Default>() {
        });
        type = new TypeLiteral<OpenIdContext>() {
        }.getType();
        types = Collections.singleton(type);
        name = this.getClass().getName() + "@" + this.hashCode() + "[" + type + "]";
        id = beanManager.hashCode() + "#" + this.name;
    }

    @Override
    public OpenIdContext create(CreationalContext<OpenIdContext> arg0) {
        return new OpenIdContextImpl();
    }

    @Override
    public void destroy(OpenIdContext arg0, CreationalContext<OpenIdContext> arg1) {
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public Set<Type> getTypes() {
        return types;
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public Class<?> getBeanClass() {
        return OpenIdContext.class;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public String getId() {
        return id;
    }

    public OpenIdContext getOpenIdContext() {

        Subject subject = null;
        OpenIdContext openIdContext = null;

        try {
            subject = (Subject) java.security.AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    return new SubjectManager().getCallerSubject();
                }
            });
        } catch (PrivilegedActionException pae) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getOpenIdContext() Exception caught: " + pae);
            }
        }

        SubjectHelper subjectHelper = new SubjectHelper();

        if (subject != null && !subjectHelper.isUnauthenticated(subject)) {

            Set<OpenIdContextImpl> openIdContextImplSet = subject.getPrivateCredentials(OpenIdContextImpl.class);

            if (openIdContextImplSet == null || openIdContextImplSet.isEmpty()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getOpenIdContext() Got an authenticated subject, but did not find an OpenIdContextImpl");
                }
            } else {
                if (openIdContextImplSet.size() > 1) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "getOpenIdContext() Multiple OpenIdContextImpl instances on the subject!");
                    }
                }

                openIdContext = openIdContextImplSet.iterator().next();
            }
        } else if (subject == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getOpenIdContext The subject is null from SubjectManager.getCallerSubject");
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getOpenIdContext The subject is not null but is unauthentictaed.");
            }
        }

        if (openIdContext == null) {
            // TODO: do we need to log message or throw an exception here or just trace?

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getOpenIdContext() Did not find an openIdContext, returning null");
            }
        }

        /*
         * TODO: Could be returning null here -- do we want to return an empty or dummy openIdContext or stay with null if not found?
         */
        return openIdContext;
    }

}
