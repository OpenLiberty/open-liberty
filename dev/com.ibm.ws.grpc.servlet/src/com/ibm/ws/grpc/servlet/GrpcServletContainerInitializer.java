package com.ibm.ws.grpc.servlet;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;

import io.grpc.BindableService;
import io.grpc.servlet.GrpcServlet;

@Component(service = { ApplicationStateListener.class,
		ServletContainerInitializer.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class GrpcServletContainerInitializer implements ServletContainerInitializer, ApplicationStateListener {

	private static final String CLASS_NAME = GrpcServletContainerInitializer.class.getName();
	private static final Logger logger = Logger.getLogger(GrpcServletContainerInitializer.class.getName());

	private Set<String> grpcServiceClassNames;

	/**
	 * Search for all implementors of io.grpc.BindableService and register them with
	 * the Liberty gRPC server
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void onStartup(Set<Class<?>> ctx, ServletContext sc) throws ServletException {

		if (grpcServiceClassNames != null) {
			logMessage(Level.FINE, "onStartup", "Attempting to load gRPC services on " + sc.getServletContextName());

			Map<String, BindableService> grpcServiceClasses = new HashMap<String, BindableService>();

			// TODO: optionally load classes with CDI to allow injection in service classes
			// loadClassesWithCDI(grpcServiceClasses);

			// init all other BindableService classes via reflection
			for (String serviceClassName : grpcServiceClassNames) {
				if (grpcServiceClasses.containsKey(serviceClassName)) {
					continue;
				}
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
					// TODO: better handle specific exception cases
					logMessage(Level.FINE, "onStartup",
							"caught " + e.getClass().getName() + " attempting to load class " + serviceClassName, e);
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
					logMessage(Level.FINE, "onStartup", "Registered gRPC service at URL: " + urlPattern);
				}
			} else {
				logMessage(Level.FINE, "onStartup",
						"No gRPC services have been registered for " + sc.getServletContextName());
			}
		}
	}

	// TODO: we need to move this logic elsewhere so that this bundle doesn't depend
	// on CDI
//	private void loadClassesWithCDI(Map<String, BindableService> grpcServiceClasses) {
//		if (CDI.current() != null) {
//			Instance<BindableService> grpcServiceInstances = CDI.current().select(BindableService.class);
//			for (BindableService service : grpcServiceInstances) {
//				grpcServiceClasses.put(service.getClass().getName(), service);
//			}
//		}
//	}

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
			grpcServiceClassNames = annoTargets.getAllImplementorsOf("io.grpc.BindableService");
		} catch (UnableToAdaptException e) {
			logMessage(Level.FINE, "initGrpcServices", e.getClass().getName(), e);
		}
	}

	@Override
	public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
		initGrpcServices(appInfo);

		logMessage(Level.FINE, "applicationStarting",
				"gRPC BindableService implementations discovered during applicationStarting: ");
		for (String service : grpcServiceClassNames) {
			logMessage(Level.FINE, "applicationStarting", service);
		}
	}

	@Override
	public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
	}

	@Override
	public void applicationStopping(ApplicationInfo appInfo) {
		if (grpcServiceClassNames != null) {
			grpcServiceClassNames = null;
		}
	}

	@Override
	public void applicationStopped(ApplicationInfo appInfo) {
	}

	private void logMessage(Level level, String method, String message) {
		logMessage(level, method, message, null);
	}

	private void logMessage(Level level, String method, String message, Throwable exception) {
		if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(level)) {
			if (exception != null) {
				logger.logp(level, CLASS_NAME, method, message, exception);
			} else {
				logger.logp(level, CLASS_NAME, method, message);
			}
		}
	}
}
