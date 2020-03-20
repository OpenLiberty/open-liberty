/*******************************************************************************
 * Copyright (c) 2015,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.management.j2ee.internal;

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
import com.ibm.websphere.management.j2ee.JCAResourceMBean;
import com.ibm.ws.ffdc.FFDCFilter;

//TWAS Source: /runtime.fw/src/com/ibm/ws/runtime/component/collaborator/J2EEResourceMBean.java
/**
 * An MBean for a JCA Resource (JSR-77 JCAResource)
 * JCAResourceMBeanImpl is an MBean for JCA Resource. It implements the JCAResourceMBean interface.
 * This Mbean provide getters of information only. No information setter is provided.
 * 
 */
public class JCAResourceMBeanImpl extends StandardMBean implements JCAResourceMBean {

    /////////////////////////////////// Variables used in tracing. ///////////////////////////////////
    private static final TraceComponent tc = Tr.register(JCAResourceMBeanImpl.class, "WAS.j2c");
    private static final boolean IS_DEBUGGING = false; //Change to true if testing needed only.
    private static final String className = "JCAResourceMBeanImpl";

    ///////////////////////// Variables used in the registration of the MBean /////////////////////////
    private transient ServiceRegistration<?> reg = null;
    private transient ObjectName obn;
    private StringBuilder objectName = null;

    ///////////// Values retrieved from {@code ResourceAdapterMetaData} and used by the getters. /////////////
    private String name;
    private String resourceAdapter;
    private final ConcurrentHashMap<String, JCAConnectionFactoryMBeanImpl> cfMBeanChildrenList = new ConcurrentHashMap<String, JCAConnectionFactoryMBeanImpl>();

    ////////////////////////////////////// The constructor method //////////////////////////////////////
    /**
     * ResourceMBeanImpl is the construct responsible of populating the variables used by the getters
     * with information on the JCAResource which is given by {@code JCAMBeanRuntime}.
     * 
     * @param nameValue String representing the value of the XPath which as the same as config.displayId.
     * @param raValue String representing the parent MBean ResourceAdapter's value of its "name" key.
     * @param serverValue String representing the parent MBean J2EEServer's value of its "name" key.
     */
    public JCAResourceMBeanImpl(String nameValue,
                                String raValue,
                                String serverValue) {
        super(JCAResourceMBean.class, false);
        final String methodName = "JCAResourceMBeanImpl()";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, this);

        //Start building String which will be used in the {@code ObjectName}.
        objectName = new StringBuilder("WebSphere:type=JCAResourceMBean,j2eeType=JCAResource");

        if (nameValue != null && !nameValue.isEmpty()) {
            this.name = nameValue;
            objectName.append(",name=" + MBeanHelper.toObnString(this.name));
        }

        if (serverValue != null && !serverValue.isEmpty()) {
            objectName.append(",J2EEServer=" + serverValue);
        }

        // TODO - need to get the jca resource for this mbean.
        if (raValue != null && !raValue.isEmpty()) {
            this.resourceAdapter = raValue;
            objectName.append(",ResourceAdapter=" + MBeanHelper.toObnString(this.resourceAdapter));
        }
        //Example output of this ObjectName:
        // WebSphere:type=JCAResourceMBean,j2eeType=JCAResource,
        //           name=Xpathid,
        //           J2EEServer=xxx,
        //           ResourceAdapter=xxx

        //Building {@code ObjectName} with the String we concatenated.
        try {
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, methodName + " : objectName : " + objectName.toString(), this);
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

    protected String getJCAResourceName() {
        return name;
    }

    protected String getResourceAdapter() {
        return resourceAdapter;
    }

    /////////////////////////////////// Registration Methods ///////////////////////////////////

    public void register(BundleContext bndCtx) {
        final String methodName = "register()";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
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
        this.reg = bndCtx.registerService(JCAResourceMBeanImpl.class.getName(), this, props);
        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName, this);
    }

    public void unregister() {
        final String methodName = "unregister()";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, this);

        if (reg != null)
            reg.unregister();
        reg = null;
        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName, this);
    }

    ////////////////////////////////// New Methods /////////////////////////////////

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.management.j2ee.J2EEManagedObjectMBean#getobjectName()
     */
    @Override
    public String getobjectName() {
        return objectName.toString();
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
     * @see com.ibm.websphere.management.j2ee.JCAResourceMBean#getconnectionFactories()
     */
    @Override
    public String[] getconnectionFactories() {

        final String methodName = "getconnectionFactories()";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, this);

        final Collection<JCAConnectionFactoryMBeanImpl> c = cfMBeanChildrenList.values();
        final int size = c.size();
        final String[] result = new String[size];
        int index = 0;
        for (JCAConnectionFactoryMBeanImpl mbeanItem : c)
            result[index++] = mbeanItem.getobjectName();

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName, this);
        return result;
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
     * setConnectionFactoryChild add a child of type JCAConnectionFactoryMBeanImpl to this MBean.
     * 
     * @param key the String value which will be used as the key for the JCAConnectionFactoryMBeanImpl item
     * @param cf the JCAConnectionFactoryMBeanImpl value to be associated with the specified key
     * @return The previous value associated with key, or null if there was no mapping for key.
     *         (A null return can also indicate that the map previously associated null with key.)
     */
    protected JCAConnectionFactoryMBeanImpl setConnectionFactoryChild(String key, JCAConnectionFactoryMBeanImpl cf) {
        return cfMBeanChildrenList.put(key, cf);
    }

    /**
     * getConnectionFactoryChild get the child associated with the specified key if it exist.
     * 
     * @param key the String value which is used as the key for the JCAConnectionFactoryMBeanImpl item
     * @return The value to which the specified key is mapped, or null if this map contains no mapping for the key.
     */
    protected JCAConnectionFactoryMBeanImpl getConnectionFactoryChild(String key) {
        return cfMBeanChildrenList.get(key);
    }

    /**
     * removeConnectionFactoryChild get the child associated with the specified key if it exist, and then removes it from
     * the children list.
     * 
     * @param key the String value which is used as the key for the JCAConnectionFactoryMBeanImpl item
     * @return the previous value associated with key, or null if there was no mapping for key.
     *         (A null return can also indicate that the map previously associated null with key.)
     */
    protected JCAConnectionFactoryMBeanImpl removeConnectionFactoryChild(String key) {
        return cfMBeanChildrenList.remove(key);
    }

    /**
     * getConnectionFactoryChildrenCount() get the number of children of type JCAConnectionFactoryMBeanImpl for this MBean.
     * 
     * @return The number of key-value mappings in this map.
     */
    protected int getConnectionFactoryChildrenCount() {
        return cfMBeanChildrenList.size();
    }
}
