/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.HashMap;

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

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * <p>
 * An object of this class represents the FVT Resource Adapter instance. This
 * class should be a JavaBean.
 * </p>
 *
 * <p>
 * This class is also responsible for manage the endpoint factories instances.
 * It has the information of all activated endpoint instances.
 * </p>
 */
public class FVTAdapterImpl implements ResourceAdapter, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private transient static final TraceComponent tc = Tr
                    .register(FVTAdapterImpl.class);
    private transient static String lineSeparator = System
                    .getProperty("line.separator");

    /** Work manager instance from the application server. */
    transient WorkManager workManager;

    // d177221: swai: 09/22/03
    // Use XATerminatorWrapper instead of XATerminator directly so that xaTerm
    // calls can be
    // intercepted.
    // /** XATerminator instance from the application server.*/
    // transient XATerminator xaTerm;
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
    transient String propertyPM26175; // Added for PM26175
    transient String ThreadContextPropagationRequired; // This is needed to
                                                       // enable the 'asycn
                                                       // work proxy' test

    transient BootstrapContext bootstrapContext;

    /** Specify whether running in Unit test mode or not */
    public transient boolean testMode = false;

    /**
     * Endpoint factories. This HashMap stores the endpoint factories plus its
     * associated endpoint application instances. Whenever an endpoint
     * application is activated, its endpoint factory is added to this Hashmap.
     */
    transient protected HashMap messageFactories;

    private final String NAME_OF_MESSAGE_DRIVEN_EJB = "mdEJB";
    private final String WAS_SERVER_NAME = "WAS_SERVER_NAME";
    private final String WAS_TEMP_DIR = "WAS_TEMP_DIR";
    private final String SERVER_FILE_NAME = "serverRAisON.txt";

    public static HashMap raName = new HashMap();

    /**
     * Default constructor
     */
    public FVTAdapterImpl() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "<init>");

        propertyD = "6";
        propertyPM26175 = "VERSION1";
        propertyW = "6";
        propertyX = "6";
        propertyI = "6";

        FVTAdapterVerifyImpl.setAdapterName(adapterName);
        FVTAdapterVerifyImpl.setPropertyD(propertyD);
        FVTAdapterVerifyImpl.setPropertyD(propertyPM26175);
        FVTAdapterVerifyImpl.setPropertyW(propertyW);
        FVTAdapterVerifyImpl.setPropertyX(propertyX);
        FVTAdapterVerifyImpl.setPropertyI(propertyI);

        // must have a default value of false because it drives J2C processing,
        // and in most cases we want to drive
        // J2C down the 'false' path. In the one case where we want to drive J2C
        // down the 'true' path (the async work proxy case)
        // we explicitly override the default value and set this to be true via
        // the resources.xml file/setter() methods
        ThreadContextPropagationRequired = "false";

    }

    /**
     * <p>
     * This is called when a resource adapter instance is bootstrapped. This may
     * be during resource adapter deployment or application server startup. This
     * is a startup notification from the application server, and this method is
     * called by an application server thread. The application server thread
     * executes in an unspecified context.
     * </p>
     *
     * <p>
     * During this method call a the resource adapter instance gets the work
     * manager instance and XA terminator instance from the bootstrap context.
     * </p>
     *
     * @param context
     *                    a bootstrap context containing references to useful facilities
     *                    that could be used by a resource adapter instance.
     *
     * @exception ResourceAdapterInternalException
     *                                                 indicates bootstrap failure. The resource adapter instance
     *                                                 is unusable and must be discarded.
     */
    @Override
    public void start(BootstrapContext context) throws ResourceAdapterInternalException {
        Tr.debug(tc, "Inside the .start() method for SA RA...");
        if (tc.isEntryEnabled())
            Tr.entry(tc, "start", new Object[] { this, context });

        bootstrapContext = context;
        raName.put(this.getAdapterName(), this);

        workManager = context.getWorkManager();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "start", workManager);

        // d177221: swai: 09/22/03
        // Wrap the xaTerm from the context with XATerminatorWrapper so that
        // TRA can intercept the xaTerminator calls.
        xaTerm = new XATerminatorWrapper(context.getXATerminator());

        if (tc.isDebugEnabled())
            Tr.debug(tc, "start", xaTerm);

        // initialize messageFactories
        messageFactories = new HashMap(3);

        Tr.debug(tc, "TRA " + adapterName + " starts successfully");

        if (tc.isEntryEnabled())
            Tr.exit(tc, "start");

    }

    /**
     * <p>
     * This is called when a resource adapter instance is undeployed or during
     * application server shutdown. This is a shutdown notification from the
     * application server, and this method is called by an application server
     * thread. The application server thread executes in an unspecified context.
     * </p>
     */
    @Override
    public void stop() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "stop", this);

        workManager = null;
        xaTerm = null;

        Tr.debug(tc, "TRA " + adapterName + " stops successfully");
        adapterName = null;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "stop");
    }

    /**
     * <p>
     * This is called during the activation of a message endpoint. This causes
     * the resource adapter instance to do the necessary setup.
     * </p>
     *
     * @param endpointFactory
     *                            a message endpoint factory instance.
     * @param spec
     *                            an activation spec JavaBean instance.
     *
     * @exception NotSupportedException
     *                                      indicates message endpoint activation rejection due to
     *                                      incorrect activation setup information.
     */
    @Override
    public void endpointActivation(MessageEndpointFactory factory,
                                   ActivationSpec spec) throws NotSupportedException {

        System.out.println("Entering endpointActivation() for SA RA...");
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "endpointActivation", new Object[] { this, factory,
                                                              spec, ((ActivationSpecImpl) spec).introspectSelf() }); // d180125

            // d180125
            Tr
                            .debug(tc,
                                   "If factory is null, ensure EJB module is 2.1. Check ejb-jar.xml");
        }

        // 04/16/04: swai
        // New code for ActivationSpecAlias test case by Cathy.
        if (spec instanceof ActivationSpecAuthImpl) {
            Tr.debug(tc,
                     "This is an ActivationSpecAuthImpl, set user id and pw");
            ActivationSpecAuthImpl authSpec = (ActivationSpecAuthImpl) spec;
            String userName = authSpec.getUserName();
            String password = authSpec.getPassword();
            Tr.debug(tc, "UserName: " + userName + " Password: " + password);
            int testVar = authSpec.getTestVariation();
            switch (testVar) {
                case 1:
                    Tr.debug(tc, "Setting user id and pw for test variation 1");
                    ActivationSpecVerifyImpl.setVariation1(userName, password);
                    // The following needs to go into the test program, but
                    // I'm putting it here for now to test it
                    if (ActivationSpecVerifyImpl.testVariation1()) {
                        Tr.debug(tc, "Test variation 1 passed");
                    } else {
                        Tr.debug(tc, "Test variation 1 failed");
                    }
                    break;
                case 2:
                    Tr.debug(tc, "Setting user id and pw for test variation 1");
                    ActivationSpecVerifyImpl.setVariation2(userName, password);
                    // The following needs to go into the test program, but
                    // I'm putting it here for now to test it
                    if (ActivationSpecVerifyImpl.testVariation2()) {
                        Tr.debug(tc, "Test variation 2 passed");
                    } else {
                        Tr.debug(tc, "Test variation 2 failed");
                    }
                    break;
                case 3:
                    Tr.debug(tc, "Setting user id and pw for test variation 1");
                    ActivationSpecVerifyImpl.setVariation3(userName, password);
                    // The following needs to go into the test program, but
                    // I'm putting it here for now to test it
                    if (ActivationSpecVerifyImpl.testVariation3()) {
                        Tr.debug(tc, "Test variation 3 passed");
                    } else {
                        Tr.debug(tc, "Test variation 3 failed");
                    }
                    break;
            }
        }

        /*
         * try { validate(factory, spec); } catch (NotSupportedException nse) {
         * if (tc.isEntryEnabled()) Tr.exit(tc, "endpointActivation",
         * "exception"); throw nse; }
         */

        // In terms of actually activiting an endpoint, this stanza of logic is
        // the only stuff in this entire method
        // that is actually needed.
        //
        // This is the logic that take some piece of information from the passed
        // in J2C-ActivationSpec (in this case,
        // the 'name' attribute) and associates it with the passed
        // MessageEndpointFactory so that we can find it at
        // a later time when we actually want to create the pipe to the MDB.
        //
        // In this case, we extract the 'name' from the passed in
        // J2C-ActivationSpec, and we throw that into a hashtable
        // as the 'key'...and we create a MessageEndpointFactoryWrapper class
        // (which is just a container class holding other objects),
        // which contains the specified MessageEndpointFactory, the specified
        // J2C-ActivationSpec, and this resource adapter class instance,
        // throws that into the hashtable as the associatd 'value'.
        //
        // This allows us to come along at some point in the future, during a
        // test, and specify a 'name'...which we then
        // use as the key into this local hashtable variable...which gives us
        // the associated MessageEndpointFactoryWrapper object...
        // from which we extract the MessageEndpointFactory object...which we
        // then get a 'pipe' from.
        //
        // All of the logic in this method above this is still that is NOT
        // needed to activate an endpoint. (In my mind,
        // activiting an endpoint means creating some kind of mechanism that
        // allows you to find it later when you actually need it.)
        // Rather, its simply used to persist information to global storage
        // objects so that tests can come along at some
        // future point and verify if the data was - or was not - passed in.

        String endpointName = ((ActivationSpecImpl) spec).getName();

        if (messageFactories.get(endpointName) != null) {
            Tr.debug(tc, "The end point factory with the name " + endpointName
                         + "has already existed. "
                         + "The new one will override the old one");
        } // end if

        // Write the name of the server that this RA is running on to a log.
        // We do this so that a test can determine which server - out of the
        // many that exist in a cluster -
        // actually had its RA started. We only write this if we actually need
        // it...otherwise, the method no-ops.
        writeServerNameThatThisRAIsRunningOnToLog(endpointName);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "endpointActivation");

    }

    /**
     * <p>
     * This is called when a message endpoint is deactivated. The instances
     * passed as arguments to this method call should be identical to those
     * passed in for the corresponding endpointActivation call. This causes the
     * resource adapter to stop delivering messages to the message endpoint.
     * </p>
     * = *
     *
     * @param endpointFactory
     *                            a message endpoint factory instance.
     * @param spec
     *                            - an activation spec JavaBean instance.
     */
    @Override
    public void endpointDeactivation(MessageEndpointFactory factory,
                                     ActivationSpec spec) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "endpointDeactivation", new Object[] { this, factory,
                                                                spec });

        // remove the endpint factory
        // Pass the spec so we can validate the spec is the same spec when
        // remove the factory instance
        removeMessageFactory(factory, spec);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "endpointDeactivation");

    }

    /**
     * <p>
     * Remove the message factory during the endpoint application deactivation.
     * </p>
     *
     * @param factory
     *                    a message endpoint factory instance.
     * @param spec
     *                    an activation spec JavaBean instance.
     */
    private void removeMessageFactory(MessageEndpointFactory factory,
                                      ActivationSpec spec) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removeMessageFactory", new Object[] { this, factory,
                                                                spec });

        String key = matchMessageFactory(factory);

        if (key == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Cannot find the factory " + factory
                             + ". Continue processing.");

            throw new RuntimeException("Cannot find the factory " + factory
                                       + ". Continue processing.");
        } else {
            messageFactories.remove(key);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Factory is successfully removed.");
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "removeMessageFactory");
    }

    /**
     * <p>
     * Find the message endpoint factory key
     * </p>
     *
     * @param factory
     *                    a message endpoint factory instance.
     */
    private String matchMessageFactory(MessageEndpointFactory factory) {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "matchMessageFactory", new Object[] { this, factory });

        String key = null;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "matchMessageFactory", key);

        return key;
    }

    // ---------------------------------------------------------------------------------
    // getters and setters
    // ---------------------------------------------------------------------------------

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
     * @param adapterName
     *                        The adapterName to set
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
     * @param abc
     *                The abc to set
     */
    public void setAbc(int abc) {
        this.abc = abc;
    }

    /**
     * Returns the messageFactories.
     *
     * @return HashMap
     */
    public HashMap getMessageFactories() {
        return messageFactories;
    }

    public String introspectSelf() {
        StringBuffer str = new StringBuffer();

        str.append("Adapter instance : " + lineSeparator
                   + "Adapter name     : " + adapterName + lineSeparator

                   + lineSeparator + "workManager      : " + workManager
                   + lineSeparator + "xaTerm           : " + xaTerm
                   + lineSeparator);

        str.append("Factories:" + lineSeparator);

        return str.toString();
    }

    /**
     * Returns the bootstrapContext.
     *
     * @return BootstrapContext
     */
    public BootstrapContext getBootstrapContext() {
        return bootstrapContext;
    }

    // @alvinso.2
    /**
     * @return
     */
    public String getPropertyD() {
        System.out.println("inside .getPropertyD()");
        return propertyD;
    }

    /**
     * @return
     */
    public String getPropertyI() {
        System.out.println("inside .getPropertyI()");
        return propertyI;
    }

    /**
     * @return
     */
    public String getPropertyW() {
        System.out.println("inside .getPropertyW()");
        return propertyW;
    }

    /**
     * @return
     */
    public String getPropertyX() {
        System.out.println("inside .getPropertyX()");
        return propertyX;
    }

    /**
     * @return
     */
    public String getPropertyPM26175() {
        System.out.println("inside .getPropertyPM26175()");
        return propertyPM26175;
    }

    /**
     * @param string
     */
    public void setPropertyD(String string) {
        System.out.println("inside .setPropertyD()");
        propertyD = string;
    }

    /**
     * @param string
     */
    public void setPropertyI(String string) {
        System.out.println("inside .setPropertyI()");
        propertyI = string;
    }

    /**
     * @param string
     */
    public void setPropertyW(String string) {
        System.out.println("inside .setPropertyW()");
        propertyW = string;
    }

    /**
     * @param string
     */
    public void setPropertyX(String string) {
        System.out.println("inside .setPropertyX()");
        propertyX = string;
    }

    /**
     * @param string
     */
    public void setPropertyPM26175(String string) {
        System.out.println("inside .setPropertyPM26175()");
        propertyPM26175 = string;
    }

    @Override
    public boolean equals(Object someResourceAdapter) {
        System.out.println("Inside the .equals() method of SA RA...");
        try {
            // say false if the passed in object is null
            if (someResourceAdapter == null) {
                System.out
                                .println("The passed in ResourceAdapter was null. This means it can't equal the current ResourceAdapter instance.");
                return false;
            } // end if

            // Determine if the passed in object is an instance of something we
            // consider an equal
            String className = someResourceAdapter.getClass().getName();
            if ("fvt.adapter.FVTAdapterImpl".equalsIgnoreCase(className)) {
                // its an FVTAdapterImpl...this is considered an equal...return
                // true
                System.out
                                .println("The passed in RA was a FVTAdapterImpl...the RA's are equal.");
                return true;
            } // end if
            else if ("fvt.adapter.EmbeddedFVTAdapterImpl"
                            .equalsIgnoreCase(className)) {
                // its an EmbeddedFVTAdapterImpl...this is considered an
                // equal...return true
                System.out
                                .println("The passed in RA was a EmbeddedFVTAdapterImpl...the RA's are equal.");
                return true;
            } // end else if
            else {
                // its neither of the above...so consider it not equal
                System.out.println("The passed in RA was a **" + className
                                   + "**.  The RAs are NOT equal.");
                return false;
            } // end else
        } // end try
        catch (Throwable e) {
            System.out
                            .println("We got some kind of error trying to determine if the ResourceAdapter instances "
                                     + "are equal.  We'll interpret this to mean they are NOT equal.  The error was:\n"
                                     + e);
            return false;
        } // end catch
    }// end equals()

    private void writeServerNameThatThisRAIsRunningOnToLog(String endpointName) {
        if (NAME_OF_MESSAGE_DRIVEN_EJB.equalsIgnoreCase(endpointName)) {
            System.out
                            .println("We ARE activating the endpoint for **"
                                     + endpointName
                                     + "**, therefore we ARE logging the server that the RA is running on.");

            OutputStreamWriter osw = null;
            FileOutputStream fos = null;
            try {
                // Get the environment variables that tell us what server we are
                // running on, and where to write the log to
                String serverRAIsRunningOn = System
                                .getProperty(WAS_SERVER_NAME);
                String logPathToWriteServerNameTo = System
                                .getProperty(WAS_TEMP_DIR);
                String filePath = logPathToWriteServerNameTo + "/"
                                  + SERVER_FILE_NAME;
                System.out.println("WAS server is: **" + serverRAIsRunningOn
                                   + "**");
                System.out.println("WAS RA log path is: **"
                                   + logPathToWriteServerNameTo + "**");

                File file = new File(filePath);
                PrintStream printStream = new PrintStream(file);
                printStream.print(serverRAIsRunningOn);
                boolean didWeHaveError = printStream.checkError();
                System.out.println("check error value is: **" + didWeHaveError
                                   + "**");
                printStream.close();
            } // end try
            catch (Throwable e) {
                // We catch and bury any exceptions here so we don't kill
                // endpoint deployment because we couldn't determine and write
                // the server
                // for some reason. Only one test relies on this, so it doesn't
                // make sense to jeapordize all other function because of it.
                System.out
                                .println("Failed to write the server that the RA is running on to the log. The error was:\n");
                e.printStackTrace(System.out);
            } // end
            finally {

            } // end finally
        } // end if
        else {
            // We are not activating the endpoint for the specific
            // EJB...therefore, we don't bother updating the log
            System.out
                            .println("Activating endpoint for **"
                                     + endpointName
                                     + "**, so we are NOT logging the server that the RA is running on.");
        } // end else
    }// end writeServerNameThatThisRAIsRunningOnToLog

    public void setThreadContextPropagationRequired(String string) {
        System.out.println("inside .setThreadContextPropagationRequired()");
        ThreadContextPropagationRequired = string;
    }

    public String getThreadContextPropagationRequired() {
        System.out.println("inside .getThreadContextPropagationRequired()");
        return ThreadContextPropagationRequired;
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] arg0) throws ResourceException {
        // TODO Auto-generated method stub
        return null;
    }

}
