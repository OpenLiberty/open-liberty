/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.pmi.factory;

/**
 * StatsFactoryException is thrown from StatsFactory to indicate an error while creating a StatsInstance or a StatsGroup.
 * 
 * @ibm-spi
 */

public class StatsFactoryException extends java.lang.Exception {
    private static final long serialVersionUID = 3038181289491772545L;

    /**
     * Default Constructor
     */
    public StatsFactoryException() {
        super();
    }

    /**
     * Constructor taking a String.
     * 
     * @param s message
     */
    public StatsFactoryException(String s) {
        super(s);
    }
}
