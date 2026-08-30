/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.authorization.saf;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;

/**
 * For exceptions thrown from SAFAuthorizationService.
 */
public class SAFAuthorizationException extends Exception {

    /** default */
    private static final long serialVersionUID = 1L;

    private int SAFReturnCode;
    private int RACFReturnCode;
    private int RACFReasonCode;
    private String userSecurityName;
    private String applID;
    private String SAFProfile;
    private String SAFClass;
    private String volser;
    private boolean vsam;

    public SAFAuthorizationException(Throwable t) {
        super(t);
    }

    public SAFAuthorizationException() {
        SAFReturnCode = -1;
        RACFReturnCode = -1;
        RACFReasonCode = -1;
        userSecurityName = null;
        applID = null;
        SAFProfile = null;
        SAFClass = null;
        volser = null;
        vsam = false;
    }

    public SAFAuthorizationException(int safReturnCode, int racfReturnCode, int racfReasonCode,
                                     String userSecName, String applID, String safProfile, String safClass) {
        this.SAFReturnCode = safReturnCode;
        this.RACFReturnCode = racfReturnCode;
        this.RACFReasonCode = racfReasonCode;
        this.userSecurityName = userSecName;
        this.applID = applID;
        this.SAFProfile = safProfile;
        this.SAFClass = safClass;
    }

    /**
     * Constructor with volser and vsam fields
     */
    public SAFAuthorizationException(int safReturnCode, int racfReturnCode, int racfReasonCode,
                                     String userSecName, String applID, String safProfile, String safClass, String volser, boolean vsam) {
        this.SAFReturnCode = safReturnCode;
        this.RACFReturnCode = racfReturnCode;
        this.RACFReasonCode = racfReasonCode;
        this.userSecurityName = userSecName;
        this.applID = applID;
        this.SAFProfile = safProfile;
        this.SAFClass = safClass;
        this.volser = volser;
        this.vsam = vsam;
    }

    /**
     * The SAF return code from the service which generated this authorization failure.
     *
     * @return The SAF return code, or -1 if the SAF return code is not available.
     */
    public int getSafReturnCode() {
        return SAFReturnCode;
    }

    /**
     * The RACF return code from the service which generated this authorization failure. Note that
     * if a security product other than RACF is being used, its return code will be returned by this
     * method.
     *
     * @return The RACF return code, or -1 if the RACF return code is not available.
     */
    public int getRacfReturnCode() {
        return RACFReturnCode;
    }

    /**
     * The RACF reason code from the service which generated this authorization failure. Note that
     * if a security product other than RACF is being used, its reason code will be returned by this
     * method.
     *
     * @return The RACF reason code, or -1 if the RACF reason code is not available.
     */
    public int getRacfReasonCode() {
        return RACFReasonCode;
    }

    /**
     * The username against who this authorization check was made.
     *
     * @return The username against who this authorization check was made, or null if not available.
     */
    public String getUserSecurityName() {
        return userSecurityName;
    }

    /**
     * The APPLID used to perform this authorization check.
     *
     * @return The APPLID, or null if not available.
     */
    public String getApplid() {
        return applID;
    }

    /**
     * The SAF profile against which the authorization check was made.
     *
     * @return The SAF profile, or null if not available.
     */
    public String getSafProfile() {
        return SAFProfile;
    }

    /**
     * The SAF class against which the authorization check was made.
     *
     * @return The SAF class, or null if not available.
     */
    public String getSafClass() {
        return SAFClass;
    }

    /**
     * The Volume Serial number for where the dataset is stored.
     * Used for isAuthorizedToDataset and isGroupAuthorizedToDataset
     *
     * @return 6-char string volume serial number, or null if not available
     */
    public String getVolser() {
        return volser;
    }

    /**
     * Flag that specifies whether dataset is vsam or non-vsam.
     * Used for isAuthorizedToDataset and isGroupAuthorizedToDataset
     *
     * @return boolean value of vsam
     */
    public boolean getVsam() {
        return vsam;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        ObjectOutputStream.PutField putField = out.putFields();
        putField.put("SAFReturnCode", SAFReturnCode);
        putField.put("RACFReturnCode", RACFReturnCode);
        putField.put("RACFReasonCode", RACFReasonCode);
        putField.put("userSecurityName", userSecurityName);
        putField.put("applID", applID);
        putField.put("SAFProfile", SAFProfile);
        putField.put("SAFClass", SAFClass);
        putField.put("volser", volser);
        putField.put("vsam", vsam);
        out.writeFields();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField getField = in.readFields();
        SAFReturnCode = getField.get("SAFReturnCode", -1);
        RACFReturnCode = getField.get("RACFReturnCode", -1);
        RACFReasonCode = getField.get("RACFReasonCode", -1);
        userSecurityName = (String) getField.get("userSecurityName", null);
        applID = (String) getField.get("applID", null);
        SAFProfile = (String) getField.get("SAFProfile", null);;
        SAFClass = (String) getField.get("SAFClass", null);
        volser = (String) getField.get("volser", null);
        vsam = getField.get("vsam", false);
    }

    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[] { new ObjectStreamField("SAFReturnCode", Integer.class),
                                                                                                new ObjectStreamField("RACFReturnCode", Integer.class),
                                                                                                new ObjectStreamField("RACFReasonCode", Integer.class),
                                                                                                new ObjectStreamField("userSecurityName", String.class),
                                                                                                new ObjectStreamField("applID", String.class),
                                                                                                new ObjectStreamField("SAFProfile", String.class),
                                                                                                new ObjectStreamField("SAFClass", String.class),
                                                                                                new ObjectStreamField("volser", String.class),
                                                                                                new ObjectStreamField("vsam", boolean.class)
    };
}
