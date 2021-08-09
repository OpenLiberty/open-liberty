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
 * This class represents the performance monitoring/instrumentation specification for a {@link com.ibm.websphere.pmi.stat.StatDescriptor}.
 * The specification allows to enable or disable statistics individually. The statistic IDs are defined in WS*Stats interface in <code>com.ibm.websphere.pmi.stat</code>
 * package. For example, JVM statistics are defined in {@link com.ibm.websphere.pmi.stat.WSJVMStats}.
 * 
 * @ibm-api
 */
public class StatLevelSpec implements java.io.Serializable {
    private static final long serialVersionUID = -5784101041304135137L;
    /**
     * Indicate all statistics that available for the given StatDescriptor
     */
    public static final int ALL_STATISTICS = PmiConstants.ALL_DATA;
    private String[] path = null;
    private int[] enable = new int[] { ALL_STATISTICS };

    /**
     * Constructs a monitoring specification to selectively enable statistics.
     * 
     * @param path Stats for which statistics needs to be enabled. A null indicates the root of PMI tree (server).
     * @param enable List of statistic ID that needs be enabled. If path is null then <code>new int[StatLevelSpec.ALL_STATISTICS]</code> is the only valid value for this parameter.
     *            Only the statistics specified in this list will be enabled
     *            and the statistics that are not specified in this list will be disabled.<br>
     *            Use new int[StatLevelSpec.ALL_STATISTICS] to enable all the statistics that are available for this StatDescriptor.
     */
    public StatLevelSpec(String[] path, int[] enable) {
        this.path = path;
        this.enable = enable;
    }

    /**
     * Get StatDescriptor
     */
    public String[] getPath() {
        return path;
    }

    /**
     * Get the list of statistics that are enabled.
     * StatLevelSpec.ALL_STATISTICS indicate all statistics are enabled.
     */
    public int[] getEnabled() {
        return enable;
    }

    /**
     * Set StatDescriptor
     * 
     * @param path Path of the stats in the PMI tree. A null indicates the root of PMI tree (server).
     */
    public void setPath(String[] path) {
        this.path = path;
    }

    /**
     * Set statistics that needs to be enabled.
     * 
     * @param enabled List of statistic ID that needs be enabled.
     *            Only the statistics specified in this list will be enabled
     *            and the statistics that are not specified in this list will be disabled.<br>
     *            Use new int[MBeanLevelSpec.ALL_STATISTICS] to enable all the statistics.
     */
    public void setEnabled(int[] enabled) {
        this.enable = enabled;
    }

    /**
     * Returns String representation of StatLevelSpec
     */
    public String toString() {
        if (path == null || path.length == 0)
            return null;

        StringBuffer ret = new StringBuffer(path[0]);
        for (int i = 1; i < path.length; i++) {
            ret.append(">").append(path[i]);
        }

        ret.append("=");
        if (enable != null && enable.length > 0) {

            ret.append((enable[0] == ALL_STATISTICS) ? "*" : String.valueOf(enable[0]));
            for (int k = 1; k < enable.length; k++)
                ret.append(",").append(enable[k]);
        }
        return ret.toString();
    }
}
