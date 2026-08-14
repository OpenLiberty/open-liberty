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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.cdi.extension.CDIExtensionMetadataInternal;
import com.ibm.ws.classloading.ClassLoaderIdentifierService;
import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.metadata.extended.DeferredMetaDataFactory;
import com.ibm.ws.container.service.metadata.extended.MetaDataIdentifierService;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import io.openliberty.concurrent.internal.qualified.QualifiedResourceFactories;
import io.openliberty.concurrent.internal.qualified.QualifiedResourceFactory;
import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.concurrent.Schedule;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Qualifier;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = { ApplicationStateListener.class,
                       CDIExtensionMetadata.class,
                       QualifiedResourceFactories.class })
public class ConcurrencyExtensionMetadata implements //
                ApplicationStateListener, //
                CDIExtensionMetadata, //
                CDIExtensionMetadataInternal, //
                QualifiedResourceFactories {
    private static final TraceComponent tc = //
                    Tr.register(ConcurrencyExtensionMetadata.class);

    private static final Set<Class<?>> BEAN_CLASSES = //
                    Set.of(ContextService.class,
                           ManagedExecutorService.class,
                           ManagedScheduledExecutorService.class,
                           ManagedThreadFactory.class);

    private static final String DEFAULT_MSES_FILTER = //
                    "(&(id=DefaultManagedScheduledExecutorService)(component.name=com.ibm.ws.concurrent.internal.ManagedScheduledExecutorServiceImpl))";

    /**
     * For obtaining the JEE name of the application artifact that provides each
     * bean with a scheduled method. AtomicServiceReference is used to avoid a
     * circular dependency.
     */
    final AtomicServiceReference<CDIService> cdiServiceRef = //
                    new AtomicServiceReference<CDIService>("CDIService");

    /**
     * For obtaining the class loader identifier of a bean with a scheduled method.
     */
    ClassLoaderIdentifierService classloaderIdSvc;

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
    @Reference(target = DEFAULT_MSES_FILTER,
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
     * Map of application name to a
     * map of JEE name to bean types with methods annotated @Schedule
     * that are waiting for the application to start.
     */
    private final Map<String, Map<J2EEName, List<AnnotatedType<?>>>> //
    scheduledMethodsAwaitingAppStart = new ConcurrentHashMap<>();

    /**
     * Metadata identifier service.
     */
    MetaDataIdentifierService metadataIdSvc;

    /**
     * Creates dummy component metadata for ManagedThreadFactories based on the application metadata.
     */
    @Reference(target = "(deferredMetaData=MTF)")
    public volatile DeferredMetaDataFactory mtfMetadataFactory;

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
     * Declarative Services method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context context for this component
     */
    protected void activate(ComponentContext context) {
        cdiServiceRef.activate(context);
    }

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
    public void add(String jeeName,
                    QualifiedResourceFactory.Type resourceType,
                    List<String> qualifierNames,
                    QualifiedResourceFactory resourceFactory) {
        List<Map<List<String>, QualifiedResourceFactory>> list = resourceFactories.get(jeeName);
        if (list == null) {
            list = List.of(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
            resourceFactories.put(jeeName, list);
        }

        Map<List<String>, QualifiedResourceFactory> qualifiersToResourceFactory = //
                        list.get(resourceType.ordinal());
        QualifiedResourceFactory conflict = //
                        qualifiersToResourceFactory.put(qualifierNames, resourceFactory);

        if (conflict != null) {
            List<String> names = List.of(resourceFactory.getName(), conflict.getName());

            Tr.error(tc, "CWWKC1412.qualifier.conflict",
                     jeeName,
                     resourceType,
                     resourceFactory.getQualifiers(),
                     names);

            throw new IllegalStateException(Tr //
                            .formatMessage(tc, "CWWKC1412.qualifier.conflict",
                                           jeeName,
                                           resourceType,
                                           resourceFactory.getQualifiers(),
                                           names));
        }
    }

    @Override
    public boolean applicationBeansVisible() {
        return true;
    }

    @Override
    @Trivial
    public void applicationStarting(ApplicationInfo appInfo) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "applicationStarting " + appInfo.getDeploymentName() +
                               " (" + appInfo.getName() + ')');
    }

    /**
     * Schedule bean methods that are annotated @Schedule.
     */
    @Override
    @Trivial
    public void applicationStarted(ApplicationInfo appInfo) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "applicationStarted " + appInfo.getDeploymentName() +
                               " (" + appInfo.getName() + ')');

        // Use deployment name but fall back to generated name
        String appName = appInfo.getDeploymentName() == null //
                        ? appInfo.getName() //
                        : appInfo.getDeploymentName();

        Map<J2EEName, List<AnnotatedType<?>>> perModule = scheduledMethodsAwaitingAppStart.remove(appName);
        if (perModule != null && !perModule.isEmpty())
            initScheduledMethods(perModule);

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "applicationStarted");
    }

    @Override
    @Trivial
    public void applicationStopping(ApplicationInfo appInfo) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "applicationStopping " + appInfo.getDeploymentName() +
                               " (" + appInfo.getName() + ')');
    }

    @Override
    @Trivial
    public void applicationStopped(ApplicationInfo appInfo) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "applicationStopped" + appInfo.getDeploymentName() +
                               " (" + appInfo.getName() + ')');

        // TODO

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "applicationStopped");
    }

    /**
     * Declarative Services method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context context for this component
     */
    protected void deactivate(ComponentContext context) {
        cdiServiceRef.deactivate(context);
    }

    @Override
    public Set<Class<?>> getBeanClasses() {
        return BEAN_CLASSES;
    }

    @Override
    public Set<Class<? extends Extension>> getExtensions() {
        return Collections.singleton(ConcurrencyExtension.class);
    }

    /**
     * Schedules bean methods that are annotated @Schedule.
     *
     * @param perModule map of module JEE name to AnnotatedType for beans that have
     *                      at least one method annotated Schedule
     */
    void initScheduledMethods(Map<J2EEName, List<AnnotatedType<?>>> perModule) {
        // DefaultManagedScheduledExecutor is used by all Schedule methods
        BundleContext bc = FrameworkUtil//
                        .getBundle(WSManagedExecutorService.class) //
                        .getBundleContext();
        Collection<ServiceReference<ResourceFactory>> refs;
        try {
            refs = bc == null ? //
                            List.of() : //
                            bc.getServiceReferences(ResourceFactory.class,
                                                    DEFAULT_MSES_FILTER);
        } catch (InvalidSyntaxException x) {
            throw new RuntimeException(x); // should be unreachable
        }
        WSManagedExecutorService execSvc = refs.isEmpty() ? //
                        null : //
                        (WSManagedExecutorService) bc //
                                        .getService(refs.iterator().next());
        // TODO consider if we should unget the above on applicaton stop
        // or if it would be safe to do so at the end of this method.
        // Altenatively, possibly reuse defaultManagedScheduledExecutorFactory

        // CDIService can identify which module a bean comes from
        CDIService cdiSvc = cdiServiceRef.getService();

        if (execSvc == null || cdiSvc == null)
            throw new IllegalStateException(); // TODO NLS message if shutting down?

        for (Entry<J2EEName, List<AnnotatedType<?>>> group : perModule.entrySet()) {
            J2EEName moduleJeeName = group.getKey();
            List<AnnotatedType<?>> beanTypes = group.getValue();

            String appName = moduleJeeName.getApplication();
            String modName = moduleJeeName.getModule();
            ClassLoader beanClassLoader = beanTypes.get(0) //
                            .getJavaClass() //
                            .getClassLoader();

            String clIdentifier = classloaderIdSvc //
                            .getClassLoaderIdentifier(beanClassLoader);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "class loader identifier: " + clIdentifier);

            // metadataIdentifier examples:
            // WEB#MyApp#MyWebModule.war
            // EJB#MyApp#MyEJBModule.jar#MyEJB
            String metadataIdentifier = null;

            if (clIdentifier.startsWith("WebModule:")) {
                metadataIdentifier = metadataIdSvc.getMetaDataIdentifier("WEB",
                                                                         appName,
                                                                         modName,
                                                                         null);

                if (!metadataIdSvc.isMetaDataAvailable(metadataIdentifier)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "not available: " + metadataIdentifier);
                    metadataIdentifier = null;
                }
            }
            if (metadataIdentifier == null) {
                metadataIdentifier = metadataIdSvc.getMetaDataIdentifier("EJB",
                                                                         appName,
                                                                         modName,
                                                                         null);
            }

            ComponentMetaData metadata = (ComponentMetaData) metadataIdSvc //
                            .getMetaData(metadataIdentifier);

            // Establish context for the respective module and capture it
            ThreadContextDescriptor threadContext;
            ComponentMetaDataAccessorImpl accessor = //
                            ComponentMetaDataAccessorImpl //
                                            .getComponentMetaDataAccessor();
            if (metadata == null)
                accessor.beginDefaultContext();
            else
                accessor.beginContext(metadata);
            try {
                threadContext = execSvc.captureThreadContext(null);
            } finally {
                accessor.endContext();
            }

            // Schedule each method that is annotated @Schedule
            for (AnnotatedType<?> beanType : beanTypes)
                for (AnnotatedMethod<?> method : beanType.getMethods()) {
                    Class<?> beanClass = beanType.getJavaClass();
                    Schedule schedule = method.getAnnotation(Schedule.class);
                    if (schedule != null &&
                        !beanClass.isAnnotationPresent(Asynchronous.class) &&
                        !method.isAnnotationPresent(Asynchronous.class)) {

                        ArrayList<Annotation> beanAnnoList = new ArrayList<>();
                        for (Annotation beanAnno : beanType.getAnnotations())
                            if (beanAnno.annotationType() //
                                            .isAnnotationPresent(Qualifier.class))
                                beanAnnoList.add(beanAnno);
                        Annotation[] beanAnnos = beanAnnoList //
                                        .toArray(new Annotation[beanAnnoList.size()]);
                        new ScheduledMethod<>( //
                                        method.getJavaMember(), //
                                        schedule, //
                                        threadContext, //
                                        execSvc, //
                                        beanClass, //
                                        beanAnnos);
                    }
                } // let Asynchronous handle the invalid combination of annos
        }
    }

    /**
     * Register to have methods annotated @Schedule scheduled when the given
     * application starts.
     *
     * @param appName               name of the application
     * @param annotatedWithSchedule map of module Java EE name to bean types
     *                                  within the module that have at least
     *                                  one method annotated Schedule
     */
    void scheduleOnAppStart(String appName,
                            Map<J2EEName, List<AnnotatedType<?>>> annotatedWithSchedule) {
        scheduledMethodsAwaitingAppStart.put(appName, annotatedWithSchedule);
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
     * Declarative Services method for setting the CDIService service reference.
     *
     * @param ref reference to the service
     */
    @Reference(service = CDIService.class)
    protected void setCDIService(ServiceReference<CDIService> ref) {
        cdiServiceRef.setReference(ref);
    }

    @Reference
    protected void setClassLoaderIdService(ClassLoaderIdentifierService clIdSvc) {
        this.classloaderIdSvc = clIdSvc;
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

    @Reference
    protected void setMetaDataIdService(MetaDataIdentifierService metadataIdSvc) {
        this.metadataIdSvc = metadataIdSvc;
    }

    @Reference(target = "(deferrable=false)")
    protected void setScheduledExecutor(ScheduledExecutorService svc) {
        scheduledExecutor = svc;
    }

    /**
     * Declarative Services method for unsetting the CDIService service reference.
     *
     * @param ref reference to the service
     */
    protected void unsetCDIService(ServiceReference<CDIService> ref) {
        cdiServiceRef.setReference(ref);
    }

    protected void unsetClassLoaderIdService(ClassLoaderIdentifierService clIdSvc) {
        if (this.classloaderIdSvc == clIdSvc)
            this.classloaderIdSvc = null;
    }

    protected void unsetEEVersion(ServiceReference<JavaEEVersion> ref) {
    }

    protected void unsetMetaDataIdService(MetaDataIdentifierService metadataIdSvc) {
        if (this.metadataIdSvc == metadataIdSvc)
            this.metadataIdSvc = null;
    }

    protected void unsetScheduledExecutor(ScheduledExecutorService svc) {
    }
}