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

package com.ibm.websphere.pmi.stat;

import com.ibm.websphere.pmi.PmiConstants;

/**
 * 
 * This class is used to identify a Stats object in the WebSphere PMI.
 * <br>
 * Typically, the JMX ObjectName is used to locate a managed object in the J2EE domain. The ObjectName
 * can be used get statistics about the managed object.
 * When a JMX ObjectName is not available StatDescriptor can be used to locate the Stats.<br>
 * WebSphere Performance Monitoring Infrastructure (PMI) maintains the Stats from various components in a tree structure.
 * Following is a sample Stats tree:
 * <br><br>server1
 * <br>&nbsp;&nbsp;&nbsp; |__ WSJVMStats
 * <br>&nbsp;&nbsp;&nbsp; |__ WSThreadPoolStats
 * <br>&nbsp;&nbsp;&nbsp; |__ WSEJBStats
 * <br>&nbsp;&nbsp;&nbsp; |__ WSWebAppStats
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; |__ &lt;MyApplication.war&gt;
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; |__ WSServletStats
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; |__ &lt;Servlet_1&gt;
 * 
 * 
 * <br>
 * <br>
 * StatDescriptor is used to locate and access particular Stats in the PMI tree.
 * For instance, StatDescriptor that represents Servlet_1 Stats can be constructed as follows:
 * <br><code>new StatDescriptor (new String[] {WSWebAppStats.NAME, "MyApplication.war", WSServletStats.NAME, "Servlet_1"}); </code>
 * 
 * @ibm-api
 */

/*
 * The <code> StatDescriptor </code> is a descriptor used to specify
 * a subset of PMI data in a MBean. It should be passed with a MBean ObjectName
 * to create the corresponding MBeanStatDescriptor and/or MBeanLevelSpec.
 * For example, if you request PMI data for a specific EJB method, you will
 * have to give a StatDescriptor since there is no MBean for individual EJB method.
 */

public class StatDescriptor implements java.io.Serializable {
    private static final long serialVersionUID = -2844135786824830882L;

    private String[] subPath = null;
    private int dataId = PmiConstants.ALL_DATA; // a positive dataId means it represents a data

    /**
     * Constructor
     * 
     * @param path Path of the Stats in the PMI tree. A null indicates the root of PMI tree (server).
     */
    public StatDescriptor(String[] path) {
        this.subPath = path;
    }

    /**
     * @deprecated No replacement
     */
    public StatDescriptor(String[] path, int dataId) {
        this.subPath = path;
        this.dataId = dataId;
    }

    /**
     * 
     * Returns Stats path represented by this StatDescriptor
     */
    public String[] getPath() {
        return subPath;
    }

    /**
     * @deprecated No replacement
     */
    public String getName() {
        if (subPath == null || subPath.length == 0)
            return null;
        else
            return subPath[subPath.length - 1];
    }

    /**
     * @deprecated No replacement
     */
    public int getDataId() {
        return dataId;
    }

    /**
     * @deprecated No replacement
     */
    public int getType() {
        if (dataId == PmiConstants.ALL_DATA)
            return PmiConstants.TYPE_COLLECTION;
        else
            return PmiConstants.TYPE_DATA;
    }

    /**
     * @deprecated No replacement
     */
    public boolean isSame(StatDescriptor sd) {
        if (sd == null)
            return false;
        String[] otherPath = sd.getPath();
        if (subPath == null && otherPath == null)
            return true;
        else if (subPath == null || otherPath == null)
            return false;
        else if (subPath.length != otherPath.length)
            return false;
        else {
            for (int i = 0; i < subPath.length; i++) {
                if (!subPath[i].equals(otherPath[i]))
                    return false;
            }
            if (dataId != sd.getDataId())
                return false;
            else
                return true;
        }
    }

    /**
     * Returns String representation of StatDescriptor
     */
    public String toString() {
        if (subPath == null || subPath.length == 0)
            return null;

        StringBuffer ret = new StringBuffer(subPath[0]);
        for (int i = 1; i < subPath.length; i++) {
            ret.append(">").append(subPath[i]);
        }

        /*
         * if(dataId > 0)
         * {
         * ret.append("/").append(dataId);
         * }
         */
        return ret.toString();
    }
}
