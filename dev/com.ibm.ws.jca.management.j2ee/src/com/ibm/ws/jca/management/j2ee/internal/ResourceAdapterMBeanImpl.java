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
import java.util.Iterator;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.management.j2ee.ResourceAdapterMBean;
import com.ibm.ws.ffdc.FFDCFilter;

//TWAS Source: /runtime.fw/src/com/ibm/ws/runtime/component/collaborator/J2EEResourceMBean.java
/**
 * An MBean for a JCA ResourceAdapter (JSR-77 ResourceAdapter)
 * ResourceAdapterMBeanImpl is an MBean for Resource Adapter. It implements the ResourceAdapterMBean interface.
 * This Mbean provide getters of information only. No information setter is provided.
 * 
 */
public class ResourceAdapterMBeanImpl extends StandardMBean implements ResourceAdapterMBean {

    /////////////////////////////////// Variables used in tracing. ///////////////////////////////////
    private static final TraceComponent tc = Tr.register(ResourceAdapterMBeanImpl.class, "WAS.j2c");
    private static final boolean IS_DEBUGGING = false; //Change to true if testing needed only.
    private static final String className = "ResourceAdapterMBeanImpl";

    ///////////////////////////////// Variables used by the getters /////////////////////////////////
    private String name;

    ///////////////////////// Variables used in the registration of the MBean /////////////////////////
    private ServiceRegistration<?> reg = null;
    private ObjectName obn = null;
    private StringBuilder objectName = null;
    private JCAMBeanRuntime jcaMBeanRuntime = null;
    private String resourceAdapter = null;

    ////////////////////////////////////// The constructor method //////////////////////////////////////
    /**
     * ResourceAdapterMBeanImpl is the construct responsible of populating the variables used by the getters
     * with information on the {@code ResourceAdapter} which is given by {@code JCAMBeanRuntime}.
     * 
     * @param idValue String representing the value of the id of the DataSource assigned by the user.
     * @param nameValue String representing the value of the XPath which as the same as config.displayId.
     * @param serverValue String representing the the parent MBean J2EEServer's value of its "name" key.
     * @param applicationValue String representing the parent MBean J2EEApplication's value of its "name" key.
     * @param moduleValue String representing the parent MBean ResourceAdapterModule's value of its "name" key.
     * @param jcaMBeanRuntime a reference to the {@code JCAMBeanRuntime} declarative services
     */
    public ResourceAdapterMBeanImpl(String idValue,
                                    String nameValue,
                                    String serverValue,
                                    String applicationValue,
                                    String moduleValue,
                                    JCAMBeanRuntime jcaMBeanRuntime) {
        super(ResourceAdapterMBean.class, false);
        final String methodName = "ResourceAdapterMBeanImpl()";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, this);

        this.jcaMBeanRuntime = jcaMBeanRuntime;
        this.resourceAdapter = idValue;

        //Start building the String which will be used in the {@code ObjectName}.
        objectName = new StringBuilder("WebSphere:type=ResourceAdapterMBean,j2eeType=ResourceAdapter");

        //Start adding the obtained values to the {@code ObjectName} if they are available.
        if (idValue != null && !idValue.isEmpty()) {
            objectName.append(",id=" + MBeanHelper.toObnString(idValue));
        }
        if (nameValue != null && !nameValue.isEmpty()) {
            objectName.append(",name=" + MBeanHelper.toObnString(nameValue));
            this.name = nameValue;
        }
        if (serverValue != null && !serverValue.isEmpty()) {
            objectName.append(",J2EEServer=" + MBeanHelper.toObnString(serverValue));
        }
        if (applicationValue != null && !applicationValue.isEmpty()) {
            objectName.append(",J2EEApplication=" + MBeanHelper.toObnString(applicationValue));
        }
        if (moduleValue != null && !moduleValue.isEmpty()) {
            objectName.append(",ResourceAdapterModule=" + MBeanHelper.toObnString(moduleValue));
        }
        //Example output of this ObjectName:
        //WebSphere:type=ResourceAdapterMBean,j2eeType=ResourceAdapter,
        //          id=xxx,
        //          name=Xpathid,
        //          J2EEServer=xxx,
        //          J2EEApplication=xxx,
        //          ResourceAdapterModule=xxx

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
        this.reg = bndCtx.registerService(ResourceAdapterMBean.class.getName(), this, props);
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

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.management.j2ee.ResourceAdapterMBean#getjcaResource()
     */
    @Override
    public String getjcaResource() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        Collection<JCAResourceMBeanImpl> c = jcaMBeanRuntime.jcaResources.values();
        Iterator<JCAResourceMBeanImpl> i = c.iterator();
        String jcaResourceString = null;
        while (i.hasNext()) {
            JCAResourceMBeanImpl jcaResource = i.next();
            String resourceAdapter = jcaResource.getResourceAdapter();
            String s = jcaResource.getobjectName().toString();
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, "getjcaResource - ", s);
            //ResourceAdapter
            if (this.resourceAdapter.equals(resourceAdapter)) {
                // I believe we should only have one that matches, if we have more, its likely a bug.
                // A resource adapter should only have one jcaResource and a jca jcaResource can
                // have several connection factories.   While debugging this code I see we have two
                // jcaResources for this resource adapter.
                jcaResourceString = s;
            }
        }
        return jcaResourceString;
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
