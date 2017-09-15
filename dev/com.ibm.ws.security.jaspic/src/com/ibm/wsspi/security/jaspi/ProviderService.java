/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.jaspi;

import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;

/**
 * This is the OSGI service interface that a JASPI provider bundle
 * must provide to run on the WebSphere application server Liberty profile.
 */
public interface ProviderService {
    /**
     * This method is called to construct the AuthConfigProvider by invoking
     * the JSR-196 defined constructor of the AuthConfigProvider:
     * <p><code>
     * public MyAuthConfigProviderImpl(java.util.Map properties, AuthConfigFactory factory);
     * </code>
     * <p>This method may read it's own provider configuration properties and
     * and pass them to the constructor.
     * <p>
     * @param factory An AuthConfigFactory instance
     * @return An object instance that implements AuthConfigProvider
     */
    public AuthConfigProvider getAuthConfigProvider(AuthConfigFactory factory);
}
