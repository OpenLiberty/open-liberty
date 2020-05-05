/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * This class is a container for shared connections.
 */
package com.ibm.ejs.j2c;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.j2c.MCWrapper;
import com.ibm.ws.jca.adapter.WSManagedConnection;
import com.ibm.ws.resource.ResourceRefInfo;

/**
 * Shared Pool class
 */

public final class SharedPool {
    private static final TraceComponent tc = Tr.register(SharedPool.class,
                                                         J2CConstants.traceSpec,
                                                         J2CConstants.messageFile);

    /*
     * SharedLockObject to synchronize access to the mcWrapperList
     */
    protected Integer sharedLockObject = new Integer(0);
    /*
     * mcWrapperList - contains shared mcWrappers
     * mcWrapperListTemp - used for changing the mcWrapperList
     * object array size.
     * mcWrapperListSize - number of mcWrappers in the object array
     */
    private MCWrapper[] mcWrapperList;
    private MCWrapper[] mcWrapperListTemp = null;
    private int mcWrapperListSize = 0;
    /*
     * _pm - reference to poolmanager
     */
    private PoolManager _pm = null;
    /*
     * objectArraySize - max number of mcWrappers that can be
     * stored in the mcWrapperList
     */
    private int objectArraySize = 0;

    /*
     * The following are used for statistics
     */
    protected int sop_removes = 0;
    protected int snop_removes = 0;
    protected int sop_gets = 0;
    protected int snop_gets = 0;
    protected int sop_gets_notfound = 0;
    protected int snop_gets_notfound = 0;

    /**
     * Shared Pool constructor
     */

    protected SharedPool(int initialSize, PoolManager pm) {
        super();
        _pm = pm;
        objectArraySize = initialSize;
        if (objectArraySize < 1) {
            /*
             * if maxConnections is set to zero, we need to initialize the
             * initialSize to something. Since a value of zero represents
             * infinite number of connection can be created and a heavy load
             * may occur, an initial value of 100 should be OK. If more is
             * needed, the mcWrapperList object will need to increase its size.
             */
            objectArraySize = 100;
        } else if (objectArraySize > J2CConstants.INITIAL_SIZE) {
            /*
             * If maxConnections is set to a large value, we should not be
             * initializing the initialSize to that value. Instead we will
             * need to set an initial value and then the mcWrapperList array
             * size should be increased on demand
             */
            objectArraySize = J2CConstants.INITIAL_SIZE;
        }
        /*
         * Create the mcWrapperList object array that will contain MCWrappers
         */
        mcWrapperList = new MCWrapper[objectArraySize];
    }

    /**
     * Return a share connection if it exists.
     *
     * @concurrency concurrent
     */

    protected MCWrapper getSharedConnection(
                                            Object affinity,
                                            Subject subject,
                                            ConnectionRequestInfo cri,
                                            boolean enforceSerialReuse,
                                            String pmiName,
                                            int commitPriority,
                                            int branchCoupling) {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "getSharedConnection");
        }

        MCWrapper mcWrapper = null;
        boolean affinityIsEqual = false;
        boolean cmConfigDataIsCompatible = false;
        boolean dumpLTCMessage = true; // We want to only dump one of 86 trace message for every getSharedConnection

        /*
         * Dirty read the size of the mcWrapperList. There are no mcWrappers in the
         * list if mcWrapperListSize is zero and it is imposible for a matching
         * mcWrapper and affinity id to be added to this list since this thread has
         * to do the adding.
         */
        if (mcWrapperListSize > 0) {

            MCWrapper mcWrapperTemp = null;
            /*
             * We have one or more mcWrapper(s). We need to synchronize.
             */
            synchronized (sharedLockObject) {
                if (mcWrapperListSize > 0) {

                    /*
                     * Look for a matching affinity id and commit priority.
                     */
                    mcWrapperTemp = mcWrapperList[0];

                    if (affinity != null && affinity.equals(mcWrapperTemp.getSharedPoolCoordinator())) {
                        affinityIsEqual = true;

                        ConnectionManager cm = ((com.ibm.ejs.j2c.MCWrapper) mcWrapperTemp).getCm();
                        ResourceRefInfo resRefInfo = cm.getResourceRefInfo();
                        if (resRefInfo.getCommitPriority() == commitPriority) {
                            int tempBranchCoupling = resRefInfo.getBranchCoupling();
                            if (branchCoupling == tempBranchCoupling) { // Check if they match first for performance
                                cmConfigDataIsCompatible = true;
                            } else {
                                cmConfigDataIsCompatible = cm.matchBranchCoupling(branchCoupling, tempBranchCoupling,
                                                                                  ((com.ibm.ejs.j2c.MCWrapper) mcWrapperTemp).get_managedConnectionFactory());
                            }
                        }

                    }

                }

            } // Lets be really optimistic and release the lock

            if (affinityIsEqual && cmConfigDataIsCompatible) {

                // When the new transaction code is drop, we will want to use
                // the following if test for comparing Coordinators
                // if (mcWrapperTemp.getSharedPoolCoordinator() == affinity) {

                /*
                 * We have a matching affinity id, now we need a matching user data
                 */
                boolean subjectMatch = false;
                Subject mcWrapperSubject = mcWrapperTemp.getSubject();

                if ((subject == null) && (mcWrapperSubject == null)) {
                    subjectMatch = true;
                } else {

                    if ((subject != null) && (mcWrapperSubject != null)) {

                        Equals e = new Equals();
                        e.setSubjects(subject, mcWrapperTemp.getSubject());

                        if (AccessController.doPrivileged(e)) {
                            subjectMatch = true;
                        }

                    }

                }

                ManagedConnection mc = mcWrapperTemp.getManagedConnection();
                ConnectionRequestInfo mcWrapperCRI = mc instanceof WSManagedConnection ? ((WSManagedConnection) mc).getConnectionRequestInfo() : mcWrapperTemp.getCRI();
                // The cri can be null, so we need to check for null.
                boolean criMatch = cri == mcWrapperCRI || cri != null && cri.equals(mcWrapperCRI);

                if (criMatch && subjectMatch) {

                    /*
                     * We have a matching affinity id and user data, but we have one more
                     * test. The following if checks the serial reuse rule.
                     */
                    if (enforceSerialReuse && (mcWrapperTemp.getHandleCount() >= 1)) {

                        /*
                         * We can not use this connections. We need to look for a shared
                         * connection with the handle count of zero or we need to get a new
                         * shareable connection.
                         */

                        if (isTracingEnabled && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "allocateConnection_Common:  HandleCount = " + mcWrapperTemp.getHandleCount());
                        }

                        if (_pm.logSerialReuseMessage) {
                            Tr.info(tc, "ATTEMPT_TO_SHARE_LTC_CONNECTION_J2CA0086", mcWrapperTemp, pmiName);
                            _pm.logSerialReuseMessage = false;
                        }

                        if (dumpLTCMessage) {
                            if (isTracingEnabled && tc.isDebugEnabled()) {
                                dumpLTCMessage = false; // only dump this info once for every getSharedConnection
                                                        // request.
                                Tr.debug(this, tc, "Attempt to share connection within LTC (J2CA0086)");
                                Tr.debug(this, tc, "mcWrapper = " + mcWrapperTemp);
                                Tr.debug(this, tc, "pmiName   = " + pmiName);
                                Tr.debug(this, tc, "LTC stack information = " + dumpLTCInformation(mcWrapperTemp, pmiName, affinity));
                            }
                        }

                    } // end enforceSerialReuse && (mcWrapperTemp.getHandleCount() >= 1)

                    else {

                        /*
                         * We have a shareable connection to use
                         */
                        if ((isTracingEnabled && tc.isDebugEnabled())) {
                            /*
                             * This is used for tracking shared pool usage data
                             */
                            ++sop_gets;
                        }

                        mcWrapper = mcWrapperTemp;

                    }

                } // end criMatch && subjectMatch

                else {

                    if ((isTracingEnabled && tc.isDebugEnabled())) {
                        /*
                         * This is used for tracking shared pool usage data
                         */
                        ++snop_gets_notfound;
                    }

                } // end else (cri, subj don't both match)

            } // end affinityIsEqual && commitPriorityIsEqual

            else {

                if ((isTracingEnabled && tc.isDebugEnabled())) {
                    /*
                     * This is used for tracking shared pool usage data
                     */
                    ++snop_gets_notfound;
                }

            }

            if (mcWrapper == null) {
                synchronized (sharedLockObject) {
                    /*
                     * If the mcWrapperListSize > 0, we may have a connection that will
                     * match. We most likely already checked one of the connections, but
                     * we released the shared lock, so we need to check all of them again,
                     * no more perf tricks.
                     */
                    for (int i = 0; i < mcWrapperListSize; ++i) {

                        mcWrapperTemp = mcWrapperList[i];

                        /*
                         * Look for a matching affinity id and user data.
                         */

                        if (affinity != null && affinity.equals(mcWrapperTemp.getSharedPoolCoordinator())) {

                            // When the new transaction code is drop, we will want to use
                            // the following if test for comparing Coordinators
                            // if (mcWrapperTemp.getSharedPoolCoordinator() == affinity) {

                            /*
                             * We have a matching affinity id, now we need a matching user
                             * data
                             */
                            boolean subjectMatch = false;
                            Subject mcWrapperSubject = mcWrapperTemp.getSubject();

                            if ((subject == null) && (mcWrapperSubject == null)) {
                                subjectMatch = true;
                            } else {

                                if ((subject != null) && (mcWrapperSubject != null)) {

                                    Equals e = new Equals();
                                    e.setSubjects(subject, mcWrapperTemp.getSubject());
                                    if ((AccessController.doPrivileged(e)).booleanValue()) {
                                        subjectMatch = true;
                                    }

                                }

                            }

                            ManagedConnection mc = mcWrapperTemp.getManagedConnection();
                            ConnectionRequestInfo mcWrapperCRI = mc instanceof WSManagedConnection ? ((WSManagedConnection) mc).getConnectionRequestInfo() : mcWrapperTemp.getCRI();
                            // The cri can be null, so we need to check for null.
                            boolean criMatch = cri == mcWrapperCRI || cri != null && cri.equals(mcWrapperCRI);

                            if (criMatch && subjectMatch) {

                                /*
                                 * We have a matching affinity id and user data, but we have one
                                 * more test. The following if checks the serial reuse rule.
                                 */
                                if (enforceSerialReuse && (mcWrapperTemp.getHandleCount() >= 1)) {
                                    /*
                                     * We can not use this connections. We need to look for a
                                     * shared connection with the handle count of zero or we need
                                     * to get a new shareable connection.
                                     */
                                    if (isTracingEnabled && tc.isDebugEnabled()) {
                                        Tr.debug(this, tc, "allocateConnection_Common:  HandleCount = " + mcWrapperTemp.getHandleCount());
                                    }

                                    if (_pm.logSerialReuseMessage) {
                                        Tr.info(tc, "ATTEMPT_TO_SHARE_LTC_CONNECTION_J2CA0086", mcWrapperTemp, pmiName);
                                        _pm.logSerialReuseMessage = false;
                                    }

                                    if (dumpLTCMessage) {
                                        if (isTracingEnabled && tc.isDebugEnabled()) {
                                            dumpLTCMessage = false; // only dump this info once for every getSharedConnection
                                                                    // request.
                                            Tr.debug(this, tc, "Attempt to share connection within LTC (J2CA0086)");
                                            Tr.debug(this, tc, "mcWrapper = " + mcWrapperTemp);
                                            Tr.debug(this, tc, "pmiName   = " + pmiName);
                                            Tr.debug(this, tc, "LTC stack information = " + dumpLTCInformation(mcWrapperTemp, pmiName, affinity));
                                        }
                                    }

                                } // end enforceSerialReuse && (mcWrapperTemp.getHandleCount() >= 1)

                                else {

                                    /*
                                     * Look for a matching commitPriority/branchCoupling.
                                     */
                                    ConnectionManager cm = ((com.ibm.ejs.j2c.MCWrapper) mcWrapperTemp).getCm();
                                    ResourceRefInfo resRefInfo = cm.getResourceRefInfo();
                                    if (resRefInfo.getCommitPriority() == commitPriority) {
                                        int tempBranchCoupling = resRefInfo.getBranchCoupling();
                                        if (branchCoupling == tempBranchCoupling) { // Check if they match first for performance
                                            cmConfigDataIsCompatible = true;
                                        } else {
                                            cmConfigDataIsCompatible = cm.matchBranchCoupling(branchCoupling, tempBranchCoupling,
                                                                                              ((com.ibm.ejs.j2c.MCWrapper) mcWrapperTemp).get_managedConnectionFactory());
                                        }
                                    }

                                    if (cmConfigDataIsCompatible) {

                                        /*
                                         * We have a shareable connection to use
                                         */
                                        if ((isTracingEnabled && tc.isDebugEnabled())) {
                                            /*
                                             * This is used for tracking shared pool usage data
                                             */
                                            ++snop_gets;

                                        }
                                        mcWrapper = mcWrapperTemp;
                                        break;

                                    }

                                }

                            } // end criMatch && subjectMatch

                        } // end mcWrapperTemp.getSharedPoolCoordinator().equals(affinity)

                    } // end for loop

                } // end synchronized

                if (mcWrapper == null) {

                    if ((isTracingEnabled && tc.isDebugEnabled())) {
                        /*
                         * This is used for tracking shared pool usage data
                         */
                        ++snop_gets_notfound;
                    }

                }

            } // end mcWrapper == null

        } else {

            if ((isTracingEnabled && tc.isDebugEnabled())) {
                ++sop_gets_notfound;
            }

        }

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "getSharedConnection", mcWrapper);
        }

        return mcWrapper;

    }

    private String dumpLTCInformation(MCWrapper mcWrapperTemp, String pmiName, Object affinity) {
        StringBuffer connectionLeakBuffer = new StringBuffer();
        /*
         * The filter list is just an attempt to reduce the stack information in the trace and to narrow in
         * on the customer application code that is using connection get, use, get, use within an LTC.
         *
         * The goal is for the customer application to be at the top of the printed trace information.
         */
        String filterList[] = new String[] { "com.ibm.ejs.j2c.SharedPool.dumpLTCInformation",
                                             "com.ibm.ejs.j2c.SharedPool.getSharedConnection",
                                             "com.ibm.ejs.j2c.PoolManager.reserve",
                                             "com.ibm.ejs.j2c.ConnectionManager.allocateMCWrapper",
                                             "com.ibm.ejs.j2c.ConnectionManager.allocateConnection",
                                             "com.ibm.ws.rsadapter.jdbc.WSJdbcDataSource.getConnection" };
        /*
         * Dump the current connection requests information
         */
        Throwable currentThrowable = new Throwable();
        StackTraceElement[] ste = currentThrowable.getStackTrace();
        connectionLeakBuffer.append("Current connection request in LTC " + affinity);
        connectionLeakBuffer.append(CommonFunction.nl);
        connectionLeakBuffer.append("       Application connection request stack:" + CommonFunction.nl);
        boolean processFilterList = true;
        for (StackTraceElement steValue : ste) {
            boolean addStackInfo = true;
            if (processFilterList) {
                for (String filterItem : filterList) {
                    if (steValue.toString().contains(filterItem)) {
                        addStackInfo = false;
                        break;
                    }
                }
            }
            if (addStackInfo) {
                processFilterList = false;
                connectionLeakBuffer.append("          " + steValue.toString() + CommonFunction.nl);
            }
        }
        /*
         * Get the previously stored stack from the already in use managed connection. This connection has a connection handle
         * count of 1 and is being used in an LTC.
         */
        Throwable previousStoredThrowable = ((com.ibm.ejs.j2c.MCWrapper) mcWrapperTemp).getInitialRequestStackTrace();
        if (previousStoredThrowable != null) {
            /*
             * Dump the initial connection request that we are matching
             * if available.
             */
            connectionLeakBuffer.append(CommonFunction.nl);
            connectionLeakBuffer.append("Previous connection request in LTC " + mcWrapperTemp.getSharedPoolCoordinator());
            connectionLeakBuffer.append(CommonFunction.nl);
            connectionLeakBuffer.append("       Application connection request stack:" + CommonFunction.nl);
            ste = previousStoredThrowable.getStackTrace();
            processFilterList = true;
            for (StackTraceElement steValue : ste) {
                boolean addStackInfo = true;
                if (processFilterList) {
                    for (String filterItem : filterList) {
                        if (steValue.toString().contains(filterItem)) {
                            addStackInfo = false;
                            break;
                        }
                    }
                }
                if (addStackInfo) {
                    processFilterList = false;
                    connectionLeakBuffer.append("          " + steValue.toString() + CommonFunction.nl);
                }
            }
            connectionLeakBuffer.append(CommonFunction.nl);
        }
        return connectionLeakBuffer.toString();
    }

    /**
     * Adds a shared connection to the list
     */

    protected void setSharedConnection(Object affinity, MCWrapper mcWrapper) {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "setSharedConnection");
        }
        /*
         * Set the shared pool affinity on the mcWrapper for comparing in the
         * getSharedConnection method
         */
        mcWrapper.setSharedPoolCoordinator(affinity);
        mcWrapper.setSharedPool(this);
        synchronized (sharedLockObject) {

            /*
             * Add the mcWrapper to the mcWrapper array list
             */
            mcWrapperList[mcWrapperListSize] = mcWrapper;
            mcWrapper.setPoolState(2);
            ++mcWrapperListSize;
            if (mcWrapperListSize >= objectArraySize) {
                /*
                 * We need to increase our size
                 */
                objectArraySize = objectArraySize * 2;
                mcWrapperListTemp = new MCWrapper[objectArraySize];
                System.arraycopy(mcWrapperList, 0, mcWrapperListTemp, 0, mcWrapperList.length);
                mcWrapperList = mcWrapperListTemp;
            }
        } // end synchronized (sharedLockObject)

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "setSharedConnection");
        }

    }

    //  protected void increaseMCWrapperObjectArraySize(int newSize) {
    //    synchronized (sharedLockObject) {
    //     * More work here for size less than objectArraySize
    //      mcWrapperListTemp = new MCWrapper[newSize];
    //      for (int i = 0; i < mcWrapperListSize; ++i) {
    //        mcWrapperListTemp[i] = mcWrapperList[i];
    //      }
    //      mcWrapperList = mcWrapperListTemp;
    //      objectArraySize = newSize;
    //    }
    //  }

    /**
     * Remove a shared connection from the list
     */

    protected void removeSharedConnection(MCWrapper mcWrapper) throws ResourceException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        //Used to sanity check that the mcWrapperList was reduced by one.
        int sizeDifference;

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "removeSharedConnection");
        }

        synchronized (sharedLockObject) {

            sizeDifference = mcWrapperListSize; //set to the orginal size
            if (mcWrapperListSize == 1) {
                /*
                 * If there is only one mcWrapper in the list, the odds of it
                 * being the right one are good. So, optimistically
                 * remove it from the mcWrapper list and compare mcWrappers.
                 * If we are right, we are happy and we exit the method.
                 * If we are wrong (This should not happen), we are sad
                 * and need to add back into the list
                 */
                if (mcWrapper == mcWrapperList[0]) {
                    mcWrapperListSize = 0;
                    mcWrapperList[mcWrapperListSize] = null;
                }
            } else { // Too many mcWrappers to feel optimistic
                /*
                 * If there is more than one mcWrapper in the list, the odds of it
                 * being the right are not as good. So, we
                 * get it from the mcWrapper list and compare mcWrappers.
                 * If the compare is equal, we remove the mcWrapper from the list.
                 * If the compare is not equal, we continue to look through the list
                 * hoping to match mcWrappers. In the normal case we will find a
                 * match and remove the mcWrapper from the list and exit.
                 */
                for (int i = 0; i < mcWrapperListSize; ++i) {
                    if (mcWrapper == mcWrapperList[i]) {
                        mcWrapperList[i] = mcWrapperList[--mcWrapperListSize]; // shift all remain wrappers up to fill any open location.
                        mcWrapperList[mcWrapperListSize] = null; // - For safety, setting the last one to null is good.
                    }
                }
            }
            sizeDifference = sizeDifference - mcWrapperListSize; //take the difference between orginal size and now

        }

        if (sizeDifference != 1) {
            /*
             * We should never throw this exception unless a resource adapter
             * replace the Subject or CRI references. They are not allow to
             * replace the Subject or CRI references when the connection is
             * being used.
             */
            Tr.error(tc, "SHAREDPOOL_REMOVESHAREDCONNECTION_ERROR_J2CA1003", mcWrapper);
            ResourceException re = new ResourceException("removeSharedConnection: failed to remove MCWrapper " + mcWrapper.toString());
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        re,
                                                        "com.ibm.ejs.j2c.poolmanager.SharedPool.removeSharedConnection",
                                                        "184",
                                                        this);
            _pm.activeRequest.decrementAndGet();
            throw re;

        } else {
            mcWrapper.setPoolState(0);
        }

        if (isTracingEnabled && tc.isEntryEnabled()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Removed connection");
            }
            Tr.exit(this, tc, "removeSharedConnection");
        }

    }

    /**
     * This is not thread safe. Lock on sharedLockObject is required
     * Do not call this method unless the new maxConnection size is greater than
     * the previous size.
     *
     * @concurrency concurrent
     * @param newSize
     */
    //  protected void updateSharedPoolObjectArray(int newSize){
    //    if(newSize < 1) {
    //       * if maxConnections is set to zero, we need to initialize the
    //       * initialSize to something.  Since a value of zero represents
    //       * infinite number of connection can be created and a heavy load
    //       * may occur, an initial value of 100 should be OK.  If more is
    //       * needed, the mcWrapperList object will need to increase its size.
    //      newSize = 100;
    //      sizeCheckNeeded = true;
    //    } else {
    //       * Since we have a fixed number of connections, we will just initialize
    //       * the array to the maxConnections.  The size of the array will not need
    //       * to be changed.
    //      sizeCheckNeeded = false;
    //    }
    //    if (newSize > objectArraySize) {
    //       * We only want to increase the shared pool object array size at
    //       * this time.  In future releases this restriction may change.
    //       * Create the new mcWrapperList object array that will contain MCWrappers
    //      mcWrapperListTemp = new MCWrapper[newSize];
    //      for (int i = 0; i < mcWrapperListSize; ++i) {
    //         * Copy the existing mcWrappers to the new list.
    //        mcWrapperListTemp[i] = mcWrapperList[i];
    //      }
    //       * Replace the mcWrapper list with the new list.
    //      mcWrapperList = mcWrapperListTemp;
    //    }
    //  }
    /*
     * Return the MCWrapper list
     */
    MCWrapper[] getMCWrapperList() {
        return mcWrapperList;
    }

    protected int getMCWrapperListSize() {
        return mcWrapperListSize;
    }

    /**
     * Returns the current state of the used pool as a String.
     *
     * @return java.lang.String
     * @concurrency concurrent
     */
    @Override
    public String toString() {

        StringBuilder aBuffer = new StringBuilder();

        aBuffer.append("SharedPool object:");
        aBuffer.append("  Number of connection in shared pool: ");
        synchronized (sharedLockObject) {
            aBuffer.append(mcWrapperListSize);
            aBuffer.append(CommonFunction.nl);
            aBuffer.append(Arrays.toString(mcWrapperList));
        }

        return aBuffer.toString();

    }

    private static class Equals implements PrivilegedAction<Boolean> {

        Subject _s1, _s2;

        public final void setSubjects(Subject s1, Subject s2) {
            _s1 = s1;
            _s2 = s2;
        }

        @Override
        public Boolean run() {
            boolean subjectsMatch = false;
            if (checkCredentials(_s1.getPrivateCredentials(), _s2.getPrivateCredentials())) {
                // if the private credentials match check public creds.
                subjectsMatch = checkCredentials(_s1.getPublicCredentials(), _s2.getPublicCredentials());
            }

            return subjectsMatch;
        }

        /**
         * This method is replacing checkPrivateCredentials and checkPublicCredentials. The code in both methods
         * contained the same logic.
         *
         * This method needs to be called two times. The first time with private credentials and the second time
         * with public credentials. Both calls must return true for the Subjects to be equal.
         *
         * This new method fixes apar The implementation of Set.equals(Set) is synchronized for the
         * Subject object. This can not be synchronized for the J2C and RRA code implementations. We may be
         * able to code this differently, but I believe this implementation performs well and allows for trace
         * messages during subject processing. This method assumes the Subject's private and public credentials
         * are not changing during the life of a managed connection and managed connection wrapper.
         *
         * @param s1Credentials
         * @param s2Credentials
         * @return
         */
        private boolean checkCredentials(Set<Object> s1Credentials, Set<Object> s2Credentials) {
            boolean rVal = false;

            if (s1Credentials != s2Credentials) {
                if (s1Credentials != null) {
                    if (s2Credentials != null) {
                        /*
                         * Check to see if the sizes are equal. If the first one and second one are
                         * equal, then check one of them to see if they are empty.
                         * If both are empty, they are equal, If one is empty and the other is not,
                         * they are not equal.
                         */
                        int it1size = s1Credentials.size();
                        int it2size = s2Credentials.size();
                        if (it1size == it2size) {
                            if (it1size == 0) {
                                if (TraceComponent.isAnyTracingEnabled()
                                    && tc.isDebugEnabled())
                                    Tr.debug(this, tc, "Processing credential sets, both are empty, They are equal");
                                return true;
                            }
                        } else {
                            if (TraceComponent.isAnyTracingEnabled()
                                && tc.isDebugEnabled())
                                Tr.debug(this, tc, "Processing credential sets, sets do not contain the same number of elements. They are not equal");
                            return false;
                        }

                        if (it1size > 1) {
                            /*
                             * This is the slow path. In most cases, we should not use this code path.
                             * We should have no objects or one object for each set.
                             *
                             * This is an unsynchronized unordered equals of two Sets.
                             */
                            Iterator<Object> it1 = s1Credentials.iterator();
                            int objectsEqual = 0;
                            while (it1.hasNext()) {
                                Object s1Cred = it1.next();
                                Iterator<Object> it2 = s2Credentials.iterator();
                                while (it2.hasNext()) {
                                    Object s2Cred = it2.next();
                                    if (s1Cred != null) {
                                        if (!s1Cred.equals(s2Cred)) {
                                            // Objects are not equal
                                            continue;
                                        }
                                    } else {
                                        if (s2Cred != null) {
                                            // Objects are not equal, one object is null");
                                            continue;
                                        }
                                    }
                                    ++objectsEqual;
                                    break;
                                }
                            }
                            // if(it2.hasNext()){
                            // if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() &&
                            // tc.isDebugEnabled()) Tr.debug(this, tc, " - Object sets do not
                            // contain the same number of elements, they are not equal");
                            // } else {
                            // have same number of private credentials, they are =
                            if (objectsEqual == it1size) {
                                // add trace at this point.
                                rVal = true;
                            }
                            // }
                        } else { // optimized path since we only have one object in both
                                 // sets to compare.
                            Iterator<Object> it1 = s1Credentials.iterator();
                            Iterator<Object> it2 = s2Credentials.iterator();

                            Object s1Cred = it1.next();
                            Object s2Cred = it2.next();
                            if (s1Cred != null) {
                                if (!s1Cred.equals(s2Cred)) {
                                    if (TraceComponent.isAnyTracingEnabled()
                                        && tc.isDebugEnabled())
                                        Tr.debug(this, tc, "PK69110 - Objects are not equal");
                                    return false;
                                }
                            } else {
                                if (s2Cred != null) {
                                    if (TraceComponent.isAnyTracingEnabled()
                                        && tc.isDebugEnabled())
                                        Tr.debug(this, tc,
                                                 "PK69110 - Objects are not equal, one objest is null");
                                    return false;
                                }
                            }
                            rVal = true;
                        }
                    } // second check for null
                } // first check for null
            } else {
                rVal = true;
            }

            return rVal;
        }

        //      *  - For the subjects to be equal, when the connection is inuse, we need to check the
        //      *   identity pricipal.
        //     private boolean checkIdentityPricipals(Set idPrincipal){
        //       boolean rVal = false;
        //      Set idPrincipal1 = _s1.getPrincipals(IdentityPrincipal.class);
        //       if(idPrincipal1 != idPrincipal){
        //         if(idPrincipal1 != null){
        //           rVal = idPrincipal1.equals(idPrincipal);
        //         }
        //       } else {
        //         rVal = true;
        //       }
        //       return rVal;
        //     }
    }
}
