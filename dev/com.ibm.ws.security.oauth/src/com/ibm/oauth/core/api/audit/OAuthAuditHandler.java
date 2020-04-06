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

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OAuthException;

/**
 * Public interface that consumer should implement to handle the OAuth audit entries, basically
 * to save the entries to file system or database for further processing.
 *
 */
public interface OAuthAuditHandler {

    /**
     * This method will be called when the audit handler is initializing, handler can get the configuration
     * from passed in parameter and perform initialization in this method.
     * @param config the configuration of the OAuth component
     */
    public void init(OAuthComponentConfiguration config);

    /**
     * When the audit entry is generated, this method is immediately called to write the audit entry for persistence.
     * @param entry the audit entry to be processed
     * @throws OAuthException thrown if there are any errors
     */
    public void writeEntry(OAuthAuditEntry entry) throws OAuthException;
}
