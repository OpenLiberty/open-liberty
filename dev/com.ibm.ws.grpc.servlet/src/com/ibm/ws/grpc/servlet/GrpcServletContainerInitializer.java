package com.ibm.ws.grpc.servlet;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.grpc.Utils;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;

import io.grpc.BindableService;
import io.grpc.servlet.GrpcServlet;

@Component(service = { ApplicationStateListener.class,
		ServletContainerInitializer.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class GrpcServletContainerInitializer implements ServletContainerInitializer, ApplicationStateListener {

	private static final String CLASS_NAME = GrpcServletContainerInitializer.class.getName();
	private static final Logger logger = Logger.getLogger(GrpcServletContainerInitializer.class.getName());

	private static ConcurrentHashMap<String, GrpcServletApplication>  grpcApplications;
	GrpcServlet grpcServlet;

	/**
	 * Search for all implementors of io.grpc.BindableService and register them with
	 * the Liberty gRPC server
	 */
	@Override
	public void onStartup(Set<Class<?>> ctx, ServletContext sc) throws ServletException {

		if (grpcApplications != null) {
			Utils.traceMessage(logger, CLASS_NAME, Level.FINE, "onStartup",
					"Attempting to load gRPC services for app " + sc.getServletContextName());

			// TODO: optionally load classes with CDI to allow injection in service classes
			// init all other BindableService classes via reflection
			GrpcServletApplication currentApp = grpcApplications.get(((WebApp) sc).getApplicationName());
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
					// pass all of our grpc service implementors into a new GrpcServlet
					// and register that new Servlet on this context
					grpcServlet = new GrpcServlet(new ArrayList<BindableService>(grpcServiceClasses.values()));
					ServletRegistration.Dynamic servletRegistration = sc.addServlet("grpcServlet", grpcServlet);
					servletRegistration.setAsyncSupported(true);

					// register URL mappings for each gRPC service we've registered
					for (BindableService service : grpcServiceClasses.values()) {
						String serviceName = service.bindService().getServiceDescriptor().getName();
						String urlPattern = "/" + serviceName + "/*";
						servletRegistration.addMapping(urlPattern);
						// keep track of this service name -> application path mapping
						currentApp.addServiceName(serviceName, sc.getContextPath());
						Utils.traceMessage(logger, CLASS_NAME, Level.INFO, "onStartup",
								"Registered gRPC service at URL: " + urlPattern);
					}
					return;
				}
			}
		}
		Utils.traceMessage(logger, CLASS_NAME, Level.FINE, "onStartup",
				"No gRPC services have been registered for app " + sc.getServletContextName());
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
			Utils.traceMessage(logger, CLASS_NAME, Level.FINE, "onStartup",
					"The following class extended io.grpc.BindableService but could not be loaded: " + serviceClassName
							+ " due to " + e);
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
					if (grpcApplications == null) {
						grpcApplications = new ConcurrentHashMap<String, GrpcServletApplication>();
					}
					GrpcServletApplication currentApplication = new GrpcServletApplication();
					currentApplication.addServiceClassNames(services);
					grpcApplications.put(appInfo.getName(), currentApplication);
				}
			}
		} catch (UnableToAdaptException e) {
			// FFDC
		}
	}

	@Override
	public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
		initGrpcServices(appInfo);
	}

	@Override
	public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
	}

	@Override
	public void applicationStopping(ApplicationInfo appInfo) {
		grpcServlet = null;
		// clean up any grpc URL mappings
		if (grpcApplications != null) {
			GrpcServletApplication currentApp = grpcApplications.remove(appInfo.getName());
			if (currentApp != null) {
				currentApp.destroy();
				if (grpcApplications.isEmpty()) {
					grpcApplications = null;
				}
			}
		}
	}

	@Override
	public void applicationStopped(ApplicationInfo appInfo) {
	}
}
