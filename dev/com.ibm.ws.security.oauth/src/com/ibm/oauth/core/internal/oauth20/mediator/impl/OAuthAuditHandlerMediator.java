/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.internal.oauth20.mediator.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.audit.OAuthAuditEntry;
import com.ibm.oauth.core.api.audit.OAuthAuditHandler;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MediatorException;
import com.ibm.oauth.core.api.oauth20.mediator.OAuth20Mediator;
import com.ibm.oauth.core.internal.audit.OAuthAuditEntryImpl;

public class OAuthAuditHandlerMediator implements OAuth20Mediator {
    final static String CLASS = OAuthAuditHandlerMediator.class.getName();
    final static Logger _log = Logger.getLogger(CLASS);

    private OAuthAuditHandler _auditHandler;

    public OAuthAuditHandlerMediator(OAuthAuditHandler auditHandler) {
        _auditHandler = auditHandler;
    }

    public void init(OAuthComponentConfiguration config) {
        _auditHandler.init(config);
    }

    public void mediateAuthorize(AttributeList attributeList)
            throws OAuth20MediatorException {
        OAuthAuditEntry entry = new OAuthAuditEntryImpl(attributeList);
        try {
            _auditHandler.writeEntry(entry);
        } catch (OAuthException e) {
            _log.log(Level.SEVERE, "Fails to write audit entry", e);
        }
    }

    public void mediateToken(AttributeList attributeList)
            throws OAuth20MediatorException {
        OAuthAuditEntry entry = new OAuthAuditEntryImpl(attributeList);
        try {
            _auditHandler.writeEntry(entry);
        } catch (OAuthException e) {
            _log.log(Level.SEVERE, "Fails to write audit entry", e);
        }
    }

    public void mediateResource(AttributeList attributeList)
            throws OAuth20MediatorException {
        OAuthAuditEntry entry = new OAuthAuditEntryImpl(attributeList);
        try {
            _auditHandler.writeEntry(entry);
        } catch (OAuthException e) {
            _log.log(Level.SEVERE, "Fails to write audit entry", e);
        }
    }

    public void mediateAuthorizeException(AttributeList attributeList,
            OAuthException exception) throws OAuth20MediatorException {
        OAuthAuditEntry entry = new OAuthAuditEntryImpl(attributeList, exception);
        try {
            _auditHandler.writeEntry(entry);
        } catch (OAuthException e) {
            _log.log(Level.SEVERE, "Fails to write audit entry", e);
        }
    }

    public void mediateTokenException(AttributeList attributeList,
            OAuthException exception) throws OAuth20MediatorException {
        OAuthAuditEntry entry = new OAuthAuditEntryImpl(attributeList, exception);
        try {
            _auditHandler.writeEntry(entry);
        } catch (OAuthException e) {
            _log.log(Level.SEVERE, "Fails to write audit entry", e);
        }
    }

    public void mediateResourceException(AttributeList attributeList,
            OAuthException exception) throws OAuth20MediatorException {
        OAuthAuditEntry entry = new OAuthAuditEntryImpl(attributeList, exception);
        try {
            _auditHandler.writeEntry(entry);
        } catch (OAuthException e) {
            _log.log(Level.SEVERE, "Fails to write audit entry", e);
        }
    }
}
