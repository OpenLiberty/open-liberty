/*******************************************************************************
 * Copyright (c) 2016,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.example.jca.adapter;

/**
 *
 */
public class DestinationImpl implements Destination {

    private String Destination;

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.example.jca.adapter.Destination#setDestination(java.lang.String)
     */
    @Override
    public void setDestination(String Destination) {
        this.Destination = Destination;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.example.jca.adapter.Destination#getDestination()
     */
    @Override
    public String getDestination() {
        return Destination;
    }
}
