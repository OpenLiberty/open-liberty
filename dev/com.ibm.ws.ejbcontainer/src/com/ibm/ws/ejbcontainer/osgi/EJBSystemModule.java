/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi;

import com.ibm.ws.ejbcontainer.EJBReferenceFactory;

/**
 * A handle to a started system module.
 */
public interface EJBSystemModule {
    /**
     * This method must be called when the EJBs in the module should no longer
     * be accessible.
     */
    void stop();

    /**
     * Returns a factory for obtaining references to EJBs in the module
     *
     * @param ejbName the {@linkplain EJBSystemBeanConfig#getName EJB name}
     * @return the reference factory
     * @throws IllegalArgumentException if the EJB name is not valid
     */
    EJBReferenceFactory getReferenceFactory(String ejbName);
}
