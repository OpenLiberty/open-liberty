/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.jwtsso.token;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialException;

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
import com.ibm.ws.security.credentials.CredentialProvider;
import com.ibm.ws.security.jwtsso.token.proxy.JwtSSOTokenProxy;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = CredentialProvider.class, name = "JwtSSOTokenCredentialProvider", configurationPolicy = ConfigurationPolicy.IGNORE, property = {
		"service.vendor=IBM", "type=JwtSSOToken" })
public class JwtSSOTokenCredentialProvider implements CredentialProvider {
	private static final TraceComponent tc = Tr.register(JwtSSOTokenCredentialProvider.class);
	public static final String JSON_WEB_TOKEN_SSO_PROXY = "JwtSSOTokenProxy";
	protected final static AtomicServiceReference<JwtSSOTokenProxy> jwtSSOTokenProxyRef = new AtomicServiceReference<JwtSSOTokenProxy>(
			JSON_WEB_TOKEN_SSO_PROXY);

	@Reference(service = JwtSSOTokenProxy.class, name = JSON_WEB_TOKEN_SSO_PROXY, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	protected void setJwtSSOToken(ServiceReference<JwtSSOTokenProxy> ref) {
		jwtSSOTokenProxyRef.setReference(ref);
	}

	protected void unsetJwtSSOToken(ServiceReference<JwtSSOTokenProxy> ref) {
		jwtSSOTokenProxyRef.unsetReference(ref);
	}

	@Activate
	protected void activate(ComponentContext cc) {
		jwtSSOTokenProxyRef.activate(cc);
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "JwtSSOTokenCredentialProvider service is activated");
		}
	}

	@Deactivate
	protected void deactivate(ComponentContext cc) {
		jwtSSOTokenProxyRef.deactivate(cc);
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "JwtSSOTokenCredentialProvider service is deactivated");
		}
	}

	@Override
	public void setCredential(Subject subject) throws CredentialException {
	}

	@Override
	public boolean isSubjectValid(Subject subject) {
		JwtSSOTokenProxy jwtSSOTokenProxy = jwtSSOTokenProxyRef.getServiceWithException();
		return jwtSSOTokenProxy.isSubjectValid(subject);
	}
}
