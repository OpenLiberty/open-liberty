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

package com.ibm.ws.grpc;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.grpc.Utils;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;

import io.grpc.BindableService;
import io.grpc.Server;

@Component(service = {
		ApplicationStateListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class GrpcApplicationManager implements ApplicationStateListener {

	private static final String CLASS_NAME = GrpcApplicationManager.class.getName();
	private static final Logger logger = Logger.getLogger(GrpcApplicationManager.class.getName());

	private static final String BINDABLE_SERVICE_INTERFACE = "io.grpc.BindableService";

	@Activate
	protected void activate(ComponentContext cc) {
	}

	@Deactivate
	protected void deactivate(ComponentContext cc) {
	}

	@Modified
	protected void modified(Map<?, ?> newProperties) {
	}

	/**
	 * Discover and start gRPC services provided by this application
	 */
	@Override
	public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
		Utils.traceMessage(logger, CLASS_NAME, Level.FINE, "applicationStarting",
				"starting application: " + appInfo.getName());

		Set<String> grpcServiceClassNames = findGrpcServiceImplementors(appInfo);
		Set<BindableService> services = initGrpcServices(appInfo, grpcServiceClassNames);
		if (services != null && !services.isEmpty()) {
			LibertyGrpcServerBuilder serverBuilder = new LibertyGrpcServerBuilder();
			for (BindableService service : services) {
				Utils.traceMessage(logger, CLASS_NAME, Level.INFO, "applicationStarting",
						"adding service: " + service + " for application " + appInfo.getName());
				serverBuilder.addService(service.bindService());
			}
			Server server = serverBuilder.build();
			try {
				server.start();
				ActiveGrpcServers.addServer(appInfo.getName(), server);
			} catch (IOException e) {
				// FFDC
				Utils.traceMessage(logger, CLASS_NAME, Level.FINE, "applicationStarting",
						"gRPC Server " + server + " could not be started ", e);
			}
		}
	}

	@Override
	public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
	}

	@Override
	public void applicationStopping(ApplicationInfo appInfo) {
		Utils.traceMessage(logger, CLASS_NAME, Level.FINE, "applicationStopping",
				"stopping application: " + appInfo.getName());
		
		Server s = ActiveGrpcServers.getServer(appInfo.getName());
		if (s != null) { 
			Utils.traceMessage(logger, CLASS_NAME, Level.FINE, "applicationStopping",
					"shutting down gRPC server: " + s);
			s.shutdown();
			ActiveGrpcServers.removeServer(appInfo.getName());
		}
	}

	@Override
	public void applicationStopped(ApplicationInfo appInfo) {
	}
	
	/**
	 * Find all implementors of io.grpc.BindableService in the current application
	 * 
	 * @param ApplicationInfo appInfo
	 * @return Set<String> classnames of implementors of io.grpc.BindableService
	 */
	private Set<String> findGrpcServiceImplementors(ApplicationInfo appInfo) {
		try {
			WebAnnotations webAnno = AnnotationsBetaHelper.getWebAnnotations(appInfo.getContainer());
			AnnotationTargets_Targets annoTargets = webAnno.getAnnotationTargets();
			Set<String> serviceClassNames = annoTargets.getAllImplementorsOf(BINDABLE_SERVICE_INTERFACE);
			return serviceClassNames;
		} catch (UnableToAdaptException e) {
			// FFDC
		}
		return null;
	}

	/**
	 *  Attempt to load and init the set of classes implementing io.grpc.BindableService in this application 
	 * 
	 * @param ApplicationInfo appInfo
	 * @param Set<String> grpcServiceClassNames 
	 * @return Set<BindableService> the set of BindableServices for this application
	 */
	private Set<BindableService> initGrpcServices(ApplicationInfo appInfo, Set<String> grpcServiceClassNames) {

		if (grpcServiceClassNames != null && !grpcServiceClassNames.isEmpty()) {
			Set<BindableService> services = new HashSet<BindableService>();

			Utils.traceMessage(logger, CLASS_NAME, Level.FINE, "initGrpcServices",
					"gRPC BindableService implementations discovered during applicationStarting: ");
			for (String service : grpcServiceClassNames) {
				Utils.traceMessage(logger, CLASS_NAME, Level.FINE, "initGrpcServices", service);
			}
			try {
				ClassLoader cl = null;
				NonPersistentCache overlayCache = appInfo.getContainer().adapt(NonPersistentCache.class);
				WebModuleInfo moduleInfo = (WebModuleInfo) overlayCache.getFromCache(WebModuleInfo.class);
				if (moduleInfo != null) {
					cl = moduleInfo.getClassLoader();
				}
				for (String serviceClassName : grpcServiceClassNames) {
					BindableService s = newServiceInstanceFromClassName(cl, serviceClassName);
					if (s != null) {
						services.add(s);
					}
				}
				return services;
			} catch (UnableToAdaptException e) {
				// FFDC
				return null;
			}
		}
		return null;
	}

	/**
	 * Create a new io.grpc.BindableService from a class name
	 *
	 * @param ClassLoader
	 * @param String classname of the io.grpc.BindableService
	 * @return BindableService or null if the class could not be initialized
	 */
	private BindableService newServiceInstanceFromClassName(ClassLoader loader, String serviceClassName) {
		try {
			Class<?> serviceClass = Class.forName(serviceClassName, true, loader);
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
			Utils.traceMessage(logger, CLASS_NAME, Level.FINE, "onStartup",
					"The following class extended io.grpc.BindableService but could not be loaded: " + serviceClassName
							+ " due to " + e);
		}
		return null;
	}

}
