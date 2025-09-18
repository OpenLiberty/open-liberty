/*******************************************************************************
 * Copyright (c) 2003, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Christoph Läubrich - Issue #38 - Expose the url of a location as service properties
 *******************************************************************************/

package org.eclipse.osgi.internal.framework;

import static org.eclipse.osgi.internal.debug.Debug.OPTION_DEBUG_SECURITY;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import org.apache.felix.resolver.Logger;
import org.apache.felix.resolver.ResolverImpl;
import org.eclipse.equinox.plurl.Plurl;
import org.eclipse.equinox.plurl.PlurlContentHandlerFactory;
import org.eclipse.equinox.plurl.PlurlStreamHandlerFactory;
import org.eclipse.equinox.plurl.impl.PlurlImpl;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.debug.FrameworkDebugOptions;
import org.eclipse.osgi.internal.framework.legacy.PackageAdminImpl;
import org.eclipse.osgi.internal.framework.legacy.StartLevelImpl;
import org.eclipse.osgi.internal.location.BasicLocation;
import org.eclipse.osgi.internal.location.EquinoxLocations;
import org.eclipse.osgi.internal.permadmin.EquinoxSecurityManager;
import org.eclipse.osgi.internal.permadmin.SecurityAdmin;
import org.eclipse.osgi.internal.url.ContentHandlerFactoryImpl;
import org.eclipse.osgi.internal.url.URLStreamHandlerFactoryImpl;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.localization.BundleLocalization;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.eclipse.osgi.storage.BundleLocalizationImpl;
import org.eclipse.osgi.storage.url.BundleResourceHandler;
import org.eclipse.osgi.storage.url.BundleURLConverter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.condition.Condition;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.resolver.Resolver;
import org.osgi.service.startlevel.StartLevel;

/**
 * This class activates the System Bundle.
 */

public class SystemBundleActivator implements BundleActivator {
	private Plurl plurl;
	private PlurlStreamHandlerFactory plurlStreamHandlerFactory;
	private PlurlContentHandlerFactory plurlContentHandlerFactory;
	private List<ServiceRegistration<?>> registrations = new ArrayList<>(10);
	private SecurityManager setSecurityManagner;

	@SuppressWarnings("deprecation")
	@Override
	public void start(BundleContext bc) throws Exception {
		registrations.clear();

		EquinoxBundle bundle = (EquinoxBundle) bc.getBundle();
		EquinoxContainer equinoxContainer = bundle.getEquinoxContainer();

		EquinoxConfiguration configuration = bundle.getEquinoxContainer().getConfiguration();
		equinoxContainer.getLogServices().start(bc);

		installSecurityManager(configuration);

		equinoxContainer.systemStart(bc);

		plurl = new PlurlImpl();
		try {
			plurl.install();
			URLStreamHandlerFactoryImpl ushf = new URLStreamHandlerFactoryImpl(bc, equinoxContainer);
			ContentHandlerFactoryImpl chf = new ContentHandlerFactoryImpl(bc, equinoxContainer);
			Plurl.add(ushf);
			plurlStreamHandlerFactory = ushf;
			Plurl.add(chf);
			plurlContentHandlerFactory = chf;
		} catch (Exception e) {
			// Don't fail framework launch if we cannot register with plurl.
			// Any handler services will get ignored for this framework instance.
			equinoxContainer.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, e.getMessage(), e);
		}

		FrameworkDebugOptions dbgOptions = (FrameworkDebugOptions) configuration.getDebugOptions();
		dbgOptions.start(bc);
		Hashtable<String, Object> props = new Hashtable<>(7);
		props.clear();

		props.put(Condition.CONDITION_ID, Condition.CONDITION_ID_TRUE);
		register(bc, Condition.class, Condition.INSTANCE, false, props);

		registerLocations(bc, equinoxContainer.getLocations());
		register(bc, EnvironmentInfo.class, equinoxContainer.getConfiguration(), null);
		PackageAdmin packageAdmin = new PackageAdminImpl(equinoxContainer,
				equinoxContainer.getStorage().getModuleContainer().getFrameworkWiring());
		register(bc, PackageAdmin.class, packageAdmin, null);
		StartLevel startLevel = new StartLevelImpl(
				equinoxContainer.getStorage().getModuleContainer().getFrameworkStartLevel());
		register(bc, StartLevel.class, startLevel, null);

		SecurityAdmin sa = equinoxContainer.getStorage().getSecurityAdmin();
		register(bc, PermissionAdmin.class, sa, null);
		register(bc, ConditionalPermissionAdmin.class, sa, null);

		props.clear();
		props.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
		register(bc, Resolver.class, new ResolverImpl(new Logger(0), null), false, props);

		register(bc, DebugOptions.class, dbgOptions, null);

		ClassLoader tccl = equinoxContainer.getContextFinder();
		if (tccl != null) {
			props.clear();
			props.put("equinox.classloader.type", "contextClassLoader"); //$NON-NLS-1$ //$NON-NLS-2$
			register(bc, ClassLoader.class, tccl, props);
		}

		props.clear();
		props.put("protocol", new String[] { BundleResourceHandler.OSGI_ENTRY_URL_PROTOCOL, //$NON-NLS-1$
				BundleResourceHandler.OSGI_RESOURCE_URL_PROTOCOL });
		register(bc, URLConverter.class, new BundleURLConverter(), props);

		register(bc, BundleLocalization.class, new BundleLocalizationImpl(), null);

		boolean setTccl = "true".equals( //$NON-NLS-1$
				bundle.getEquinoxContainer().getConfiguration().getConfiguration("eclipse.parsers.setTCCL", "true")); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			register(bc, "javax.xml.parsers.SAXParserFactory", new XMLParsingServiceFactory(true, setTccl), false, //$NON-NLS-1$
					null);
			register(bc, "javax.xml.parsers.DocumentBuilderFactory", new XMLParsingServiceFactory(false, setTccl), //$NON-NLS-1$
					false, null);
		} catch (NoClassDefFoundError e) {
			// ignore; on a platform with no javax.xml (Java 8 SE compact1 profile)
		}
		bundle.getEquinoxContainer().getStorage().getExtensionInstaller().startExtensionActivators(bc);

		// Add an options listener; we already read the options on initialization.
		// Here we are just allowing the options to change
		props.clear();
		props.put(DebugOptions.LISTENER_SYMBOLICNAME, EquinoxContainer.NAME);
		register(bc, DebugOptionsListener.class, bundle.getEquinoxContainer().getConfiguration().getDebug(), props);
		register(bc, DebugOptionsListener.class, bundle.getModule().getContainer(), props);
	}

	private void installSecurityManager(EquinoxConfiguration configuration) throws BundleException {
		String frameworkSecurityProp = configuration.getConfiguration(Constants.FRAMEWORK_SECURITY);

		if (System.getSecurityManager() != null) {
			if (Constants.FRAMEWORK_SECURITY_OSGI.equals(frameworkSecurityProp)) {
				throw new BundleException("Cannot specify the \"" + Constants.FRAMEWORK_SECURITY //$NON-NLS-1$
						+ "\" configuration property when a security manager is already installed."); //$NON-NLS-1$
			}
			// otherwise, never do anything if there is an existing security manager
			return;
		}

		String javaSecurityProp = configuration.getConfiguration(EquinoxConfiguration.PROP_EQUINOX_SECURITY,
				configuration.getProperty("java.security.manager")); //$NON-NLS-1$

		SecurityManager toInstall = null;
		if (Constants.FRAMEWORK_SECURITY_OSGI.equals(frameworkSecurityProp)) {
			toInstall = new EquinoxSecurityManager();
		} else if (javaSecurityProp != null) {
			switch (javaSecurityProp) {
			case "disallow": //$NON-NLS-1$
			case "allow": //$NON-NLS-1$
				// in both cases someone set the java.security.manager property but
				// not the osgi specific security properties, just ignore
				break;
			case "": //$NON-NLS-1$
			case "default": //$NON-NLS-1$
				toInstall = new SecurityManager(); // use the default one from java
				break;
			default:
				// try to use a specific classname
				try {
					Class<?> clazz = Class.forName(javaSecurityProp);
					toInstall = (SecurityManager) clazz.getConstructor().newInstance();
				} catch (Throwable t) {
					throw new BundleException("Failed to create security manager", t); //$NON-NLS-1$
				}
				break;
			}
		}

		if (configuration.getDebug().DEBUG_SECURITY)
			configuration.getDebug().trace(OPTION_DEBUG_SECURITY, "Setting SecurityManager to: " + toInstall); //$NON-NLS-1$
		try {
			if (toInstall != null) {
				System.setSecurityManager(toInstall);
			}
		} catch (UnsupportedOperationException e) {
			throw new UnsupportedOperationException(
					"Setting the security manager is not allowed. The java.security.manager=allow java property must be set.", //$NON-NLS-1$
					e);
		}
		setSecurityManagner = toInstall;
	}

	private void registerLocations(BundleContext bc, EquinoxLocations equinoxLocations) {
		registerLocation(bc, equinoxLocations.getUserLocation(), EquinoxLocations.PROP_USER_AREA);
		registerLocation(bc, equinoxLocations.getInstanceLocation(), EquinoxLocations.PROP_INSTANCE_AREA);
		registerLocation(bc, equinoxLocations.getConfigurationLocation(), EquinoxLocations.PROP_CONFIG_AREA);
		registerLocation(bc, equinoxLocations.getInstallLocation(), EquinoxLocations.PROP_INSTALL_AREA);
		registerLocation(bc, equinoxLocations.getEclipseHomeLocation(), EquinoxLocations.PROP_HOME_LOCATION_AREA);
	}

	private void registerLocation(BundleContext bc, BasicLocation location, String type) {
		if (location != null) {
			location.register(bc);
		}
	}

	@Override
	public void stop(BundleContext bc) throws Exception {
		EquinoxBundle bundle = (EquinoxBundle) bc.getBundle();

		bundle.getEquinoxContainer().getStorage().getExtensionInstaller().stopExtensionActivators(bc);

		FrameworkDebugOptions dbgOptions = (FrameworkDebugOptions) bundle.getEquinoxContainer().getConfiguration()
				.getDebugOptions();
		dbgOptions.stop(bc);

		if (plurlStreamHandlerFactory != null) {
			try {
				Plurl.remove(plurlStreamHandlerFactory);
			} catch (Exception e) {
				bundle.getEquinoxContainer().getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR,
						e.getMessage(), e);
			}
		}
		if (plurlContentHandlerFactory != null) {
			try {
				Plurl.remove(plurlContentHandlerFactory);
			} catch (Exception e) {
				bundle.getEquinoxContainer().getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR,
						e.getMessage(), e);
			}
		}
		try {
			plurl.uninstall();
		} catch (Exception e) {
			bundle.getEquinoxContainer().getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR,
					e.getMessage(), e);
		}
		// unregister services
		for (ServiceRegistration<?> registration : registrations)
			registration.unregister();
		registrations.clear();
		bundle.getEquinoxContainer().getLogServices().stop(bc);
		unintallSecurityManager();
		bundle.getEquinoxContainer().systemStop(bc);
	}

	private void unintallSecurityManager() {
		if (setSecurityManagner != null && System.getSecurityManager() == setSecurityManagner)
			System.setSecurityManager(null);
		setSecurityManagner = null;
	}

	private void register(BundleContext context, Class<?> serviceClass, Object service,
			Dictionary<String, Object> properties) {
		register(context, serviceClass.getName(), service, true, properties);
	}

	private void register(BundleContext context, Class<?> serviceClass, Object service, boolean setRanking,
			Dictionary<String, Object> properties) {
		register(context, serviceClass.getName(), service, setRanking, properties);
	}

	private void register(BundleContext context, String serviceClass, Object service, boolean setRanking,
			Dictionary<String, Object> properties) {
		if (properties == null)
			properties = new Hashtable<>();
		if (setRanking) {
			properties.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		}
		properties.put(Constants.SERVICE_PID, context.getBundle().getBundleId() + "." + service.getClass().getName()); //$NON-NLS-1$
		registrations.add(context.registerService(serviceClass, service, properties));
	}
}
