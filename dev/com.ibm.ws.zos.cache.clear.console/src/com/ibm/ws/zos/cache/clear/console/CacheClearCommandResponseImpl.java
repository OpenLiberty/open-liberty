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
package com.ibm.ws.zos.cache.clear.console;

import java.lang.management.ManagementFactory;
import java.util.List;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.SecurityService;
import com.ibm.websphere.security.authentication.cache.DeleteAuthCache;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.zos.command.processing.CommandResponses;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM", "type:String=cacheClear" }, service = { CommandResponses.class })
public class CacheClearCommandResponseImpl implements CommandResponses {
	/** trace variable */
	private static final TraceComponent tc = Tr.register(CacheClearCommandResponseImpl.class);
	static final String KEY_SECURITY_SERVICE = "securityService";
	private static final String DELETE_AUTH_CACHE_MXBEAN = "WebSphere:service=com.ibm.websphere.security.authentication.cache.DeleteAuthCache";
	private DeleteAuthCache deleteAuthCacheMbean;

    /**
     * Mbean Server instance to invoke the service
     */
    private static final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

    /**
     * Static reference from this bundle to Security Service to ensure 
     * that this bundle will not start unless the security service has started.
     */
    private static final AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);

    @Reference(service = SecurityService.class, name = KEY_SECURITY_SERVICE)
    protected void setSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.setReference(reference);
    }

    protected void unsetSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.unsetReference(reference);
    }

	@Activate
    protected void activate(ComponentContext cc) {
        securityServiceRef.activate(cc);
    }

	@Deactivate
    protected void deactivate(ComponentContext cc) {
        securityServiceRef.deactivate(cc);
    }

	@Override
	public int getCommandResponses(String command, List<String> responses) {
		// Initialize return code
		int rc = 0;
		try {		
			// Builds response string for cache,clear,auth command
			callRevokeAuthCacheMbean(responses);
		} catch (Exception e) {
			// Relying on the FFDC to debug the exception
			responses.add("Unable to process z/OS clear cache command due");
			responses.add("to an error. Check logs for details");
			
			// RC of -1 to set modify results to ERROR in command handler
			rc = -1;	
		}
		return rc;
	}
	
	/**
	 * Helper method for calling the mbean to clear the authentication cache
	 * @param responses - responses sent back to MVS console
	 * @throws Exception
	 */
	private void callRevokeAuthCacheMbean(List<String> responses) throws Exception {
		deleteAuthCacheMbean = JMX.newMBeanProxy(mbs, new ObjectName(DELETE_AUTH_CACHE_MXBEAN), DeleteAuthCache.class);
		deleteAuthCacheMbean.removeAllEntries();
		responses.add("User authentication cache has been cleared.");
	}
}
