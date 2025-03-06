// https://github.com/resteasy/resteasy/blob/main/resteasy-core-spi/src/main/java/org/jboss/resteasy/spi/util/MethodHashing.java
package org.jboss.resteasy.spi.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public final class MethodHashing {
    public static long methodHash(Method method)
            throws Exception {
        Class<?>[] parameterTypes = method.getParameterTypes();
        StringBuilder methodDesc = new StringBuilder(method.getName()).append("(");
        for (int j = 0; j < parameterTypes.length; j++) {
            methodDesc.append(getTypeString(parameterTypes[j]));
        }
        methodDesc.append(")").append(getTypeString(method.getReturnType()));
        return createHash(methodDesc.toString());
    }

    public static long createHash(String methodDesc)
            throws Exception {
        long hash = 0;
        ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream(512);
        MessageDigest messagedigest = getMessageDigest(); // Liberty Change
        DataOutputStream dataoutputstream = new DataOutputStream(new DigestOutputStream(bytearrayoutputstream, messagedigest));
        dataoutputstream.writeUTF(methodDesc);
        dataoutputstream.flush();
        byte[] abyte0 = messagedigest.digest();
        for (int j = 0; j < Math.min(8, abyte0.length); j++)
            hash += (long) (abyte0[j] & 0xff) << j * 8;
        return hash;

    }

    static String getTypeString(Class<?> cl) {
        if (cl == Byte.TYPE) {
            return "B";
        } else if (cl == Character.TYPE) {
            return "C";
        } else if (cl == Double.TYPE) {
            return "D";
        } else if (cl == Float.TYPE) {
            return "F";
        } else if (cl == Integer.TYPE) {
            return "I";
        } else if (cl == Long.TYPE) {
            return "J";
        } else if (cl == Short.TYPE) {
            return "S";
        } else if (cl == Boolean.TYPE) {
            return "Z";
        } else if (cl == Void.TYPE) {
            return "V";
        } else if (cl.isArray()) {
            return "[" + getTypeString(cl.getComponentType());
        } else {
            return "L" + cl.getName().replace('.', '/') + ";";
        }
    }

    // Liberty Change Start
    // https://issues.redhat.com/browse/RESTEASY-3585 
    private static MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
//        try {
//            return MessageDigest.getInstance("SHA-1");
//        } catch (Throwable ignore) {
//        }
        return MessageDigest.getInstance("SHA-256");
    }
    // Liberty Change End
}