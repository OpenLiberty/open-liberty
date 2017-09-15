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

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.management.j2ee.JDBCDriverMBean;
import com.ibm.ws.ffdc.FFDCFilter;

//TWAS Source: /SERV1/ws/code/j2c.impl/src/com/ibm/ejs/j2c/mbeans/JDBCDriverMbeanImpl.java
/**
 * JDBCDriverMBeanImpl is an MBean for a JDBCDriver. It implements the JDBCDriverMBean interface.
 * This Mbean provide getters of information only. No information setter is provided.
 */
public class JDBCDriverMBeanImpl extends StandardMBean implements JDBCDriverMBean {

    /////////////////////////////////// Variables used in tracing ///////////////////////////////////
    private static final TraceComponent tc = Tr.register(JDBCDriverMBeanImpl.class, "RRA");
    private static final boolean IS_DEBUGGING = false; //Change to true if testing needed only.
    private static final String className = "JDBCResourceMBeanImpl";

    ///////////////////////////////// Variables used by the getters /////////////////////////////////
    private String name;

    ///////////////////////// Variables used in the registration of the MBean /////////////////////////
    private ServiceRegistration<?> reg = null;
    private ObjectName obn = null;
    private StringBuilder objectName = null;

    //////////////////////////////////////The constructor method //////////////////////////////////////
    /**
     * JDBCDriverMbeanImpl is the construct responsible for populating the variables used by the getters
     * with information from the JDBCDriver which is given by {@code JDBCDriverService}.
     * 
     * @param id String representing the value of the id of the DataSource assigned by the user.
     * @param name String representing the value of the XPath which as the same as config.displayId.
     * @param J2EEServer String representing the parent MBean J2EEServer's value of its "name" key.
     */
    protected JDBCDriverMBeanImpl(String id, String name, String J2EEServer) {
        super(JDBCDriverMBean.class, false);
        final String methodName = "JDBCDriverMBeanImpl()";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, this);
        if (trace && tc.isDebugEnabled())
            Tr.debug(tc, "\n *** Values provided to " + className + ": " + methodName + " ***"
                         + "\n String id: " + id
                         + "\n String name: " + name
                         + "\n String J2EEServer: " + J2EEServer);

        //Start building String which will be used in the {@code ObjectName}.
        objectName = new StringBuilder("WebSphere:type=JDBCDriverMBean,j2eeType=JDBCDriver");

        if (id != null && !id.isEmpty()) {
            objectName.append(",id=" + MBeanHelper.toObnString(id));
        }
        if (name != null && !name.isEmpty()) {
            objectName.append(",name=" + MBeanHelper.toObnString(name));
            this.name = name;
        }
        if (J2EEServer != null && !J2EEServer.isEmpty()) {
            objectName.append(",J2EEServer=" + MBeanHelper.toObnString(J2EEServer));
        }
        //Example output of this ObjectName:
        //WebSphere:type=JDBCDriverMBean,j2eeType=JDBCDriver,
        //          id=xxx,
        //          name=XPath,
        //          J2EEServer=xxx

        //Building {@code ObjectName} with the String we concatenated.
        try {
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, objectName.toString(), this);
            obn = new ObjectName(objectName.toString());
        } catch (MalformedObjectNameException e) {
            FFDCFilter.processException(e, getClass().getName(), "[Exp: 4567]", this);
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, "Unable to create ObjectName with the string: " + objectName.toString(), e);
        } catch (NullPointerException e) {
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
        this.reg = bndCtx.registerService(JDBCDriverMBean.class.getName(), this, props);
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
        return this.objectName.toString();
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

    /**
     * getName() obtain the value of the name key in the {@code ObjectName}
     */
    protected String getName() {
        return this.name;
    }
}
