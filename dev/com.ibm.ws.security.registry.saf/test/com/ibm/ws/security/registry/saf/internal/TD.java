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
package com.ibm.ws.security.registry.saf.internal;

import java.util.Formatter;
import java.util.regex.Matcher;

/**
 * Common test data (userIds, passwords, etc) used by the SAF registry tests.
 */
public class TD {

    // Various Ids in EBCDIC.
    public static final String id1_str = "RSTID1";
    public static final byte[] id1_ebc = new byte[] { -39, -30, -29, -55, -60, -15, 0 }; // "RSTID1" in EBCDIC (hex.d9.e2.e3.c9.c4.f1.00)
    public static final String id2_str = "rstid2";
    public static final byte[] id2_ebc = new byte[] { -103, -94, -93, -119, -124, -14, 0 }; // "rstid2" in EBCDIC (hex.99.a2.a3.89.84.f2.00)
    public static final String id3_str = "rst id3";
    public static final byte[] id3_ebc = new byte[] { -103, -94, -93, 64, -119, -124, -13, 0 }; // "rst id3" in EBCDIC (hex.99.a2.a3.40.89.84.f3.00)
    public static final String id4_str = "RST\0ID4";
    public static final byte[] id4_ebc = new byte[] { -39, -30, -29, 0, -55, -60, -12, 0 }; // "RST\0ID4" in EBCDIC (hex.d9.e2.e3.00.c9.c4.f4.00)
    public static final String id5_str = "        ";
    public static final byte[] id5_ebc = new byte[] { 64, 64, 64, 64, 64, 64, 64, 64, 0 }; // "        " in EBCDIC (hex.40.40.40.40.40.40.40.40.00)
    public static final String id6_str = "";
    public static final byte[] id6_ebc = new byte[] { 0 }; // "" in EBCDIC (hex.00)
    public static final String id7_str = "!@#$%^&*";
    public static final byte[] id7_ebc = new byte[] { 90, 124, 123, 91, 108, 95, 80, 92, 0 }; // "!@#$%^&*" in EBCDIC (hex.5a.7c.7b.5b.6c.5f.50.5c.00)

    // Various userIds in EBCDIC.
    public static final String u1_str = "USER1";
    public static final byte[] u1_ebc = new byte[] { -28, -30, -59, -39, -15, 0 }; // "USER1" in EBCDIC (hex.e4.e2.c5.d9.f1.00)
    public static final String u2_str = "USER2";
    public static final byte[] u2_ebc = new byte[] { -28, -30, -59, -39, -14, 0 }; // "USER2" in EBCDIC (hex.e4.e2.c5.d9.f2.00)
    public static final String u3_str = "USER3";
    public static final byte[] u3_ebc = new byte[] { -28, -30, -59, -39, -13, 0 }; // "USER3" in EBCDIC (hex.e4.e2.c5.d9.f3.00)
    public static final String u4_str = "USER4";
    public static final byte[] u4_ebc = new byte[] { -28, -30, -59, -39, -12, 0 }; // "USER4" in EBCDIC (hex.e4.e2.c5.d9.f4.00)
    public static final String u5_str = "WUSER5";
    public static final byte[] u5_ebc = new byte[] { -26, -28, -30, -59, -39, -11, 0 }; // "WUSER5" in EBCDIC (hex.e6.e4.e2.c5.d9.f5.00)
    public static final String u6_str = "WUSER6";
    public static final byte[] u6_ebc = new byte[] { -26, -28, -30, -59, -39, -10, 0 }; // "WUSER6" in EBCDIC (hex.e6.e4.e2.c5.d9.f6.00)
    public static final String u7_str = "USER7";
    public static final byte[] u7_ebc = new byte[] { -28, -30, -59, -39, -9, 0 }; // "USER7" in EBCDIC (hex.e4.e2.c5.d9.f7.00)

    // Various groupIds in EBCDIC.
    public static final String g1_str = "GROUP1";
    public static final byte[] g1_ebc = new byte[] { -57, -39, -42, -28, -41, -15, 0 }; // "GROUP1" in EBCDIC (hex.c7.d9.d6.e4.d7.f1.00)
    public static final String g2_str = "GROUP2";
    public static final byte[] g2_ebc = new byte[] { -57, -39, -42, -28, -41, -14, 0 }; // "GROUP2" in EBCDIC (hex.c7.d9.d6.e4.d7.f2.00)
    public static final String g3_str = "GROUP3";
    public static final byte[] g3_ebc = new byte[] { -57, -39, -42, -28, -41, -13, 0 }; // "GROUP3" in EBCDIC (hex.c7.d9.d6.e4.d7.f3.00)
    public static final String g4_str = "GROUP4";
    public static final byte[] g4_ebc = new byte[] { -57, -39, -42, -28, -41, -12, 0 }; // "GROUP4" in EBCDIC (hex.c7.d9.d6.e4.d7.f4.00)
    public static final String g5_str = "GROUP5";
    public static final byte[] g5_ebc = new byte[] { -57, -39, -42, -28, -41, -11, 0 }; // "GROUP5" in EBCDIC (hex.c7.d9.d6.e4.d7.f5.00)
    public static final String g6_str = "MGROUP6";
    public static final byte[] g6_ebc = new byte[] { -44, -57, -39, -42, -28, -41, -10, 0 }; // "MGROUP6" in EBCDIC (hex.d4.c7.d9.d6.e4.d7.f6.00)
    public static final String g7_str = "MGROUP7";
    public static final byte[] g7_ebc = new byte[] { -44, -57, -39, -42, -28, -41, -9, 0 }; // "MGROUP7" in EBCDIC (hex.d4.c7.d9.d6.e4.d7.f7.00)

    // Various passwords in EBCDIC.
    public static final String p1_str = "trailsp  ";
    public static final byte[] p1_ebc = new byte[] { -93, -103, -127, -119, -109, -94, -105, 64, 64, 0 }; // "trailsp  " in EBCDIC (hex.a3.99.81.89.93.a2.97.40.40.00)
    public static final String p2_str = "  leadsp";
    public static final byte[] p2_ebc = new byte[] { 64, 64, -109, -123, -127, -124, -94, -105, 0 }; // "  leadsp" in EBCDIC (hex.40.40.93.85.81.84.a2.97.00)
    public static final String p3_str = "emb space";
    public static final byte[] p3_ebc = new byte[] { -123, -108, -126, 64, -94, -105, -127, -125, -123, 0 }; // "emb space" in EBCDIC (hex.85.94.82.40.a2.97.81.83.85.00)
    public static final String p4_str = "        ";
    public static final byte[] p4_ebc = new byte[] { 64, 64, 64, 64, 64, 64, 64, 64, 0 }; // "        " in EBCDIC (hex.40.40.40.40.40.40.40.40.00)
    public static final String p5_str = "";
    public static final byte[] p5_ebc = new byte[] { 0 }; // "" in EBCDIC (hex.00)
    public static final String p6_str = "!@#$%^&*";
    public static final byte[] p6_ebc = new byte[] { 90, 124, 123, 91, 108, 95, 80, 92, 0 }; // "!@#$%^&*" in EBCDIC (hex.5a.7c.7b.5b.6c.5f.50.5c.00)
    public static final String p7_str = "emb\0null";
    public static final byte[] p7_ebc = new byte[] { -123, -108, -126, 0, -107, -92, -109, -109, 0 }; // "emb\0null" in EBCDIC (hex.85.94.82.00.95.a4.93.93.00)
    public static final String p8_str = "mIxEdCaSe";
    public static final byte[] p8_ebc = new byte[] { -108, -55, -89, -59, -124, -61, -127, -30, -123, 0 }; // "mIxEdCaSe" in EBCDIC (hex.94.c9.a7.c5.84.c3.81.e2.85.00)
    public static final String p9_str = "NORMAL1";
    public static final byte[] p9_ebc = new byte[] { -43, -42, -39, -44, -63, -45, -15, 0 }; // "NORMAL1" in EBCDIC (hex.d5.d6.d9.d4.c1.d3.f1.00)

    // Various native realm names in EBCDIC.
    public static final String r1_str = "NTVREALM";
    public static final byte[] r1_ebc = new byte[] { -43, -29, -27, -39, -59, -63, -45, -44, 0 }; // "NTVREALM" in EBCDIC (hex.d5.e3.e5.d9.c5.c1.d3.d4.00)
    public static final String r2_str = "NTVPLEXNAME";
    public static final byte[] r2_ebc = new byte[] { -43, -29, -27, -41, -45, -59, -25, -43, -63, -44, -59, 0 }; // "NTVPLEXNAME" in EBCDIC (hex.d5.e3.e5.d7.d3.c5.e7.d5.c1.d4.c5.00)
    public static final String r3_str = "";
    public static final byte[] r3_ebc = new byte[] {}; // Empty

    // Empty ebc, used for saf profile param in native methods calls
    public static byte[] empty_ebc = new byte[] { 0 };

    /**
     * This utility method converts the input string to EBCDIC bytes and prints
     * those bytes in a byte[] array initialization format (i.e. new byte[] { 98,57,-106,0 };)
     *
     * Note: This utility is not intended to actually be used for any testing purposes.
     * It is intended only to assist test writers in figuring out the expected EBCDIC
     * bytes for a String. All the userIds, groupIds, passwords, etc, defined at the
     * bottom of this class were generated by this method.
     *
     * For example, if you make the following calls:
     *
     * convertToEBCDICHelper("USER1", "u1");
     * convertToEBCDICHelper("USER2", "u2");
     *
     * the code will print the following to stdout:
     *
     * final String u1_str = "USER1";
     * final byte[] u1_ebc = new byte[] { -28,-30,-59,-39,-15,0 }; // "USER1" in EBCDIC (hex.e4.e2.c5.d9.f1.00)
     * final String u2_str = "USER2";
     * final byte[] u2_ebc = new byte[] { -28,-30,-59,-39,-14,0 }; // "USER2" in EBCDIC (hex.e4.e2.c5.d9.f2.00)
     *
     * You can then copy-and-paste those declarations into this test class and
     * use them for your tests.
     *
     * @param s        the String data to convert to EBCDIC
     * @param sVarName the stem used to name the generated variable declarations.
     *                     For example, "u1" would generate variables "u1_str" (declared
     *                     as the String to convert), and "u1_ebc" (the byte[] array
     *                     containing the EBCDIC bytes).
     */
    public static void convertToEBCDICHelper(String s, String sVarName) throws Exception {
        byte[] b = (s + '\0').getBytes("IBM-1047");

        String bArrStr = "";
        Formatter f = new Formatter();

        for (int i = 0; i < b.length; ++i) {
            bArrStr += b[i] + ((i < b.length - 1) ? "," : "");
            f.format(".%1$02x", b[i]);
        }

        String s2 = s.replaceAll("\\00", Matcher.quoteReplacement("\\0")); // replace null chars with "\0" in string literal
        String sDeclare = "final String " + sVarName + "_str = \"" + s2 + "\";"; // declare String
        String bytesDeclare = "final byte[] " + sVarName + "_ebc = new byte[] { " + bArrStr + " };"; // declare byte[]

        System.out.println(sDeclare);
        System.out.println(bytesDeclare + " // \"" + s2 + "\" in EBCDIC (hex" + f.toString() + ")");
    }

    // /**
    //  * A place to put convertToEBCDICHelper calls, if you need some.
    //  */
    // @Test
    // public void getEBCDICData() throws Exception {
    //     convertToEBCDICHelper("NTVREALM", "r1");
    // }

}
