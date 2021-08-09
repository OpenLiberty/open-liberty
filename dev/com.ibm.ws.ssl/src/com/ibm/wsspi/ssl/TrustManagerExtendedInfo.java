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

import java.util.Map;
import java.util.Properties;

/**
 * <p>
 * TrustManager Extended Info Interface. This interface is extended by custom
 * Trust Managers which need information about the remote connection information
 * to make decisions about whether to trust the remote connection.
 * </p>
 * 
 * @author IBM Corporation
 * @version 1.0
 * @since WAS 6.1
 * @see com.ibm.websphere.ssl.JSSEHelper
 * @ibm-spi
 **/

public interface TrustManagerExtendedInfo {
    /**
     * Method called by WebSphere Application Server runtime to set the custom
     * properties configured for the custom TrustManager.
     * 
     * @param customProperties
     * @ibm-spi
     */
    void setCustomProperties(Properties customProperties);

    /**
     * Method called by WebSphere Application Server runtime to set the target
     * host information and potentially other connection info in the future.
     * 
     * @param info
     *            - contains information about the target connection.
     * @ibm-spi
     */
    void setExtendedInfo(Map<String, Object> info);

    /**
     * Method called by WebSphere Application Server runtime to set the SSL
     * configuration properties being used for this connection.
     * 
     * @param config
     *            - contains a property for the SSL configuration.
     * @ibm-spi
     */
    void setSSLConfig(Properties config);

}
