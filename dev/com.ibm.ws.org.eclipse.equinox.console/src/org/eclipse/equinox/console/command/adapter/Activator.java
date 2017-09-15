/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation, SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * 	   Thomas Watson, IBM Corporation - initial API and implementation
 *     Lazar Kirchev, SAP AG - initial API and implementation   
 *******************************************************************************/

package org.eclipse.equinox.console.command.adapter;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.eclipse.equinox.console.commands.CommandsTracker;
import org.eclipse.equinox.console.commands.DisconnectCommand;
import org.eclipse.equinox.console.commands.EquinoxCommandProvider;
import org.eclipse.equinox.console.commands.HelpCommand;
import org.eclipse.equinox.console.commands.ManCommand;
import org.eclipse.equinox.console.telnet.TelnetCommand;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.framework.console.ConsoleSession;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator implements BundleActivator {
	private ServiceTracker<StartLevel, ?> startLevelManagerTracker;
	private ServiceTracker<ConditionalPermissionAdmin, ?> condPermAdminTracker;
	private ServiceTracker<PermissionAdmin, ?> permissionAdminTracker;
	private ServiceTracker<PackageAdmin, PackageAdmin> packageAdminTracker;
	private static boolean isFirstProcessor = true;
	private static TelnetCommand telnetConnection = null;
	
	private ServiceTracker<CommandProcessor, ServiceTracker<ConsoleSession, CommandSession>> commandProcessorTracker;
	// Tracker for Equinox CommandProviders
	private ServiceTracker<CommandProvider, List<ServiceRegistration<?>>> commandProviderTracker;
	
	private EquinoxCommandProvider equinoxCmdProvider;

	public static class ProcessorCustomizer implements
			ServiceTrackerCustomizer<CommandProcessor, ServiceTracker<ConsoleSession, CommandSession>> {

		private final BundleContext context;

		public ProcessorCustomizer(BundleContext context) {
			this.context = context;
		}

		public ServiceTracker<ConsoleSession, CommandSession> addingService(
				ServiceReference<CommandProcessor> reference) {
			CommandProcessor processor = context.getService(reference);
			if (processor == null)
				return null;
			
			if (isFirstProcessor) {
				isFirstProcessor = false;
				telnetConnection = new TelnetCommand(processor, context);
				telnetConnection.startService();
			} else {
				telnetConnection.addCommandProcessor(processor);
			}
			
			ServiceTracker<ConsoleSession, CommandSession> tracker = new ServiceTracker<ConsoleSession, CommandSession>(context, ConsoleSession.class, new SessionCustomizer(context, processor));
			tracker.open();
			return tracker;
		}

		public void modifiedService(
			ServiceReference<CommandProcessor> reference,
			ServiceTracker<ConsoleSession, CommandSession> service) {
			// nothing
		}

		public void removedService(
			ServiceReference<CommandProcessor> reference,
			ServiceTracker<ConsoleSession, CommandSession> tracker) {
			tracker.close();
			CommandProcessor processor = context.getService(reference);
			telnetConnection.removeCommandProcessor(processor);
		}	
	}

	// Provides support for Equinox ConsoleSessions
	public static class SessionCustomizer implements
			ServiceTrackerCustomizer<ConsoleSession, CommandSession> {
		private final BundleContext context;
		final CommandProcessor processor;
		
		public SessionCustomizer(BundleContext context, CommandProcessor processor) {
			this.context = context;
			this.processor = processor;
		}

		public CommandSession addingService(
				ServiceReference<ConsoleSession> reference) {
			final ConsoleSession equinoxSession = context.getService(reference);
			if (equinoxSession == null)
				return null;
			PrintStream output = new PrintStream(equinoxSession.getOutput());
			final CommandSession gogoSession = processor.createSession(equinoxSession.getInput(), output, output);
			new Thread(new Runnable(){
				public void run() {
                    try {
                    	gogoSession.put("SCOPE", "equinox:*");
                    	gogoSession.put("prompt", "osgi> ");
                        gogoSession.execute("gosh --login --noshutdown");
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {
                        gogoSession.close();
                        equinoxSession.close();
                    }
				}
				
			}, "Equinox Console Session").start();
			return null;
		}

		public void modifiedService(ServiceReference<ConsoleSession> reference,
				CommandSession service) {
			// nothing
		}

		public void removedService(ServiceReference<ConsoleSession> reference,
				CommandSession session) {
			session.close();
		}
	}

	// All commands, provided by an Equinox CommandProvider, are registered as provided by a CommandProviderAdapter.
	public class CommandCustomizer implements
			ServiceTrackerCustomizer<CommandProvider, List<ServiceRegistration<?>>> {

		private BundleContext context;
		public CommandCustomizer(BundleContext context) {
			this.context = context;
		}

		public List<ServiceRegistration<?>> addingService(ServiceReference<CommandProvider> reference) {
			if (reference.getProperty("osgi.command.function") != null) {
				// must be a gogo function already; don' track
				return null;
			}
			CommandProvider command = context.getService(reference);
			try {
				Method[] commandMethods = getCommandMethods(command);

				if (commandMethods.length > 0) {
					List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
					registrations.add(context.registerService(Object.class, new CommandProviderAdapter((CommandProvider) command, commandMethods), getAttributes(commandMethods)));
					return registrations;
				} else {
					context.ungetService(reference);
					return null;
				}
			} catch (Exception e) {
				context.ungetService(reference);
				return null;
			}
		}


		public void modifiedService(ServiceReference<CommandProvider> reference, List<ServiceRegistration<?>> service) {
			// Nothing to do.
		}

		public void removedService(ServiceReference<CommandProvider> reference, List<ServiceRegistration<?>> registrations) {
			for (ServiceRegistration<?> serviceRegistration : registrations) {
				serviceRegistration.unregister();
			}
		}

	}

	public void start(BundleContext context) throws Exception {
		commandProviderTracker = new ServiceTracker<CommandProvider, List<ServiceRegistration<?>>>(context, CommandProvider.class.getName(), new CommandCustomizer(context));
		commandProviderTracker.open();
		commandProcessorTracker = new ServiceTracker<CommandProcessor, ServiceTracker<ConsoleSession,CommandSession>>(context, CommandProcessor.class, new ProcessorCustomizer(context));
		commandProcessorTracker.open();
		
		condPermAdminTracker = new ServiceTracker<ConditionalPermissionAdmin, Object>(context, ConditionalPermissionAdmin.class.getName(), null);
		condPermAdminTracker.open();

		// grab permission admin
		permissionAdminTracker = new ServiceTracker<PermissionAdmin, Object>(context, PermissionAdmin.class.getName(), null);
		permissionAdminTracker.open();

		startLevelManagerTracker = new ServiceTracker<StartLevel, Object>(context, StartLevel.class.getName(), null);
		startLevelManagerTracker.open();

		packageAdminTracker = new ServiceTracker<PackageAdmin, PackageAdmin>(context, PackageAdmin.class, null);
		packageAdminTracker.open();
		
		equinoxCmdProvider = new EquinoxCommandProvider(context, this);
		equinoxCmdProvider.startService();
		
		HelpCommand helpCommand = new HelpCommand(context); 
		helpCommand.startService();
		
		ManCommand manCommand = new ManCommand(context);
		manCommand.startService();
		
		DisconnectCommand disconnectCommand = new DisconnectCommand(context);
		disconnectCommand.startService();
		
		CommandsTracker commandsTracker = new CommandsTracker(context);
		context.registerService(CommandsTracker.class.getName(), commandsTracker, null);

		// Don't explicitly start any bundles - Liberty will do that for us, and 
		// we might get the names wrong
	}

	public StartLevel getStartLevel() {
		return (StartLevel) getServiceFromTracker(startLevelManagerTracker, StartLevel.class.getName());
	}

	public PermissionAdmin getPermissionAdmin() {
		return (PermissionAdmin) getServiceFromTracker(permissionAdminTracker, PermissionAdmin.class.getName());
	}

	public ConditionalPermissionAdmin getConditionalPermissionAdmin() {
		return (ConditionalPermissionAdmin) getServiceFromTracker(condPermAdminTracker, ConditionalPermissionAdmin.class.getName());
	}

	public PackageAdmin getPackageAdmin() {
		return (PackageAdmin) getServiceFromTracker(packageAdminTracker, PackageAdmin.class.getName());
	}

	private static Object getServiceFromTracker(ServiceTracker<?, ?> tracker, String serviceClass) {
		if (tracker == null)
			throw new IllegalStateException("Missing service: " + serviceClass);
		Object result = tracker.getService();
		if (result == null)
			throw new IllegalStateException("Missing service: " + serviceClass);
		return result;
	}

	Method[] getCommandMethods(Object command) {
		ArrayList<Method> names = new ArrayList<Method>();
		Class<?> c = command.getClass();
		Method[] methods = c.getDeclaredMethods();
		for (Method method : methods) {
			if (method.getName().startsWith("_")
					&& method.getModifiers() == Modifier.PUBLIC && !method.getName().equals("_help")) {
				Type[] types = method.getGenericParameterTypes();
				if (types.length == 1
						&& types[0].equals(CommandInterpreter.class)) {
					names.add(method);
				}
			}
		}
		return names.toArray(new Method[names.size()]);
	}

	Dictionary<String, Object> getAttributes(Method[] commandMethods) {
		Dictionary<String, Object> dict = new Hashtable<String, Object>();
		dict.put("osgi.command.scope", "equinox");
		String[] methodNames = new String[commandMethods.length];
		for (int i = 0; i < commandMethods.length; i++) {
			String methodName = "" + commandMethods[i].getName().substring(1);
			methodNames[i] = methodName;
		}

		dict.put("osgi.command.function", methodNames);
		return dict;
	}

	public void stop(BundleContext context) throws Exception {
		commandProviderTracker.close();
		commandProcessorTracker.close();
		if (equinoxCmdProvider != null) {
			equinoxCmdProvider.stopService();
		}

		try {
			telnetConnection.telnet(new String[]{"stop"});
		} catch (Exception e) {
			// expected if the telnet server is not started
		}

	}
}
