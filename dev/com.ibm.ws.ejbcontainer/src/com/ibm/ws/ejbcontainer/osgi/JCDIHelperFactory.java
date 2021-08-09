/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi;

import com.ibm.ws.ejbcontainer.JCDIHelper;
import com.ibm.wsspi.adaptable.module.Container;

/**
 * This is an abstraction for EJB container to be able to optionally depend on CDI.
 */
public interface JCDIHelperFactory {

    /**
     * Returns a JCDIHelper for the specified container representing an EJB module, or null if the module is not CDI-enabled or CDI is not enabled in the server
     * 
     * @param container The container that will be tested to see if it is CDI enabled
     * @return a JCDIHelper object if both the container and server are CDI enabled, null otherwise
     */
    public JCDIHelper getJCDIHelper(Container container);
}
