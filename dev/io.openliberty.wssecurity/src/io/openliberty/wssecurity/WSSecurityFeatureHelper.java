/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wssecurity;

import java.net.URL;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

import org.apache.cxf.binding.soap.SoapMessage;

/**
 *
 */
@Component(service = WSSecurityFeatureHelper.class, name = "WSSecurityFeatureHelper", immediate = true)
public class WSSecurityFeatureHelper {
	private static final TraceComponent tc = Tr.register(WSSecurityFeatureHelper.class);
	static final String WSSEC_HELPER_SERVICE = "WSSecurityFeatureHelperService";
	protected final static AtomicServiceReference<WSSecurityFeatureHelperService> WssecurityFeatureHelperServiceRef = new AtomicServiceReference<WSSecurityFeatureHelperService>(
			WSSEC_HELPER_SERVICE);
	
	public WSSecurityFeatureHelper() {
		
	}
	
	public boolean isWSSecurityFeatureHelperServiceActive() {
		return (WssecurityFeatureHelperServiceRef.getService() != null);
	}

	public void handleEhcache2Mapping(String key, URL url, SoapMessage message) {
		WSSecurityFeatureHelperService wssecFeatureHelperservice = getWSSecurityFeatureHelperService();
		if (wssecFeatureHelperservice != null) {
			wssecFeatureHelperservice.handleEhcache2Mapping(key, url, message);
		}
	}
	
	private WSSecurityFeatureHelperService getWSSecurityFeatureHelperService() {
	        return WssecurityFeatureHelperServiceRef.getService();
	}
	
	@Reference(service = WSSecurityFeatureHelperService.class, name = WSSEC_HELPER_SERVICE, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setWSSecurityFeatureHelperService(ServiceReference<WSSecurityFeatureHelperService> ref) {
		WssecurityFeatureHelperServiceRef.setReference(ref);
    }

    protected void unsetWSSecurityFeatureHelperService(ServiceReference<WSSecurityFeatureHelperService> ref) {
    	WssecurityFeatureHelperServiceRef.unsetReference(ref);
    }
	
	@Activate
    protected void activate(ComponentContext cc) {
		WssecurityFeatureHelperServiceRef.activate(cc);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "WssecurityFeatureHelper  service is activated");
        }
    }

    @Modified
    protected void modified(Map<String, Object> props) {
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
    	WssecurityFeatureHelperServiceRef.deactivate(cc);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "WssecurityFeatureHelper service is de-activated");
        }
    }
}
