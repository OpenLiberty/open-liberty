/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.naming;

import com.ibm.ejs.container.HomeRecord;

/**
 * Represents EJB object data held for lookup
 */
public class EJBBinding {
    public final HomeRecord homeRecord;
    public final String interfaceName;

    /**
     * The interface index, or -1 for home.
     */
    public final int interfaceIndex;

    public final boolean isLocal;

    /**
     * Create EJB binding data
     *
     * @param interfaceIndex the business interface index, or -1 for home
     */
    public EJBBinding(HomeRecord homeRecord, String interfaceName, int interfaceIndex, boolean local) {
        this.homeRecord = homeRecord;
        this.interfaceName = interfaceName;
        this.interfaceIndex = interfaceIndex;
        this.isLocal = local;
    }

    public boolean isHome() {
        return interfaceIndex == -1;
    }

    @Override
    public String toString() {
        return super.toString() +
               '[' + homeRecord.getJ2EEName() +
               ", " + interfaceName +
               ", " + interfaceIndex +
               ']';
    }
}
