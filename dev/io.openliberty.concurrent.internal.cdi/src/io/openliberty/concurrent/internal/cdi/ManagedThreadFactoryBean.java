/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
package io.openliberty.concurrent.internal.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.classloading.ClassLoaderIdentifierService;
import com.ibm.ws.kernel.service.util.ServiceCaller;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

import io.openliberty.concurrent.internal.cdi.metadata.MTFDeferredMetaDataFactory;
import io.openliberty.concurrent.internal.qualified.QualifiedResourceFactory;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.PassivationCapable;

/**
 * Bean that delegates to the OSGi service registry to obtain ManagedThreadFactory resources.
 */
public class ManagedThreadFactoryBean implements Bean<ManagedThreadFactory>, PassivationCapable {
    private final static TraceComponent tc = Tr.register(ManagedThreadFactoryBean.class);

    /**
     * Injectable bean types.
     */
    private final Set<Type> beanTypes = Set.of(ManagedThreadFactory.class);

    /**
     * Class loader of the application artifact that defines the managed thread factory definition.
     * Or, if a bean for a default instance then the class loader for the application.
     */
    private final ClassLoader declaringClassLoader;

    /**
     * Metadata of the application artifact that defines the managed thread factory definition.
     */
    private final MetaData declaringMetadata;

    /**
     * Resource factory that creates the resource.
     */
    private final ResourceFactory factory;

    /**
     * Qualifiers for the injection points for this bean.
     */
    private final Set<Annotation> qualifiers;

    /**
     * Construct a new bean for this resource, which is for default ManagedThreadFactory instances
     * at the application level.
     *
     * @param cmd        component metadata from the thread upon which the CDI extension runs.
     * @param extSvc     OSGi service for the Concurrency extension.
     * @param qualifiers qualifiers for the bean.
     */
    ManagedThreadFactoryBean(ComponentMetaData cmd, ConcurrencyExtensionMetadata extSvc, Set<Annotation> qualifiers) {
        this.factory = extSvc.defaultManagedThreadFactoryFactory;
        this.qualifiers = qualifiers;
        this.declaringClassLoader = ServiceCaller.runOnce(ManagedThreadFactoryBean.class, ClassLoaderIdentifierService.class, service -> {
            // Check if an EAR Classloader exists for this component
            ClassLoader found = service.getClassLoader("EARApplication:" + cmd.getJ2EEName().getApplication());
            if (Objects.nonNull(found)) {
                return found;
            }

            // If no EAR Classloader, then check if a WEB Classloader exists
            found = service.getClassLoader("WebModule:" + cmd.getJ2EEName().getApplication() + "#" + cmd.getJ2EEName().getModule());
            if (Objects.nonNull(found)) {
                return found;
            }

            return null; // Should be unreachable
            //TODO add NLS message
        }).orElseThrow(() -> new IllegalStateException("Could not find the classloader for the application " + cmd.getJ2EEName()
                                                       + " during the creation of the default managed thread factory bean."));

        // The Concurrency extension could be running under any module/component of the application.
        ApplicationMetaData amd = cmd.getModuleMetaData().getApplicationMetaData();
        MTFDeferredMetaDataFactory metadataFactory = (MTFDeferredMetaDataFactory) extSvc.mtfMetadataFactory;
        this.declaringMetadata = metadataFactory.createComponentMetadata(amd, declaringClassLoader);
    }

    /**
     * Construct a new bean for this resource.
     *
     * @param factory resource factory.
     * @param extSvc  OSGi service for the Concurrency extension.
     */
    ManagedThreadFactoryBean(QualifiedResourceFactory factory, ConcurrencyExtensionMetadata extSvc) {
        this.factory = factory;
        this.qualifiers = factory.getQualifiers();
        this.declaringClassLoader = factory.getDeclaringClassLoader();

        MetaData mdata = factory.getDeclaringMetadata();
        if (mdata instanceof ApplicationMetaData amd) {
            MTFDeferredMetaDataFactory metadataFactory = (MTFDeferredMetaDataFactory) extSvc.mtfMetadataFactory;
            this.declaringMetadata = metadataFactory.createComponentMetadata(amd, declaringClassLoader);
        } else {
            this.declaringMetadata = mdata;
        }
    }

    @Override
    @Trivial
    public ManagedThreadFactory create(CreationalContext<ManagedThreadFactory> cc) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "create", cc, factory, qualifiers);

        ManagedThreadFactory instance;
        try {
            ResourceInfo info = new MTFBeanResourceInfoImpl(declaringClassLoader, declaringMetadata);
            instance = (ManagedThreadFactory) factory.createResource(info);
        } catch (RuntimeException x) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "create", x);
            throw x;
        } catch (Exception x) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "create", x);
            throw new RuntimeException(x);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "create", instance);
        return instance;
    }

    @Override
    public void destroy(ManagedThreadFactory instance, CreationalContext<ManagedThreadFactory> creationalContext) {
    }

    @Override
    @Trivial
    public Class<ManagedThreadFactory> getBeanClass() {
        return ManagedThreadFactory.class;
    }

    /**
     * @return unique identifier for PassivationCapable.
     */
    @Override
    @Trivial
    public String getId() {
        return new StringBuilder(getClass().getName()) //
                        .append(":").append(qualifiers) //
                        .append(':').append(factory) //
                        .toString();
    }

    @Override
    @Trivial
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    @Trivial
    public String getName() {
        return null;
    }

    @Override
    @Trivial
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    @Trivial
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    @Override
    @Trivial
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    @Trivial
    public Set<Type> getTypes() {
        return beanTypes;
    }

    @Override
    @Trivial
    public boolean isAlternative() {
        return false;
    }

    @Override
    @Trivial
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('@').append(Integer.toHexString(hashCode())) //
                        .append(' ').append(factory) //
                        .append(" with qualifiers ").append(qualifiers) //
                        .toString();
    }
}