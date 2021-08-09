/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.management.j2ee.internal;

import java.util.Collection;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.management.j2ee.JDBCResourceMBean;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * JDBCResourceMBeanImpl is an MBean for JDBCResource. It implements the JDBCResourceMBean interface.
 * This Mbean provide getters of information only. No information setter is provided.
 */
public class JDBCResourceMBeanImpl extends StandardMBean implements JDBCResourceMBean {

    /////////////////////////////////// Variables used in tracing ///////////////////////////////////
    private static final TraceComponent tc = Tr.register(JDBCResourceMBeanImpl.class, "RRA");
    private static final boolean IS_DEBUGGING = false; //Change to true if testing needed only.
    private static final String className = "JDBCResourceMBeanImpl";

    ///////////////////////////////// Variables used by the getters /////////////////////////////////
    private String name;
    private final ConcurrentHashMap<String, JDBCDataSourceMBeanImpl> dataSourceMBeanChildrenList = new ConcurrentHashMap<String, JDBCDataSourceMBeanImpl>();

    ///////////////////////// Variables used in the registration of the MBean /////////////////////////
    private ServiceRegistration<?> reg = null;
    private ObjectName obn = null;
    private StringBuilder objectName = null;

    //////////////////////////////////////The constructor method //////////////////////////////////////
    /**
     * JDBCResourceMBeanImpl is the construct responsible of populating the variables used by the getters
     * with information on the {@code JDBCResource} which is given by {@code JDBCMBeanRuntime}.
     * 
     * @param id String representing the value of the id of the DataSource assigned by the user.
     * @param jndiName String representing the value of the jnidiName of the DataSource assigned by the user.
     * @param name String representing the value of the XPath which as the same as config.displayId.
     * @param J2EEServer String representing the parent MBean J2EEServer's value of its "name" key.
     */
    protected JDBCResourceMBeanImpl(String id, String jndiName, String name, String J2EEServer) {
        super(JDBCResourceMBean.class, false);
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        final String methodName = "JDBCResourceMBeanImpl()";
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, this);
        if (trace && tc.isDebugEnabled())
            Tr.debug(tc, "\n *** Values provided to " + className + ": " + methodName + " ***"
                         + "\n String id: " + id
                         + "\n String jndiName: " + jndiName
                         + "\n String name: " + name
                         + "\n String J2EEServer: " + J2EEServer);

        //Start building String which will be used in the {@code ObjectName}.
        objectName = new StringBuilder("WebSphere:type=JDBCResourceMBean,j2eeType=JDBCResource");

        if (id != null && !id.isEmpty()) {
            objectName.append(",id=" + MBeanHelper.toObnString(id));
        }
        if (jndiName != null && !jndiName.isEmpty()) {
            objectName.append(",jndiName=" + MBeanHelper.toObnString(jndiName));
        }
        if (name != null && !name.isEmpty()) {
            objectName.append(",name=" + MBeanHelper.toObnString(name));
            this.name = name;
        }
        if (J2EEServer != null && !J2EEServer.isEmpty()) {
            objectName.append(",J2EEServer=" + MBeanHelper.toObnString(J2EEServer));
        }
        //Example output of this ObjectName:
        //WebSphere:type=JDBCResourceMBean,j2eeType=JDBCResource,
        //          id=xxx,
        //          jndiName=xxx,
        //          name=XPath,
        //          J2EEServer=xxx

        //Building {@code ObjectName} with the String we concatenated.
        try {
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, objectName.toString(), this);
            obn = new ObjectName(objectName.toString());
        } catch (MalformedObjectNameException e) {
            //Should never happen because we are cleaning user input with toObnString()
            FFDCFilter.processException(e, getClass().getName(), "[Exp: 4567]", this);
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, "Unable to create ObjectName with the string: " + objectName.toString(), e);
        } catch (NullPointerException e) {
            //Should never happen because we are building the ObjectName
            FFDCFilter.processException(e, getClass().getName(), "[Exp: 4568]", this);
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, "Unable to create ObjectName with a null string: " + objectName.toString(), e);
        }
        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName, this);
    }

    /////////////////////////////////// Registration Methods ///////////////////////////////////
    public void register(BundleContext bndCtx) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        final String methodName = "register()";
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, this);

        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put("jmx.objectname", this.obn.toString());

        if (IS_DEBUGGING)
            // Extra Check: Does this MBean exist already?
            MBeanHelper.isMbeanExist(objectName.toString() + "*", className, methodName);

        // Register the MBean
        if (trace && tc.isDebugEnabled())
            Tr.debug(tc, "activateMBean started for " + this.obn.toString());
        this.reg = bndCtx.registerService(JDBCResourceMBean.class.getName(), this, props);
        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName, this);
    }

    public void unregister() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        final String methodName = "unregister()";
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, this);

        if (reg != null)
            reg.unregister();
        reg = null;
        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName, this);
    }

    /////////////////////////////////// Public Getters Methods ///////////////////////////////////
    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.management.j2ee.J2EEManagedObjectMBean#getobjectName()
     */
    @Override
    public String getobjectName() {
        return this.obn.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.management.j2ee.J2EEManagedObjectMBean#isstateManageable()
     */
    @Override
    public boolean isstateManageable() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.management.j2ee.J2EEManagedObjectMBean#isstatisticsProvider()
     */
    @Override
    public boolean isstatisticsProvider() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.management.j2ee.J2EEManagedObjectMBean#iseventProvider()
     */
    @Override
    public boolean iseventProvider() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.management.j2ee.JDBCResourceMBean#getjdbcDataSources()
     */
    @Override
    public String[] getjdbcDataSources() {
        final String methodName = "getjdbcDataSources()";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, this);

        final Collection<JDBCDataSourceMBeanImpl> c = dataSourceMBeanChildrenList.values();
        final int size = c.size();
        final String[] result = new String[size];
        int index = 0;
        for (JDBCDataSourceMBeanImpl mbeanItem : c)
            result[index++] = mbeanItem.getobjectName();

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName, this);
        return result;
        //return dataSourcesList.toArray(new String[dataSourcesList.size()]);
    }

    ///////////////////////////// Protected Getters  & Setters Methods ////////////////////////////
    /**
     * getName() obtain the value of the name key in the {@code ObjectName}
     * 
     * @return The String value of the name in this MBean's {@code ObjectName}
     */
    protected String getName() {
        return this.name;
    }

    /**
     * setDataSourceChild add a child of type JDBCDataSourceMBeanImpl to this MBean.
     * 
     * @param key the String value which will be used as the key for the JDBCDataSourceMBeanImpl item
     * @param ds the JDBCDataSourceMBeanImpl value to be associated with the specified key
     * @return The previous value associated with key, or null if there was no mapping for key.
     *         (A null return can also indicate that the map previously associated null with key.)
     */
    protected JDBCDataSourceMBeanImpl setDataSourceChild(String key, JDBCDataSourceMBeanImpl ds) {
        return dataSourceMBeanChildrenList.put(key, ds);
    }

    /**
     * getDataSourceChild get the child associated with the specified key if it exist.
     * 
     * @param key the String value which is used as the key for the JDBCDataSourceMBeanImpl item
     * @return The value to which the specified key is mapped, or null if this map contains no mapping for the key.
     */
    protected JDBCDataSourceMBeanImpl getDataSourceChild(String key) {
        return dataSourceMBeanChildrenList.get(key);
    }

    /**
     * removeDataSourceChild get the child associated with the specified key if it exist, and then removes it from
     * the children list.
     * 
     * @param key the String value which is used as the key for the JDBCDataSourceMBeanImpl item
     * @return the previous value associated with key, or null if there was no mapping for key.
     *         (A null return can also indicate that the map previously associated null with key.)
     */
    protected JDBCDataSourceMBeanImpl removeDataSourceChild(String key) {
        return dataSourceMBeanChildrenList.remove(key);
    }

    /**
     * getDataSourceChildrenCount() get the number of children of type JDBCDataSourceMBeanImpl for this MBean.
     * 
     * @return The number of key-value mappings in this map.
     */
    protected int getDataSourceChildrenCount() {
        return dataSourceMBeanChildrenList.size();
    }

}
