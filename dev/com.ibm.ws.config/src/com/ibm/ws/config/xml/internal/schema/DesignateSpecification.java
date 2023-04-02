/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package com.ibm.ws.config.xml.internal.schema;

/**
 *
 */
class DesignateSpecification {

    private String pid;
    private boolean isFactory;
    private String ocdId;

    /**
     * @param pid
     * @param isFactory
     * @param ocdref
     */
    public DesignateSpecification() {}

    public void setPid(String pid) {
        this.pid = pid;
    }

    public void setOcdId(String ocdId) {
        this.ocdId = ocdId;
    }

    public void setIsFactory(boolean isFactory) {
        this.isFactory = isFactory;
    }

    /**
     * @return the pid
     */
    public String getPid() {
        return pid;
    }

    /**
     * @return the isFactory
     */
    public boolean isFactory() {
        return isFactory;
    }

    /**
     * @return the ocdId
     */
    public String getOcdId() {
        return ocdId;
    }

    /** debug info */
    @Override
    public String toString() {
        return "DesignateSpecification [pid=" + pid + ", isFactory=" + isFactory + ", ocdId=" + ocdId + "]";
    }

}
