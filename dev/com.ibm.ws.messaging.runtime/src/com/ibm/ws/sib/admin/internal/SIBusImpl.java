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
package com.ibm.ws.sib.admin.internal;


import com.ibm.ws.sib.admin.SIBus;

/**
 *This class Implements the SIBus interface
 */
public class SIBusImpl implements SIBus {

    String uuid = null;
    String name = JsAdminConstants.DEFAULTBUS;

    /** {@inheritDoc} */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return name;
    }

    /** Nothing is set as we dont want user to change the name of the bus */
    @Override
    public void setName(String value) {
    // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void setUuid(String value) {
    // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public String getUuid() {
        // TODO Auto-generated method stub
        return uuid;
    }

    /**
     * All the other mock methods of JsEObject
     */

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void setDescription(String value) {
    // TODO Auto-generated method stub

    }

  
}
