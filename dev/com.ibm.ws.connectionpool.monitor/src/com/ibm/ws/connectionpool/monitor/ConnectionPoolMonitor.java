/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.connectionpool.monitor;

import java.util.HashMap;

import com.ibm.websphere.jca.pmi.JCAPMIHelper;
import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.ProbeAtEntry;
import com.ibm.websphere.monitor.annotation.ProbeAtExceptionExit;
import com.ibm.websphere.monitor.annotation.ProbeAtReturn;
import com.ibm.websphere.monitor.annotation.ProbeSite;
import com.ibm.websphere.monitor.annotation.PublishedMetric;
import com.ibm.websphere.monitor.annotation.This;
import com.ibm.websphere.monitor.meters.MeterCollection;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;
import com.ibm.wsspi.pmi.factory.StatsGroup;

/**
 * This class is responsible for ConnectionPool Monitoring.Using GroupName(=ConnectionPool) one can use FineGrained Approach to filter collection of Data.
 */
@Monitor(group = "ConnectionPool")
public class ConnectionPoolMonitor extends StatisticActions {
    @PublishedMetric
    public MeterCollection<ConnectionPoolStats> connectionPoolCountByName = new MeterCollection<ConnectionPoolStats>("ConnectionPool", this);
    private static final TraceComponent tc = Tr.register(ConnectionPoolMonitor.class, "ConnectionPoolMonitor");

    private final ThreadLocal<Long> tlocalforwtTime = new ThreadLocal<Long>();
    private final ThreadLocal<Long> tlocalforiuTime = new ThreadLocal<Long>();
    private final ThreadLocal<Integer> tlocalforiumconThread = new ThreadLocal<Integer>();

    private final ThreadLocal<Boolean> tlocalfpsize = new ThreadLocal<Boolean>() {
        @Override
        public Boolean initialValue() {
            return false;
        }
    };
    private static HashMap<String, ConnectionPoolStats> CPStatsMap = new HashMap<String, ConnectionPoolStats>();
    private static final String template = "/com/ibm/ws/connectionpool/monitor/xml/j2cModule.xml";
    private StatsGroup grp;
    private final HashMap<String, LegacyMonitor> legacyPMIMap = new HashMap<String, LegacyMonitor>();

    public ConnectionPoolMonitor() {

        try {
            /*
             * Below code comes into picture only when traditional PMI is enabled
             */
            if (StatsFactory.isPMIEnabled()) {
                grp = StatsFactory.createStatsGroup("ConnectionPool", template, null, this);
            }
        } catch (StatsFactoryException e) {
            //If PMI Is disabled, we get this.
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "ConnectionPool Module is not registered with PMI");
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "ConnectionPoolMonitor");
        }

    }

    /**
     * @param dsName=DataSourceName
     * @return ConnectionPoolStats Object
     *         This is responsible for creating a ConnectionPoolStats for specific datasource and adding it to the MeterCollection.Before adding
     *         we check in the synchronized block to make sure that there is no already existing connectionPoolStats Object for the specific datasource.
     */
    public synchronized ConnectionPoolStats initializeConnectionPoolStats(String dsName) {
        ConnectionPoolStats cStats = this.connectionPoolCountByName.get(dsName);
        if (cStats == null) {
            cStats = new ConnectionPoolStats();
            this.connectionPoolCountByName.put(dsName, cStats);
            if (StatsFactory.isPMIEnabled()) {
                legacyPMIMap.put(dsName, new LegacyMonitor(dsName, grp));
                CPStatsMap.put(dsName, cStats);
            }
            return cStats;
        } else {
            return cStats;
        }
    }

    /**
     * @param FreePool Object
     *            This method is responsible for calculating createCount(incremented) = number of connections created and ManagedConnectionCount(incremented) = number of
     *            connections in use
     *            This is called when control successfully returns from FreePool createManagedConnectionWithMCWrapper which is an indication that connection is
     *            successfully created or is being used.
     *            HookPoints=FreePool.createManagedConnectionWithMCWrapper
     */
    @ProbeAtReturn
    @ProbeSite(clazz = "com.ibm.ejs.j2c.FreePool", method = "createManagedConnectionWithMCWrapper")
    public void incCreateCount(@This Object fpObject) {
        try {
            JCAPMIHelper fpobj = (JCAPMIHelper) fpObject;
            if (tc.isEntryEnabled()) {
                Tr.entry(tc, "incCreateCount");
            }
            String JNDIName = fpobj.getJNDIName(); //First get the actual JNDIName
            if (JNDIName == null) { //Check for null
                JNDIName = fpobj.getUniqueId(); //IF it is null then use getUniqueId.JndiName will be null when no JNDI name is given.
            }
            if (JNDIName == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "JNDI Name returned is null this will only come when DS is created but no JNDI name is provided.We Should not handle this case");
                }
                if (tc.isEntryEnabled()) {
                    Tr.exit(tc, "incCreateCount");
                }
                return;
            }
            if (JNDIName.contains(":")) {
                JNDIName = JNDIName.replace(":", "-");
            }
            ConnectionPoolStats cStats = connectionPoolCountByName.get(JNDIName);
            if (cStats == null) {
                cStats = initializeConnectionPoolStats(JNDIName);
            }
            cStats.incCreateCount();
            cStats.incManagedConnectionCount();
            cStats.incFreeConnectionCount();
            if (tc.isEntryEnabled()) {
                Tr.exit(tc, "incCreateCount");
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
     * @param MCWrapper Object
     *            This code is responsible for calculating Destroy Count(incremented)=Number of connections destroyed or released and ManagedConnections(Decremented).
     *            HookPoints=MCWrapper.destroy.
     */
    @ProbeAtReturn
    @ProbeAtExceptionExit
    @ProbeSite(clazz = "com.ibm.ejs.j2c.MCWrapper", method = "destroy")
    public void incDestroyCount(@This Object ob) {
        try {
            if (tc.isEntryEnabled()) {
                Tr.entry(tc, "incDestroyCount");
            }
            JCAPMIHelper mObject = (JCAPMIHelper) ob;
            String JNDIName = mObject.getJNDIName(); //First get the actual JNDIName
            if (JNDIName == null) { //Check for null
                JNDIName = mObject.getUniqueId(); //IF it is null then use getUniqueId.JndiName will be null when no JNDI name is given.
            }
            if (JNDIName == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc,
                             "JNDI Name returned is null this will only come when DS is created but no JNDI name is provided so J2C returns us a null.We Should not handle this case");
                }
                if (tc.isEntryEnabled()) {
                    Tr.exit(tc, "incDestroyCount");
                }
                return;
            }
            if (JNDIName.contains(":")) {
                JNDIName = JNDIName.replace(":", "-");
            }
            ConnectionPoolStats cStats = connectionPoolCountByName.get(JNDIName);
            if (cStats == null) {
                if (tc.isEntryEnabled()) {
                    Tr.exit(tc, "incDestroyCount");
                }
                return;
            }
            //ConnectionPoolStats cStats = connectionPoolCountByName.get(JNDIName);
            cStats.incDestroyCount();
            cStats.decManagedConnectionCount();
            cStats.decFreeConnectionCount();
            if (tc.isEntryEnabled()) {
                Tr.exit(tc, "incDestroyCount");
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.getMessage();
        }
    }

    /**
     * @param MCWrapper Object
     *            This method is responsible for calculating connectionHandleCount(increment)
     *            HookPoints=MCWrapper.incrementHandleCount
     */
    @ProbeAtReturn
    @ProbeSite(clazz = "com.ibm.ejs.j2c.MCWrapper", method = "incrementHandleCount")
    public void incConnectionHandleCount(@This Object ob) {
        try {
            if (tc.isEntryEnabled()) {
                Tr.entry(tc, "incConnectionHandleCount");
            }
            JCAPMIHelper mObject = (JCAPMIHelper) ob;

            String JNDIName = mObject.getJNDIName(); //First get the actual JNDIName
            if (JNDIName == null) { //Check for null
                JNDIName = mObject.getUniqueId(); //IF it is null then use getUniqueId.JndiName will be null when no JNDI name is given.
            }
            if (JNDIName == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "JNDI Name returned is null this will only come when DS is created but no JNDI name is provided.We Should not handle this case");
                }
                if (tc.isEntryEnabled()) {
                    Tr.exit(tc, "incConnectionHandleCount");
                }
                return;
            }
            if (JNDIName.contains(":")) {
                JNDIName = JNDIName.replace(":", "-");
            }
            ConnectionPoolStats cStats = connectionPoolCountByName.get(JNDIName);
            if (cStats == null) {
                cStats = initializeConnectionPoolStats(JNDIName);
            }
            //cStats = connectionPoolCountByName.get(JNDIName);
            cStats.incConnectionHandleCount();
            if (tc.isEntryEnabled()) {
                Tr.exit(tc, "incConnectionHandleCount");
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.getMessage();
        }
    }

    /**
     * @param MCWrapper Object
     *            This method is responsible for calculating connectionHandleCount(decrement)
     *            HookPoints:MCWrapper.decrementHandleCount
     */
    @ProbeAtReturn
    @ProbeSite(clazz = "com.ibm.ejs.j2c.MCWrapper", method = "decrementHandleCount")
    public void decConnectionHandleCount(@This Object ob) {
        try {
            if (tc.isEntryEnabled()) {
                Tr.entry(tc, "decConnectionHandleCount");
            }
            JCAPMIHelper mObject = (JCAPMIHelper) ob;
            String JNDIName = mObject.getJNDIName(); //First get the actual JNDIName
            if (JNDIName == null) { //Check for null
                JNDIName = mObject.getUniqueId(); //IF it is null then use getUniqueId.JndiName will be null when no JNDI name is given.
            }
            if (JNDIName == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "JNDI Name returned is null this will only come when DS is created but no JNDI name is provided.We Should not handle this case");
                }
                if (tc.isEntryEnabled()) {
                    Tr.exit(tc, "decConnectionHandleCount");
                }
                return;
            }
            if (JNDIName.contains(":")) {
                JNDIName = JNDIName.replace(":", "-");
            }
            ConnectionPoolStats cStats = connectionPoolCountByName.get(JNDIName);
            if (cStats == null) {
                if (tc.isEntryEnabled()) {
                    Tr.exit(tc, "decConnectionHandleCount");
                }
                return;
            }
            //ConnectionPoolStats cStats = connectionPoolCountByName.get(JNDIName);
            cStats.decConnectionHandleCount();
            if (tc.isEntryEnabled()) {
                Tr.exit(tc, "decConnectionHandleCount");
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.getMessage();
        }
    }

    /**
     * Code which sets the entry time for queueRequest for wait time.We set it in ThreadLocal variable
     */
    @ProbeAtEntry
    @ProbeSite(clazz = "com.ibm.ejs.j2c.FreePool", method = "queueRequest")
    public void waitTimeEntry() {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "waitTimeEntry");
        }
        tlocalforwtTime.set(System.currentTimeMillis());
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "waitTimeEntry");
        }
    }

    /**
     * @param wtobj
     *            Code which gets value from ThreadLocal and calculates the time spent in queueRequest which will give the wait time.
     */
    @ProbeAtReturn
    @ProbeAtExceptionExit
    @ProbeSite(clazz = "com.ibm.ejs.j2c.FreePool", method = "queueRequest")
    public void waitTimeExit(@This Object wtobj) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "waitTimeExit");
        }
        long elapsed = (System.currentTimeMillis() - tlocalforwtTime.get());
        JCAPMIHelper mObject = (JCAPMIHelper) wtobj;
        String JNDIName = mObject.getJNDIName(); //First get the actual JNDIName
        if (JNDIName == null) { //Check for null
            JNDIName = mObject.getUniqueId(); //IF it is null then use getUniqueId.JndiName will be null when no JNDI name is given.
        }
        if (JNDIName == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "JNDI Name returned is null this will only come when DS is created but no JNDI name is provided.We Should not handle this case");
            }
            if (tc.isEntryEnabled()) {
                Tr.exit(tc, "waitTimeExit");
            }
            return;
        }
        if (JNDIName.contains(":")) {
            JNDIName = JNDIName.replace(":", "-");
        }
        ConnectionPoolStats cStats = connectionPoolCountByName.get(JNDIName);
        if (cStats == null) {
            cStats = initializeConnectionPoolStats(JNDIName);
        }
        //cStats = connectionPoolCountByName.get(JNDIName);
        cStats.updateWaitTime(elapsed);

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "waitTimeExit");
        }
    }

    /**
     * @param MCWrapper Object
     *            This particular Method is called when when a connection going through cleanup was not already in the Free Active
     *            state. This means the free pool connection count should be incremented. The connection is either going to be
     *            added to the free pool, or will be destroyed (and the free pool count will be decremented in destroy).
     */
    @ProbeAtEntry
    @ProbeSite(clazz = "com.ibm.ejs.j2c.MCWrapper", method = "isNotAlreadyFreeActive")
    public void incFreePoolSize(@This Object ob) {
        try {
            if (tc.isEntryEnabled()) {
                Tr.entry(tc, "incFreePoolSize");
            }
            JCAPMIHelper mObject = (JCAPMIHelper) ob;
            String JNDIName = mObject.getJNDIName(); //First get the actual JNDIName
            if (JNDIName == null) { //Check for null
                JNDIName = mObject.getUniqueId(); //IF it is null then use getUniqueId.JndiName will be null when no JNDI name is given.
            }
            if (JNDIName == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "JNDI Name returned is null this will only come when DS is created but no JNDI name is provided.We Should not handle this case");
                }
                if (tc.isEntryEnabled()) {
                    Tr.exit(tc, "incFreePoolSize");
                }
                return;
            }
            if (JNDIName.contains(":")) {
                JNDIName = JNDIName.replace(":", "-");
            }
            ConnectionPoolStats cStats = connectionPoolCountByName.get(JNDIName);
            if (cStats == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc,
                             "Do not create new ConnectionPooLStas  at this point if w dnt get ConnectionPoolStats as we are sure that by this time CStats should be created");
                }
                if (tc.isEntryEnabled()) {
                    Tr.exit(tc, "incFreePoolSize");
                }
                return;
            }
            Long iuTime = tlocalforiuTime.get();
            if (iuTime != null) {
                long elapsed = (System.currentTimeMillis() - tlocalforiuTime.get());
                cStats.updateInUseTime(elapsed);
                if (tlocalforiumconThread.get() < 2) {
                    tlocalforiuTime.set(null);
                } else {
                    tlocalforiumconThread.set(tlocalforiumconThread.get() - 1);
                }
            }
            cStats.incFreeConnectionCount();
            tlocalfpsize.set(true);
            if (tc.isEntryEnabled()) {
                Tr.exit(tc, "incFreePoolSize");
            }
        } catch (

        Exception e) {
            e.getMessage();
        }
    }

    /**
     * @param MCWrapper
     *            This method is called when Connection is being marked "as in use" which means that thread is being pulled from
     */
    @ProbeAtReturn
    @ProbeSite(clazz = "com.ibm.ejs.j2c.MCWrapper", method = "markInUse")
    public void decFreePoolSize(@This Object McObj) {
        try {
            if (tc.isEntryEnabled()) {
                Tr.entry(tc, "decFreePoolSize");
            }
            JCAPMIHelper mcwrapperobj = (JCAPMIHelper) McObj;
            if (!(mcwrapperobj.getParkedValue())) {
                String JNDIName = mcwrapperobj.getJNDIName(); //First get the actual JNDIName
                if (JNDIName == null) { //Check for null
                    JNDIName = mcwrapperobj.getUniqueId(); //IF it is null then use getUniqueId.JndiName will be null when no JNDI name is given.
                }
                if (JNDIName == null) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "JNDI Name returned is null this will only come when DS is created but no JNDI name is provided.We Should not handle this case");
                    }
                    if (tc.isEntryEnabled()) {
                        Tr.exit(tc, "decFreePoolSize");
                    }
                    return;
                }
                if (JNDIName.contains(":")) {
                    JNDIName = JNDIName.replace(":", "-");
                }
                Long iuTime = tlocalforiuTime.get();
                if (iuTime == null) {
                    tlocalforiuTime.set(System.currentTimeMillis()); // start the in use time for connections
                    tlocalforiumconThread.set(1);
                } else {
                    tlocalforiumconThread.set(tlocalforiumconThread.get() + 1);
                }
                ConnectionPoolStats cStats = connectionPoolCountByName.get(JNDIName);
                if (cStats == null) {
                    if (tc.isEntryEnabled()) {
                        Tr.exit(tc, "decFreePoolSize");
                    }
                    return;
                }
                cStats.decFreeConnectionCount();
            }
            if (tc.isEntryEnabled()) {
                Tr.exit(tc, "decFreePoolSize");
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
     * @param obj
     *            This code comes when datasource is removed or during server shutdown to clean up the MXBeans created while Monitoring Framework takes care of cleaning up
     *            injection code.
     */
    @ProbeAtReturn
    @ProbeSite(clazz = "com.ibm.ejs.j2c.PoolManager", method = "serverShutDown")
    public void removeDataSource(@This Object obj) {
        JCAPMIHelper fpObj = (JCAPMIHelper) obj;
        String JNDIName = fpObj.getJNDIName(); //First get the actual JNDIName
        if (JNDIName == null) { //Check for null
            JNDIName = fpObj.getUniqueId(); //IF it is null then use getUniqueId.JndiName will be null when no JNDI name is given.
        }
        if (JNDIName == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "JNDI Name returned is null this will only come when DS is created but no JNDI name is provided.We Should not handle this case");
            }
            return;
        }
        if (JNDIName.contains(":")) {
            JNDIName = JNDIName.replace(":", "-");
        }
        connectionPoolCountByName.remove(JNDIName);

        if (StatsFactory.isPMIEnabled()) {
            //Cleanup legacy PMI Instance else this will cause trouble when adding again
            (legacyPMIMap.remove(fpObj.getUniqueId())).removeSInstance();
        }
    }

    /**
     * @param appName
     * @return
     *         Utility method which comes into picture in case of traditional PMI
     */
    static ConnectionPoolStats getConnectionPoolOB(String dsName) {
        return CPStatsMap.get(dsName);
    }
}