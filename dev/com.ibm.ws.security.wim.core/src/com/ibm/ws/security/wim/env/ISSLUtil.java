/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.env;

import java.util.Hashtable;
import java.util.Properties;

import com.ibm.wsspi.security.wim.exception.WIMException;

/**
 * Interface for SSL utilities
 */
public interface ISSLUtil {
    /**
     * Set SSL properties on the thread
     */
    public void setSSLPropertiesOnThread(Properties props);

    /**
     * Get the SSL properties that are set on the thread
     */
    public Properties getSSLPropertiesOnThread();

    /**
     * Set the SSL Alias
     * 
     * @param sslAlias
     * @param ldapEnv
     * @throws WIMException
     */
    public void setSSLAlias(String sslAlias, Hashtable<?, ?> ldapEnv) throws WIMException;

    /**
     * Reset the SSL Alias
     */
    public void resetSSLAlias();
}
