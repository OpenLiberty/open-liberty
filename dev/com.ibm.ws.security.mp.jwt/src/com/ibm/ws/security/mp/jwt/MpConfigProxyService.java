/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt;

import java.util.NoSuchElementException;
import java.util.Set;

public interface MpConfigProxyService {

    /**
     * @return
     */
    public String getVersion();

    /**
     * @return
     */
    public boolean isMpConfigAvailable();

    /**
     * @return
     */
    public <T> T getConfigValue(ClassLoader cl, String propertyName, Class<T> propertyType) throws IllegalArgumentException, NoSuchElementException;

    public Set<String> getSupportedConfigPropertyNames();
}
