/*******************************************************************************
 * Copyright (c) 2001 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws390.sm.smfview;

import java.util.Vector;

/** Utility class for security policies. */
public class SmfSecurityPolicy {

    /** Dce security policy flag bit. */
    public static final int DceSecurityPolicy = 0x01;

    /** Userid/password security policy flag bit. */
    public static final int UseridPasswordSecurityPolicy = 0x02;

    /** Userid/passticket security policy flag bit. */
    public static final int UseridPassticketSecurityPolicy = 0x04;

    /** SSL security policy flag bit. */
    public static final int SslType1SecurityPolicy = 0x08;

    /** Non authenticated client security policy flag bit. */
    public static final int NonAuthenticatedClientsSecurityPolicy = 0x10;

    /** SSL client certificates security policy flag bit. */
    public static final int SslClientCertificatesSecurityPolicy = 0x20;

    /** Kerberos security policy flag bit. */
    public static final int KerberosSecurityPolicy = 0x40;

    /** Send asserted id security policy flag bit. */
    public static final int SendAssertedIdSecurityPolicy = 0x80;

    /** Accept asserted id security policy flag bit. */
    public static final int AcceptAssertedIdSecurityPolicy = 0x100;

    /** Security policies in text form. */
    private static String[] s_policies = {
                                           "DCE",
                                           "UseridPassword",
                                           "UseridPassticket",
                                           "SSL Type1",
                                           "NonAuthenticatedClients",
                                           "SSL ClientCertificates",
                                           "Kerberos",
                                           "SendAssertedId",
                                           "AcceptAssertedId"
    };

    /** Associated texts. */
    public static int[] s_policyFlags = {
                                          DceSecurityPolicy,
                                          UseridPasswordSecurityPolicy,
                                          UseridPassticketSecurityPolicy,
                                          SslType1SecurityPolicy,
                                          NonAuthenticatedClientsSecurityPolicy,
                                          SslClientCertificatesSecurityPolicy,
                                          KerberosSecurityPolicy,
                                          SendAssertedIdSecurityPolicy,
                                          AcceptAssertedIdSecurityPolicy
    };

    //----------------------------------------------------------------------------
    /**
     * Get policies in text form from flag word.
     *
     * @param aPolicyWord Word containing securiy policy flags.
     * @return String with policies in text format.
     */
    public static String policies(int aPolicyWord) {
        StringBuffer sb = null;

        for (int pX = 0; pX < s_policies.length; ++pX) {
            if (SmfUtil.checkBit(aPolicyWord, s_policyFlags[pX])) {
                if (sb == null) {
                    sb = new StringBuffer(s_policies[pX]);
                } else {
                    sb.append(",").append(s_policies[pX]);
                }
            }
        }

        if (sb != null)
            return sb.toString();

        return "Unknown SmfSecurityPolicy";

    } // SmfSecurityPolicy.policies(...)

    //----------------------------------------------------------------------------
    /**
     * Get policies in text form from flag word.
     *
     * @param aPolicyWord Word containing securiy policy flags.
     * @param indent      Initial indentation for result.
     * @return String with policies in text format.
     */
    public static String policies(int aPolicyWord, String indent) {
        Vector policyV = new Vector();
        for (int pX = 0; pX < s_policies.length; ++pX) {
            if (SmfUtil.checkBit(aPolicyWord, s_policyFlags[pX])) {
                policyV.add(s_policies[pX]);
            }
        }

        int policyN = policyV.size();
        if (policyN == 0) {
            policyV.add("Unknown SmfSecurityPolicy");
        }

        StringBuffer policiesSB = new StringBuffer(indent);
        policiesSB.append("(");
        int newlineX = 0;
        for (int pX = 0; pX < policyN; pX++) {
            policiesSB.append((String) policyV.elementAt(pX));
            newlineX++;
            if (pX + 1 < policyN) { // there will be at least one more element
                policiesSB.append(", ");
                if (newlineX == 3) {
                    policiesSB.append("\n");
                    policiesSB.append(indent);
                    newlineX = 0;
                }
            }
        }
        policiesSB.append(")");
        return policiesSB.toString();

    } // SmfSecurityPolicy.policies(...)

} // SmfSecurityPolicy