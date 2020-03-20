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
import com.ibm.websphere.management.j2ee.JCAConnectionFactoryMBean;
import com.ibm.ws.ffdc.FFDCFilter;

//TWAS Source: j2c.impl/src/com/ibm/ejs/j2c/mbeans/ConnectionFactoryMbeanImpl.java
/**
 * An MBean for a JCA Connection Factory (JSR-77 JCAConnectionFactory)
 * JCAConnectionFactoryMBeanImpl is an MBean for JCA Connection Factory. It implements the
 * JCAConnectionFactoryMBean interface. This Mbean provide getters of information only.
 * No information setter is provided.
 * 
 */
public class JCAConnectionFactoryMBeanImpl extends StandardMBean implements JCAConnectionFactoryMBean {

    /////////////////////////////////// Variables used in tracing. ///////////////////////////////////
    private static final TraceComponent tc = Tr.register(JCAConnectionFactoryMBeanImpl.class, "WAS.j2c");
    private static final boolean IS_DEBUGGING = false; //Change to true if testing needed only.
    private static final String className = "JCAConnectionFactoryMBeanImpl";

    ///////////////////////// Variables used in the registration of the MBean /////////////////////////
    private transient ServiceRegistration<?> reg = null;
    private transient ObjectName obn = null;
    private StringBuilder objectName = null;

    ///////////// Values retrieved from gConfigProps & properties to be used by the getters. /////////////
    private String jndiName;
    private String name;
    private String id;
    private String mcf = "Value not available";

    ////////////////////////////////////// The constructor method //////////////////////////////////////
    /**
     * JCAConnectionFactoryMBeanImpl is the construct responsible of populating the variables used by the getters
     * with information on the {@code JCAConnectionFactory} which is given by {@code JDBCMBeanRuntime}.
     * 
     * @param idValue String representing the value of the id of the DataSource assigned by the user.
     * @param jndiNameValue String representing the value of the jnidiName of the DataSource assigned by the user.
     * @param nameValue String representing the value of the XPath which as the same as config.displayId.
     * @param mbeanServer String representing the parent MBean J2EEServer's value of its "name" key.
     * @param mcf String representing the value the MBean ManagedConnectionFactory's value of its "name" key.
     * @param jcaResourceName String representing the parent MBean JCAResource's value of its "name" key.
     */
    public JCAConnectionFactoryMBeanImpl(String idValue,
                                         String jndiNameValue,
                                         String nameValue,
                                         String mbeanServer,
                                         String mcf,
                                         String jcaResourceName) {
        super(JCAConnectionFactoryMBean.class, false);
        final String methodName = "JCAConnectionFactoryMBeanImpl()";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, this);

        this.mcf = mcf;

        //Start building String which will be used in the {@code ObjectName}.
        objectName = new StringBuilder("WebSphere:type=JCAConnectionFactoryMBean,j2eeType=JCAConnectionFactory");

        if (idValue != null && !idValue.isEmpty()) {
            this.id = idValue;
            objectName.append(",id=" + MBeanHelper.toObnString(this.id));
        }
        if (jndiNameValue != null && !jndiNameValue.isEmpty()) {
            this.jndiName = jndiNameValue;
            objectName.append(",jndiName=" + MBeanHelper.toObnString(this.jndiName));
        }
        if (nameValue != null && !nameValue.isEmpty()) {
            this.name = nameValue;
            objectName.append(",name=" + MBeanHelper.toObnString(this.name));
        }
        if (mbeanServer != null && !mbeanServer.isEmpty()) {
            objectName.append(",J2EEServer=" + MBeanHelper.toObnString(mbeanServer));
        }
        if (jcaResourceName != null && !jcaResourceName.isEmpty()) {
            objectName.append(",JCAResource=" + MBeanHelper.toObnString(jcaResourceName));
        }
        //Example output of this ObjectName:
        //WebSphere:type=JCAConnectionFactoryMBean,j2eeType=JCAConnectionFactory,
        //          id=xxx,
        //          jndiName=xxx,
        //          name=Xpathid,
        //          J2EEServer=xxx,
        //          JCAResource=xxx

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
        this.reg = bndCtx.registerService(JCAConnectionFactoryMBeanImpl.class.getName(), this, props);
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
     * @see com.ibm.websphere.management.j2ee.JCAConnectionFactoryMBean#getmanagedConnectionFactory()
     */
    @Override
    public String getmanagedConnectionFactory() {
        return this.mcf;
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
