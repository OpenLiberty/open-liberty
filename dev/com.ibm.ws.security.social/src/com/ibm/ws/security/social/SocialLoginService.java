/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
