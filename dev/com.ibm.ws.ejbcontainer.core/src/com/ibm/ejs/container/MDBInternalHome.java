/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

/**
 * This interface is used to provide the methods that are
 * unique to a MDB home bean objects and is to be used only
 * internally by the EJB container component.
 */
public interface MDBInternalHome
{
    /**
     * Activate the home for a MDB so that it can receive messages
     * from a message provider.
     */
    void activateEndpoint()
                    throws Exception;

    /**
     * Deactivate a previously activated MDB home.
     */
    void deactivateEndpoint()
                    throws Exception;
}
