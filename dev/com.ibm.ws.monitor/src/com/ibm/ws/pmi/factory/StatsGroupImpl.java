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

package com.ibm.ws.pmi.factory;

import java.util.ArrayList;

import javax.management.ObjectName;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.pmi.PerfModules;
import com.ibm.websphere.pmi.PmiConstants;
import com.ibm.websphere.pmi.PmiModuleConfig;
import com.ibm.websphere.pmi.stat.MBeanStatDescriptor;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.pmi.server.ModuleAggregate;
import com.ibm.ws.pmi.server.ModuleItem;
import com.ibm.ws.pmi.server.PmiRegistry;
import com.ibm.ws.pmi.stat.StatsImpl;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;
import com.ibm.wsspi.pmi.factory.StatsGroup;
import com.ibm.wsspi.pmi.factory.StatsInstance;

public class StatsGroupImpl extends ModuleAggregate implements StatsGroup {
    private static final long serialVersionUID = 7503693980854338755L;
    private static final TraceNLS nls = TraceNLS.getTraceNLS(StatsGroupImpl.class, PmiConstants.MSG_BUNDLE);
    private static final TraceComponent tc = Tr.register(StatsGroupImpl.class, null, PmiRegistry.MSG_BUNDLE);

    private final String[] _path;

    // custom instance name (display name. not the module name)
    private final String _cGroupName;
    private boolean _bHasMBean;
    private final StringBuffer _subModuleID = new StringBuffer();
    private ArrayList subModuleList;

    protected StatsGroupImpl(String name, PmiModuleConfig config) {
        super(config.getUID(), false);
        this._cGroupName = name;
        this._path = new String[] { this._cGroupName };
    }

    protected StatsGroupImpl(String name, PmiModuleConfig config, String[] path) {
        super(config.getUID(), false);
        this._cGroupName = name;
        this._path = path;
    }

    // ----------------- Private methods -----------------
    private void _register(ObjectName userProvidedMBeanObjectName, boolean bCreateDefaultMBean) throws StatsFactoryException {
        MBeanStatDescriptor msd = StatsFactoryUtil.createMBean(this, _cGroupName, userProvidedMBeanObjectName, bCreateDefaultMBean);
        if (msd == null) {
            _bHasMBean = false;
        } else {
            _bHasMBean = true;
        }

        StatsFactoryUtil.registerModule(this, msd);
    }

    // ----------------- FACTORY methods -----------------
    // create group under root
    public static StatsGroup createGroup(String name, String configXmlPath, ObjectName userProvidedMBeanObjectName, boolean bCreateDefaultMBean, StatisticActions actionLsnr)
                    throws StatsFactoryException {
        PmiModuleConfig cfg = PerfModules.getConfigFromXMLFile(configXmlPath, actionLsnr.getCurrentBundle());
        if (cfg == null) {
            //Tr.warning (tc, "PMI0102W", configXmlPath);
            throw new StatsFactoryException(nls.getFormattedMessage("PMI0102W", new Object[] { configXmlPath }, "Unable to read custom PMI module configuration: {0}") + ". "
                                            + PerfModules.getParseExceptionMsg());
        }

        StatsGroupImpl group = new StatsGroupImpl(name, cfg);
        group._register(userProvidedMBeanObjectName, bCreateDefaultMBean);
        //System.out.println("stocksStatisticsGroup"+ group.getName()+" is successful: ModuleCOnfig for this is " + group.getModuleConfig());
        return group;
    }

    // create group under group
    public static StatsGroup createGroup(String name, StatsGroup igroup, String configXmlPath, ObjectName userProvidedMBeanObjectName, boolean bCreateDefaultMBean,
                                         StatisticActions actionLsnr)
                    throws StatsFactoryException {
        StatsGroupImpl parentGroup = (StatsGroupImpl) igroup;

        PmiModuleConfig cfg = PerfModules.getConfigFromXMLFile(configXmlPath, actionLsnr.getCurrentBundle());
        if (cfg == null) {

            Tr.warning(tc, "PMI0102W", configXmlPath);
            throw new StatsFactoryException(nls.getFormattedMessage("PMI0102W", new Object[] { configXmlPath }, "Unable to read custom PMI module configuration: {0}") + ". "
                                            + PerfModules.getParseExceptionMsg());
        }

        StatsFactoryUtil.checkDataIDUniqueness(parentGroup, cfg);

        // construct path
        String[] parentPath = parentGroup.getPath();
        String[] path = new String[parentPath.length + 1];
        for (int i = 0; i < parentPath.length; i++) {
            path[i] = parentPath[i];
        }
        path[parentPath.length] = name;

        // create ModuleAggregate
        StatsGroupImpl group = new StatsGroupImpl(name, cfg, path);
        group._register(userProvidedMBeanObjectName, bCreateDefaultMBean);

        return group;
    }

    // create group under instance        
    public static StatsGroup createGroup(String name, StatsInstance iInstance, String subModuleConfigXMLFile, ObjectName userProvidedMBeanObjectName, boolean bCreateDefaultMBean,
                                         StatisticActions actionLsnr)
                    throws StatsFactoryException {
        StatsInstanceImpl parentInstance = (StatsInstanceImpl) iInstance;

        PmiModuleConfig cfg = PerfModules.getConfigFromXMLFile(subModuleConfigXMLFile, actionLsnr.getCurrentBundle());
        if (cfg == null) {

            Tr.warning(tc, "PMI0102W", subModuleConfigXMLFile);
            throw new StatsFactoryException(nls.getFormattedMessage("PMI0102W", new Object[] { subModuleConfigXMLFile }, "Unable to read custom PMI module configuration: {0}")
                                            + ". " + PerfModules.getParseExceptionMsg());
        }

        StatsFactoryUtil.checkDataIDUniqueness(parentInstance, cfg);

        // construct path
        String[] parentPath = parentInstance.getPath();
        String[] path = new String[parentPath.length + 1];
        for (int i = 0; i < parentPath.length; i++) {
            path[i] = parentPath[i];
        }
        path[parentPath.length] = name;

        // create ModuleAggregate
        StatsGroupImpl group = new StatsGroupImpl(name, cfg, path);
        group._register(userProvidedMBeanObjectName, bCreateDefaultMBean);

        return group;
    }

    // getModuleID is defined in ModuleAggregate.java
    // Do not redefine here

    @Override
    public int getDefaultLevel() {
        return PmiConstants.LEVEL_NONE;
    }

    @Override
    public String[] getPath() {
        return _path;
    }

    @Override
    public String getName() {
        return _cGroupName;
    }

    @Override
    public StatsImpl getStats(ArrayList dataMembers, ArrayList colMembers) {
        StatsImpl s = new StatsImpl(getModuleID() + _subModuleID.toString(), getName(), type, currentLevel, dataMembers, colMembers);
        //s.setStatsType (getModuleID());
        return s;
    }

    @Override
    public boolean isCustomModule() {
        return true;
    }

    public ObjectName getMBean() {
        if (_bHasMBean) {
            // call PmiAbstractModule
            return getMBeanName();
        } else {
            return null;
        }
    }

    public void setMBean(ObjectName mBeanName) {
        ModuleItem mItem = PmiRegistry.findModuleItem(_path);
        if (mItem != null) {
            StatsFactoryUtil.setMBeanMapping(mItem, mBeanName);
        } else {
            Tr.warning(tc, "PMI0105W", _path[_path.length - 1]);
        }
    }

    // called by ModuleAggregate when a instance/group of different type is added
    @Override
    public void addModuleID(String id) {
        if (subModuleList == null) {
            subModuleList = new ArrayList(2);
        }

        for (int i = 0; i < subModuleList.size(); i++) {
            if (id.compareTo((String) subModuleList.get(i)) == 0) {
                return;
            }
        }

        subModuleList.add(id);
        _subModuleID.append(",").append(id);
    }

    @Override
    protected String[] getCustomSubModuleList() {
        if (subModuleList == null)
            return null;
        else {
            String[] list = new String[subModuleList.size()];
            for (int i = 0; i < subModuleList.size(); i++) {
                list[i] = (String) subModuleList.get(i);
            }
            return list;
        }
    }

    @Override
    public String getWCCMStatsType() {
        return getModuleID() + _subModuleID.toString();
    }

    @Override
    public int[] getStatisticIDBySet(String setID) {
        ArrayList list = new ArrayList();
        int[] set = PerfModules.getConfig(getModuleID()).listStatisticsBySet(setID);
        for (int i = 0; i < set.length; i++)
            list.add(new Integer(set[i]));

        if (subModuleList != null) {
            for (int i = 0; i < subModuleList.size(); i++) {
                set = PerfModules.getConfig((String) subModuleList.get(i)).listStatisticsBySet(setID);
                for (int k = 0; k < set.length; k++)
                    list.add(new Integer(set[k]));
            }
        }

        int[] statSet = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            statSet[i] = ((Integer) list.get(i)).intValue();
        }

        return statSet;
    }
}
