/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tra.ann;

import java.io.PrintStream;
import java.io.Serializable;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.Connector;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.TransactionSupport;
import javax.resource.spi.XATerminator;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAResource;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tra.inbound.impl.ActivationSpecImpl;
import com.ibm.tra.trace.DebugTracer;

// All @Connector elements are specified to their empty, most
// degenerate values. Use this class to verify the merging of
// @Connector elements that are specified, but contain no values.

@SuppressWarnings("serial")
@Connector(
           authMechanisms = {}, // CONN_0_AUTHMECH

           description = "CONN_0_DESC",

           displayName = "TRA_jca16_ann_ConnectorMergeAction_NoElement",

           eisType = "CONN_0_EISTYPE",

           licenseDescription = {}, // CONN_0_LICDESC
           licenseRequired = false, // CONN_0_LICREQ

           reauthenticationSupport = false, // CONN_0_REAUTHSPRT

           requiredWorkContexts = {}, // CONN_0_REQWORKCTX

           securityPermissions = {}, // CONN_0_SECPERM

           transactionSupport = TransactionSupport.TransactionSupportLevel.LocalTransaction,
// CONN_0_TXSUPPORT

           smallIcon = {}, // CONN_0_SMALLICON
           largeIcon = {}, // CONN_0_LARGEICON

           vendorName = "CONN_0_VENDORNAME",

           version = "CONN_0_VERSION")
public class ConnectorAnn0 implements ResourceAdapter, Serializable {

    static final String className = ConnectorAnn0.class.getName();

    static final String RAS_COMPONENT = "J2CResourceAdapter";
    static public final String RAS_GROUP = "TestRA";

    private static final TraceComponent tc = Tr.register(className, RAS_GROUP, null);

    @SuppressWarnings("unused")
    private transient BootstrapContext myBootstrapCtx = null;

    @SuppressWarnings("unused")
    private transient WorkManager myWorkManager = null;

    private transient XATerminator myXATerminator = null;

    public ConnectorAnn0() {
    }

    // Resource Adapter Configuration Properties
//  @ConfigProperty(
//      description = "The name of the server executing this RA")
    String serverName;
//   @ConfigProperty(
//      description = "The user id")
    String userName1;
//  @ConfigProperty(
//      description = "The password for userName")
    String password1;
//  @ConfigProperty(
//      description = "Enable debug output related to Message Passing on and off",
//      type = java.lang.Boolean.class,
//      defaultValue = "true")
    Boolean debugMessages = false;
//  @ConfigProperty(
//      description = "Enable debug output related to ActivationSpecs on and off",
//      type = java.lang.Boolean.class,
//      defaultValue = "true")
    Boolean debugActivationSpec = false;
//  @ConfigProperty(
//      description = "Allow redirection of the Debug output from the test resource adapter to another print stream or a file (by specifying a file name). WARNING: If you select an existing file, the contents of the file will be lost.",
//     type = java.lang.String.class,
//      defaultValue = "System.out")
    String outputName = null;
//  @ConfigProperty(
//      description = "Enable classloader info debug",
//      type = java.lang.Boolean.class,
//      defaultValue = "true")
    Boolean printClassLoader = false;
//  @ConfigProperty(
//      description = "Enable classloader info debug",
//      type = java.lang.Boolean.class,
//      defaultValue = "false")
    Boolean dumpStack = false;

    @Override
    public void start(BootstrapContext serverCtx) throws ResourceAdapterInternalException {
        final String methodName = "start";
        Tr.entry(tc, methodName, serverCtx);

        myBootstrapCtx = serverCtx;
        myWorkManager = serverCtx.getWorkManager();
        myXATerminator = serverCtx.getXATerminator();

        // Toggle these here just for good measure.
        DebugTracer.setDumpStack(dumpStack);
        DebugTracer.setPrintClassLoader(printClassLoader);
        if (myXATerminator == null)
            Tr.debug(tc, methodName, "***** ERROR ***** The XATerminator instance returned from the BoostrapContext is null.");
        else
            Tr.debug(tc, methodName, "The XATerminator instance class is " + myXATerminator.getClass().getName());

        /*
         * == B@rkk ========================= Remove MBean-Registration-related Test code ========================
         * // Register and Activate the MBean
         * try {
         * MBeanFactory mbeanFac = AdminServiceFactory.getMBeanFactory();
         * _runtimeCollab = this.new Collab();
         * mbeanFac.activateMBean( MBEAN_TYPE, _runtimeCollab, MBEAN_CONFIGID, MBEAN_DESCRIPTOR );
         * Tr.debug(tc, methodName, MBEAN_TYPE + " MBean activated." );
         * }
         * catch ( com.ibm.websphere.management.exception.AdminException ex ) {
         * traceLogger.exception( RASITraceEvent.TYPE_ERROR_EXC, this, methodName, ex );
         * throw new ResourceAdapterInternalException( "Unexpected Exception registering the MBean.", ex );
         * }
         * == E@rkk ==
         */

        Tr.debug(tc, methodName, "BURA0010I: Outbound TestRA started successfully....");
        Tr.exit(tc, methodName);
    }

    /**
     * The ResourceAdapter JavaBean is responsible for performing an orderly shutdown of the
     * resource adapter instance during the stop method call. This may involve closing network
     * endpoints, relinquishing threads, releasing all active Work instances, and flushing any
     * cached data to the EIS.
     *
     */
    @Override
    public void stop() {
        final String methodName = "stop";
        Tr.entry(tc, methodName);
        Tr.debug(tc, methodName, "BURA0090I: Outbound TestRA stopped.");
        Tr.exit(tc, methodName);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.resource.spi.ResourceAdapter#endpointActivation(javax.resource.spi.endpoint.MessageEndpointFactory, javax.resource.spi.ActivationSpec)
     */
    @Override
    public void endpointActivation(MessageEndpointFactory msgEndpointFac, ActivationSpec activationSpec) throws ResourceException {
        boolean debug = debugActivationSpec;
        PrintStream out = DebugTracer.getPrintStream();
        if (debug) {
            out.println("endpointActivation called with following parameters: ");
            out.println("MEF: " + msgEndpointFac.toString());
            out.println("ActivationSpec: " + activationSpec.toString());
        }
        if (activationSpec instanceof ActivationSpecImpl) {
            if (debug) {
                ActivationSpecImpl aSpec = (ActivationSpecImpl) activationSpec;
                out.println("Received com.ibm.inbound.impl.ActivationSpecImpl");
                out.println("Contents of ActivationSpec: ");
                out.println("prop1: " + aSpec.getProp1());
                out.println("destType: " + aSpec.getDestinationType());
                if (aSpec.getDestination() != null) {
                    out.println("destination: " + aSpec.getDestination().toString());
                } else {
                    out.println("destination is null");
                }
            }
        } else {
            if (debug) {
                out.println("ActivationSpec of type: [" + activationSpec.getClass().getName() + "] and is not supported by this resource adapter.");
            }
            throw new NotSupportedException("The activationSpec passed is not supported by the resource adapter");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.resource.spi.ResourceAdapter#endpointDeactivation(javax.resource.spi.endpoint.MessageEndpointFactory, javax.resource.spi.ActivationSpec)
     */
    @Override
    public void endpointDeactivation(MessageEndpointFactory msgEndpointFac, ActivationSpec activationSpec) {
        boolean debug = debugActivationSpec;
        PrintStream out = DebugTracer.getPrintStream();
        if (debug) {
            out.println("endpointDeactivation called with: ");
            out.println("MEF: " + msgEndpointFac.toString());
            out.println("ActivationSpec: " + activationSpec.toString());
        }
        if (activationSpec instanceof ActivationSpecImpl) {
            ActivationSpecImpl aSpec = (ActivationSpecImpl) activationSpec;
            if (debug) {
                out.println("Received com.ibm.inbound.impl.ActivationSpecImpl");
                out.println("Contents of ActivationSpec: ");
                out.println("prop1:" + aSpec.getProp1());
                out.println("destType: " + aSpec.getDestinationType());
                if (aSpec.getDestination() != null) {
                    out.println("destination:" + aSpec.getDestination().toString());
                } else {
                    out.println("destination is null");
                }
            }
        } else {
            if (debug) {
                out.println("Received activationSpec of type: " + activationSpec.getClass().getName());
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.resource.spi.ResourceAdapter#getXAResources(javax.resource.spi.ActivationSpec[])
     */
    @Override
    public XAResource[] getXAResources(ActivationSpec[] arg0) throws ResourceException {
        final String methodName = "getXAResources";
        Tr.entry(tc, methodName);
        Tr.exit(tc, methodName);
        return null;
    }

    public Boolean getDebugActivationSpec() {
        return new Boolean(debugActivationSpec);
    }

    public Boolean getDebugMessages() {
        return new Boolean(debugMessages);
    }

    public Boolean getDumpStack() {
        System.out.println("In getDumpStack()");
        return new Boolean(dumpStack);
    }

    public String getOutputName() {
        return outputName;
    }

    public Boolean getPrintClassLoader() {
        System.out.println("In getPrintClassLoader()");
        return new Boolean(printClassLoader);
    }

    public void setDebugActivationSpec(Boolean val) {
        debugActivationSpec = val.booleanValue();
        DebugTracer.setDebugActivationSpec(debugActivationSpec);
    }

    public void setDebugMessages(Boolean val) {
        debugMessages = val.booleanValue();
        DebugTracer.setDebugMessages(debugMessages);
    }

    public void setDumpStack(Boolean val) {
        System.out.println("In setDumpStack(), val: " + val.booleanValue());
        dumpStack = val.booleanValue();
        DebugTracer.setDumpStack(dumpStack);
    }

    public void setOutputName(String fileName) {
        // if the fileName is null, is empty, equals "null", or equals "System.out", use system.out
        if (fileName == null || fileName.equals("")
            || fileName.equals("null") || fileName.equals("System.out")) {
            outputName = "System.out";
            DebugTracer.setPrintStream(System.out);
        } else if (fileName.equals("System.err")) {
            outputName = "System.err";
            DebugTracer.setPrintStream(System.err);
        } else {
            outputName = fileName;
            try {
                DebugTracer.setPrintStream(new PrintStream(outputName));
            } catch (Exception e) {
                System.out.println("An error occurred while trying to open the new file: " + fileName + " " + e);
            }
        }
    }

    public void setPrintClassLoader(Boolean val) {
        System.out.println("In setPrintClassLoader(), val: " + val.toString());
        printClassLoader = val.booleanValue();
        DebugTracer.setPrintClassLoader(val.booleanValue());
    }

    /**
     * @return the serverName
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * @param serverName the serverName to set
     */
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
     * @return the userName1
     */
    public String getUserName1() {
        return userName1;
    }

    /**
     * @param userName1 the userName1 to set
     */
    public void setUserName1(String userName1) {
        this.userName1 = userName1;
    }

    /**
     * @return the password1
     */
    public String getPassword1() {
        return password1;
    }

    /**
     * @param password1 the password1 to set
     */
    public void setPassword1(String password1) {
        this.password1 = password1;
    }

}
