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

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.management.j2ee.JCAManagedConnectionFactoryMBean;
import com.ibm.ws.ffdc.FFDCFilter;

//TWAS Source: j2c.impl/src/com/ibm/ejs/j2c/mbeans/ConnectionFactoryMbeanImpl.java
/**
 * An MBean for a JCA Managed Connection Factory (JSR-77 JCAManagedConnectionFactory)
 * JCAManagedConnectionFactoryMBeanImpl is an MBean for JCA Managed Connection Factory. It implements the
 * JCAManagedConnectionFactoryMBean interface. This Mbean provide getters of information only.
 * No information setter is provided.
 * 
 */
public class JCAManagedConnectionFactoryMBeanImpl extends StandardMBean implements JCAManagedConnectionFactoryMBean {

    /////////////////////////////////// Variables used in tracing. ///////////////////////////////////
    private static final TraceComponent tc = Tr.register(JCAManagedConnectionFactoryMBeanImpl.class, "WAS.j2c");
    private static final boolean IS_DEBUGGING = false; //Change to true if testing needed only.
    private static final String className = "JCAManagedConnectionFactoryMBeanImpl";

    ///////////////////////////////// Variables used by the getters /////////////////////////////////
    private String name;

    ///////////////////////// Variables used in the registration of the MBean /////////////////////////
    private transient ServiceRegistration<?> reg = null;
    private transient ObjectName obn = null;
    private StringBuilder objectName = null;

    ////////////////////////////////////// The constructor method //////////////////////////////////////
    /**
     * JCAManagedConnectionFactoryMBeanImpl is the construct responsible of populating the variables used by the getters
     * with information on the JCAResource which is given by {@code ConnectionFactoryService}.
     * 
     * @param ramd is the {@code ResourceAdapterMetaData} object which contain the meta data information
     *            of the resource adapter.
     */
    public JCAManagedConnectionFactoryMBeanImpl(String nameValue,
                                                String serverValue) {
        super(JCAManagedConnectionFactoryMBean.class, false);
        final String methodName = "JCAManagedConnectionFactoryMBeanImpl()";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, this);

        //Start building String which will be used in the {@code ObjectName}.
        objectName = new StringBuilder("WebSphere:type=JCAManagedConnectionFactoryMBean,j2eeType=JCAManagedConnectionFactory");

        //Start adding the obtained values to the {@code ObjectName} if they are available.
        if (nameValue != null && !nameValue.isEmpty()) {
            objectName.append(",name=" + MBeanHelper.toObnString(nameValue));
            this.name = nameValue;
        }
        //Start adding the obtained values to the {@code ObjectName} if they are available.
        if (serverValue != null && !serverValue.isEmpty()) {
            objectName.append(",J2EEServer=" + MBeanHelper.toObnString(serverValue));
        }
        //Example output of this ObjectName:
        //WebSphere:type=JCAManagedConnectionFactoryMBean,j2eeType=JCAManagedConnectionFactory,
        //          name=Xpathid,
        //          J2EEServer=xxx

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
        this.reg = bndCtx.registerService(JCAManagedConnectionFactoryMBean.class.getName(), this, props);
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

    /////////////////////////////////// Getters Methods ///////////////////////////////////
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

    ///////////////////////////// Protected Getters  & Setters Methods ////////////////////////////
    /**
     * getName() obtain the value of the name key in the {@code ObjectName}
     * 
     * @return The String value of the name in this MBean's {@code ObjectName}
     */
    protected String getName() {
        return this.name;
    }
}
