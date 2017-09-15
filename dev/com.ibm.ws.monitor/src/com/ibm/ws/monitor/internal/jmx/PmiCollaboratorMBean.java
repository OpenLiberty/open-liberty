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

package com.ibm.ws.monitor.internal.jmx;

import com.ibm.websphere.pmi.PmiModuleConfig;
import com.ibm.websphere.pmi.stat.StatDescriptor;
import com.ibm.websphere.pmi.stat.StatLevelSpec;
import com.ibm.websphere.pmi.stat.WSStats;

public interface PmiCollaboratorMBean {
    /** List child StatDescriptors. Returns immediate children if recursive is false and all children if recursive is true */
    public StatDescriptor[] listStatMembers(StatDescriptor sd, Boolean recursive);

    /** Get instrumentation level using StatDescriptor */
    public StatLevelSpec[] getInstrumentationLevel(StatDescriptor sd, Boolean recursive);

    /** Set instrumentation level for multiple StatDescritors */
    public void setInstrumentationLevel(StatLevelSpec[] sls, Boolean recursive);

    /** Set instrumentation level using statistic sets. The sets are defined in com.ibm.websphere.pmi.stat.StatConstants */
    public void setStatisticSet(String statisticSet);

    /** Get static XML config info for all the PMI modules. A null for the Locale means default server locale */
    public PmiModuleConfig[] getConfigs(java.util.Locale locale);

    /** Get static XML config info for the given stats type. */
    public PmiModuleConfig getConfig(String statsType);

    /** It will return data from multiple Stats (i.e., PMI modules) */
    public WSStats[] getStatsArray(StatDescriptor[] mNames, Boolean recursive);

    /** It will return data from multiple Stats (i.e., PMI modules) */
    public String queryAllStatsAsString();

    /** Get current custom statistic settings */
    public String getCustomSetString();

    /** Set custom statistic set using fine-grained control */
    public void setCustomSetString(String setting, Boolean recursive);

    /** Append custom statistic set using fine-grained control */
    public void appendCustomSetString(String setting, Boolean recursive);
} // PmiCollaboratorMBean

