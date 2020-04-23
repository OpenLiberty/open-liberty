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

package com.ibm.ws.security.acme.internal;

import java.util.Calendar;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
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

	/** Lock used to signal when the application has started. */
	private final Lock lock = new ReentrantLock();

	/** Condition used to signal when the application has started. */
	private final Condition appStartedCondition = lock.newCondition();

	@Override
	public void applicationStarting(ApplicationInfo appInfo) {
		/*
		 * Ignore. The service and web application cannot be up without one
		 * another, so no need to do anything here.
		 */
	}

	@Override
	public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
		final String methodName = "applicationStarted(ApplicationInfo)";

		if (AcmeAuthorizationServlet.APP_NAME.equals(appInfo.getName())) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
				Tr.event(tc,
						methodName + ": ACME authorization web application has started and is available for requests.");
			}

			lock.lock();
			try {
				isAppStarted = true;
				appStartedCondition.signalAll();
			} finally {
				lock.unlock();
			}
		}
	}

	@Override
	public void applicationStopping(ApplicationInfo appInfo) {
		/*
		 * Ignore. The service and web application cannot be up without one
		 * another, so no need to do anything here.
		 */
	}

	@Override
	public void applicationStopped(ApplicationInfo appInfo) {
		/*
		 * Ignore. The service and servlet cannot be up without one another, so
		 * no need to do anything here.
		 */
	}

	/**
	 * Wait until the ACME authorization web application is available for
	 * service at /.well-known/acme-authorization.
	 * 
	 * @throws AcmeCaException
	 *             If the application is not available within the expected time.
	 */
	public void waitUntilWebAppAvailable() throws AcmeCaException {
		final String methodName = "waitUntilWebAppAvailable()";

		try {
			lock.lock();
			if (!isAppStarted) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
					Tr.debug(tc, methodName + ": ACME authorization web application has not started - waiting.");
				}

				boolean signalled = false, keepWaiting = true;
				Calendar cal = Calendar.getInstance();
				int timeToWait = 2;
				cal.add(Calendar.MINUTE, timeToWait); // Wait 2 minutes, maximum
				while (keepWaiting) {
					try {
						keepWaiting = false;
						signalled = appStartedCondition.awaitUntil(cal.getTime());
					} catch (InterruptedException e) {
						keepWaiting = true;
					}
				}
				if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
					Tr.debug(tc, methodName + ": Finished waiting.");
				}

				/*
				 * If the wait above expired and we weren't signaled by the
				 * applicationStarted(...) method, the ACME authorization web
				 * application did not start, we can't proceed.
				 */
				if (!signalled) {
					throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2036E", timeToWait));
				} else if (!isAppStarted) {
					/*
					 * This should never happen, but throw an exception if it
					 * does.
					 */
					throw new AcmeCaException("ACME authorization web application did not start.");
				}

			} else {
				if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
					Tr.debug(tc, methodName + ": ACME authorization web application already started - not waiting.");
				}
			}
		} finally {
			lock.unlock();
		}
	}
}
