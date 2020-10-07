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
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.kernel.feature.FeatureProvisioner;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

import io.grpc.ClientInterceptor;
import io.openliberty.grpc.client.monitor.GrpcMonitoringClientInterceptorService;

@Component(service = {
		ApplicationStateListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class GrpcClientComponent implements ApplicationStateListener {

	private static final TraceComponent tc = Tr.register(GrpcClientComponent.class, GrpcClientMessages.GRPC_TRACE_NAME,
			GrpcClientMessages.GRPC_BUNDLE);

	private final String FEATUREPROVISIONER_REFERENCE_NAME = "featureProvisioner";
	private final String GRPC_SSL_SERVICE_NAME = "GrpcSSLService";
	private final String GRPC_MONITOR_NAME = "GrpcMonitoringClientInterceptorService";

	private final AtomicServiceReference<FeatureProvisioner> featureProvisioner = new AtomicServiceReference<FeatureProvisioner>(
			FEATUREPROVISIONER_REFERENCE_NAME);
	private static GrpcSSLService sslService = null;
	private static GrpcMonitoringClientInterceptorService monitorService = null;

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

	@Reference(name = "GRPC_SSL_SERVICE_NAME", service = GrpcSSLService.class,
	        cardinality = ReferenceCardinality.OPTIONAL,
	        policy = ReferencePolicy.DYNAMIC,
	        policyOption = ReferencePolicyOption.GREEDY)
	 protected void setGrpcSSLService(GrpcSSLService service) {
	     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	         Tr.debug(this, tc, "registerGrpcSSLService");
	     }
	     sslService = service;
	 }

	 protected void unsetGrpcSSLService(GrpcSSLService service) {
	     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	         Tr.debug(this, tc, "unregisterGrpcSSLService");
	     }
	     if (sslService == service) {
	    	 sslService = null;
	     }
	 }

	@Reference(name = "GRPC_MONITOR_NAME", service = GrpcMonitoringClientInterceptorService.class,
	        cardinality = ReferenceCardinality.OPTIONAL,
	        policy = ReferencePolicy.DYNAMIC,
	        policyOption = ReferencePolicyOption.GREEDY)
	 protected void setMonitoringService(GrpcMonitoringClientInterceptorService service) {
	     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	         Tr.debug(this, tc, "setMonitoringService");
	     }
	     monitorService = service;
	 }

	 protected void unsetMonitoringService(GrpcMonitoringClientInterceptorService service) {
	     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	         Tr.debug(this, tc, "unsetMonitoringService");
	     }
	     if (monitorService == service) {
	         monitorService = null;
	     }
	 }

	/**
	 * @return a GrpcSSLService if an SSL feature is enabled
	 */
	public static GrpcSSLService getGrpcSSLService() {
		return sslService;
	}

	/**
	 * @return a ClientInterceptor if monitoring is enabled
	 */
	public static ClientInterceptor getMonitoringClientInterceptor() {
		if (monitorService != null) {
			return monitorService.createInterceptor();
		}
		return null;
	}

	@Override
	public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
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
