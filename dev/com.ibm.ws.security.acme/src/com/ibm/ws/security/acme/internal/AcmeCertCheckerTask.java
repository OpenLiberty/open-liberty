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

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * The AcmeCertCheckerTask runs in the background and periodically checks if the
 * ACME CA certificate is either expiring or revoked and renews the certificate
 * if necessary.
 */
public class AcmeCertCheckerTask implements Runnable {

	private static final TraceComponent tc = Tr.register(AcmeCertCheckerTask.class);

	private final AcmeProviderImpl acmeProviderImpl;

	private ScheduledFuture<?> certChecker;

	private ScheduledExecutorService service = null;
	
	private volatile boolean runningOnErrorSchedule = false;

	public AcmeCertCheckerTask(AcmeProviderImpl acmePI) {
		acmeProviderImpl = acmePI;
	}

	/**
	 * Stop the scheduler and void the ScheduledFuture
	 */
	public synchronized void stop() {
		cancel(true);
		certChecker = null;
	}

	/**
	 * Start the certificate checker scheduled task. It will first cancel any
	 * existing task and then schedule a new repeating task.
	 * 
	 * @param service
	 *           The scheduler service ref
	 */
	protected synchronized void startCertificateChecker(ScheduledExecutorService service) {
		cancel(true);

		if (acmeProviderImpl.getAcmeConfig() == null) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "Provided acmeProviderImpl.getAcmeConfig() is null, cannot start certificate checker");
			}
			return;
		}

		if (acmeProviderImpl.getAcmeConfig().getCertCheckerScheduler() == 0
				|| (!acmeProviderImpl.getAcmeConfig().isAutoRenewOnExpiration()
						&& !acmeProviderImpl.getAcmeConfig().isRevocationCheckerEnabled())) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc,
						"ScheduledExecutorService not started for AcmeCertChecker, it is disabled-- getCertCheckerScheduler: "
								+ acmeProviderImpl.getAcmeConfig().getCertCheckerScheduler()
								+ ", isAutoRenewOnExpiration: "
								+ acmeProviderImpl.getAcmeConfig().isAutoRenewOnExpiration()
								+ ", isRevocationCheckerEnabled: "
								+ acmeProviderImpl.getAcmeConfig().isRevocationCheckerEnabled());
			}
			return;
		}

		if (service == null) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "Provided ScheduledExecutorService is null, cannot start certificate checker");
			}
			return;
		}

		this.service = service;

		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "ScheduledExecutorService starting, time: "
					+ acmeProviderImpl.getAcmeConfig().getCertCheckerScheduler());
		}
		startRegularSchedule();
	}

	/**
	 * Scheduled task to check if the current certificate is expiring or revoked. If
	 * it is expiring or revoked, a certificate request is made. If an exception occurs,
	 * it continues to run, but is rescheduled on the error schedule.
	 */
	@Override
	@FFDCIgnore(Throwable.class)
	public void run() {

		boolean isExpired = false, isRevoked = false;
		List<X509Certificate> currentCert = null;
		
		if (FrameworkState.isStopping()) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Server is marked as stopping, cert checker returning.");
			}
			return;
		}

		acmeProviderImpl.acquireWriteLock();
		try {
			currentCert = acmeProviderImpl.getConfiguredDefaultCertificateChain();

			if (currentCert == null) {
				if (tc.isDebugEnabled()) {
					Tr.debug(tc, "Attempted to check the current certificate, but it was null.");
				}
				return;
			} else {
				if (acmeProviderImpl.getAcmeConfig().isAutoRenewOnExpiration()
						&& acmeProviderImpl.isExpired(currentCert)) {
					isExpired = true;
				} else if (acmeProviderImpl.isRevoked(currentCert)) {
					isRevoked = true;
				}
			}

			if (isExpired || isRevoked) {
				if (isExpired) {
					Tr.info(tc, "CWPKI2052I", currentCert.get(0).getSerialNumber().toString(16),
							currentCert.get(0).getNotAfter().toInstant().toString(),
							acmeProviderImpl.getAcmeConfig().getDirectoryURI());
				} else if (isRevoked) {
					Tr.info(tc, "CWPKI2067I", currentCert.get(0).getSerialNumber().toString(16),
							acmeProviderImpl.getAcmeConfig().getDirectoryURI());
				}
				acmeProviderImpl.renewCertificate();
			} else {
				if (tc.isDebugEnabled()) {
					Tr.debug(tc,
							"ACME automatic certificate checker verified that the ACME CA cert is valid. Next check is "
									+ acmeProviderImpl.getAcmeConfig().getCertCheckerScheduler() + "ms. SN is "
									+ currentCert.get(0).getSerialNumber().toString(16));
				}

				if (runningOnErrorSchedule) {
					if (tc.isDebugEnabled()) {
						Tr.debug(tc,
								"ACME automatic certificate checker was running on error time, but we have a valid certificate, swap back to the regular schedule");
					}
					startRegularSchedule();
				}
				
			}

		} catch (Throwable t) {
			if (FrameworkState.isStopping()) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "Caught an exception, but server is marked as stopping, cert checker returning.");
				}
				return;
			}
			try {
				if (tc.isDebugEnabled()) {
					Tr.debug(tc, "Requested a new certificate, but request failed.", t);
				}
				if (currentCert == null) {
					if (tc.isDebugEnabled()) {
						Tr.debug(tc,
								"Attempted to check the current certificate, but it was null. Stay on regular schedule.");
					}
					return;
				} else {
					String sn = currentCert.get(0).getSerialNumber().toString(16);
					if (isExpired) {
						Tr.warning(tc, "CWPKI2065W", sn,
								acmeProviderImpl.getAcmeConfig().getCertCheckerErrorScheduler() + "ms",
								currentCert.get(0).getNotAfter().toInstant().toString(), t);
					} else if (isRevoked) {
						Tr.error(tc, "CWPKI2066E", sn,
								acmeProviderImpl.getAcmeConfig().getCertCheckerErrorScheduler() + "ms", t);
					} else {
						Tr.warning(tc, "CWPKI2068W", sn,
								acmeProviderImpl.getAcmeConfig().getCertCheckerErrorScheduler() + "ms", t);
					}
				}

				cancel(false);

				if (tc.isDebugEnabled()) {
					Tr.debug(tc, "Certificate request failed, swapping to the error schedule: "
							+ acmeProviderImpl.getAcmeConfig().getCertCheckerErrorScheduler());
				}
			} finally {
				startErrorSchedule();
			}

		} finally {
			acmeProviderImpl.releaseWriteLock();
		}
	}

	/**
	 * Cancel the current certChecker future.
	 * 
	 * @param interrupt
	 *           Whether to interrupt the current certChecker future
	 */
	private synchronized void cancel(boolean interrupt) {
		if (certChecker != null) {
			certChecker.cancel(interrupt);
		}
	}

	/**
	 * Start the certChecker using the regular timing schedule. The current future ref
	 * is cancelled.
	 */
	private void startRegularSchedule() {
		cancel(false);
		certChecker = service.scheduleAtFixedRate(this,
				acmeProviderImpl.getAcmeConfig().getCertCheckerScheduler(),
				acmeProviderImpl.getAcmeConfig().getCertCheckerScheduler(), TimeUnit.MILLISECONDS);
		runningOnErrorSchedule = false;
	}

	/**
	 * Start the certChecker using the error timing schedule. The current future ref
	 * is cancelled.
	 */
	private void startErrorSchedule() {
		cancel(false);
		certChecker = service.scheduleAtFixedRate(this,
				acmeProviderImpl.getAcmeConfig().getCertCheckerErrorScheduler(),
				acmeProviderImpl.getAcmeConfig().getCertCheckerErrorScheduler(), TimeUnit.MILLISECONDS);
		runningOnErrorSchedule = true;
	}
}
