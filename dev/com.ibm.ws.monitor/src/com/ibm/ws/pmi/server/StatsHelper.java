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

package com.ibm.ws.pmi.server;

import com.ibm.websphere.pmi.*;

import com.ibm.ws.pmi.stat.*;

/**
 * It converts Stats objects to String. We keep the 4.0 wscp format.
 */
public class StatsHelper {
    // TODO: change to StringBuffer for better performance
    public static String statsToTclAttrString(StatsImpl stats, DataDescriptor dd, String nodeName, String serverName) {
        return statsToTclAttrString(null, stats, dd, nodeName, serverName);
    }

    public static String statsToTclAttrString(String desc, StatsImpl stats, DataDescriptor dd, String nodeName, String serverName) {
        if (stats == null)
            return "";

        StringBuffer rv = new StringBuffer(256);
        String childdesc = desc;

        // ** CustomPMI
        String modName = stats.getStatsType();
        if (modName == null)
            modName = dd.getModuleName();

        if (modName == null)
            modName = "pmi";

        if (desc == null) {
            if (modName.equals(PmiConstants.APPSERVER_MODULE)) {
                desc = "PMI data for server";
                childdesc = null;
            } else {
                PmiModuleConfig config = PmiRegistry.getConfig(modName);
                desc = config.getDescription();
                childdesc = desc;
            }
        }

        // no good description
        rv.append("{Description ").append(desc).append("} ");

        rv.append("{Descriptor ").append(safeString(pmiDescriptorToTclAttrString(modName, dd, nodeName, serverName))).append("} ");
        rv.append("{Level ").append(stats.getLevel()).append("}");

        // local variable for for loop
        DataDescriptor mydd = null;

        StatisticImpl[] dataMembers = (StatisticImpl[]) stats.listStatistics();
        if (dataMembers != null && dataMembers.length > 0) {
            rv.append(" {Data {");
            for (int i = 0; i < dataMembers.length; i++) {
                mydd = new DataDescriptor(dd, dataMembers[i].getId());
                rv.append("{").append(statisticToTclAttrString(dataMembers[i], stats.getTime(), modName, mydd, nodeName, serverName)).append("}");
                if (i < dataMembers.length - 1)
                    rv.append(" ");
            } // for
            rv.append("}}");
        }

        StatsImpl[] substats = (StatsImpl[]) stats.listSubStats();
        if (substats != null && substats.length > 0) {
            rv.append(" {SubCollections {");
            for (int i = 0; i < substats.length; i++) {
                mydd = new DataDescriptor(dd, substats[i].getName());
                rv.append("{").append(statsToTclAttrString(childdesc, substats[i], mydd, nodeName, serverName)).append("}");
                if (i < substats.length - 1)
                    rv.append(" ");
            } // for
            rv.append("}}");
        }
        return rv.toString();
    }

    public static String statisticToTclAttrString(StatisticImpl stats, long time, String modName, DataDescriptor dd, String nodeName, String serverName) {
        if (stats == null)
            return "";

        StringBuffer rv = new StringBuffer(256);
        rv.append("{Id ").append(stats.getId()).append("} ");

        // comment it out because it is duplicate in PmiDataInfo block.
        //rv.append("{Description ").append(safeString(stats.getDescription())).append("} ");

        rv.append("{Descriptor ").append(safeString(pmiDescriptorToTclAttrString(modName, dd, nodeName, serverName))).append("} ");

        rv.append("{PmiDataInfo ").append(safeString(pmiDataInfoToTclAttrString(stats.getDataInfo()))).append("} ");

        rv.append("{Time ").append(time).append("} ");

        // the data value will depend on type
        rv.append("{Value ").append(statisticToValue(stats)).append("}");

        // rv.append("{Value ").append(stats.getValue()).append("}");
        return rv.toString();
    }

    private static String statisticToValue(StatisticImpl statData) {
        if (statData == null)
            return "";

        StringBuffer rv = new StringBuffer();

        if (statData instanceof CountStatisticImpl) {
            CountStatisticImpl tmp = (CountStatisticImpl) statData;
            rv.append("{Count ").append(tmp.getCount()).append("} ");
        } else if (statData instanceof TimeStatisticImpl) {
            TimeStatisticImpl tmp = (TimeStatisticImpl) statData;
            rv.append("{Total ").append(tmp.getTotal()).append("} ");
            rv.append("{Count ").append(tmp.getCount()).append("} ");
            rv.append("{Mean ").append(tmp.getMean()).append("} ");
        } else if (statData instanceof RangeStatisticImpl) {
            RangeStatisticImpl tmp = (RangeStatisticImpl) statData;
            rv.append("{Current ").append(tmp.getCurrent()).append("} ");
            rv.append("{LowWaterMark ").append(tmp.getLowWaterMark()).append("} ");
            rv.append("{HighWaterMark ").append(tmp.getHighWaterMark()).append("} ");
            rv.append("{MBean ").append(tmp.getMean()).append("} ");
        }
        return rv.toString();
    }

    public static String pmiDataInfoToTclAttrString(PmiDataInfo pmi) {
        if (pmi == null)
            return "";

        StringBuffer rv = new StringBuffer();

        rv.append("{Name ").append(safeString(pmi.getName())).append("} ");
        rv.append("{Id ").append(pmi.getId()).append("} ");
        rv.append("{Description ").append(safeString(pmi.getDescription())).append("} ");
        rv.append("{Level ").append(pmi.getLevel()).append("} ");
        rv.append("{Comment ").append(safeString(pmi.getComment())).append("} ");
        rv.append("{SubmoduleName ").append(safeString(pmi.getSubmoduleName())).append("} ");
        rv.append("{Type ").append(pmi.getType()).append("} ");
        rv.append("{Unit ").append(pmi.getUnit()).append("} ");
        rv.append("{Resettable ").append(pmi.isResettable()).append("}");

        return rv.toString();
    }

    public static String pmiDescriptorToTclAttrString(String modName, DataDescriptor dd, String nodeName, String serverName) {
        StringBuffer rv = new StringBuffer();

        rv.append("{Node ").append(safeString(nodeName)).append("} ");
        rv.append("{Server ").append(safeString(serverName)).append("} ");

        if (dd.getType() != PmiConstants.TYPE_SERVER) {
            rv.append("{Module ").append(safeString(modName)).append("} ");
            rv.append("{Name ").append(safeString(dd.getName())).append("} ");
        }
        rv.append("{Type ").append(pdTypeToString(dd.getType())).append("}");

        return rv.toString();
    }

    public static String pdTypeToString(int i) {
        switch (i) {
            case PmiConstants.TYPE_ROOT:
                return "ROOT";
            case PmiConstants.TYPE_NODE:
                return "NODE";
            case PmiConstants.TYPE_SERVER:
                return "SERVER";
            case PmiConstants.TYPE_MODULE:
                return "MODULE";
            case PmiConstants.TYPE_INSTANCE:
                return "INSTANCE";
            case PmiConstants.TYPE_SUBMODULE:
                return "SUBMODULE";
            case PmiConstants.TYPE_SUBINSTANCE:
                return "SUBINSTANCE";
            case PmiConstants.TYPE_COLLECTION:
                return "COLLECTION";
            case PmiConstants.TYPE_DATA:
                return "DATA";
            case PmiConstants.TYPE_MODULEROOT:
                return "MODULEROOT";
            default:
                return "UNKNOWN";
        }
    }

    private static String safeString(String in) {
        if (null == in)
            return in;
        if (-1 != in.indexOf(" "))
            return "{" + in + "}";
        else
            return in;
    }
}
