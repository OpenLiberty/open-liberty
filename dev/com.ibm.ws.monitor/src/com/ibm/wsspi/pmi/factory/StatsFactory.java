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

package com.ibm.wsspi.pmi.factory;

import javax.management.ObjectName;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.pmi.PerfModules;
import com.ibm.websphere.pmi.server.PmiModule;
import com.ibm.ws.pmi.factory.StatsFactoryUtil;
import com.ibm.ws.pmi.factory.StatsGroupImpl;
import com.ibm.ws.pmi.factory.StatsInstanceImpl;
import com.ibm.ws.pmi.server.ModuleItem;
import com.ibm.ws.pmi.server.PmiRegistry;

/**
 * StatsFactory is main class in Custom PMI. It is designed to simplify the process of "PMI enabling" a WebSphere application or runtime component.
 * 
 * <p>
 * The following steps are required to instrument a component using Custom PMI:
 * <ol>
 * <li>Define a Stats template
 * <li>Create Stats object using StatsFactory
 * <li>Instrument code and update the Stats object.
 * </ol>
 * 
 * <p>StatsFactory allows runtime component to create a custom Stats/PMI (Stats is the J2EE terminology) module using an XML template. The template
 * should follow the DTD <code>com/ibm/websphere/pmi/xml/stats.dtd</code>.
 * 
 * <p>The statistics created via Custom PMI will be available to the external client programs via JMX MBean and PMI API.
 * The Custom PMI will support all the Statistic types (CountStatistic, TimeStatistic, etc.) defined in the J2EE 1.4 Performance Data Framework.
 * The Custom PMI cannot support any user-defined Statistic type.
 * 
 * <p>This factory class can create objects of type StatsInstance and StatsGroup. The StatsInstance/StatsGroup will be part of the
 * Performance Monitoring Infrastructure (PMI) tree structure. Each StatsInstance/StatsGroup is identified by a unique name in the PMI tree. It is suggested that the name be
 * prefixed with the component/product name.
 * By default each StatsInstance/StatsGroup will be added to the PMI tree at the root level. StatsFactory allows to add a StatsInstance/StatsGroup to a parent
 * StatsInstance/StatsGroup.
 * 
 * <p>Each StatsInstance or StatsGroup should be associated with an MBean in order to access the statistics via JMX interface.
 * There are two ways to access the statistics via JMX:
 * <ul><li>via managed object<li>via Perf MBean</ul>
 * 
 * <p>In order to access the statistics via managed object MBean the user should provide the MBean when creating the
 * StatsInstance or StatsGroup.
 * 
 * 
 * <p>All Stats (with or without an MBean) can be fetched via the Perf MBean.
 * Stats without an MBean is identified using the {@link com.ibm.websphere.pmi.stat.StatDescriptor}. Stats with an MBean can be identified using the
 * {@link com.ibm.websphere.pmi.stat.StatDescriptor} or the <code>javax.management.ObjectName</code>.
 * 
 * @ibm-spi
 */

/*
 * <!-- <p><b><i>Note</i></b>: In WebSphere 5.0, the PMI service identifies and associates the appropriate MBean (if exists) to the Stats module by using the mapping defined in
 * pmiJmxMapper.xml.
 * This process is expensive and becomes complicated with the growing number of Stats.
 * In Custom PMI, the PMI service will NOT do any MBean mapping and it is the responsibility of the component owner to associate an appropriate MBean with the Stats module if the
 * statistics need to be accessed via managed object MBean.
 * 
 * <p>On the other hand if a component doesn't have an MBean the Custom PMI API can optionally create an MBean and associate with the Stats module. The MBean created by the Custom
 * PMI will be of type "CustomStats".
 * <p><b><i>Note</i></b>: The default MBean created by custom PMI will only provide getStats() method to access the statistics. If the component wants to provide other MBean
 * methods the component should create a new MBean. In general, it is preferable that the component owner provide a component specific MBean instead of using the default Custom PMI
 * MBean.
 * -->
 * 
 * <p>An MBeanStatDescriptor consists of an MBean ObjectName and the relative path to a StatsGroup/Instance. The Stats modules are organized in a tree structure in the PMI service
 * and each StatsGroup/StatsInstance can be located in the PMI tree using an MBeanStatDescriptor.
 * 
 * <p>From WebSphere 5.0.2 and beyond the Custom PMI API should be used to add new PMI module.
 * The existing PMI modules in WebSphere 5.0 will not be affected by the Custom PMI and doesn't require any change.
 */
public class StatsFactory {
    private static final TraceComponent tc = Tr.register(StatsFactory.class);
    private static final TraceNLS nls = TraceNLS.getTraceNLS(PmiRegistry.MSG_BUNDLE);

    /**
     * Returns PMI service status.
     * 
     * @return true if PMI service is enabled
     */
    public static boolean isPMIEnabled() {
        return !PmiRegistry.isDisabled();
    }

    /**
     * Create a StatsGroup using the Stats template and add to the PMI tree at the root level.
     * This method will associate the MBean provided by the caller to the Stats group.
     * 
     * @param groupName name of the group
     * @param statsTemplate location of the Stats template XML file
     * @param mBean MBean that needs to be associated with the Stats group
     * @return Stats group
     * @exception StatsFactoryException if error while creating Stats group
     */
    public static StatsGroup createStatsGroup(String groupName, String statsTemplate, ObjectName mBean, StatisticActions actionLsnr)
                    throws StatsFactoryException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, new StringBuffer("createStatsGroup:name=").append(groupName).append(";template=").
                            append(statsTemplate).append(";mBean=").append((mBean == null) ? null : mBean.toString()).toString());

        checkPMIService(groupName);

        StatsGroup group;
        try {
            group = StatsGroupImpl.createGroup(groupName, statsTemplate, mBean, false, actionLsnr);
        } catch (StatsFactoryException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception:", e);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "createStatsGroup");

            throw e;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createStatsGroup");
        return group;
    }

    /**
     * Create a StatsGroup using the Stats template and add to the PMI tree under the specified parent group.
     * This method will associate the MBean provided by the caller to the Stats group.
     * 
     * @param groupName name of the group
     * @param statsTemplate location of the Stats template XML file
     * @param parentGroup parent Stats group
     * @param mBean MBean that needs to be associated with the Stats group
     * @return Stats group
     * @exception StatsFactoryException if error while creating Stats group
     */

    public static StatsGroup createStatsGroup(String groupName, String statsTemplate, StatsGroup parentGroup, ObjectName mBean, StatisticActions actionLsnr)
                    throws StatsFactoryException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, new StringBuffer("createStatsGroup:name=").append(groupName).append(";parent group=").
                            append(parentGroup.getName()).append(";template=").append(statsTemplate).append(";mBean=").append((mBean == null) ? null : mBean.toString()).toString());

        checkPMIService(groupName);

        StatsGroup group;
        try {
            group = StatsGroupImpl.createGroup(groupName, parentGroup, statsTemplate, mBean, false, actionLsnr);
        } catch (StatsFactoryException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception:", e);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "createStatsGroup");

            throw e;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createStatsGroup");

        return group;
    }

    /**
     * Create a StatsGroup using the Stats template and add to the PMI tree under the specified parent instance.
     * This method will associate the MBean provided by the caller to the Stats group.
     * 
     * @param groupName name of the group
     * @param statsTemplate location of the Stats template XML file
     * @param parentInstance parent Stats instance
     * @param mBean MBean that needs to be associated with the Stats group
     * @return Stats group
     * @exception StatsFactoryException if error while creating Stats group
     */
    public static StatsGroup createStatsGroup(String groupName, String statsTemplate, StatsInstance parentInstance, ObjectName mBean, StatisticActions actionLsnr)
                    throws StatsFactoryException {
        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     new StringBuffer("createStatsGroup:name=").append(groupName).append(";parent instance=").
                                     append(parentInstance.getName()).append(";template=").append(statsTemplate).append(";mBean=").append((mBean == null) ? null : mBean.toString()).toString());

        checkPMIService(groupName);

        StatsGroup group;
        try {
            group = StatsGroupImpl.createGroup(groupName, parentInstance, statsTemplate, mBean, false, actionLsnr);
        } catch (StatsFactoryException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception:", e);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "createStatsGroup");

            throw e;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createStatsGroup");

        return group;
    }

    /**
     * Create a StatsInstance using the Stats template and add to the PMI tree at the root level.
     * This method will associate the MBean provided by the caller to the Stats instance.
     * 
     * @param instanceName name of the instance
     * @param statsTemplate location of the Stats template XML file
     * @param mBean MBean that needs to be associated with the Stats instance
     * @param listener a StatisticActionListener object. This object will be called when a statistic is created for this instance
     * @return Stats instance
     * @exception StatsFactoryException if error while creating Stats instance
     * @deprecated As of 6.1, replaced by createStatsInstance(String, String, ObjectName, StatisticActions ).
     */
    @Deprecated
    public static StatsInstance createStatsInstance(String instanceName, String statsTemplate, ObjectName mBean, StatisticActionListener listener)
                    throws StatsFactoryException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, new StringBuffer("createStatsInstance:name=").append(instanceName).append(";template=").
                            append(statsTemplate).append(";mBean=").append((mBean == null) ? null : mBean.toString()).toString());

        // call sibling method using a StatisticActions object
        StatsInstance rv = createStatsInstance(instanceName,
                                               statsTemplate,
                                               mBean,
                                               new StatisticActions(listener));

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createStatsInstance");

        return rv;
    }

    /**
     * Create a StatsInstance using the Stats template and add to the PMI tree at the root level.
     * This method will associate the MBean provided by the caller to the Stats instance.
     * 
     * @param instanceName name of the instance
     * @param statsTemplate location of the Stats template XML file
     * @param mBean MBean that needs to be associated with the Stats instance
     * @param listener A StatisticActions object. This object will be called when events occur on statistics created for this instance
     * @return Stats instance
     * @exception StatsFactoryException if error while creating Stats instance
     */
    public static StatsInstance createStatsInstance(String instanceName, String statsTemplate, ObjectName mBean, StatisticActions listener)
                    throws StatsFactoryException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, new StringBuffer("createStatsInstance:name=").append(instanceName).append(";template=").
                            append(statsTemplate).append(";mBean=").append((mBean == null) ? null : mBean.toString()).toString());

        checkPMIService(instanceName);

        StatsInstance instance;
        try {
            instance = StatsInstanceImpl.createInstance(instanceName, statsTemplate, mBean, false, listener);
        } catch (StatsFactoryException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception:", e);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "createStatsInstance");

            throw e;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createStatsInstance");

        return instance;
    }

    /**
     * Create a StatsInstance under the specified parent group. The new Stats instance will use the parent template.
     * This method will associate the MBean provided by the caller to the Stats instance.
     * 
     * @param instanceName name of the instance
     * @param parentGroup parent Stats group
     * @param mBean MBean that needs to be associated with the Stats instance
     * @param listener a StatisticActionListener object. This object will be called when a statistic is created for this instance
     * @return Stats instance
     * @exception StatsFactoryException if error while creating Stats instance
     * @deprecated As of 6.1, replaced by createStatsInstance(String, StatsGroup, ObjectName, StatisticActions ).
     */
    @Deprecated
    public static StatsInstance createStatsInstance(String instanceName, StatsGroup parentGroup, ObjectName mBean, StatisticActionListener listener)
                    throws StatsFactoryException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, new StringBuffer("createStatsInstance:name=").append(instanceName).append(";parent name=").
                            append(parentGroup.getName()).append(";mBean=").append((mBean == null) ? null : mBean.toString()).toString());

        // call sibling method using a StatisticActions object
        StatsInstance rv = createStatsInstance(instanceName,
                                               parentGroup,
                                               mBean,
                                               new StatisticActions(listener));

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createStatsInstance");

        return rv;
    }

    /**
     * Create a StatsInstance under the specified parent group. The new Stats instance will use the parent template.
     * This method will associate the MBean provided by the caller to the Stats instance.
     * 
     * @param instanceName name of the instance
     * @param parentGroup parent Stats group
     * @param mBean MBean that needs to be associated with the Stats instance
     * @param listener A StatisticActions object. This object will be called when events occur on statistics created for this instance
     * @return Stats instance
     * @exception StatsFactoryException if error while creating Stats instance
     */
    public static StatsInstance createStatsInstance(String instanceName, StatsGroup parentGroup, ObjectName mBean, StatisticActions listener)
                    throws StatsFactoryException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, new StringBuffer("createStatsInstance:name=").append(instanceName).append(";parent name=").
                            append(parentGroup.getName()).append(";mBean=").append((mBean == null) ? null : mBean.toString()).toString());

        //checkPMIService (instanceName);

        StatsInstance instance;
        try {
            instance = StatsInstanceImpl.createGroupInstance(instanceName, parentGroup, mBean, false, listener);
        } catch (StatsFactoryException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception:", e);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "createStatsInstance");

            throw e;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createStatsInstance");
        //System.out.println("$$$$$$$ instaance is "+instance);
        return instance;
    }

    /**
     * Create a StatsInstance using the template and add to the PMI tree under the specified parent group.
     * This method will associate the MBean provided by the caller to the Stats instance.
     * 
     * Note that the parent StatsGroup will only aggregate the child StatsInstances that are created
     * from the same stats template as that of the parent.
     * 
     * @param instanceName name of the instance
     * @param statsTemplate location of the Stats template XML file
     * @param parentGroup parent Stats group
     * @param mBean MBean that needs to be associated with the Stats instance
     * @param listener a StatisticActionListener object. This object will be called when a statistic is created for this instance
     * @return Stats instance
     * @exception StatsFactoryException if error while creating Stats instance
     * @deprecated As of 6.1, replaced by createStatsInstance(String, String, StatsGroup, StatsGroup, ObjectName, StatisticActions ).
     */
    @Deprecated
    public static StatsInstance createStatsInstance(String instanceName, String statsTemplate, StatsGroup parentGroup, ObjectName mBean, StatisticActionListener listener)
                    throws StatsFactoryException {
        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     new StringBuffer("createStatsInstance:name=").append(instanceName).append(";template=").
                                     append(statsTemplate).append(";parent name=").append(parentGroup.getName()).append(";mBean=").append((mBean == null) ? null : mBean.toString()).toString());

        StatsInstance instance = createStatsInstance(instanceName,
                                                     statsTemplate,
                                                     parentGroup,
                                                     mBean,
                                                     new StatisticActions(listener));

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createStatsInstance");

        return instance;
    }

    /**
     * Create a StatsInstance using the template and add to the PMI tree under the specified parent group.
     * This method will associate the MBean provided by the caller to the Stats instance.
     * 
     * Note that the parent StatsGroup will only aggregate the child StatsInstances that are created
     * from the same stats template as that of the parent.
     * 
     * @param instanceName name of the instance
     * @param statsTemplate location of the Stats template XML file
     * @param parentGroup parent Stats group
     * @param mBean MBean that needs to be associated with the Stats instance
     * @param listener A StatisticActions object. This object will be called when events occur on statistics created for this instance
     * @return Stats instance
     * @exception StatsFactoryException if error while creating Stats instance
     */
    public static StatsInstance createStatsInstance(String instanceName, String statsTemplate, StatsGroup parentGroup, ObjectName mBean, StatisticActions listener)
                    throws StatsFactoryException {
        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     new StringBuffer("createStatsInstance:name=").append(instanceName).append(";template=").
                                     append(statsTemplate).append(";parent name=").append(parentGroup.getName()).append(";mBean=").append((mBean == null) ? null : mBean.toString()).toString());

        checkPMIService(instanceName);

        StatsInstance instance;
        try {
            instance = StatsInstanceImpl.createGroupInstance(instanceName, parentGroup, statsTemplate, mBean, false, listener);
        } catch (StatsFactoryException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception:", e);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "createStatsInstance");

            throw e;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createStatsInstance");

        return instance;
    }

    /**
     * Removes a StatsInstance from the PMI tree. Note that any children under the instance will also be removed.
     * If the instance is associated with a default CustomStats MBean, the MBean will be de-activated.
     * 
     * @param instance StatsInstance to be removed
     * @exception StatsFactoryException if error while removing Stats instance
     */
    public static void removeStatsInstance(StatsInstance instance)
                    throws StatsFactoryException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, new StringBuffer("removeStatsInstance:name=").append(instance.getName()).toString());

        StatsFactoryUtil.unRegisterStats((PmiModule) instance, instance.getMBean());

        if (tc.isEntryEnabled())
            Tr.exit(tc, "removeStatsInstance");
    }

    /**
     * Removes a StatsGroup from the PMI tree. Note that any children under the group will also be removed.
     * If the group is associated with a default CustomStats MBean, the MBean will be de-activated.
     * 
     * @param group StatsGroup to be removed
     * @exception StatsFactoryException if error while removing Stats group
     */
    public static void removeStatsGroup(StatsGroup group)
                    throws StatsFactoryException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, new StringBuffer("removeStatsGroup:name=").append(group.getName()).toString());

        StatsFactoryUtil.unRegisterStats((PmiModule) group, group.getMBean());

        if (tc.isEntryEnabled())
            Tr.exit(tc, "removeStatsGroup");
    }

    /**
     * Registers a StatsTemplateLookup object with the PMI service (WebSphere internal use only).
     * 
     * @param lookupClass An instance of {@link com.ibm.wsspi.pmi.factory.StatsTemplateLookup}
     */
    public static void registerStatsTemplateLookup(StatsTemplateLookup lookupClass) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, new StringBuffer("registerStatsTemplateLookup: ").append(lookupClass.getClass().getName()).toString());

        PerfModules.registerTemplateLookupClass(lookupClass);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "registerStatsTemplateLookup");
    }

    /*******************************************************************************
     * This method may be used to retrieve an existing StatsGroup object. The object
     * should be specified using a String array specifying the path to use to retrieve the
     * object. If a StatsGroup object can not be found using the specified path, a null
     * value is returned.
     * 
     * @param path A String array. The string elements in this parameter specify the hierarchy of the stats group being retrieved. If no stats group is found matching the path
     *            provided, the response object will be null.
     *******************************************************************************/
    public static StatsGroup getStatsGroup(String[] path) {
        StatsGroup group = null;

        if (path != null) {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "getStatsGroup: " + _arrayToString(path));

            ModuleItem parent = PmiRegistry.findModuleItem(path);
            if (parent != null) {
                Object o = parent.getInstance();
                if (o instanceof com.ibm.ws.pmi.factory.StatsGroupImpl)
                    group = (StatsGroup) o;
            }
        } else {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "getStatsGroup: path is null");
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getStatsGroup");

        return group;
    }

    /*******************************************************************************
     * This method may be used to retrieve an existing StatsInstance object. The object
     * should be specified using a String array specifying the path to use to retrieve the
     * object. If a StatsInstance object can not be found using the specified path, a null
     * value is returned.
     * 
     * @param path A String array. The string elements in this parameter specify the hierarchy of the stats instance being retrieved. If no stats instance is found matching the
     *            path provided, the response object will be null.
     *******************************************************************************/
    public static StatsInstance getStatsInstance(String[] path) {
        StatsInstance instance = null;

        if (path != null) {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "getStatsInstance: " + _arrayToString(path));

            ModuleItem parent = PmiRegistry.findModuleItem(path);
            if (parent != null) {
                Object o = parent.getInstance();
                if (o instanceof com.ibm.ws.pmi.factory.StatsInstanceImpl)
                    instance = (StatsInstance) o;
            }
        } else {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "getStatsInstance: path is null");
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getStatsInstance");

        return instance;
    }

    private static String _arrayToString(String[] path) {
        StringBuffer b = new StringBuffer(path[0]);
        for (int i = 1; i < path.length; i++) {
            b.append("/").append(path[i]);
        }

        return b.toString();
    }

    private static void checkPMIService(String name) throws StatsFactoryException {
        if (!isPMIEnabled()) {
            //Tr.warning(tc, "PMI0107W", name);
            throw new StatsFactoryException(nls.getFormattedMessage("PMI0107W", new Object[] { name },
                                                                    "PMI0107W: Unable to register custom PMI module since the PMI service is not enabled: {0}"));
        }
    }
}
