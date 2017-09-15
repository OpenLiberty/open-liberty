/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.ssl;

import java.security.KeyStore;
import java.util.Properties;

import javax.net.ssl.X509KeyManager;

/**
 * <p>
 * KeyManager Extended Info Interface. This interface is extended by custom Key
 * Managers which need information about the current SSL configuration to make
 * decisions about whether to change the key information.
 * </p>
 * 
 * @author IBM Corporation
 * @version 1.0
 * @since WAS 6.1
 * @see com.ibm.websphere.ssl.JSSEHelper
 * @ibm-spi
 **/
public interface KeyManagerExtendedInfo {
    /**
     * Method called by WebSphere Application Server runtime to set the custom
     * properties configured for the custom KeyManager.
     * 
     * @param customProperties
     * @ibm-spi
     */
    void setCustomProperties(Properties customProperties);

    /**
     * Method called by WebSphere Application Server runtime to set the SSL
     * configuration properties being used for this connection.
     * 
     * @param config
     *            - contains a property for the SSL configuration.
     * @ibm-spi
     */
    void setSSLConfig(Properties config);

    /**
     * Method called by WebSphere Application Server runtime to set the default
     * X509KeyManager created by the IbmX509 KeyManagerFactory using the KeyStore
     * information present in this SSL configuration. This allows some delegation
     * to the default IbmX509 KeyManager to occur.
     * 
     * @param defaultX509KeyManager
     *            - default IbmX509 key manager for delegation
     * @ibm-spi
     */
    void setDefaultX509KeyManager(X509KeyManager defaultX509KeyManager);

    /**
     * Method called by WebSphere Application Server runtime to set the SSL
     * KeyStore used for this connection.
     * 
     * @param keyStore
     *            - the KeyStore currently configured
     * @ibm-spi
     */
    void setKeyStore(KeyStore keyStore);

    /**
     * Method called by WebSphere Application Server runtime to set the SSL
     * KeyStore certificate alias configured for use by server configurations.
     * This method is only called when the alias is configured using the
     * com.ibm.ssl.keyStoreServerAlias property.
     * 
     * @param serverAlias
     *            - the KeyStore server certificate alias currently configured
     * @ibm-spi
     */
    void setKeyStoreServerAlias(String serverAlias);

    /**
     * Method called by WebSphere Application Server runtime to set the SSL
     * KeyStore certificate alias configured for use by client configurations.
     * This method is only called when the alias is configured using the
     * com.ibm.ssl.keyStoreClientAlias property.
     * 
     * @param clientAlias
     *            - the KeyStore client certificate alias currently configured
     * @ibm-spi
     */
    void setKeyStoreClientAlias(String clientAlias);

}
