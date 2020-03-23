package com.ibm.ws.grpc.servlet;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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

	private static ConcurrentHashMap<String, Set<String>> grpcServiceClassNames;

	/**
	 * Search for all implementors of io.grpc.BindableService and register them with
	 * the Liberty gRPC server
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void onStartup(Set<Class<?>> ctx, ServletContext sc) throws ServletException {

		if (grpcServiceClassNames != null) {
			Utils.traceMessage(logger, CLASS_NAME, Level.FINE, "onStartup", "Attempting to load gRPC services for app " + sc.getServletContextName());

			Map<String, BindableService> grpcServiceClasses = new HashMap<String, BindableService>();

			// TODO: optionally load classes with CDI to allow injection in service classes
			// init all other BindableService classes via reflection
			Set<String> services = grpcServiceClassNames.get(((WebApp)sc).getApplicationName());
			if (services != null) {
				for (String serviceClassName : services) {
					try {
						// use the TCCL to load app classes
						ClassLoader cl = Thread.currentThread().getContextClassLoader();
						Class serviceClass = Class.forName(serviceClassName, true, cl);
						// get the no-arg constructor and ensure it's accessible
						Constructor ctor = serviceClass.getDeclaredConstructor();
						ctor.setAccessible(true);
						BindableService service = (BindableService) ctor.newInstance();
						grpcServiceClasses.put(serviceClassName, service);
					} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
							| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						// FFDC
					}
				}
				if (!grpcServiceClasses.isEmpty()) {
					// pass all of our grpc service implementors into a new GrpcServlet
					// and register that new Servlet on this context
					ServletRegistration.Dynamic servletRegistration = sc.addServlet("grpcServlet",
							new GrpcServlet(new ArrayList(grpcServiceClasses.values())));
					servletRegistration.setAsyncSupported(true);

					// register URL mappings for each gRPC service we've registered
					for (BindableService service : grpcServiceClasses.values()) {
						String serviceName = service.bindService().getServiceDescriptor().getName();
						String urlPattern = "/" + serviceName + "/*";
						servletRegistration.addMapping(urlPattern);
						Utils.traceMessage(logger, CLASS_NAME, Level.INFO, "onStartup", "Registered gRPC service at URL: " + urlPattern);
					}
				}
			} else {
				Utils.traceMessage(logger, CLASS_NAME, Level.FINE, "onStartup",
						"No gRPC services have been registered for app " + sc.getServletContextName());
			}
		}
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
				// TODO: we'll ignore inner classes for now, but this should be revisited
				services.removeIf((String s) -> s.contains("$"));
				if (!services.isEmpty()) {
					if (grpcServiceClassNames == null) {
						grpcServiceClassNames = new ConcurrentHashMap<String, Set<String>>();
					}
					grpcServiceClassNames.put(appInfo.getName(), services);
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
		grpcServiceClassNames = null;
	}

	@Override
	public void applicationStopped(ApplicationInfo appInfo) {
	}
}
