/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.common.zos;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * Provide access to some unauthorized native WLM services to help for test verification.
 */
public class ZFatNativeHelper {

    public static int zfatNativeLoad(String loadPath) {
        System.out.println("ZFatNativeHelper: zfatNativeLoad -- entry, loadPath:" + loadPath);
        int result = 0;

        try {
            System.load(loadPath);
        } catch (Throwable e) {
            System.out.println("ZFatNativeHelper: zfatNativeLoad -- caught Throwable, " + e);
            e.printStackTrace();
            result = 97;
        }

        System.out.println("ZFatNativeHelper: zfatNativeLoad -- after load");

        return result;
    }

    public static int doTest(int anInt) {
        int rc = ntv_le_test(anInt);
        return rc;
    }

    /**
     * /* ntv_queryWorkUnitClassification Return Data offsets and lengths
     * 
     */
    protected static final int QRD_RETURNCODE = 0x00;
    protected static final int QRD_ERRNO = 0x04;
    protected static final int QRD_ERRNO2 = 0x08;
    protected static final int QRD_TRANSACTIONCLASS = 0x10;
    protected static final int QRD_USER = 0x18;

    /**
     * Extract the Transaction class from the given WLM Enclave
     * 
     * @param enclave target enclave to extract the transaction class
     * @return String value of the transaction class or null.
     */
    public static String getTransactionClass(byte[] enclave) {
        System.out.println("ZFatNativeHelper: getCurrentTransactionClass -- entry, enclave:" + enclave);

        byte[] nativeReturnData = ntv_queryWorkUnitClassification(enclave);

        System.out.println("ZFatNativeHelper: getCurrentTransactionClass -- after call to ntv_queryWorkUnitClassification:" + toHexString(nativeReturnData));

        String tranClass = null;

        if (nativeReturnData != null) {
            ByteBuffer buf = ByteBuffer.wrap(nativeReturnData);

            int rc = buf.getInt(QRD_RETURNCODE);
            int qrd_errno = buf.getInt(QRD_ERRNO);
            int qrd_errno2 = buf.getInt(QRD_ERRNO2);

            byte[] temp = new byte[8];
            buf.position(QRD_TRANSACTIONCLASS);
            buf.get(temp, 0, 8);

            // Convert from EBCDIC to ASCII.
            try {
                tranClass = new String(temp, "Cp1047");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            String user = null;
            temp = new byte[8];
            buf.position(QRD_USER);
            buf.get(temp, 0, 8);

            // Convert from EBCDIC to ASCII.
            try {
                user = new String(temp, "Cp1047");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            System.out.println("ZFatNativeHelper: getCurrentTransactionClass -- nativeReturnData:"
                               + "\n\tReturnCode (" + rc + ")"
                               + "\n\terrno      (" + qrd_errno + ")"
                               + "\n\terrno2     (" + qrd_errno2 + ")"
                               + "\n\tTranClass  (" + tranClass + ")"
                               + "\n\tUser       (" + user + ")"
                            );
        }

        System.out.println("ZFatNativeHelper: getCurrentTransactionClass -- entry");
        return tranClass;
    }

    /**
     * /* ntv_extractWorkUnit Return Data offsets and lengths
     * 
     */
    protected static final int EWU_RETURNCODE = 0x00;
    protected static final int EWU_ERRNO = 0x04;
    protected static final int EWU_ERRNO2 = 0x08;
    protected static final int EWU_ENCLAVE = 0x10;

    public static byte[] getCurrentEnclave() {
        System.out.println("ZFatNativeHelper: getCurrentEnclave -- entry");

        byte[] nativeReturnData = ntv_extractWorkUnit();

        System.out.println("ZFatNativeHelper: getCurrentEnclave -- after call to ntv_extractWorkUnit:" + toHexString(nativeReturnData));

        byte[] enclave = null;

        if (nativeReturnData != null) {
            ByteBuffer buf = ByteBuffer.wrap(nativeReturnData);

            int rc = buf.getInt(EWU_RETURNCODE);
            int ewu_errno = buf.getInt(EWU_ERRNO);
            int ewu_errno2 = buf.getInt(EWU_ERRNO2);

            if (rc == 0) {
                enclave = new byte[8];
                buf.position(EWU_ENCLAVE);
                buf.get(enclave, 0, 8);
            }

            // Note: rc=-1, errno=143 indicates that there is no current enclave.  In that case we'll return a 
            // null value for the enclave.

            System.out.println("ZFatNativeHelper: getCurrentTransactionClass -- nativeReturnData:"
                               + "\n\tReturnCode (" + rc + ")"
                               + "\n\terrno      (" + ewu_errno + ")"
                               + "\n\terrno2     (" + ewu_errno2 + ")"
                               + "\n\tenclave    (" + toHexString(enclave) + ")"
                            );
        }

        System.out.println("ZFatNativeHelper: getCurrentEnclave -- exit");
        return enclave;
    }

    /**
     * Converts a byte array to a hexadecimal string.
     */
    public static String toHexString(byte[] b) {
        final String digits = "0123456789abcdef";

        if (b == null)
            return "null";

        StringBuffer result = new StringBuffer(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            result.append(digits.charAt((b[i] >> 4) & 0xf));
            result.append(digits.charAt(b[i] & 0xf));
        }
        return (result.toString());
    }

    private static native int ntv_le_test(int anInt);

    private static native byte[] ntv_extractWorkUnit();

    private static native byte[] ntv_queryWorkUnitClassification(byte[] etoken);
}
