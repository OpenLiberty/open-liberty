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

import javax.management.ObjectName;
import com.ibm.websphere.pmi.PmiConstants;

/**
 * This class represents the performance monitoring/instrumentation specification for a JMX Managed Object.
 * The specification allows to enable or disable statistics individually.
 * In 5.0, the <code> MBeanLevelSpec </code> includes an MBean ObjectName, an optional StatDescriptor, and an integer level.
 * In 6.0, the <code> MBeanLevelSpec </code> includes an MBean ObjectName and a list of statistic IDs that needs to be enabled.
 * 
 * @ibm-api
 */

public class MBeanLevelSpec implements java.io.Serializable {
    private static final long serialVersionUID = -3519125759099800020L;

    /**
     * Indicate all statistics that available for the given MBean
     */
    public static final int ALL_STATISTICS = PmiConstants.ALL_DATA;

    // private variables
    private ObjectName mName = null;
    private StatDescriptor sd = null;
    private int level = PmiConstants.LEVEL_UNDEFINED;

    // default is empty
    private int[] enable = new int[0];

    //private int[] enableSync = new int[] {PmiConstants.LEVEL_UNDEFINED};           

    /**
     * 
     * @param mName should be a valid ObjectName (not null).
     * @param level the instrumentation level. Level are defined in com.ibm.websphere.pmi.PmiConstants {LEVEL_NONE, LEVEL_LOW, LEVEL_MEDIUM, LEVEL_HIGH, LEVEL_MAX}.
     * 
     * @deprecated As of 6.0, replaced by MBeanLevelSpec(ObjectName mName, int[] enable).
     */
    public MBeanLevelSpec(ObjectName mName, int level) {
        this(mName, null, level);
    }

    /**
     * @param mName should be a valid ObjectName (not null).
     * @param sd an optional StatDescriptor (could be null).
     * @param level the instrumentation level for it.
     * 
     * @deprecated As of 6.0, replaced by MBeanLevelSpec(ObjectName mName, int[] enable).
     */
    public MBeanLevelSpec(ObjectName mName, StatDescriptor sd, int level) {
        this.mName = mName;
        this.sd = sd;
        this.level = level;
    }

    /**
     * Constructs a monitoring specification to selectively enable statistics.
     * 
     * @param mName A valid MBean ObjectName (not null) for which statistics needs to be enabled.
     * @param enable List of statistic ID that needs be enabled.
     *            Only the statistics specified in this list will be enabled
     *            and the statistics that are not specified in this list will be disabled.<br>
     *            Use new int[MBeanLevelSpec.ALL_STATISTICS] to enable all the statistics that are available for this MBean
     *            and new int[0] to disable all statistics.
     */
    public MBeanLevelSpec(ObjectName mName, int[] enable) //, int[] enableSync)
    {
        this.mName = mName;
        this.enable = enable;
        this.level = PmiConstants.LEVEL_FINEGRAIN;
        //this.enableSync = enableSync;
    }

    /**
     * Set statistics that needs to be enabled.
     * 
     * @param enabled List of statistic ID that needs be enabled.
     *            Only the statistics specified in this list will be enabled
     *            and the statistics that are not specified in this list will be disabled.<br>
     *            Use new int[MBeanLevelSpec.ALL_STATISTICS] to enable all the statistics and
     *            new int[0] to disable all statistics.
     */
    public void setEnabled(int[] enabled) {
        this.level = PmiConstants.LEVEL_FINEGRAIN;
        this.enable = enabled;
    }

    /*
     * public void setEnabledSync (int[] enabledSync)
     * {
     * this.enableSync = enabledSync;
     * }
     */

    /**
     * Get MBean ObjectName.
     * 
     * @return MBean ObjectName
     */
    public ObjectName getObjectName() {
        return mName;
    }

    /**
     * Get StatDescriptor.
     * 
     * @deprecated No replacement.
     */
    public StatDescriptor getStatDescriptor() {
        return sd;
    }

    /**
     * Get PMI monitoring level.
     * 
     * @deprecated No replacement.
     */
    public int getLevel() {
        return level;
    }

    /**
     * Get the list of statistics that are enabled.
     * MBeanLevelSpec.ALL_STATISTICS indicate all statistics are enabled
     * and new int[0] to disable all statistics.
     */
    public int[] getEnabled() {
        return enable;
    }

    /*
     * public int[] getEnabledSync()
     * {
     * return enableSync;
     * }
     */
    /**
     * Returns a String representation.
     */
    public String toString() {
        StringBuffer ret = new StringBuffer();
        if (mName != null)
            ret.append("ObjectName=" + mName.toString());

        if (sd != null)
            ret.append("///").append(sd.toString());

        ret.append("///level=").append(level);

        ret.append("///enabled={");

        if (enable != null) {
            for (int i = 0; i < enable.length; i++)
                ret.append(enable[i]).append(",");
        }
        ret.append("}");
        /*
         * ret.append("///");
         * if (enableSync != null)
         * {
         * for (int i = 0; i < enableSync.length; i++)
         * ret.append (enableSync[i]).append (",");
         * }
         */
        return ret.toString();
    }
}
