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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.management.ObjectName;

import com.ibm.websphere.pmi.PmiConstants;
import com.ibm.websphere.pmi.server.PmiAbstractModule;
import com.ibm.websphere.pmi.server.PmiModule;
import com.ibm.websphere.pmi.server.PmiModuleAggregate;
import com.ibm.websphere.pmi.server.SpdData;
import com.ibm.websphere.pmi.stat.MBeanLevelSpec;
import com.ibm.websphere.pmi.stat.StatLevelSpec;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.pmi.stat.StatsImpl;

public class ModuleItem implements java.io.Serializable, PmiConstants {
    private static final long serialVersionUID = 4893797935750745475L;

    PmiModule instance = null;
    //ArrayList children = null;
    Map children = null;
    ModuleItem parent = null;
    int level = LEVEL_UNDEFINED;

    private static final TraceComponent tc = Tr.register(ModuleItem.class);

    private StatsImpl myStats;
    private StatsImpl myStatsWithChildren;

    //private boolean bStatsTreeNeedsUpdate = false;

    // Constructor: root of module tree has null instance
    public ModuleItem() {
        this(null);
    }

    // Constructor:
    // Allow setting the inital size of the children vector.
    public ModuleItem(PmiModule instance) {
        this.instance = instance;
    }

    // Return the instance 
    public PmiModule getInstance() {
        return instance;
    }

    // Find an immediate child 
    public ModuleItem find(String name) {
        if (children == null)
            return null;
        else
            return (ModuleItem) children.get(name);

        /*
         * for(int i=0; i<children.size(); i++)
         * {
         * ModuleItem item = (ModuleItem)children.get(i);
         * if(item.instance != null)
         * {
         * if(item.instance.getName().equals(name))
         * return item;
         * }
         * }
         * return null;
         */
    }

    // Find a child in the subtree - do it recursively
    public ModuleItem find(String[] path, int index) {
        if (path == null)
            return null;

        if (path.length == 0)
            return this;

        ModuleItem item = find(path[index]);
        if (item == null)
            return null;

        if (index == path.length - 1)
            return item;
        else
            return item.find(path, index + 1);
    }

    // Find and add a child in the subtree if not found - do it recursively
    public synchronized ModuleItem add(String[] path, int index) {
        if (path == null)
            return null;
        if (path.length == 0)
            return this;

        ModuleItem item = find(path[index]);

        // custom module shouldn't go inside if{}
        if (item == null) {
            // Create Aggregate Module
            String[] myPath = new String[index + 1];
            System.arraycopy(path, 0, myPath, 0, myPath.length);

            if (myPath.length == 1) { // first layer will have aggregation
                ModuleAggregate aggregate = new ModuleAggregate(myPath[0]);
                item = find(path[index]);
            } else if (myPath.length == 3) { // this is supposed to be submdoule layer
                new ModuleAggregate(myPath[0], myPath[1], myPath[2]);
                item = find(path[index]);
            } else { // lower levels will not have aggregation
                //item = new ModuleItem(new PmiModuleDummyImpl(myPath));      
                Tr.warning(tc, "PMI9999E", "Parent module not found.");
            }

            // Add aggregate module to tree
            add(item);
        }

        if (index == path.length - 1)
            return item;
        else
            return item.add(path, index + 1);
    }

    // Add a child to it in a sorted way - synchronized 
    // Note: items must have different names under same parent
    public synchronized boolean add(ModuleItem item) {
        if (item == null)
            return false;

        if (children == null)
            //children = new ArrayList();
            children = new TreeMap();

        String itemName = item.getInstance().getName();
        if (find(itemName) == null) {
            item.setParent(this);
            children.put(itemName, item);

            /*
             * // add it to parent in a sorted way
             * int ind = 0;
             * boolean added = false;
             * while(ind < children.size())
             * {
             * ModuleItem child = (ModuleItem)children.get(ind);
             * String childName = child.getInstance().getName();
             * 
             * int cmp = childName.compareTo(itemName);
             * if(cmp == 0)
             * {
             * // duplicate module name (display name)
             * return false;
             * }
             * else if(cmp > 0)
             * {
             * children.add(ind, item);
             * added = true;
             * break;
             * }
             * else
             * {
             * ind++;
             * }
             * }
             * 
             * if(!added)
             * children.add(item);
             */

            if (myStatsWithChildren != null)
                addToStatsTree(item);

            //bStatsTreeNeedsUpdate = true;

            return true;
        } else {
            // a module with the same name already exists
            return false;
        }
    }

    // Return an array of children - synchronized?
    public ModuleItem[] children() {
        if (children == null)
            return null;

        ModuleItem[] members = new ModuleItem[children.size()];
        children.values().toArray(members);
        return members;
    }

    public ModuleItem getParent() {
        return parent;
    }

    public void setParent(ModuleItem parent) {
        this.parent = parent;
        updateParent();

        /*
         * // do not add to parent if I am an aggregated module
         * if(instance instanceof PmiModuleAggregate)
         * return;
         * 
         * // recursively add aggregate data for parents
         * ModuleItem myParent = parent;
         * PmiModule parModule = null;
         * while(myParent != null)
         * {
         * parModule = myParent.getInstance();
         * 
         * // if parent is aggregate
         * if(parModule != null && parModule instanceof PmiModuleAggregate)
         * {
         * ((PmiModuleAggregate)parModule).add(this.getInstance());
         * }
         * 
         * myParent = myParent.getParent();
         * }
         */
    }

    private void updateParent() {
        if (parent == null)
            return;

        // do not add to parent if I am an aggregated module
        if (instance instanceof PmiModuleAggregate) {
            // don't call as aggregate data is always created with ModuleAggregate is created
            //updateChildrenInfo (children);
            return;
        }

        // recursively add aggregate data for parents
        ModuleItem myParent = this.parent;
        PmiModule myInstance = null;
        while (myParent != null) {
            myInstance = myParent.getInstance();
            // if parent is aggregate
            if (myInstance != null && myInstance instanceof PmiModuleAggregate) {
                ((PmiModuleAggregate) myInstance).add(this.getInstance());

                // d196234: aggregate data from different modules: Custom PMI
                // updating the statstype in the cached Stats
                if (myInstance.isCustomModule()) {
                    if (myParent.myStatsWithChildren != null)
                        myParent.myStatsWithChildren.setStatsType(myInstance.getWCCMStatsType());
                    if (myParent.myStats != null)
                        myParent.myStats.setStatsType(myInstance.getWCCMStatsType());
                }
                ((PmiAbstractModule) myInstance).updateDataList();
                ((PmiAbstractModule) myInstance).updateEnabledByLevel();
            }

            myParent = myParent.getParent();

            // update the immediate parent aggregate. don't add up the chain as this is special standadlone module
            if (!((PmiAbstractModule) this.getInstance()).isStandaloneTree())
                continue;
            else
                break;
        }
    }

    /*
     * When the aggregate data is created "after" the child data are created this method
     * will add reference to all the child data in the parent ModuleAggregate class.
     */
    private void updateChildrenInfo(Map myChild) {
        if (myChild != null) {
            Iterator values = myChild.values().iterator();
            while (values.hasNext()) {
                ModuleItem childMI = (ModuleItem) values.next();
                if (childMI.getInstance() instanceof PmiModuleAggregate) {
                    // no op                    
                } else {
                    ((PmiModuleAggregate) this.instance).add(childMI.getInstance());
                    ((PmiAbstractModule) this.instance).updateDataList();
                }
                updateChildrenInfo(childMI.children);
            }
        }
    }

    // Remove a child from it - synchronized
    public synchronized void remove(ModuleItem item) {
        if (item == null || children == null)
            return;

        // remove from all the parents which have links to it due to aggregate data
        PmiModule removeInstance = item.getInstance();
        if (!(removeInstance instanceof PmiModuleAggregate)) {
            // recursively remove aggregate data for parents
            ModuleItem myParent = this;
            PmiModule parModule = null;
            while (myParent != null) {
                parModule = myParent.getInstance();
                // if parent is aggregate
                if (parModule != null && parModule instanceof PmiModuleAggregate)
                    ((PmiModuleAggregate) parModule).remove(removeInstance);

                myParent = myParent.getParent();
            }
        }

        // remove any children
        item._cleanChildren();

        // remove ModuleItem
        children.remove(item.getInstance().getName());

        if (myStatsWithChildren != null) {
            updateStatsTree();
        }

        //bStatsTreeNeedsUpdate = true;

        // remove mbean mapping and deactivate any CustomStats mbean
        //_cleanMBean(item);
        item.getInstance().cleanup();
        item = null;
    }

    // dd is the DataDescriptor for this module
    public DataDescriptor[] listMembers(DataDescriptor dd, boolean jmxBased) {
        // take care of data members
        SpdData[] dataList = null;
        int dataLength = 0;

        if (!jmxBased) {
            dataList = instance.listData();
            if (dataList != null)
                dataLength = dataList.length;
        }

        // take care of non-data members
        String[] nameList = null;
        int itemLength = 0;

        ModuleItem[] items = children();
        if (items != null) {
            itemLength = items.length;
            nameList = new String[itemLength];
            for (int i = 0; i < itemLength; i++) {
                String[] path = items[i].getInstance().getPath();
                nameList[i] = path[path.length - 1];
            }
        }

        // create the returned array
        // return both item (instance/submodule) descriptor and data descriptor if any
        if (itemLength == 0 && dataLength == 0)
            return null;

        DataDescriptor[] res = new DataDescriptor[itemLength + dataLength];
        for (int i = 0; i < itemLength; i++) {
            res[i] = new DataDescriptor(dd, nameList[i]);
        }

        for (int i = 0; i < dataLength; i++) {
            res[i + itemLength] = new DataDescriptor(dd, dataList[i].getId());
        }
        return res;
    }

    public ArrayList getStatLevelSpec(boolean recursive) {
        ArrayList list = new ArrayList(1);
        if (instance != null) {
            list.add(new StatLevelSpec(instance.getPath(), instance.getEnabled()));
        }

        if (recursive) {
            ModuleItem[] mi = children();
            if (mi != null) {
                for (int i = 0; i < mi.length; i++) {
                    ArrayList cList = mi[i].getStatLevelSpec(recursive);
                    for (int k = 0; k < cList.size(); k++) {
                        list.add(cList.get(k));
                    }
                }
            }
        }

        return list;
    }

    // returns the immediate members if recursive = false
    // returns all children if recursive = true
    public ArrayList listChildStatDescriptors(boolean recursive) {
        ArrayList list = new ArrayList(1);

        ModuleItem[] mi = children();
        if (mi != null) {
            for (int i = 0; i < mi.length; i++) {
                list.add(mi[i].getInstance().get60_StatDescriptor());

                if (recursive) {
                    ArrayList cList = mi[i].listChildStatDescriptors(recursive);

                    for (int k = 0; k < cList.size(); k++)
                        list.add(cList.get(k));
                }
            }
        }
        return list;
    }

    // Used for both old style and JMX-based
    // recursively return PerfLevelDescriptor
    public ArrayList getPerfLevelDescriptors(boolean jmxBased) {
        ArrayList res = new ArrayList();
        if (instance == null) {
            res.add(new PerfLevelDescriptor(new String[] { "pmi" }, level));
        } else {
            PmiModule instance = getInstance();
            if (jmxBased) {
                if (instance.getMBeanName() != null) {
                    int level = instance.getInstrumentationLevel();
                    MBeanLevelSpec spec = new MBeanLevelSpec(instance.getMBeanName(), instance.getMSD_StatDescriptor(), level);

                    if (level == LEVEL_FINEGRAIN)
                        spec.setEnabled(instance.getEnabled());

                    res.add(spec);
                }

                //instance.getEnabledSync()));
                else
                    return res;
            } else {
                PerfLevelDescriptor pld;
                String[] path = instance.getPath();
                pld = new PerfLevelDescriptor(path, instance.getInstrumentationLevel(), instance.getModuleID());
                res.add(pld);
            }

        }

        // recursively get from children
        ModuleItem[] items = children();
        if (items == null)
            return res;

        for (int i = 0; i < items.length; i++) {
            ArrayList childrenLevels = items[i].getPerfLevelDescriptors(jmxBased);
            if (childrenLevels == null || childrenLevels.size() == 0)
                continue;

            for (int j = 0; j < childrenLevels.size(); j++)
                res.add(childrenLevels.get(j));
        }

        return res;
    }

    // recursively return PerfLevelDescriptor
    public ArrayList getTreePerfLevelDescriptors(int parentLevel) {
        ArrayList res = new ArrayList();
        int thisLevel = level;
        if (instance == null) { // should be root
            res.add(new PerfLevelDescriptor(new String[] { "pmi" }, thisLevel));
        } else {
            PmiModule instance = getInstance();
            thisLevel = instance.getInstrumentationLevel();
            // if the level is same as parent, do not add to the list
            if ((thisLevel != LEVEL_UNDEFINED) && (thisLevel != parentLevel)) {
                res.add(new PerfLevelDescriptor(instance.getPath(), thisLevel, instance.getModuleID()));
            }
        }

        // get from children
        ModuleItem[] items = children();
        if (items == null)
            return res;
        for (int i = 0; i < items.length; i++) {
            ArrayList childrenLevels = items[i].getTreePerfLevelDescriptors(thisLevel);
            for (int j = 0; j < childrenLevels.size(); j++)
                res.add(childrenLevels.get(j));
        }
        return res;
    }

    private void updateStatisticsForStatsTree(long time) {
        if (instance != null)
            instance.updateStatistics();

        if (myStatsWithChildren != null)
            myStatsWithChildren.setTime(time);

        // getStats from children
        ModuleItem[] items = children();
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                items[i].updateStatisticsForStatsTree(time);
            }

        }
    }

    // add stats for new modules that are added: called when a module item is added
    private void addToStatsTree(ModuleItem mi) {
        ArrayList colMembers = myStatsWithChildren.subCollections();
        if (colMembers == null) {
            colMembers = new ArrayList(1);
            myStatsWithChildren.setSubcollections(colMembers);
        }

        colMembers.add(mi.getStats(true));
    }

    // refresh the stats tree to remove any stats: called when a module item is removed
    private void updateStatsTree() {
        if (myStatsWithChildren == null)
            return;

        ArrayList colMembers = myStatsWithChildren.subCollections();
        if (colMembers == null)
            return;

        colMembers.clear();

        // getStats from children
        ModuleItem[] items = children();
        if (items != null) {
            for (int i = 0; i < items.length; i++)
                colMembers.add(items[i].getStats(true));
        }
    }

    /*
     * if (colMembers != null)
     * colMembers.clear();
     * else
     * {
     * colMembers = new ArrayList(1);
     * myStatsWithChildren.setSubcollections(colMembers);
     * }
     * 
     * // getStats from children
     * ModuleItem[] items = children();
     * if(items != null)
     * {
     * for(int i=0; i<items.length; i++)
     * colMembers.add(items[i].getStats(true));
     * }
     * 
     * bStatsTreeNeedsUpdate = false;
     */

    // Return a StatsImpl for all the data in this module/instance
    // May return a StatsImpl that has no data member or subcollection
    // performance update: caching the Stats tree instead of creating and trashing the Stats object
    // for each request. Each ModuleItem will have myStats and myStatsWithChildren that will
    // be created on the first request. The stats tree will be updated if the ModuleItem children
    // is updated.
    public StatsImpl getStats(boolean recursive) {
        // DO THIS METHOD NEED SYNCHRONIZED ACCESS?
        // When multiple threads get inside the method and stats tree is updated
        if (recursive) {
            /*
             * // if leaf then do not create myStatsWithChildren. Instead return myStats
             * if (children == null && !bStatsTreeNeedsUpdate)
             * {
             * // return myStats
             * if (myStats == null)
             * {
             * if (instance != null)
             * myStats = instance.getStats (instance.listStatistics(), null);
             * else
             * myStats = new StatsImpl("server", TYPE_SERVER, level, null, null);
             * }
             * 
             * return myStats;
             * }
             * else
             */
            // first time here
            if (myStatsWithChildren == null) {
                ArrayList dataMembers = null;
                if (instance != null)
                    dataMembers = instance.listStatistics();

                ArrayList colMembers = null; // return subcollection members

                // getStats from children
                ModuleItem[] items = children();
                if (items != null) {
                    colMembers = new ArrayList(items.length);
                    for (int i = 0; i < items.length; i++) {
                        colMembers.add(items[i].getStats(recursive));
                    }
                }

                if (instance != null)
                    myStatsWithChildren = instance.getStats(dataMembers, colMembers);
                else
                    myStatsWithChildren = new StatsImpl("server", TYPE_SERVER, level, null, colMembers);
            } else {
                updateStatisticsForStatsTree(System.currentTimeMillis()); // update external & aggregate statistics
            }

            return myStatsWithChildren;
        }
        // NON-RECURSIVE
        else {
            if (myStats == null) {
                if (instance != null)
                    myStats = instance.getStats(instance.listStatistics(), null);
                else
                    myStats = new StatsImpl("server", TYPE_SERVER, level, null, null);
            } else {
                if (instance != null)
                    instance.updateStatistics(); // update external & aggregate statistics

                myStats.setTime(System.currentTimeMillis());
            }

            return myStats;
        }
        /*
         * ArrayList dataMembers = instance.listStatistics();
         * ArrayList colMembers = null; // return subcollection members
         * 
         * if (recursive)
         * {
         * ModuleItem[] items = children();
         * 
         * // getStats from children
         * if(items != null)
         * {
         * colMembers = new ArrayList(items.length);
         * for(int i=0; i<items.length; i++)
         * {
         * colMembers.add(items[i].getStats(recursive));
         * }
         * }
         * }
         * 
         * // return Stats - individual instance will return different XXXStats instance
         * // Note: construct dataMembers in ModuleItem and pass it to instance
         * // because getStats will be overwritten by subclasses
         * // and we don't want to duplicate the same code.
         * 
         * return instance.getStats(dataMembers, colMembers);
         */
    }

    // Called by PmiJmxBridgeImpl.getStats()
    // This method will return Stats in JSR77 format
    // JDBCStats and JCAStats need some special handling
    // This method will route the call to the PmiAbstractModule, which has a default impl
    // The default impl (PmiAbstractModule) is to call ModuleItem.getStats(false) (this class.)
    // ModuleItem->PmiAbstractModule->ModuleItem (default impl)    
    // Modules that need special handling (eg. J2CModule) will override the method
    // ModuleItem->PmiAbstractModule->XXXModule (special impl)    
    public StatsImpl getJSR77Stats() {
        if (instance == null) {
            return null;
        } else {
            return instance.getJSR77Stats(this);
        }
    }

    /**
     * If more PmiModule.listStatistics methods are implemented,
     * this method can be simplified as listData(recursive)
     * Leave it alone now.
     */
    public StatsImpl getStats(int[] dataIds, boolean recursive) {
        if (dataIds == null)
            return getStats(recursive);

        if (instance == null)
            return null;

        SpdData[] dataList = instance.listData(dataIds);
        ModuleItem[] items = children();
        ArrayList dataMembers = null; // return data members
        ArrayList colMembers = null; // return subcollection members

        // convert from Spd to Wpd and set dataMembers
        if (dataList != null) {
            dataMembers = new ArrayList(dataList.length);
            for (int i = 0; i < dataList.length; i++) {
                dataMembers.add(dataList[i].getStatistic());
            }
        }

        // getStats from children
        if (recursive && (items != null)) {
            colMembers = new ArrayList(items.length);
            for (int i = 0; i < items.length; i++) {
                colMembers.add(items[i].getStats(dataIds, recursive));
            }
        }

        // return Stats - individual instance will return different XXXStats instance
        // Note: construct dataMembers in ModuleItem and pass it to instance
        //       because getStats will be overwritten by subclasses
        //       and we don't want to duplicate the same code.
        return instance.getStats(dataMembers, colMembers);
    }

    // **This method is called when the level is modified by the client 
    // (not when the server starts up)
    public void setInstanceLevel(int[] enabled, int[] enabledSync, int newLevel, boolean recursive) {
        // fine grained spec is not defined, use 5.0 level
        if (newLevel != PmiConstants.LEVEL_FINEGRAIN) {
            if (newLevel != PmiConstants.LEVEL_UNDEFINED) {
                setInstanceLevel_V5(newLevel, recursive);

                /*
                 * not need since PmiConfigManager.updateWithRuntimeSpec is always called before persisting
                 * // Update in-memory EMF object (PMIModule)
                 * if (instance == null)
                 * {
                 * if (PmiConfigManager.isInitialized())
                 * PmiConfigManager.updateSpec(new String[]{"pmi"}, "", newLevel, recursive, false);
                 * }
                 * else
                 * {
                 * if (PmiConfigManager.isInitialized())
                 * PmiConfigManager.updateSpec(instance.getPath(), instance.getWCCMStatsType(), newLevel, recursive, false);
                 * }
                 */
            }
        } else {
            if (enabled != null)
                setInstanceLevel_FG(enabled, enabledSync, recursive);
        }

        // update the statistic set in PMIImpl/config
        //PMIImpl.setStatisticSet(StatConstants.STATISTIC_SET_CUSTOM);                
    }

    // set level for each module/instance/submodule
    // add data to parent if the newLevel is higher
    private void setInstanceLevel_FG(int[] enabled, int[] enabledSync, boolean recursive) {
        if (instance != null) {
            // Fine grained instrumentation overrides the old level
            // First call setFineGrainedInstrumentation.
            // If fine grained level is undefined called old setInstrumentationLevel
            boolean action = instance.setFineGrainedInstrumentation(enabled, enabledSync);

            /*
             * not need since PmiConfigManager.updateWithRuntimeSpec is always called before persisting
             * // Update in-memory EMF object (PMIModule)
             * // call instance.getEnabled() to save the filtered set (filtering done by setFineGrainedInstrumentation, specifically bean module methods)
             * if (PmiConfigManager.isInitialized())
             * PmiConfigManager.updateSpec(instance.getPath(), instance.getWCCMStatsType(), instance.getEnabled(), instance.getEnabledSync(), false, false);
             */

            // FIXME: updating parent. may need to call this conditionally - when there is a change in fg spec        
            updateParent();
        }

        if (recursive) {
            ModuleItem[] items = children();
            if (items == null)
                return;
            for (int i = 0; i < items.length; i++) {
                items[i].setInstanceLevel_FG(enabled, enabledSync, recursive);
            }
        }
    }

    private void setInstanceLevel_V5(int newLevel, boolean recursive) {
        if (instance != null) {
            int oldLevel = instance.getInstrumentationLevel();
            instance.setInstrumentationLevel(newLevel);

            // Add to the parent for data aggregation
            if (oldLevel < newLevel)
                updateParent();
            /*
             * && parent != null &&
             * !(instance instanceof PmiModuleAggregate))
             * {
             * PmiModule parentModule = null;
             * ModuleItem myparent = parent;
             * while(myparent != null)
             * {
             * parentModule = myparent.getInstance();
             * if(parentModule != null && parentModule instanceof PmiModuleAggregate)
             * {
             * // add data to parent for aggregation
             * PmiModuleAggregate aggregate = (PmiModuleAggregate)parentModule;
             * //aggregate.add(instance.listData()); -custompmi
             * aggregate.add(instance);
             * }
             * myparent = myparent.getParent();
             * }
             * }
             */
        } else {
            level = newLevel;
        }

        if (recursive) {
            ModuleItem[] items = children();
            if (items == null)
                return;
            for (int i = 0; i < items.length; i++) {
                items[i].setInstanceLevel_V5(newLevel, recursive);
            }
        }
    }

    public void setInstanceLevel_Set(String set, boolean recursive) {
        if (instance != null) {
            // Fine grained instrumentation overrides the old level
            // First call setFineGrainedInstrumentation.
            // If fine grained level is undefined called old setInstrumentationLevel
            instance.setInstrumentationBySet(set);

            /*
             * not need since PmiConfigManager.updateWithRuntimeSpec is always called before persisting
             * // Update in-memory EMF object (PMIModule)
             * // call instance.getEnabled() to save the filtered set (filtering done by setFineGrainedInstrumentation, specifically bean module methods)
             * if (PmiConfigManager.isInitialized())
             * PmiConfigManager.updateSpec(instance.getPath(), instance.getWCCMStatsType(), instance.getEnabled(), instance.getEnabledSync(), false, false);
             */

            // FIXME: updating parent. may need to call this conditionally - when there is a change in fg spec        
            updateParent();
        }

        if (recursive) {
            ModuleItem[] items = children();
            if (items == null)
                return;
            for (int i = 0; i < items.length; i++) {
                items[i].setInstanceLevel_Set(set, recursive);
            }
        }
    }

    public void turnOnOffData(int dataId, boolean on, boolean recursive) {
        // take care this instance first
        if (instance != null) {
            if (on)
                instance.enableData(dataId);
            else
                instance.disableData(dataId);
        }

        // take care of children now
        if (recursive) {
            ModuleItem[] items = children();
            if (items != null) {
                for (int i = 0; i < items.length; i++)
                    items[i].turnOnOffData(dataId, on, recursive);
            }
        }
    }

    // If this ModuleItem doesn't have an MBean call this method to get the nearest parent ModuleItem that
    // has MBean. This method will check if there is an MBean up the parent chain.
    public ObjectName getParentMBean() {
        /*
         * if (parent.getInstance() == null)
         * {
         * return PmiRegistry.getServerMBean();
         * }
         * else
         * {
         * ObjectName on = parent.getInstance().getMBeanName();
         * if (on == null)
         * {
         * return parent.getParentMBean ();
         * }
         * return on;
         * }
         */return null;
    }

    // *PmiJmxBridge interface method*
    public boolean isStatisticsProvider() {
        return instance.isJ2EEStatisticProvider();
    }

    // recursively remove reference to child ModuleItem from children ArrayList
    private void _cleanChildren() {
        //bStatsTreeNeedsUpdate = true;
        if (children != null) {
            Iterator values = children.values().iterator();

            while (values.hasNext()) {
                ModuleItem remMI = (ModuleItem) values.next();
                remMI.getInstance().cleanup();

                //_cleanMBean(remMI);
                remMI._cleanChildren();
                remMI = null;
            }

            children.clear();
        }
    }

    /*
     * // d180336: remove MBean->ModuleItem map unregistering PMI module
     * private void _cleanMBean(ModuleItem mi)
     * {
     * PmiModule pmimodule = mi.getInstance();
     * MBeanStatDescriptor mbean;
     * 
     * if (pmimodule != null)
     * {
     * // remove entry from mBeanToModuleMap
     * mbean = pmimodule.getMBeanStatDescriptor();
     * if (mbean != null)
     * PmiRegistry.removeMBeanToModuleMap(mbean);
     * }
     * else
     * {
     * return;
     * }
     * 
     * // if custom pmi module AND if the module has CustomStats MBean associated then deactivate it
     * if (pmimodule.isCustomModule())
     * {
     * if (mbean != null)
     * {
     * String type = mbean.getObjectName().getKeyProperty("type");
     * if (type.equals (StatsFactoryUtil.DEFAULT_MBEAN) && mbean.getStatDescriptor() == null)
     * {
     * try
     * {
     * if(tc.isDebugEnabled())
     * Tr.debug(tc, "PMI0201I", pmimodule.getName());
     * 
     * AdminServiceFactory.getMBeanFactory().deactivateMBean (mbean.getObjectName());
     * }
     * catch (Exception e)
     * {
     * Tr.warning(tc, "PMI0106W", e.getMessage());
     * }
     * }
     * }
     * }
     * }
     */

} // end of ModuleItem class
