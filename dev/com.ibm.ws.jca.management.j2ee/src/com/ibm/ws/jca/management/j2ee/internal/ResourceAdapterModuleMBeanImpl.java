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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.management.j2ee.ResourceAdapterModuleMBean;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

//TWAS Source: /runtime.fw/src/com/ibm/ws/runtime/component/collaborator/J2EEResourceMBean.java
/**
 * An MBean for a JCA ResourceAdapterModule (JSR-77 ResourceAdapterModule)
 * ResourceAdapterModuleMBeanImpl is an MBean for Resource Adapter Module. It implements the ResourceAdapterModuleMBean
 * interface. This Mbean provide getters of information only. No information setter is provided.
 *
 */
public class ResourceAdapterModuleMBeanImpl extends StandardMBean implements ResourceAdapterModuleMBean {

    /////////////////////////////////// Variables used in tracing. ///////////////////////////////////
    private static final TraceComponent tc = Tr.register(ResourceAdapterModuleMBeanImpl.class, "WAS.j2c");
    private static final boolean IS_DEBUGGING = false; //Change to true if testing needed only.
    private static final String className = "ResourceAdapterModuleMBeanImpl";

    ///////////////////////// Variables used in the registration of the MBean /////////////////////////
    private transient ServiceRegistration<?> reg = null;
    private transient ObjectName obn = null;
    private StringBuilder objectName = null;

    ////////////////////////////// Variables used by the getter methods //////////////////////////////
    private String name;
    private String serverName = null;
    private URI raDDAddressFull = null;
    private String raDD = null;
    private boolean isReadFull = false;
    private final ConcurrentHashMap<String, ResourceAdapterMBeanImpl> raMBeanChildrenList = new ConcurrentHashMap<String, ResourceAdapterMBeanImpl>();

    ////////////////////////////////////// The constructor method //////////////////////////////////////
    /**
     * ResourceAdapterModuleMBeanImpl is the construct responsible of populating the variables used by the getters
     * with information on the ResourceAdapterModule which is given by {@code JCAMBeanRuntime}.
     *
     * @param idValue String representing the value of the id of the DataSource assigned by the user.
     * @param nameValue String representing the value of the XPath which as the same as config.displayId.
     * @param serverValue String representing the parent MBean J2EEServer's value of its "name" key.
     * @param applicationValue String representing the parent MBean J2EEApplication's value of its "name" key.
     * @param serverLocation String representing the full path to the directory of the server.
     * @param rarName String representing the name and extension of the RAR file.
     */
    public ResourceAdapterModuleMBeanImpl(String idValue, String nameValue, String serverValue, String applicationValue, URL fullPathToRAR) {
        super(ResourceAdapterModuleMBean.class, false);
        final String methodName = "ResourceAdapterModuleMBeanImpl()";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, this);

        try {
            if (fullPathToRAR != null)
                this.raDDAddressFull = fullPathToRAR.toURI();
            else if (trace && tc.isDebugEnabled())
                Tr.debug(tc, "Unable to get the URI from the URL: " + fullPathToRAR);
        } catch (URISyntaxException e) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, "Unable to get the URI from the URL: " + fullPathToRAR, e);
        }

        //Start building the String which will be used in the {@code ObjectName}.
        objectName = new StringBuilder("WebSphere:type=ResourceAdapterModuleMBean,j2eeType=ResourceAdapterModule");

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
            this.serverName = serverValue;
        }
        if (applicationValue != null && !applicationValue.isEmpty()) {
            objectName.append(",J2EEApplication=" + MBeanHelper.toObnString(applicationValue));
        }
        //Example output of this ObjectName:
        //WebSphere:type=ResourceAdapterModuleMBean,j2eeType=ResourceAdapterModule,
        //          id=xxx,
        //          name=Xpathid,
        //          J2EEServer=xxx,
        //          J2EEApplication=xxx

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
        this.reg = bndCtx.registerService(ResourceAdapterModuleMBean.class.getName(), this, props);
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
     * @see com.ibm.websphere.jca.mbean.ResourceAdapterModuleMBean#getdeploymentDescriptor()
     */
    @Override
    public String getdeploymentDescriptor() {
        final String methodName = "getdeploymentDescriptor";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, this);

        if (this.raDDAddressFull == null) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, "Unable to parse the Deployment Descriptor because the file location construction returned null."
                             + " raDDAddressFull = " + raDDAddressFull);
            if (trace && tc.isEntryEnabled())
                Tr.exit(tc, methodName, "Short Exit02. No DD was parsed.");
            return null;
        }

        if (this.raDD == null || !isReadFull)
            this.raDD = this.parseDDfile(this.raDDAddressFull);
        else if (trace && tc.isDebugEnabled())
            Tr.debug(tc, "Content of the ra.xml is retured from a previous parsing.");

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName, this);

        return raDD;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.jca.mbean.ResourceAdapterModuleMBean#getserver()
     */
    @Override
    public String getserver() {
        return this.serverName;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.jca.mbean.ResourceAdapterModuleMBean#getjavaVMs()
     */
    @Override
    public String[] getjavaVMs() {
        final String methodName = "getjavaVMs";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, this);

        ArrayList<String> jvmArrayList = new ArrayList<String>();
        ObjectName obnToFind = null;//WebSphere:name=JVM,J2EEServer=com.ibm.ws.jca.jdbc.mbean.fat,j2eeType=JVM
        final String obnStr = "WebSphere:j2eeType=JVM,*";
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            obnToFind = new ObjectName(obnStr);
        } catch (MalformedObjectNameException e) {
            //Should never happen because we are building the ObjectName without any user input
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, "Unable to create ObjectName with the string: " + obnStr, e);
        }
        Set<ObjectInstance> matchingMbeanList = mbs.queryMBeans(obnToFind, null);
        if (matchingMbeanList.size() == 0) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, methodName + ": matchingMbeanList.size(): " + matchingMbeanList.size()
                             + ", We didn't find any Mbean matching: " + obnStr);
            if (trace && tc.isEntryEnabled())
                Tr.exit(tc, methodName, this);
            return null;
        } else {
            for (ObjectInstance bean : matchingMbeanList) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, "**  Found a matching MBean: " + bean.getObjectName().toString());
                jvmArrayList.add(bean.getObjectName().toString());
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName, this);

        return jvmArrayList.toArray(new String[jvmArrayList.size()]);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.jca.mbean.ResourceAdapterModuleMBean#getresourceAdapters()
     */
    @Override
    public String[] getresourceAdapters() {
        final String methodName = "getresourceAdapters";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, this);

        final Collection<ResourceAdapterMBeanImpl> c = raMBeanChildrenList.values();
        final int size = c.size();
        final String[] result = new String[size];
        int index = 0;
        for (ResourceAdapterMBeanImpl mbeanItem : c)
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
     * setResourceAdapterChild add a child of type ResourceAdapterMBeanImpl to this MBean.
     *
     * @param key the String value which will be used as the key for the ResourceAdapterMBeanImpl item
     * @param ra the ResourceAdapterMBeanImpl value to be associated with the specified key
     * @return The previous value associated with key, or null if there was no mapping for key.
     *         (A null return can also indicate that the map previously associated null with key.)
     */
    protected ResourceAdapterMBeanImpl setResourceAdapterChild(String key, ResourceAdapterMBeanImpl ra) {
        return raMBeanChildrenList.put(key, ra);
    }

    /**
     * getResourceAdapterChild get the child associated with the specified key if it exist.
     *
     * @param key the String value which is used as the key for the ResourceAdapterMBeanImpl item
     * @return The value to which the specified key is mapped, or null if this map contains no mapping for the key.
     */
    protected ResourceAdapterMBeanImpl getResourceAdapterChild(String key) {
        return raMBeanChildrenList.get(key);
    }

    /**
     * removeResourceAdapterChild get the child associated with the specified key if it exist, and then removes it from
     * the children list.
     *
     * @param key the String value which is used as the key for the ResourceAdapterMBeanImpl item
     * @return the previous value associated with key, or null if there was no mapping for key.
     *         (A null return can also indicate that the map previously associated null with key.)
     */
    protected ResourceAdapterMBeanImpl removeResourceAdapterChild(String key) {
        return raMBeanChildrenList.remove(key);
    }

    /**
     * getResourceAdapterChildrenCount() get the number of children of type ResourceAdapterMBeanImpl for this MBean.
     *
     * @return The number of key-value mappings in this map.
     */
    protected int getResourceAdapterChildrenCount() {
        return raMBeanChildrenList.size();
    }

    //////////////////////////////////////// Private Methods ///////////////////////////////////////
    /**
     * parseDDfile will try to read the ra.xml and parse it into a String. If it fails, it will return null
     * or whatever it managed to read from the ra.xml. If the reading succeed partially, the returned value
     * will be tailed with a warning massage explaining what happened.
     *
     * @param raDDAddress String representing the full path to the directory of the RAR file.
     * @return The content of the ra.xml file as a String
     */
    private String parseDDfile(URI raDDAddress) {
        final String methodName = "parseDDfile";
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        boolean isIssue = false;
        StringWriter result = new StringWriter();

        final AtomicReference<Exception> exceptionRef = new AtomicReference<Exception>();
        final File file = new File(raDDAddress);
        BufferedInputStream bin = null;
        ZipFile zipFile = null;
        XMLStreamReader xmlStreamReader = null;

        try {
            zipFile = AccessController.doPrivileged(new PrivilegedAction<ZipFile>() {
                @Override
                public ZipFile run() {
                    try {
                        return new ZipFile(file);
                    } catch (FileNotFoundException e) {
                        exceptionRef.set(e);
                        return null;
                    } catch (IOException e) {
                        exceptionRef.set(e);
                        return null;
                    }
                }
            });
            if (exceptionRef.get() != null) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, className + ": " + methodName + ": Exception", exceptionRef.get());
                return null;
            }

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            String desiredFile = "META-INF/ra.xml";
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().equalsIgnoreCase(desiredFile))
                    bin = new BufferedInputStream(zipFile.getInputStream(entry));
            }

            if (bin == null) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, className + ": " + methodName + ": Could not find needed file: " + desiredFile);
                return null;
            }

            // Start reading the file
            xmlStreamReader = XMLInputFactory.newInstance().createXMLStreamReader(bin);
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new StAXSource(xmlStreamReader), new StreamResult(result));
        } catch (IOException e) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, methodName + ": Unable to parse the Deployment Descriptor because of IOException", e);
            isIssue = true; //We might have partial reading of the file
        } catch (XMLStreamException e) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, methodName + ": Unable to parse the Deployment Descriptor because of XMLStreamException", e);
            isIssue = true; //We might have partial reading of the file
        } catch (FactoryConfigurationError e) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, methodName + ": Unable to parse the Deployment Descriptor because of FactoryConfigurationError", e);
        } catch (TransformerConfigurationException e) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, methodName + ": Unable to parse the Deployment Descriptor because of TransformerConfigurationException", e);
        } catch (TransformerFactoryConfigurationError e) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, methodName + ": Unable to parse the Deployment Descriptor because of TransformerFactoryConfigurationError", e);
        } catch (TransformerException e) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, methodName + ": Unable to parse the Deployment Descriptor because of TransformerException", e);
        } finally {
            try {
                if (bin != null)
                    bin.close();
                if (zipFile != null)
                    zipFile.close();
                if (xmlStreamReader != null)
                    xmlStreamReader.close();
            } catch (IOException e) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, methodName + ": Unable to close the ZipFile or BufferedInputStream", e);
            } catch (XMLStreamException e) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, methodName + ": Unable to close the XMLStreamReader", e);
            }
        }
        if (isIssue) {
            isReadFull = false; // Will return the partial reading and try reading again on next request.
            if (!result.toString().isEmpty())
                result.append("...... [Error] File failed to be fully read");
        } else
            isReadFull = true;
        return result.toString();
    }
}
