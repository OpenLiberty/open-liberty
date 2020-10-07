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
package com.ibm.oauth.core.api.audit;

import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OAuthException;

/**
 * Interface to encapsulate OAuth audit entries
 *
 */
public interface OAuthAuditEntry {
    /**
     * Gets the attributes of OAuth processing in which the audit entry is generated.
     * @return the attributes in OAuth processing
     */
    public AttributeList getAttributes();

    /**
     * Gets the time when the audit entry is generated
     * @return the time when audit entry is generated
     */
    public Date getTimeStamp();

    /**
     * Gets the OAuth exception if there are errors happen in OAuth processing.
     * @return the OAuth exception if there are any errors, otherwise null
     */
    public OAuthException getError();

    /**
     * Transforms the audit entry into representation of a element in DOM tree. This method 
     * is to support XMLFileOAuthAuditHandler specifically.
     * @param document the document not of the DOM tree
     * @return the element representation of the audit entry
     * @see com.ibm.oauth.core.api.audit.XMLFileOAuthAuditHandler
     */
    public Element toXML(Document document);
}
