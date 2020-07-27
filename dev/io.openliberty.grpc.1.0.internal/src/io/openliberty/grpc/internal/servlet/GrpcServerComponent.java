/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.grpc.internal.servlet;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;

import com.ibm.ws.kernel.feature.FeatureProvisioner;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

import io.grpc.BindableService;
import io.grpc.servlet.GrpcServlet;
import io.openliberty.grpc.internal.GrpcMessages;

@Component(service = { ApplicationStateListener.class,
		ServletContainerInitializer.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class GrpcServerComponent implements ServletContainerInitializer, ApplicationStateListener {

	private static final TraceComponent tc = Tr.register(GrpcServerComponent.class, GrpcMessages.GRPC_TRACE_NAME, GrpcMessages.GRPC_BUNDLE);

	private static HashMap<String, GrpcServletApplication> grpcApplications = new HashMap<String, GrpcServletApplication>();
	private static Object grpcApplicationLock = new Object() {
	};

	private static boolean useSecurity = false;

	private final String FEATUREPROVISIONER_REFERENCE_NAME = "featureProvisioner";

	private final AtomicServiceReference<FeatureProvisioner> _featureProvisioner = new AtomicServiceReference<FeatureProvisioner>(
			FEATUREPROVISIONER_REFERENCE_NAME);

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

	/**
	 * Search for all implementors of io.grpc.BindableService and register them with
	 * the Liberty gRPC server
	 */
	@Override
	public void onStartup(Set<Class<?>> ctx, ServletContext sc) throws ServletException {
		synchronized (grpcApplicationLock) {
			if (!grpcApplications.isEmpty()) {

				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "Attempting to load gRPC services for app {0}", sc.getServletContextName());
				}

				// TODO: optionally load classes with CDI to allow injection in service classes
				// init all other BindableService classes via reflection
				GrpcServletApplication currentApp = grpcApplications.get(((WebApp) sc).getApplicationName());
				if (currentApp != null) {
					Set<String> services = currentApp.getServiceClassNames();
					if (services != null) {
						Map<String, BindableService> grpcServiceClasses = new HashMap<String, BindableService>();
						for (String serviceClassName : services) {
							BindableService service = newServiceInstanceFromClassName(serviceClassName);
							if (service != null) {
								grpcServiceClasses.put(serviceClassName, service);
							}
						}
						if (!grpcServiceClasses.isEmpty()) {
							// keep track of the current application so that we can restart it if <grpcService/> is updated
					        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
							// register URL mappings for each gRPC service we've registered
							for (BindableService service : grpcServiceClasses.values()) {
								String serviceName = service.bindService().getServiceDescriptor().getName();

								// pass all of our grpc service implementors into a new GrpcServlet
								// and register that new Servlet on this context
								GrpcServlet grpcServlet = new GrpcServlet(
										new ArrayList<BindableService>(grpcServiceClasses.values()));
								ServletRegistration.Dynamic servletRegistration = sc.addServlet("grpcServlet" + ":" + serviceName, grpcServlet);
								servletRegistration.setAsyncSupported(true);

								String urlPattern = "/" + serviceName + "/*";
								servletRegistration.addMapping(urlPattern);
								// keep track of this service name -> application path mapping
								currentApp.addServiceName(serviceName, sc.getContextPath(), service.getClass());
								
								Tr.info(tc, "service.available", cmd.getJ2EEName().getApplication(), urlPattern);
							}
					        if (cmd != null) {
					            currentApp.setAppName(cmd.getJ2EEName().getApplication());
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
	}

	/**
	 * Create a new io.grpc.BindableService from a class name
	 * 
	 * @param String classname of the io.grpc.BindableService
	 * @return BindableService or null if the class could not be initialized
	 */
	private BindableService newServiceInstanceFromClassName(String serviceClassName) {
		try {
			// use the TCCL to load app classes
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			Class<?> serviceClass = Class.forName(serviceClassName, true, cl);
			// don't init the class if it's abstract
			if (!Modifier.isAbstract(serviceClass.getModifiers())) {
				// get the no-arg constructor and ensure it's accessible
				Constructor<?> ctor = serviceClass.getDeclaredConstructor();
				ctor.setAccessible(true);
				BindableService service = (BindableService) ctor.newInstance();
				return service;
			}
		} catch (InvocationTargetException | ClassNotFoundException | NoSuchMethodException | SecurityException
				| InstantiationException | IllegalAccessException | IllegalArgumentException e) {
			// FFDC
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "The following class extended io.grpc.BindableService but could not be loaded: {0} due to {1}", serviceClassName, e);
			}
		}
		return null;
	}

	/**
	 * Find all implementors of io.grpc.BindableService and save them off to be
	 * loaded during ServletContainerInitialization
	 * 
	 * @param ApplicationInfo appInfo
	 */
	private void initGrpcServices(ApplicationInfo appInfo) {
		try {
			WebAnnotations webAnno = AnnotationsBetaHelper.getWebAnnotations(appInfo.getContainer());
			AnnotationTargets_Targets annoTargets = webAnno.getAnnotationTargets();
			Set<String> services = annoTargets.getAllImplementorsOf("io.grpc.BindableService");

			if (services != null && !services.isEmpty()) {
				if (!services.isEmpty()) {
					GrpcServletApplication currentApplication = new GrpcServletApplication();
					synchronized (grpcApplicationLock) {
						for (String name : services) {
							// only add the class name
							currentApplication.addServiceClassName(name);
						}
						grpcApplications.put(appInfo.getName(), currentApplication);
					}
				}
			}
		} catch (UnableToAdaptException e) {
			// FFDC
		}
	}

	@Override
	public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
		setSecurityEnabled();
		initGrpcServices(appInfo);
	}

	@Override
	public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
	}

	@Override
	public void applicationStopping(ApplicationInfo appInfo) {
		synchronized (grpcApplicationLock) {
			// clean up any grpc URL mappings
			if (!grpcApplications.isEmpty()) {
				GrpcServletApplication currentApp = grpcApplications.remove(appInfo.getName());
				if (currentApp != null) {
					currentApp.destroy();
				}
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
}
