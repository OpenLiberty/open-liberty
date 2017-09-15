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

package com.ibm.ws.security.wim.registry.dataobject;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.wim.registry.WIMUserRegistryDefines;

/**
 * Data object to manage the user/group and realm parameters used in the UserRegistry methods.
 *
 */
@Trivial
public class IDAndRealm implements WIMUserRegistryDefines {

    /**
     * UserRegistry ID.
     */
    private String id = "";

    /**
     * UserRegistry realm delimiter.
     */
    private String delimiter = "";

    /**
     * UserRegistry realm.
     */
    private String realm = "";

    /**
     * Flag if the realm is defined.
     */
    private boolean realmDefined = false;

    /**
     * ID/Realm constructor.
     */
    public IDAndRealm() {}

    /**
     * ID/Realm constructor.
     */
    public IDAndRealm(String inputID, String inputRealm) {
        setId(inputID);
        setRealm(inputRealm);
    }

    /**
     * Get the id.
     *
     * @return Returns the id.
     *
     * @post $return != null
     */
    public String getId() {
        return this.id;
    }

    /**
     * Set the id.
     *
     * @param inputId The id to set.
     *
     * @pre inputId != null
     * @pre inputId != ""
     */
    public void setId(String inputId) {
        if ((inputId != null) && (!inputId.equals(""))) {
            this.id = inputId;
        }
    }

    /**
     * Get the realm.
     *
     * @return Returns the realm.
     *
     * @post $return != null
     */
    public String getRealm() {
        return this.realm;
    }

    /**
     * Set the realm.
     *
     * @param inputRealm The realm to set.
     *
     * @pre inputRealm != null
     * @pre inputRealm != ""
     */
    public void setRealm(String inputRealm) {
        if ((inputRealm != null) && (!inputRealm.equals(""))) {
            this.realm = inputRealm;
            setRealmDefined(true);
        }
    }

    /**
     * Determine if the realm is defined for this data object.
     *
     * @return Returns the realmDefined.
     */
    public boolean isRealmDefined() {
        return this.realmDefined;
    }

    /**
     * Set the realmDefined flag.
     *
     * @param realmDefined The realmDefined to set.
     */
    private void setRealmDefined(boolean inputRealmDefined) {
        this.realmDefined = inputRealmDefined;
    }

    /**
     * Get the realm delimiter.
     *
     * @return Returns the delimiter.
     *
     * @post $return != null
     */
    public String getDelimiter() {
        return this.delimiter;
    }

    /**
     * Set the realm delimiter.
     *
     * @param inputDelimiter The delimiter to set.
     *
     * @pre inputDelimiter != null
     * @pre inputDelimiter != ""
     */
    public void setDelimiter(String inputDelimiter) {
        if ((inputDelimiter != null) && (!inputDelimiter.equals(""))) {
            this.delimiter = inputDelimiter;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        // initialize the return value
        StringBuffer returnValue = new StringBuffer();
        returnValue.append(IDAndRealm.class.getName());
        returnValue.append(" ID = \"" + getId() + "\"");
        returnValue.append(" Realm = \"" + getRealm() + "\"");
        returnValue.append(" isRealmDefined = \"" + isRealmDefined() + "\"");
        return returnValue.toString();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object inputObject) {
        // initialize the return value
        boolean returnValue = false;
        if (inputObject != null && inputObject instanceof IDAndRealm) {
            if ((getId().equals(((IDAndRealm) inputObject).getId()))
                && (getRealm().equals(((IDAndRealm) inputObject).getRealm()))
                && (isRealmDefined() == (((IDAndRealm) inputObject).isRealmDefined())))

            {
                returnValue = true;
            } else {
                returnValue = false;
            }
        } else {
            returnValue = false;
        }
        return returnValue;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        // initialize the return value
        int returnValue = 0;
        returnValue += getId().hashCode();
        returnValue += getRealm().hashCode();
        returnValue += Boolean.valueOf(isRealmDefined()).hashCode();
        return returnValue;
    }
}
