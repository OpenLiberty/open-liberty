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
package com.ibm.ws.pmi.server;

import com.ibm.websphere.pmi.*;
import com.ibm.websphere.pmi.server.*;

//import com.ibm.ws.pmi.server.data.*;

public class ModuleAggregate extends PmiAbstractModule implements PmiModuleAggregate {
    private static final long serialVersionUID = -8176996451666384014L;
    private String moduleID = null;
    private final int defaultLevel = LEVEL_NONE;
    //private ArrayList instanceList = new ArrayList();
    private String[] path = null;

    /*
     * Constructor: for module level aggregate data
     */
    public ModuleAggregate(String name) {
        //this(name, "MODULE_AGGREGATE");
        this(name, null);
    }

    /*
     * workaround contructor for custom pmi
     */
    public ModuleAggregate(String name, boolean bRegister) {
        super(name, null);
        this.moduleID = name;
        //this.categoryName = categoryName;
        if (categoryName == null)
            type = TYPE_MODULE;
        else
            type = TYPE_CATEGORY;

        aggregateModule = true;

        if (bRegister) {
            registerModule(this);
        }
    }

    /**
     * Constructor:
     */
    public ModuleAggregate(String name, String categoryName) {
        super(name, null);
        this.moduleID = name;
        this.categoryName = categoryName;
        if (categoryName == null)
            type = TYPE_MODULE;
        else
            type = TYPE_CATEGORY;

        aggregateModule = true;
        registerModule(this);
    }

    /**
     * Constructor: for submodule level aggregate data
     */
    public ModuleAggregate(String name, String instanceName, String submoduleName) {
        this(name, null, instanceName, submoduleName);
    }

    public ModuleAggregate(String name, String categoryName, String instanceName, String submoduleName) {
        super(name, instanceName);
        this.moduleID = name;
        this.categoryName = categoryName;
        this.submoduleName = submoduleName;
        type = TYPE_SUBMODULE;
        aggregateModule = true;
        registerModule(this);
    }

    // the constructor to handle common case
    // type should be TYPE_SUBMODULE or TYPE_CATEGORY
    // name should be submodule name or category name
    // path must have at least two items
    public ModuleAggregate(String[] path, String name, int type) {
        super(path[0], path[1]);
        this.path = path;
        this.type = type;
        this.moduleID = path[0];

        if (type == TYPE_INSTANCE)
            this.instanceName = name;
        else if (type == TYPE_SUBMODULE)
            this.submoduleName = name;
        else if (type == TYPE_CATEGORY)
            this.categoryName = name;
        aggregateModule = true;
        registerModule(this);
    }

    public String getModuleID() {
        return moduleID;
    }

    public int getDefaultLevel() {
        return defaultLevel;
    }

    // overwrite getPath method
    public String[] getPath() {
        if (path != null)
            return path;
        else if (instanceName == null && type == TYPE_SUBMODULE)
            return new String[] { moduleID, submoduleName };
        else
            return super.getPath();
    }

    /**
     * Add SpdData to the aggregate data with same dataId.
     * The list will be used to check is an aggregate data is enabled by checking its
     * children's levels.
     * FIXME: do we add non-immediate children instance?
     */
    public synchronized void add(PmiModule instance) {
        if (instance == null)
            return;

        // no need to add instance to instanceList - to be removed.
        /*
         * if(!instanceList.contains(instance)) {
         * instanceList.add(instance);
         * }
         */

        // removing add(spddata[]) - customPmi
        //add(instance.listData());

        PmiModuleConfig instanceModCfg = null;
        boolean bSameSubMod = true;
        // Check if instance and aggregate are same type
        if (!instance.getModuleID().equals(getModuleID())) {
            bSameSubMod = false;
            addModuleID(instance.getModuleID());
            instanceModCfg = PerfModules.getConfig(instance.getModuleID());
            // can't aggregate data from different modules (different xml files)
            // customPMI limitation - fixed in VELA
            //return;
        } else {
            instanceModCfg = moduleConfig;
        }

        SpdData[] dataList = instance.listData();
        if (dataList == null || instanceModCfg == null)
            return;

        for (int i = 0; i < dataList.length; i++) {
            int dataId = dataList[i].getId();

            // Always creating aggregate data in ModuleAggregate            
            // fg: check if dataId is enabled in the parent before adding           
            //if (bSameSubMod && !isEnabled (dataId))
            //    continue;

            // if different sub module then create the sub-module aggregate data
            // in parent even if its not enabled.

            PmiDataInfo info = instanceModCfg.getDataInfo(dataId);
            if (info == null) {
                // wrong data id, ignore it. - This should not happen
                continue;
            } else if (!info.isAggregatable())
                continue;

            // Check if aggregateData is already available. If not create it.
            SpdGroup aggregateData = (SpdGroup) dataTable.get(new Integer(dataId));
            if (aggregateData == null) {
                aggregateData = (SpdGroup) createAggregateData(info);
                if (aggregateData != null)
                    putToTable((SpdData) aggregateData);

                //if (!bSameSubMod)
                //{
                // enable of parent level is
                if (currentLevel == LEVEL_FINEGRAIN) {
                    //if( !isEnabledInConfig (dataId) )             
                    if (!isEnabled(dataId))
                        ((SpdData) aggregateData).disable();
                } else if (info.getLevel() > currentLevel)
                    ((SpdData) aggregateData).disable();
                //}
                // FINE GRAINED
                /*
                 * if(info.getLevel() > currentLevel)
                 * ((SpdData)aggregateData).disable();
                 */
            }
            aggregateData.add(dataList[i]);
        }
    }

    public synchronized void remove(PmiModule instance) {
        if (instance == null)
            return;

        remove(instance.listData());
    }

    /*
     * public synchronized void add(SpdData[] dataList)
     * {
     * if(dataList == null || moduleConfig == null)
     * return;
     * 
     * for(int i = 0; i < dataList.length; i++)
     * {
     * int dataId = dataList[i].getId();
     * PmiDataInfo info = moduleConfig.getDataInfo(dataId);
     * if(info == null)
     * {
     * // wrong data id, ignore it. - This should not happen
     * continue;
     * }
     * else if(!info.isAggregatable())
     * continue;
     * 
     * SpdGroup aggregateData = (SpdGroup)dataTable.get(new Integer(dataId));
     * if(aggregateData == null)
     * {
     * aggregateData = (SpdGroup)createAggregateData(info);
     * if(aggregateData != null)
     * putToTable((SpdData)aggregateData);
     * if(info.getLevel() > currentLevel)
     * ((SpdData)aggregateData).disable();
     * }
     * aggregateData.add(dataList[i]);
     * }
     * }
     */

    /*
     * Remove SpdData from the aggregate data
     */
    public synchronized void remove(SpdData[] dataList) {
        if (dataList == null)
            return;

        for (int i = 0; i < dataList.length; i++) {
            SpdGroup aggregateData = (SpdGroup) dataTable.get(new Integer(dataList[i].getId()));
            if (aggregateData == null)
                continue;

            aggregateData.remove(dataList[i]);
        }

        return;
    }

    /*
     * Remove SpdData from the aggregate data
     */
    public synchronized boolean remove(SpdData data) {
        if (data == null)
            return false;

        SpdGroup aggregateData = (SpdGroup) dataTable.get(new Integer(data.getId()));
        if (aggregateData == null)
            return false;
        return aggregateData.remove(data);
    }

    /*
     * PMI J2EE decoupling
     * // Special handling for EJBModule. The stats for EJBModule (EJBStats) is in ModuleAggregate
     * public StatsImpl getJSR77Stats (ModuleItem mItem)
     * {
     * if (moduleID.equals (PmiConstants.BEAN_MODULE))
     * {
     * return new EJBStatsImpl(name, type, currentLevel, listStatistics(), null);
     * }
     * else
     * {
     * return mItem.getStats(false);
     * }
     * }
     */
    public void addModuleID(String id) {
        // Only for custom pmi. Implemented in StatsGroupImpl
    }

    private final static String BeanModuleWccmType = BEAN_MODULE + "#";

    public String getWCCMStatsType() {
        if (moduleID.equals(PmiConstants.BEAN_MODULE)) {
            // for EJBs the type will be beanModule#ejb.entity, beanModule#ejb.stateless, etc.
            // this helps to list just the ejb type specific counters instead of all 
            String[] p = getPath();
            if (p != null && p.length == 3)
                return BEAN_MODULE + "#" + p[2];
            else
                return BeanModuleWccmType;
        } else if (moduleID.equals(PmiConstants.WLM_MODULE)) {
            // WLM doesn't have any counters at the top level.
            // only children client and server have counters
            String[] p = getPath();
            if (p != null && p.length == 1)
                return PmiConstants.WLM_MODULE + "#";
            else
                return super.getWCCMStatsType();
        } else
            return super.getWCCMStatsType();
    }

    // It seems the 'check config approach' is not working properly.
    // For now, I'm reverting the check for enabled back to the original approach    
    /*
     * private boolean isEnabledInConfig( int ordinal )
     * {
     * boolean retval = false;
     * 
     * int[][] fgSpec = PmiConfigManager.getSpec(getPath());
     * int[] search;
     * if (fgSpec != null)
     * {
     * if (fgSpec[0] != null) // 5.0 spec
     * search = fgSpec[0];
     * else
     * search = fgSpec[1];
     * 
     * if( search != null )
     * {
     * for( int i=0; i<search.length; i++ )
     * {
     * if( ordinal == search[i] )
     * {
     * retval = true;
     * break;
     * }
     * }
     * }
     * }
     * 
     * return retval;
     * }
     */
}
