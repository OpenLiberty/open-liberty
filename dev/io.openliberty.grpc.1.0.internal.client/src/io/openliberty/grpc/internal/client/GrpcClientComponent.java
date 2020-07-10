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
package io.openliberty.grpc.internal.client;

import java.util.Set;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.kernel.feature.FeatureProvisioner;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = {
		ApplicationStateListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class GrpcClientComponent implements ApplicationStateListener {

	private static final TraceComponent tc = Tr.register(GrpcClientComponent.class, GrpcClientMessages.GRPC_TRACE_NAME,
			GrpcClientMessages.GRPC_BUNDLE);

	/** Indicates whether the monitor feature is enabled */
	private static boolean monitoringEnabled = false;

	private final String FEATUREPROVISIONER_REFERENCE_NAME = "featureProvisioner";

	private final AtomicServiceReference<FeatureProvisioner> featureProvisioner = new AtomicServiceReference<FeatureProvisioner>(
			FEATUREPROVISIONER_REFERENCE_NAME);

	@Activate
	protected void activate(ComponentContext cc) {
		featureProvisioner.activate(cc);
	}

	@Deactivate
	protected void deactivate(ComponentContext cc) {
		featureProvisioner.deactivate(cc);
	}

	@Reference(name = FEATUREPROVISIONER_REFERENCE_NAME, service = FeatureProvisioner.class, cardinality = ReferenceCardinality.MANDATORY)
	protected void setFeatureProvisioner(ServiceReference<FeatureProvisioner> ref) {
		featureProvisioner.setReference(ref);
	}

	protected void unsetFeatureProvisioner(ServiceReference<FeatureProvisioner> ref) {
		featureProvisioner.unsetReference(ref);
	}

	/**
	 * Set the indication whether the monitor feature is enabled
	 */
	private void setMonitoringEnabled() {
		Set<String> currentFeatureSet = featureProvisioner.getService().getInstalledFeatures();
		monitoringEnabled = currentFeatureSet.contains("monitor-1.0");
	}

	/**
	 * @return <code>true</code> if the monitor feature is enabled,
	 *         <code>false</code> otherwise
	 */
	public static boolean isMonitoringEnabled() {
		return monitoringEnabled;
	}

	@Override
	public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
		setMonitoringEnabled();
	}

	@Override
	public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
	}

	@Override
	public void applicationStopping(ApplicationInfo appInfo) {
	}

	@Override
	public void applicationStopped(ApplicationInfo appInfo) {
	}
}
