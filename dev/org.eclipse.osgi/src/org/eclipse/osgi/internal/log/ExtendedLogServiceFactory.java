/*******************************************************************************
 * Copyright (c) 2006, 2021 Cognos Incorporated, IBM Corporation and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.osgi.internal.log;

import static org.eclipse.osgi.internal.log.EquinoxLogServices.EQUINOX_LOGGER_NAME;
import static org.eclipse.osgi.internal.log.EquinoxLogServices.PERF_LOGGER_NAME;

import java.security.AccessController;
import java.security.Permission;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.eclipse.equinox.log.ExtendedLogService;
import org.eclipse.equinox.log.LogPermission;
import org.eclipse.osgi.framework.util.SecureAction;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.Logger;
import org.osgi.service.log.admin.LoggerAdmin;
import org.osgi.service.log.admin.LoggerContext;

public class ExtendedLogServiceFactory implements ServiceFactory<ExtendedLogService>, BundleListener {
	static final SecureAction secureAction = AccessController.doPrivileged(SecureAction.createSecureAction());
	static final String ADD_LIST_KEY = "/+/"; //$NON-NLS-1$
	final ReentrantReadWriteLock contextsLock = new ReentrantReadWriteLock();
	final LoggerContextTargetMap loggerContextTargetMap = new LoggerContextTargetMap();
	private final Permission logPermission = new LogPermission("*", LogPermission.LOG); //$NON-NLS-1$
	final ExtendedLogReaderServiceFactory logReaderServiceFactory;
	private final LoggerAdmin loggerAdmin = new EquinoxLoggerAdmin();
	private final boolean captureLogEntryLocation;
	final EquinoxConfiguration equinoxConfiguration;
	final ExtendedLogServiceImpl systemBundleLog;

	public ExtendedLogServiceFactory(ExtendedLogReaderServiceFactory logReaderServiceFactory,
			EquinoxConfiguration equinoxConfiguration, Bundle systemBundle) {
		this.logReaderServiceFactory = logReaderServiceFactory;
		this.equinoxConfiguration = equinoxConfiguration;
		String captureEntryLocationConfigProp = equinoxConfiguration
				.getConfiguration(EquinoxConfiguration.PROP_LOG_CAPTURE_ENTRY_LOCATION, "true"); //$NON-NLS-1$
		this.captureLogEntryLocation = "true".equals(captureEntryLocationConfigProp); //$NON-NLS-1$
		this.systemBundleLog = getLogService(systemBundle);
	}

	boolean captureLogEntryLocation() {
		return captureLogEntryLocation;
	}

	EquinoxConfiguration getConfiguration() {
		return equinoxConfiguration;
	}

	@Override
	public ExtendedLogServiceImpl getService(Bundle bundle, ServiceRegistration<ExtendedLogService> registration) {
		return getLogService(bundle);
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<ExtendedLogService> registration,
			ExtendedLogService service) {
		// do nothing
		// Notice that we do not remove the the LogService impl for the bundle because
		// other bundles
		// still need to be able to get the cached loggers for a bundle.
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		if (event.getType() == BundleEvent.UNINSTALLED)
			removeLogService(event.getBundle());
	}

	ExtendedLogServiceImpl getLogService(Bundle bundle) {
		contextsLock.writeLock().lock();
		try {
			return loggerContextTargetMap.getLogService(bundle, this);
		} finally {
			contextsLock.writeLock().unlock();
		}
	}

	void shutdown() {
		contextsLock.writeLock().lock();
		try {
			loggerContextTargetMap.clear();
		} finally {
			contextsLock.writeLock().unlock();
		}

	}

	void removeLogService(Bundle bundle) {
		contextsLock.writeLock().lock();
		try {
			loggerContextTargetMap.remove(bundle);
		} finally {
			contextsLock.writeLock().unlock();
		}
	}

	boolean isLoggable(Bundle bundle, String name, int level) {
		return logReaderServiceFactory.isLoggable(bundle, name, level);
	}

	void log(Bundle bundle, String name, StackTraceElement stackTraceElement, Object context, LogLevel logLevelEnum,
			int level, String message, ServiceReference<?> ref, Throwable exception) {
		logReaderServiceFactory.log(bundle, name, stackTraceElement, context, logLevelEnum, level, message, ref,
				exception);
	}

	void checkLogPermission() throws SecurityException {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(logPermission);
	}

	EquinoxLoggerContext createEquinoxLoggerContext(String name) {
		return new EquinoxLoggerContext(name);
	}

	LoggerAdmin getLoggerAdmin() {
		return loggerAdmin;
	}

	class EquinoxLoggerAdmin implements LoggerAdmin {
		@Override
		public LoggerContext getLoggerContext(String name) {
			contextsLock.writeLock().lock();
			try {
				return loggerContextTargetMap.createLoggerContext(name, ExtendedLogServiceFactory.this);
			} finally {
				contextsLock.writeLock().unlock();
			}
		}

	}

	class EquinoxLoggerContext implements LoggerContext {
		final String contextName;
		final Map<String, LogLevel> contextLogLevels = new HashMap<>();

		EquinoxLoggerContext(String name) {
			this.contextName = name;
		}

		@Override
		public String getName() {
			return contextName;
		}

		@Override
		public LogLevel getEffectiveLogLevel(final String name) {
			contextsLock.readLock().lock();
			try {
				LogLevel level = null;
				String lookupName = name;
				while ((level = contextLogLevels.get(lookupName)) == null) {
					int lastDot = lookupName.lastIndexOf('.');
					if (lastDot >= 0) {
						lookupName = lookupName.substring(0, lastDot);
					} else {
						break;
					}
				}
				if (level == null) {
					level = contextLogLevels.get(Logger.ROOT_LOGGER_NAME);
				}
				if (level == null && contextName != null) {
					// non-null context name is a non-root context;
					// must check the root context for non-root contexts
					EquinoxLoggerContext rootContext = loggerContextTargetMap.getRootLoggerContext();
					if (rootContext != null) {
						level = rootContext.getEffectiveLogLevel(name);
					}
				}
				if (level == null) {
					level = logReaderServiceFactory.getDefaultLogLevel();
				}
				return level;
			} finally {
				contextsLock.readLock().unlock();
			}
		}

		@Override
		public Map<String, LogLevel> getLogLevels() {
			contextsLock.readLock().lock();
			try {
				return new HashMap<>(contextLogLevels);
			} finally {
				contextsLock.readLock().unlock();
			}
		}

		Map<String, LogLevel> getEffectiveLogLevels() {
			contextsLock.readLock().lock();
			try {
				Map<String, LogLevel> effectiveLogLevels = loggerContextTargetMap.getRootLoggerContext().getLogLevels();
				effectiveLogLevels.putAll(getLogLevels());
				return effectiveLogLevels;
			} finally {
				contextsLock.readLock().unlock();
			}
		}

		@Override
		public void setLogLevels(Map<String, LogLevel> logLevels) {
			EquinoxLoggerContext systemBundleLoggerContext = null;
			boolean readLocked = false;
			try {
				contextsLock.writeLock().lock();
				try {
					contextLogLevels.clear();
					contextLogLevels.putAll(logLevels);
					// downgrade to readlock
					contextsLock.readLock().lock();
					readLocked = true;
				} finally {
					contextsLock.writeLock().unlock();
				}
				// Note that the readlock is still held here
				systemBundleLoggerContext = loggerContextTargetMap.applyLogLevels(this);
			} finally {
				if (readLocked) {
					contextsLock.readLock().unlock();
				}
			}
			// enable outside of any locks to avoid deadlock on the contextsLock
			enableEquinoxTrace(systemBundleLoggerContext);
		}

		private void enableEquinoxTrace(EquinoxLoggerContext systemBundleLoggerContext) {
			if (systemBundleLoggerContext == null) {
				return;
			}
			Map<String, LogLevel> logLevels = systemBundleLoggerContext.getEffectiveLogLevels();
			LogLevel equinoxTrace = logLevels.remove(Debug.EQUINOX_TRACE);
			if (equinoxTrace == null) {
				return;
			}
			DebugOptions debugOptions = getConfiguration().getDebugOptions();
			Map<String, String> options = debugOptions.getOptions();
			options.clear();
			boolean enable = false;
			if (equinoxTrace.implies(LogLevel.DEBUG)) {
				Map<String, StringBuilder> lists = new HashMap<>();
				// enable trace options if the EQUINOX.TRACE is set to trace
				for (Entry<String, LogLevel> level : logLevels.entrySet()) {
					if (level.getValue() != null && level.getValue().implies(LogLevel.DEBUG)//
							&& !(EQUINOX_LOGGER_NAME.equals(level.getKey())
									|| PERF_LOGGER_NAME.equals(level.getKey()))) {
						int listIdx = level.getKey().lastIndexOf(ADD_LIST_KEY);
						if (listIdx != -1) {
							// If the logger name contains "/+/" then Equinox trace option is a list of
							// values;
							// Where the trace logger name is everything upto but not including "/+/... and
							// the
							// remaining key after "/+/" becomes a value in the list.
							String listKey = level.getKey().substring(0, listIdx);
							String listValue = level.getKey().substring(listIdx + ADD_LIST_KEY.length());
							StringBuilder current = lists.get(listKey);
							if (current == null) {
								current = new StringBuilder(listValue);
								lists.put(listKey, current);
							} else {
								current.append(',').append(listValue);
							}
						} else {
							options.put(level.getKey(), Boolean.TRUE.toString());
							enable = true;
						}
					}
				}
				// set all the values for list keys found.
				for (Entry<String, StringBuilder> list : lists.entrySet()) {
					options.put(list.getKey(), list.getValue().toString());
					enable = true;
				}
			}
			debugOptions.setOptions(options);
			if (enable) {
				equinoxConfiguration.getDebug().setLogService(systemBundleLog);
			} else {
				equinoxConfiguration.getDebug().setLogService(null);
			}
			debugOptions.setDebugEnabled(enable);
		}

		@Override
		public void clear() {
			setLogLevels(Collections.emptyMap());
		}

		@Override
		public boolean isEmpty() {
			contextsLock.readLock().lock();
			try {
				return contextLogLevels.isEmpty();
			} finally {
				contextsLock.readLock().unlock();
			}
		}
	}

	ExtendedLogServiceImpl getSystemBundleLog() {
		return systemBundleLog;
	}

}
