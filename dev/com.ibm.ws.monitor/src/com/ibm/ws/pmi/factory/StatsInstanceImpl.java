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

package com.ibm.ws.pmi.factory;

import java.util.ArrayList;

import javax.management.ObjectName;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.pmi.PerfModules;
import com.ibm.websphere.pmi.PmiConstants;
import com.ibm.websphere.pmi.PmiModuleConfig;
import com.ibm.websphere.pmi.server.PmiAbstractModule;
import com.ibm.websphere.pmi.server.SpdDouble;
import com.ibm.websphere.pmi.server.SpdLoad;
import com.ibm.websphere.pmi.server.SpdLong;
import com.ibm.websphere.pmi.server.SpdStat;
import com.ibm.websphere.pmi.stat.MBeanStatDescriptor;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.monitor.internal.ProbeManagerImpl;
import com.ibm.ws.pmi.server.ModuleItem;
import com.ibm.ws.pmi.server.PmiRegistry;
import com.ibm.ws.pmi.stat.StatisticImpl;
import com.ibm.ws.pmi.stat.StatsImpl;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;
import com.ibm.wsspi.pmi.factory.StatsGroup;
import com.ibm.wsspi.pmi.factory.StatsInstance;
import com.ibm.wsspi.pmi.stat.SPIStatistic;

public class StatsInstanceImpl extends PmiAbstractModule implements StatsInstance {
    private static final long serialVersionUID = 7571693856735644184L;
    private static final TraceNLS nls = TraceNLS.getTraceNLS(StatsInstanceImpl.class, PmiConstants.MSG_BUNDLE);
    private static final TraceComponent tc = Tr.register(StatsInstanceImpl.class, PmiConstants.TR_GROUP, PmiConstants.MSG_BUNDLE);

    private final String[] _path;
    private final String _moduleID;

    private final String _cInstanceName; // custom instance name (display name. not the module name)
    private boolean _bHasMBean;
    private final StatisticImpl[] _stats;
    private int _statCount = 0;

    // ----------------- Constructors -----------------
    protected StatsInstanceImpl(String name, PmiModuleConfig config, StatisticActions sal) {
        super(config, null, sal);
        super.type = TYPE_MODULE;
        //System.out.println("!!!!!!!!!!!! Inside StatsInstanceImpl 1"+name+". Config="+config+".StatisticActions="+sal);
        this._moduleID = config.getUID();
        this._cInstanceName = name;
        this._path = new String[] { this._cInstanceName };
        this._stats = new StatisticImpl[config.getNumData()];
    }

    protected StatsInstanceImpl(String name, PmiModuleConfig config, String[] path, StatisticActions sal) {
        super(config, name, sal);
        super.type = TYPE_INSTANCE;
        //System.out.println("!!!!!!!!!!!! Inside StatsInstanceImpl 2"+name+". Config="+config+".StatisticActions="+sal);
        this._moduleID = config.getUID();
        this._cInstanceName = name;
        this._path = path;
        this._stats = new StatisticImpl[config.getNumData()];
    }

    // ----------------- Private methods -----------------
    private void _register(ObjectName userProvidedMBeanObjectName, boolean bCreateDefaultMBean) throws StatsFactoryException {
        MBeanStatDescriptor msd = StatsFactoryUtil.createMBean(this, _cInstanceName, userProvidedMBeanObjectName, bCreateDefaultMBean);
        if (msd == null) {
            _bHasMBean = false;
        } else {
            _bHasMBean = true;
        }

        StatsFactoryUtil.registerModule(this, msd);
        //Register ModuletoBundle Mapping
        String corrospondingClazz = super.statisticActionLsnr.getClass().getName();
        ProbeManagerImpl.moduleInstanceToBundleMap.put(this, corrospondingClazz);
    }

    // ----------------- FACTORY methods -----------------
    // Create singleton instance
    public static StatsInstanceImpl createInstance(String name, String configXmlPath, ObjectName userProvidedMBeanObjectName, boolean bCreateDefaultMBean,
                                                   StatisticActions actionLsnr)
                    throws StatsFactoryException {

        PmiModuleConfig cfg = PerfModules.getConfigFromXMLFile(configXmlPath, actionLsnr.getCurrentBundle());
        if (cfg == null) {
            Tr.warning(tc, "PMI0102W", configXmlPath);
            throw new StatsFactoryException(nls.getFormattedMessage("PMI0102W", new Object[] { configXmlPath }, "Unable to read custom PMI module configuration: {0}") + ". "
                                            + PerfModules.getParseExceptionMsg());
        }

        StatsInstanceImpl instance = new StatsInstanceImpl(name, cfg, actionLsnr);
        //instance._scListener = scl;        

        instance._register(userProvidedMBeanObjectName, bCreateDefaultMBean);

        return instance;
    }

    // Create instance under group (same type)
    public static StatsInstanceImpl createGroupInstance(String name, StatsGroup igroup, ObjectName userProvidedMBeanObjectName, boolean bCreateDefaultMBean,
                                                        StatisticActions actionLsnr)
                    throws StatsFactoryException {
        StatsGroupImpl group = (StatsGroupImpl) igroup;

        // construct path
        String[] parentPath = group.getPath();
        String[] path = new String[parentPath.length + 1];

        for (int i = 0; i < parentPath.length; i++) {
            path[i] = parentPath[i];
        }
        path[parentPath.length] = name;

        StatsInstanceImpl instance = new StatsInstanceImpl(name, group.getModuleConfig(), path, actionLsnr);
        //instance._scListener = scl;

        instance._register(userProvidedMBeanObjectName, bCreateDefaultMBean);

        return instance;
    }

    // Create instance under group
    public static StatsInstanceImpl createGroupInstance(String name, StatsGroup igroup, String configXmlPath, ObjectName userProvidedMBeanObjectName, boolean bCreateDefaultMBean,
                                                        StatisticActions actionLsnr)
                    throws StatsFactoryException {
        PmiModuleConfig cfg = PerfModules.getConfigFromXMLFile(configXmlPath, actionLsnr.getCurrentBundle());
        if (cfg == null) {

            Tr.warning(tc, "PMI0102W", configXmlPath);
            throw new StatsFactoryException(nls.getFormattedMessage("PMI0102W", new Object[] { configXmlPath }, "Unable to read custom PMI module configuration: {0}") + ". "
                                            + PerfModules.getParseExceptionMsg());
        }

        StatsGroupImpl group = (StatsGroupImpl) igroup;

        StatsFactoryUtil.checkDataIDUniqueness(group, cfg);

        // construct path
        String[] parentPath = group.getPath();
        String[] path = new String[parentPath.length + 1];

        for (int i = 0; i < parentPath.length; i++) {
            path[i] = parentPath[i];
        }
        path[parentPath.length] = name;

        StatsInstanceImpl instance = new StatsInstanceImpl(name, cfg, path, actionLsnr);
        //instance._scListener = scl;

        instance._register(userProvidedMBeanObjectName, bCreateDefaultMBean);

        return instance;
    }

    // ----------------- getters -----------------

    // A Statistic array is created to store the reference to the statistics.
    // Generally, the number of statistics in a module will be around 10 to 15 (based on the existing 
    // pmi modules) it is ok to loop through the array. This eliminates HashMap lookup and creation of 
    // temp Integer object for lookup.
    // This design allows to cache the statistics in the wrapper class (StatsInstanceImpl) and frees 
    // the component owner.

    // Note that this array is redundant since PmiAbstractModule has a HashMap of statistics.

    // Apart from this the component code need to keep a reference to the statistic to reduce the 
    // looping overhead in this method
    public SPIStatistic getStatistic(int id) {
        for (int i = 0; i < _stats.length; i++) {
            if (_stats[i] != null && _stats[i].getId() == id) {
                return _stats[i];

                /*
                 * ~~~~~~~~~~~~~~ commented ~~~~~~~~~~~~~~
                 * // do not check as it is done in individual statistic type
                 * if (_stats[i].isEnabled())
                 * return _stats[i];
                 * else
                 * return null;
                 * ~~~~~~~~~~~~~~ commented ~~~~~~~~~~~~~~
                 */
            }
        }

        return null;
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

    // ----------------- Methods inherited from abstract class -----------------
    @Override
    public String getModuleID() {
        return _moduleID;
    }

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
        return _cInstanceName;
    }

    @Override
    public StatsImpl getStats(ArrayList dataMembers, ArrayList colMembers) {
        // TODO: Create specific Stats Object in future if required.
        StatsImpl s = new StatsImpl(getModuleID(), getName(), type, currentLevel, dataMembers, colMembers);
        //s.setStatsType (getModuleID());
        return s;
    }

    @Override
    public boolean isCustomModule() {
        return true;
    }

    @Override
    protected boolean longCreated(SpdLong data) {
        _setStatMap(data.getStatistic());
        return true;
    }

    @Override
    protected boolean doubleCreated(SpdDouble data) {
        _setStatMap(data.getStatistic());
        return true;
    }

    @Override
    protected boolean statCreated(SpdStat data) {
        _setStatMap(data.getStatistic());
        return true;
    }

    @Override
    protected boolean loadCreated(SpdLoad data) {
        _setStatMap(data.getStatistic());
        return true;
    }

    @Override
    protected boolean externalStatisticCreated(StatisticImpl stat) {
        _setStatMap(stat);
        return true;
    }

    private void _setStatMap(StatisticImpl s) {
        //Statistic s = data.getStatistic ();

        if (super.statisticActionLsnr != null) {
            super.statisticActionLsnr.statisticCreated(s);
        }

        // cache and increment stat count
        _stats[_statCount] = s;
        ++_statCount;
    }

    @Override
    public String getWCCMStatsType() {
        return _moduleID;
    }

    @Override
    public synchronized boolean setFineGrainedInstrumentation(int[] enabled, int[] enableWithSync) {
        // call super, no real processing going on in this method 
        boolean retVal = super.setFineGrainedInstrumentation(enabled, enableWithSync);

        // check and see if 
        if (super.statisticActionLsnr != null) {
            int[] _enabled;
            int[] _disabled;
            int _enabledCount = 0;
            int _disabledCount = 0;

            // get count for enableds
            for (int i = 0; i < _statCount; i++) {
                if (_stats[i].isEnabled())
                    _enabledCount++;
            }

            // create arrays
            _enabled = new int[_enabledCount];
            _disabled = new int[_statCount - _enabledCount];

            // populate arrays            
            _enabledCount = 0;
            for (int i = 0; i < _statCount; i++) {
                if (_stats[i].isEnabled())
                    _enabled[_enabledCount++] = _stats[i].getId();
                else
                    _disabled[_disabledCount++] = _stats[i].getId();
            }

            // pass information back to agent            
            super.statisticActionLsnr.enableStatusChanged(_enabled, _disabled);
        }

        return retVal;
    }
}
