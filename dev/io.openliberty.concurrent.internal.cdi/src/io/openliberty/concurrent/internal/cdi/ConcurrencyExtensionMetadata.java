/*******************************************************************************
 * Copyright (c) 2021,2024 IBM Corporation and others.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.cdi.extension.CDIExtensionMetadataInternal;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.wsspi.resource.ResourceFactory;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import io.openliberty.concurrent.internal.qualified.QualifiedResourceFactories;
import io.openliberty.concurrent.internal.qualified.QualifiedResourceFactory;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.inject.spi.Extension;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = { CDIExtensionMetadata.class, QualifiedResourceFactories.class })
public class ConcurrencyExtensionMetadata implements CDIExtensionMetadata, CDIExtensionMetadataInternal, QualifiedResourceFactories {
    private static final Set<Class<?>> beanClasses = Set.of(ContextService.class,
                                                            ManagedExecutorService.class,
                                                            ManagedScheduledExecutorService.class,
                                                            ManagedThreadFactory.class);

    /**
     * ResourceFactory for the default ContextService instance: java:comp/DefaultContextService.
     */
    @Reference(target = "(&(id=DefaultContextService)(component.name=com.ibm.ws.context.service))",
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected volatile ResourceFactory defaultContextServiceFactory;

    /**
     * ResourceFactory for the default ManagedExecutorService instance: java:comp/DefaultManagedExecutorService.
     */
    @Reference(target = "(&(id=DefaultManagedExecutorService)(component.name=com.ibm.ws.concurrent.internal.ManagedExecutorServiceImpl))",
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected volatile ResourceFactory defaultManagedExecutorFactory;

    /**
     * ResourceFactory for the default ManagedScheduledExecutorService instance: java:comp/DefaultManagedExecutorService.
     */
    @Reference(target = "(&(id=DefaultManagedScheduledExecutorService)(component.name=com.ibm.ws.concurrent.internal.ManagedScheduledExecutorServiceImpl))",
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected volatile ResourceFactory defaultManagedScheduledExecutorFactory;

    /**
     * ResourceFactory for the default ManagedThreadFactory instance: java:comp/DefaultManagedThreadFactory.
     */
    @Reference(target = "(&(id=DefaultManagedThreadFactory)(component.name=com.ibm.ws.concurrent.internal.ManagedThreadFactoryService))",
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected volatile ResourceFactory defaultManagedThreadFactoryFactory;

    /**
     * Jakarta EE version.
     */
    public static Version eeVersion;

    /**
     * Maintains associations of qualifiers to resource factory for
     * each type of resource and for each JEE name.
     *
     * JEEName -> [qualifiers -> ResourceFactory for ContextService,
     * . . . . . . qualifiers -> ResourceFactory for ManagedExecutorService,
     * . . . . . . qualifiers -> ResourceFactory for ManagedScheduledExecutorService,
     * . . . . . . qualifiers -> ResourceFactory for ManagedThreadFactory ]
     */
    final private Map<String, List<Map<List<String>, QualifiedResourceFactory>>> resourceFactories = new ConcurrentHashMap<>();

    /**
     * Liberty Scheduled Executor.
     */
    public static ScheduledExecutorService scheduledExecutor;

    /**
     * The resource factory builder invokes this method to add a
     * resource factory with qualifiers to be processed by the
     * concurrency CDI extension.
     *
     * @param jeeName         JEE name of the form APP#MODULE or APP.
     *                            // TODO EJBs and component level
     * @param resourceType    type of resource definition
     * @param qualifierNames  names of qualifier annotation classes
     * @param resourceFactory the resource factory
     */
    @Override
    public void add(String jeeName, QualifiedResourceFactory.Type resourceType,
                    List<String> qualifierNames, QualifiedResourceFactory resourceFactory) {
        List<Map<List<String>, QualifiedResourceFactory>> list = resourceFactories.get(jeeName);
        if (list == null) {
            list = List.of(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
            resourceFactories.put(jeeName, list);
        }

        Map<List<String>, QualifiedResourceFactory> qualifiersToResourceFactory = list.get(resourceType.ordinal());
        QualifiedResourceFactory conflict = qualifiersToResourceFactory.put(qualifierNames, resourceFactory);

        if (conflict != null)
            throw new IllegalStateException("The " + jeeName + " application artifact defines multiple " + //
                                            resourceType + " resources with the " + qualifierNames + " qualifiers."); // TODO NLS and Tr.error
    }

    @Override
    public boolean applicationBeansVisible() {
        return true;
    }

    @Override
    public Set<Class<?>> getBeanClasses() {
        return beanClasses;
    }

    @Override
    public Set<Class<? extends Extension>> getExtensions() {
        return Collections.singleton(ConcurrencyExtension.class);
    }

    /**
     * The concurrency CDI extension invokes this method to obtain all
     * of the resource factories so it can register them as beans with
     * their respective qualifiers.
     *
     * @param jeeName JEE name of the form APP#MODULE or APP.
     *                    // TODO EJBs and component level
     * @return list of the form [qualifiers -> ResourceFactory for ContextService,
     *         . . . . . . . . . qualifiers -> ResourceFactory for ManagedExecutorService,
     *         . . . . . . . . . qualifiers -> ResourceFactory for ManagedScheduledExecutorService,
     *         . . . . . . . . . qualifiers -> ResourceFactory for ManagedThreadFactory ]
     */
    @Override
    public List<Map<List<String>, QualifiedResourceFactory>> removeAll(String jeeName) {
        return resourceFactories.remove(jeeName);
    }

    /**
     * The service ranking of JavaEEVersion ensures we get the highest
     * Jakarta EE version for the configured features.
     */
    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    protected void setEEVersion(ServiceReference<JavaEEVersion> ref) {
        String version = (String) ref.getProperty("version");
        eeVersion = Version.parseVersion(version);
    }

    @Reference(target = "(deferrable=false)")
    protected void setScheduledExecutor(ScheduledExecutorService svc) {
        scheduledExecutor = svc;
    }

    protected void unsetScheduledExecutor(ScheduledExecutorService svc) {
    }

    protected void unsetEEVersion(ServiceReference<JavaEEVersion> ref) {
    }
}