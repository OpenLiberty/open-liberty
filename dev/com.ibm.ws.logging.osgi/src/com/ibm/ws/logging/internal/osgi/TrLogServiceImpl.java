/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.osgi;

import java.util.EventObject;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * WebSphere implementation of the OSGi log service: One of these per bundle
 * (via TrLogServiceFactory).
 * 
 * This logs directly via Tr before publishing the log entry to other
 * readers/listeners
 */
public class TrLogServiceImpl implements LogService {

	final static class OSGiTraceComponent extends TraceComponent {
		protected OSGiTraceComponent(String logName, Class<?> aClass, String[] groups) {
			super("LogService-" + logName, aClass, groups, OsgiLogConstants.MESSAGE_BUNDLE);
		}
	}

	public static final int LOG_EVENT = -5;

	private static final Object COULD_NOT_OBTAIN_LOCK_EXCEPTION = "Could not obtain lock";

	static boolean publishEvents = false;
	static boolean publishDebugEvents = false;

	static void updatePublishEventConfig(String eventConfig) {
		if ("INFO".equals(eventConfig)) {
			publishEvents = true;
			publishDebugEvents = false;
		} else if ("ALL".equals(eventConfig)) {
			publishEvents = true;
			publishDebugEvents = true;
		} else {
			publishEvents = false;
			publishDebugEvents = false;
		}
	}

	protected final TrLogImpl logImpl;
	protected final Bundle bundle;

	protected final String ffdcMe;
	protected final OSGiTraceComponent tc;

	TrLogServiceImpl(TrLogImpl logImpl, Bundle b) {
		this.logImpl = logImpl;
		this.bundle = b;

		String symName = b.getSymbolicName();
		long id = b.getBundleId();
		if (symName != null) {
			ffdcMe = symName + "-" + b.getVersion();
		} else {
			// if the bundle doesn't have a symbolic name, make something up
			symName = "osgi";
			ffdcMe = "osgi-bundle-" + id;
		}
		String logName = id + "-" + symName;

		// TODO: MORE ICK! the metatype bundle trace really is obnoxious...
		if (symName.startsWith("org.eclipse.equinox.metatype")) {
			tc = new OSGiTraceComponent(logName, this.getClass(), new String[] { symName });
		} else {
			String group = b.getHeaders("").get("WS-TraceGroup");
			if (group == null) {
				tc = new OSGiTraceComponent(logName, this.getClass(),
						new String[] { symName, OsgiLogConstants.LOG_SERVICE_GROUP });
			} else {
				tc = new OSGiTraceComponent(logName, this.getClass(),
						new String[] { symName, group, OsgiLogConstants.LOG_SERVICE_GROUP });
			}
		}
		TrConfigurator.registerTraceComponent(tc);
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[" + ffdcMe + "]";
	}

	/** {@inheritDoc} */
	@Override
	public void log(int level, String message) {
		log(null, level, level, message, null, null);
	}

	/** {@inheritDoc} */
	@Override
	public void log(int level, String message, Throwable exception) {
		log(null, level, level, message, exception, null);
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("rawtypes")
	public void log(ServiceReference sr, int level, String message) {
		log(sr, level, level, message, null, null);
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("rawtypes")
	public void log(ServiceReference sr, int level, String message, Throwable t) {
		log(sr, level, level, message, t, null);
	}

	protected void log(ServiceReference<?> sr, int level, int trLevel, String msg, Throwable t, EventObject event) {
		boolean isAnyTraceEnabled = TraceComponent.isAnyTracingEnabled();

		TrLogEntry logEntry = new TrLogEntry(logImpl, System.currentTimeMillis(), level, msg, bundle, sr, t, event);

		try {
			switch (trLevel) {
			default:
			case LogService.LOG_DEBUG:
				if (isAnyTraceEnabled && tc.isDebugEnabled()) {
					Tr.debug(bundle, tc, logEntry.getMessage(), logEntry.getObjects(false));
				}
				break;

			case LogService.LOG_INFO:
				if (tc.isInfoEnabled())
					Tr.info(tc, "OSGI_MSG001", logEntry.getObjects(true));
				break;

			case LogService.LOG_WARNING:
				Tr.warning(tc, "OSGI_WARNING_MSG", logEntry.getObjects(true));
				break;

			case LogService.LOG_ERROR:
				// BundleException's have good translated messages, so if
				// there's no cause that might provide additional relevant
				// information (e.g., NoClassDefFoundError), then just print
				// the message.
				if (t instanceof BundleException && t.getMessage() != null && t.getCause() == null) {
					Tr.error(tc, "OSGI_BUNDLE_EXCEPTION", t.getMessage());
				} else if (shouldBeLogged(t, logEntry)) {
					Tr.error(tc, "OSGI_ERROR_MSG", logEntry.getObjects(true));
				}
				break;

			case LOG_EVENT:
				if (isAnyTraceEnabled && tc.isEventEnabled()) {
					Tr.event(bundle, tc, logEntry.getMessage(), logEntry.getObjects(false));
				}
				break;
			}
		} catch (Throwable t2) {
			FFDCFilter.processException(t2, ffdcMe, "log", logEntry);
		}

		if (publishEvents && (publishDebugEvents || level != LogService.LOG_DEBUG)) {
			logImpl.publishLogEntry(logEntry);
		}
	}

	/*
	 * Check to see if this exception should be squelched. 
	 */
	private boolean shouldBeLogged(Throwable t, TrLogEntry logEntry) {
		while (t != null) {
			if (t instanceof IllegalStateException && COULD_NOT_OBTAIN_LOCK_EXCEPTION.equals(t.getMessage())) {
				if (tc.isDebugEnabled()) {
					Tr.debug(tc, "DS could not obtain a lock. This is not an error, but may indicate high system load",
							logEntry.getObjects(true));					
				}
				return false;
			}
			t = t.getCause();
		}
		return true;

	}

	/**
	 * @return Bundle associated with this instance
	 */
	public Object getBundle() {
		return bundle;
	}
}
