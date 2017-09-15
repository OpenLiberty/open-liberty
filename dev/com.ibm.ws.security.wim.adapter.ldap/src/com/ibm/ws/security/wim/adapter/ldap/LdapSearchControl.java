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

import java.util.List;

import javax.naming.directory.SearchControls;

public class LdapSearchControl {

    private String[] iBases = null;
    private String iFilter = null;
    private int iCountLimit = 0;
    private int iTimeLimit = 0;
    private List<String> iPropNames = null;
    private List<String> iEntityTypes = null;
    private int iScope = SearchControls.SUBTREE_SCOPE;

    /**
     *
     */
    public LdapSearchControl(String[] bases, List<String> entityTypes, String filter, List<String> propNames, int countLimit,
                             int timeLimit) {
        iBases = bases;
        iEntityTypes = entityTypes;
        iFilter = filter;
        iPropNames = propNames;
        iCountLimit = countLimit;
        iTimeLimit = timeLimit;
    }

    /**
     * @return Returns the iBases.
     */
    public String[] getBases() {
        return iBases;
    }

    /**
     * @param bases The iBases to set.
     */
    public void setBases(String[] bases) {
        iBases = bases;
    }

    /**
     * @return Returns the iCountLimit.
     */
    public int getCountLimit() {
        return iCountLimit;
    }

    /**
     * @param countLimit The iCountLimit to set.
     */
    public void setCountLimit(int countLimit) {
        iCountLimit = countLimit;
    }

    /**
     * @return Returns the iEntityTypes.
     */
    public List<String> getEntityTypes() {
        return iEntityTypes;
    }

    /**
     * @param entityTypes The iEntityTypes to set.
     */
    public void setEntityTypes(List<String> entityTypes) {
        iEntityTypes = entityTypes;
    }

    /**
     * @return Returns the iFilter.
     */
    public String getFilter() {
        return iFilter;
    }

    /**
     * @return Returns the search scope.
     */
    public int getScope() {
        return iScope;
    }

    /**
     * @param filter The iFilter to set.
     */
    public void setFilter(String filter) {
        iFilter = filter;
    }

    /**
     * @return Returns the iPropNames.
     */
    public List<String> getPropertyNames() {
        return iPropNames;
    }

    /**
     * @param propNmaes The iPropNmaes to set.
     */
    public void setPropertiyNmaes(List<String> propNmaes) {
        iPropNames = propNmaes;
    }

    /**
     * @return Returns the iTimeLimit.
     */
    public int getTimeLimit() {
        return iTimeLimit;
    }

    /**
     * @param timeLimit The iTimeLimit to set.
     */
    public void setTimeLimit(int timeLimit) {
        iTimeLimit = timeLimit;
    }

    public void setScope(int scope) {
        iScope = scope;
    }
}
