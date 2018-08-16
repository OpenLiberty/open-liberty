/*
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2016
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.security.social;

import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;

public interface SocialLoginService {

    /**
     * @return
     */
    public ConfigurationAdmin getConfigAdmin();

    public String getBundleLocation();

    /**
     * @return
     */
    public AtomicServiceReference<SSLSupport> getSslSupportRef();

    /**
     * @return
     */
    public SSLSupport getSslSupport();

    /**
     * @return
     */
    AtomicServiceReference<KeyStoreService> getKeyStoreServiceRef();

}
