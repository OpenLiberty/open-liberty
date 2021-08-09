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
import java.util.List;

import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

/**
 * @ibm-api
 *          Events of this type are sent to objects implementing the {@linkplain} DomainResolverListener} interface which is related to {@linkplain} DomainResolver} locate API
 *          calls.
 * 
 * @author Noam Almog
 */
public class SipURILookupEvent extends EventObject {

	private List <SipURI> _results;
	private SipURI _sipUri;

    /**
     * Constructor for a new SipURILookupEvent.
     * 
     * @param session - the session the event relates to
     * @param sipUri - the queried sip uri
     * @param results - List of resulted SipUri objects
     * 
     */
    public SipURILookupEvent(SipSession session, SipURI sipUri, List<SipURI> results) {
        super(session);
        _results = results;
        _sipUri = sipUri;
    }

    /**
     * Returns the related SipSession
     * 
     * @return SipSession
     */
    public SipSession getSipSession() {
        return (SipSession) getSource();
    }

    /**
     * Returns a list of resulted SipUri objects
     * 
     * @return List
     */
    public List<SipURI> getResults() {
        return _results;
    }

    /**
     * Returns a queried sip uri
     * 
     * @return SipURI
     */
    public SipURI getSipUri() {
        return _sipUri;
    }

}
