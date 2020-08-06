package com.ibm.tx.jta.impl;

/*******************************************************************************
 * Copyright (c) 2002, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import java.io.File;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.transaction.Status;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.tx.TranConstants;
import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.tx.jta.XAResourceFactory;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.Transaction.JTA.JTAResource;
import com.ibm.ws.Transaction.JTA.Util;
import com.ibm.ws.Transaction.JTA.XAReturnCodeHelper;
import com.ibm.ws.Transaction.JTA.XARminst;
import com.ibm.ws.recoverylog.spi.RecoverableUnit;
import com.ibm.ws.recoverylog.spi.RecoverableUnitSection;
import com.ibm.ws.recoverylog.spi.RecoveryLog;

/**
 * XARecoveryData is a specialization of PartnerLogData
 *
 * The log data object is an XARecoveryWrapper and this class provides
 * methods to support the use of this particular data type.
 */
public class XARecoveryData extends PartnerLogData {
    private static final TraceComponent tc = Tr.register(XARecoveryData.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    protected boolean _supportsIsSameRM;

    // The classloader used to load the XARecoveryWrapper in recovery scenarios.
    protected ClassLoader _recoveryClassLoader;

    // Parsed log data on recovery
    protected byte[] _wrapperData;
    protected final String[] _extDirs;
    protected final int _priority;

    protected boolean auditRecovery = ConfigurationProviderManager.getConfigurationProvider().getAuditRecovery();

    /**
     * When we're calling setTransactionTimeout on XAResources, we normally stop after the first exception from an RM.
     * When this field is set, we carry on setting the timeout.
     */
    private static boolean _continuePropagatingXAResourceTimeout = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
        @Override
        public Boolean run() {
            return Boolean.getBoolean("com.ibm.tx.continuePropagatingXAResourceTimeout");
        }
    });

    /**
     * This field says whether to call setTransactionTimeout on XAResources
     */
    protected boolean _propagateXAResourceTransactionTimeout = ConfigurationProviderManager.getConfigurationProvider().getPropagateXAResourceTransactionTimeout();

    /**
     * Ctor when called from registration of an XAResource
     *
     * @param failureScopeController
     * @param logData
     */
    protected XARecoveryData(FailureScopeController failureScopeController, XARecoveryWrapper logData) {
        super(logData, failureScopeController);
        _sectionId = TransactionImpl.XARESOURCEDATA_SECTION;
        _priority = logData.getPriority();
        // Required to build a recoveryClassLoader on retries
        _extDirs = logData.getXAResourceFactoryClasspath();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "XARecoveryData", new Object[] { failureScopeController, logData, this });
    }

    /**
     * Ctor when called for recovery or registration of an XAResource for z/OS HA recovery
     *
     * @param partnerLog
     * @param logData
     * @param id
     * @param priority
     */
    /* @LIDB1578-22A */
    public XARecoveryData(RecoveryLog partnerLog, byte[] serializedLogData, long id, int priority) {
        super(serializedLogData, null, id, partnerLog);
        _priority = priority;

        // Extract serialized wrapper data and the classpath array from the serialized logdata
        int delimiterPosition = 0;

        for (int i = 0; i < serializedLogData.length; i++) {
            if (serializedLogData[i] == 0) {
                delimiterPosition = i;
                break;
            }
        }

        _wrapperData = new byte[serializedLogData.length - delimiterPosition - 1];
        System.arraycopy(serializedLogData, delimiterPosition + 1, _wrapperData, 0, _wrapperData.length);

        if (delimiterPosition > 0) {
            final byte[] classpathBytes = new byte[delimiterPosition];

            System.arraycopy(serializedLogData, 0, classpathBytes, 0, classpathBytes.length);

            final String xaResInfoClasspath = new String(classpathBytes);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Classpath data recovered", xaResInfoClasspath);

            final StringTokenizer tokenizer = new StringTokenizer(xaResInfoClasspath, File.pathSeparator);
            _extDirs = new String[tokenizer.countTokens()];

            for (int i = 0; i < _extDirs.length; i++) {
                _extDirs[i] = tokenizer.nextToken();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "_extDirs[" + i + "] = " + _extDirs[i]);
            }
        } else {
            _extDirs = null;
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "XARecoveryData", new Object[] { partnerLog, serializedLogData, id, priority, this });
    }

    /*
     * Perform a pre-log data check prior to logging the XARecoveryData.
     * Need to validate that the classpath hasnt changed by admin resource
     * configuration during runtime. If it has we need to log again.
     * This will only occur in main-line calls as any recovered data will
     * already be written to disk. We can just use the recovery manager
     * from the configuration object.
     */
    @Override
    protected void preLogData() throws Exception {
        _fsc.getRecoveryManager().waitForReplayCompletion();
    }

    /*
     * Perform a post-log data check after logging the XARecoveryData prior to the force.
     * Use this to log the priority in a separate log unit section to the
     * main XARecoveryData serialized wrapper and classpath data. Note: this
     * method is not called if we have no logs defined.
     */
    @Override
    protected void postLogData(RecoverableUnit ru) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "postLogData");

        // Only log if priority is non-zero to keep compatability with old releases
        if (_priority != JTAResource.DEFAULT_COMMIT_PRIORITY) {
            // Let caller catch any exceptions as it is already handling RU/RUS failures
            final RecoverableUnitSection section = ru.createSection(TransactionImpl.RESOURCE_PRIORITY_SECTION, true);
            section.addData(Util.intToBytes(_priority));
        }

    }

    /*
     * deserialize is called from recover prior to recovery and repeatedly if it cannot deserialize
     * for some reason. The logic of repeating this is that each record from the resource files
     * is passed to replay. replay deserializes the record and saves the information in the
     * recoveryTable. We do not need to check for duplicate recoveryIds as these are filtered out
     * by the RecoveryLogManager cache. Note: we can get multiple records which match the same
     * resource manager. This means that on recover we can get the same xids twice. This does not
     * matter as both copies of the same xid will match the same JTAXAResource. XARminst will
     * cope with a JTAXAResource from an apparent different ressource manager (rmid) - the rmid is
     * mainly used to determine if we are not able to contact a resource manager on recovery.
     *
     * deserialize is also called various times on z/OS when reading a partner log record. Note:
     * on z/OS many SRs may be running and each has its own cached PartnerLogTable which may not be
     * in step with the recovery log. At certain points, the log is re-read and entries deserialized
     * to compare with the cached table.
     *
     * @param parentClassLoader
     */
    public void deserialize(ClassLoader parentClassLoader) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "deserialize", new Object[] { this, parentClassLoader });

        // Deserialize the logData object.  If this fails, we may retry later.  We should
        // always be able to deserialize, otherwise we can never recover.  There should only
        // ever be resource recovery records in the log if:
        //
        // 1) we crashed - there may have been active txns
        // 2) we closed down with active txns
        // 3) we closed down with no active txns but failed to recover all resources at startup
        //
        // If we shutdown normally with no active txns, there will be no resource recovery
        // records to recover.  Note: we log the XA recovery log data at enlist time - and
        // we may never ever perform a prepare to the RM.  Also, on z/OS we never clean out the
        // logs on shutdown although this may change in the future,

        // Before we deserialize the data from the object we need to determine if there
        // is any classpath data logged. If there is then we may have to setup a special classloader
        // with this data so that the serializedLogData can be successfully deserialized.
        // We save the resulting class loader as we will need it again when we need to create
        // an xaresource for recover, commit, rollback, forget, etc. since we may not have the
        // correct parentClassLoader available at that point.   This works as we never use the
        // same XARecoveryData record for recovery and normal running.

        final XARecoveryWrapper wrapper = XARecoveryWrapper.deserialize(_wrapperData);
        if (wrapper != null) {
            if (_extDirs != null)
                wrapper.setXAResourceFactoryClasspath(_extDirs);
            wrapper.setPriority(_priority);
            _logData = wrapper;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "deserialize");
    }

    public XARecoveryWrapper getXARecoveryWrapper() {
        return (XARecoveryWrapper) _logData;
    }

    public Serializable getXAResourceInfo() {
        return ((XARecoveryWrapper) _logData).getXAResourceInfo();
    }

    public int getPriority() {
        return _priority;
    }

    /*
     * Create a XAResourceFactory and obtain an XAResource.
     * The factory and resource are returned in an XARMinst object which can be
     * used for xa_recover calls and closeConnection when complete.
     *
     * This call is made from this.recover and from JTAXAResourceImpl.reconnectRM
     * either during recovery or retry of a failed XAResource. Care is needed when
     * considering the recoveryClassLoader (RCL). We need to use it for xa_recover
     * and subsequently on any xa_commit or xa_rollback which may be issued from
     * other threads asynchronously to the recover processing - hence we save the
     * RCL during deserialization and keep it available for the remainder of recovery.
     * When the PartnerLogData inuse count drops to zero (ie recovery is complete),
     * the RCL is released for garbage collection. For the normal running case,
     * we always build a RCL on the fly.
     */
    public XARminst getXARminst() throws XAException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getXARminst", new Object[] { this });

        XAResource xaRes = null;
        XAResourceFactory xaResFactory;
        Serializable xaResInfo = null;

        final XARecoveryWrapper xarw = (XARecoveryWrapper) _logData;

        if ((xaResInfo = getXAResourceInfo()) instanceof DirectEnlistXAResourceInfo) {
            xaResFactory = null;
            xaRes = ((DirectEnlistXAResourceInfo) xaResInfo).getXAResource();
        } else {
            // We should always have a non-null non-blank factory classname as it is
            // validated by registerResourceInfo before creating an XARecoveryWrapper
            final String xaResFactoryClassName = xarw.getXAResourceFactoryClassName();

            /*
             * Starting in 8.5 we can have a filter registered instead of a class name
             */
            xaResFactory = XARecoveryDataHelper.lookupXAResourceFactory(xaResFactoryClassName);
            if (xaResFactory != null) {
                // Have recovered xaResourceFactory
                try {
                    xaRes = xaResFactory.getXAResource(xaResInfo);
                } catch (XAResourceNotAvailableException e) {
                    // Swallow this exception we'll follow the "traditional" codepath below
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "getXARminst", e);
                }
            }

            if (xaRes == null) {
                // If this looks like a filter we'll try again later
                if (xaResFactoryClassName.startsWith("(")) {
                    final XAException e = new XAException(XAException.XAER_RMFAIL);
                    e.initCause(new XAResourceNotAvailableException());
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "getXARminst", e);
                    throw e;
                }

                // Traditional
                Class<?> xaResFactoryClass = null;

                try {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "calling Class.forName", xaResFactoryClassName);
                    xaResFactoryClass = Class.forName(xaResFactoryClassName);

                    // Trace the class and its loader - this should work as we are not in app code
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "xaResFactoryClass", new Object[] { xaResFactoryClass, xaResFactoryClass.getClassLoader() });

                    xaResFactory = (XAResourceFactory) xaResFactoryClass.newInstance();
                } catch (Throwable t) {
                    //
                    // If we cannot create one of our known factories, then we are broken.
                    // Flag this and carry on.  We try to recover as much as possible for all resources.
                    //
                    FFDCFilter.processException(t, "com.ibm.tx.jta.impl.XARecoveryData.getXARminst", "419");
                    Tr.error(tc, "WTRN0004_CANT_CREATE_XARESOURCEFACTORY", new Object[] { xaResFactoryClassName, t });
                    final XAException xae = new XAException(XAException.XAER_RMERR);
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "getXARminst", xae);
                    throw xae;
                }

                try {
                    // If we can't proceed of if the factory gives us a null XAResource
                    // we want to retry
                    xaResInfo = getXAResourceInfo();
                    if (null == (xaRes = xaResFactory.getXAResource(xaResInfo))) {
                        throw new XAResourceNotAvailableException();
                    }
                } catch (XAResourceNotAvailableException e) {
                    FFDCFilter.processException(e, "com.ibm.tx.jta.impl.XARecoveryData.getXARminst", "491", this);
                    final Throwable t = new XAException(XAException.XAER_RMFAIL).initCause(e);
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "getXARminst", t);
                    throw (XAException) t;
                } catch (Throwable e) {
                    //
                    // Fatal error - mark it as such for when we return
                    //
                    FFDCFilter.processException(e, "com.ibm.tx.jta.impl.XARecoveryData.getXARminst", "563");
                    Tr.error(tc, "WTRN0005_CANT_RECREATE_XARESOURCE", new Object[] { xaResInfo, e });
                    final Throwable t = new XAException(XAException.XAER_RMERR).initCause(e);
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "getXARminst", t);
                    throw (XAException) t;
                }

            }
        }

        // Trace the resource and its loader - this should work as we are not in app code
        if (tc.isDebugEnabled())
            Tr.debug(tc, "xaResource", new Object[] { xaRes, xaRes.getClass().getClassLoader() });

        //
        // Create XARminst, which is a Resource Manager proxy.
        // Need to add xaResFactory to XARminst during
        // recovery, so we can closeConnection after recovery.
        //
        final XARminst xarm = new XARminst(xaRes, xaResFactory);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "getXARminst", xarm);
        return xarm;
    }

    @Override
    public boolean recover(ClassLoader cl, Xid[] knownXids, byte[] failedStoken, byte[] cruuid, int restartEpoch) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recover", new Object[] { cl, knownXids, failedStoken, cruuid, restartEpoch, this });

        // If we've already recovered this XARecoveryData entry, skip to next
        if (_recovered) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recover", "recovered");
            return true;
        }

        // If this entry is terminating then do not try to process it
        if (_terminating) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recover", "terminating");
            return false; // flag to retry later
        }

        // Check we have already deserialized the log data
        if (_logData == null) {
            deserialize(cl);

            if (_logData == null) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "recover", "deserialize failed");
                return RecoveryManager.recoveryOnlyMode;
            }
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "recovering", _logData);

        //
        // Create XARminst, which is a Resource Manager proxy.
        //
        XARminst xarm = null;
        try {
            auditXaRecover(getXAResourceInfo());
            xarm = getXARminst();
            if (xarm == null)
                throw new XAException(XAException.XAER_RMERR);
        } catch (XAException xae) {
            boolean result; // recovery retry complete status
            switch (xae.errorCode) {
                case XAException.XA_HEURMIX:
                    // Non-retriable condition - RA uninstalled
                    // Cant recover again so mark recovered and decrement use count so
                    // the entry will get deleted from the log
                    decrementCount();
                    _recovered = true;
                    // return _recovered status
                case XAException.XAER_RMFAIL:
                    // Retriable condition - unable to create XAResource
                    result = _recovered;
                    break;
                case XAException.XAER_RMERR:
                default:
                    // Failure case - unable to create factory or XAResource
                    // Retry in normal server state but not for recoveryOnly mode -
                    // LIDB3645 probably no point retrying,
                    // but dont mark entry recovered as we want it to stay in the log
                    // so it will be retried at next server restart.
                    result = RecoveryManager.recoveryOnlyMode;
                    break;
            }
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recover", result);
            return result;
        }

        Xid[] inDoubt = null;
        int numXids = 0;
        try {
            /*----------------------------------------------------------*/
            /* Drive recovery on that resource. Pass both Start and */
            /* End flags to the resource so that the complete list of */
            /* Xids is obtained. Our WS390XARminst wrapper ensures */
            /* that a null list is never returned (the length is zero). */
            /*----------------------------------------------------------*/
            inDoubt = xarm.recover();
            _recovered = true; // Flag we have successfully issued recovery
            numXids = inDoubt.length;
        } catch (Throwable t) {
            // FFDC and messages logged in xarm.recover()
            FFDCFilter.processException(t, "com.ibm.tx.jta.impl.XARecoveryData.recover", "564", this);

            // Issue #11556, need to close connection prior to method exit to avoid a connection leak.
            xarm.closeConnection();

            if (tc.isEntryEnabled())
                Tr.exit(tc, "recover", false);
            return false;
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Resource returned " + numXids + " Xids");
            for (int n = 0; n < numXids; n++) {
                if (inDoubt[n] == null)
                    continue;
                int formatID = inDoubt[n].getFormatId();
                byte[] gtrid = inDoubt[n].getGlobalTransactionId();
                byte[] bqual = inDoubt[n].getBranchQualifier();
                Tr.debug(tc, "Trace Xid[" + n + "] FormatID: " + Integer.toHexString(formatID));
                Tr.debug(tc, "Trace Xid[" + n + "] Gtrid: " + Util.toHexString(gtrid));
                Tr.debug(tc, "Trace Xid[" + n + "] Bqual: " + Util.toHexString(bqual));
            }
        }

        /*----------------------------------------------------------*/
        /* Filter out all non-WAS formatIds from the inDoubt list. */
        /*----------------------------------------------------------*/
        ArrayList xidList = filterXidsByType(inDoubt);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "After type filter, Xids to recover " + xidList.size());
        }

        /*----------------------------------------------------------*/
        /* Filter out all Xids that don't have this */
        /* cruuid, or whose epoch number is greater than or equal */
        /* to the current epoch number. */
        /*----------------------------------------------------------*/
        xidList = filterXidsByCruuidAndEpoch(xidList, cruuid, restartEpoch);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "After filter by cruuid and epoch, Xids " +
                         "to recover " + xidList.size());
        }

        auditXaRecoverCount(getXAResourceInfo(), numXids, xidList.size());

        /*----------------------------------------------------------*/
        /* For each Xid that is left, see if it belongs in one of */
        /* the transactions (XID_t) that we know about. If it */
        /* doesn't, forget it. */
        /*----------------------------------------------------------*/
        for (int y = 0; y < xidList.size(); y++) {
            final XidImpl ourXid = (XidImpl) xidList.get(y);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Recovering Xid[" + y + "]", ourXid);
            }
            if (ourXid.getEpoch() < (restartEpoch - 1)) {
                auditLateEpoch(ourXid, getXAResourceInfo());
            }

            /*------------------------------------------------------*/
            /* If we have no transactions to recover, or if we don't */
            /* know about this Xid, we roll it back. */
            /*------------------------------------------------------*/
            if (knownXids == null || canWeForgetXid(ourXid, knownXids)) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Found XID with no associated transaction");

                // Roll the XID back if the transaction is not recognised.  This
                // happens when the RM has recorded its prepare vote, but the
                // TM has not recorded its prepare vote.
                try {
                    auditSendRollback(ourXid, getXAResourceInfo());
                    xarm.rollback(ourXid);
                    auditRollbackResponse(XAResource.XA_OK, ourXid, getXAResourceInfo());
                } catch (XAException xae) {
                    FFDCFilter.processException(xae, "com.ibm.tx.jta.impl.XARecoveryData.recover", "660", this);
                    final int errorCode = xae.errorCode;
                    auditRollbackResponse(errorCode, ourXid, getXAResourceInfo());
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "XAException: error code " + XAReturnCodeHelper.convertXACode(errorCode), xae);
                    // Force retry of recovery for retriable errors - ignore non-recoverable ones
                    // such as XAER_INVAL or XAER_PROTO
                    if ((errorCode == XAException.XAER_RMFAIL) ||
                        (errorCode == XAException.XAER_RMERR)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Forcing retry of recovery");
                        _recovered = false;
                    }
                }
            }
        } // end for y

        //
        // Now we have finished with the XAResource we can destroy it
        //
        xarm.closeConnection();

        if (_recovered)
            decrementCount(); // recovery good

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recover", new Boolean(_recovered));

        return _recovered;
    }

    /**
     * Removes all non-WAS Xids from an array of Xids, and puts them
     * in an ArrayList structure.
     *
     * @param xidArray An array of generic Xids.
     * @return An ArrayList of XidImpl objects.
     */
    protected ArrayList filterXidsByType(Xid[] xidArray) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "filterXidsByType", xidArray);

        final ArrayList<XidImpl> xidList = new ArrayList<XidImpl>();

        if (xidArray != null) {
            /*----------------------------------------------------------*/
            /* Iterate over the list of returned xids, and insert them */
            /* into a new list containing all the xids that have been */
            /* recovered thus far. We don't have to worry about */
            /* duplicates because every resource is guaranteed to have a */
            /* unique bqual + gtrid combination. */
            /*----------------------------------------------------------*/
            for (int y = 0; y < xidArray.length; y++) {
                // PQ56777 - Oracle can return entries with null in them.
                // normally(?) it will be the last in the list.  It
                // appears to happen if an indoubt transaction on the
                // database completes during our call to recover.
                if (xidArray[y] == null) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "RM has returned null inDoubt Xid entry - " + y);
                    continue;
                }

                /*------------------------------------------------------*/
                /* We only want to add this Xid to our list if it is */
                /* one that we generated. */
                /*------------------------------------------------------*/
                if (XidImpl.isOurFormatId(xidArray[y].getFormatId())) {
                    /*--------------------------------------------------*/
                    /* It is possible that the Xid we get back from the */
                    /* RM is actually an instance of our XidImpl. In */
                    /* the case that it's not, we have to re-construct */
                    /* the XidImpl so that we can extract the xid bytes. */
                    /*--------------------------------------------------*/
                    XidImpl ourXid = null;
                    if (xidArray[y] instanceof XidImpl) {
                        ourXid = (XidImpl) xidArray[y];
                    } else {
                        ourXid = new XidImpl(xidArray[y]);
                        // Check the bqual is one of ours...
                        // as V5.1 also uses the same formatId but with
                        // different length encoding
                        if (ourXid.getBranchQualifier().length != XidImpl.BQUAL_JTA_BQUAL_LENGTH) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Xid is wrong length - " + y);
                            continue;
                        }
                    }
                    xidList.add(ourXid);
                } /* if isOurFormatId() */
            } /* for each Xid */
        } /* if xidArray != null */

        if (tc.isEntryEnabled())
            Tr.exit(tc, "filterXidsByType", xidList);

        return xidList;
    }

    /**
     * Removes all Xids from an ArrayList that don't have our cruuid in
     * their bqual. Assumes that all Xids are XidImpls.
     *
     * @param xidList A list of XidImpls.
     * @param cruuid The cruuid to filter.
     * @param epoch The epoch number to filter.
     * @return An ArrayList of XidImpl objects.
     */
    protected ArrayList filterXidsByCruuidAndEpoch(ArrayList xidList,
                                                   byte[] cruuid,
                                                   int epoch) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "filterXidsByCruuidAndEpoch", new Object[] {
                                                                      xidList,
                                                                      cruuid,
                                                                      epoch });

        for (int x = xidList.size() - 1; x >= 0; x--) {
            final XidImpl ourXid = (XidImpl) xidList.get(x);
            final byte[] xidCruuid = ourXid.getCruuid();
            final int xidEpoch = ourXid.getEpoch();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Trace other Cruuid: " + xidCruuid + ", or: " + Util.toHexString(xidCruuid));
                Tr.debug(tc, "Trace my Cruuid: " + cruuid + ", or: " + Util.toHexString(cruuid));
            }
            if ((!java.util.Arrays.equals(cruuid, xidCruuid))) {

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "filterXidsByCruuidAndEpoch: cruuid is different: " + ourXid.getCruuid());

                xidList.remove(x);
            } else if (xidEpoch >= epoch) {

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "filterXidsByCruuidAndEpoch: xid epoch is " + xidEpoch);

                xidList.remove(x);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "filterXidsByCruuidAndEpoch", xidList);

        return xidList;
    }

    /**
     * Iterates over the list of known Xids retrieved from transaction
     * service, and tries to match the given javax.transaction.xa.Xid
     * with one of them.
     *
     * @param ourXid The javax.transaction.xa.Xid we are trying to match.
     * @param knownXids The array of Xids that are possible matches.
     * @return true if we find a match, false if not.
     */
    protected boolean canWeForgetXid(XidImpl ourXid, Xid[] knownXids) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "canWeForgetXid", new Object[] {
                                                          ourXid,
                                                          knownXids });

        if (tc.isDebugEnabled()) {
            // We are only called if knownXids != null
            for (int i = 0; i < knownXids.length; i++) {
                Tr.debug(tc, "tx xid[" + i + "] " + knownXids[i]);
            }
        }

        boolean forgetMe = true;

        /*----------------------------------------------------------------*/
        /* Yank the parts of the JTA xid. The branch qualifier will be */
        /* the full JTA branch qualifier, and will need to be shortened */
        /* to the same length of the transaction bqual so that the array */
        /* compare has a chance to complete successfully. */
        /*----------------------------------------------------------------*/
        final int jtaFormatId = ourXid.getFormatId();
        final byte[] jtaGtrid = ourXid.getGlobalTransactionId();
        final byte[] fullJtaBqual = ourXid.getBranchQualifier();
        byte[] jtaBqual = null;

        /*----------------------------------------------------------------*/
        /* We have to separate the transaction gtrid and bqual for the */
        /* array compares. These are places to store these items. */
        /*----------------------------------------------------------------*/
        int txnFormatId;
        byte[] txnGtrid;
        byte[] txnBqual;

        /*----------------------------------------------------------------*/
        /* Iterate over all the known XIDs (if there are none, we won't */
        /* iterate over anything). */
        /*----------------------------------------------------------------*/
        int x = 0;
        while ((x < knownXids.length) && (forgetMe == true)) {
            /*------------------------------------------------------------*/
            /* If this is the first XID, shorten the JTA bqual to the */
            /* length of the transaction bqual. We are assuming here */
            /* that all the transaction bquals are the same length. If */
            /* they arent, for some reason, we will need to revisit this. */
            /*------------------------------------------------------------*/
            if (x == 0) {
                final int bqualLength = (knownXids[x].getBranchQualifier()).length;
                jtaBqual = new byte[bqualLength];
                System.arraycopy(fullJtaBqual, 0,
                                 jtaBqual, 0,
                                 bqualLength);
            }

            /*------------------------------------------------------------*/
            /* Separate the transaction XID into comparable units. */
            /*------------------------------------------------------------*/
            txnFormatId = knownXids[x].getFormatId();
            txnGtrid = knownXids[x].getGlobalTransactionId();
            txnBqual = knownXids[x].getBranchQualifier();

            /*------------------------------------------------------------*/
            /* Compare the individual parts of the XID for equality. If */
            /* they are equal, set a boolean value and we can stop */
            /* checking. */
            /*------------------------------------------------------------*/
            if ((jtaFormatId == txnFormatId) &&
                (java.util.Arrays.equals(jtaGtrid, txnGtrid)) &&
                (java.util.Arrays.equals(jtaBqual, txnBqual))) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Xid has been matched to a transaction:",
                             ourXid);
                }
                auditTransactionXid(ourXid, knownXids[x], getXAResourceInfo());
                forgetMe = false;
            }

            x++;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "canWeForgetXid", forgetMe);
        return forgetMe;
    }

    // Called once recovery has finished with this XARecoveryData object
    // We can clean up as recovery entries are not shared with runtime entries.
    @Override
    public synchronized boolean clearIfNotInUse() {
        boolean cleared = super.clearIfNotInUse();
        if (cleared) {
            // Entry has been recovered and removed from the log
            // gc the class loader etc.
            _recoveryClassLoader = null;
        }
        return cleared;
    }

    /**
     * Retrieves the recovery classloader
     *
     * @return The recovery classloader
     */
    public ClassLoader getRecoveryClassLoader() /* @369064.2A */
    {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getRecoveryClassLoader", _recoveryClassLoader);
        }

        return _recoveryClassLoader;
    }

    public boolean supportsIsSameRM() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "supportsIsSameRM");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "supportsIsSameRM", Boolean.valueOf(_supportsIsSameRM));

        return _supportsIsSameRM;
    }

    protected void auditXaRecover(Serializable xaResInfo) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "auditXaRecover", xaResInfo);

        if (auditRecovery) {
            Tr.audit(tc, "WTRN0151_REC_XA_RECOVER", getRMInfo(xaResInfo));
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "auditXaRecover");
    }

    protected void auditXaRecoverCount(Serializable xaResInfo, int rms, int ours) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "auditXaRecoverCount", new Object[] { xaResInfo, rms, ours });

        if (auditRecovery) {
            Tr.audit(tc, "WTRN0146_REC_XA_RECOVERED", new Object[] { rms, getRMInfo(xaResInfo), ours });
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "auditXaRecoverCount");
    }

    public String getRMInfo(Serializable xaResInfo) {
        if (xaResInfo instanceof DirectEnlistXAResourceInfo) {
            return ((DirectEnlistXAResourceInfo) xaResInfo).getXAResource().toString();
        } else {
            return xaResInfo.toString();
        }
    }

    protected void auditLateEpoch(XidImpl xid, Serializable xaResInfo) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "auditLateEpoch", new Object[] { xid, xaResInfo });

        if (auditRecovery) {
            Tr.audit(tc, "WTRN0147_REC_XID_LATE", new Object[] { xid.printOtid(), getRMInfo(xaResInfo), xid.getEpoch() });
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "auditLateEpoch");
    }

    protected void auditSendRollback(XidImpl xid, Serializable xaResInfo) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "auditSendRollback", new Object[] { xid, xaResInfo });

        if (auditRecovery) {
            Tr.audit(tc, "WTRN0148_REC_XA_ROLLBACK", new Object[] { xid.printOtid(), getRMInfo(xaResInfo) });
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "auditSendRollback");
    }

    protected void auditRollbackResponse(int code, XidImpl xid, Serializable xaResInfo) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "auditRollbackResponse", new Object[] { xid, xaResInfo });

        if (auditRecovery) {
            Tr.audit(tc, "WTRN0150_REC_XA_ROLLEDBACK", new Object[] { xid.printOtid(), getRMInfo(xaResInfo), XAReturnCodeHelper.convertXACode(code) });
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "auditRollbackResponse");
    }

    private void auditTransactionXid(XidImpl xid, Xid txnXid, Serializable xaResInfo) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "auditTransactionXid", new Object[] { xid, txnXid, xaResInfo });

        if (auditRecovery) {
            String txnid;
            if (txnXid instanceof XidImpl) {
                txnid = ((XidImpl) txnXid).printOtid();
            } else {
                txnid = txnXid.toString();
            }
            Tr.audit(tc, "WTRN0149_REC_XA_TRAN", new Object[] { xid.printOtid(), getRMInfo(xaResInfo), getTransactionId(txnXid), Util.printStatus(getTransactionStatus(txnXid)) });
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "auditTransactionXid");
    }

    protected int getTransactionStatus(Xid xid) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getTransactionStatus", xid);

        if (tc.isDebugEnabled())
            Tr.debug(tc, "fsc, rm", new Object[] { _fsc, _fsc.getRecoveryManager() });
        // Get current list of recovering txns
        final TransactionImpl[] trans = (_fsc.getRecoveryManager().getRecoveringTransactions());
        // Go through list and look for a match.  We should find a match as we are called on the "recover" thread
        // and we have already found a match of the XID with a transaction XID.  Note: the XID matching is common
        // with ZOS which is why we need to go back and look for the TransactionImpl again.
        for (int i = 0; i < trans.length; i++) {
            // txnXid should be an XidImpl
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Matching xid with ", trans[i]);
            if (xid.equals(trans[i].getXidImpl())) {
                int status = trans[i].getStatus();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getTransactionStatus", Util.printStatus(status));
                return status;
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getTransactionStatus", "Failed to find a transaction - error");
        return Status.STATUS_UNKNOWN;
    }

    protected String getTransactionId(Xid xid) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getTransactionId", xid);

        if (tc.isDebugEnabled())
            Tr.debug(tc, "fsc, rm", new Object[] { _fsc, _fsc.getRecoveryManager() });
        // Get current list of recovering txns
        final TransactionImpl[] trans = (_fsc.getRecoveryManager().getRecoveringTransactions());
        // Go through list and look for a match.  We should find a match as we are called on the "recover" thread
        // and we have already found a match of the XID with a transaction XID.  Note: the XID matching is common
        // with ZOS which is why we need to go back and look for the TransactionImpl again.
        for (int i = 0; i < trans.length; i++) {
            // txnXid should be an XidImpl
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Matching xid with ", trans[i]);
            if (xid.equals(trans[i].getXidImpl())) {
                long id = trans[i].getLocalTID();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getTransactionId", id);
                return Long.toString(id);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getTransactionId", "Failed to find a transaction - error");
        return "null";
    }

    public boolean continuePropagatingXAResourceTimeout() {
        return _continuePropagatingXAResourceTimeout;
    }

    public boolean propagateXAResourceTransactionTimeout() {
        return _propagateXAResourceTransactionTimeout;
    }

    public void disablePropagatingXAResourceTimeout() {
        _propagateXAResourceTransactionTimeout = false;
    }
}