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
import com.ibm.websphere.management.j2ee.JDBCDataSourceMBean;
import com.ibm.ws.ffdc.FFDCFilter;

//TWAS Source: /(SERV1)/j2c.impl/src/com/ibm/ejs/j2c/mbeans/JDBCDataSourceMBeanImpl.java 
/**
 * JDBCDataSourceMBeanImpl is an MBean for JDBCDataSource. It implements the JDBCDataSourceMBean interface.
 * This Mbean provide getters of information only. No information setter is provided.
 */
public class JDBCDataSourceMBeanImpl extends StandardMBean implements JDBCDataSourceMBean {

    /////////////////////////////////// Variables used in tracing. ///////////////////////////////////
    private static final TraceComponent tc = Tr.register(JDBCDataSourceMBeanImpl.class, "RRA");
    private static final boolean IS_DEBUGGING = false; //Change to true if testing needed only.
    private static final String className = "JDBCDataSourceMBeanImpl";

    ///////////////////////////////// Variables used by the getters /////////////////////////////////
    private String name;
    private String JDBCResource;

    ///////////////////////// Variables used in the registration of the MBean /////////////////////////
    private ServiceRegistration<?> reg = null;
    private StringBuilder objectName = null;
    private ObjectName obn = null;
    private String JDBCDriver = null;

    ////////////////////////////////////// The constructor method //////////////////////////////////////
    /**
     * JDBCDataSourceMBeanImpl is the construct responsible of populating the variables used by the getters
     * with information on the {@code DataSource} which is given by {@code JDBCMBeanRuntime}.
     * 
     * @param id String representing the value of the id of the DataSource assigned by the user.
     * @param jndiName String representing the value of the jnidiName of the DataSource assigned by the user.
     * @param name String representing the value of the XPath which as the same as config.displayId.
     * @param J2EEServer String representing the the parent MBean J2EEServer's value of its "name" key.
     * @param JDBCResource String representing the parent MBean JDBCResource's value of its "name" key.
     * @param JDBCDriver String representing the value of the JDBCDriver's {@code objectName} that identifies the
     *            JDBC driver for the corresponding JDBC data source.
     */
    protected JDBCDataSourceMBeanImpl(String id, String jndiName, String name, String J2EEServer, String JDBCResource, String JDBCDriver) {

        super(JDBCDataSourceMBean.class, false);//false because it's not MXBean.
        final String methodName = "JDBCDataSourceMBeanImpl()";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, this);
        if (trace && tc.isDebugEnabled())
            Tr.debug(tc, "\n *** Values provided to " + className + ": " + methodName + " ***"
                         + "\n String id: " + id
                         + "\n String jndiName: " + jndiName
                         + "\n String name: " + name
                         + "\n String J2EEServer: " + J2EEServer
                         + "\n String JDBCResource: " + JDBCResource
                         + "\n String JDBCDriver : " + JDBCDriver);

        this.JDBCDriver = JDBCDriver;

        //Start building String which will be used in the {@code ObjectName}.
        objectName = new StringBuilder("WebSphere:type=JDBCDataSourceMBean,j2eeType=JDBCDataSource");

        //Start adding the obtained values to the {@code ObjectName} if they are available.
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
        if (JDBCResource != null && !JDBCResource.isEmpty()) {
            objectName.append(",JDBCResource=" + MBeanHelper.toObnString(JDBCResource));
            this.JDBCResource = JDBCResource;
        }
        //Example output of this ObjectName:
        //WebSphere:type=JDBCDataSourceMBean,j2eeType=JDBCDataSource,
        //          id=xxx,
        //          jndiName=xxx,
        //          name=XPath,
        //          J2EEServer=xxx,
        //          JDBCResource=xxx

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
        this.reg = bndCtx.registerService(JDBCDataSourceMBean.class.getName(), this, props);
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
     * @see com.ibm.websphere.management.j2ee.JDBCDataSourceMBean#getjdbcDriver()
     */
    @Override
    public String getjdbcDriver() {
        return this.JDBCDriver;
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
     * getJDBCResource() obtain the value of the JDBCResource key in the {@code ObjectName}
     * 
     * @return The String value of the JDBCResource in this MBean's {@code ObjectName}
     */
    protected String getJDBCResource() {
        return this.JDBCResource;
    }
}
