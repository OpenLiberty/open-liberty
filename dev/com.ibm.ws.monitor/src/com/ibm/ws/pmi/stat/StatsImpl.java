/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.pmi.stat;

import java.util.ArrayList;
import java.util.Locale;

import com.ibm.websphere.pmi.PmiConstants;
import com.ibm.websphere.pmi.PmiModuleConfig;
import com.ibm.websphere.pmi.stat.WSStatistic;
import com.ibm.websphere.pmi.stat.WSStats;
import com.ibm.ws.pmi.server.PmiUtil;
import com.ibm.ws.pmi.wire.WpdCollection;
import com.ibm.ws.pmi.wire.WpdData;
import com.ibm.wsspi.pmi.stat.SPIStats;

/**
 * Implements com.ibm.websphere.pmi.stat.Stats interface.
 * It also implements WpdCollection for backward compatibility.
 */
public class StatsImpl implements SPIStats, WpdCollection, PmiConstants, java.io.Serializable {
    private static final long serialVersionUID = -5812710047173154854L;

    // a self-identifier
    // MBeanObjectName, StatDescriptor, 
    // NodeName, ServerName 
    // However, nodeName and serverName have to attached from cell manager
    // PerfMBeanDescriptor pmd;

    // this module name is used to identify the static config (there can be many Stats instance
    // with the same config. This name can be used to get the PmiModuleConfig from the server.
    // this name doesn't help to locate the absolute path of the xml file. so this name
    // may not be used to get the PmiModuleConfig without talking to server (as required by jsr77)
    protected String statsType; //moduleName: added for customPmi
    protected String name;
    protected int type;
    protected int instrumentationLevel = LEVEL_UNDEFINED;

    protected ArrayList dataMembers;
    protected ArrayList subCollections;
    protected long time;

    //static variables
    transient private static boolean bTextInfoEnabled = true;
    transient private static boolean bTextInfoTranslationEnabled = true;
    transient private static Locale _locale = null;

    public StatsImpl(String name, int type) {
        this(name, type, LEVEL_UNDEFINED, null, null);
    }

    public StatsImpl(String name, int type, int level) {
        this(name, type, level, null, null);
    }

    // constructor: 
    // Note: collection hierarchy: node->server->module->collections
    public StatsImpl(String name, int type, int level, ArrayList dataMembers, ArrayList subCollections) {
        time = PmiUtil.currentTime();
        if (name == null)
            name = "Undefined";
        this.name = name;
        if (type == TYPE_NODE || type == TYPE_SERVER || type == TYPE_MODULE)
            this.type = type;
        else
            this.type = TYPE_COLLECTION;
        this.instrumentationLevel = level;
        this.dataMembers = dataMembers;
        this.subCollections = subCollections;
    }

    // constructor taking statsType as an arg.
    // for modules that dont have specific XXXStatsImpl use this constructor
    // for predefined stats override getStatsType in XXXStatsImpl returning a final string
    // this will avoid transferring the statsType over wire all the time        
    public StatsImpl(String statsType, String name, int type, int level, ArrayList dataMembers, ArrayList subCollections) {
        this(name, type, level, dataMembers, subCollections);
        this.statsType = statsType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int numStatistics() {
        if (dataMembers == null)
            return 0;
        else
            return dataMembers.size();
    }

    // returns the moduleID. used to get static config info and bind to the stats
    @Override
    public String getStatsType() {
        return statsType;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setStatsType(String modName) {
        this.statsType = modName;
    }

    @Override
    public int getType() {
        return type;
    }

    /**
     * Set query time - the time when the client request comes to server
     */
    @Override
    public long getTime() {
        return time;
    }

    /**
     * Set query time - the time when the client request comes to server
     */
    public synchronized void setTime(long t) {
        time = t;
    }

    /**
     * get the instrumentation level
     */
    @Override
    public int getLevel() {
        return instrumentationLevel;
    }

    /**
     * set the instrumentation level
     */
    @Override
    public void setLevel(int level) {
        instrumentationLevel = level;
    }

    public static void setEnableTextInfo(boolean flag) {
        bTextInfoEnabled = flag;
    }

    public static boolean getEnableTextInfo() {
        return bTextInfoEnabled;
    }

    public static void setEnableNLS(boolean flag, Locale locale) {
        _locale = locale;
        bTextInfoTranslationEnabled = flag;
    }

    public static boolean getEnableNLS() {
        return bTextInfoTranslationEnabled;
    }

    public static Locale getNLSLocale() {
        return _locale;
    }

    /**
     * set data members
     */
    @Override
    public void setStatistics(ArrayList dataMembers) {
        this.dataMembers = dataMembers;
    }

    /**
     * set sub-stats
     */
    @Override
    public void setSubStats(ArrayList subCollections) {
        this.subCollections = subCollections;
    }

    /**
     * get Statistic by data id
     */
    @Override
    public WSStatistic getStatistic(int dataId) {
        ArrayList members = copyStatistics();
        if (members == null || members.size() <= 0)
            return null;

        int sz = members.size();
        for (int i = 0; i < sz; i++) {
            StatisticImpl data = (StatisticImpl) members.get(i);
            if (data != null && data.getId() == dataId) {
                return data;
            }
        }
        return null;
    }

    @Override
    public WSStatistic getStatistic(String name) {
        ArrayList members = copyStatistics();
        if (members == null)
            return null;

        int sz = members.size();
        for (int i = 0; i < sz; i++) {
            StatisticImpl statistic = (StatisticImpl) members.get(i);
            if (statistic != null && statistic.getName().equals(name))
                return statistic;
        }
        return null;
    }

    @Override
    public WSStatistic[] getStatistics() {
        ArrayList members = copyStatistics();
        if (members == null || members.size() == 0)
            return new StatisticImpl[0];

        StatisticImpl[] ret = new StatisticImpl[members.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = (StatisticImpl) members.get(i);
        }
        return ret;
    }

    @Override
    public WSStatistic[] listStatistics() {
        return getStatistics();
    }

    @Override
    public String[] listStatisticNames() {
        return getStatisticNames();
    }

    @Override
    public String[] getStatisticNames() {
        ArrayList members = copyStatistics();
        if (members == null)
            return new String[0];

        String[] ret = new String[members.size()];
        for (int i = 0; i < ret.length; i++) {
            if (members.get(i) != null)
                ret[i] = ((StatisticImpl) members.get(i)).getName();
        }
        return ret;
    }

    /*
     * JSR77 proxy: NOT exposed via WSStats
     */
    public StatisticImpl getJ2EEStatistic(String name) {
        ArrayList members = copyStatistics();
        if (members == null)
            return null;

        int sz = members.size();
        for (int i = 0; i < sz; i++) {
            StatisticImpl statistic = (StatisticImpl) members.get(i);
            if (statistic != null && statistic.getName().equals(name))
                return statistic;
        }
        return null;
    }

    /*
     * JSR77 proxy: NOT exposed via WSStats
     */
    public StatisticImpl[] getJ2EEStatistics() {
        ArrayList members = copyStatistics();
        if (members == null || members.size() == 0)
            return new StatisticImpl[0];

        StatisticImpl[] ret = new StatisticImpl[members.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = (StatisticImpl) members.get(i);
        }
        return ret;
    }

    /*
     * JSR77 proxy: NOT exposed via WSStats
     */
    public String[] getJ2EEStatisticNames() {
        ArrayList members = copyStatistics();
        if (members == null)
            return new String[0];

        String[] ret = new String[members.size()];
        for (int i = 0; i < ret.length; i++) {
            if (members.get(i) != null)
                ret[i] = ((StatisticImpl) members.get(i)).getName();
        }
        return ret;
    }

    @Override
    public WSStats getStats(String name) {

        ArrayList collections = copyStats();
        if (collections == null)
            return null;

        StatsImpl stat = null;
        for (int i = 0; i < collections.size(); i++) {
            stat = (StatsImpl) collections.get(i);
            if (stat.getName().equals(name))
                return stat;
        }
        return null;
    }

    @Override
    public WSStats[] getSubStats() {

        ArrayList collections = copyStats();

        if (collections == null || collections.size() == 0)
            return new StatsImpl[0];

        StatsImpl[] ret = new StatsImpl[collections.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = (StatsImpl) collections.get(i);
        }
        return ret;
    }

    @Override
    public WSStats[] listSubStats() {
        return getSubStats();
    }

    public synchronized boolean add(StatisticImpl newMember) {
        if (dataMembers == null) {
            dataMembers = new ArrayList();
        }
        if (newMember == null)
            return false;
        // TODO: do we need to check if the data is already there?
        dataMembers.add(newMember);
        return true;
    }

    public synchronized boolean add(StatsImpl newMember) {
        if (subCollections == null) {
            subCollections = new ArrayList();
        }
        if (newMember == null)
            return false;
        // TODO: do we need to check if the data is already there?
        subCollections.add(newMember);
        return true;
    }

    @Override
    public synchronized boolean remove(int dataId) {
        if ((dataMembers == null) || (dataMembers.size() <= 0))
            return false;
        for (int i = dataMembers.size() - 1; i >= 0; i--) {
            if (((StatisticImpl) dataMembers.get(i)).getId() == dataId) {
                dataMembers.remove(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean remove(String name) {
        if ((subCollections == null) || (subCollections.size() <= 0))
            return false;
        for (int i = subCollections.size() - 1; i >= 0; i--) {
            StatsImpl collection = (StatsImpl) subCollections.get(i);
            if (collection.getName().equals(name)) {
                subCollections.remove(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return toString("");
    }

    @Override
    public String toString(String indent) {
        String myIndent = indent;

        StringBuffer res = new StringBuffer();
        res.append("\n");
        res.append(indent);
        res.append("Stats name=").append(name);
        res.append(", type=").append(getStatsType()); // need call getStatsType to get type from XXXStatsImpl
        res.append("\n{");
        ArrayList members = copyStatistics();
        // write data members first
        if (members != null) {
            for (int i = 0; i < members.size(); i++) {
                StatisticImpl data = (StatisticImpl) members.get(i);
                if (data != null) {
                    res.append("\n").append(myIndent);
                    res.append(data.toString());
                    res.append("\n");
                }
            }
        }

        // write subCollections next

        ArrayList collections = copyStats();

        if (collections != null) {
            for (int i = 0; i < collections.size(); i++) {
                StatsImpl col = (StatsImpl) collections.get(i);
                if (col == null)
                    continue;
                res.append(col.toString(myIndent));
            }
        }

        res.append("}");
        return res.toString();
    }

    @Override
    public void setConfig(PmiModuleConfig config) {
        if (config == null)
            return;

        ArrayList members = copyStatistics();
        if (members != null) {
            for (int i = 0; i < members.size(); i++) {
                StatisticImpl data = (StatisticImpl) members.get(i);
                if (data != null)
                    data.setDataInfo(config);
            }
        }
    }

    @Override
    public void mSetConfig(PmiModuleConfig config) {
        if (config == null)
            return;

        ArrayList members = copyStatistics();
        if (members != null) {
            for (int i = 0; i < members.size(); i++) {
                StatisticImpl data = (StatisticImpl) members.get(i);
                if (data != null)
                    data.mSetDataInfo(config);
            }
        }
    }

    // methods in WpdCollection - they are here for backward compatibility

    @Override
    public boolean add(WpdData newMember) {
        return false;
    }

    @Override
    public boolean add(WpdCollection newMember) {
        if (newMember instanceof StatsImpl)
            return add((StatsImpl) newMember);
        else
            return false;
    }

    @Override
    public String toXML() {
        String typeString = null;

        switch (type) {
            case TYPE_NODE:
                typeString = "NODE";
                break;
            case TYPE_SERVER:
                typeString = "SERVER";
                break;
            case TYPE_MODULE:
                typeString = "MODULE";
                break;
            case TYPE_COLLECTION:
                typeString = "COLLECTION";
                break;
            default:
                typeString = "WRONG_TYPE";
                break;
        }

        StringBuffer res = new StringBuffer();

        res.append("<Stats name=\"");
        res.append(name);
        res.append("\" statType=\"");
        res.append(getStatsType());

        res.append("\" il=\"");
        res.append(instrumentationLevel);

        res.append("\" type=\"");
        res.append(typeString);
        res.append("\">\n");

        ArrayList members = copyStatistics();
        // write data members first
        if (members != null) {

            for (int i = 0; i < members.size(); i++) {
                StatisticImpl data = (StatisticImpl) members.get(i);

                if (data == null) {
                    continue;
                }

                res.append(data.toXML());
                res.append("\n");
            }
        }

        // write subCollections next

        ArrayList collections = copyStats();

        if (collections != null) {

            for (int i = 0; i < collections.size(); i++) {
                StatsImpl col = (StatsImpl) collections.get(i);

                if (col == null) {
                    continue;
                }

                res.append(col.toXML());
            }
        }
        //res.append();
        res.append("</Stats>");
        res.append("\n");

        return res.toString();
    }

    @Override
    public void setDataMembers(ArrayList dataMembers) {
        setStatistics(dataMembers);
    }

    // set subcollections
    @Override
    public void setSubcollections(ArrayList subCollections) {
        setSubStats(subCollections);
    }

    // return data members only
    @Override
    public ArrayList dataMembers() {
        return dataMembers;
    }

    // returns a copy of dataMembers
    public synchronized ArrayList copyStatistics() {
        if (dataMembers != null)
            return new ArrayList(dataMembers);
        else
            return null;
    }

    // return subcollections only
    @Override
    public ArrayList subCollections() {
        return subCollections;
    }

    // returns a copy of subCollections
    public synchronized ArrayList copyStats() {
        if (subCollections != null)
            return new ArrayList(subCollections);
        else
            return null;
    }

    /**
     * This method is defunc'ed
     */
    @Override
    public WpdData getData(int dataId) {
        System.err.println("Warning: this method is defunc'ed - call getStatistic(dataId) instead");
        return null;
    }

    /**
     * This method is defunc'ed
     */
    @Override
    public WpdCollection getSubcollection(String name) {
        System.err.println("Warning: this method is defunc'ed - call getStats(name) instead");
        return null;
    }

    // new methods for update/reset

    /**
     * Update this Stats using the newStats.
     * Note: this Stats and newStats must represent the same Pmi module/submodule.
     * It is caller's responsibility since simply checking name and type here
     * may not be sufficient.
     */
    @Override
    public synchronized void update(WSStats newStats, boolean keepOld, boolean recursiveUpdate) {
        if (newStats == null)
            return;

        StatsImpl newStats1 = (StatsImpl) newStats;
        // check if they are same Stats
        if (!name.equals(newStats1.getName()) || type != newStats1.getType())
            return;

        myupdate(newStats1, keepOld, recursiveUpdate);
    }

    // Assume we have verified newStats is the same PMI module as this Stats
    private synchronized void myupdate(WSStats newStats, boolean keepOld, boolean recursiveUpdate) {
        if (newStats == null)
            return;

        StatsImpl newStats1 = (StatsImpl) newStats;
        // update the level and description of this collection
        this.instrumentationLevel = newStats1.getLevel();

        // update data
        updateMembers(newStats, keepOld);

        // update subcollections
        if (recursiveUpdate)
            updateSubcollection(newStats, keepOld, recursiveUpdate);
    }

    private synchronized void updateMembers(WSStats grp, boolean keepOld) {
        // remove old data
        if (!keepOld) {
            // simply remove everything
            this.dataMembers = null;
        }

        if (grp == null)
            return;

        // add new data
        if (this.dataMembers == null || this.dataMembers.size() == 0) {
            // the simple case, just set dataMembers reference
            this.dataMembers = ((StatsImpl) grp).dataMembers;
        } else {
            WSStatistic[] otherdata = grp.getStatistics();
            if (otherdata == null || otherdata.length == 0)
                return; // no new data

            // PMR 03180,048,866 - START

            /*
             * Perform a double loop here for checking whether
             * a Statistic already exists. Sorting is not enforced
             * in the Statistic lists, therefore the algorithm must
             * not rely on the list to be sorted.
             */
            for (int i = 0; i < otherdata.length; i++) {
                boolean found = false;
                StatisticImpl myData = null;
                for (int n = 0; n < dataMembers.size(); n++) {
                    myData = (StatisticImpl) dataMembers.get(n);
                    if (otherdata[i].getId() == myData.getId()) {
                        found = true;
                        myData.update(otherdata[i]);
                        break;
                    }
                }
                if (!found) {
                    dataMembers.add(otherdata[i]);
                }
            }
        }
    }

    private synchronized void updateSubcollection(WSStats otherStats, boolean keepOld, boolean recursiveUpdate) {
        // remove old data
        if (!keepOld) {
            // simply remove everything
            this.subCollections = null;
        }

        if (otherStats == null)
            return;

        // add new data
        if (this.subCollections == null || this.subCollections.size() == 0) {
            // the simple case, just set subCollections reference
            this.subCollections = ((StatsImpl) otherStats).subCollections;
        } else {
            // go through each subCollection
            WSStats[] newSubStats = otherStats.getSubStats();
            if (newSubStats == null || newSubStats.length == 0)
                return; // no new data

            // add new data now
            StatsImpl thisStats = null;
            for (int i = 0; i < newSubStats.length; i++) {
                boolean found = false;
                int cmp = 0;
                int index = -1;
                for (int j = 0; j < subCollections.size(); j++) {
                    thisStats = (StatsImpl) subCollections.get(j);
                    cmp = newSubStats[i].getName().compareTo(thisStats.getName());
                    if (cmp == 0) { // subcollectio exist, update it
                        found = true;
                        thisStats.update(newSubStats[i], keepOld, recursiveUpdate);
                        break;
                    } else if (cmp < 0) {
                        index = i;
                        break;
                    }
                }
                if (!found && index > 0) { // add to index
                    subCollections.add(index, newSubStats[i]);
                } else if (!found && index == -1) { // add to the last one
                    subCollections.add(newSubStats[i]);
                }
            }
        }
    }

    @Override
    public void resetOnClient(boolean recursive) {
        // reset data members
        if (dataMembers != null) {
            StatisticImpl data = null;
            for (int i = 0; i < dataMembers.size(); i++) {
                data = (StatisticImpl) dataMembers.get(i);
                data.resetOnClient(null);
            }
        }

        if (!recursive)
            return;

        // recursively reset on subcollections
        if (subCollections != null) {
            StatsImpl col = null;
            for (int i = 0; i < subCollections.size(); i++) {
                col = (StatsImpl) subCollections.get(i);
                col.resetOnClient(recursive);
            }
        }
    }

    private synchronized void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        out.defaultWriteObject();
    }

    private synchronized void readObject(java.io.ObjectInputStream in) throws java.lang.ClassNotFoundException, java.io.IOException {
        in.defaultReadObject();
    }
}
