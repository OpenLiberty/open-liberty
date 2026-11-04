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

package com.ibm.ws390.smf.formatters;

import java.io.UnsupportedEncodingException;

import com.ibm.ws390.sm.smfview.SmfPrintStream;
import com.ibm.ws390.sm.smfview.SmfStream;
import com.ibm.ws390.sm.smfview.SmfUtil;
import com.ibm.ws390.sm.smfview.UnsupportedVersionException;
import com.ibm.ws390.sm.smfview.UserDataSection;

/**
 * Formats SMF 120.11 User Data Tag type 102 (z/OS Connect)
 */
public class SMFType120SubType11UserDataType102 extends UserDataSection {

    public int m_dataVersion;
    public byte[] m_arrivalTime;
    public byte[] m_completionTime;
    public String m_targetURI;
    public int m_inputLength;
    public String m_serviceName;
    public String m_methodName;
    public int m_responseLength;
    public String m_userid;
    public byte[] m_requestId;
    public byte[] m_reserved1;
    public String m_description;
    public String m_mappedUserid;

    /**
     * The SMF 120 Subtype 11 User Data tag type 102 (z/OS Connect)
     *
     * @param uds The User Data Section
     * @throws UnsupportedVersionException  unknown version
     * @throws UnsupportedEncodingException text encoding errors
     */
    public SMFType120SubType11UserDataType102(UserDataSection uds) throws UnsupportedVersionException, UnsupportedEncodingException {
        super(uds);

        /*
         * The data looks like this:
         * * DCL 1 zConnectUserData,
         * 3 version Fixed(31), // Version
         * 3 arrivalTime CHAR(8), // STCK request arrival time
         * 3 completionTime CHAR(8), // STCK request completion time
         * 3 targetURI CHAR(64), // Target URI of request, possibly truncated
         * 3 inputLength Fixed(31), // Length of input JSON string
         * 3 serviceName Char(64), // Service name
         * 3 methodName Char(8), // Method name
         * 3 responseLength Fixed(31), // Length of response JSON string
         * 3 userName Char(64), // userid
         * 3 requestId Char(23), // request id
         * 3 * Char(1), // reserved
         * 3 description Char(64), // description
         * 3 mappedUserName Char(8); // mapped user id
         */

        SmfStream data = new SmfStream(m_data);
        m_dataVersion = data.getInteger(4);
        m_arrivalTime = data.getByteBuffer(8);
        m_completionTime = data.getByteBuffer(8);
        m_targetURI = data.getString(64, SmfUtil.EBCDIC);
        m_inputLength = data.getInteger(4);
        m_serviceName = data.getString(64, SmfUtil.EBCDIC);
        m_methodName = data.getString(8, SmfUtil.EBCDIC);
        m_responseLength = data.getInteger(4);
        m_userid = data.getString(64, SmfUtil.EBCDIC);
        // Request ID is present but blank in version 1..
        m_requestId = data.getByteBuffer(23);
        // pad byte and description are present in version 2
        if (m_dataVersion >= 2) {
            m_reserved1 = data.getByteBuffer(1);
            m_description = data.getString(64, SmfUtil.EBCDIC);
        }
        // mapped used id in version 3
        if (m_dataVersion >= 3) {
            m_mappedUserid = data.getString(8, SmfUtil.EBCDIC);
        }
    }

    //----------------------------------------------------------------------------
    /**
     * Returns the supported version of this class.
     *
     * @return supported version of this class.
     */
    @Override
    public int supportedVersion() {

        return 1; // if we had a real version number in there...whatever..

    }

    //----------------------------------------------------------------------------
    /**
     * Dumps the fields of this object to a print stream.
     *
     * @param aPrintStream   The stream to print to.
     * @param aTripletNumber The triplet number of this UserDataSection.
     */
    @Override
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "UserDataSection");

        aPrintStream.push();
        aPrintStream.printlnKeyValue("Version       ", m_version);
        aPrintStream.printlnKeyValue("Data Type     ", m_dataType);
        aPrintStream.printlnKeyValue("Data Length   ", m_dataLength);

        aPrintStream.printlnKeyValue("data version     ", m_dataVersion);
        aPrintStream.printlnKeyValue("arrival Time     ", m_arrivalTime, null);
        aPrintStream.printlnKeyValue("completion Time  ", m_completionTime, null);
        aPrintStream.printlnKeyValue("target URI       ", m_targetURI);
        aPrintStream.printlnKeyValue("input Length     ", m_inputLength);
        aPrintStream.printlnKeyValue("service name     ", m_serviceName);
        aPrintStream.printlnKeyValue("method name      ", m_methodName);
        aPrintStream.printlnKeyValue("response Length  ", m_responseLength);
        aPrintStream.printlnKeyValue("userid           ", m_userid);
        // request id is present in version 1, but blank so don't bother
        // In version 2 description is there and request ID is filled in (sort of)
        if (m_dataVersion >= 2) {
            aPrintStream.printlnKeyValue("request ID       ", m_requestId, null);
            aPrintStream.printlnKeyValue("description      ", m_description);
        }
        // version 3 has the mapped user id
        if (m_dataVersion >= 3) {
            aPrintStream.printlnKeyValue("mapped userid    ", m_mappedUserid);
        }
        aPrintStream.pop();
    }
}
