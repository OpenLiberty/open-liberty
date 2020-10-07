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
package com.ibm.websphere.sip.resolver.events;

import java.util.EventObject;

import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import com.ibm.websphere.sip.resolver.exception.SipURIResolveException;

/**
 * @ibm-api
 * Events of this type are sent to objects implementing the
 * {@linkplain} DomainResolverListener} interface which is related to
 * {@linkplain} DomainResolver} locate API calls.
 *
 * @author Noam Almog
 */
public class SipURILookupErrorEvent extends EventObject {

	private SipURI _sipUri;
	private SipURIResolveException _ex;

	/**
	 * Constructor for a new SipURILookupErrorEvent.
	 *
	 * @param session - the session the error event relates to
	 * @param sipUri - the sip uri the error event relates to
	 * @param exception
	 *
	 */
	public SipURILookupErrorEvent(SipSession session, SipURI sipUri, SipURIResolveException exception) {
		super(session);
		_sipUri = sipUri;
		_ex = exception;
	}

	/**
	 * Returns the session the error event relates to
	 * @return SipSession
	 */
	public SipSession getSipSession() {
		return (SipSession)getSource();
	}

	/**
	 * Returns a queried sip uri
	 * @return SipURI
	 */
	public SipURI getSipUri() {
		return _sipUri;
	}

	/**
	 * Returns related SipURIResolveException
	 * @return SipURIResolveException
	 */
	public SipURIResolveException getException() {
		return _ex;
	}
}
