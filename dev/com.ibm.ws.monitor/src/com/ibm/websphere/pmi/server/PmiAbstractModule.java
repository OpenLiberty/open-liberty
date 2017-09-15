/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.pmi.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.management.ObjectName;

import com.ibm.websphere.pmi.PerfModules;
import com.ibm.websphere.pmi.PmiConstants;
import com.ibm.websphere.pmi.PmiDataInfo;
import com.ibm.websphere.pmi.PmiModuleConfig;
import com.ibm.websphere.pmi.stat.MBeanStatDescriptor;
import com.ibm.websphere.pmi.stat.StatDescriptor;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.pmi.factory.StatsFactoryUtil;
import com.ibm.ws.pmi.server.ModuleItem;
import com.ibm.ws.pmi.server.PmiCallback;
import com.ibm.ws.pmi.server.PmiRegistry;
import com.ibm.ws.pmi.server.data.SpdDoubleAggregate;
import com.ibm.ws.pmi.server.data.SpdDoubleImpl;
import com.ibm.ws.pmi.server.data.SpdLoadAggregate;
import com.ibm.ws.pmi.server.data.SpdLoadImpl;
import com.ibm.ws.pmi.server.data.SpdLongAggregate;
import com.ibm.ws.pmi.server.data.SpdLongImpl;
import com.ibm.ws.pmi.server.data.SpdStatAggregate;
import com.ibm.ws.pmi.server.data.SpdStatImpl;
import com.ibm.ws.pmi.server.data.SpdStatisticAggregate;
import com.ibm.ws.pmi.server.data.SpdStatisticExternal;
import com.ibm.ws.pmi.stat.RangeStatisticImpl;
import com.ibm.ws.pmi.stat.StatisticImpl;
import com.ibm.ws.pmi.stat.StatsImpl;
import com.ibm.wsspi.pmi.factory.StatisticActions;

public abstract class PmiAbstractModule implements PmiModule, PmiConstants {
    protected String name = null; // the name of this module/submodule
    protected String instanceName = null; // passed in by extended class
    protected String submoduleName = null; // set by extended class if used
    protected String subinstanceName = null; // set by extended lass if used
    protected String categoryName = null; // passed in by extended class
    protected ObjectName mbeanName = null;
    protected StatDescriptor msd_sd = null;
    protected StatDescriptor sd60 = null;

    public PmiModuleConfig moduleConfig = null;
    protected int type = TYPE_INSTANCE; // override by extended class if needed
    protected int currentLevel = LEVEL_UNDEFINED;
    protected boolean aggregateModule = false; // override by extended class if needed
    protected PmiCallback callback = null;

    protected int[] enabled = new int[0];
    protected int[] enabledSync = new int[0];

    protected StatisticActions statisticActionLsnr = null;
    protected boolean bStandaloneTree = false;
    protected HashMap nameDataTable = null;

    // a hash table to map id and enabled SpdData for a fast lookup
    protected Map dataTable = new TreeMap(); //HashMap();

    // we don't allow this to update anywhere else
    private final ArrayList dataList = new ArrayList(3);
    protected boolean bAllCountersDisabled = true;

    private static final TraceComponent tc = Tr.register(PmiAbstractModule.class);

    public static int SET = 1;
    public static int INCREMENT = 2;
    public static int DECREMENT = 3;

    /**
     * Constructor:
     * Set instance name
     * Get moduleConfig
     * Create hashtable
     * 
     * Note: extended class should set the following members when necessary
     * moduleID
     * submoduleName
     * subinstanceName
     * type
     * aggregateModule
     */

    public PmiAbstractModule(String moduleID, String instanceName) {
        this(null, moduleID, instanceName, null);
    }

    public PmiAbstractModule(String moduleID, String instanceName, PmiCallback callback) {
        this(null, moduleID, instanceName, callback);
    }

    public boolean isStandaloneTree() {
        return bStandaloneTree;
    }

    public PmiAbstractModule(ObjectName mbeanName, String moduleID, String instanceName, PmiCallback callback) {
        // System.out.println("%%%%%% calling PmiAbstractModule .. for "+mbeanName+". Instance ="+instanceName+". callback = "+callback+".");
        if (!PmiRegistry.isDisabled()) {
            this.instanceName = instanceName;
            this.callback = callback;
            this.mbeanName = mbeanName;
            //moduleConfig = PmiRegistry.getConfig(moduleID);
            // VELA: this is path is not taken by Custom PMI
            //System.out.println("%%%%%% calling PerfModules.getConfig"+moduleID);
            moduleConfig = PerfModules.getConfig(moduleID);
            // System.out.println("%%%%%% moduleConfig="+moduleConfig);
            if (moduleConfig == null) {
                Tr.warning(tc, "PMI0007W", moduleID);
            }
        } else {
            //System.out.println("%%%%%% calling PmiAbstractModule .PMI REG is DISABLED.");
        }
    }

    // Custom PMI constructor
    // updated for defect 165617
    public PmiAbstractModule(PmiModuleConfig mcfg, String instanceName, StatisticActions sal) {
        if (!PmiRegistry.isDisabled()) {
            this.instanceName = instanceName;
            this.moduleConfig = mcfg;
            this.statisticActionLsnr = sal;
            // System.out.println("%%%%%%@@@@@@@@@ calling PmiAbstractModule stat. listner"+sal);
        }
    }

    // dummy constructor:
    public PmiAbstractModule() {}

    // This method do nothing here. Its subclass will override if needed.
    @Override
    public void init(Object[] params) {}

    /**
     * Protected method to be called by the constructor of each class that extends
     * this class. The constructor should call it after all other initialization.
     * Call PmiRegistry.register() that does a number of work:
     * - get the instrumentation level
     * - create the data based on the level
     * - add its data to the corresponding module aggregate data
     * - insert it to the module tree
     */
    protected void registerModule(PmiModule moduleInstance) {
        if (moduleConfig != null)
            PmiRegistry.registerModule(moduleInstance);
        //setMBeanName(mbeanName);   // do not call it now. Wait until requested.
    }

    /**
     * This method removes the specified PmiModule from the PMI Module tree .
     */
    protected void unregisterModule(PmiModule moduleInstance) {
        dataTable = null;
        PmiRegistry.unregisterModule(moduleInstance);
    }

    /**
     * This method returns true if the PMI Service is disabled and
     * false if the PMI Service is enabled.
     */
    protected boolean isDisabled() {
        return PmiRegistry.isDisabled();
    }

    /**
     * Protected methods to be override in extended classes.
     * In each extended class (ie, concrete module class), the developer will get the
     * reference of the newly created data and store the reference in a Spd<Type> member.
     * NOTE: Although Java can safely cast the data type since the type is static, there is
     * a method per type here due to a compromise with C++ code in EE. The same code
     * will also be implemented in C++ for EE. However, C++ can cast a class to any
     * other class, a wrong casting may corrupt memory. That is why we seperate types
     * here.
     */
    protected boolean longCreated(SpdLong data) {
        return false;
    }

    protected boolean doubleCreated(SpdDouble data) {
        return false;
    }

    protected boolean statCreated(SpdStat data) {
        return false;
    }

    protected boolean loadCreated(SpdLoad data) {
        return false;
    }

    // overridden by extended classes: 215921
    protected void counterStateChanged(boolean isAllDisabled) {
        // do nothing
        // sub class will override this method to receive notification
        // when the bAllCountersDisabled is changed
    }

    protected boolean externalStatisticCreated(StatisticImpl stat) {
        return false;
    }

    // method to call back runtime - should be component dependent
    // Do nothing here and the specific module implementation should overwrite it
    protected void callbackRuntime(int newLevel) {}

    // This method may be overrided by subclasses.
    // Based on the local vars, initialize concurrent counters
    protected void initializeMe(int newLevel) {}

    // Fine grained
    protected void initializeFG(int dataID) {
        SpdData data = (SpdData) dataTable.get(new Integer(dataID));

        if (data != null && data instanceof SpdLoadImpl) {
            // reset negative numbers to zero
            RangeStatisticImpl val = (RangeStatisticImpl) data.getStatistic();
            if (val != null && val.getCurrent() < 0) {
                val.setLastValue(0);
            }
        }
    }

    // Put the data to the hashtable when create and enabled a data
    protected synchronized void putToTable(SpdData data) {
        if (data == null)
            return;
        dataTable.put(new Integer(data.getId()), data);
        // Uncomment the following if we need statistic info on server side.
        // bind config info with the data
        data.setDataInfo(moduleConfig);
        if (data.isEnabled())
            addStatToEnabledArray(data.getId());
    }

    private void addStatToEnabledArray(int stat) {
        // first check and see if the stat is already in the list...
        for (int i = 0; i < enabled.length; i++) {
            if (enabled[i] == stat)
                return;
        }

        // allocate new array
        int[] newEnabled = new int[enabled.length + 1];

        // cycle through stats and add them to new array
        for (int i = 0; i < enabled.length; i++)
            newEnabled[i] = enabled[i];

        // append new stat to end of new array
        newEnabled[enabled.length] = stat;

        // assign new array to class member
        enabled = newEnabled;
    }

    private void removeStatFromEnabledArray(int stat) {
        int len = enabled.length;

        // first run through and verify that the stat is in the list...
        // if it is in the list, we'll need to decrement the size of the new
        // array. Just in case the stat is in the list twice, we'll check the
        // entire array...shouldn't be the case, but better to be safe
        for (int i = 0; i < enabled.length; i++) {
            if (enabled[i] == stat)
                len--;
        }

        // no need proceeding if stat is not in list
        if (enabled.length == len)
            return;
        else if (len < 0)
            len = 0;

        // allocate the new array
        int[] newEnabled = new int[len];

        // cycle through the old array, copying the values over
        // to the new array - skipping the value being removed
        int ii = 0;
        for (int i = 0; i < enabled.length; i++) {
            if (!(enabled[i] == stat))
                newEnabled[ii++] = enabled[i];
        }

        // assign new array to class member
        enabled = newEnabled;
    }

    // Remove the data from the hashtable
    // Note: we actually never remove a data form hashtable - we only disable it
    protected synchronized void removeFromTable(SpdData data) {
        if (data == null)
            return;
        dataTable.remove(new Integer(data.getId()));
        removeStatFromEnabledArray(data.getId()); //dtaxxx - add
    }

    /**
     * create a SpdData based on the PmiDataInfo in moduleConfig.
     * This should be shared by extended concrete module classess except for
     * the following cases:
     * - modules for aggregate data
     * - modules for external data
     * - common submodule entry for all subinstances (eg, all methods in BeanModule)
     * It may return an aggregate data.
     */
    protected SpdData createOneData(PmiDataInfo info) {
        //System.out.println("!!!!!!!!!!! calling createOneData");
        if (info == null || info.getLevel() == LEVEL_DISABLE)
            return null;

        // Platform check - z merge
        if (!info.isAvailableInPlatform(PmiRegistry.PLATFORM)) {
            return null;
        }

        // data in submodule must be one of them: module/submodule/subinstance
        if ((info.getSubmoduleName() != null) &&
            type != TYPE_MODULE && type != TYPE_SUBMODULE && type != TYPE_SUBINSTANCE) {
            return null;
        }

        // data out of a submodule cannot be created
        if (type == TYPE_SUBMODULE || type == TYPE_SUBINSTANCE) {
            if (info.getSubmoduleName() == null) {
                return null;
            } else if (!(info.getSubmoduleName().equals(submoduleName))) {
                // this will happen when there are multiple submodules
                return null;
            }
        }

        // special hard-coded handling for top-level beanModule to not create the method-level data
        // in top level beanModule
        // create method level counters only methods group and in individual method instance
        // sorry...this ugly hardcoding
        String[] _path = getPath();
        if (_path[0].equals("beanModule") && _path.length == 1 && info.getId() >= 50) //50 is methodsubModule id
        {
            return null;
        }

        // check category
        if (!info.getCategory().equals("all") && !isInCategory(categoryName, info.getCategory()))
            return null;

        if (aggregateModule) {
            // create aggregate data in this case
            return createAggregateData(info);
        }

        if (info.getType() == TYPE_SUBMODULE)
            return null;

        // If not an aggregate module, do not create submodule data
        // because there is no way to aggregate data from submodules to the module
        if (!aggregateModule && type == TYPE_MODULE && info.getSubmoduleName() != null)
            return null;

        int dataId = info.getId();

        // custom pmi external statistic
        if (info.isUpdateOnRequest()) {
            SpdStatisticExternal onReqStat = null;
            if (statisticActionLsnr != null) {
                onReqStat = new SpdStatisticExternal(info, statisticActionLsnr);
                externalStatisticCreated(onReqStat.getStatisticRef());
            }

            return onReqStat;
        }

        switch (info.getType()) {
            case TYPE_LONG:
                SpdLong longData = new SpdLongImpl(dataId);
                longCreated(longData);
                return longData;
            case TYPE_DOUBLE:
                SpdDouble doubleData = new SpdDoubleImpl(dataId);
                doubleCreated(doubleData);
                return doubleData;
            case TYPE_STAT:
                SpdStat statData = new SpdStatImpl(dataId);
                statCreated(statData);
                return statData;
            case TYPE_AVGSTAT:
                SpdStat avgData = new SpdStatImpl(TYPE_AVGSTAT, dataId);
                statCreated(avgData);
                return avgData;
            case TYPE_LOAD:
                SpdLoad loadData = new SpdLoadImpl(dataId);
                loadCreated(loadData);
                return loadData;
            case TYPE_RANGE:
                SpdLoad rangeData = new SpdLoadImpl(TYPE_RANGE, dataId);
                loadCreated(rangeData);
                return rangeData;
            default:
                return null;
        }
    }

    private boolean isInCategory(String myCategory, String categoryList) {
        if (myCategory == null)
            return true;
        StringTokenizer st = new StringTokenizer(categoryList, "+");
        while (st.hasMoreTokens()) {
            if (myCategory.equals(st.nextToken()))
                return true;
        }
        return false;
    }

    /*
     * Create one aggregate data based on the data info
     */
    protected SpdData createAggregateData(PmiDataInfo info) {
        if (!info.isAggregatable())
            return null;

        int dataId = info.getId();

        // custom pmi external statistic
        if (info.isUpdateOnRequest()) {
            // SpdStatisticAggregate can also be used for non external data
            return new SpdStatisticAggregate(info);
        }

        switch (info.getType()) {
            case TYPE_LONG:
                SpdLongAggregate longData = new SpdLongAggregate(dataId);
                return longData;
            case TYPE_DOUBLE:
                SpdDoubleAggregate doubleData = new SpdDoubleAggregate(dataId);
                return doubleData;
            case TYPE_STAT:
                SpdStatAggregate statData = new SpdStatAggregate(dataId);
                return statData;
            case TYPE_LOAD:
                SpdLoadAggregate loadData = new SpdLoadAggregate(dataId);
                return loadData;
            case TYPE_RANGE:
                SpdLoadAggregate rangeData = new SpdLoadAggregate(TYPE_RANGE, dataId);
                return rangeData;
            case TYPE_AVGSTAT:
                SpdStatAggregate avgData = new SpdStatAggregate(TYPE_AVGSTAT, dataId);
                return avgData;

            default:
                return null;
        }
    }

    /**
     * Create/enable the performance data whose level is lower than or equal to level
     * Create it if not in the hash table; enable it if in hashtable.
     */
    protected void createData(PmiModuleConfig aModCfg, int level) {
        if (aModCfg == null)
            return;

        if (dataTable == null)
            dataTable = new TreeMap();

        // if submoduleName is null returns all data
        PmiDataInfo[] infoList = aModCfg.listData(submoduleName);
        for (int i = 0; i < infoList.length; i++) {
            // check the level and if the data is in a submodule
            if (infoList[i].getLevel() <= level ||
                ((infoList[i].getType() == TYPE_LOAD || infoList[i].getType() == TYPE_RANGE) && infoList[i].getLevel() == LEVEL_HIGH)) {
                // check hash table if the data is already there
                // enable it if yes and create  it if not yet
                SpdData data = (SpdData) dataTable.get(new Integer(infoList[i].getId()));
                if (data != null && infoList[i].getLevel() <= level) {
                    // only enable lower level data
                    data.enable(level);
                } else if (data == null) {
                    data = createOneData(infoList[i]);
                    if (data != null) {
                        putToTable(data);
                        if (infoList[i].getLevel() <= level) // disable higher level data
                            data.enable(level);
                        else
                            data.disable();
                    } else {
                        // FIXME: Custom PMI: if this is ModuleAggregate it may be data that is not part this module
                        // and part of the sub-module
                        // Need to find the level of the data and enable it based on the input level
                        // This needs to be fixed for the case where the level is set to low -> M/H
                    }
                }
            }
        }
    }

    // Check all the data in dataTable and disable a data if its level is higher than level
    protected synchronized void removeData(int level) {
        if (dataTable == null)
            return;
        Iterator members = dataTable.values().iterator();
        while (members.hasNext()) {
            SpdData data = (SpdData) members.next();
            PmiDataInfo info = moduleConfig.getDataInfo(data.getId());
            if (info != null) {
                if (info.getLevel() > level) {
                    data.disable();
                }
            } else {
                // PmiDataInfo not found in this module. This will happen if
                // different grouping with custom pmi.
                data.disable();
            }
        }
    }

    protected synchronized void createNameDataTable() {
        if (moduleConfig == null) {
            Tr.warning(tc, "PMI0007W", getModuleID());
            return;
        }
        if (nameDataTable != null)
            return;
        PmiDataInfo[] dataInfos = moduleConfig.listAllData();
        nameDataTable = new HashMap(dataInfos.length * 2);
        for (int i = 0; i < dataInfos.length; i++)
            nameDataTable.put(dataInfos[i].getName(), new Integer(dataInfos[i].getId()));
    }

    // Implement methods in PerfModule interface.
    // These methods will be called through PmiRegistry
    @Override
    abstract public int getDefaultLevel();

    @Override
    abstract public String getModuleID();

    @Override
    public ObjectName getMBeanName() {
        if (!isCustomModule() && mbeanName == null)
            _findMBean();

        return mbeanName;
    }

    @Override
    public MBeanStatDescriptor getMBeanStatDescriptor() {
        if (!isCustomModule() && mbeanName == null)
            _findMBean();

        return new MBeanStatDescriptor(this.mbeanName, this.msd_sd);
    }

    @Override
    public StatDescriptor getMSD_StatDescriptor() {
        if (!isCustomModule() && mbeanName == null)
            _findMBean();

        return msd_sd;
    }

    @Override
    public StatDescriptor get60_StatDescriptor() {
        if (sd60 == null)
            sd60 = new StatDescriptor(getPath());

        return sd60;
    }

    @Override
    public void setMBeanName(ObjectName on) {
        this.mbeanName = on;
    }

    @Override
    public void setMBeanName(ObjectName on, StatDescriptor msd_sd) {
        this.mbeanName = on;
        this.msd_sd = msd_sd;
    }

    private void _findMBean() {
        // don't query PmiJMX mapper for custom module
        // if mbeanName != null, mbean already found
        if (isCustomModule() || mbeanName != null)
            return;

        // finds if a StatDescriptor
        // PmiJmxMapper: DD ->  MSD
        MBeanStatDescriptor msd = null;//PmiRegistry.jmxMapper.getMBeanStatDescriptor (null, new DataDescriptor(getPath()));
        if (msd != null) {
            this.mbeanName = msd.getObjectName();
            this.msd_sd = msd.getStatDescriptor();
        } else {
            this.msd_sd = new StatDescriptor(getPath()); // set the dd to be StatDescriptor
            Tr.warning(tc, "PMI0006W", getModuleID() + "," + getName());
        }
    }

    /**
     * Return an array of MBeanStatDescriptor (may be null) which can be used to
     * query a subset of PMI data in the MBean/Module. It provides a finer granularity
     * than MBean.
     * 
     * Sub-class should implement its own if it has any MBeanStatDescriptor.
     */
    @Override
    public MBeanStatDescriptor[] listStatMembers() {
        return null;
    }

    @Override
    public String getName() {
        if (name != null)
            return name;

        // set name - initialization
        String[] path = getPath();

        if (path == null || path.length == 0)
            name = null;
        else
            name = path[path.length - 1];

        return name;
    }

    /**
     * getPath provides a pre-defined way in the module tree.
     * Note: extended class should override it if doesn't follow the predefined
     * module -> [category] -> instance -> submodule -> subinstance hierarchy.
     * 
     * For example, you may want to have module -> submodule or
     * module -> submodule ->subinstance hierarchy,
     * You should override getPath() in your own module class.
     */
    @Override
    public String[] getPath() {
        String[] path = null;
        if (type == TYPE_MODULE) {
            path = new String[] { getModuleID() };
        } else {
            if (categoryName == null) {
                if (type == TYPE_INSTANCE) {
                    path = new String[] { getModuleID(), instanceName };
                } else if (type == TYPE_SUBMODULE) {
                    path = new String[] { getModuleID(), instanceName, submoduleName };
                } else if (type == TYPE_SUBINSTANCE) {
                    path = new String[] { getModuleID(), instanceName, submoduleName, subinstanceName };
                }
            } else {
                if (type == TYPE_CATEGORY) {
                    path = new String[] { getModuleID(), categoryName };
                } else if (type == TYPE_INSTANCE) {
                    path = new String[] { getModuleID(), categoryName, instanceName };
                } else if (type == TYPE_SUBMODULE) {
                    path = new String[] { getModuleID(), categoryName, instanceName, submoduleName };
                } else if (type == TYPE_SUBINSTANCE) {
                    path = new String[] { getModuleID(), categoryName, instanceName, submoduleName, subinstanceName };
                }
            }
        }

        return path;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public int getInstrumentationLevel() {
        if (currentLevel == LEVEL_ENABLE)
            return LEVEL_NONE;
        else
            return currentLevel;
    }

    @Override
    public int[] getEnabled() {
        return enabled;
    }

    @Override
    public int[] getEnabledSync() {
        return enabledSync;
    }

    //protected void update
    /**
     * set instrumentation level to be newLevel:
     * - disable the higher level data if any
     * - create/enalbe the lower leve data if any
     */
    @Override
    public synchronized void setInstrumentationLevel(int newLevel) {
        // use try/catch in case it has confiction with deregister
        try {
            // something is wrong
            if (moduleConfig == null) {
                Tr.warning(tc, "PMI0007W", getModuleID());
                return;
            }

            if (newLevel == LEVEL_UNDEFINED)
                return;

            // do nothing if same level
            if (newLevel == currentLevel) {
                // level is not changed
                return;
            } else if (currentLevel == LEVEL_FINEGRAIN || newLevel < currentLevel) {
                // level is changed to be lower
                createData(moduleConfig, newLevel); // this will reset the sync boolean
                removeData(newLevel);
            } else if (newLevel > currentLevel) {
                // level is changed to be higher
                createData(moduleConfig, newLevel);
                initializeMe(newLevel);
            }

            // update currentLevel
            currentLevel = newLevel;
        } catch (Exception ex) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, ex.getMessage());
            }

        }

        updateDataList();
        updateEnabledByLevel();
    }

    @Override
    public void setInstrumentationBySet(String set) {
        if (moduleConfig != null) {
            setFineGrainedInstrumentation(getStatisticIDBySet(set), new int[0]);
        }
    }

    @Override
    public synchronized boolean setFineGrainedInstrumentation(int[] enabled, int[] enableWithSync) {
        if (moduleConfig == null) {
            Tr.warning(tc, "PMI0007W", getModuleID());
            return false;
        }

        currentLevel = LEVEL_FINEGRAIN;

        // EMPTY SPEC: Disable all
        if (enabled == null || enabled.length == 0) {
            this.enabled = enabled;
            this.enabledSync = enableWithSync;
            disableAll();

            updateDataList();
            return true;
        }

        boolean bSelectiveDisable = false;
        boolean bEnableAll = false;

        // 1. process enable[]
        if (enabled.length == 1 && enabled[0] == PmiConstants.ALL_DATA) {
            this.enabled = enabled;
            enableAll(false); // enable all WITHOUT sync
            bEnableAll = true;

            //updateEnabledByLevel ();   // update enable with ALL dataIDs
        } else {
            //this.enabled = enabled;
            ArrayList enList = new ArrayList(2);
            bSelectiveDisable = true;
            // enable SELECTIVELY
            if (dataTable == null)
                dataTable = new TreeMap();

            for (int i = 0; i < enabled.length; i++) {
                SpdData data = (SpdData) dataTable.get(new Integer(enabled[i]));

                if (data == null) {
                    PmiDataInfo dInfo = moduleConfig.getDataInfo(enabled[i]);
                    if (dInfo != null)
                        data = createOneData(dInfo);
                    if (data != null) {
                        putToTable(data);
                        enList.add(new Integer(enabled[i]));
                    }
                } else {
                    if (!data.isEnabled()) {
                        // counter state: disabled -> enabled
                        initializeFG(enabled[i]);
                    }
                    data.enable(PmiConstants.LEVEL_LOW); //LEVEL_LOW will enable
                    enList.add(new Integer(enabled[i]));
                }
            }

            // update enabled with valid data IDs
            int[] nEnabled = new int[enList.size()];
            for (int k = 0; k < nEnabled.length; k++)
                nEnabled[k] = ((Integer) enList.get(k)).intValue();

            this.enabled = nEnabled;
        }

        /*
         * enableSync is disabled
         * // 2. process enableSync []
         * if (enableWithSync.length == 1 && enableWithSync[0] == PmiConstants.ALL_DATA)
         * {
         * this.enabledSync = enableWithSync;
         * enableAll (true); // enable all WITH sync
         * bEnableAll = true;
         * }
         * else
         * {
         * // enable SELECTIVELY
         * ArrayList enList = new ArrayList(2);
         * bSelectiveDisable = true;
         * for(int i = 0; i < enableWithSync.length; i++)
         * {
         * SpdData data = (SpdData)dataTable.get(new Integer (enableWithSync[i]));
         * 
         * if (data == null)
         * {
         * PmiDataInfo dInfo = moduleConfig.getDataInfo (enableWithSync[i]);
         * if (dInfo != null)
         * data = createOneData (dInfo);
         * 
         * //data = createOneData (moduleConfig.getDataInfo (enableWithSync[i]));
         * if (data != null)
         * {
         * putToTable(data);
         * enList.add(new Integer(enableWithSync[i]));
         * }
         * }
         * else
         * {
         * if (!data.isEnabled())
         * {
         * // counter state: disabled -> enabled
         * initializeFG (enableWithSync[i]);
         * }
         * data.enable(PmiConstants.LEVEL_MAX); //LEVEL_MAX will enable_With_Sync
         * enList.add(new Integer(enableWithSync[i]));
         * }
         * }
         * 
         * int[] nEnabled = new int[enList.size()];
         * for (int k = 0; k < nEnabled.length; k++)
         * nEnabled[k] = ((Integer)enList.get(k)).intValue();
         * 
         * this.enabledSync = nEnabled;
         * }
         */

        // 3. Disable counters that are not enabled by enable[] and enableWithSync[]
        if (!bEnableAll && bSelectiveDisable) {
            // Disable others
            Iterator members = dataTable.values().iterator();
            while (members.hasNext()) {
                SpdData data = (SpdData) members.next();
                int did = data.getId();
                boolean found = false;

                for (int i = 0; i < enabled.length; i++) {
                    if (did == enabled[i]) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    for (int k = 0; k < enableWithSync.length; k++) {
                        if (did == enableWithSync[k]) {
                            found = true;
                            break;
                        }
                    }

                    if (!found)
                        data.disable();
                }
            }
        }

        enableDependencies();
        updateDataList();
        return true;
    }

    // not used. need to update dataList and enabled[]
    protected void setStatisticEnabled(int id, boolean flag, boolean bCreateIfReqd) {
        SpdData data = (SpdData) dataTable.get(new Integer(id));
        if (flag) {
            if (data == null) {
                if (bCreateIfReqd) {
                    data = createOneData(moduleConfig.getDataInfo(id));
                    if (data != null)
                        putToTable(data); //enabled by default when created
                }
            } else {
                if (!data.isEnabled()) {
                    // counter state: disabled -> enabled
                    initializeFG(id);
                    data.enable(PmiConstants.LEVEL_LOW); //LEVEL_LOW will enable
                }
            }
        } else {
            if (data != null) {
                data.disable();
            }
        }
    }

    //
    private void enableDependencies() {
        int[] parent = moduleConfig.listStatisticsWithDependents();
        if (parent == null || parent.length == 0)
            return;

        // for each statistic
        for (int i = 0; i < parent.length; i++) {
            if (isEnabled(parent[i])) {
                ArrayList child = moduleConfig.getDataInfo(parent[i]).getDependency();
                if (child == null)
                    continue;

                // enable all dependencies
                for (int k = 0; k < child.size(); k++) {
                    setStatisticEnabled(((Integer) child.get(k)).intValue(), true, true);
                }
            }
        }
    }

    // returns the custom pmi child template list
    // applicable only if different templates are nested
    // overridden in StatsGroupImpl
    protected String[] getCustomSubModuleList() {
        return null;
    }

    private void enableAll(boolean sync) {
        if (dataTable == null)
            dataTable = new TreeMap();

        _enableAllInModule(moduleConfig, sync);

        // get child template list (applicable to custom pmi StatsGroup only)
        String[] childModules = getCustomSubModuleList();

        // enable all the nested template counters at the aggregate level
        if (childModules != null) {
            for (int k = 0; k < childModules.length; k++) {
                PmiModuleConfig childModCfg = PerfModules.getConfig(childModules[k]);
                if (childModCfg != null)
                    _enableAllInModule(childModCfg, sync);
            }
        }
    }

    private void _enableAllInModule(PmiModuleConfig _config, boolean sync) {
        //PmiRegistry.printpath(getPath());
        PmiDataInfo[] infoList = _config.listData(submoduleName); // if submoduleName is null returns all data
        for (int i = 0; i < infoList.length; i++) {
            SpdData data = (SpdData) dataTable.get(new Integer(infoList[i].getId()));

            if (data == null) {
                data = createOneData(infoList[i]);
                if (data != null)
                    putToTable(data);
            }

            if (data != null) {
                if (!data.isEnabled()) {
                    // counter state: disabled -> enabled
                    initializeFG(infoList[i].getId());
                }

                if (sync)
                    data.enable(PmiConstants.LEVEL_MAX); //enableWithSync
                else
                    data.enable(PmiConstants.LEVEL_LOW); //enable
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "enableAll: cannot create data: " + _config.getUID() + "; " + infoList[i].getId());
            }
        }
    }

    private void disableAll() {
        if (dataTable != null) {
            Iterator itr = dataTable.values().iterator();
            while (itr.hasNext()) {
                ((SpdData) itr.next()).disable();
            }
        }
    }

    @Override
    public synchronized void enableData(int dataId) {
        if (dataTable == null)
            dataTable = new TreeMap();

        if (moduleConfig == null)
            return;

        if (dataId == ALL_DATA) { // equivalent to turn on all data
            setInstrumentationLevel(LEVEL_HIGH);
            return;
        }

        SpdData data = (SpdData) dataTable.get(new Integer(dataId));
        if (data != null) {
            data.enable(currentLevel);
        } else { // we don't create data here for aggregate module
            PmiDataInfo[] infoList = moduleConfig.listAllData();
            PmiDataInfo info = null;
            for (int j = 0; j < infoList.length; j++) {
                if (infoList[j].getId() == dataId) {
                    info = infoList[j];
                    break;
                }
            }

            if (info == null) // cannot find the data, do nothing
                return;

            // update the level
            if (currentLevel == LEVEL_NONE)
                currentLevel = LEVEL_ENABLE;

            data = createOneData(info);
            if (data != null)
                putToTable(data);
        }
    }

    @Override
    public synchronized void disableData(int dataId) {
        if (dataTable == null)
            return;
        if (dataId == ALL_DATA) { // equivalent to turn off all data
            setInstrumentationLevel(LEVEL_NONE);
        } else {
            SpdData data = (SpdData) dataTable.get(new Integer(dataId));
            if (data != null) {
                data.disable();
            }
            // call listData to update the level
            SpdData[] list = listData();

            // update the level except for TYPE_MODULE and TYPE_SUBMODULE
            if (list == null || list.length == 0) {
                if (type != TYPE_MODULE && type != TYPE_SUBMODULE)
                    currentLevel = LEVEL_NONE;
            }

        }
    }

    /**
     * return an array of all the enabled SpdData in the hash table
     */

    @Override
    public SpdData[] listData() {
        if (dataTable == null) // FG: || (currentLevel <= LEVEL_NONE))
            return null;

        Iterator members = dataTable.values().iterator();
        ArrayList list = new ArrayList();
        while (members.hasNext()) {
            SpdData data = (SpdData) members.next();
            if (data.isEnabled())
                list.add(data);
        }

        // construct the return array
        SpdData[] retData = new SpdData[list.size()];
        for (int i = 0; i < list.size(); i++)
            retData[i] = (SpdData) list.get(i);
        return retData;
    }

    /**
     * Return a SpdData if it is enabled.
     */
    @Override
    public SpdData listData(int dataId) {
        if (dataTable == null)
            return null;

        SpdData data = (SpdData) dataTable.get(new Integer(dataId));
        if (data != null && data.isEnabled()) {
            return data;
        } else {
            return null;
        }
    }

    /**
     * Return an array of SpdData
     */
    @Override
    public SpdData[] listData(int[] dataIds) {
        if (dataTable == null) // FG: || (currentLevel <= LEVEL_NONE))
            return null;

        if (dataIds == null)
            return listData();

        ArrayList list = new ArrayList(dataIds.length);
        for (int i = 0; i < dataIds.length; i++) {
            SpdData data = (SpdData) dataTable.get(new Integer(dataIds[i]));
            if (data != null && data.isEnabled())
                list.add(data);
        }
        if (list.size() == 0)
            return null;

        // construct the return array
        SpdData[] retData = new SpdData[list.size()];
        for (int i = 0; i < list.size(); i++)
            retData[i] = (SpdData) list.get(i);
        return retData;
    }

    public void updateDataList() {
        if (dataTable == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "PmiAbstractModule.updateDataList: module data is not initialized");
            return;
        }
        dataList.clear();

        Iterator members = dataTable.values().iterator();
        while (members.hasNext()) {
            SpdData data = (SpdData) members.next();
            if (data.isEnabled()) {
                StatisticImpl s = data.getStatistic();
                if (s == null) {
                    Tr.warning(tc, "PMI9999E", "PmiAbstractModule.updateDataList: " + getModuleID() + ", " + getName());
                    Tr.warning(tc, "PMI9999E", new Exception().fillInStackTrace());
                } else
                    dataList.add(s);
            }
        }

        // flag to indicate if all counters are disabled
        // the dataList is updated when the instrumentation level is changed
        // CUSTOM PMI:statisticActionLsnr can be called here to indicate if all counters are disabled
        if (dataList.size() > 0) {
            if (bAllCountersDisabled) // true -> false
            {
                bAllCountersDisabled = false;
                counterStateChanged(false); //215921
            }
        } else {
            if (!bAllCountersDisabled) // false -> true
            {
                bAllCountersDisabled = true;
                counterStateChanged(true); //215921
            }
        }
    }

    public boolean isAllDisabled() {
        return bAllCountersDisabled;
    }

    protected void update() {

        // TODO: need to optimize this call
        Iterator members = dataTable.values().iterator();
        while (members.hasNext()) {
            SpdData data = (SpdData) members.next();
            if (data.isEnabled()) {
                if (data.isExternal())
                    data.updateExternal();
                else if (data.isAggregate())
                    ((SpdGroup) data).updateAggregate();
            }
        }

        /*
         * int sz = dataList.size();
         * for (int i = 0; i < sz; i++)
         * {
         * // can't do this. dataList has statistic and not spdata
         * SpdData d = (SpdData)dataList.get(i);
         * if (d.isExternal())
         * {
         * d.updateExternal();
         * }
         * }
         */
    }

    @Override
    public void updateStatistics() {
        update();
    }

    @Override
    public ArrayList listStatistics() {
        // TODO: need to optimize this call

        update();

        return dataList; //NOTES: isJ2EEStatisticProvider uses this list to find if atleast one statisitc is enabled

        /*
         * if(dataTable == null) // || (currentLevel <= LEVEL_NONE))
         * return null;
         * 
         * Iterator members = dataTable.values().iterator();
         * 
         * ArrayList list = new ArrayList();
         * while(members.hasNext())
         * {
         * SpdData data = (SpdData)members.next();
         * if(data.isEnabled())
         * list.add(data.getStatistic());
         * }
         * return list;
         */
    }

    // Note: construct dataMembers in ModuleItem and pass it here
    //       because this method will be overwritten by subclasses
    //       and we don't want to duplicate the same code.
    @Override
    public StatsImpl getStats(ArrayList dataMembers, ArrayList colMembers) {
        // need to instantiate specifc Stats (ie EJBStats etc.)
        StatsImpl s = new StatsImpl(getName(), type, currentLevel, dataMembers, colMembers);
        s.setStatsType(getWCCMStatsType()); //getModuleID());

        // VELA: moduleID is same as UID for pre-defined module
        // This is pre-defined module path
        return s;
    }

    // generic methods to update data
    @Override
    public void updateData(int dataId, int opType, double value) {
        if (dataTable == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "PmiAbstractModule.updateData: module data is not initialized");
            return;
        }

        SpdData mydata = (SpdData) dataTable.get(new Integer(dataId));
        updateData(mydata, opType, value);
    }

    @Override
    public void updateData(String dataName, int opType, double value) {
        if (nameDataTable == null) {
            createNameDataTable();
            if (nameDataTable == null) {
                return;
            }
        }
        Integer dataId = (Integer) nameDataTable.get(dataName);
        SpdData mydata = (SpdData) dataTable.get(dataId);
        updateData(mydata, opType, value);
    }

    @Override
    public void updateData(SpdData mydata, int opType, double value) {
        if (mydata == null) {
            Tr.warning(tc, "PMI0005W", "PmiAbstractModule.updateData, moduleID=" + getModuleID());
            return;
        }

        if (mydata instanceof SpdLong) {
            if (opType == SET)
                ((SpdLong) mydata).set((long) value);
            else if (opType == INCREMENT)
                ((SpdLong) mydata).increment((long) value);
            else if (opType == DECREMENT)
                ((SpdLong) mydata).decrement((long) value);
            else
                Tr.warning(tc, "PMI0009W", "opType");
        } else if (mydata instanceof SpdLoad) {
            if (opType == SET)
                ((SpdLoad) mydata).add(value);
            else if (opType == INCREMENT)
                ((SpdLoad) mydata).increment(value);
            else if (opType == DECREMENT)
                ((SpdLoad) mydata).decrement(value);
            else
                Tr.warning(tc, "PMI0009W", "opType");
        } else if (mydata instanceof SpdStat) {
            if (opType == SET)
                ((SpdStat) mydata).add(value);
            else
                Tr.warning(tc, "PMI0009W", "opType");
        } else {
            Tr.warning(tc, "PMI0009W", "dataType");
        }
    }

    /**
     * set all the counters to be null.
     */
    @Override
    public void cleanup() {
        // d180336: remove MBean->ModuleItem map unregistering PMI module
        // remove from map if mbean mapping is done
        // don't call getMbeanStatDescriptor as it call _findMBean (). If the mapping is not
        // done then why try to remove.
        if (mbeanName != null) {
            PmiRegistry.removeMBeanToModuleMap(new MBeanStatDescriptor(mbeanName, msd_sd));
        }

        if (isCustomModule()) {
            StatsFactoryUtil.deactivateMBean(mbeanName);
        }

        // null data table
        if (dataTable != null) {
            dataTable.clear();
            dataTable = null;
        }

        // null name data table
        if (nameDataTable != null) {
            nameDataTable.clear();
            nameDataTable = null;
        }

        // null the members - not necessary
        instanceName = null;
        subinstanceName = null;
        categoryName = null;
        mbeanName = null;
        msd_sd = null;
        moduleConfig = null;
        callback = null;
    }

    /**
     * Each module may need to have each own deregister method
     */
    @Override
    public void unregister() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "unregister " + getModuleID() + ", " + getName());
        PmiRegistry.unregisterModule(this);
        currentLevel = LEVEL_NONE;
        cleanup();
    }

    // By default its not a custom module
    @Override
    public boolean isCustomModule() {
        return false;
    }

    @Override
    public PmiModuleConfig getModuleConfig() {
        return moduleConfig;
    }

    // The default impl is to call ModuleItem.getStats(false)
    // ModuleItem->PmiAbstractModule->ModuleItem (default impl)
    // Modules that need special handling (eg. J2CModule) will override the method
    @Override
    public StatsImpl getJSR77Stats(ModuleItem mitem) {
        return mitem.getStats(false);
    }

    // Method to identify counters in a given PMIModule in the tree
    // Used in fine grained administration
    // This is for pre-custom only. For custom this method is overridden in StatsInstanceImpl and StatsGroupImpl

    // webAppModule => all counters
    // webAppModule# => counters at top level excluding the submodule counters
    // webAppModule#webAppModule.servlets = > counters at webAppModule.servlets submodule
    // overridden by BeanModule.java
    @Override
    public String getWCCMStatsType() {
        StringBuffer b = new StringBuffer(getModuleID());
        if (type == TYPE_MODULE)
            return b.toString();
        else if (type == TYPE_SUBMODULE || type == TYPE_SUBINSTANCE)
            return b.append("#").append(submoduleName).toString();
        else if (type == TYPE_INSTANCE) {
            if (moduleConfig.hasSubModule())
                b.append("#");

            return b.toString();
        } else
            return b.toString();
    }

    public boolean isEnabled(int dataID) {
        if (dataTable == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "PmiAbstractModule.isEnabled: module data is not initialized");
            return false;
        }
        SpdData data = (SpdData) dataTable.get(new Integer(dataID));
        if (data == null)
            return false;
        else
            return data.isEnabled();

        /*
         * for (int i = 0; i < enabled.length; i++)
         * {
         * if (enabled[i] == dataID)
         * return true;
         * }
         * 
         * for (int i = 0; i < enabledSync.length; i++)
         * {
         * if (enabledSync[i] == dataID)
         * return true;
         * }
         * 
         * return false;
         */
    }

    // this method will be overridden by individual modules that provide JSR77 statistics
    @Override
    public boolean isJ2EEStatisticProvider() {
        //191705.1 - StatisticProvider = true if one or more statistic is enabled
        //if (currentLevel > LEVEL_NONE || currentLevel == PmiConstants.LEVEL_FINEGRAIN)
        if (dataList != null && dataList.size() > 0)
            return true;
        else
            return false;
    }

    // overridden in StatsGroupImpl
    public int[] getStatisticIDBySet(String setID) {
        if (moduleConfig != null)
            return moduleConfig.listStatisticsBySet(setID);
        else
            return new int[0];
    }

    // enable/disable sync for all "enabled" statistics
    @Override
    public void setSyncEnabled(boolean sync) {
        if (dataTable != null) {
            Iterator members = dataTable.values().iterator();
            while (members.hasNext()) {
                SpdData data = (SpdData) members.next();
                if (data.isEnabled()) {
                    if (sync)
                        data.enable(LEVEL_MAX);
                    else
                        data.enable(LEVEL_LOW);
                }
            }
        }
    }

    public void updateEnabledByLevel() {
        if (currentLevel == LEVEL_FINEGRAIN)
            return;

        if (dataTable != null) // FG: || (currentLevel <= LEVEL_NONE))
        {
            Iterator members = dataTable.values().iterator();
            ArrayList list = new ArrayList();
            while (members.hasNext()) {
                SpdData data = (SpdData) members.next();
                if (data.isEnabled())
                    list.add(new Integer(data.getId()));
            }

            this.enabled = new int[list.size()];
            for (int i = 0; i < list.size(); i++)
                this.enabled[i] = ((Integer) list.get(i)).intValue();
        }
    }
}
