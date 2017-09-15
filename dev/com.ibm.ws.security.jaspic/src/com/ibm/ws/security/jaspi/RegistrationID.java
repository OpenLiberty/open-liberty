/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * Date     Defect/feature CMVC ID   Description
 * -------- -------------- --------- -----------------------------------------------
 * 11/16/09 F743-19053.1   leou      Initial version
 * 11/29/09 630172         leou      Use RegistrationID as key in ProviderRegistry cache.
 *
 */
package com.ibm.ws.security.jaspi;

public class RegistrationID {

    private final String registrationID;

    public RegistrationID(String layer, String appContext) {
        super();
        this.registrationID = (layer == null ? "" : layer) + "[" + (appContext == null ? "" : appContext) + "]";
    }

    public RegistrationID(String registrationID) {
        this.registrationID = registrationID != null ? registrationID : "[]";
    }

    @Override
    public String toString() {
        return registrationID;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RegistrationID && ((RegistrationID) o).registrationID.equals(registrationID);
    }

    @Override
    public int hashCode() {
        return registrationID != null ? registrationID.hashCode() : "[]".hashCode();
    }

}
