/*******************************************************************************
 * Copyright (c) 1997, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import java.security.AccessController;

//import com.ibm.ejs.j2c.ActivationSpecWrapperImpl; //688092

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Set;
import java.util.Vector;

import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ConnectionRequestInfo;
import javax.security.auth.Subject;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
//import com.ibm.ws.management.commands.jca.J2CCommandHelper; //688092
//import com.ibm.ws.security.util.AccessController;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.IncidentStream;

// The arguments passed to the methods below are:
// 1 - The encountered exception
// 2 - The IncidentStream.  This is where data that is captured should be written
// 3 - Object[]. The value of the array may be null. If not null, it contains an
//      array of objects which the caller to the FFDC filter passed. The caller will
//      need to know the order and content of the array.
//      Since the information in the array may vary depending upon the location in the code,
//      the first index of the array may contain hints as to the content of the rest of the
//      array
// 4 - callerThis. The callerThis value may be null, if the method which invoked the filter
//      was a static method. Or if the current point of execution for the filter, does not
//      correspond to the DM being invoked.
// 5 - sourceId. The sourceId passed to the filter.
// Note: the current package.class.method name can be found by invoking the getExecutionPoint
//      method on the parent implementation (this method is expensive)

public class DiagnosticModuleForJ2C extends com.ibm.ws.ffdc.DiagnosticModule {

    TraceComponent tc = Tr.register("ConnLeakLogic", J2CConstants.traceSpec, J2CConstants.messageFile); //LI3162-5 188900
    public static String SIB_JMS_Resource_Adapter_displayname = "SIB JMS Resource Adapter",
                    WebSphere_Relational_Resource_Adapter_displayname = "WebSphere Relational Resource Adapter",
                    WebSphere_MQ_Resource_Adapter_displayname = "WebSphere MQ Resource Adapter";

    // F743-15568 Move to J2CConstants

    /*
     * All Methods that dump data MUST HAVE A NAME THAT STARTS with "ffdcDump"
     * for the ffdc runtime to recognize them. Any methods whose name does not start
     * with ffdcDump will not be recognized by the ffdc runtime.
     *
     * All Methods with a name that starts with "ffdcDumpDefault" will always be called when this component is
     * involved in an exception or error situation. These methods will be responsible for performing
     * the default data capture for this component
     *
     * Method names that simply start with "ffdcDump" [not ffdcDumpDefault] are only called under certain conditions
     * and are not considered part of the default data capture. See the comment below to understand
     * when and how these methods will be called.
     *
     * Every diagnostic module must have at least ONE ffdcDumpDefault... method. If at least one is not found an
     * IllegalStateError will be thrown and this will be considered a programming/design error.
     */

    /**
     * This is the most generic dump scenario that applies to the J2C runtime.
     * <p>This method introspects on :
     * <UL> <LI> ConnectorRuntime.txToMcSet </LI>
     * <LI> the This object from which the exception originated </LI>
     * </UL>
     *
     * @param th         The encountered exception
     * @param is         The IncidentStream. This is where data that is captured should be written
     * @param callerThis callerThis. The callerThis value may be null, if the method which invoked the filter
     *                       was a static method. Or if the current point of execution for the filter, does not
     *                       correspond to the DM being invoked.
     * @param o          Object[]. The value of the array may be null. If not null, it contains an
     *                       array of objects which the caller to the FFDC filter passed. The caller will
     *                       need to know the order and content of the array.
     *                       Since the information in the array may vary depending upon the location in the code,
     *                       the first index of the array may contain hints as to the content of the rest of the
     *                       array
     * @param sourceId   sourceId. The sourceId passed to the filter.
     */
    public void ffdcDumpDefaultJ2C(
                                   Throwable th,
                                   IncidentStream is,
                                   Object callerThis,
                                   Object[] o,
                                   String sourceId) {

        if (sourceId.equals(J2CConstants.DMSID_MAX_CONNECTIONS_REACHED)) { // F743-15568
            ffdcDumpMaxConnectionsReached(th, is, callerThis, o, sourceId); //174269
        }

        //LI3162-5 start
        else if (sourceId.equals("com.ibm.ejs.j2c.LocalTransactionWrapper.enlist") &&
                 callerThis instanceof LocalTransactionWrapper &&
                 // TODO 201595 jms:kws The following code should have already been fixed up by now, right?
                 o != null && o.length >= 1 /*
                                             * && // TODO jms ,mdd: once we get the TM code, modify the following line as needed
                                             * o[0] instanceof IllegalResourceIn2PCTransactionException
                                             */) {

            ffdcDumpIllegal1PCResourceMessage(th, is, callerThis, o, sourceId);
        }
        //LI3162-5 end

        //688363 start
        else if (callerThis instanceof ActivationSpec) {
            is.writeLine("sourceId ", sourceId);
            ActivationSpec aswi = (ActivationSpec) callerThis;
//         if (aswi != null && (SIB_JMS_Resource_Adapter_displayname.equals(rarName)
//                         || WebSphere_Relational_Resource_Adapter_displayname.equals(rarName)
//                         || WebSphere_MQ_Resource_Adapter_displayname.equals(rarName))) {
//           is.introspectAndWriteLine("This (sensitive)", aswi.ffdcCallerThisSensitive());
//         }
//         else {
            is.introspectAndWriteLine("This", callerThis);
//         }
        }
        //688352 end

        else {
            is.writeLine("sourceId ", sourceId);
            is.introspectAndWriteLine("This", callerThis);
            // Start new code for defect 220489
            try {
                if (callerThis instanceof MCWrapper) {
                    MCWrapper mcw = (MCWrapper) callerThis;
                    is.writeLine("Pool Information:", mcw.getPoolManager().toString());
                }
                if (callerThis instanceof ConnectionManager) {
                    ConnectionManager cm = (ConnectionManager) callerThis;
                    cm._pm.toString();
                    is.writeLine("Pool Information:", cm._pm.toString());
//          if(cm._pm.isAlternateResourceEnabled() && cm.alternatePM  != null) //4@F003709-19851.2A
//          {
//        	  is.writeLine("Alternate Pool Information:",cm.alternatePM.toString());
//          }
                }
                if (callerThis instanceof XATransactionWrapper) {
                    XATransactionWrapper xtw = (XATransactionWrapper) callerThis;
                    is.writeLine("Pool Information:", xtw.getMcWrapper().getPoolManager().toString());
                }
                if (callerThis instanceof LocalTransactionWrapper) {
                    LocalTransactionWrapper ltw = (LocalTransactionWrapper) callerThis;
                    is.writeLine("Pool Information:", ltw.getMcWrapper().getPoolManager().toString());
                }
                if (callerThis instanceof ConnectionEventListener) {
                    ConnectionEventListener cel = (ConnectionEventListener) callerThis;
                    is.writeLine("Pool Information:", cel.getMcWrapper().getPoolManager().toString());
                }
            } catch (NullPointerException nep) {
                /*
                 * We can not dump the poolmanager toString data due to
                 * a missing object.
                 */
                is.writeLine("Pool Information:", "No Pool information available");
            }
            // End new code for defect 220489
        }

    } // end ffdcDumpDefaultJ2C

    //   The methods below are only called if there are directives for calling them.
    //
    //   A directive is a method name without the prefix "ffdcDump".
    //
    //   So for example if a certain problem is encountered for which we would like
    //   ffdcDumpTableA and ffdcDumpTableB called then the directives will be:  tablea, tableb
    //
    //   Briefly ffdc works as follows:
    //
    //   1 - An exception occurs and is routed to the FFDC runtime.  An analysis is performed
    //       and the exception stack is matched against a set of known errors.
    //
    //   2 - If no match exists the default ffdcDump'ing methods are called
    //       These are the methods whose name starts with ffdcDumpDefault...
    //       e.g. ffdcDumpDefaultOurMasterTable, ffdcDumpDefaultOurStateTable etc.
    //       and this is how the default data is collected and no further action is taken!
    //
    //   3 - If a match is found an array of strings (directives) is returned by the analysis engine.
    //       The ffdc runtime still performs step 2 above to collect the default data for this component.
    //       Additionally each directive is converted into a method name as follows: methodname = "ffdcDump"+directive.
    //       If a method below matches that method name ffdc automatically detects it and calls it.
    //       Note: case does not matter. i.e. if a directive=OBJECTABC the ffdc runtime converts it and all method names
    //       in this class to lower case and attempts to find a match.  If a matching method name is found, it is called!

    /**
     * ffdcDumpMaxConnectionsReached is called by the PoolManager via FFDC when the max number of
     * connections is reached, and the connection request has been waiting longer than ConnectionWaitTime. This method will
     * dump MaxConnections, currentConnections, ConnectionWaitTime, and the normal introspection on the poolmanager. Some information
     * may be duplicated, but may prove usefull in case of an application server internal error.
     */

    public void ffdcDumpMaxConnectionsReached(
                                              Throwable th,
                                              IncidentStream is,
                                              Object callerThis,
                                              Object[] o,
                                              String sourceId) {

        Throwable ttt = new Throwable();
        for (Object ste : ttt.getStackTrace())
            is.writeLine(ste.toString(), "");

        is.writeLine("JMS20190315DiagModa - Start of ffdcDumpMaxConnectionsReached dump", "");
        is.write("JMS20190314 - Maximum number of connections has been reached, and the connection request has been waiting longer than", "");
        is.write("ConnectionWaitTime.  Two possible solutions  : increase the max number of connections, or increase the", "");
        is.writeLine("ConnectionWaitTime.", "");

        // Now we are called from ConnectionManager.
        try {

            PoolManager pm = (PoolManager) callerThis;

            //LI3162-5 start
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { //Defect 736528 737787

                long now = System.currentTimeMillis();
                SharedPool[] p = pm.getSharedPool();
                Tr.debug(tc, "Dumping initial request stack traces");
                com.ibm.ws.j2c.MCWrapper[] wrappers = null;
                for (int i = 0; i < p.length; i++) {
                    if (p[i] == null)
                        continue;
                    wrappers = p[i].getMCWrapperList();
                    for (int j = 0; j < wrappers.length; j++) {
                        if (wrappers[j] == null)
                            continue;
                        Throwable t = ((MCWrapper) wrappers[j]).getInitialRequestStackTrace();
                        if (t != null) {
                            long timestamp = ((MCWrapper) wrappers[j]).getCreatedTimeStamp();
                            Tr.debug(tc, wrappers[j].toString() + " in-use for " + (now - timestamp) + "ms", t);
                        }
                    }
                }
                // Start new code for defect 196608
                wrappers = pm.getUnSharedPoolConnections(); // 207305
                for (int j = 0; j < wrappers.length; j++) {
                    if (wrappers[j] == null)
                        continue;
                    Throwable t = ((MCWrapper) wrappers[j]).getInitialRequestStackTrace();
                    if (t != null) {
                        long timestamp = ((MCWrapper) wrappers[j]).getCreatedTimeStamp();
                        Tr.debug(tc, wrappers[j].toString() + " in-use for " +
                                     (now - timestamp) + "ms",
                                 t);
                    }
                }
                // End new code for defect 196608

            }
            //LI3162-5 end

            is.writeLine("   Maximum Connections           = ", pm.maxConnections);
            is.writeLine("   Current number of connections = ", pm.totalConnectionCount.get());//  getConnectionCount());
            is.writeLine("   Connection Wait Timout        = ", pm.connectionTimeout);
            is.write("If the current number of connections is not greater than or equal to max connections, ", "");
            is.writeLine("there has been a WebSphere internal error.", "");

            //PK25981 remove is.writeLine("Dumping the list of J2EEComponents and their corresponding handle count", "");
            //PK25981 remove is.writeLine(ConnectionHandleManager.getConnectionHandleManager().dumpComponentListWithHandlesToString(), "" );
        } catch (ClassCastException cce) {
            is.writeLine("Internal J2C FFDC Diagnostic Module error", "");
            is.writeLine("ClassCastException in ffdcDumpMaxConnectionsReached", "");
            is.writeLine("Object this was not of type ConnectionManager when ffdcDumpMaxConnectionsReached was called - introspecting the object: ", "");

            // silly statement to appease Eclipse warning about unused vars
            // if ((th != th) && (o != o) && (sourceId != sourceId));// 188900
        } catch (Exception e) {
            // don't let anything "ripple" up.
            is.writeLine("Internal J2C FFDC Diagnostic Module error", "");
            is.writeLine("Exception ", e);
        } finally {
            is.introspectAndWriteLine("This = ", callerThis);
            is.writeLine("--------------------------------Pool Contents-------------------------------", ""); // PI11713
            is.write(callerThis.toString(), ""); // PI11713
            is.writeLine("JMS20190315DiagModa - End of ffdcDumpMaxConnectionsReached dump", "");

        }

    } // end ffdcDumpMaxConnectionsReached

    /**
     * TODO 201595 mdd:jms this comment is wrong
     * LI3162-5
     * ffdcDumpIllegal1PCResourceMessage is called by the PoolManager via FFDC when the max number of
     * connections is reached, and the connection request has been waiting longer than ConnectionWaitTime. This method will
     * dump MaxConnections, currentConnections, ConnectionWaitTime, and the normal introspection on the poolmanager. Some information
     * may be duplicated, but may prove usefull in case of an application server internal error.
     */
    public void ffdcDumpIllegal1PCResourceMessage(
                                                  Throwable th,
                                                  IncidentStream is,
                                                  Object callerThis,
                                                  Object[] o,
                                                  String sourceId) {

        MCWrapper mcWrapper = (MCWrapper) o[0];
        MCWrapper initialMCWrapper = mcWrapper;
        UOWCoordinator uowCoord = mcWrapper.getUOWCoordinator();
        com.ibm.ejs.j2c.PoolManager pm = mcWrapper.getPoolManager();
        Vector allConnsForThisTran = pm.getMCWrappersByTran(uowCoord);
        if (allConnsForThisTran != null && allConnsForThisTran.size() > 0) {
            //print all connections involved in this transaction
            is.writeLine("DSRA9002E J2CA0030E WTRN0062E ", "Possible misuse of " +
                                                           "connection manager may have caused 2PC transaction when 1PC " +
                                                           "tran was desired.");

            //UserData ud = (UserData) mcWrapper.getUserData();
            Subject subj = mcWrapper.getSubject();
            ConnectionRequestInfo cri = mcWrapper.getCRI();
            final String password = "not set";
//            if (cri instanceof com.ibm.ws.rsadapter.spi.WSConnectionRequestInfoImpl) {
//                password = ((com.ibm.ws.rsadapter.spi.WSConnectionRequestInfoImpl) cri).getPassword();
//            } else {
//                password = "";
//            }

            //print first connection
            printMCWrapperDetails(mcWrapper, is, password, subj);

            //print all other connections in the tran
            for (int i = 0; i < allConnsForThisTran.size(); i++) {
                mcWrapper = (MCWrapper) allConnsForThisTran.elementAt(i);
                if (mcWrapper != initialMCWrapper)
                    printMCWrapperDetails(mcWrapper, is, password, subj);
            }

            // silly statement to appease Eclipse warning about unused vars
            //  if ((th != th) && (o != o) && (sourceId != sourceId) && (callerThis != callerThis));// 188900

        }

    } // end ffdcDumpIllegal1PCResourceMessage

    private void printMCWrapperDetails(
                                       MCWrapper mcWrapper,
                                       IncidentStream is,
                                       String password,
                                       Subject origSubj) {

        Subject subj = mcWrapper.getSubject();
        ConnectionRequestInfo cri = mcWrapper.getCRI();
        is.writeLine("MCWrapper", mcWrapper.toString());
        is.writeLine("PoolName", mcWrapper.gConfigProps.getJNDIName());
        is.writeLine(cri.toString(), "");

        if (!subjectsMatch(subj, origSubj)) {
            is.writeLine("Subject does NOT match!", "");
        }
        if (subj == null) {
            is.writeLine("Subject", "null");
        }
        is.writeLine("Container Managed Auth",
                     mcWrapper.getConnectionManager().containerManagedAuth ? "Yes" : "No");

//        if (cri instanceof com.ibm.ws.rsadapter.spi.WSConnectionRequestInfoImpl) {
//            String password2 = ((com.ibm.ws.rsadapter.spi.WSConnectionRequestInfoImpl) cri).getPassword();
//            if (password != null && !password.equals(password2)) {
//                is.writeLine("Password does NOT match!", "");
//            }
//        }

    } // end printMCWrapperDetails

    private boolean subjectsMatch(Subject subj1, Subject subj2) {

        if (subj1 == subj2) {
            return true;
        }

        if ((subj1 == null && subj2 != null) ||
            (subj1 != null && subj2 == null)) {
            return false;
        }

        final Set publicCreds1 = subj1.getPublicCredentials(); // 336845
        final Set publicCreds2 = subj2.getPublicCredentials(); // 336845
        final Set privateCreds1 = subj1.getPrivateCredentials(); // 336845
        final Set privateCreds2 = subj2.getPrivateCredentials(); // 336845

        // begin 336845
        Boolean eq = null;
        try {
            eq = (Boolean) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {

                @Override
                public Object run() throws ClassNotFoundException {

                    if ((publicCreds1 == null && publicCreds2 != null)
                        || (publicCreds1 != null && publicCreds2 == null)) {

                        return new Boolean(false);

                    }
                    boolean retVal = false;
                    if (publicCreds1 != null && publicCreds1.equals(publicCreds2)) {
                        retVal = (privateCreds1 != null && privateCreds1.equals(privateCreds2));
                    }
                    return new Boolean(retVal);
                }
            });
        } catch (PrivilegedActionException pae) {
            FFDCFilter.processException(
                                        pae.getException(),
                                        "com.ibm.ejs.j2c.DiagnosticModuleForJ2C.subjectsMatch", "688", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { //Defect 736528 737787
                Tr.debug(tc, "Caught PrivilegedActionException " + pae.getException());
            }
        }

        if (eq != null) {
            return eq.booleanValue();
        } else {
            return false;
        }
        // end 336845

    } // end subjectsMatch

    /* This is left in for debug */
    public static void main(String args[]) {
        System.out.println("Start of the test for the component DM");
        DiagnosticModuleForJ2C dmfc = new DiagnosticModuleForJ2C();
        dmfc.validate();

    } // end main

} // end class DiagnosticModuleForJ2c
