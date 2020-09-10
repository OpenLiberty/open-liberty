/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.naming;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ejs.container.HomeRecord;
import com.ibm.websphere.csi.J2EEName;

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

    public boolean isAmbiguousReference = false;

    public List<J2EEName> j2eeNames = new ArrayList<J2EEName>();

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
        this.j2eeNames.add(homeRecord.getJ2EEName());
    }

    public boolean isHome() {
        return interfaceIndex == -1;
    }

    public void setAmbiguousReference() {
        this.isAmbiguousReference = true;
    }

    public List<J2EEName> getJ2EENames() {
        return this.j2eeNames;
    }

    public void addJ2EENames(List<J2EEName> nameList) {
        this.j2eeNames.addAll(nameList);
    }

    @Override
    public String toString() {
        return super.toString() +
               '[' + homeRecord.getJ2EEName() +
               ", " + interfaceName +
               ", " + interfaceIndex +
               ", " + isLocal +
               ", " + isAmbiguousReference +
               ", " + j2eeNames.toString() +
               ']';
    }
}
