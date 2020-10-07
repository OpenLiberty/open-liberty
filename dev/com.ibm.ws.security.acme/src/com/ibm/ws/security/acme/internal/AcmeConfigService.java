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

import static com.ibm.ws.security.acme.internal.util.AcmeConstants.ACME_CONFIG_PID;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.acme.AcmeCaException;
import com.ibm.wsspi.kernel.service.utils.DictionaryUtils;

/**
 * ACME configuration service. This service is used to validate configuration
 * before it is passed onto the {@link AcmeProviderImpl} service.
 */
@Component(immediate = true, configurationPolicy = REQUIRE, configurationPid = ACME_CONFIG_PID, service = {}, property = {
		"service.vendor=IBM" })
public class AcmeConfigService {

	private static final TraceComponent tc = Tr.register(AcmeConfigService.class);

	@Activate
	private BundleContext bundleContext;

	private ServiceRegistration<AcmeConfigService> serviceRegistration;

	private static ThreadLocal<AcmeConfig> threadLocalAcmeConfig = new ThreadLocal<AcmeConfig>();

	private final Object syncObject = new Object();

	@Activate
	@FFDCIgnore({ AcmeCaException.class })
	public void activate(Map<String, Object> properties) {
		try {
			initialize(properties);
			registerService(properties);
		} catch (AcmeCaException e) {
			Tr.error(tc, e.getMessage()); // AcmeCaException are localized.
		}
	}

	@Deactivate
	public void deactivate(int reason) {
		unregisterService();
	}

	/**
	 * Get the {@link AcmeConfig} instance set on this thread.
	 * 
	 * @return the {@link AcmeConfig} set on this thread; otherwise, null.
	 */
	public static AcmeConfig getThreadLocalAcmeConfig() {
		return threadLocalAcmeConfig.get();
	}

	@Modified
	@FFDCIgnore(AcmeCaException.class)
	public void modified(Map<String, Object> properties) {
		try {
			initialize(properties);

			/*
			 * Register this component as a service if necessary. If we do
			 * register the service, there is no need to update the properties
			 * by calling setProperties as the properties will have been sent
			 * when registering the service.
			 */
			if (!registerService(properties)) {

				/*
				 * The properties are good, update the service properties. This
				 * will send them to the
				 * AcmeConfigService.updateAcmeConfigService(..) method.
				 */
				serviceRegistration.setProperties(DictionaryUtils.mapToDictionary(properties));
			}
		} catch (AcmeCaException e) {
			Tr.error(tc, e.getMessage()); // AcmeCaException are localized.
		}
	}

	/**
	 * Initialize the {@link AcmeClient} instance that will be used to
	 * communicate with the ACME CA server.
	 * 
	 * @param properties
	 *            Configuration properties.
	 * @throws AcmeCaException
	 *             If there was an issue initializing the {@link AcmeClient}.
	 */
	public void initialize(Map<String, Object> properties) throws AcmeCaException {
		/*
		 * Construct a new ACME client.
		 */
		AcmeConfig acmeConfig = new AcmeConfig(properties, true);
		AcmeClient acmeClient = new AcmeClient(acmeConfig);

		/*
		 * Perform a dry-run of fetching the certificate. This should catch any
		 * errors we can before activating, which will set the certificate
		 * retrieval in motion.
		 */
		try {
			threadLocalAcmeConfig.set(acmeConfig);
			acmeClient.fetchCertificate(true);
		} finally {
			threadLocalAcmeConfig.remove();
		}
	}

	/**
	 * Manually register this service if it is not already registered.
	 * 
	 * @param properties
	 *            The component's configuration properties.
	 * @return Whether the service was registered.
	 */
	private boolean registerService(Map<String, Object> properties) {
		boolean registered = false;
		synchronized (syncObject) {
			if (serviceRegistration == null) {
				/*
				 * Registering this component as a service will send the
				 * properties to the AcmeProviderImpl.setAcmeConfigService(..)
				 * method, which references the AcmeConfigService.
				 */
				serviceRegistration = bundleContext.registerService(AcmeConfigService.class, this,
						DictionaryUtils.mapToDictionary(properties));
				registered = true;
			}
		}
		return registered;
	}

	/**
	 * Manually unregister this service if it has been registered.
	 */
	private void unregisterService() {
		synchronized (syncObject) {
			if (serviceRegistration != null) {
				try {
					serviceRegistration.unregister();
					serviceRegistration = null;
				} catch (IllegalArgumentException iae) {
					// Ignore. Can happen if bundle is being shutdown.
				}
			}
		}
	}
}
