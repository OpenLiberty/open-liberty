/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.grpc.internal.servlet;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.FeatureProvisioner;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.osgi.webapp.WebApp;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.webcontainer.facade.ServletContextFacade;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.servlet.GrpcServlet;
import io.openliberty.grpc.internal.GrpcManagedObjectProvider;
import io.openliberty.grpc.internal.GrpcMessages;
import io.openliberty.grpc.server.monitor.GrpcMonitoringServerInterceptorService;

@Component(service = { ApplicationStateListener.class,
        ServletContainerInitializer.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class GrpcServerComponent implements ServletContainerInitializer, ApplicationStateListener {

    private static final TraceComponent tc = Tr.register(GrpcServerComponent.class, GrpcMessages.GRPC_TRACE_NAME, GrpcMessages.GRPC_BUNDLE);

    private static Map<String, GrpcServletApplication> grpcApplications = new ConcurrentHashMap<String, GrpcServletApplication>();

    private static boolean useSecurity = false;

    private final String FEATUREPROVISIONER_REFERENCE_NAME = "featureProvisioner";

    private final AtomicServiceReference<FeatureProvisioner> _featureProvisioner = new AtomicServiceReference<FeatureProvisioner>(
            FEATUREPROVISIONER_REFERENCE_NAME);

    private static GrpcMonitoringServerInterceptorService monitorService = null;


    @Activate
    protected void activate(ComponentContext cc) {
        _featureProvisioner.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        _featureProvisioner.deactivate(cc);
    }

    @Reference(name = FEATUREPROVISIONER_REFERENCE_NAME, service = FeatureProvisioner.class, cardinality = ReferenceCardinality.MANDATORY)
    protected void setFeatureProvisioner(ServiceReference<FeatureProvisioner> ref) {
        _featureProvisioner.setReference(ref);
    }

    protected void unsetFeatureProvisioner(FeatureProvisioner featureProvisioner) {
    }

    @Reference(name = "GRPC_MONITOR_NAME", service = GrpcMonitoringServerInterceptorService.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY)
    protected void setMonitoringService(GrpcMonitoringServerInterceptorService service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "setMonitoringService");
        }
        monitorService = service;
    }

    protected void unsetMonitoringService(GrpcMonitoringServerInterceptorService service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "unsetMonitoringService");
        }
        if (monitorService == service) {
            monitorService = null;
        }
    }

    /**
     * Search for all implementors of io.grpc.BindableService and register them with
     * the Liberty gRPC server
     */
    @Override
    public void onStartup(Set<Class<?>> ctx, ServletContext sc) throws ServletException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Attempting to load gRPC services for app {0}", sc.getServletContextName());
        }
        IServletContext isc = unwrapServletContext(sc);
        Container container = isc.getModuleContainer();
        if (container != null) {
            GrpcServletApplication currentApp = initGrpcServices(container, isc.getWebAppConfig().getApplicationName());
            if (currentApp != null) {
                // synchronize to protect against GrpcServletApplication.destroy() being invoked concurrently
                synchronized(currentApp) {
                    Set<String> services = currentApp.getServiceClassNames();
                    if (services!= null && !services.isEmpty()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "gRPC services found for app {0}; starting initialization", sc.getServletContextName());
                        }
                        Map<String, BindableService> grpcServiceClasses = new HashMap<String, BindableService>();
                        for (String serviceClassName : services) {
                            BindableService service = newServiceInstanceFromClassName(currentApp, serviceClassName);
                            if (service != null) {
                                grpcServiceClasses.put(serviceClassName, service);
                            }
                        }
                        if (!grpcServiceClasses.isEmpty()) {
                            // keep track of the current application so that we can restart it if <grpcService/> is updated
                            ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
                            if (cmd != null) {
                                currentApp.setAppName(cmd.getJ2EEName().getApplication());
                            }
                            // register URL mappings for each gRPC service we've registered
                            for (BindableService service : grpcServiceClasses.values()) {
                                String serviceName = service.bindService().getServiceDescriptor().getName();
    
                                // pass all of our grpc service implementors into a new GrpcServlet
                                // and register that new Servlet on this context
                                GrpcServlet grpcServlet = new GrpcServlet(
                                        new ArrayList<BindableService>(grpcServiceClasses.values()), isc.getWebAppConfig().getApplicationName());
                                ServletRegistration.Dynamic servletRegistration = sc.addServlet("grpcServlet" + ":" + serviceName, grpcServlet);
                                servletRegistration.setAsyncSupported(true);
    
                                String urlPattern = "/" + serviceName + "/*";
                                servletRegistration.addMapping(urlPattern);
    
                                // keep track of this service name -> application path mapping
                                currentApp.addServiceName(serviceName, sc.getContextPath(), service.getClass());
    
                                Tr.info(tc, "service.available", cmd.getJ2EEName().getApplication(), urlPattern);
                            }
                            return;
                        }
                    }
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No gRPC services have been registered for app {0}", sc.getServletContextName());
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "The module container for {0} was null", ((WebApp) sc).getApplicationName());
        }
    }

    /**
     * Create a new io.grpc.BindableService from a class name
     * 
     * @param String classname of the io.grpc.BindableService
     * @return BindableService or null if the class could not be initialized
     */
    @SuppressWarnings("unchecked")
    private BindableService newServiceInstanceFromClassName(GrpcServletApplication grpcApp, String serviceClassName) {
        try {
            // use the TCCL to load app classes
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class<BindableService> serviceClass = (Class<BindableService>) Class.forName(serviceClassName, true, cl);
            // don't init the class if it's abstract
            if (!Modifier.isAbstract(serviceClass.getModifiers())) {
                // use the managed object service to create a new instance of the BindableService class
                // this enables container/CDI support for the instance
                ManagedObject<BindableService> mo = 
                        (ManagedObject<BindableService>) GrpcManagedObjectProvider.createManagedObject(serviceClass);
                // keep track of the ManagedObject so we can call release() on it during app stop
                grpcApp.addManagedObjectContext(mo.getContext());
                return mo.getObject();
            }
        } catch (ClassNotFoundException | SecurityException | IllegalArgumentException | ManagedObjectException e) {
            // FFDC
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The following class extended io.grpc.BindableService but could not be loaded: {0} due to {1}", 
                        serviceClassName, e);
            }
        }
        return null;
    }

    /**
     * Find implementors of io.grpc.BindableService in the given container, and initialize a 
     * GrpcServletApplication containing the class names
     * 
     * @param container
     * @param appName
     * @return GrpcServletApplication
     */
    @FFDCIgnore(UnableToAdaptException.class)
    private GrpcServletApplication initGrpcServices(Container container, String appName)  {
        try {
            WebAnnotations webAnno = AnnotationsBetaHelper.getWebAnnotations(container);
            AnnotationTargets_Targets annoTargets = webAnno.getAnnotationTargets();
            Set<String> services = annoTargets.getAllImplementorsOf("io.grpc.BindableService");
            if (services != null && !services.isEmpty()) {
                if (!services.isEmpty()) {
                    GrpcServletApplication currentApplication = new GrpcServletApplication();
                    currentApplication.setAppName(appName);
                    for (String name : services) {
                        // only add the class name
                        currentApplication.addServiceClassName(name);
                    }
                    grpcApplications.put(appName, currentApplication);
                    return currentApplication;
                }
            }
        } catch (UnableToAdaptException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Failed to adapt: container=" + container + " : \n" + e.getMessage());
            }
        }
        return null;
    }

    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        setSecurityEnabled();
    }

    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
    }

    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        // clean up any grpc URL mappings
        GrpcServletApplication currentApp = grpcApplications.remove(appInfo.getName());
        if (currentApp != null) {
            synchronized(currentApp) {
                currentApp.destroy();
            }
        }
    }

    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
    }

    /**
     * Set useSecurity to true if any of the appSecurity features are enabled
     */
    private void setSecurityEnabled() {
        Set<String> currentFeatureSet = _featureProvisioner.getService().getInstalledFeatures();
        if (currentFeatureSet.contains("appSecurity-2.0") || currentFeatureSet.contains("appSecurity-1.0")
                || currentFeatureSet.contains("appSecurity-3.0")) {
            useSecurity = true;
            return;
        }
        useSecurity = false;
    }

    public static boolean isSecurityEnabled() {
        return useSecurity;
    }

    /**
     * @param appName 
     * @param serviceName 
     * @return a GrpcMonitoringServerInterceptorService if monitoring is enabled
     */
    public static ServerInterceptor getMonitoringServerInterceptor(String serviceName, String appName) {
        if (monitorService != null) {
            return monitorService.createInterceptor(serviceName, appName);
        }
        return null;
    }

    /**
     * Util to unwrap the ServletContext to get the IServletContext
     * @param context
     * @return
     */
    private static IServletContext unwrapServletContext(ServletContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "unwrapServletContext", "original context->" + context);
        }
        while (context instanceof ServletContextFacade) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "unwrapServletContext", "nested context->" + context);
            }
            context = ((ServletContextFacade) context).getIServletContext();
        }
        return (IServletContext) context;
    }

}
