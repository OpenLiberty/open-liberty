/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.sip.resolver;
import java.util.List;

import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import com.ibm.websphere.sip.resolver.exception.SipURIResolveException;

/**
 * The <code>DomainResolver</code> provides an interface for DNS querying {@linkplain SipURI} 
 * synchronously and asynchronously.
 *  
 * @author Noam Almog, Feb 2010
 * @ibm-api
 *
 */
public interface DomainResolver {
	
	public static final String IBM_TTL_PARAM = "ibmttl";

	/**
	 * query DNS synchronously for SIP Uri.
	 *  
	 * @param sipUri sip uri for the DNS query 
	 * @return List<SipURI> list of results, null if nothing was found.
	 */
	public List<SipURI> locate(SipURI sipUri) throws SipURIResolveException;

	/**
	 * query DNS asynchronously for SIP Uri.
	 * 
	 * @param sipUri sip uri for the DNS query 
	 * @param drListener listener which will be triggered once query is completed.
	 * @param sipSession sip session which this query is related for.
	 */
	public void locate(SipURI sipUri, DomainResolverListener drListener, SipSession sipSession) throws SipURIResolveException;

}
