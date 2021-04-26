/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.acme.internal;

import java.util.Calendar;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.kernel.feature.ServerStartedPhase2;
import com.ibm.ws.security.acme.AcmeCaException;
import com.ibm.ws.security.acme.internal.web.AcmeAuthorizationServlet;

/**
 * An {@link ApplicationStateListener} that will listen for whether the ACME
 * authorization web application has started.
 */
@Component(immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, service = {
		AcmeApplicationStateListener.class,
		ApplicationStateListener.class }, property = { "service.vendor=IBM", "includeAppsWithoutConfig=true" })
public class AcmeApplicationStateListener implements ApplicationStateListener {

	private static final TraceComponent tc = Tr.register(AcmeApplicationStateListener.class);

	/** Has the ACME authorization web application started? */
	private boolean isAppStarted = false;

	/** Has the server started which means the HTTP port is available? */
	private boolean isHttpStarted;

	/** Lock used to signal when the application has started. */
	private final Lock appLock = new ReentrantLock();

	/** Lock used to signal when the application has started. */
	private final Lock httpLock = new ReentrantLock();

	/** Condition used to signal when the application has started. */
	private final Condition appStartedCondition = appLock.newCondition();

	private final Condition httpStartedCondition = httpLock.newCondition();

	@Override
	@Trivial
	public void applicationStarting(ApplicationInfo appInfo) {
		/*
		 * Ignore. The service and web application cannot be up without one
		 * another, so no need to do anything here.
		 */
	}

	@Override
	@Trivial
	public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
		final String methodName = "applicationStarted(ApplicationInfo)";

		if (AcmeAuthorizationServlet.APP_NAME_EE8.equals(appInfo.getName())
				|| AcmeAuthorizationServlet.APP_NAME_EE9.equals(appInfo.getName())) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
				Tr.event(tc,
						methodName + ": ACME authorization web application has started and is available for requests.");
			}

			appLock.lock();
			try {
				isAppStarted = true;
				appStartedCondition.signalAll();
			} finally {
				appLock.unlock();
			}
		}
	}

	/**
	 * Declarative services method that is invoked once the ServerStarted service is
	 * available. Only after this method is invoked are the activation
	 * specifications activated thereby ensuring that endpoints are activated only
	 * after server startup.
	 *
	 * @param serverStartedPhase2
	 *                          The server started instance
	 */
	@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
	protected synchronized void setServerStartedPhase2(ServerStartedPhase2 serverStartedPhase2) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, ": HTTP is open.");
		}
		httpLock.lock();
		try {
			isHttpStarted = true;
			httpStartedCondition.signalAll();
		} finally {
			httpLock.unlock();
		}
	}

	/**
	 * Declarative services method for unsetting the ServerStarted service instance.
	 *
	 * @param serverStarted
	 *                          The Started service instance
	 */
	@Trivial
	protected void unsetServerStartedPhase2(ServerStartedPhase2 serverStartedPhase2) {
		// No cleanup is needed since the server has stopped.
		isHttpStarted = false;
	}

	@Override
	@Trivial
	public void applicationStopping(ApplicationInfo appInfo) {
		/*
		 * Ignore. The service and web application cannot be up without one
		 * another, so no need to do anything here.
		 */
	}

	@Override
	@Trivial
	public void applicationStopped(ApplicationInfo appInfo) {
		/*
		 * Ignore. The service and servlet cannot be up without one another, so
		 * no need to do anything here.
		 */
	}

	/**
	 * Wait until the ACME authorization web application is available for service at
	 * /.well-known/acme-authorization.
	 * 
	 * Then wait until all applications are started as this signals the HTTP is
	 * open. If we're not signaled that HTTP is open, optimistically log it and
	 * continue.
	 *
	 * 
	 * @throws AcmeCaException
	 *             If the application is not available within the expected time.
	 */
	public void waitUntilResourcesAvailable(AcmeConfig acmeConfig) throws AcmeCaException {
		final String methodName = "waitUntilWebAppAvailable()";

		Calendar cal = Calendar.getInstance();

		try {
			appLock.lock();

			/*
			 * The startReadyTimeout is used both for waiting on the acme servlet and the
			 * HTTP port to open as a single timeout (not waiting for startReadyTimeout
			 * twice).
			 */
			cal.setTimeInMillis(System.currentTimeMillis() + acmeConfig.getStartReadyTimeout());

			if (!isAppStarted) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					if (!isAppStarted) {
						Tr.debug(tc, methodName + ": ACME authorization web application has not started - waiting.");
					}
				}

				boolean signaled = false, keepWaiting = true;


				while (keepWaiting) {
					try {
						keepWaiting = false;
						signaled = appStartedCondition.awaitUntil(cal.getTime());
					} catch (InterruptedException e) {
						keepWaiting = true;
					}
				}
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, methodName + ": Finished waiting on ACME authorization web application.");
				}

				/*
				 * If the wait above expired and we weren't signaled by the
				 * applicationStarted(...) method, the ACME authorization web application did
				 * not start, log it and attempt to proceed.
				 */
				if (!signaled || !isAppStarted) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
						Tr.event(tc, methodName
								+ ": Not signalled that acme application is ready, letting ACME flow happen anyway: signaled: "
								+ signaled + " isHttpStarted: " + isAppStarted);
					}
					Tr.warning(tc, "CWPKI2036W", acmeConfig.getStartReadyTimeout() + "ms");
				}

			} else {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, methodName + ": ACME authorization web application already started - not waiting.");
				}
			}
		} finally {
			appLock.unlock();
		}

		try {
			httpLock.lock();
			if (!isHttpStarted) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, methodName + ": HTTP has not started - waiting. Time left: "
							+ (cal.getTimeInMillis() - System.currentTimeMillis()) + "ms");
				}

				boolean signaled = false, keepWaiting = true;
				while (keepWaiting) {
					try {
						keepWaiting = false;
						/*
						 * Use any time left from waiting on the servlet to start
						 */
						signaled = httpStartedCondition.awaitUntil(cal.getTime());
					} catch (InterruptedException e) {
						keepWaiting = true;
					}
				}
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, methodName + ": Finished waiting on HTTP.");
				}

				/*
				 * If the wait above expired and we weren't signaled by the startedPhase2
				 * method, we will attempt to start ACME anyway. If the HTTP port is truly not
				 * open, then the CA call back to the well-known URL will fail
				 * ("Connection refused" or similar).
				 */
				if (!signaled || !isHttpStarted) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
						Tr.event(tc, methodName
								+ ": Not signalled that HTTP is ready, letting ACME flow happen anyway : signaled: "
								+ signaled + " isHttpStarted: " + isHttpStarted);
					}
					Tr.warning(tc, "CWPKI2074W", acmeConfig.getStartReadyTimeout() + "ms");
				}
			} else {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, methodName + ": HTTP is already opened - not waiting.");
				}
			}
		} finally {
			httpLock.unlock();
		}
	}
}
