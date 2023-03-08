/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.request.logging.internal;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.zos.request.logging.UserData;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.kernel.productinfo.DuplicateProductInfoException;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.productinfo.ProductInfoParseException;
import com.ibm.ws.kernel.productinfo.ProductInfoReplaceException;
import com.ibm.ws.zos.core.structures.MvsCommonFields;
import com.ibm.ws.zos.core.utils.NativeUtils;
import com.ibm.ws.zos.core.utils.Smf;
import com.ibm.ws.zos.request.logging.SmfDataRecorder;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

/**
 * SMF data recorder.
 */
@Component(service = { SmfDataRecorder.class }, property = { "service.vendor=IBM" })
public class SmfDataRecorderImpl implements SmfDataRecorder {

    private static final TraceComponent tc = Tr.register(SmfDataRecorderImpl.class);

    /** An empty byte array used to pad user data to length */
    private static final byte[] nulls = new byte[UserData.USER_DATA_MAX_SIZE];

    /** WebSphere's com.ibm.websphere.productId property as it appears in the WebSphereApplicationServer.properties file. */
    private static final String PRODUCT_NAME = "com.ibm.websphere.appserver";

    /** Product version max length */
    private static final int PRODUCT_VERSION_BYTES_MAX = 16;

    /** Total size of a user data section. user data max plus 12 for length type and size */
    private static final int USER_DATA_TOTAL_SIZE = UserData.USER_DATA_MAX_SIZE + 12;

    /** SMF record entry lengths. */
    private static final int RECORD_HEADER_SIZE = 48;
    private static final int TRIPLET_SIZE = 12;
    private static final int CURRENT_NUMBER_OF_TRIPLETS = 5; // Note if you change this change the value in record_header
    private static final int RECORD_HEADER_AND_TRIPLETS_SIZE = RECORD_HEADER_SIZE + (CURRENT_NUMBER_OF_TRIPLETS * TRIPLET_SIZE);
    private static final int WLM_TRANSACTION_CLASS_MAX = 8;
    private static final int REQUEST_ID_SIZE = 23;
    private static final int REMOTE_ADDR_LENGTH_MAX = 40;
    private static final int SERVER_CONFIG_DIR_LENGTH_MAX = 128;

    /** Current server data version. */
    private static final int CURRENT_SERVER_DATA_VERSION = 3;

    /** Triplet - server info section count (always 1) */
    private final byte serverInfoCountForTriplet[] = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };

    /** Triplet - server info section count (always 1) */
    private final byte requestInfoCountForTriplet[] = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };

    /** Current request data version. */
    private final byte requestDataVersion[] = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };

    /** Current classification data version. */
    private final byte classificationDataVersion[] = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };

    /** classification data triplet length. */
    private final byte classificationDataTripletLength[] = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x8C };

    /** classification data triplet count for http. */
    private final byte classificationDataCountHttp[] = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03 };

    /** Current network data version. */
    private final byte networkDataVersion[] = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };

    /** Triplet - network info section count (always 1) */
    private final byte networkDataCountForTriplet[] = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };

    /** maximum length of the string in the classification data section. */
    private static final int CLASSIFICATION_DATA_MAX_LENGTH = 128;

    /** maximum length of the request URI string in the request data section. */
    private static final int REQUEST_URI_MAX_LENGTH = 128;

    /** Classification data types. */
    private final byte classificationTypeUri[] = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x06 };
    private final byte classificationTypeHost[] = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x07 };
    private final byte classificationTypePort[] = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08 };

    /** SMF 120-11 record header. This follows the 2 byte length. */
    private final byte record_header[] = new byte[] {
                                                      (byte) 0x00, (byte) 0x00, /* Two bytes, unused by us */
                                                      (byte) 0x5E, /* One byte, subtypes used (bit 01000000) and MVS version (rest) */
                                                      (byte) 0x78, /* One byte, record type 120 */
                                                      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, /* Four bytes, write-time, filled in by SMF */
                                                      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, /* Four bytes, write-date, filled in by SMF */
                                                      (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, /* Four bytes, system-id, just EBCDIC blanks */
                                                      (byte) 0xE6, (byte) 0XC1, (byte) 0xE2, (byte) 0x40, /* Four bytes, subsystem-id, "WAS " in EBCDIC */
                                                      (byte) 0x00, (byte) 0x0B, /* Two byte, subtype, using '11' for this record */
                                                      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, /*
                                                                                                           * Four bytes, version number for record: if changed update
                                                                                                           * CURRENT_RECORD_VERSION
                                                                                                           */
                                                      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x05, /*
                                                                                                           * Four bytes, number of triplets if changed update
                                                                                                           * CURRENT_NUMBER_OF_TRIPLETS
                                                                                                           */
                                                      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, /* Four bytes, record-index. No split-records so zero */
                                                      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, /* Four bytes, total-records, no splits, so one */
                                                      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, /* Eight bytes, continuation token, no splits so zero */
                                                      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 /* rest of it */

    };
    private final byte empty_triplet[] = new byte[] {
                                                      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };
    private final byte sixtyFourEbcdicBlanks[] = new byte[] { (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                                                              (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                                                              (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                                                              (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                                                              (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                                                              (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                                                              (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                                                              (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40
    };

    /** a place to save data about the server so we don't have to generate it every time */
    private byte[] savedServerData = null;

    /** Native Utility object reference. */
    private NativeUtils nativeUtils;

    /** MVS common fields object reference. */
    private MvsCommonFields mvsCommonFields;

    /** Smf object reference. */
    private Smf smf;

    /** The reference to the WSLocationAdmin service. */
    private WsLocationAdmin locationAdmin = null;

    /**
     * DS method to activate this component.
     *
     * @param properties : Map containing service properties
     */
    protected void activate(Map<String, Object> configuration) {
        createServerData();
    }

    /**
     * DS method to deactivate this component.
     * Currently nothing needs to be done here.
     */
    @Deactivate
    protected void deactivate(int reason) {
    }

    /**
     * Sets the NativeUtils object reference.
     *
     * @param nativeUtils The NativeUtils object reference.
     */
    @Reference(service = NativeUtils.class, cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    protected void setNativeUtils(NativeUtils nativeUtils) {
        this.nativeUtils = nativeUtils;
    }

    /**
     * Unsets the NativeUtils object reference.
     *
     * @param nativeUtils The NativeUtils object reference.
     */
    protected void unsetNativeUtils(NativeUtils nativeUtils) {
        if (this.nativeUtils == nativeUtils) {
            this.nativeUtils = null;
        }
    }

    /**
     * Sets MvsCommonFields object reference.
     *
     * @param mvsCommonFields The MvsCommonFields object reference.
     */
    @Reference(service = MvsCommonFields.class, cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    protected void setMvsCommonFields(MvsCommonFields mvsCommonFields) {
        this.mvsCommonFields = mvsCommonFields;
    }

    /**
     * Unsets the MvsCommonFields object reference.
     *
     * @param mvsCommonFields The MvsCommonFields object reference.
     */
    protected void unsetMvsCommonFields(MvsCommonFields mvsCommonFields) {
        if (this.mvsCommonFields == mvsCommonFields) {
            this.mvsCommonFields = null;
        }
    }

    /**
     * Sets Smf object reference.
     *
     * @param smf The Smf object reference.
     */
    @Reference(service = Smf.class, cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    protected void setSmf(Smf smf) {
        this.smf = smf;
    }

    /**
     * Unsets the Smf object object reference.
     *
     * @param smf The Smf object reference.
     */
    protected void unsetSmf(Smf smf) {
        if (this.smf == smf) {
            this.smf = null;
        }
    }

    /**
     * Sets WsLocationAdmin object reference.
     *
     * @param locationAdmin The WsLocationAdmin object reference.
     */
    @Reference(service = WsLocationAdmin.class, cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    protected void setLocationAdmin(WsLocationAdmin locationAdmin) {
        this.locationAdmin = locationAdmin;
    }

    /**
     * Unsets the WsLocationAdmin object reference.
     *
     * @param locationAdmin The WsLocationAdmin object reference.
     */
    protected void unsetLocationAdmin(WsLocationAdmin locationAdmin) {
        if (this.locationAdmin == locationAdmin) {
            this.locationAdmin = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int buildAndWriteRecord(Map<String, Object> requestdata) {

        // create a record...
        ByteArrayOutputStream record = new ByteArrayOutputStream();

        short recordLength = 0;

        // get the server Info..
        byte[] serverInfo = savedServerData;

        @SuppressWarnings("unchecked")
        HashMap<Integer, byte[]> userDataBytes = (HashMap<Integer, byte[]>) requestdata.get(API_USER_DATA);

        byte[] requestInfoBytes = getRequestInfoBytes(requestdata);

        byte[] httpClassificationBytes = getHttpClassificationBytes(requestdata);

        byte[] networkDataBytes = getNetworkDataBytes(requestdata);

        int currentOffset = RECORD_HEADER_AND_TRIPLETS_SIZE + serverInfo.length;

        byte[] userDataOffsetBytes = intToBytes(currentOffset);
        int countForUserDataTriplet = (userDataBytes != null) ? userDataBytes.size() : 0;
        if (countForUserDataTriplet != 0) {
            if (countForUserDataTriplet > UserData.USER_DATA_MAX_COUNT) {
                countForUserDataTriplet = UserData.USER_DATA_MAX_COUNT;
            }
            currentOffset = currentOffset + (countForUserDataTriplet * USER_DATA_TOTAL_SIZE);
        }

        byte[] requestInfoOffsetBytes = intToBytes(currentOffset);
        currentOffset = currentOffset + requestInfoBytes.length;

        byte[] classificationOffsetBytes = intToBytes(currentOffset);
        if (httpClassificationBytes != null) {
            currentOffset = currentOffset + httpClassificationBytes.length;
        }

        byte[] networkDataOffsetBytes = intToBytes(currentOffset);
        currentOffset = currentOffset + networkDataBytes.length;

        // Write length of whole record. It is 2 bytes
        recordLength = (short) currentOffset;
        byte[] recordLengthBytes = shortToBytes(recordLength);
        record.write(recordLengthBytes, 0, recordLengthBytes.length);
        // write in the fixed header
        record.write(record_header, 0, record_header.length);

        // write server info triplet. offset length count
        record.write(intToBytes(RECORD_HEADER_AND_TRIPLETS_SIZE), 0, 4); // triplet offset
        record.write(intToBytes(serverInfo.length), 0, 4); // triplet length
        record.write(serverInfoCountForTriplet, 0, 4); // triplet count

        // write user data triplet. offset length count
        if (countForUserDataTriplet != 0) {
            // offset length count
            record.write(userDataOffsetBytes, 0, 4); // triplet offset
            record.write(intToBytes(USER_DATA_TOTAL_SIZE), 0, 4); // triplet length
            record.write(intToBytes(countForUserDataTriplet), 0, 4); // triplet count
        } else {
            record.write(empty_triplet, 0, empty_triplet.length);
        }

        // write request data triplet. offset length count
        // offset length count
        record.write(requestInfoOffsetBytes, 0, 4); //  triplet offset
        record.write(intToBytes(requestInfoBytes.length), 0, 4); // triplet length
        record.write(requestInfoCountForTriplet, 0, 4); // triplet count

        // write classification data triplet. offset length count
        if (httpClassificationBytes != null) {
            // offset length count
            record.write(classificationOffsetBytes, 0, 4); //  triplet offset
            record.write(classificationDataTripletLength, 0, 4); // triplet length
            record.write(classificationDataCountHttp, 0, 4); // triplet count
        } else {
            record.write(empty_triplet, 0, empty_triplet.length);
        }

        // write network data data triplet. offset length count
        // offset length count
        record.write(networkDataOffsetBytes, 0, 4); //  triplet offset
        record.write(intToBytes(networkDataBytes.length), 0, 4); // triplet length
        record.write(networkDataCountForTriplet, 0, 4); // triplet count

        // Write the server info into the record
        record.write(serverInfo, 0, serverInfo.length);

        // Write the user data into the record
        if (countForUserDataTriplet != 0) {
            Iterator<Integer> it = (userDataBytes.keySet()).iterator();
            int sectionsWritten = 0;
            while (it.hasNext()) {
                byte[] data = userDataBytes.get(it.next());
                if (sectionsWritten >= countForUserDataTriplet) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Sections written " + sectionsWritten + " is more than expected " + countForUserDataTriplet + " map size is " + userDataBytes.size());
                    }
                    break;
                }
                record.write(data, 0, data.length);
                sectionsWritten = sectionsWritten + 1;
            }
        }

        // write request info
        record.write(requestInfoBytes, 0, requestInfoBytes.length);

        // write http classification data
        if (httpClassificationBytes != null) {
            record.write(httpClassificationBytes, 0, httpClassificationBytes.length);
        }

        // write network info data
        record.write(networkDataBytes, 0, networkDataBytes.length);

        // Oh look..we do the actual point of the method in the return..cool.
        return smf.smfRecordT120S11Write(record.toByteArray());
    }

    /**
     * Create the common data for the server.
     */
    public void createServerData() {
        if (savedServerData == null) {
            byte[] serverDataVersion = intToBytes(CURRENT_SERVER_DATA_VERSION);

            ByteArrayOutputStream serverData = new ByteArrayOutputStream();
            serverData.write(serverDataVersion, 0, serverDataVersion.length);
            serverData.write(mvsCommonFields.getCVTSNAME(), 0, 8);
            serverData.write(mvsCommonFields.getECVTSPLX(), 0, 8);
            serverData.write(mvsCommonFields.getJSABJBID(), 0, 8);
            serverData.write(mvsCommonFields.getJSABJBNM(), 0, 8);
            serverData.write(mvsCommonFields.getASSBSTKN(), 0, 8);

            // added for version 2
            int asid = mvsCommonFields.getASCBASID();
            byte[] asidBytes = intToBytes(asid);
            serverData.write(asidBytes, 0, 4);

            // Add server config dir. It has the server name.
            String serverConfigDir = getServerConfigDir();
            byte[] serverConfigDirBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(serverConfigDir, SERVER_CONFIG_DIR_LENGTH_MAX);
            serverData.write(serverConfigDirBytes, 0, SERVER_CONFIG_DIR_LENGTH_MAX);

            String productVersion = getProductVersion();
            byte[] productVersionBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(productVersion, PRODUCT_VERSION_BYTES_MAX);
            serverData.write(productVersionBytes, 0, PRODUCT_VERSION_BYTES_MAX);

            byte[] pidBytes = intToBytes(nativeUtils.getPid());
            serverData.write(pidBytes, 0, 4);

            // start of fields added for server data version 3
            int cvtFlags = mvsCommonFields.getCVTFLAGS();
            // if CvtzCBP is on set flag
            if ((cvtFlags & 0x00000200) == 0x00000200) {
                int serverDataFlags = 0x80000000;
                byte[] serverDataFlagsBytes = intToBytes(serverDataFlags);
                serverData.write(serverDataFlagsBytes, 0, 4);
            } else {
                serverData.write(nulls, 0, 4);
            }

            // IF YOU ADD STUFF HERE, bump the version CURRENT_SERVER_DATA_VERSION!!!
            // version 1 padded to 80 bytes.
            // version 2 used up all the padding and more.
            // version 3 added 4 bytes for flags
            // No reason to have padding because you need to bump the version to let vendors know there is something there.

            savedServerData = serverData.toByteArray();
        }
    }

    private byte[] getNetworkDataBytes(Map<String, Object> requestdata) {

        ByteArrayOutputStream networkData = new ByteArrayOutputStream();
        networkData.write(networkDataVersion, 0, networkDataVersion.length);

        /* received bytes length 8. set it to nulls for now */
        networkData.write(nulls, 0, 8);

        Long responseBytes = (Long) requestdata.get(RESPONSE_BYTES);
        if (responseBytes != null) {
            networkData.write(longToBytes(responseBytes.longValue()), 0, 8);
        } else {
            networkData.write(nulls, 0, 8);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Response Bytes is null.");
            }
        }
        Integer localPort = (Integer) requestdata.get(LOCAL_PORT);
        if (localPort != null) {
            byte[] localPortBytes = intToBytes(localPort.intValue());
            networkData.write(localPortBytes, 0, localPortBytes.length);
        } else {
            networkData.write(nulls, 0, 4);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Network data local port is null.");
            }
        }

        Integer remotePort = (Integer) requestdata.get(REMOTE_PORT);
        if (remotePort != null) {
            byte[] remotePortBytes = intToBytes(remotePort.intValue());
            networkData.write(remotePortBytes, 0, remotePortBytes.length);
        } else {
            networkData.write(nulls, 0, 4);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Network data remote port is null.");
            }
        }

        String remoteAddr = (String) requestdata.get(REMOTE_ADDR);
        if (remoteAddr != null) {
            int remoteAddrLength = remoteAddr.length();
            if (remoteAddrLength > REMOTE_ADDR_LENGTH_MAX) {
                remoteAddrLength = REMOTE_ADDR_LENGTH_MAX;
            }
            byte[] remoteAddrLengthBytes = intToBytes(remoteAddrLength);
            networkData.write(remoteAddrLengthBytes, 0, remoteAddrLengthBytes.length);
            byte[] remoteAddrBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(remoteAddr, REMOTE_ADDR_LENGTH_MAX);
            networkData.write(remoteAddrBytes, 0, REMOTE_ADDR_LENGTH_MAX);
        } else {
            networkData.write(nulls, 0, 4 + REMOTE_ADDR_LENGTH_MAX);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Network data remote addr is null.");
            }
        }

        return networkData.toByteArray();
    }

    private byte[] getRequestInfoBytes(Map<String, Object> requestData) {

        ByteArrayOutputStream requestInfoBytes = new ByteArrayOutputStream();
        requestInfoBytes.write(requestDataVersion, 0, requestDataVersion.length);

        byte[] smfData = (byte[]) requestData.get(THREAD_INFO);
        if (smfData != null) {
            requestInfoBytes.write(smfData, 0, 36); // psatold 4 ttoken 16 thread id 8 and cvtldto 8
        } else {
            requestInfoBytes.write(nulls, 0, 36);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Thread info is null.");
            }
        }

        Long threadId = (Long) requestData.get(THREAD_ID); // from request context Thread.currentThread().getId()
        if (threadId != null) {
            requestInfoBytes.write(longToBytes(threadId.longValue()), 0, 8);
        } else {
            requestInfoBytes.write(nulls, 0, 8);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Thread Id is null.");
            }
        }

        String requestId = (String) requestData.get(REQUEST_ID);
        if (requestId != null) {
            if (requestId.length() == REQUEST_ID_SIZE) {
                byte[] requestIdBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(requestId, REQUEST_ID_SIZE);
                if (requestIdBytes != null) {
                    requestInfoBytes.write(requestIdBytes, 0, REQUEST_ID_SIZE);
                } else {
                    requestInfoBytes.write(nulls, 0, REQUEST_ID_SIZE);
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Conversion of request id to bytes failed. requestId " + requestId);
                    }
                }
            } else {
                requestInfoBytes.write(nulls, 0, REQUEST_ID_SIZE);
                // They changed the size of a request id. FFDC Hopefully a fat will fail.
                FFDCFilter.processException(new java.lang.IndexOutOfBoundsException(), UserDataImpl.class.getName(), "1", this);
            }
        } else {
            requestInfoBytes.write(nulls, 0, REQUEST_ID_SIZE);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Request Id is null.");
            }
        }

        // request id is 23. Add a byte for alignment.
        requestInfoBytes.write(nulls, 0, 1);

        Long wlmStartStck = (Long) requestData.get(WLM_DATA_ARRIVAL_TIME);
        if (wlmStartStck != null) {
            requestInfoBytes.write(longToBytes(wlmStartStck.longValue()), 0, 8);
        } else {
            Long startStck = (Long) requestData.get(START_STCK);
            if (startStck != null) {
                requestInfoBytes.write(longToBytes(startStck.longValue()), 0, 8);
            } else {
                requestInfoBytes.write(nulls, 0, 8);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Both start times are null.");
                }
            }
        }

        Long wlmEndStck = (Long) requestData.get(WLM_DATA_END_TIME);
        if (wlmEndStck != null) {
            requestInfoBytes.write(longToBytes(wlmEndStck.longValue()), 0, 8);
        } else {
            Long endStck = (Long) requestData.get(END_STCK);
            if (endStck != null) {
                requestInfoBytes.write(longToBytes(endStck.longValue()), 0, 8);
            } else {
                requestInfoBytes.write(nulls, 0, 8);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Both end times are null.");
                }
            }
        }

        byte[] transClassBytes = (byte[]) requestData.get(WLM_DATA_TRAN_CLASS);
        if (transClassBytes != null) {
            int transClassByteslength = transClassBytes.length;
            if (transClassByteslength >= WLM_TRANSACTION_CLASS_MAX) {
                requestInfoBytes.write(transClassBytes, 0, WLM_TRANSACTION_CLASS_MAX);
            } else {
                requestInfoBytes.write(transClassBytes, 0, transClassByteslength);
                requestInfoBytes.write(sixtyFourEbcdicBlanks, 0, WLM_TRANSACTION_CLASS_MAX - transClassByteslength);
            }
        } else {
            requestInfoBytes.write(sixtyFourEbcdicBlanks, 0, WLM_TRANSACTION_CLASS_MAX);
        }

        byte[] timeusedStart = (byte[]) requestData.get(TIMEUSED_START);
        if (timeusedStart != null) {
            requestInfoBytes.write(timeusedStart, 0, 16);
        } else {
            requestInfoBytes.write(nulls, 0, 16);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "timeused start was null.");
            }
        }

        byte[] timeusedEnd = (byte[]) requestData.get(TIMEUSED_END);
        if (timeusedEnd != null) {
            requestInfoBytes.write(timeusedEnd, 0, 16);
        } else {
            requestInfoBytes.write(nulls, 0, 16);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "timeused end was null.");
            }
        }

        byte[] deleteDataBytes = (byte[]) requestData.get(WLM_DATA_DELETE_DATA);
        if (deleteDataBytes != null) {
            if (deleteDataBytes.length == WLM_DELETE_DATA_LENGTH) {
                requestInfoBytes.write(deleteDataBytes, 0, WLM_DELETE_DATA_LENGTH);
            } else {
                requestInfoBytes.write(nulls, 0, WLM_DELETE_DATA_LENGTH);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "WLM delete data length is incorrect. Length is " + deleteDataBytes.length);
                }
            }
        } else {
            // Will not be there if the WLM feature is off
            requestInfoBytes.write(nulls, 0, WLM_DELETE_DATA_LENGTH);
        }
        String userName = (String) requestData.get(USER_NAME);
        if (userName != null) {
            byte[] userNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(userName, 64);
            if (userNameBytes != null) {
                requestInfoBytes.write(userNameBytes, 0, 64);
            } else {
                requestInfoBytes.write(sixtyFourEbcdicBlanks, 0, 64);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Conversion of user name to bytes failed. userName " + userName);
                }
            }
        } else {
            requestInfoBytes.write(sixtyFourEbcdicBlanks, 0, 64);
        }

        String mappedUserName = (String) requestData.get(MAPPED_USER_NAME);
        if (mappedUserName != null) {
            byte[] mappedUserNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(mappedUserName, 8);
            if (mappedUserNameBytes != null) {
                requestInfoBytes.write(mappedUserNameBytes, 0, 8);
            } else {
                requestInfoBytes.write(sixtyFourEbcdicBlanks, 0, 8);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Conversion of mapped user name to bytes failed. mappedUserName " + mappedUserName);
                }
            }

        } else {
            requestInfoBytes.write(sixtyFourEbcdicBlanks, 0, 8);
        }

        String requestUri = (String) requestData.get(REQUEST_URI);
        if (requestUri != null) {
            int requestUriLength = requestUri.length();
            if (requestUriLength > REQUEST_URI_MAX_LENGTH) {
                requestUriLength = REQUEST_URI_MAX_LENGTH;
            }
            byte[] requestUriBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(requestUri, requestUriLength);
            if (requestUriBytes != null) {
                byte[] requestUriLengthBytes = intToBytes(requestUriLength);
                requestInfoBytes.write(requestUriLengthBytes, 0, 4); // length
                requestInfoBytes.write(requestUriBytes, 0, requestUriLength); // data
                // pad if necessary
                if (requestUriLength < REQUEST_URI_MAX_LENGTH) {
                    requestInfoBytes.write(nulls, 0, REQUEST_URI_MAX_LENGTH - requestUriLength);
                }
            } else {
                requestInfoBytes.write(nulls, 0, 4 + REQUEST_URI_MAX_LENGTH); // length and data area
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Conversion of request URI to bytes failed. Request URI " + requestUri);
                }
            }
        } else {
            requestInfoBytes.write(nulls, 0, 4 + REQUEST_URI_MAX_LENGTH); // length and data area
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Request URI is null.");
            }
        }

        return requestInfoBytes.toByteArray();
    }

    private byte[] getHttpClassificationBytes(Map<String, Object> requestData) {
        // there are 3 sections for http  URI host port
        // each section has this format
        // version                           length of 4
        // type                              length of 4
        // length of data area that is used  length of 4
        // data                              max length of 128
        // Note if this size changes constant classificationDataTripletLength used for triplet length needs to change

        String wlmUri = (String) requestData.get(WLM_DATA_URI);
        String wlmHost = (String) requestData.get(WLM_DATA_HOST);
        Object wlmPort = requestData.get(WLM_DATA_PORT);

        byte[] ouptputBytes;

        if ((wlmUri == null) && (wlmHost == null) && (wlmPort == null)) {
            ouptputBytes = null;
        } else {
            ByteArrayOutputStream classificationBytes = new ByteArrayOutputStream();
            classificationBytes.write(classificationDataVersion, 0, classificationDataVersion.length); // version
            classificationBytes.write(classificationTypeUri, 0, classificationTypeUri.length); // type

            // length
            if (wlmUri != null) {
                int wlmUriLength = wlmUri.length();
                if (wlmUriLength > CLASSIFICATION_DATA_MAX_LENGTH) {
                    wlmUriLength = CLASSIFICATION_DATA_MAX_LENGTH;
                }
                byte[] wlmUriBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(wlmUri, wlmUriLength);
                if (wlmUriBytes != null) {
                    byte[] wlmUriLengthBytes = intToBytes(wlmUriLength);
                    classificationBytes.write(wlmUriLengthBytes, 0, 4); // length
                    classificationBytes.write(wlmUriBytes, 0, wlmUriLength); // data
                    // pad if necessary
                    if (wlmUriLength < CLASSIFICATION_DATA_MAX_LENGTH) {
                        classificationBytes.write(nulls, 0, CLASSIFICATION_DATA_MAX_LENGTH - wlmUriLength);
                    }
                } else {
                    classificationBytes.write(nulls, 0, 4 + CLASSIFICATION_DATA_MAX_LENGTH); // length and data area
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Conversion of URI to bytes failed. URI " + wlmUri);
                    }
                }
            } else {
                classificationBytes.write(nulls, 0, 4 + CLASSIFICATION_DATA_MAX_LENGTH); // length and data area
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "URI is null.");
                }
            }

            classificationBytes.write(classificationDataVersion, 0, classificationDataVersion.length); // version
            classificationBytes.write(classificationTypeHost, 0, classificationTypeHost.length); // type
            // length
            if (wlmHost != null) {
                int wlmHostLength = wlmHost.length();
                if (wlmHostLength > CLASSIFICATION_DATA_MAX_LENGTH) {
                    wlmHostLength = CLASSIFICATION_DATA_MAX_LENGTH;
                }
                byte[] wlmHostBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(wlmHost, wlmHostLength);
                if (wlmHostBytes != null) {
                    byte[] wlmHostLengthBytes = intToBytes(wlmHostLength);
                    classificationBytes.write(wlmHostLengthBytes, 0, 4); // length
                    classificationBytes.write(wlmHostBytes, 0, wlmHostLength); // host data
                    // pad if necessary
                    if (wlmHostLength < CLASSIFICATION_DATA_MAX_LENGTH) {
                        classificationBytes.write(nulls, 0, CLASSIFICATION_DATA_MAX_LENGTH - wlmHostLength);
                    }

                } else {
                    classificationBytes.write(nulls, 0, 4 + CLASSIFICATION_DATA_MAX_LENGTH); // length and data area
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Conversion of host to bytes failed. host " + wlmHost);
                    }
                }
            } else {
                classificationBytes.write(nulls, 0, 4 + CLASSIFICATION_DATA_MAX_LENGTH); // length and data area
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Host is null.");
                }
            }

            classificationBytes.write(classificationDataVersion, 0, classificationDataVersion.length); // version
            classificationBytes.write(classificationTypePort, 0, classificationTypePort.length); // type
            // length
            if (wlmPort != null) {
                String wlmPortString = wlmPort.toString();
                int wlmPortLength = wlmPortString.length();
                if (wlmPortLength > CLASSIFICATION_DATA_MAX_LENGTH) {
                    wlmPortLength = CLASSIFICATION_DATA_MAX_LENGTH;
                }
                byte[] wlmPortBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(wlmPortString, wlmPortLength);
                if (wlmPortBytes != null) {
                    byte[] wlmPortLengthBytes = intToBytes(wlmPortLength);
                    classificationBytes.write(wlmPortLengthBytes, 0, 4); // length
                    classificationBytes.write(wlmPortBytes, 0, wlmPortLength); // port data
                    // pad if necessary
                    if (wlmPortLength < CLASSIFICATION_DATA_MAX_LENGTH) {
                        classificationBytes.write(nulls, 0, CLASSIFICATION_DATA_MAX_LENGTH - wlmPortLength);
                    }
                } else {
                    classificationBytes.write(nulls, 0, 4 + CLASSIFICATION_DATA_MAX_LENGTH); // length and data area
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Conversion of port to bytes failed. port " + wlmPort);
                    }
                }
            } else {
                classificationBytes.write(nulls, 0, 4 + CLASSIFICATION_DATA_MAX_LENGTH); // length and data area
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Port is null.");
                }
            }
            ouptputBytes = classificationBytes.toByteArray();
        }
        return ouptputBytes;
    }

    /**
     * Little routine to make an short into a byte array
     *
     * @param s a short
     * @return the short as a byte array
     */
    @Trivial
    private static byte[] shortToBytes(short s) {
        return new byte[] { (byte) (s >> 8), (byte) (s) };
    }

    /**
     * Little routine to make an int into a byte array
     *
     * @param Int an int
     * @return the int as a byte array
     */
    @Trivial
    private static byte[] intToBytes(int Int) {
        return new byte[] { (byte) (Int >> 24), (byte) (Int >> 16), (byte) (Int >> 8), (byte) (Int) };
    }

    /**
     * Little routine to make an long into a byte array
     *
     * @param L an long
     * @return the long as a byte array
     */
    @Trivial
    private static byte[] longToBytes(long l) {
        return new byte[] { (byte) (l >> 56),
                            (byte) (l >> 48),
                            (byte) (l >> 40),
                            (byte) (l >> 32),
                            (byte) (l >> 24),
                            (byte) (l >> 16),
                            (byte) (l >> 8),
                            (byte) (l) };
    }

    private static String getProductVersion() {
        String version = "";

        try {
            Map<String, ProductInfo> productProperties = ProductInfo.getAllProductInfo();
            ProductInfo wasProperties = productProperties.get(PRODUCT_NAME);
            if (wasProperties != null) {
                version = wasProperties.getVersion();

            }
        } catch (ProductInfoParseException e1) {
            // we tried. just go with empty string
        } catch (DuplicateProductInfoException e1) {
            //we tried. just go with empty string
        } catch (ProductInfoReplaceException e1) {
            // we tried. just go with empty string
        }
        return version;
    }

    private String getServerConfigDir() {
        String fullServerConfigDir = locationAdmin.resolveString("${" + WsLocationConstants.LOC_SERVER_CONFIG_DIR + "}");
        String serverConfigDir;
        if (fullServerConfigDir.length() > SERVER_CONFIG_DIR_LENGTH_MAX) {
            serverConfigDir = fullServerConfigDir.substring(fullServerConfigDir.length() - SERVER_CONFIG_DIR_LENGTH_MAX);
        } else {
            serverConfigDir = fullServerConfigDir;
        }
        return serverConfigDir;
    }
}
