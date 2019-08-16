/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 *
 */
public class JSONUtilTest {

    /**
     * Test method for {@link com.ibm.oauth.core.util.JSONUtil#getJsonObject(java.util.Map)}.
     */
    @Test
    public void testGetJsonObject() {

        String customname = "Ричард";

        String givenName = formatString(customname).toString();
        String customname2 = "Кэмпбелл";
        String familyName = formatString(customname2).toString();

        //String name = givenName + " " + familyName;
        String name = customname + " " + customname2;
        String[] str = { name };

        //System.out.println("@AV222 , name : " + str[0]);

        Map<String, String[]> extMap = new HashMap();

        String[] str2 = { "506Q5JSJET" };
        extMap.put("com.ibm.wsspi.security.oidc.external.claims:uniqueSecurityName", str2);

        String[] str3 = { "https://prepiam.toronto.ca.ibm.com" };
        extMap.put("com.ibm.wsspi.security.oidc.external.claims:iss", str3);

        extMap.put("com.ibm.wsspi.security.oidc.external.claims:name", str
                        );

        JSONUtil.getJsonObject(extMap);
        //should not receive any exception here.

        //fail("Not yet implemented");
    }

    @Test
    public void testGetJsonObjectWithAscii() {

        String customname = "hyderabad";

        String givenName = formatString(customname).toString();
        String customname2 = "secunderabad";
        String familyName = formatString(customname2).toString();

        //String name = givenName + " " + familyName;
        String name = customname + " " + customname2;
        String[] str = { name };

        //System.out.println("@AV222 , name : " + str[0]);

        Map<String, String[]> extMap = new HashMap();

        String[] str2 = { "506Q5JSJET" };
        extMap.put("com.ibm.wsspi.security.oidc.external.claims:uniqueSecurityName", str2);

        String[] str3 = { "https://prepiam.toronto.ca.ibm.com" };
        extMap.put("com.ibm.wsspi.security.oidc.external.claims:iss", str3);

        extMap.put("com.ibm.wsspi.security.oidc.external.claims:name", str
                        );

        JSONUtil.getJsonObject(extMap);
        //should not receive any exception here.

        //fail("Not yet implemented");

    }

    private static StringBuilder formatString(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            switch (ch) {
                case '\"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if ((ch & 0xFF00) == 0) {
                        sb.append(ch);
                    } else {
                        sb.append("\\u");
                        String hex = Integer.toHexString(Character.codePointAt(str, i));
                        //new code
                        int zerosNeeded = 4 - hex.length();
                        for (int j = 0; j < zerosNeeded; j++) {
                            sb.append('0');
                        }
                        //original code
                        //if (hex.length() < 3) {
                        //    sb.append('0');
                        //}
                        sb.append(hex);
                    }
            }
        }
        return sb;
    }
}
