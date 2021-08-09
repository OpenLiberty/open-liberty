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
package com.ibm.websphere.pmi;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import com.ibm.websphere.pmi.stat.StatConstants;

/**
 * This class contains the information in a XML config file that contains the static info
 * for a Pmi module. A module could include a number of performance data and subModule(s).
 * This class holds a hash table containing PmiDataInfo objects and data ID is the key
 *
 * @ibm-api
 */

/**
 * This class contains configuration information for a Stats object (PMI module).
 */
public class PmiModuleConfig implements java.io.Serializable, PmiConstants {
    private static final long serialVersionUID = 9139791110927568058L;

    // fully qualified module name, eg, com.ibm.websphere.pmi.beanModule
    private final String UID;
    private String description;

    private String mbeanType = null;

    // hashtable to hold PmiDataInfo objects
    private final HashMap perfData;

    // added for custom PMI
    private String statsNLSFile;
    private int[] dependList = null;
    private boolean hasSubMod = false;

    /**
     * PMI data are organized in modules (Stats). Each module has a unique UID.
     * This class contains all the PmiDataInfo (Statistics) for the module.
     */
    public PmiModuleConfig(String UID) {
        //System.out.println("Creating New PmiModuleConfig for UID="+UID);
        this.UID = UID;
        perfData = new HashMap();
    }

    /**
     * Sets the module description (WebSphere internal use only).
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the mapping MBean type (WebSphere internal use only).
     */
    public void setMbeanType(String mbeanType) {
        this.mbeanType = mbeanType;
    }

    /**
     * Sets the resource bundle to translate the Stats name, decription, and unit (WebSphere internal use only).
     */
    public void setResourceBundle(String nlsFile) {
        this.statsNLSFile = nlsFile;
    }

    /**
     * Add PmiDataInfo for a statistic (WebSphere internal use only)
     */
    public synchronized void addData(PmiDataInfo info) {
        if (info != null)
            perfData.put(new Integer(info.getId()), info);
        //System.out.println("&&&&&&& Adding "+info.getName()+" to "+this.getShortName());
        //System.out.println("perfData.values() = "+perfData.values());
    }

    /**
     * Remove PmiDataInfo for a statistic (WebSphere internal use only)
     */
    public synchronized void removeData(PmiDataInfo info) {
        if (info != null && perfData != null)
            perfData.remove(new Integer(info.getId()));
    }

    /**
     * Returns the resource bundle name.
     */
    public String getResourceBundle() {
        return statsNLSFile;
    }

    /**
     * Returns the number of statistics in this module
     */
    public int getNumData() {
        return perfData.size();
    }

    /**
     * Returns the UID of this module (Stats).
     */
    public String getUID() {
        return UID;
    }

    /**
     * Returns the Stats name - eg, beanModule (WebSphere internal use only)
     */
    public String getShortName() {
        int index = UID.lastIndexOf(".");
        return UID.substring(index + 1);
    }

    /**
     * Returns the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the mapping MBean type.
     */
    public String getMbeanType() {
        return mbeanType;
    }

    /**
     * Returns the data ID for a Statistic name in this module (Stats)
     */
    public int getDataId(String name) {
        Iterator allData = perfData.values().iterator();
        while (allData.hasNext()) {
            PmiDataInfo dataInfo = (PmiDataInfo) allData.next();
            if (dataInfo.getName().equalsIgnoreCase(name))
                return dataInfo.getId();
        }
        return UNKNOWN_ID;
    }

    /**
     * Returns the PmiDataInfo for a data ID in this module (Stats)
     */
    public PmiDataInfo getDataInfo(int dataId) {
        return (PmiDataInfo) perfData.get(new Integer(dataId));
    }

    /**
     * Returns all the PmiDataInfo in the submodule.
     * If submoduleName is null, return all the PmiDataInfo in the module.
     */
    public PmiDataInfo[] submoduleMembers(String submoduleName) {
        return submoduleMembers(submoduleName, LEVEL_MAX);
    }

    /**
     * Returns an array of PmiDataInfo for the given submoduleName and level.
     */
    public PmiDataInfo[] submoduleMembers(String submoduleName, int level) {
        if (submoduleName == null)
            return listLevelData(level);

        ArrayList returnData = new ArrayList();

        // special case for category
        boolean inCategory = false;
        if (submoduleName.startsWith("ejb."))
            inCategory = true;

        Iterator allData = perfData.values().iterator();
        while (allData.hasNext()) {
            PmiDataInfo info = (PmiDataInfo) allData.next();
            if (inCategory) { // submoduleName is actually the category name for entity/session/mdb
                if (info.getCategory().equals("all") || isInCategory(submoduleName, info.getCategory()))
                    returnData.add(info);
            } else if (info.getSubmoduleName() != null &&
                       info.getSubmoduleName().equals(submoduleName) && info.getLevel() <= level) {
                returnData.add(info);
            }
        }

        PmiDataInfo[] ret = new PmiDataInfo[returnData.size()];
        for (int i = 0; i < ret.length; i++)
            ret[i] = (PmiDataInfo) returnData.get(i);
        return ret;
    }

    /**
     * Returns the submodule members.
     */
    public PmiDataInfo[] listData(String submoduleName) {
        if (submoduleName == null)
            return listAllData();
        else
            return submoduleMembers(submoduleName);
    }

    /**
     * Returns the PmiDataInfo for all the statistics in the module.
     */
    public PmiDataInfo[] listAllData() {
        return listLevelData(LEVEL_MAX);

    }

    /**
     * Returns the statistic with level equal to or lower than 'level'
     */
    public PmiDataInfo[] listLevelData(int level) {
        ArrayList levelData = new ArrayList();

        Iterator allData = perfData.values().iterator();
        while (allData.hasNext()) {
            PmiDataInfo dataInfo = (PmiDataInfo) allData.next();
            if (dataInfo.getLevel() <= level) {
                levelData.add(dataInfo);
            }

        }

        // get the array
        PmiDataInfo[] ret = new PmiDataInfo[levelData.size()];
        for (int i = 0; i < ret.length; i++)
            ret[i] = (PmiDataInfo) levelData.get(i);
        return ret;
    }

    /**
     * Returns the statistic with level equal to 'level'
     */
    public PmiDataInfo[] listMyLevelData(int level) {
        ArrayList levelData = new ArrayList();
        Iterator allData = perfData.values().iterator();
        while (allData.hasNext()) {
            PmiDataInfo dataInfo = (PmiDataInfo) allData.next();
            if (dataInfo.getLevel() == level) {
                levelData.add(dataInfo);
            }

        }
        return (PmiDataInfo[]) levelData.toArray();
    }

    /** @deprecated No replacement */
    @Deprecated
    public void print(PrintWriter pw) {
        pw.println("UID:" + UID);
        pw.println("description:" + description);
        PmiDataInfo[] data = this.listAllData();
        for (int i = 0; i < data.length; i++) {
            data[i].print(pw);
        }
    }

    /** Returns String representation of this object */
    @Override
    public String toString() {
        StringBuffer b = new StringBuffer("Stats type=").append(UID);
        b.append(", Description=").append(description).append("\n");

        PmiDataInfo[] data = this.listAllData();
        for (int i = 0; i < data.length; i++) {
            b.append("\n").append(data[i].toString()).append("\n");
        }

        return b.toString();
    }

    // PRIVATE method
    private boolean isInCategory(String myCategory, String categoryList) {
        StringTokenizer st = new StringTokenizer(categoryList, "+");
        while (st.hasMoreTokens()) {
            if (myCategory.equals(st.nextToken()))
                return true;
        }
        return false;
    }

    /** Returns String representation of this object */
    public int[] listStatisticsWithDependents() {
        if (dependList == null) {
            ArrayList list = new ArrayList();
            Iterator allData = perfData.values().iterator();
            while (allData.hasNext()) {
                PmiDataInfo dataInfo = (PmiDataInfo) allData.next();
                if (dataInfo.getDependency() != null) {
                    list.add(new Integer(dataInfo.getId()));
                }
            }

            dependList = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                dependList[i] = ((Integer) list.get(i)).intValue();
            }
        }
        return dependList;
    }

    /**
     * Return the list of statistic IDs that are in the given pre-defined statistic sets.
     * Statistic sets are defined in {@link com.ibm.websphere.pmi.stat.StatConstants}
     */
    public int[] listStatisticsBySet(String statisticSet) {
        //System.out.println("&&&&& calling listStatisticsBySet "+statisticSet);

        if (statisticSet.equals(StatConstants.STATISTIC_SET_NONE) ||
            statisticSet.equals(StatConstants.STATISTIC_SET_CUSTOM))
            return new int[0];

        int k = 0;
        if (statisticSet.equals(StatConstants.STATISTIC_SET_BASIC))
            k = 1;
        else if (statisticSet.equals(StatConstants.STATISTIC_SET_EXTENDED))
            k = 2;
        else if (statisticSet.equals(StatConstants.STATISTIC_SET_ALL))
            k = 3;
        //System.out.println("perfData.values() for  = "+this.getShortName());
        //System.out.println("perfData.values() = "+perfData.values());
        ArrayList list = new ArrayList(3);
        Iterator allData = perfData.values().iterator();

        //System.out.println("&&& alldata ="+allData);

        while (allData.hasNext()) {
            PmiDataInfo dataInfo = (PmiDataInfo) allData.next();
            //System.out.println("&&& dataInfo ="+dataInfo.getName());
            String s = dataInfo.getStatisticSet();
            if (s.equals(StatConstants.STATISTIC_SET_BASIC)) {
                if (k > 0)
                    list.add(new Integer(dataInfo.getId()));
            } else if (s.equals(StatConstants.STATISTIC_SET_EXTENDED)) {
                if (k > 1)
                    list.add(new Integer(dataInfo.getId()));
            } else if (s.equals(StatConstants.STATISTIC_SET_ALL)) {
                if (k > 2)
                    list.add(new Integer(dataInfo.getId()));
            }
        }

        int[] statSet = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            statSet[i] = ((Integer) list.get(i)).intValue();
        }

        return statSet;
    }

    private boolean hasSubModInit = false;

    /**
     * Returns true if there is a sub-module defined (applicable only to 5.x)
     */
    public boolean hasSubModule() {
        if (!hasSubModInit) {
            hasSubModInit = true;
            if (perfData != null) {
                Iterator allData = perfData.values().iterator();
                while (allData.hasNext()) {
                    PmiDataInfo dataInfo = (PmiDataInfo) allData.next();
                    if (dataInfo.getSubmoduleName() != null) {
                        hasSubMod = true;
                        break;
                    }
                }
            }
        }
        return hasSubMod;
    }

    /**
     * Creates a copy of this object
     * 
     * @return copy of this object
     */
    public PmiModuleConfig copy() {
        PmiModuleConfig r = new PmiModuleConfig(UID);

        // Only description is translatable
        if (description != null)
            r.description = new String(description);

        r.mbeanType = mbeanType;
        r.statsNLSFile = statsNLSFile;
        r.hasSubMod = hasSubMod;
        r.dependList = dependList;

        if (perfData != null) {
            Iterator allData = perfData.values().iterator();
            while (allData.hasNext()) {
                PmiDataInfo dataInfo = (PmiDataInfo) allData.next();
                r.perfData.put(new Integer(dataInfo.getId()), dataInfo.copy());
            }
        }

        return r;
    }
}