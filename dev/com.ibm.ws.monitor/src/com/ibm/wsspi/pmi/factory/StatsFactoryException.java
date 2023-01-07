/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
