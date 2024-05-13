/*******************************************************************************
 * Copyright (c) 2003, 2018 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.osgi.internal.debug;

import java.io.PrintStream;

import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.util.SupplementDebug;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;

/**
 * This class has debug constants which can be used by the Framework
 * implementation and Adaptor implementations
 * 
 * @since 3.1
 */
public class Debug implements DebugOptionsListener {
	/**
	 * Base debug option key (org.eclispe.osgi).
	 */
	public static final String ECLIPSE_OSGI = EquinoxContainer.NAME;
	/**
	 * General Debug option key.
	 */
	public static final String OPTION_DEBUG_GENERAL = ECLIPSE_OSGI + "/debug"; //$NON-NLS-1$
	/**
	 * Bundle time Debug option key.
	 */
	public static final String OPTION_DEBUG_BUNDLE_TIME = ECLIPSE_OSGI + "/debug/bundleTime"; //$NON-NLS-1$

	/**
	 * Bundle start time Debug option key.
	 */
	public static final String OPTION_DEBUG_BUNDLE_START_TIME = ECLIPSE_OSGI + "/debug/bundleStartTime"; //$NON-NLS-1$
	/**
	 * Loader Debug option key.
	 */
	public static final String OPTION_DEBUG_LOADER = ECLIPSE_OSGI + "/debug/loader"; //$NON-NLS-1$

	/**
	 * Loader Debug option key.
	 */
	public static final String OPTION_DEBUG_LOADER_CDS = ECLIPSE_OSGI + "/debug/loader/cds"; //$NON-NLS-1$

	/**
	 * Storage Debug option key.
	 */
	public static final String OPTION_DEBUG_STORAGE = ECLIPSE_OSGI + "/debug/storage"; //$NON-NLS-1$
	/**
	 * Events Debug option key.
	 */
	public static final String OPTION_DEBUG_EVENTS = ECLIPSE_OSGI + "/debug/events"; //$NON-NLS-1$
	/**
	 * Services Debug option key.
	 */
	public static final String OPTION_DEBUG_SERVICES = ECLIPSE_OSGI + "/debug/services"; //$NON-NLS-1$
	/**
	 * Services Debug option key.
	 */
	public static final String OPTION_DEBUG_HOOKS = ECLIPSE_OSGI + "/debug/hooks"; //$NON-NLS-1$
	/**
	 * Manifest Debug option key.
	 */
	public static final String OPTION_DEBUG_MANIFEST = ECLIPSE_OSGI + "/debug/manifest"; //$NON-NLS-1$
	/**
	 * Filter Debug option key.
	 */
	public static final String OPTION_DEBUG_FILTER = ECLIPSE_OSGI + "/debug/filter"; //$NON-NLS-1$
	/**
	 * Security Debug option key.
	 */
	public static final String OPTION_DEBUG_SECURITY = ECLIPSE_OSGI + "/debug/security"; //$NON-NLS-1$
	/**
	 * Start level Debug option key.
	 */
	public static final String OPTION_DEBUG_STARTLEVEL = ECLIPSE_OSGI + "/debug/startlevel"; //$NON-NLS-1$
	/**
	 * PackageAdmin Debug option key.
	 */
	public static final String OPTION_DEBUG_PACKAGEADMIN = ECLIPSE_OSGI + "/debug/packageadmin"; //$NON-NLS-1$
	/**
	 * PackageAdmin timing Debug option key.
	 */
	public static final String OPTION_DEBUG_PACKAGEADMIN_TIMING = ECLIPSE_OSGI + "/debug/packageadmin/timing"; //$NON-NLS-1$
	/**
	 * Monitor activation Debug option key.
	 */
	public static final String OPTION_MONITOR_ACTIVATION = ECLIPSE_OSGI + "/monitor/activation"; //$NON-NLS-1$

	/**
	 * Monitor lazy activation Debug option key
	 */
	public static final String OPTION_MONITOR_LAZY = ECLIPSE_OSGI + "/monitor/lazy"; //$NON-NLS-1$
	/**
	 * Message bundles Debug option key.
	 */
	public static final String OPTION_DEBUG_MESSAGE_BUNDLES = ECLIPSE_OSGI + "/debug/messageBundles"; //$NON-NLS-1$

	public static final String OPTION_DEBUG_LOCATION = ECLIPSE_OSGI + "/debug/location"; //$NON-NLS-1$

	public static final String OPTION_CACHED_MANIFEST = ECLIPSE_OSGI + "/debug/cachedmanifest"; //$NON-NLS-1$

	public static final String OPTION_DEBUG_SYSTEM_BUNDLE = ECLIPSE_OSGI + "/debug/systemBundle"; //$NON-NLS-1$

	public static final String OPTION_DEBUG_BUNDLE_FILE = ECLIPSE_OSGI + "/debug/bundleFile"; //$NON-NLS-1$

	public static final String OPTION_DEBUG_BUNDLE_FILE_OPEN = ECLIPSE_OSGI + "/debug/bundleFile/open"; //$NON-NLS-1$

	public static final String OPTION_DEBUG_BUNDLE_FILE_CLOSE = ECLIPSE_OSGI + "/debug/bundleFile/close"; //$NON-NLS-1$

	/**
	 * General debug flag.
	 */
	public boolean DEBUG_GENERAL = false; // "debug"
	/**
	 * Bundle time debug flag.
	 */
	public boolean DEBUG_BUNDLE_TIME = false; // "debug.bundleTime"
	/**
	 * Loader debug flag.
	 */
	public boolean DEBUG_LOADER = false; // "debug.loader"
	/**
	 * Loader debug flag.
	 */
	public boolean DEBUG_LOADER_CDS = false; // "debug.loader"
	/**
	 * Storage debug flag.
	 */
	public boolean DEBUG_STORAGE = false; // "debug.storage"
	/**
	 * Events debug flag.
	 */
	public boolean DEBUG_EVENTS = false; // "debug.events"
	/**
	 * Services debug flag.
	 */
	public boolean DEBUG_SERVICES = false; // "debug.services"

	/**
	 * Hooks debug flag.
	 */
	public boolean DEBUG_HOOKS = false; // "debug.hooks"
	/**
	 * Manifest debug flag.
	 */
	public boolean DEBUG_MANIFEST = false; // "debug.manifest"
	/**
	 * Filter debug flag.
	 */
	public boolean DEBUG_FILTER = false; // "debug.filter"
	/**
	 * Security debug flag.
	 */
	public boolean DEBUG_SECURITY = false; // "debug.security"
	/**
	 * Start level debug flag.
	 */
	public boolean DEBUG_STARTLEVEL = false; // "debug.startlevel"
	/**
	 * PackageAdmin debug flag.
	 */
	public boolean DEBUG_PACKAGEADMIN = false; // "debug.packageadmin"
	/**
	 * PackageAdmin timing debug flag.
	 */
	// TODO remove this or use it somewhere
	public boolean DEBUG_PACKAGEADMIN_TIMING = false; // "debug.packageadmin/timing"
	/**
	 * Message debug flag.
	 */
	public boolean DEBUG_MESSAGE_BUNDLES = false; // "/debug/messageBundles"
	/**
	 * Monitor activation debug flag.
	 */
	public boolean MONITOR_ACTIVATION = false; // "monitor/activation"

	public boolean DEBUG_LOCATION = false; // debug/location

	public boolean DEBUG_CACHED_MANIFEST = false;

	public boolean DEBUG_SYSTEM_BUNDLE = false; // debug/systemBundle

	public boolean DEBUG_BUNDLE_FILE = false; // debug/bundleFile

	public boolean DEBUG_BUNDLE_FILE_OPEN = false; // debug/bundleFile/open

	public boolean DEBUG_BUNDLE_FILE_CLOSE = false; // debug/bundleFile/close

	public Debug(DebugOptions dbgOptions) {
		optionsChanged(dbgOptions);
	}

	@Override
	public void optionsChanged(DebugOptions dbgOptions) {
		DEBUG_GENERAL = dbgOptions.getBooleanOption(OPTION_DEBUG_GENERAL, false);
		DEBUG_BUNDLE_TIME = dbgOptions.getBooleanOption(OPTION_DEBUG_BUNDLE_TIME, false)
				|| dbgOptions.getBooleanOption("org.eclipse.core.runtime/timing/startup", false); //$NON-NLS-1$
		DEBUG_LOADER = dbgOptions.getBooleanOption(OPTION_DEBUG_LOADER, false);
		DEBUG_LOADER_CDS = dbgOptions.getBooleanOption(OPTION_DEBUG_LOADER_CDS, false);
		DEBUG_STORAGE = dbgOptions.getBooleanOption(OPTION_DEBUG_STORAGE, false);
		DEBUG_EVENTS = dbgOptions.getBooleanOption(OPTION_DEBUG_EVENTS, false);
		DEBUG_SERVICES = dbgOptions.getBooleanOption(OPTION_DEBUG_SERVICES, false);
		DEBUG_HOOKS = dbgOptions.getBooleanOption(OPTION_DEBUG_HOOKS, false);
		DEBUG_MANIFEST = dbgOptions.getBooleanOption(OPTION_DEBUG_MANIFEST, false);
		SupplementDebug.STATIC_DEBUG_MANIFEST = DEBUG_MANIFEST;
		DEBUG_FILTER = dbgOptions.getBooleanOption(OPTION_DEBUG_FILTER, false);
		DEBUG_SECURITY = dbgOptions.getBooleanOption(OPTION_DEBUG_SECURITY, false);
		DEBUG_STARTLEVEL = dbgOptions.getBooleanOption(OPTION_DEBUG_STARTLEVEL, false);
		DEBUG_PACKAGEADMIN = dbgOptions.getBooleanOption(OPTION_DEBUG_PACKAGEADMIN, false);
		DEBUG_PACKAGEADMIN_TIMING = dbgOptions.getBooleanOption(OPTION_DEBUG_PACKAGEADMIN_TIMING, false)
				|| dbgOptions.getBooleanOption("org.eclipse.core.runtime/debug", false); //$NON-NLS-1$
		DEBUG_MESSAGE_BUNDLES = dbgOptions.getBooleanOption(OPTION_DEBUG_MESSAGE_BUNDLES, false);
		SupplementDebug.STATIC_DEBUG_MESSAGE_BUNDLES = DEBUG_MESSAGE_BUNDLES;
		MONITOR_ACTIVATION = dbgOptions.getBooleanOption(OPTION_MONITOR_ACTIVATION, false);
		DEBUG_LOCATION = dbgOptions.getBooleanOption(OPTION_DEBUG_LOCATION, false);
		DEBUG_CACHED_MANIFEST = dbgOptions.getBooleanOption(OPTION_CACHED_MANIFEST, false);
		DEBUG_SYSTEM_BUNDLE = dbgOptions.getBooleanOption(OPTION_DEBUG_SYSTEM_BUNDLE, false);
		DEBUG_BUNDLE_FILE = dbgOptions.getBooleanOption(OPTION_DEBUG_BUNDLE_FILE, false);
		DEBUG_BUNDLE_FILE_OPEN = dbgOptions.getBooleanOption(OPTION_DEBUG_BUNDLE_FILE_OPEN, false);
		DEBUG_BUNDLE_FILE_CLOSE = dbgOptions.getBooleanOption(OPTION_DEBUG_BUNDLE_FILE_CLOSE, false);
	}

	/**
	 * The PrintStream to print debug messages to.
	 */
	public static PrintStream out = System.out;

	/**
	 * Prints x to the PrintStream
	 */
	public static void print(boolean x) {
		out.print(x);
	}

	/**
	 * Prints x to the PrintStream
	 */
	public static void print(char x) {
		out.print(x);
	}

	/**
	 * Prints x to the PrintStream
	 */
	public static void print(int x) {
		out.print(x);
	}

	/**
	 * Prints x to the PrintStream
	 */
	public static void print(long x) {
		out.print(x);
	}

	/**
	 * Prints x to the PrintStream
	 */
	public static void print(float x) {
		out.print(x);
	}

	/**
	 * Prints x to the PrintStream
	 */
	public static void print(double x) {
		out.print(x);
	}

	/**
	 * Prints x to the PrintStream
	 */
	public static void print(char x[]) {
		out.print(x);
	}

	/**
	 * Prints x to the PrintStream
	 */
	public static void print(String x) {
		out.print(x);
	}

	/**
	 * Prints x to the PrintStream
	 */
	public static void print(Object x) {
		out.print(x);
	}

	/**
	 * Prints x to the PrintStream
	 */
	public static void println(boolean x) {
		out.println(x);
	}

	/**
	 * Prints x to the PrintStream
	 */
	public static void println(char x) {
		out.println(x);
	}

	/**
	 * Prints x to the PrintStream
	 */
	public static void println(int x) {
		out.println(x);
	}

	/**
	 * Prints x to the PrintStream
	 */
	public static void println(long x) {
		out.println(x);
	}

	/**
	 * Prints x to the PrintStream
	 */
	public static void println(float x) {
		out.println(x);
	}

	/**
	 * Prints x to the PrintStream
	 */
	public static void println(double x) {
		out.println(x);
	}

	/**
	 * Prints x to the PrintStream
	 */
	public static void println(char x[]) {
		out.println(x);
	}

	/**
	 * Prints x to the PrintStream
	 */
	public static void println(String x) {
		out.println(x);
	}

	/**
	 * Prints x to the PrintStream
	 */
	public static void println(Object x) {
		out.println(x);
	}

	/**
	 * Prints t to the PrintStream
	 */
	public static void printStackTrace(Throwable t) {
		if (t == null)
			return;
		t.printStackTrace(out);
	}
}
