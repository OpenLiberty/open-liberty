/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.zos.channel.wola.internal.natv;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import org.osgi.framework.BundleContext;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.zos.channel.local.LocalCommClientConnHandle;
import com.ibm.ws.zos.channel.wola.WolaInterruptObjectBridge;
import com.ibm.ws.zos.channel.wola.internal.otma.OTMAException;
import com.ibm.ws.zos.core.utils.NativeUtils;
import com.ibm.ws.zos.jni.NativeMethodManager;

/**
 * WOLA channel native utilities.
 */
public class WOLANativeUtils {

    /**
     * Reference to the native method manager that allows for JNI interaction with native code.
     */
    private NativeMethodManager nativeMethodManager;

    /**
     * Interface with the WOLA ODI.
     */
    private WolaInterruptObjectBridge odiBridge;

    /**
     * Do other native things.
     */
    private NativeUtils zosNativeUtils;

    /**
     * Needed to lookup WAS_LIB_PATH.
     */
    BundleContext bundleContext;

    /**
     * Location of BBOACALL. This module gets loaded under ntv_advertiseWolaServer.
     */
    final static String BBOACALL_FUNCTION_MODULE = "native/zos/s390x/bboacall";

    /**
     * Return Code area buffer offsets to each return code.
     */
    final static int PC_ADVERTISE_WOLA_RC = 0;
    final static int PC_GET_IPT_TOKEN_RC = 4;
    final static int REGISTRY_RC = 8;
    final static int LOAD_BBOACALL_RC = 12;

    private static final TraceComponent tc = Tr.register(WOLANativeUtils.class);

    /**
     * DS method to activate this component.
     */
    protected void activate(BundleContext bundleContext) {
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

        nativeMethodManager.registerNatives(WOLANativeUtils.class, o);
        this.bundleContext = bundleContext;
    }

    /**
     * DS setter.
     */
    protected void setNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = nativeMethodManager;
    }

    /**
     * DS un-setter.
     */
    protected void unsetNativeMethodManager(NativeMethodManager nativeMethodManager) {
        if (this.nativeMethodManager == nativeMethodManager) {
            this.nativeMethodManager = null;
        }
    }

    /**
     * DS setter.
     */
    protected void setInterruptObjectBridge(WolaInterruptObjectBridge odiBridge) {
        this.odiBridge = odiBridge;
    }

    /**
     * DS un-setter.
     */
    protected void unsetInterruptObjectBridge(WolaInterruptObjectBridge odiBridge) {
        if (this.odiBridge == odiBridge) {
            this.odiBridge = null;
        }
    }

    /**
     * DS setter.
     */
    protected void setZosNativeUtils(NativeUtils zosNativeUtils) {
        this.zosNativeUtils = zosNativeUtils;
    }

    /**
     * DS un-setter.
     */
    protected void unsetZosNativeUtils(NativeUtils zosNativeUtils) {
        this.zosNativeUtils = null;
    }

    /**
     * DS method to deactivate this component.
     *
     */
    void deactivate(BundleContext bundleContext) {
        this.bundleContext = null;
    }

    /**
     * Create (if necessary) and attach to the shared memory area for the given wolaGroup.
     *
     * @param wolaGroup - The name of the WOLA group for this server
     */
    public RegistryToken attachToWolaGroupSharedMemoryArea(String wolaGroup) {

        RegistryToken registryToken = new RegistryToken();
        int[] returnCodeArea = new int[5];

        ntv_attachToBboashr(CodepageUtils.getEbcdicBytesPadded(wolaGroup, 8),
                            registryToken.getBytes(),
                            returnCodeArea);

        // The first four slots of the return code area contain return codes for the various
        // things this function does.  If any of them are non-zero, we have a problem.
        if ((returnCodeArea[0] | returnCodeArea[1] | returnCodeArea[2] | returnCodeArea[3]) != 0) {
            throw new RuntimeException("Unexpected error occurred under ntv_attachToBboashr." +
                                       " Return codes: " + Integer.toHexString(returnCodeArea[0]) + "." +
                                       Integer.toHexString(returnCodeArea[1]) + "." +
                                       Integer.toHexString(returnCodeArea[2]) + "." +
                                       Integer.toHexString(returnCodeArea[3]) + ".");
        }

        // "Let the user know" if they have a lot of registrations.
        int rgeCount = returnCodeArea[4];
        Object[] traceObjs = new Object[] { wolaGroup, new Integer(rgeCount) };
        if (rgeCount > 500) {
            Tr.info(tc, "WOLA_GROUP_REGISTRATION_COUNT", traceObjs);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled())) {
                Tr.debug(tc, "WOLA registration count for WOLA group", traceObjs);
            }
        }

        return registryToken;
    }

    /**
     * Detach from the wola group shared memory area.
     *
     * This will detach the server's local interest in the shared memory area.
     * The shared memory area is not deleted until the system's interest in the
     * area is detached. This is accomplished by passing systemAffinity=true on
     * the call to detach. However, this is all moot because the WOLA group's
     * shared memory area should never ever be deleted (only an IPL will reclaim
     * the storage).
     *
     * @param wolaGroup - The name of the WOLA group for this server
     */
    public void detachFromWolaGroupSharedMemoryArea(RegistryToken registryToken) {

        int rc = ntv_detachFromBboashr(registryToken.getBytes());

        if (rc != 0) {
            throw new RuntimeException("Unexpected error occurred under ntv_detachFromBboashr: utilregistry rc=" + rc);
        }
    }

    /**
     *
     * Create (if necessary) and activate the WOLA registration entry (BBOARGE)
     * for this server.
     *
     * "Activation" consists of:
     * 1. setting the "active" flag
     * 2. updating the ASID and SToken
     *
     * @param wolaGroup           - The WOLA group
     * @param wolaName2           - the second part of the WOLA name (aka node name)
     * @param wolaName3           - the third part of the WOLA name (aka server short name)
     * @param allowCicsTaskUserId - set to true if this registration will allow an alternate ACEE (not tcbsenv) to be propagated from CICS.
     *
     * @return A RegistryToken that represents the WOLA registration. This token must be
     *         passed back on subsequent calls to deactivateWolaRegistration.
     *
     */
    public RegistryToken activateWolaRegistration(String wolaGroup, String wolaName2, String wolaName3, boolean allowCicsTaskUserId) {

        RegistryToken registryToken = new RegistryToken();
        ByteBufferBacked returnCodeArea = new ByteBufferBacked(16);

        int rc = ntv_activateWolaRegistration(CodepageUtils.getEbcdicBytesPadded(wolaGroup, 8),
                                              CodepageUtils.getEbcdicBytesPadded(wolaName2, 8),
                                              CodepageUtils.getEbcdicBytesPadded(wolaName3, 8),
                                              allowCicsTaskUserId,
                                              registryToken.getBytes(),
                                              returnCodeArea.getBytes());

        if (rc != 0) {
            throw new RuntimeException("Unexpected error occurred under ntv_activateWolaRegistration." +
                                       " Return codes: " + returnCodeArea.toString());
        }

        return registryToken;
    }

    /**
     * Deactivate the WOLA registration for this server. This involves resetting the
     * "active" flag in the server's BBOARGE.
     *
     * @param registryToken - previously returned from the call to activateWolaRegistration.
     *
     */
    public void deactivateWolaRegistration(RegistryToken registryToken) {

        int rc = ntv_deactivateWolaRegistration(registryToken.getBytes());

        if (rc != 0) {
            throw new RuntimeException("Unexpected error occurred under ntv_deactivateWolaRegistration: utilregistry rc=" + rc);
        }

    }

    /**
     * Advertise the WOLA server's presence to prospective WOLA clients.
     * Called when the WOLA Channel is starting up.
     * Also loads the client stubs and hangs them off of the master BGVT
     *
     * @param wolaGroup
     * @param wolaName2
     * @param wolaName3
     *
     * @return a RegistryToken that must be passed back on the call to deadvertiseWolaServer.
     */
    public RegistryToken advertiseWolaServer(String wolaGroup, String wolaName2, String wolaName3) {

        RegistryToken registryToken = new RegistryToken();
        /*
         * Return code area holds 4 return codes (4 bytes each) in the folloaing order
         * Offset 0 - Results from the PC call to advertise the wola service
         * Offset 4 - Return code from getting the IPT Token
         * Offset 8 - return code from setting up the registry token
         * Offset 12- Return code from loading BBOACALL
         */
        ByteBufferBacked returnCodeArea = new ByteBufferBacked(16);

        int rc = ntv_advertiseWolaServer(CodepageUtils.getEbcdicBytesPadded(wolaGroup, 8),
                                         CodepageUtils.getEbcdicBytesPadded(wolaName2, 8),
                                         CodepageUtils.getEbcdicBytesPadded(wolaName3, 8),
                                         CodepageUtils.getEbcdicBytes(getBboacall().getAbsolutePath()),
                                         registryToken.getBytes(),
                                         returnCodeArea.getBytes());

        if (rc != 0) {
            if (returnCodeArea.getInt(PC_ADVERTISE_WOLA_RC) != 0) {
                throw new RuntimeException("ntv_advertiseWolaServer Unable to acquire resource"
                                           + wolaGroup + wolaName2 + wolaName3 + " " +
                                           " Return codes: " + returnCodeArea.toString());
            } else {
                throw new RuntimeException("Unexpected error occurred under ntv_advertiseWolaServer." +
                                           " Return codes: " + returnCodeArea.toString());
            }
        }

        return registryToken;
    }

    /**
     * @return A ref to wlp/lib/native/zos/s390x/bboacall
     */
    private File getBboacall() {
        String WAS_LIB_DIR = bundleContext.getProperty(BootstrapConstants.LOC_INTERNAL_LIB_DIR);
        File library = new File(WAS_LIB_DIR, BBOACALL_FUNCTION_MODULE);
        if (!fileExists(library)) {
            throw new RuntimeException("Unexpected error: bboacall module does not exist at " + library.getPath());
        }

        return library;
    }

    /**
     * @return true if the given file exists.
     */
    private boolean fileExists(final File file) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return file.exists();
            }
        });
    }

    /**
     * Deadvertise the WOLA server's presence.
     * Called during WOLAChannel.stop
     *
     * @param RegistryToken - previously obtained via advertiseWolaServer.
     *
     */
    public void deadvertiseWolaServer(RegistryToken registryToken) {

        ByteBufferBacked returnCodeArea = new ByteBufferBacked(12);

        int rc = ntv_deadvertiseWolaServer(registryToken.getBytes(), returnCodeArea.getBytes());

        if (rc != 0) {
            throw new RuntimeException("Unexpected error occurred under ntv_deadvertiseWolaServer." +
                                       " Return codes: " + returnCodeArea.toString());
        }
    }

    /**
     * Create (if necessary) and attach to the WOLA group's shared memory area
     * (anchored by BBOASHR).
     *
     * @param wolaGroup      - byte[8] containing the wola group name, in EBCDIC, blank-padded
     * @param registryToken  - Output - A byte[64] to contain the registry token, which is used later on detach
     * @param returnCodeArea - Output - A int[5] to contain RCs from the various native services that are
     *                           called under this routine.
     */
    protected native void ntv_attachToBboashr(byte[] wolaGroup, byte[] registryToken, int[] returnCodeArea);

    /**
     * Detach from the WOLA group's shared memory area (anchored by BBOASHR).
     *
     * @param registryToken - byte[64] containing the registry token obtained by a previous call to attach.
     *
     * @return 0 if all is well; non-zero if there was a problem obtaining the bboashr_p from the
     *         registry token.
     */
    protected native int ntv_detachFromBboashr(byte[] registryToken);

    /**
     * Advertise the WOLA server's presence by obtaining an ENQ using the server's
     * WOLA group and WOLA identity.
     *
     * The ENQ is tucked inside a RegistryToken, which is returned to the caller
     * via the output parm. This RegistryToken must be passed back on the
     * deadvertiseWolaServer call.
     *
     * @param wolaGroup      - byte[8] containing the wola group name in EBCDIC, blank-padded
     * @param wolaName2      - byte[8] containing the wola 2nd-part name in EBCDIC, blank-padded
     * @param wolaName3      - byte[8] containing the wola 3rd-part name in EBCDIC, blank-padded
     * @param registryToken  - output - byte[64] to contain the RegistryToken that refers to the ENQ
     * @param returnCodeArea - output - byte[12] to contain RCs in the event of failure
     *
     * @return 0 if all's well; non-zero otherwise (check returnCodeArea for RCs).
     */
    protected native int ntv_advertiseWolaServer(byte[] wolaGroup,
                                                 byte[] wolaName2,
                                                 byte[] wolaName3,
                                                 byte[] loadModuleName,
                                                 byte[] registryToken,
                                                 byte[] returnCodeArea);

    /**
     * DE-Advertise the WOLA server's presence by release the ENQ that was
     * previously obtained on the call to ntv_advertiseWolaServer.
     *
     * The ENQ is tucked inside the given RegistryToken, which was returned
     * by the call to ntv_advertiseWolaServer.
     *
     * @param registryToken  - the RegistryToken that refers to the ENQ, previously obtained
     *                           via ntv_advertiseWolaServer
     * @param returnCodeArea - output - byte[12] to contain RCs in the event of failure
     *
     * @return 0 if all's well; non-zero otherwise (check returnCodeArea for RCs).
     */
    protected native int ntv_deadvertiseWolaServer(byte[] registryToken, byte[] returnCodeArea);

    /**
     * Activate the WOLA server's registration (BBOARGE).
     *
     * If the registration element doesn't exist, it is created.
     *
     * The BBOARGE pointer is tucked inside a RegistryToken, which is returned to the caller
     * via the output parm. This RegistryToken must be passed back on ntv_deactivateWolaRegistration.
     *
     * @param wolaGroup           - byte[8] containing the wola group name in EBCDIC, blank padded
     * @param wolaName2           - byte[8] containing the wola 2nd name in EBCDIC, blank padded
     * @param wolaName3           - byte[8] containing the wola 3rd name in EBCDIC, blank padded
     * @param allowCicsTaskUserId - set to true if this registration will allow an alternate ACEE (not tcbsenv) to be propagated from CICS.
     * @param registryToken       - output - byte[64] to contain the RegistryToken that refers to the BBOARGE.
     * @param returnCodeArea      - output - byte[16] to contain RCs in the event of failure
     *
     * @return 0 if all's well; non-zero otherwise (check returnCodeArea for RCs).
     */
    protected native int ntv_activateWolaRegistration(byte[] wolaGroup,
                                                      byte[] wolaName2,
                                                      byte[] wolaName3,
                                                      boolean allowCicsTaskUserId,
                                                      byte[] registryToken,
                                                      byte[] returnCodeArea);

    /**
     * DE-activate the WOLA server's registration (BBOARGE).
     *
     * The BBOARGE pointer is tucked inside the given RegistryToken, which was returned
     * by the call to ntv_activateWolaRegistration.
     *
     * @param registryToken - the RegistryToken that refers to the BBOARGE, previously obtained
     *                          via ntv_activateWolaRegistration
     *
     * @return 0 if all's well; non-zero if there was a problem obtaining the bboarge_p from the
     *         registry token.
     */
    protected native int ntv_deactivateWolaRegistration(byte[] registryToken);

    /**
     * Retrieve the localcomm connection handle for the client who has registered
     * with the given WOLA registration name and who is hosting the given serviceName.
     *
     * @param wolaGroup    - the wola group
     * @param registerName - The registration name of the WOLA client
     * @param serviceName  - The service hosted by the WOLA client
     * @param timeout_s    - the time to wait (in seconds) for the client service to become available before giving up
     *
     * @return The LocalCommClientConnHandle for the WOLA client hosting the given service.
     */
    public LocalCommClientConnHandle getClientService(String wolaGroup,
                                                      String registerName,
                                                      String serviceName,
                                                      int timeout_s) throws IOException {
        GetClientServiceReturnCodeArea returnCodeArea = new GetClientServiceReturnCodeArea();
        ByteBufferBacked clientConnHandle = new ByteBufferBacked(LocalCommClientConnHandle.Size);

        // This token is used to make sure that the ODI, the STIMERM exit, and this thread
        // can all reference the waiter that may be created on the WOLA service waiter queue.
        long waiterToken = zosNativeUtils.getSTCK();
        byte[] wolaGroupBytes = CodepageUtils.getEbcdicBytesPadded(wolaGroup, 8);
        byte[] registerNameBytes = CodepageUtils.getEbcdicBytesPadded(registerName, 16);
        Object odiToken = null;
        if (odiBridge != null) {
            odiToken = odiBridge.register(wolaGroupBytes, registerNameBytes, waiterToken);
        }

        int rc = ntv_getClientService(wolaGroupBytes,
                                      registerNameBytes,
                                      CodepageUtils.getEbcdicBytes(serviceName),
                                      timeout_s,
                                      clientConnHandle.getBytes(),
                                      waiterToken,
                                      returnCodeArea.getBytes());

        if (odiToken != null) {
            odiBridge.deregister(odiToken);
        }

        if (rc == 0) {
            return new LocalCommClientConnHandle(clientConnHandle.getBytes());
        } else if (returnCodeArea.isTimeout()) {
            throw new IOException(new TimeoutException(new GetClientServiceException("getClientService timed out waiting for the client service to become available", wolaGroup, registerName, serviceName, timeout_s, returnCodeArea).getMessage()));
        } else {
            throw new RuntimeException(new GetClientServiceException("Unexpected error occurred under ntv_getClientService", wolaGroup, registerName, serviceName, timeout_s, returnCodeArea));
        }
    }

    /**
     * Retrieve the localcomm connection handle for the client who has registered
     * with the given WOLA registration name and who is hosting the given serviceName.
     *
     * Note: this method will BLOCK until the specified serviceName becomes available.
     *
     * @param wolaGroup        - byte[8] containing the wola group name in EBCDIC, blank padded
     * @param registerName     - The registration name of the WOLA client (byte[16], in EBCDIC bytes, blank-padded)
     * @param serviceName      - The service hosted by the WOLA client (in EBCDIC bytes, max len=256)
     * @param timeout_s        - the time to wait (in seconds) for the client service to become available before giving up
     * @param clientConnHandle - Output - byte[16] is populated with the client's localcomm conn handle
     * @param waiterToken      - A token to use if we create a client waiter.
     * @param returnCodeArea   - Output - byte[12] return area for RC and RSN codes
     *
     * @return 0 if all is well; non-zero otherwise (check returnCodeArea)
     */
    protected native int ntv_getClientService(byte[] wolaGroup,
                                              byte[] registerName,
                                              byte[] serviceName,
                                              int timeout_s,
                                              byte[] clientConnHandle,
                                              long waiterToken,
                                              byte[] returnCodeArea);

    /**
     * Open a connection to IMS with appropriate parameters and pass
     * back the returned anchor.
     *
     * @param GroupName   - The XCF group name
     * @param MemberName  - The XCF server member name
     * @param PartnerName - The XCF client member name
     *
     * @return The anchor for the OTMA connection
     * @throws OTMAException
     */
    public byte[] openOTMAConenction(String groupName, String memberName, String partnerName) throws OTMAException {

        byte[] anchor = new byte[8];
        int[] returnCodeArea = new int[5];

        int rc = ntv_OpenOTMAConnection(CodepageUtils.getEbcdicBytesPadded(groupName, 8),
                                        CodepageUtils.getEbcdicBytesPadded(memberName, 16),
                                        CodepageUtils.getEbcdicBytesPadded(partnerName, 16),
                                        anchor,
                                        returnCodeArea);

        if (rc != 0) {
            throw new OTMAException(returnCodeArea);
        }
        return anchor;
    }

    /**
     * Open a connetion to IMS and pass back the returned anchor
     *
     *
     * @param GroupName      - byte[8] containing the XCF group name in EBCDIC, blank padded
     * @param MemberName     - byte[16] containing the XCF server member name in EBCDIC, blank-padded)
     * @param PartnerName    - byte[16] containing the XCF client member name in EBCDIC, blank-padded)
     * @param anchor         - Output - byte[8] is populated with the returned connection anchor
     * @param returnCodeArea - Output - int[5] return area for RC and RSN codes
     *
     * @return 0 if all is well; non-zero otherwise (check returnCodeArea)
     */
    protected native int ntv_OpenOTMAConnection(byte[] GroupName,
                                                byte[] MemberName,
                                                byte[] PartnerName,
                                                byte[] anchor,
                                                int[] returnCodeArea);

    /**
     * Send and receive data synchronously to and from IMS
     *
     * @param anchor           - byte[8] used for this connection
     * @param sendSegmentList  - The integer array of segments to send to IMS
     * @param sendSegmentData  - The data to be sent to IMS
     * @param syncLevel        - The syncLevel of the send request
     * @param rcvSegmentCount  - The number of segments allowed for the response data
     * @param rcvSegmentLength - The max length of the response data.
     *
     * @return The response data and the response segment list
     *
     * @throws OTMAException                when a non-zero return code is returned from the JNI call
     * @throws UnsupportedEncodingException
     */
    public OTMASendRcvResponseData otmaSendRcv(byte[] anchor, int[] sendSegmentList, byte[] sendSegmentData,
                                               int syncLevel, int rcvSegmentCount, int rcvSegmentLength) throws OTMAException, UnsupportedEncodingException {

        byte[] rcvSegmentData = new byte[rcvSegmentLength];
        int[] rcvSegmentList = new int[rcvSegmentCount + 1];
        int[] returnCodeArea = new int[5];
        byte[] IMSErrorMessage = new byte[120]; //120 byte array for any error message from IMS

        int rc = ntv_otmaSendReceive(anchor,
                                     sendSegmentList,
                                     sendSegmentData,
                                     sendSegmentData.length,
                                     syncLevel,
                                     rcvSegmentCount,
                                     rcvSegmentList,
                                     rcvSegmentData,
                                     rcvSegmentLength,
                                     returnCodeArea,
                                     IMSErrorMessage,
                                     odiBridge);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "otmaSendRcv", "OTMA returnCode area " + returnCodeArea[0] + " "
                                        + returnCodeArea[1] + " "
                                        + returnCodeArea[2] + " "
                                        + returnCodeArea[3] + " "
                                        + returnCodeArea[4] + " ");
        }
        if (rc != 0) {
            if (returnCodeArea[0] == 20) {
                throw new OTMAException(returnCodeArea, new String(IMSErrorMessage, CodepageUtils.EBCDIC));

            } else {
                throw new OTMAException(returnCodeArea);
            }
        } else if ((returnCodeArea[0] == 0) && (returnCodeArea[1] == 44)) {

            TraceNLS nls = TraceNLS.getTraceNLS(this.getClass(), "com.ibm.ws.zos.channel.wola.internal.resources.ZWOLAChannelMessages");
            Object[] nlsParm = new Object[] { Integer.toString(returnCodeArea[3]), Integer.toString(returnCodeArea[2]) };
            throw new OTMAException(returnCodeArea, nls.getFormattedMessage("OTMA_MAX_RESP_SEGMENTS_EXCEEDED",
                                                                            nlsParm,
                                                                            "CWWKB0511E: OTMA response segment count of " + returnCodeArea[3]
                                                                                     + " exceeds the maximum allowed number of segments " + returnCodeArea[2] + "."));
        }

        return new OTMASendRcvResponseData(rcvSegmentList, rcvSegmentData);

    }

    /**
     * Wrapper class for segment list/segment data return value.
     */
    public class OTMASendRcvResponseData {
        public final int[] segmentList;
        public final byte[] segmentData;

        public OTMASendRcvResponseData(int[] list, byte[] data) {
            this.segmentList = Arrays.copyOf(list, list.length);
            this.segmentData = Arrays.copyOf(data, data.length);
        }
    }

    /**
     * Send and receive a message to/from IMS and pass back the reply buffer
     *
     *
     * @param anchor                - byte[8] containing the XCF group name in EBCDIC, blank padded
     * @param sendSegmentList       - The integer array of segments to send to IMS
     * @param sendSegmentData       - The byte array of data to be sent to IMS
     * @param sendSegmentDataLength - The integer length of the send data
     * @param syncLevel             - The integer sync level of the send request
     * @param rcvSegmentCount       - The number of segments allowed for the response data
     * @param rcvSegmentList        - The integer array of segments for the IMS response data
     * @param rcvSegmentData        - The byte array to hold the IMS response data
     * @param rcvSegmentLength      - The max length of the IMS response data
     * @param returnCodeArea        - int[5] to hold return and reason codes
     * @param IMSErrorMessage       - byte[120] to hold any IMS error messages that may be written
     * @param odiBridge             - The interface to the ODI, if requestTiming-1.0 is configured.
     *
     * @return 0 if all is well; non-zero otherwise (check returnCodeArea)
     */
    protected native int ntv_otmaSendReceive(byte[] anchor,
                                             int[] sendSegmentList,
                                             byte[] sendSegmentData,
                                             int sendSegmentDataLength,
                                             int syncLevel,
                                             int rcvSegmentCount,
                                             int[] rcvSegmentList,
                                             byte[] rcvSegmentData,
                                             int rcvSegmentLength,
                                             int[] returnCodeArea,
                                             byte[] IMSErrorMessage,
                                             WolaInterruptObjectBridge odiBridge);

    /**
     * Close an existing IMS OTMA connection.
     *
     * @param anchor - 8 byte connection anchor to close
     */
    public int closeOtmaConnection(byte[] anchor) {
        int[] retRsnCodes = new int[5];

        int rc = ntv_closeOtmaConnection(anchor, retRsnCodes);

        //TODO check return codes?

        return rc;
    }

    /**
     * Close an IMS OTMA connection by calling otma_close from 31-bit mode.
     *
     * @param anchor            - 8 byte connection anchor to close
     * @param returnReasonCodes - output area for return and reason codes
     *
     * @return 0 if successful
     */
    protected native int ntv_closeOtmaConnection(byte[] anchor,
                                                 int[] returnReasonCodes);

}

/**
 * For exceptions thrown from WolaNativeUtils.getClientService.
 * Contains relevant diagnostic info (parms/return codes).
 */
class GetClientServiceException extends Exception {

    public GetClientServiceException(String msg,
                                     String wolaGroup,
                                     String registerName,
                                     String serviceName,
                                     int timeout_s,
                                     GetClientServiceReturnCodeArea returnCodeArea) {
        super(buildMessage(msg, wolaGroup, registerName, serviceName, timeout_s, returnCodeArea));
    }

    protected static String buildMessage(String msg,
                                         String wolaGroup,
                                         String registerName,
                                         String serviceName,
                                         int timeout_s,
                                         GetClientServiceReturnCodeArea returnCodeArea) {
        String parms = "wolaGroup=" + wolaGroup + ", registerName=" + registerName + ", serviceName=" + serviceName + ", timeout=" + timeout_s + "s";
        return msg + "; Return codes: " + returnCodeArea + "; Parms: " + parms;
    }
}
