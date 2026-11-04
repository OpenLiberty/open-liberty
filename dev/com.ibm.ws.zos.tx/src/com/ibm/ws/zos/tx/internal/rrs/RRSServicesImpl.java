/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.zos.tx.internal.rrs;

import com.ibm.ws.zos.jni.NativeMethodManager;

/**
 * Provides the capability of calling native RRS services.
 */
public final class RRSServicesImpl implements RRSServices {

    protected NativeMethodManager nativeMethodManager = null;

    /**
     * Constructor.
     */
    public RRSServicesImpl() {

    }

    /**
     * Component activation process.
     */
    protected void activate() {

        // -------------------------------------------------------------------
        // When registering the native methods, we need to pass our classloader
        // down to the native code and use it to load classes in this bundle.
        // This is because the native method registration is driven from
        // the native service tracker, which has a different class loader than
        // we do.  So we can't rely on JNI FindClass() to find our classes.
        // -------------------------------------------------------------------
        Class<?> myClass = this.getClass();
        ClassLoader myClassLoader = myClass.getClassLoader();
        Object[] o = new Object[] { myClassLoader };

        nativeMethodManager.registerNatives(RRSServicesImpl.class, o);
    }

    /**
     * Component deactivation process.
     */
    protected void deactivate() {

    }

    /**
     * Sets the native method manager for JNI processing.
     *
     * @param nativeMethodManager
     */
    protected void setNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = nativeMethodManager;
    }

    /**
     * Clears the native method manager instance.
     *
     * @param nativeMethodManager
     */
    protected void unsetNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = null;
    }

    /**
     * Begins a transaction given the specified transaction mode.
     *
     * @param tranMode Global or Local
     * @return The object containing all information returned by the service call.
     */
    @Override
    public final BeginTransactionReturnType beginTransaction(int transactionMode) {
        return ntv_beginTransaction(transactionMode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int endUR(int action, byte[] currentUrToken) {
        return ntv_endUR(action, currentUrToken);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int backoutUR() {
        return ntv_backoutUR();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final RetrieveSideInformationFastReturnType retrieveSideInformationFast(byte[] ctxToken, int infoOptions) {
        return ntv_retireveSideInformationFast(ctxToken, infoOptions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final RetrieveURDataReturnType retrieveURData(byte[] uriToken,
                                                         int statesOption) {
        return ntv_retireveURData(uriToken, statesOption);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final RegisterResMgrReturnType registerResourceManager(int unregOption,
                                                                  byte[] globalData,
                                                                  byte[] rmNamePrefix,
                                                                  byte[] rmNameSTCK) {

        return ntv_registerResourceManager(unregOption,
                                           globalData,
                                           rmNamePrefix,
                                           (rmNamePrefix != null) ? rmNamePrefix.length : 0,
                                           rmNameSTCK);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int unregisterResourceManager(byte[] rmNameRegistryToken, byte[] rmRegistryToken) {
        return ntv_unregisterResourceManager(rmNameRegistryToken, rmRegistryToken);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final SetExitInformationReturnType setExitInformation(byte[] rmNameRegistryToken, byte[] rmRegistryToken, boolean recovery) {
        return ntv_setExitInformation(rmNameRegistryToken, rmRegistryToken, recovery);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int beginRestart(byte[] rmRegistryToken) {
        return ntv_beginRestart(rmRegistryToken);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int endRestart(byte[] rmRegistryToken) {
        return ntv_endRestart(rmRegistryToken);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final RetrieveLogNameReturnType retrieveLogName(byte[] rmRegistryToken) {
        return ntv_retrieveLogName(rmRegistryToken);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int setLogName(byte[] rmRegistryToken, byte[] logname) {
        return ntv_setLogName(rmRegistryToken, logname.length, logname);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final RetrieveWorkIdentifierReturnType retrieveWorkIdentifier(byte[] uriRegistryToken) throws RegistryException {
        return ntv_retrieveWorkIdentifier(uriRegistryToken,
                                          RRSServices.ATR_CURRENT,
                                          RRSServices.ATR_GENERATE,
                                          RRSServices.ATR_XID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int setWorkIdentifier(byte[] ur_or_uriToken, byte[] xid) {
        return ntv_setWorkIdentifier(ur_or_uriToken,
                                     RRSServices.ATR_CURRENT,
                                     RRSServices.ATR_XID,
                                     xid.length,
                                     xid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ExpressInterestReturnType expressURInterest(byte[] rmRegistryToken,
                                                             byte[] ctxToken,
                                                             int protocol,
                                                             byte[] nonPData,
                                                             byte[] pdata,
                                                             byte[] xid,
                                                             byte[] parentUrToken) throws RegistryException {

        byte[] xidBytes = xid;

        if (xid == null) {
            xidBytes = new byte[0];
        }

        int interestOptions = RRSServices.ATR_PROTECTED_INT_MASK;

        if (protocol == RRSServices.ATR_PRESUMED_ABORT)
            interestOptions |= RRSServices.ATR_PRESUME_ABORT_MASK;
        if (parentUrToken != null) {
            interestOptions |= (RRSServices.ATR_CREATE_CASCADED_UR_MASK | RRSServices.ATR_USE_BQUAL_MASK);
        }

        return ntv_expressUrInterest(rmRegistryToken,
                                     ctxToken,
                                     interestOptions,
                                     nonPData,
                                     pdata,
                                     pdata.length,
                                     xidBytes,
                                     xidBytes.length,
                                     parentUrToken);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final RetrieveSideInformationReturnType retrieveSideInformation(byte[] uriRegistryToken,
                                                                           int[] info_ids) throws RegistryException {
        return ntv_retrieveSideInformation(uriRegistryToken, info_ids, info_ids.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final RetrieveURInterestReturnType retrieveURInterest(byte[] rmRegistryToken) throws RegistryException {
        return ntv_retrieveUrInterest(rmRegistryToken);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int setEnvironment(byte[] stoken,
                                    int[] envIds,
                                    int[] envIdValues,
                                    int[] protectionValues) {
        return ntv_setEnvironment(stoken,
                                  envIds.length,
                                  envIds,
                                  envIdValues,
                                  protectionValues);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final PrepareAgentURReturnType prepareAgentUR(byte[] uriRegistryToken, byte[] ctxRegistryToken, byte[] rmRegistryToken, int logOption) throws RegistryException {
        return ntv_prepareAgentUR(uriRegistryToken, ctxRegistryToken, rmRegistryToken, logOption);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int commitAgentUR(byte[] uriRegistryToken, byte[] ciRegistryToken, int logOption) throws RegistryException {
        return ntv_commitAgentUR(uriRegistryToken, ciRegistryToken, logOption);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int delegateCommitAgentUR(byte[] uriRegistryToken,
                                           int logOption, int commitOptions) throws RegistryException {
        return ntv_delegateCommitAgentUR(uriRegistryToken, logOption, commitOptions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int backoutAgentUR(byte[] uriRegistryToken, byte[] ciRegistryToken, int logOption) throws RegistryException {
        return ntv_backoutAgentUR(uriRegistryToken, ciRegistryToken, logOption);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int forgetAgentURInterest(byte[] uriRegistryToken, int logOption) throws RegistryException {
        return ntv_forgetAgentURInterest(uriRegistryToken, logOption);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int postDeferredURExit(byte[] uriRegistryToken,
                                        int exitNumber,
                                        int completionCode) throws RegistryException {
        return ntv_postDeferredURExit(uriRegistryToken, exitNumber, completionCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int respondToRetrievedInterest(byte[] uriRegistryToken,
                                                int responseCode,
                                                byte[] nonPData) throws RegistryException {
        return ntv_respondToRetrievedInterest(uriRegistryToken, responseCode, nonPData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int setPersistentInterestData(byte[] uriRegistryToken,
                                               byte[] pdata) throws RegistryException {
        return ntv_setPersistentInterestData(uriRegistryToken, pdata.length, pdata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int setSyncpointControls(byte[] uriRegistryToken,
                                          int prepareExitCode,
                                          int commitExitCode,
                                          int backoutExitCode,
                                          int role) throws RegistryException {
        return ntv_setSyncpointControls(uriRegistryToken,
                                        prepareExitCode,
                                        commitExitCode,
                                        backoutExitCode,
                                        role);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int setSideInformation(byte[] uriRegistryToken, int[] infoIds) throws RegistryException {
        return ntv_setSideInformation(uriRegistryToken, infoIds.length, infoIds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int setRMMetadata(byte[] rmRegistryToken, byte[] metadata) {
        return ntv_setRMMetadata(rmRegistryToken, metadata.length, metadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RetrieveRMMetadataReturnType retrieveRMMetadata(byte[] rmRegistryToken) {
        return ntv_retrieveRMMetadata(rmRegistryToken);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final BeginContextReturnType beginContext(byte[] rmRegistryToken) {
        return ntv_beginContext(rmRegistryToken);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final SwitchContextReturnType contextSwitch(byte[] ctxToken) {
        return ntv_contextSwitch(ctxToken);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int endContext(byte[] ctxToken, int completionType) {
        return ntv_endContext(ctxToken, completionType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final RetrieveCurrentContextTokenReturnType retrieveCurrentContextToken() {
        return ntv_retrieveCurrentContextToken();
    }

    // Native method definitions.

    public static native int ntv_endContext(byte[] ctxToken, int completionType);

    public static native BeginTransactionReturnType ntv_beginTransaction(int transactionMode);

    public static native int ntv_endUR(int action, byte[] currentUrToken);

    public static native int ntv_backoutUR();

    public static native RetrieveSideInformationFastReturnType ntv_retireveSideInformationFast(byte[] ctxToken,
                                                                                               int infoOptions);

    public static native RetrieveURDataReturnType ntv_retireveURData(byte[] uriToken, int statesOption);

    public static native RegisterResMgrReturnType ntv_registerResourceManager(int unregOption,
                                                                              byte[] globalData,
                                                                              byte[] rmNamePrefix,
                                                                              int rmNamePrefixLength,
                                                                              byte[] rmNameSTCK);

    public static native int ntv_unregisterResourceManager(byte[] rmName, byte[] rmRegistryToken);

    private static native SetExitInformationReturnType ntv_setExitInformation(byte[] rmName, byte[] rmRegistryToken, boolean recovery);

    public static native int ntv_beginRestart(byte[] rmRegistryToken);

    public static native int ntv_endRestart(byte[] rmRegistryToken);

    public static native RetrieveLogNameReturnType ntv_retrieveLogName(byte[] rmRegistryToken);

    public static native int ntv_setLogName(byte[] rmRegistryToken,
                                            int rmLogNameLength,
                                            byte[] rmLogName);

    public static native RetrieveWorkIdentifierReturnType ntv_retrieveWorkIdentifier(byte[] uriRegistryToken,
                                                                                     int retrieve_option,
                                                                                     int generate_Option,
                                                                                     int uwidType) throws RegistryException;

    public static native int ntv_setWorkIdentifier(byte[] urToken,
                                                   int set_option,
                                                   int workId_type,
                                                   int xidLength,
                                                   byte[] xid);

    public static native ExpressInterestReturnType ntv_expressUrInterest(byte[] rmRegistryToken,
                                                                         byte[] ctxToken,
                                                                         int interestOptions,
                                                                         byte[] nonPData,
                                                                         byte[] pdata,
                                                                         int pdataLength,
                                                                         byte[] xid,
                                                                         int xidLength,
                                                                         byte[] parentUrToken) throws RegistryException;

    public static native RetrieveSideInformationReturnType ntv_retrieveSideInformation(byte[] uriRegistryToken,
                                                                                       int[] info_ids,
                                                                                       int info_id_count) throws RegistryException;

    public static native RetrieveURInterestReturnType ntv_retrieveUrInterest(byte[] rmRegistryToken) throws RegistryException;

    public static native int ntv_setEnvironment(byte[] stoken,
                                                int elementCount,
                                                int[] envIds,
                                                int[] envIdValues,
                                                int[] protectionValues);

    public static native PrepareAgentURReturnType ntv_prepareAgentUR(byte[] uriRegistryToken, byte[] ctxRegistryToken, byte[] rmRegistryToken,
                                                                     int logOption) throws RegistryException;

    public static native int ntv_commitAgentUR(byte[] uriRegistryToken, byte[] ciRegistryToken, int logOption) throws RegistryException;

    public static native int ntv_delegateCommitAgentUR(byte[] uriRegistryToken,
                                                       int logOption,
                                                       int commitOptions) throws RegistryException;

    public static native int ntv_backoutAgentUR(byte[] uriRegistryToken, byte[] ciRegistryToken, int logOption) throws RegistryException;

    public static native int ntv_forgetAgentURInterest(byte[] uriRegistryToken,
                                                       int logOption) throws RegistryException;

    public static native int ntv_postDeferredURExit(byte[] uriRegistryToken,
                                                    int exitNumber,
                                                    int completionCode) throws RegistryException;

    public static native int ntv_respondToRetrievedInterest(byte[] uriRegistryToken,
                                                            int responseCode,
                                                            byte[] nonPData) throws RegistryException;

    public static native int ntv_setPersistentInterestData(byte[] uriRegistryToken,
                                                           int pdataLength,
                                                           byte[] pdata) throws RegistryException;

    public static native int ntv_setSyncpointControls(byte[] uriRegistryToken,
                                                      int prepareExitCode,
                                                      int commitExitCode,
                                                      int backoutExitCode,
                                                      int role) throws RegistryException;

    public static native int ntv_setSideInformation(byte[] uriRegistryToken,
                                                    int elementCount,
                                                    int[] infoIds) throws RegistryException;

    public static native int ntv_setRMMetadata(byte[] rmRegistryToken,
                                               int metadataLength,
                                               byte[] metadata);

    public static native RetrieveRMMetadataReturnType ntv_retrieveRMMetadata(byte[] rmRegistryToken);

    public static native BeginContextReturnType ntv_beginContext(byte[] rmRegistryToken);

    public static native SwitchContextReturnType ntv_contextSwitch(byte[] ctxToken);

    public static native RetrieveCurrentContextTokenReturnType ntv_retrieveCurrentContextToken();
}
