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

/**
 * This class is a "Java typesafe enum", the values of which represent different
 * types of configured destination.
 * 
 * @author philip
 */
public class DestinationConfigType {

    public final static DestinationConfigType LOCAL = new DestinationConfigType("Local", 0);

    public final static DestinationConfigType ALIAS = new DestinationConfigType("Alias", 1);

    public final static DestinationConfigType FOREIGN = new DestinationConfigType("Foreign", 2);

    private final String name;
    private final int value;
    private final static DestinationConfigType[] set = { LOCAL, ALIAS, FOREIGN };

    /**
     * @param name
     * @param value
     */
    private DestinationConfigType(String name, int value) {
        this.name = name;
        this.value = value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public final String toString() {
        return name;
    }

    /**
     * Returns an integer value representing the instance of this class
     * 
     * @return
     */
    public final int toInt() {
        return value;
    }

    /**
     * Get the DestinationConfigType represented by the given integer value;
     * 
     * @param value the integer representation of the required DestinationType
     * @return the DestinationType represented by the given integer value
     */
    public final static DestinationConfigType getDestinationType(int value) {
        return set[value];
    }
}
