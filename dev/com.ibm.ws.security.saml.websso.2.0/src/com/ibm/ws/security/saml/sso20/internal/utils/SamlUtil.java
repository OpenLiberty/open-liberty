/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal.utils;

import java.security.SecureRandom;
import java.security.Security;
import java.util.Random;
import java.util.Set;

import javax.security.auth.Subject;
import javax.xml.namespace.QName;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.saml.Constants;

/**
 * Collection of utility methods for String to byte[] conversion.
 */
public class SamlUtil {
    @SuppressWarnings("unused")
    private static final TraceComponent tc = Tr.register(SamlUtil.class,
                                                         TraceConstants.TRACE_GROUP,
                                                         TraceConstants.MESSAGE_BUNDLE);

    static final String chars = Constants.CHARS;

    static final String JCEPROVIDER_IBM = "IBMJCE";

    static final String SECRANDOM_IBM = "IBMSecureRandom";

    static final String SECRANDOM_SHA1PRNG = "SHA1PRNG";

    public static String hash(String stringToEncrypt) {
        if (stringToEncrypt == null) {
            return null;
        }
        int hashCode = stringToEncrypt.hashCode();
        if (hashCode < 0) {
            hashCode = hashCode * -1;
            return "n" + hashCode;
        } else {
            return "p" + hashCode;
        }
    }

    /**
     * This method will return a <code>String</code> that is compliant with xs:id simple
     * type which is used to declare SAML identifiers for assertions, requests, and responses.
     * The String returned will contain 33 chars and will always start with '_', the probability
     * to have a duplicate is 62^32.
     *
     * @return <code>String</code> compliant with xs:id
     */
    public static String generateRandomID() {
        return "_" + generateRandom(32);
    }

    public static String generateRandom() {
        return generateRandom(32);
    }

    public static String generateRandom(int iCharCnt) {
        StringBuffer sb = new StringBuffer();
        //if (prefix != null)
        //    sb.append(prefix);
        Random r = getRandom();
        for (int i = 0; i < iCharCnt; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    static Random getRandom() {
        Random result = null;
        try {
            if (Security.getProvider(JCEPROVIDER_IBM) != null) {
                result = SecureRandom.getInstance(SECRANDOM_IBM);
            } else {
                result = SecureRandom.getInstance(SECRANDOM_SHA1PRNG);
            }
        } catch (Exception e) {
            result = new Random();
        }
        return result;
    }

    public static QName cloneQName(QName qName) {
        if (qName == null)
            return null;
        return new QName(qName.getNamespaceURI(), qName.getLocalPart(), qName.getPrefix());
    }

    /**
     * @param subject
     * @return
     */
    public static WSCredential getWSCredential(Subject subject) {
        if (subject == null)
            return null;
        Set<WSCredential> credentials = subject.getPublicCredentials(WSCredential.class);
        return credentials.iterator().next();
    }

    /**
     * @param realmName
     * @param userId
     * @param cRealmName
     * @param cUserId
     * @return
     */
    public static boolean sameUser(String realmName, String userId, String cRealmName, String cUserId) {
        if (realmName == null) {
            if (cRealmName != null)
                return false;
        } else {
            if (!realmName.equals(cRealmName)) {
                return false;
            }
        }
        if (userId == null) {
            //if( cUserId != null) return false;
            return false;
        } else {
            return userId.equals(cUserId);
        }
    }

    /**
     * @param realmName
     * @param userId
     * @param cRealmName
     * @param cUserId
     * @return
     */
    public static boolean sameUser(String userName, String accessId) {
        if (userName == null) {
            return false;
        }

        return userName.equals(accessId);
    }

    @Trivial
    public static StringBuffer dumpStackTrace(Throwable cause, int iLimited) {
        StackTraceElement[] stackTrace = cause.getStackTrace();
        if (iLimited == -1 || iLimited > stackTrace.length)
            iLimited = stackTrace.length;
        StringBuffer sb = new StringBuffer("\n  ");
        int iI = 0;
        for (; iI < iLimited; iI++) {
            sb.append(stackTrace[iI].toString() + "\n  ");
        }
        if (iI < stackTrace.length) {
            sb.append("  ....\n");
        }
        return sb;
    }
}
