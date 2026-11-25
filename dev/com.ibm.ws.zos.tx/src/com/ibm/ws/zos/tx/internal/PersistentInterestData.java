/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.tx.internal;

import com.ibm.ws.Transaction.JTA.Util;
import com.ibm.ws.zos.tx.internal.rrs.RRSServices;

/**
 * Persistent interest data.
 */
public class PersistentInterestData {

    /**
     * The resource interest type.
     */
    public static final byte RESOURCE_INTEREST_TYPE = 1;

    /**
     * The max resource interest type.
     */
    public static final byte MAX_INTEREST_TYPE = Byte.MAX_VALUE;

    /**
     * The length of the pdata's header.
     */
    public static final int HEADER_LENGTH = 16;

    /**
     * The offset to the length in the header.
     */
    private static final int LENGTH_OFFSET = 0;

    /**
     * The offset to the type in the header.
     */
    private static final int TYPE_OFFSET = 4;

    /**
     * The offset to the version in the header.
     */
    private static final int VERSION_OFFSET = 5;

    /**
     * The interest type.
     */
    private final byte type;

    /**
     * The version number.
     */
    private final byte version;

    /**
     * The persistent interest data to be hardened: [[header][data]].
     */
    private byte[] pdata;

    /**
     * Mainline constructor.
     *
     * @param type    The interest type.
     * @param version The PDATA version.
     */
    PersistentInterestData(byte type, byte version) {
        this.type = type;
        this.version = version;
    }

    /**
     * Recovery constructor.
     *
     * @param pdata The persistent interest data.
     */
    PersistentInterestData(byte[] pdata) {
        this.pdata = pdata;
        this.type = pdata[TYPE_OFFSET];
        this.version = pdata[VERSION_OFFSET];
    }

    /**
     * Generates the persistent interest data [[header][data]].
     *
     * @param data The data to be placed in the data section of the pdata. If
     *                 no data is provided (null), the header information is returned.
     *
     * @return The persistent data to be hardened by RRS.
     */
    public byte[] generatePdata(byte[] data) {
        int pdataSize = HEADER_LENGTH;
        int lengthToCopy = 0;

        if (data != null) {
            pdataSize += data.length;
            if (pdataSize > RRSServices.ATR_MAX_PDATA_LENGTH) {
                switch (version) {
                    case 1:
                        // Under this version the pdata does not hold critical UR information.
                        // The data logged is purely informational server related data (name,
                        // uuid, configDirPath) that in practice should not exceed the max pdata
                        // length. However, in the event that it does, we will save up to the
                        // max value.
                        pdataSize = RRSServices.ATR_MAX_PDATA_LENGTH;
                        lengthToCopy = RRSServices.ATR_MAX_PDATA_LENGTH;
                        break;
                    default:
                        throw new RuntimeException("Invalid perstent interest data version: " + version);
                }
            } else {
                lengthToCopy = data.length;
            }
        }

        pdata = new byte[pdataSize];

        // Populate header section.
        pdata[TYPE_OFFSET] = type;
        pdata[VERSION_OFFSET] = version;
        Util.setBytesFromInt(pdata, LENGTH_OFFSET, 4, pdataSize);

        // Populate data section.
        if (data != null && lengthToCopy > 0) {
            System.arraycopy(data, 0, pdata,
                             HEADER_LENGTH,
                             lengthToCopy);
        }

        return pdata;
    }

    /**
     * Retrieves the persistent data's version.
     *
     * @return The persistent data's version.
     */
    public byte getPdataVersion() {
        return version;
    }

    /**
     * Retrieves the interest type associated with the persistent data.
     *
     * @return The interest type.
     */
    public byte getInterestType() {
        return type;
    }

    /**
     * Retrieves the persistent interest data: [[header][data]].
     *
     * @return The persistent data.
     */
    public byte[] getPdata() {
        return pdata;
    }

    /**
     * Retrieves the data section of the persistent interest data.
     *
     * @return The data section of the persistent interest data.
     */
    public byte[] getDataSection() {
        byte[] data = null;

        if (pdata != null) {
            int dataLength = pdata.length - HEADER_LENGTH;

            if (dataLength > 0) {
                data = new byte[dataLength];
                System.arraycopy(pdata, HEADER_LENGTH, data, 0, dataLength);
            }
        }

        return data;
    }
}
