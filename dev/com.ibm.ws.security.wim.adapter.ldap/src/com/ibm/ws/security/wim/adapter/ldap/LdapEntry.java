/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.ldap;

import javax.naming.directory.Attributes;

/**
 * The class which contains the returned information of a entity from the LDAP query.
 */
public class LdapEntry {

    private String iDN = null;
    private String iExtId = null;
    private String iUniqueName = null;
    private String iType = null;
    private Attributes iAttrs = null;
    private String iChangeType = null;

    /**
     *
     */
    public LdapEntry(String dn, String extId, String uniqueName, String type, Attributes attrs) {
        iDN = dn;
        iExtId = extId;
        iUniqueName = uniqueName;
        iType = type;
        iAttrs = attrs;
        iChangeType = null;
    }

    /**
     * @return Returns the iAttrs.
     */
    public Attributes getAttributes() {
        return iAttrs;
    }

    /**
     * @param iAttrs The iAttrs to set.
     */
    public void setAttributes(Attributes attrs) {
        this.iAttrs = attrs;
    }

    /**
     * @return Returns the iDN.
     */
    public String getDN() {
        return iDN;
    }

    /**
     * @param dn The iDN to set.
     */
    public void setDN(String dn) {
        iDN = dn;
    }

    /**
     * @return Returns the iExtId.
     */
    public String getExtId() {
        return iExtId;
    }

    /**
     * @param extId The iExtId to set.
     */
    public void setExtId(String extId) {
        iExtId = extId;
    }

    /**
     * @return Returns the iType.
     */
    public String getType() {
        return iType;
    }

    /**
     * @param type The iType to set.
     */
    public void setType(String type) {
        iType = type;
    }

    /**
     * @return Returns the iUniqueName.
     */
    public String getUniqueName() {
        return iUniqueName;
    }

    /**
     * @param uniqueName The iUniqueName to set.
     */
    public void setIUniqueName(String uniqueName) {
        iUniqueName = uniqueName;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("\nDN: ").append(iDN).append("  ");
        result.append("ExtId: ").append(iExtId).append("  ");
        result.append("UniqueName: ").append(iUniqueName).append("  ");
        result.append("Type: ").append(iType).append("\n");
        result.append("Attributes: ").append(iAttrs);
        return result.toString();
    }

    /*
     * Sets the change type for an entity.
     * Applicable during a search for changed entities.
     */
    public void setChangeType(String changeType) {
        this.iChangeType = changeType;
    }

    /*
     * Gets the change type for an entity.
     * Applicable during a search for changed entities.
     */
    public String getChangeType() {
        return this.iChangeType;
    }
}
