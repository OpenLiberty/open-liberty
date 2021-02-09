/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.XATerminator;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAResource;

import com.ibm.ws.ejbcontainer.fat.rar.activationSpec.ActivationSpecImpl;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessageProviderImpl;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointFactoryWrapper;

/**
 * <p>An object of this class represents the FVT Resource Adapter instance. This class
 * should be a JavaBean.</p>
 *
 * <p>This class is also responsible for manage the endpoint factories instances. It
 * has the information of all activated endpoint instances.</p>
 */
public class FVTAdapterImpl implements ResourceAdapter, Serializable {
    private final static String CLASSNAME = FVTAdapterImpl.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private transient static String lineSeparator = System.getProperty("line.separator");

    /** A message provider object. */
    transient FVTMessageProviderImpl provider;

    /** Work manager instance from the application server. */
    transient WorkManager workManager;

    // Use XATerminatorWrapper instead of XATerminator directly so that xaTerm calls can be
    // intercepted.
    /** XATerminatorWrapper instance from the application server. */
    transient XATerminatorWrapper xaTerm;

    /** FVT adapter name */
    transient String adapterName;

    transient int abc;

    // @alvinso.2
    transient String propertyD;
    transient String propertyW;
    transient String propertyX;
    transient String propertyI;
    transient String ThreadContextPropagationRequired; // This is needed to enable the 'async work proxy' test

    transient BootstrapContext bootstrapConext;

    /** Specify whether running in Unit test mode or not */
    public transient boolean testMode = false;

    /**
     * Endpoint factories. This HashMap stores the endpoint factories plus its associated endpoint
     * application instances. Whenever an endpoint application is activated, its endpoint factory
     * is added to this HashMap.
     */
    transient protected ConcurrentHashMap messageFactories;

    private final String NAME_OF_MESSAGE_DRIVEN_EJB = "mdEJB";
    private final String WAS_SERVER_NAME = "WAS_SERVER_NAME";
    private final String WAS_TEMP_DIR = "WAS_TEMP_DIR";
    private final String SERVER_FILE_NAME = "serverRAisON.txt";

    // F743-7048
    /**
     * Mapping of test-supplied 'name' to the server supplied
     * MessageEndpointFactory instance.
     *
     * When a MDB is activated, the resource associates that MDB with a
     * MessageEndpointFactory and an ActivationSpec, and it passes those
     * two items into the .endpointActivation() method on this RA instance.
     *
     * In our testing, we need to get at the MessageEndpoint instance for our
     * MDB, so we can then use that MessageEndpoint instance to send messages
     * to the MDB and drive through various paths in the ejbcontainer code.
     *
     * The server supplied MessageEndpoint instance comes from the server supplied
     * MessageEndpointFactory instance, and so at some future point in our testing
     * we go into this map, and dig out the needed MessageEndpointFactory.
     *
     * The key into this map is an arbitrary 'name' value, which we get from the
     * passed in server supplied ActivationSpec instance. The 'name' value gets
     * into the ActivationSpec instance because its explicitly called out as an
     * <activation-config-property> element in the deployment descriptor stanza
     * that defines the MDB.
     *
     * Note also that this static map is actually getting updated by two different
     * instances of this class. The EJB FAT is configured (at current time) to
     * create two different resource adapter instances (one of them is configured to
     * be JCA 1.5, the other JCA 1.6)...and both of them share this same RA implementation
     * class. As a result, when the two MDBs (again, one for each flavor of the adapter)
     * are activated, the .activateEndpoint() method will get called into on each
     * RA instance, and so they will both update the same static location in memory.
     */
    public static HashMap<String, MessageEndpointFactory> messageEndpointFactoryMap = new HashMap<String, MessageEndpointFactory>();

    /**
     * Default constructor
     */
    public FVTAdapterImpl() {
        svLogger.info("<init>");

        // @alvinso.2
        propertyD = "6";
        propertyW = "6";
        propertyX = "6";
        propertyI = "6";

        //must have a default value of false because it drives J2C processing, and in most cases we want to drive
        //J2C down the 'false' path.  In the one case where we want to drive J2C down the 'true' path (the async work proxy case)
        //we explicitly override the default value and set this to be true via the resources.xml file/setter() methods
        ThreadContextPropagationRequired = "false";
    }

    /**
     * <p>This is called when a resource adapter instance is bootstrapped. This may be during
     * resource adapter deployment or application server startup. This is a startup notification
     * from the application server, and this method is called by an application server thread.
     * The application server thread executes in an unspecified context. </p>
     *
     * <p>During this method call a the resource adapter instance gets the work manager instance
     * and XA terminator instance from the bootstrap context. </p>
     *
     * @param context a bootstrap context containing references to useful facilities that
     *            could be used by a resource adapter instance.
     *
     * @exception ResourceAdapterInternalException indicates bootstrap failure. The resource
     *                adapter instance is unusable and must be discarded.
     */
    @Override
    public void start(BootstrapContext context) throws ResourceAdapterInternalException {
        svLogger.info("Inside the .start() method for SA RA...");
        svLogger.entering(CLASSNAME, "start", new Object[] { this, context });
        bootstrapConext = context;
        workManager = context.getWorkManager();
        svLogger.info("workManager: " + workManager);

        // Wrap the xaTerm from the context with XATerminatorWrapper so that
        // TRA can intercept the xaTerminator calls.
        xaTerm = new XATerminatorWrapper(context.getXATerminator());
        svLogger.info("xaTerm: " + xaTerm);

        // initialize messageFactories
        messageFactories = new ConcurrentHashMap(3);
        svLogger.info("TRA " + adapterName + " starts successfully");

        // @alvinso.2
        svLogger.info("TRA's propertyD = " + propertyD);
        svLogger.info("TRA's propertyW = " + propertyW);
        svLogger.info("TRA's propertyX = " + propertyX);
        svLogger.info("TRA's propertyI = " + propertyI);

        FVTAdapterVerifyImpl.setAdapterName(adapterName);
        FVTAdapterVerifyImpl.setPropertyD(propertyD);
        FVTAdapterVerifyImpl.setPropertyI(propertyI);
        FVTAdapterVerifyImpl.setPropertyW(propertyW);
        FVTAdapterVerifyImpl.setPropertyX(propertyX);

        svLogger.exiting(CLASSNAME, "start");
    }

    /**
     * <p>This is called when a resource adapter instance is undeployed or during application
     * server shutdown. This is a shutdown notification from the application server, and
     * this method is called by an application server thread. The application server thread
     * executes in an unspecified context. </p>
     */
    @Override
    public void stop() {
        svLogger.entering(CLASSNAME, "stop", this);
        workManager = null;
        xaTerm = null;
        provider = null;
        svLogger.info("TRA " + adapterName + " stops successfully");
        adapterName = null;
        svLogger.exiting(CLASSNAME, "stop");
    }

    /**
     * <p>This is called during the activation of a message endpoint. This causes the
     * resource adapter instance to do the necessary setup. </p>
     *
     * @param endpointFactory a message endpoint factory instance.
     * @param spec an activation spec JavaBean instance.
     *
     * @exception NotSupportedException indicates message endpoint activation rejection
     *                due to incorrect activation setup information.
     */
    @Override
    public void endpointActivation(MessageEndpointFactory factory, ActivationSpec spec) throws NotSupportedException {
        svLogger.info("Entering endpointActivation() for ejb RA...");
        svLogger.entering(CLASSNAME, "endpointActivation", new Object[] { this, factory, spec,
                                                                          ((ActivationSpecImpl) spec).introspectSelf() }); // d180125

        // d180125
        svLogger.info("If factory is null, ensure EJB module is 2.1. Check ejb-jar.xml");

        // This is the logic that take some piece of information from the passed in J2C-ActivationSpec (in this case,
        // the 'name' attribute) and associates it with the passed MessageEndpointFactory so that we can find it at
        // a later time when we actually want to create the pipe to the MDB.
        //
        // In this case, we extract the 'name' from the passed in J2C-ActivationSpec, and we throw that into a hashtable
        // as the 'key'...and we create a MessageEndpointFactoryWrapper class (which is just a container class holding other objects),
        // which contains the specified MessageEndpointFactory, the specified J2C-ActivationSpec, and this resource adapter class instance,
        // throws that into the hashtable as the associated 'value'.
        //
        // This allows us to come along at some point in the future, during a test, and specify a 'name'...which we then
        // use as the key into this local hashtable variable...which gives us the associated MessageEndpointFactoryWrapper object...
        // from which we extract the MessageEndpointFactory object...which we then get a 'pipe' from.
        //
        // All of the logic in this method above this is still that is NOT needed to activate an endpoint.  (In my mind,
        // activating an endpoint means creating some kind of mechanism that allows you to find it later when you actually need it.)
        // Rather, its simply used to persist information to global storage objects so that tests can come along at some
        // future point and verify if the data was - or was not - passed in.

        // construct the factory wrapper object
        MessageEndpointFactoryWrapper factoryWrapper = new MessageEndpointFactoryWrapper(this, factory, spec);

        String endpointName = ((ActivationSpecImpl) spec).getName();

        if (messageFactories.get(endpointName) != null) {
            svLogger.info("The end point factory with the name " + endpointName + "already exists, mapping it under " + endpointName + "+");
            endpointName += "+";
        }

        // Add the factory to messagefactories.
        svLogger.info("Adding MessageEndpointFactory for endpoint named **" + endpointName + "**");
        messageFactories.put(endpointName, factoryWrapper);

        // Write the name of the server that this RA is running on to a log.
        // We do this so that a test can determine which server - out of the many that exist in a cluster -
        // actually had its RA started.  We only write this if we actually need it...otherwise, the method no-ops.
        writeServerNameThatThisRAIsRunningOnToLog(endpointName);

        // F743-7048
        // Shove the name/MessageEndpointFactory into the global map, so our test
        // can get at the MessageEndpointFactory at some future point in time.
        svLogger.info("Added the MessageEndpointFactory to the global map, via RA instance " + this.hashCode());
        messageEndpointFactoryMap.put(endpointName, factory);
        svLogger.exiting(CLASSNAME, "endpointActivation");
    }

    /**
     * <p>This is called when a message endpoint is deactivated. The instances passed
     * as arguments to this method call should be identical to those passed in for
     * the corresponding endpointActivation call. This causes the resource adapter to
     * stop delivering messages to the message endpoint. </p>
     * = *
     *
     * @param endpointFactory a message endpoint factory instance.
     * @param spec - an activation spec JavaBean instance.
     */
    @Override
    public void endpointDeactivation(MessageEndpointFactory factory, ActivationSpec spec) {
        svLogger.entering(CLASSNAME, "endpointDeactivation", new Object[] { this, factory, spec });

        // remove the endpoint factory
        // Pass the spec so we can validate the spec is the same spec when remove the factory instance
        removeMessageFactory(factory, spec);
        svLogger.exiting(CLASSNAME, "endpointDeactivation");
    }

    /**
     * <p>This method is called by the application server during crash recovery. This
     * method takes in an array of ActivationSpec JavaBeans and returns an array of
     * XAResource objects each of which represents a unique resource manager. The
     * resource adapter may return null if it does not implement the XAResource
     * interface. Otherwise, it must return an array of XAResource objects, each of
     * which represents a unique resource manager that was used by the endpoint
     * applications. The application server uses the XAResource objects to query
     * each resource manager for a list of in-doubt transactions. It then completes
     * each pending transaction by sending the commit decision to the participating
     * resource managers. </p>
     *
     * @param specs an array of ActivationSpec JavaBeans each of which corresponds
     *            to an deployed endpoint application that was active prior to the system crash.
     *
     * @return an array of XAResource objects each of which represents a unique
     *         resource manager.
     *
     * @exception ResourceException generic exception if operation fails due to an
     *                error condition.
     */
    @Override
    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        //alvinso.1
        FVTXAResourceImpl xaResource = new FVTXAResourceImpl();
        return new FVTXAResourceImpl[] { xaResource };
    }

    /**
     * <p>Remove the message factory during the endpoint application deactivation.</p>
     *
     * @param factory a message endpoint factory instance.
     * @param spec an activation spec JavaBean instance.
     */
    private void removeMessageFactory(MessageEndpointFactory factory, ActivationSpec spec) {
        svLogger.entering(CLASSNAME, "removeMessageFactory", new Object[] { this, factory, spec });
        String key = matchMessageFactory(factory);

        if (key == null) {
            svLogger.info("Cannot find the factory " + factory + ". Continue processing.");
            throw new RuntimeException("Cannot find the factory " + factory + ". Continue processing.");
        } else {
            messageFactories.remove(key);
            svLogger.info("Factory is successfully removed.");
        }

        svLogger.exiting(CLASSNAME, "removeMessageFactory");
    }

    /**
     * <p>Find the message endpoint factory key</p>
     *
     * @param factory a message endpoint factory instance.
     */
    private String matchMessageFactory(MessageEndpointFactory factory) {
        svLogger.entering(CLASSNAME, "matchMessageFactory", new Object[] { this, factory });
        String key = null;

        Collection factories = messageFactories.entrySet();
        if (factories != null) {
            // Iterate the HashMap to match the endpoint factory
            for (Iterator iter = factories.iterator(); iter.hasNext();) {
                Map.Entry map = (Map.Entry) iter.next();
                MessageEndpointFactoryWrapper wrapper = (MessageEndpointFactoryWrapper) map.getValue();

                if (factory.equals(wrapper.getFactory())) {
                    key = (String) map.getKey();
                    return key;
                }
            }
        }

        svLogger.exiting(CLASSNAME, "matchMessageFactory", key);
        return key;
    }

    //---------------------------------------------------------------------------------
    // getters and setters
    //---------------------------------------------------------------------------------

    /**
     * Returns the message provider.
     *
     * @return the message provider object
     */
    public FVTMessageProviderImpl getProvider() {
        return provider;
    }

    // Need to add setProvider so that I can associate provider object
    // to the adapter object. Then Adapter can check the status of provider
    // (EIS)
    /**
     * Set the message provider.
     */
    public void setProvider(FVTMessageProviderImpl provider) {
        this.provider = provider;
    }

    /**
     * Returns the work manager from the application server.
     *
     * @return the work manager
     */
    public WorkManager getWorkManager() {
        return workManager;
    }

    /**
     * Returns the xa terminator from the application server.
     *
     * @return the xa terminator from the application server
     */
    public XATerminator getXaTerm() {
        return xaTerm;
    }

    /**
     * Returns the adapter name.
     *
     * @return the adapter name
     */
    public String getAdapterName() {
        return adapterName;
    }

    /**
     * Sets the adapter name.
     *
     * @param adapterName The adapterName to set
     */
    public void setAdapterName(String adapterName) {
        this.adapterName = adapterName;
    }

    /**
     * Returns the abc.
     *
     * @return int
     */
    public int getAbc() {
        return abc;
    }

    /**
     * Sets the abc.
     *
     * @param abc The abc to set
     */
    public void setAbc(int abc) {
        this.abc = abc;
    }

    /**
     * Returns the messageFactories.
     *
     * @return HashMap
     */
    public ConcurrentHashMap getMessageFactories() {
        return messageFactories;
    }

    public String introspectSelf() {
        StringBuffer str = new StringBuffer();
        str.append("Adapter instance : "
                   + lineSeparator
                   + "Adapter name     : "
                   + adapterName
                   + lineSeparator
                   + "Provider         : "
                   + provider
                   + lineSeparator
                   + lineSeparator
                   + "workManager      : "
                   + workManager
                   + lineSeparator
                   + "xaTerm           : "
                   + xaTerm
                   + lineSeparator);

        str.append("Factories:" + lineSeparator);
        Collection factories = messageFactories.entrySet();
        if (factories != null) {
            for (Iterator iter = factories.iterator(); iter.hasNext();) {
                Map.Entry map = (Map.Entry) iter.next();
                String iKey = (String) map.getKey();
                MessageEndpointFactoryWrapper factory = (MessageEndpointFactoryWrapper) map.getValue();
                str.append(iKey + "={" + factory + "}" + lineSeparator);
            }
        } else {
            str.append("No factories" + lineSeparator);
        }

        return str.toString();
    }

    /**
     * Returns the bootstrapConext.
     *
     * @return BootstrapContext
     */
    public BootstrapContext getBootstrapConext() {
        return bootstrapConext;
    }

    // @alvinso.2
    /**
     * @return
     */
    public String getPropertyD() {
        svLogger.info("inside .getPropertyD()");
        return propertyD;
    }

    /**
     * @return
     */
    public String getPropertyI() {
        svLogger.info("inside .getPropertyI()");
        return propertyI;
    }

    /**
     * @return
     */
    public String getPropertyW() {
        svLogger.info("inside .getPropertyW()");
        return propertyW;
    }

    /**
     * @return
     */
    public String getPropertyX() {
        svLogger.info("inside .getPropertyX()");
        return propertyX;
    }

    /**
     * @param string
     */
    public void setPropertyD(String string) {
        svLogger.info("inside .setPropertyD()");
        propertyD = string;
    }

    /**
     * @param string
     */
    public void setPropertyI(String string) {
        svLogger.info("inside .setPropertyI()");
        propertyI = string;
    }

    /**
     * @param string
     */
    public void setPropertyW(String string) {
        svLogger.info("inside .setPropertyW()");
        propertyW = string;
    }

    /**
     * @param string
     */
    public void setPropertyX(String string) {
        svLogger.info("inside .setPropertyX()");
        propertyX = string;
    }

    @Override
    public boolean equals(Object someResourceAdapter) {
        svLogger.info("Inside the .equals() method of SA RA...");
        try {
            // say false if the passed in object is null
            if (someResourceAdapter == null) {
                svLogger.info("The passed in ResourceAdapter was null. This means it can't equal the current ResourceAdapter instance.");
                return false;
            }

            // Determine if the passed in object is an instance of something we consider an equal
            String className = someResourceAdapter.getClass().getName();
            if ("com.ibm.ws.ejbcontainer.fat.rar.core.FVTAdapterImpl".equalsIgnoreCase(className)) {
                // it's an FVTAdapterImpl...this is considered an equal...return true
                svLogger.info("The passed in RA was a FVTAdapterImpl...the RA's are equal.");
                return true;
            } else if ("com.ibm.ws.ejbcontainer.fat.rar.core.EmbeddedFVTAdapterImpl".equalsIgnoreCase(className)) {
                // it's an EmbeddedFVTAdapterImpl...this is considered an equal...return true
                svLogger.info("The passed in RA was a EmbeddedFVTAdapterImpl...the RA's are equal.");
                return true;
            } else {
                // it's neither of the above...so consider it not equal
                svLogger.info("The passed in RA was a **" + className + "**.  The RAs are NOT equal.");
                return false;
            }
        } catch (Throwable e) {
            svLogger.info("We got some kind of error trying to determine if the ResourceAdapter instances " +
                          "are equal.  We'll interpret this to mean they are NOT equal.  The error was:\n" + e);
            return false;
        }
    }

    private void writeServerNameThatThisRAIsRunningOnToLog(String endpointName) {
        if (NAME_OF_MESSAGE_DRIVEN_EJB.equalsIgnoreCase(endpointName)) {
            svLogger.info("We ARE activating the endpoint for **" + endpointName + "**, therefore we ARE logging the server that the RA is running on.");
            OutputStreamWriter osw = null;
            FileOutputStream fos = null;
            try {
                // Get the environment variables that tell us what server we are running on, and where to write the log to
                String serverRAIsRunningOn = System.getProperty(WAS_SERVER_NAME);
                String logPathToWriteServerNameTo = System.getProperty(WAS_TEMP_DIR);
                String filePath = logPathToWriteServerNameTo + "/" + SERVER_FILE_NAME;
                svLogger.info("WAS server is: **" + serverRAIsRunningOn + "**");
                svLogger.info("WAS RA log path is: **" + logPathToWriteServerNameTo + "**");

                File file = new File(filePath);
                PrintStream printStream = new PrintStream(file);
                printStream.print(serverRAIsRunningOn);
                boolean didWeHaveError = printStream.checkError();
                svLogger.info("check error value is: **" + didWeHaveError + "**");
                printStream.close();
            } catch (Throwable e) {
                // We catch and bury any exceptions here so we don't kill endpoint deployment because we couldn't determine and write the server
                // for some reason. Only one test relies on this, so it doesn't make sense to jeopardize all other function because of it.
                svLogger.info("Failed to write the server that the RA is running on to the log. The error was:\n");
                e.printStackTrace(System.out);
            }
        } else {
            // We are not activating the endpoint for the specific EJB...therefore, we don't bother updating the log
            svLogger.info("Activating endpoint for **" + endpointName + "**, so we are NOT logging the server that the RA is running on.");
        }
    }

    public void setThreadContextPropagationRequired(String string) {
        svLogger.info("inside .setThreadContextPropagationRequired()");
        ThreadContextPropagationRequired = string;
    }

    public String getThreadContextPropagationRequired() {
        svLogger.info("inside .getThreadContextPropagationRequired()");
        return ThreadContextPropagationRequired;
    }

    public String getActivationName(String name) {
        return messageEndpointFactoryMap.get(name).getActivationName();
    }
}