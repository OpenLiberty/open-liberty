/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.monitor.internal.jmx;

import java.util.StringTokenizer;

import com.ibm.websphere.pmi.PmiConstants;
import com.ibm.websphere.pmi.PmiModuleConfig;
import com.ibm.websphere.pmi.stat.StatDescriptor;
import com.ibm.websphere.pmi.stat.StatLevelSpec;
import com.ibm.websphere.pmi.stat.WSStats;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.pmi.server.DataDescriptor;
import com.ibm.ws.pmi.server.PerfLevelDescriptor;
import com.ibm.ws.pmi.server.PmiRegistry;
import com.ibm.ws.pmi.stat.StatsImpl;

public class PmiCollaborator implements PmiCollaboratorMBean {
    // trace
    private final static TraceComponent tc = Tr.register(PmiCollaborator.class);

    private static PmiCollaborator _instance = null;

    private PmiCollaborator() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "init: PmiCollaboratorImpl");
    }

    public static PmiCollaborator getSingletonInstance() {
        if (_instance == null)
            _instance = new PmiCollaborator();

        return _instance;
    }

    /** New in 6.0 */
    @Override
    public StatDescriptor[] listStatMembers(StatDescriptor sd, Boolean recursive) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "listStatMembers(StatDescriptor, Boolean)");

        StatDescriptor[] members = PmiRegistry.listStatMembers(sd, recursive.booleanValue());

        if (tc.isEntryEnabled())
            Tr.exit(tc, "listStatMembers(StatDescriptor, Boolean)");
        return members;
    }

    /** New in 6.0 */
    @Override
    public void setStatisticSet(String statSet) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setStatisticSet(statSet): " + statSet);

        PmiRegistry.setInstrumentationLevel(statSet);

        // update config (Liberty: No Config to update)
        //com.ibm.ws.pmi.component.PMIImpl.setStatisticSet(statSet);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setStatisticSet(statSet)");
    }

    /** New in 6.0: Get current custom statistic settings */
    @Override
    public String getCustomSetString() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getCustomSetString");

        String ret = PmiRegistry.getInstrumentationLevelString60();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getCustomSetString");

        return ret;
    }

    /** New in 6.0: Set custom statistic set using fine-grained control */
    @Override
    public void setCustomSetString(String setting, Boolean recursive) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setCustomSetString (String, Boolean): " + setting);

        setInstrumentationLevel(_createSLSFromString(setting), recursive);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setCustomSetString");
    }

    //@@@@@@@@@@@@@@
    @Override
    public void appendCustomSetString(String setting, Boolean recursive) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "appendCustomSetString (String, Boolean): " + setting);

        appendInstrumentationLevel(_createSLSFromString(setting), recursive);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "appendCustomSetString");
    }

    //@@@@@@@@@@@@@@

    private StatLevelSpec[] _createSLSFromString(String spec) {
        StringTokenizer psr1 = new StringTokenizer(spec, ":");
        int s = psr1.countTokens();

        StatLevelSpec[] sls = new StatLevelSpec[s];
        int k = 0;
        while (psr1.hasMoreTokens()) {
            sls[k++] = _createSLS(psr1.nextToken());
        }

        return sls;
    }

    private StatLevelSpec _createSLS(String spec) {
        StringTokenizer psr1 = new StringTokenizer(spec, "=");

        String sd[];
        try {
            sd = parsePath(psr1.nextToken());
        } catch (Exception e) {
            return null;
        }

        String enable;
        try {
            enable = psr1.nextToken();
        } catch (Exception e) {
            enable = null;
        }

        return new StatLevelSpec(sd, parseSpecStr(enable));
    }

    private static String[] parsePath(String path) {
        StringTokenizer p = new StringTokenizer(path, ">");
        String[] ret = new String[p.countTokens()];
        int i = 0;
        while (p.hasMoreTokens()) {
            ret[i++] = p.nextToken();
        }

        return ret;
    }

    /**
     * Helper function to parse the configuration's spec strings.
     * 
     * @param in The configuration string.
     * @return An integer array of all the counters enabled by the specified string.
     */
    public static int[] parseSpecStr(String in) {
        if (in == null) {
            return new int[0];
        }

        in = in.replaceAll(" ", "");
        in = in.trim();

        if (in.length() == 0) {
            return new int[0];
        }

        String[] tokens = in.split(",");
        int[] toReturn = new int[tokens.length];

        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = tokens[i].trim();

            try {
                toReturn[i] = Integer.parseInt(tokens[i]);
            } catch (NumberFormatException e) {

                if (tokens[i].equals("*") && i == 0) {
                    toReturn[i] = PmiConstants.ALL_DATA;
                }

                // invalid configuration specification, set to undefined (no op)
                else {
                    toReturn[i] = PmiConstants.LEVEL_UNDEFINED;
                }
            }
        }

        return toReturn;
    }

    /*
     * // ** JSR77 TEST METHOD **
     * public javax.management.j2ee.statistics.Stats getJ2EEStats (ObjectName mName)
     * {
     * if(tc.isEntryEnabled())
     * Tr.entry(tc, "getStatsObject(ObjectName)");
     * 
     * //com.ibm.ws.management.collaborator.PmiJmxBridge bridge = PmiRegistry.getPmiJmxBridge (mName);
     * ModuleItem bridge = (ModuleItem)PmiRegistry.getPmiJmxBridge (mName);
     * 
     * if (bridge != null)
     * {
     * return bridge.getStats();
     * }
     * else
     * {
     * System.out.println ("[PmiCollaboratorImpl] Bridge is NULL!");
     * return null;
     * }
     * }
     */

    @Override
    public WSStats[] getStatsArray(StatDescriptor[] sd, Boolean recursive) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getStatsArray (StatDescriptor[], Boolean)");

        if (sd == null || sd.length == 0) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "StatDescriptor is null or empty");
            return null;
        }

        StatsImpl[] stats = PmiRegistry.getStats(sd, recursive.booleanValue(), new PmiModuleConfig(null));

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getStatsArray (StatDescriptor[], Boolean)");

        return stats;
    }

    /** {@inheritDoc} */
    @Override
    public String queryAllStatsAsString() {
        WSStats[] s = getStatsArray(new StatDescriptor[] { new StatDescriptor(null) }, true);
        if (s == null) {
            return "No PMI Data found.";
        }
        return s[0].toString();
    }

    @Override
    public void setInstrumentationLevel(StatLevelSpec[] mlss, Boolean recursive) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setInstrumentationLevel (StatLevelSpec[], Boolean)");

        if (mlss == null || mlss.length == 0) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "StatLevelSpec is null or empty");
            return;
        }

        PmiRegistry.setInstrumentationLevel(mlss, recursive.booleanValue());
        if (tc.isEntryEnabled())
            Tr.exit(tc, "setInstrumentationLevel (StatLevelSpec[], Boolean)");
    }

    //@@@@@@@@@@
    public void appendInstrumentationLevel(StatLevelSpec[] mlss, Boolean recursive) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "appendInstrumentationLevel (StatLevelSpec[], Boolean)");

        if (mlss == null || mlss.length == 0) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "StatLevelSpec is null or empty");
            return;
        }

        PmiRegistry.appendInstrumentationLevel(mlss, recursive.booleanValue());
        if (tc.isEntryEnabled())
            Tr.exit(tc, "appendInstrumentationLevel (StatLevelSpec[], Boolean)");
    }

    //@@@@@@@@@@

    @Override
    public StatLevelSpec[] getInstrumentationLevel(StatDescriptor sd,
                                                   Boolean recursive) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getInstrumentationLevel (StatDescriptor, Boolean)");

        if (sd == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getInstrumentationLevel (StatDescriptor, Boolean)");
            return null;
        }
        StatLevelSpec[] specs = PmiRegistry.getInstrumentationLevel(sd, recursive.booleanValue());

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getInstrumentationLevel (StatDescriptor, Boolean)");

        return specs;
    }

    // get  static XML config info for all the PMI modules in the server
    /** @deprecated as of 6.0 */
    @Deprecated
    public PmiModuleConfig[] getConfigs() {
        //printDeprecationWarning ("getConfigs ()");

        if (tc.isEntryEnabled())
            Tr.entry(tc, "getConfigs");

        PmiModuleConfig[] configs = PmiRegistry.getConfigs();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getConfigs");

        return configs;
    }

    // get  static XML config info for all the PMI modules in the server
    @Override
    public PmiModuleConfig[] getConfigs(java.util.Locale l) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getConfigs(Locale)");

        PmiModuleConfig[] configs = PmiRegistry.getConfigs(l);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getConfigs(Locale)");

        return configs;
    }

    // get  static XML config info for all the PMI modules in the server
    @Override
    public PmiModuleConfig getConfig(String statsType) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getConfig(String)");

        PmiModuleConfig configs = PmiRegistry.getConfig(statsType);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getConfig(String)");

        return configs;
    }

    /** @deprecated as of 6.0 */
    @Deprecated
    public String getInstrumentationLevelString() {
        //printDeprecationWarning ("getInstrumentationLevelString ()");

        if (tc.isEntryEnabled())
            Tr.entry(tc, "getInstrumentationLevelString");

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getInstrumentationLevelString");

        return PmiRegistry.getInstrumentationLevelString();
    }

    // ################################################################
    // Added to by-pass PMI->JMX mapping in 5.0.2
    // ----- methods taking DataDescriptor as input -----
    /** @deprecated as of 6.0 */
    @Deprecated
    public WSStats getStatsObject(DataDescriptor dd, Boolean recursive) {
        //printDeprecationWarning ("getStatsObject (DataDescriptor, Boolean)");

        if (tc.isEntryEnabled())
            Tr.entry(tc, "getStatsObject(DataDescriptor, Boolean)");

        StatsImpl stat = PmiRegistry.getStats(dd, recursive.booleanValue());

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getStatsObject(DataDescriptor, Boolean)");
        return stat;
    }

    /** @deprecated as of 6.0 */
    @Deprecated
    public WSStats[] getStatsArray(DataDescriptor[] dd, Boolean recursive) {
        //printDeprecationWarning ("getStatsArray (DataDescriptor[], Boolean)");

        if (tc.isEntryEnabled())
            Tr.entry(tc, "getStatsArray(DataDescriptor[], Boolean)");

        if (dd == null || dd.length == 0) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getStatsArray(DataDescriptor[], Boolean)");
            return null;
        }

        /*
         * for (int i = 0; i < dd.length; i++)
         * {
         * if (dd[i] != null)
         * Tr.debug (tc, dd[i].toString());
         * else
         * Tr.debug (tc, "DD is NULL");
         * }
         */
        StatsImpl[] stats = PmiRegistry.getStats(dd, recursive.booleanValue());

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getStatsArray(DataDescriptor)" + stats.length);

        return stats;
    }

    /** @deprecated as of 6.0 */
    @Deprecated
    public DataDescriptor[] listStatMembers(DataDescriptor dd) {
        //printDeprecationWarning ("listStatMembers (DataDescriptor)");

        if (tc.isEntryEnabled())
            Tr.entry(tc, "listStatMembers(DataDescriptor)");

        DataDescriptor[] members = PmiRegistry.listMembers(dd);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "listStatMembers(DataDescriptor)");

        return members;
    }

    /** @deprecated as of 6.0 */
    @Deprecated
    public void setInstrumentationLevel(PerfLevelDescriptor pld, Boolean recursive) {
        //printDeprecationWarning ("setInstrumentationLevel (PerfLevelDescriptor, Boolean)");

        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "setInstrumentationLevel(PerfLevelDescriptor, Boolean)");
        }

        PmiRegistry.setInstrumentationLevel(pld, recursive.booleanValue());

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setInstrumentationLevel(PerfLevelDescriptor)");
    }

    /** @deprecated as of 6.0 */
    @Deprecated
    public void setInstrumentationLevel(PerfLevelDescriptor[] pld, Boolean recursive) {
        //printDeprecationWarning ("setInstrumentationLevel (PerfLevelDescriptor[], Boolean)");

        if (tc.isEntryEnabled())
            Tr.entry(tc, "setInstrumentationLevel(PerfLevelDescriptor[], Boolean)");

        PmiRegistry.setInstrumentationLevel(pld, recursive.booleanValue());

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setInstrumentationLevel(PerfLevelDescriptor)");
    }

    /** @deprecated as of 6.0 */
    @Deprecated
    public PerfLevelDescriptor[] getInstrumentationLevel(DataDescriptor dd, Boolean recursive) {
        //printDeprecationWarning ("getInstrumentationLevel (DataDescriptor, Boolean)");

        if (tc.isEntryEnabled())
            Tr.entry(tc, "getInstrumentationLevel(DataDescriptor, Boolean)");

        if (dd == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getInstrumentationLevel(DataDescriptor)");
            return null;
        }

        PerfLevelDescriptor[] specs = PmiRegistry.getInstrumentationLevel(dd, recursive.booleanValue());

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getInstrumentationLevel(DataDescriptor)");

        return specs;
    }

} // PmiCollaborator