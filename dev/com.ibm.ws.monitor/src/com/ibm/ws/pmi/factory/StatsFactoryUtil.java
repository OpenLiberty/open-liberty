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

import javax.management.ObjectName;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.pmi.PmiConstants;
import com.ibm.websphere.pmi.PmiDataInfo;
import com.ibm.websphere.pmi.PmiModuleConfig;
import com.ibm.websphere.pmi.server.PmiModule;
import com.ibm.websphere.pmi.stat.MBeanStatDescriptor;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.pmi.server.ModuleItem;
import com.ibm.ws.pmi.server.PmiRegistry;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;

public class StatsFactoryUtil {
    public static final String DEFAULT_MBEAN = "CustomStats";
    private static final String DEFAULT_MBEAN_DESCRIPTOR = "com/ibm/ws/pmi/factory/CustomStats.xml";

    private static final TraceComponent tc = Tr.register(StatsFactoryUtil.class, PmiConstants.TR_GROUP, PmiConstants.MSG_BUNDLE);
    private static final TraceNLS nls = TraceNLS.getTraceNLS(PmiConstants.MSG_BUNDLE);

    public static MBeanStatDescriptor createMBean(Object runtimeObject, String beanName, ObjectName userProvidedMBeanObjectName, boolean bCreateDefaultMBean)
                    throws StatsFactoryException {
        if (userProvidedMBeanObjectName != null) {
            return new MBeanStatDescriptor(userProvidedMBeanObjectName);
        }
        /*
         * else
         * if (bCreateDefaultMBean)
         * {
         * return new MBeanStatDescriptor (StatsFactoryUtil.createDefaultMBean (runtimeObject, beanName));
         * }
         */
        else {
            return null;
        }
    }

    private static ObjectName createDefaultMBean(Object runtimeObject, String beanName) throws StatsFactoryException {
        return null;
        /*
         * try
         * {
         * // use path to create unique mbean identifier
         * String[] path = ((PmiAbstractModule) runtimeObject).getPath();
         * Properties prop = new Properties();
         * prop.put ("name", beanName);
         * 
         * MBeanFactory factory = AdminServiceFactory.getMBeanFactory();
         * DefaultRuntimeCollaborator collab = new DefaultRuntimeCollaborator(runtimeObject, beanName);
         * return factory.activateMBean(DEFAULT_MBEAN, collab, getMBeanID(path), DEFAULT_MBEAN_DESCRIPTOR, prop);
         * }
         * catch(Exception e)
         * {
         * Tr.warning (tc, "PMI0101W", beanName);
         * throw new StatsFactoryException (nls.getFormattedMessage ("PMI0101W", new Object[]{beanName}, "Unable to create default MBean for custom PMI module: {0}"));
         * }
         */
    }

    public static void registerModule(PmiModule module, MBeanStatDescriptor msd) throws StatsFactoryException {
        // Register module
        ModuleItem mItem = PmiRegistry.registerModule(module);

        if (mItem == null) {
            TraceComponent tcc = Tr.register(StatsFactoryUtil.class, PmiConstants.TR_GROUP, PmiConstants.MSG_BUNDLE);
            Tr.warning(tcc, "PMI0103W", module.getName());
            throw new StatsFactoryException(nls.getFormattedMessage(
                                                                    "PMI0103W",
                                                                    new Object[] { module.getName() },
                                                                    "Unable to register custom PMI module due to duplicate name under the same parent or invalid PMI tree path: {0}"));
        }
        // MBean mapping
        if (msd != null) {
            setMBeanMapping(mItem, msd);
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "No MBean associated with " + module.getName());
        }
    }

    public static void setMBeanMapping(ModuleItem mItem, MBeanStatDescriptor msd) {
        // Map MI -> MSD
        PmiRegistry.setMBeanToModuleMap(mItem, msd);

        //done in PmiRegistry.setMBeanToModuleMap
        // Set the MSD reference in PmiAbstractModule
        //mItem.getInstance().setMBeanName (msd.getObjectName(), msd.getStatDescriptor());
    }

    public static void setMBeanMapping(ModuleItem mItem, ObjectName mBean) {
        MBeanStatDescriptor msd = new MBeanStatDescriptor(mBean);
        // Map MI -> MSD
        PmiRegistry.setMBeanToModuleMap(mItem, msd);

        //done in PmiRegistry.setMBeanToModuleMap
        // Set the MSD reference in PmiAbstractModule
        //mItem.getInstance().setMBeanName (msd.getObjectName());        
    }

    private static String getMBeanID(String[] path) {
        if (path == null || path.length == 0) {
            return null;
        }

        StringBuffer b = new StringBuffer(path[0]);
        for (int i = 1; i < path.length; i++) {
            b.append("#").append(path[i]);
        }

        return b.toString();
    }

    public static void unRegisterStats(PmiModule mod, ObjectName mbean) throws StatsFactoryException {
        PmiRegistry.unregisterModule(mod);
    }

    // if custom pmi module AND if the module has CustomStats MBean associated then deactivate it
    public static void deactivateMBean(ObjectName mbean) {
        /*
         * not needed since CustomStats MBean will not be created
         * if (mbean != null)
         * {
         * String type = mbean.getKeyProperty("type");
         * if (type.equals (StatsFactoryUtil.DEFAULT_MBEAN))
         * {
         * try
         * {
         * if(tc.isDebugEnabled())
         * Tr.debug(tc, "PMI0201I", mbean.getKeyProperty("name"));
         * 
         * AdminServiceFactory.getMBeanFactory().deactivateMBean (mbean);
         * }
         * catch (Exception e)
         * {
         * Tr.warning(tc, "PMI0106W", e.getMessage());
         * }
         * }
         * }
         */
    }

    public static void checkDataIDUniqueness(PmiModule parentMod, PmiModuleConfig cfg) throws StatsFactoryException {
        ModuleItem parent = PmiRegistry.findModuleItem(parentMod.getPath());

        while (parent != null && parent.getInstance() != null) {
            PmiModuleConfig pCfg = parent.getInstance().getModuleConfig();
            if (pCfg.getUID().equals(cfg.getUID())) {
                // skip
            } else {
                PmiDataInfo[] dInfo1 = pCfg.listAllData();
                PmiDataInfo[] dInfo2 = cfg.listAllData();

                for (int i = 0; i < dInfo1.length; i++) {
                    int id = dInfo1[i].getId();
                    for (int k = 0; k < dInfo2.length; k++) {
                        if (dInfo2[k].getId() == id) {
                            throw new StatsFactoryException(nls.getFormattedMessage(
                                                                                    "PMI0108W",
                                                                                    new Object[] { new Integer(id), pCfg.getUID() },
                                                                                    "Unable to register custom PMI module due to duplicate statistic id in the parent stats group/instance: ID={0}; ParentStats={1}"));
                        }
                    }
                }
            }

            parent = parent.getParent();
        }
    }
}
