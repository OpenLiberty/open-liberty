/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.registry.saf;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;

/**
 * For exceptions thrown from SAFPasswordUtility.
 */
public class SAFPasswordChangeException extends Exception {

    /**  */
    private static final long serialVersionUID = 826815047L;
    private int SAFReturnCode;
    private int RACFReturnCode;
    private int RACFReasonCode;
    private String userSecurityName;
    private String applID;

    public SAFPasswordChangeException(Throwable t) {
        super(t);
    }

    public SAFPasswordChangeException(String userSecName, String ApplID) {
        SAFReturnCode = -1;
        RACFReturnCode = -1;
        RACFReasonCode = -1;
        userSecurityName = userSecName;
        applID = ApplID;
    }

    public SAFPasswordChangeException(int safReturnCode, int racfReturnCode, int racfReasonCode,
                                      String userSecName, String applID) {
        this.SAFReturnCode = safReturnCode;
        this.RACFReturnCode = racfReturnCode;
        this.RACFReasonCode = racfReasonCode;
        this.userSecurityName = userSecName;
        this.applID = applID;
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

    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[] { new ObjectStreamField("SAFReturnCode", Integer.class),
                                                                                                new ObjectStreamField("RACFReturnCode", Integer.class),
                                                                                                new ObjectStreamField("RACFReasonCode", Integer.class),
                                                                                                new ObjectStreamField("userSecurityName", String.class),
                                                                                                new ObjectStreamField("applID", String.class)
    };

    private void writeObject(ObjectOutputStream out) throws IOException {
        ObjectOutputStream.PutField putField = out.putFields();
        putField.put("SAFReturnCode", SAFReturnCode);
        putField.put("RACFReturnCode", RACFReturnCode);
        putField.put("RACFReasonCode", RACFReasonCode);
        putField.put("userSecurityName", userSecurityName);
        putField.put("applID", applID);
        out.writeFields();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField getField = in.readFields();
        SAFReturnCode = getField.get("SAFReturnCode", -1);
        RACFReturnCode = getField.get("RACFReturnCode", -1);
        RACFReasonCode = getField.get("RACFReasonCode", -1);
        userSecurityName = (String) getField.get("userSecurityName", null);
        applID = (String) getField.get("applID", null);
    }

}
