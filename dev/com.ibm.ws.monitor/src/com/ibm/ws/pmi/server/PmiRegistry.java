/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*for modules to register and for
* clients to retrieve data and change instrumentation level.
* The data are organized in modules. Within a server, the generic module hierarchy
* is:		module
*				-> collections (0, 1, or more)
*					-> data
*
* It will automatically takes care of aggregate data in the immediate parent and module
* level assuming those module objects implment PmiModuleAggregate interface.
*
* A predefined module hierarchy for all the default PMI instrumentation provided by
* WebSphere is as follows where data can be included in any level.
*
*		    module
*             -> instance
*                  -> submodule
*						-> subinstance
*							-> data
*
* To reduce footprint and PMI admin burden, method subinstances are not created
* unless user set the instrumentation level to be LEVEL_MAX for methods submodule.
*/
package com.ibm.ws.pmi.server;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.ibm.websphere.pmi.PerfModules;
import com.ibm.websphere.pmi.PmiConstants;
import com.ibm.websphere.pmi.PmiModuleConfig;
import com.ibm.websphere.pmi.server.PmiModule;
import com.ibm.websphere.pmi.stat.MBeanStatDescriptor;
import com.ibm.websphere.pmi.stat.StatConstants;
import com.ibm.websphere.pmi.stat.StatDescriptor;
import com.ibm.websphere.pmi.stat.StatLevelSpec;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.monitor.internal.jmx.PmiCollaboratorFactory;
import com.ibm.ws.pmi.stat.StatsConfigHelper;
import com.ibm.ws.pmi.stat.StatsImpl;
import com.ibm.ws.pmi.wire.WpdCollection;

public class PmiRegistry implements PmiConstants {
    public static final String COPYRIGHT = "Copyright (c) 2000, 2004 IBM Corporation and others.\n" +
                                           " All rights reserved. This program and the accompanying materials\n" +
                                           " are made available under the terms of the Eclipse Public License v1.0\n" +
                                           " which accompanies this distribution, and is available at\n" +
                                           " http://www.eclipse.org/legal/epl-v10.html\n" +
                                           " \n" +
                                           " Contributors:\n" +
                                           "     IBM Corporation - initial API and implementation";

    public static final String PLATFORM = initPlatform();
    public static final String MSG_BUNDLE = PmiConstants.MSG_BUNDLE;
    private static boolean disabled = true;
    private static boolean initialized = false;
    private static boolean allLevelNone = true;
    private static boolean beanMethodDisabled = true; // used by PmiBeanFactoryImpl
    public static String nodeName = "mynode";
    public static String serverName = "myserver";
    // the root for module tree
    private static ModuleItem moduleRoot;

    // a hashtable to keep the references for all the module aggregate instance
    private static Hashtable moduleAggregates = new Hashtable();
    // a hash map for mbean ObjectName to Pmi ModuleItem
    private static HashMap mbeanToModuleMap = new HashMap();
    // an array to keep the persistent level setting when a server is restarted
    private static PerfLevelDescriptor[] _plds = null;
    private static String defaultLevel;
    // trace
    private static final TraceComponent tc = Tr.register(PmiRegistry.class);

    // the shared PmiJmxMapper instance for all server side reference

    private static String initPlatform() {
        return PmiConstants.PLATFORM_DISTRIBUTED;
    }

    // Static block for initialization of PmiRegistry
    // Initialize: module configs, module aggregates
    public static synchronized void init() {
        if (initialized)
            return;
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "init");
        }

        initialized = true;
        //defaultLevel = StatConstants.STATISTIC_SET_EXTENDED;
        defaultLevel = StatConstants.STATISTIC_SET_BASIC;
        moduleRoot = new ModuleItem();
        setInstrumentationLevel(defaultLevel);
        //disabled = false;
        try {
            MBeanServer mServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName pmiMBean = new ObjectName(PmiConstants.MBEAN_NAME);
            mServer.registerMBean(PmiCollaboratorFactory.getPmiCollaborator(), pmiMBean);
            printAllMBeans();
        } catch (Exception e) {
            Tr.error(tc, "Unable to create Perf MBean.");
            FFDCFilter.processException(e, "com.ibm.ws.pmi.server.PmiRegistry", "Init");
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "init");
        }
    }

    private static void printAllMBeans() {
        /*
         * MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
         * System.out.println("COUNT = " + mbs.getMBeanCount());
         * //ObjectName name = new ObjectName("name=Perf");
         * Set<ObjectInstance> allPerfMBeans = mbs.queryMBeans(null, null);
         * Iterator<ObjectInstance> itr = allPerfMBeans.iterator();
         * System.out.println("--" + allPerfMBeans.size());
         * while (itr.hasNext()) {
         * System.out.println("--" + itr.next().toString());
         * }
         */
    }

    public static boolean isDisabled() {
        return disabled;
    }

    public static void disable() {
        disabled = true;
    }

    public static void enable() {
        disabled = false;
    }

    public static boolean isAllLevelNone() {
        return allLevelNone;
    }

    public static boolean isBeanMethodDisabled() {
        if (moduleRoot != null) {
            beanMethodDisabled = checkBeanMethodDisabled(moduleRoot);
            return beanMethodDisabled;
        } else {
            return beanMethodDisabled;
        }
    }

    public static void setAllLevelNone(boolean allNone) {
        allLevelNone = allNone;
    }

    /**
     * Register a module - synchronized l
     * Note: The top level is always TYPE_MODULE. After that, the generic hierarchy
     * is a number (0-n) of TYPE_COLLECTION module objects.
     *
     * A pre-defined module organization for the default PMI instrumentation in
     * WebSphere is as follows (instance, submodule, subinstance are collections).
     * module
     * -> category (e.g., entity, stateful, stateless ...)
     * -> instance
     * -> submodule
     * -> subinstance
     *
     * TYPE_INSTANCE, TYPE_SUBMODULE, TYPE_SUBINSTANCE are used for module tree hierarchy
     * that are only visible within the module object itself and this class. From the
     * client's point of view, they are all collecitons.
     *
     */
    public synchronized static ModuleItem registerModule(PmiModule instance) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "registerModule");
        }
        if (!initialized)
            init();
        if (!validateModule(instance)) {
            Tr.exit(tc, "registerModule - module is null");
            return null;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "**** " + "registerModule: " + instance.getModuleID() + ", " + instance.getName());
        }

        String[] path = instance.getPath();

        //int parentLevel = LEVEL_UNDEFINED;
        ModuleItem parent = null;
        if (path == null || path.length == 0) {
            Tr.warning(tc, "PMI0001W", instance.getName());
            return null;
        } else if (path.length == 1) {
            parent = moduleRoot;
            if (tc.isDebugEnabled()) {
                printPMITree(parent);
            }
        } else {
            String[] parentPath = getParentPath(path);
            parent = moduleRoot.add(parentPath, 0);
        }
        //SET THE LEVEL TO BASIC
        instance.setInstrumentationBySet(defaultLevel);

        ModuleItem retItem = new ModuleItem(instance);
        if (!parent.add(retItem)) {
            Tr.warning(tc, "PMI0023W", instance.getName());

            if (tc.isDebugEnabled()) {
                // unable to add due to duplicate name.
                // print all children.
                String p = "root";
                if (parent.getInstance() != null)
                    p = parent.getInstance().getName();

                StringBuffer b = new StringBuffer("Current entries under: " + p);
                ModuleItem c[] = parent.children();
                if (c != null) {
                    for (int k = 0; k < c.length; k++) {
                        PmiModule pm = c[k].getInstance();
                        if (pm != null)
                            b.append("\r\n").append(pm.getName());
                    }
                }
                Tr.debug(tc, b.toString());
            }

            retItem = null;
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "registerModule");
        return retItem;
    }

    private static ModuleItem _getAggregateParent(ModuleItem mi) {
        ModuleItem p = mi;
        do {
            if (p.getInstance() == null)
                return null;

            if (p.getInstance() instanceof ModuleAggregate)
                return p;

            p = p.getParent();
        } while (p != null);

        return null;
    }

    /**
     * remove module - never remove TYPE_MODULE.
     */
    public static synchronized void unregisterModule(PmiModule instance) {
        if (disabled)
            return;
        if (instance == null) // || instance.getPath().length<=1) return;
            return;

        // check if the path has null in it. if a module is unregistered twice
        // the path will have null
        String[] path = instance.getPath();
        if (path == null || path.length == 0)
            return;
        for (int k = 0; k < path.length; k++) {
            if (path[k] == null)
                return;
        }

        if (tc.isEntryEnabled())
            Tr.entry(tc, "unregisterModule: " + instance.getModuleID() + ", " + instance.getName());
        // unregister itself

        String[] parentPath = new String[path.length - 1];
        System.arraycopy(path, 0, parentPath, 0, parentPath.length);
        // locate parent
        ModuleItem parent = moduleRoot.find(parentPath, 0);
        if (parent != null) {
            // remove "instance" from parent
            parent.remove(parent.find(path[path.length - 1]));
            // do not remove the empty parent group in custom pmi
            // in custom PMI groups/instances are explicitly created and
            // should be remove explicitly

            // in pre-custom groups are create IMPLICITYLY when needed
            if (instance.isCustomModule())
                return;

            // check if parent is empty
            if (parent.children == null || parent.children.size() == 0) {
                if (parent.getInstance() != null) {
                    String[] mypath = parent.getInstance().getPath();
                    // TODO: ask Wenjian about this?
                    // exclude WEBAPP_MODULE because it is created explicitly and
                    // should be removed explictly by calling PmiFactory.removePmiModule
                    if (!(mypath.length == 2 && (mypath[0].equals(WEBSERVICES_MODULE)))) {
                        // recursive call?: unregisterModule (parent.getInstance());
                        parent.getInstance().unregister();
                    }
                }
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "unregisterModule");
    }

    private static boolean validateModule(PmiModule instance) {
        if (instance == null)
            return false;
        return true;
        // TODO: will need more validation here if we allow 3rd party to add its own modules
        // more code to be added.
    }

    private static String[] getParentPath(String[] path) {
        if (path == null || path.length <= 1)
            return null;
        String[] parentPath = new String[path.length - 1];
        System.arraycopy(path, 0, parentPath, 0, parentPath.length);
        return parentPath;
    }

    /**
     * For user to add customized module:
     * create module config, add to hash table
     */
    public synchronized static boolean addModuleInfo(String moduleID, String modulePrefix) {
        if (PerfModules.addModuleInfo(modulePrefix + moduleID) != null)
            return true;
        else
            return false;
    }

    // return ModuleAggregate for a given moduleID.
    // create it if not there - synchronized
    protected static ModuleAggregate getModuleAggregate(String moduleID) {
        ModuleAggregate aggregate = (ModuleAggregate) moduleAggregates.get(moduleID);
        if (aggregate != null)
            return aggregate;
        // need to create it - synchronized
        synchronized (moduleAggregates) {
            aggregate = (ModuleAggregate) moduleAggregates.get(moduleID);
            if (aggregate != null)
                return aggregate;
            PmiModuleConfig config = getConfig(moduleID);
            if (config == null)
                return null;
            aggregate = new ModuleAggregate(moduleID);
            moduleAggregates.put(moduleID, aggregate);
            return aggregate;
        }
    }

    // Remove me
    // return PmiModuleConfig for a given moduleID
    public static PmiModuleConfig getConfig(String moduleID) {
        if (disabled)
            return null;

        return StatsConfigHelper.getTranslatedStatsConfig(moduleID);
    }

    // return all the ModuleConfigs
    public static PmiModuleConfig[] getConfigs() {
        return getConfigs(null);
    }

    // return all the ModuleConfigs
    public static PmiModuleConfig[] getConfigs(java.util.Locale l) {
        if (disabled)
            return null;
        PmiModuleConfig[] allConfigs = PerfModules.getConfigs();

        for (int i = 0; i < allConfigs.length; i++) {
            // For CustomPMI modules mBeanType will be set in wrapper classes (ie. PmiModuleInstance)
            //            if (allConfigs[i].getMbeanType() == null) {
            //                String mbeanType = jmxMapper.getMBeanType(allConfigs[i].getShortName());
            //                allConfigs[i].setMbeanType(mbeanType);
            //            }
            StatsConfigHelper.translateAndCache(allConfigs[i], l);
        }

        //return allConfigs;
        return StatsConfigHelper.getAllConfigs(null);
    }

    public static PmiModule findPmiModule(DataDescriptor dd) {
        if (disabled)
            return null;

        ModuleItem item = findModuleItem(dd);

        if (item == null)
            return null;
        else
            return item.getInstance();
    }

    // find a ModuleItem
    public static ModuleItem findModuleItem(String[] path) {
        if (disabled)
            return null;
        if (path == null || path[0].equals(APPSERVER_MODULE)) {
            return moduleRoot;
        }
        return moduleRoot.find(path, 0);
    }

    public static ModuleItem findModuleItem(DataDescriptor dd) {
        if (disabled)
            return null;
        if (dd == null || dd.getModuleName().equals(APPSERVER_MODULE)) {
            return moduleRoot;
        }
        return moduleRoot.find(dd.getPath(), 0);
    }

    private static ModuleItem findParentModuleItem(DataDescriptor dd) {
        String[] path = dd.getPath();
        String[] parentPath = new String[path.length - 1];
        System.arraycopy(path, 0, parentPath, 0, parentPath.length);
        return findModuleItem(new DataDescriptor(parentPath));
    }

    // 179079: check if the node and server names are for this server
    private static boolean isSameNodeAndServer(ObjectName mName) {
        String processName = mName.getKeyProperty("process");
        String nodeName = mName.getKeyProperty("node");

        if (tc.isDebugEnabled())
            Tr.debug(tc, "isSameNodeAndServer: " + mName.toString());

        if (processName != null && !processName.equals(PmiRegistry.serverName)) {
            // print only when debug is enabled since customers get worried seeing this message
            if (tc.isDebugEnabled())
                Tr.warning(tc, "PMI0002W", mName.toString() + ", expected process name is " + PmiRegistry.serverName);
            return false;
        }
        if (nodeName != null && !nodeName.equals(PmiRegistry.nodeName)) {
            // print only when debug is enabled since customers get worried seeing this message
            if (tc.isDebugEnabled())
                Tr.warning(tc, "PMI0002W", mName.toString() + ", expected node name is " + PmiRegistry.nodeName);
            return false;
        }

        return true;
    }

    // Old format taking DataDescriptor
    public static DataDescriptor[] listMembers() {
        if (disabled)
            return null;
        ModuleItem[] modItems = moduleRoot.children();
        if (modItems == null)
            return null;
        DataDescriptor[] res = new DataDescriptor[modItems.length];
        for (int i = 0; i < res.length; i++) {
            // root level
            res[i] = new DataDescriptor(new String[] { modItems[i].getInstance().getName() }); // replaced getModuleID: customPMI
        }
        return res;
    }

    // Old format taking DataDescriptor
    public static DataDescriptor[] listMembers(DataDescriptor dd) {
        if (disabled)
            return null;
        if (dd == null || dd.getPath() == null) {
            // server
            return listMembers();
        } else if (dd.getType() == TYPE_INVALID) {
            return null;
        } else {
            // find the module
            ModuleItem module = findModuleItem(dd);
            if (module == null)
                return null;
            else
                return module.listMembers(dd, false); //false=>pmi based
        }
    }

    // JMX based: 6.0
    // returns the immediate members if recursive = false
    // returns all children if recursive = true
    public static StatDescriptor[] listStatMembers(StatDescriptor sd, boolean recursive) {
        if (disabled)
            return null;

        ModuleItem module = null;
        if (sd == null)
            module = moduleRoot; // root
        else
            module = findModuleItem(sd.getPath());
        if (module == null)
            return null;
        else {
            ArrayList list = module.listChildStatDescriptors(recursive);
            int n = list.size();
            StatDescriptor[] ret = new StatDescriptor[n];
            for (int k = 0; k < n; k++)
                ret[k] = (StatDescriptor) list.get(k);

            return ret;
        }
    }

    // Old format taking DataDescriptor
    public static WpdCollection get(DataDescriptor dd, boolean recursive) {
        if (disabled)
            return null;
        return getStats(findModuleItem(dd), recursive);
    }

    // Remove me:
    public static WpdCollection[] gets(DataDescriptor[] dds, boolean recursive) {
        if (disabled)
            return null;
        WpdCollection[] ret = new StatsImpl[dds.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = getStats(findModuleItem(dds[i]), recursive);
        }
        return ret;
    }

    /*
     * // Return a StatsImpl for server data
     * // Take a boolean parameter for two modes: recursive and non-recursive
     * private static StatsImpl getServer(boolean recursive)
     * {
     * // Note: there is no data under directly under server module tree root,
     * // so return null if not recursive
     * if(!recursive) return null;
     * ModuleItem[] modItems = moduleRoot.children();
     * if(modItems == null)
     * {
     * return new StatsImpl("server", TYPE_SERVER, moduleRoot.level, null, null);
     * }
     * ArrayList modMembers = new ArrayList(modItems.length);
     * for(int i=0; i<modItems.length; i++)
     * {
     * modMembers.add(modItems[i].getStats(recursive));
     * }
     * StatsImpl sCol = new StatsImpl("server", TYPE_SERVER, moduleRoot.level, null, modMembers);
     * return sCol;
     * }
     */
    private static StatsImpl getStats(ModuleItem moduleItem, boolean recursive) {
        // Note: cannot retrieve single data for JMX interface
        //int[] dataIds = msd.getDataIds();
        if (moduleItem == null) { // not found
            return null;
        }

        return moduleItem.getStats(recursive);

        /*
         * else if(moduleItem.getInstance() == null)
         * { // root module item
         * return getServer(recursive);
         * }
         * else
         * {
         * return moduleItem.getStats(recursive);
         * }
         */
    }

    // --- DD based: Custom PMI ---
    public static StatsImpl getStats(DataDescriptor dd, boolean recursive) {
        if (disabled)
            return null;
        return getStats(findModuleItem(dd), recursive);
    }

    public static StatsImpl[] getStats(DataDescriptor[] dd, boolean recursive) {
        if (disabled)
            return null;

        StatsImpl[] stats = new StatsImpl[dd.length];
        for (int i = 0; i < dd.length; i++) {
            stats[i] = getStats(dd[i], recursive);
        }
        return stats;
    }

    public static StatsImpl[] getStats(StatDescriptor[] dd, boolean recursive) {
        if (disabled)
            return null;

        StatsImpl[] stats = new StatsImpl[dd.length];
        for (int i = 0; i < dd.length; i++) {
            stats[i] = getStats(findModuleItem(dd[i].getPath()), recursive);
        }
        return stats;
    }

    //getStats Added for NLS support (Liberty)
    public static StatsImpl[] getStats(StatDescriptor[] dd, boolean recursive, PmiModuleConfig config) {
        //StatsImpl[] stats = null;
        PmiModuleConfig[] pmconfigs = StatsConfigHelper.getAllConfigs(null);
        ModuleItem mi = findModuleItemList(null);
        StatsImpl sil = mi.getStats(true);
        for (int i = 0; i < sil.getSubStats().length; i++) {
            for (int j = 0; j < pmconfigs.length; j++) {
                if (sil.getSubStats()[i].getStatsType().contains(pmconfigs[j].getUID())) {
                    sil.getSubStats()[i].mSetConfig(pmconfigs[j]);
                    if (sil.getSubStats()[i].getSubStats().length > 0) {
                        for (int k = 0; k < pmconfigs.length; k++) {
                            for (int l = 0; l < sil.getSubStats()[i].getSubStats().length; l++) {
                                if (sil.getSubStats()[i].getSubStats()[l].getStatsType().equalsIgnoreCase(pmconfigs[k].getUID())) {
                                    sil.getSubStats()[i].getSubStats()[l].setConfig(pmconfigs[k]);
                                    if (sil.getSubStats()[i].getSubStats()[l].getSubStats().length > 0) {
                                        for (int m = 0; m < sil.getSubStats()[i].getSubStats()[l].getSubStats().length; m++) {
                                            for (int n = 0; n < pmconfigs.length; n++) {
                                                if (sil.getSubStats()[i].getSubStats()[l].getSubStats()[m].getStatsType().equalsIgnoreCase(pmconfigs[n].getUID())) {
                                                    sil.getSubStats()[i].getSubStats()[l].getSubStats()[m].setConfig(pmconfigs[n]);
                                                    if (sil.getSubStats()[i].getSubStats()[l].getSubStats()[m].getSubStats().length > 0) {
                                                        for (int p = 0; p < sil.getSubStats()[i].getSubStats()[l].getSubStats()[m].getSubStats().length; p++) {
                                                            sil.getSubStats()[i].getSubStats()[l].getSubStats()[m].getSubStats()[p].setConfig(pmconfigs[n]);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        StatsImpl[] stats = new StatsImpl[dd.length];
        for (int i = 0; i < dd.length; i++) {
            stats[i] = getStats(findModuleItem(dd[i].getPath()), recursive);
        }

        return stats;
    }

    public static ModuleItem findModuleItemList(String[] path) {
        if (disabled)
            return null;
        if (path == null || path[0].equals(APPSERVER_MODULE)) {
            return moduleRoot;
        }
        return moduleRoot;
    }

    // 6.0 API
    public static StatLevelSpec[] getInstrumentationLevel(StatDescriptor sd, boolean recursive) {
        if (disabled)
            return null;

        ModuleItem item = findModuleItem(sd.getPath());
        if (item == null) { // wrong moduleName
            return null;
        } else {
            if (!recursive) {
                StatLevelSpec[] pld = new StatLevelSpec[1];
                PmiModule instance = item.getInstance();
                if (instance != null) {
                    pld[0] = new StatLevelSpec(sd.getPath(), instance.getEnabled());
                    return pld;
                } else {
                    return null;
                }
            } else {
                ArrayList res = item.getStatLevelSpec(recursive);
                StatLevelSpec[] pld = new StatLevelSpec[res.size()];
                for (int i = 0; i < pld.length; i++)
                    pld[i] = (StatLevelSpec) res.get(i);

                return pld;
            }
        }
    }

    // @@@@@@@@@@
    public static void appendInstrumentationLevel(StatLevelSpec[] plds,
                                                  boolean recursive) {
        if (disabled)
            return;
        for (int i = 0; i < plds.length; i++) {
            if (plds[i] == null)
                continue;

            ModuleItem item = findModuleItem(plds[i].getPath());
            if (item != null) {
                if (item.getInstance() == null) // if root only ALL_STATISTICS
                // is valid "enable" list
                {
                    int[] e = plds[i].getEnabled();
                    if (e != null && e.length > 0
                        && e[0] == StatLevelSpec.ALL_STATISTICS)
                        item.setInstanceLevel(e, new int[0],
                                              PmiConstants.LEVEL_FINEGRAIN, recursive);
                } else {
                    // Get the existing statistics using
                    // item.getInstance().getEnabled()
                    // Get new statistics using plds[i].getEnabled()
                    // Form a union of both and pass it to setInstanceLevel()
                    int[] oldCounters = item.getInstance().getEnabled();
                    int[] newCounters = plds[i].getEnabled();

                    // We need to consider special cases.
                    // CASE1: If already all statistics are enabled, then do nothing.
                    // CASE2: If No New Counter is specified, then don't do anything.
                    if ((oldCounters.length != 0 && oldCounters[0] == StatLevelSpec.ALL_STATISTICS)
                        || newCounters.length == 0) {
                        continue;
                    }

                    // CASE3: If new counters is ALL, then, set level to new Counters (ALL)
                    // CASE4: If no old counter then, set level to new Counters.
                    if (newCounters[0] == StatLevelSpec.ALL_STATISTICS
                        || oldCounters.length == 0) {
                        item.setInstanceLevel(newCounters, new int[0],
                                              PmiConstants.LEVEL_FINEGRAIN, recursive);
                    } else {
                        // CASE5: If there are few counters enabled, and there are new counters to be enabled,
                        // then we need to get union of them (basically merge both counters)
                        item.setInstanceLevel(
                                              getUnionForAppendInstrumentationLevel(
                                                                                    oldCounters, newCounters),
                                              new int[0],
                                              PmiConstants.LEVEL_FINEGRAIN, recursive);
                    }
                }
            }
        }
        // allLevelNone = checkAllLevelNone(moduleRoot);
        beanMethodDisabled = checkBeanMethodDisabled(moduleRoot);
    }

    // @slagiset : Modified the getUnionForAppendInstrumentationLevel() methods
    // implementation
    private static int[] getUnionForAppendInstrumentationLevel(
                                                               int[] oldCounters, int[] newCounters) {

        int[] merged = new int[newCounters.length + oldCounters.length];
        int i, j, k = -1;

        // Copying the Old Counters to the merged array
        for (i = 0; i < oldCounters.length; i++) {
            merged[i] = oldCounters[i];
            k++;
        }

        // Checking for the duplicate elements
        boolean flag = false;
        for (i = 0; i < newCounters.length; i++, flag = false) {
            for (j = 0; (j < oldCounters.length) && (flag == false); j++) {
                if (newCounters[i] == oldCounters[j]) {
                    flag = true; // If the array element is a duplicate then
                    // breaking the loop.
                }
            }
            if (flag == false) {
                k++;
                merged[k] = newCounters[i]; // If the array element is not
                // present, then adding it to the
                // Merged array.
            }
        }

        // Forming an array without duplicates.
        int[] final_result = new int[k + 1];
        for (i = 0; i <= k; i++)
            final_result[i] = merged[i];
        return final_result;

    }

    // @@@@@@@@@@

    // custom pmi : dd based API
    public static PerfLevelDescriptor[] getInstrumentationLevel(DataDescriptor dd, boolean recursive) {
        if (disabled)
            return null;

        ModuleItem item = findModuleItem(dd);
        if (item == null) { // wrong moduleName
            return null;
        } else {
            if (!recursive) {
                PerfLevelDescriptor[] pld = new PerfLevelDescriptor[1];
                String[] path = item.getInstance().getPath();
                PmiModule instance = item.getInstance();
                pld[0] = new PerfLevelDescriptor(instance.getPath(), instance.getInstrumentationLevel(), instance.getModuleID());

                return pld;
            } else {
                ArrayList res = item.getPerfLevelDescriptors(false);
                PerfLevelDescriptor[] pld = new PerfLevelDescriptor[res.size()];
                for (int i = 0; i < pld.length; i++) {
                    pld[i] = (PerfLevelDescriptor) res.get(i);
                }
                return pld;
            }
        }
    }

    // Remove me
    // get instrumenation level based on the path during runtime
    public static int getInstrumentationLevel(String[] path) {
        if (disabled)
            return LEVEL_UNDEFINED;
        DataDescriptor dd = new DataDescriptor(path);
        ModuleItem item = findModuleItem(dd);
        if (item == null) { // wrong moduleName
            return LEVEL_UNDEFINED;
        } else {
            return item.getInstance().getInstrumentationLevel();
        }
    }

    // Remove me
    // return the PerfLevelDescriptor for each module/instance/submodule/subinstance during runtime.
    public static PerfLevelDescriptor[] getAllInstrumentationLevels() {
        if (disabled)
            return null;
        ArrayList res = moduleRoot.getPerfLevelDescriptors(false);
        // create returned array
        PerfLevelDescriptor[] retArray = new PerfLevelDescriptor[res.size()];
        for (int i = 0; i < retArray.length; i++)
            retArray[i] = (PerfLevelDescriptor) res.get(i);
        return retArray;
    }

    // return the top level modules's PerfLevelDescriptor in String
    public static String getInstrumentationLevelString() {
        if (disabled)
            return null;
        Map modules = moduleRoot.children;
        if (modules == null) {
            return "";
        } else {
            PerfLevelDescriptor[] plds = new PerfLevelDescriptor[modules.size()];
            Iterator values = modules.values().iterator();
            int i = 0;
            while (values.hasNext()) {
                PmiModule instance = ((ModuleItem) values.next()).getInstance();
                plds[i++] = new PerfLevelDescriptor(instance.getPath(), instance.getInstrumentationLevel(), instance.getModuleID());
            }
            return PmiUtil.getStringFromPerfLevelSpecs(plds);
        }
    }

    public static String getInstrumentationLevelString60() {
        StatLevelSpec[] specs = getInstrumentationLevel(new StatDescriptor(null), true);
        if (specs == null || specs.length == 0)
            return "";

        StringBuffer buf = new StringBuffer(specs[0].toString());
        for (int k = 1; k < specs.length; k++)
            buf.append(":").append(specs[k].toString());

        return buf.toString();
    }

    // Remove me
    // return the PerfLevelDescriptor for modules and instances during runtime.
    // If a child has same level as its parent, its pld will not be returned.
    // This will save number of PerfLevelDescriptors.
    /*
     * private static PerfLevelDescriptor[] getTreeInstrumentationLevels()
     * {
     * ArrayList res = moduleRoot.getTreePerfLevelDescriptors(LEVEL_UNDEFINED);
     * // create returned array
     * PerfLevelDescriptor[] retArray = new PerfLevelDescriptor[res.size()];
     * for(int i=0; i<retArray.length; i++)
     * retArray[i] = (PerfLevelDescriptor)res.get(i);
     * return retArray;
     * }
     */
    // 6.0 API. Supports ONLY fine-grained.
    public static void setInstrumentationLevel(StatLevelSpec[] plds, boolean recursive) {
        if (disabled)
            return;
        for (int i = 0; i < plds.length; i++) {
            if (plds[i] == null)
                continue;

            ModuleItem item = findModuleItem(plds[i].getPath());
            if (item != null) {
                if (item.getInstance() == null) // if root only ALL_STATISTICS is valid "enable" list
                {
                    int[] e = plds[i].getEnabled();
                    if (e != null && e.length > 0 && e[0] == StatLevelSpec.ALL_STATISTICS)
                        item.setInstanceLevel(e, new int[0], PmiConstants.LEVEL_FINEGRAIN, recursive);
                } else
                    item.setInstanceLevel(plds[i].getEnabled(), new int[0], PmiConstants.LEVEL_FINEGRAIN, recursive);
            }
        }
        //allLevelNone = checkAllLevelNone(moduleRoot);
        beanMethodDisabled = checkBeanMethodDisabled(moduleRoot);
    }

    // Remove me: old API
    public static void setInstrumentationLevel(PerfLevelDescriptor pld, boolean recursive) {
        if (disabled)
            return;
        setLevel(pld, recursive);
        allLevelNone = checkAllLevelNone(moduleRoot);
        beanMethodDisabled = checkBeanMethodDisabled(moduleRoot);
    }

    // -- Custom PMI. dd based API ---
    public static void setInstrumentationLevel(PerfLevelDescriptor[] plds, boolean recursive) {
        if (disabled)
            return;
        for (int i = 0; i < plds.length; i++) {
            setLevel(plds[i], recursive);
        }
        allLevelNone = checkAllLevelNone(moduleRoot);
        beanMethodDisabled = checkBeanMethodDisabled(moduleRoot);
    }

    // -- Custom PMI. dd based API ---
    // Call each module/instance to set the new level - recursive or not recursive
    // 4.0 API: Supports ONLY 5.0 level
    private static void setLevel(PerfLevelDescriptor pld, boolean recursive) {
        if (pld == null)
            return;
        String[] path = pld.getPath();
        // find the module item first
        ModuleItem item = null;
        if (path == null) {
            Tr.warning(tc, "PMI0001W", "PmiRegistry.setLevel");
            return;
        } else if (path.length == 1 && path[0].equals("pmi")) {
            item = moduleRoot;
        } else if (path[0].equals("pmi")) {
            String[] thisPath = new String[path.length - 1];
            System.arraycopy(path, 1, thisPath, 0, thisPath.length);
            item = findModuleItem(new DataDescriptor(thisPath));
        }
        if (item != null) {
            item.setInstanceLevel(null, null, pld.getLevel(), recursive);
        }
    }

    // return true if all levels are none; return false otherwise
    private static boolean checkAllLevelNone(ModuleItem item) {
        // first, check the item itself
        if (item.instance != null && item.instance.getInstrumentationLevel() != LEVEL_NONE)
            return false;
        // second, check all children
        if (item.children == null)
            return true;
        ModuleItem child = null;
        Iterator values = item.children.values().iterator();
        while (values.hasNext()) {
            child = (ModuleItem) values.next();
            if (checkAllLevelNone(child) == false)
                return false;
        }
        return true;
    }

    private final static String beanMethodWccmType = BEAN_MODULE + "#" + BEAN_METHODS_SUBMODULE;

    // return true if all levels are none; return false otherwise
    private static boolean checkBeanMethodDisabled(ModuleItem item) {
        // first, check the item itself
        if (item.instance != null && !item.instance.getModuleID().equals(BEAN_MODULE))
            return true;
        // moduleId is beanModule
        if (item.instance != null) {
            int inLevel = item.instance.getInstrumentationLevel();
            if (inLevel >= LEVEL_HIGH)
                return false;
            else if (inLevel == LEVEL_FINEGRAIN) {
                // 6.0
                if (item.instance.getModuleID().equals(BEAN_MODULE)) {
                    int[] en = item.instance.getEnabled();
                    if (en != null && en.length > 0)
                        return false;
                }
            }
        }

        // second, check all children
        if (item.children == null)
            return true;
        ModuleItem child = null;
        Iterator values = item.children.values().iterator();
        while (values.hasNext()) {
            child = (ModuleItem) values.next();
            if (!child.getInstance().getModuleID().equals(BEAN_MODULE))
                continue;
            if (checkBeanMethodDisabled(child) == false)
                return false;
        }
        return true;
    }

    /**
     * ori's version of this method uses an array list rather than an array;
     * otherwise identical to old method.
     */
    /**
     * Enable data
     */
    public static void enableData(DataDescriptor[] dds, boolean recursive) {
        if (disabled)
            return;
        turnOnOffData(dds, true, recursive);
    }

    /**
     * Disable data
     */
    public static void disableData(DataDescriptor[] dds, boolean recursive) {
        if (disabled)
            return;
        turnOnOffData(dds, false, recursive);
    }

    private static void turnOnOffData(DataDescriptor[] dds, boolean on, boolean recursive) {
        for (int i = 0; i < dds.length; i++) {
            // find module item first
            ModuleItem item = findModuleItem(dds[i]);
            if (item != null) {
                int[] dataIds = dds[i].getDataIds();
                if (dataIds == null)
                    item.turnOnOffData(ALL_DATA, on, recursive);
                else {
                    for (int j = 0; j < dataIds.length; j++)
                        item.turnOnOffData(dataIds[j], on, recursive);
                }
            }
        }
    }

    // Before a server is running, we have no idea about what modules
    // will be in the server. So we use all the available modules here.
    public static PerfLevelDescriptor[] getDefaultPerfLevelSpecs() {
        String[] existingModules = PerfModules.moduleIDs;
        PerfLevelDescriptor[] ret = new PerfLevelDescriptor[existingModules.length + 1];
        ret[0] = new PerfLevelDescriptor(null, LEVEL_NONE);
        for (int i = 0; i < existingModules.length; i++) {
            ret[i + 1] = new PerfLevelDescriptor(new String[] { existingModules[i] }, LEVEL_NONE);
        }
        return ret;
    }

    // MBean -> MI is one-to-one mapping.
    // If same MBean is used for more than one MI then the previous mapping will be overwritten
    public static boolean setMBeanToModuleMap(ModuleItem moduleItem, MBeanStatDescriptor msd) {
        if (moduleItem == null || msd == null) {
            return false;
        }

        synchronized (mbeanToModuleMap) {
            ModuleItem prevVal = (ModuleItem) mbeanToModuleMap.put(msd.getIdentifier(), moduleItem);
            if (moduleItem.getInstance() != null) {
                // d180336: set the mbean name in PmiAbstractModule
                moduleItem.getInstance().setMBeanName(msd.getObjectName(), msd.getStatDescriptor());
            }
            if (prevVal != null) {
                // previous value is overwritten
                if (tc.isDebugEnabled())
                    Tr.warning(tc, "PMI0024W", prevVal.getInstance().getName());
            }
            if (tc.isDebugEnabled())
                Tr.debug(tc, "setMBeanToModuleMap(): " + msd.getIdentifier());
        }
        return true;
    }

    // called when unregistering a module
    public static void removeMBeanToModuleMap(MBeanStatDescriptor msd) {
        if (msd != null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "removeMBeanToModuleMap(): " + msd.getIdentifier());
            mbeanToModuleMap.remove(msd.getIdentifier());
        }
    }

    // find if the given name (module) exist at the root level
    public static boolean isDuplicateModule(String name) {
        if (moduleRoot.find(name) != null) {
            return true;
        } else {
            return false;
        }
    }

    // used by zOS in PerfPrivate.java
    public static com.ibm.websphere.pmi.stat.WSStats getServerSnapshot() {
        return moduleRoot.getStats(true); //getServer(true);
    }

    // =========================== DEBUG ONLY METHODS ===========================
    public static void printpath(String[] path) {
        StringBuffer b = new StringBuffer("printpath: ");

        for (int i = 0; i < path.length; i++)
            b.append(path[i]).append("/");
        Tr.debug(tc, b.toString());
    }

    // debug
    private static String pathToString(int indent, String[] path) {
        StringBuffer b = new StringBuffer("|");
        while (indent != 0) {
            b.append("_");
            --indent;
        }
        b.append("_ ");

        for (int i = 0; i < path.length; i++)
            b.append("/").append(path[i]);
        return b.toString();
    }

    public static void printPMITree(ModuleItem root) {
        if (moduleRoot == null) {
            Tr.debug(tc, "Specified PMI ModuleItem is empty!");
            return;
        }
        Tr.debug(tc, ">> Begin PMI tree");
        StringBuffer b = new StringBuffer();
        _printTree(0, root, b);
        System.err.println(b.toString());
        Tr.debug(tc, "<< End PMI tree");
    }

    private static void _printTree(int level, ModuleItem root, StringBuffer buf) {
        ModuleItem[] child = root.children();
        if (child == null)
            return;

        for (int i = 0; i < child.length; i++) {
            buf.append("\r\n").append(pathToString(level, child[i].getInstance().getPath()));
            _printTree(level + 1, child[i], buf);
        }
    }

    public static void setSynchronizedUpdate(boolean flag) {
        if (moduleRoot != null) {
            _setSyncEnabled(moduleRoot, flag);
        }
    }

    // Method to enable/disable synchronized update for all the enabled statistics
    private static void _setSyncEnabled(ModuleItem m, boolean flag) {
        if (m != null) {
            // set instance in m
            PmiModule pm = m.getInstance();
            if (pm != null)
                pm.setSyncEnabled(flag);

            // set children in m
            ModuleItem[] mi = m.children();
            if (mi != null) {
                for (int i = 0; i < mi.length; i++)
                    _setSyncEnabled(mi[i], flag);
            }
        }
    }

    // server level pre-defined group
    public static void setInstrumentationLevel(String statisticSet) {
        if (statisticSet != null) {
            moduleRoot.setInstanceLevel_Set(statisticSet, true);
        }
    }

}
