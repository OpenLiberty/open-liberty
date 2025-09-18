/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.wss4j.common.util;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.util.List;

/**
 * Convert a RFC 2253 String using \ to escape unicode characters into one that is compatible
 * with Microsoft's WFC and Java.<p><br>
 *
 * <strong>Detail:</strong>
 * Converts a string in RFC2253 format and replaces \ escaped characters with a string quoted representation.
 * It also places a space before the next RDN. <p> <br>
 * There are two alternate ways an RFC 2253 RDN can escape unicode characters, either with '\'
 * or by using quotes. Java seems to recognize both formats but Microsoft's WFC only seems to recognize quotes.
 * Since implementations may escape any characters and string is already in valid format no knowledge is
 * required of escapable characters.
 */
public class CommaDelimiterRfc2253Name {

    /**
     * Return rfc2253String that delimits using quotes.<p>
     *
     *
     * @param rfc2253String a string in rfc 2253 format using a \ as delimiter.
     * @return Rdn in quoted form if required.
     * @throws  IllegalArgumentException if an error occurred parsing the rfc2256 string.
     * However, since its assumed that a valid RFC 2253 string, X500Principal.getName(),
     * is passed its unlikely that this exception would ever be thrown.
     */
    public String execute(String rfc2253String) {
        StringBuilder commaDNBuilder = new StringBuilder();
        try {
            LdapName ldapname = new LdapName(rfc2253String);
            List<Rdn> rdns = ldapname.getRdns();

            for (int i = rdns.size() - 1; i >= 0; i--) {
                Rdn rdn = rdns.get(i);
                String rdnString = rdn.toString();
                String appendString;
                if (requiresDoubleQuoting(rdnString)) {
                    appendString = convertToDoubleQuotes(rdnString);
                } else {
                    appendString = rdnString;
                }
                if (i == rdns.size() - 1) {
                    commaDNBuilder.append(appendString);
                } else {
                    commaDNBuilder.append(", ").append(appendString);
                }
            }
        } catch (InvalidNameException e) {
            throw new IllegalArgumentException(" The distinguished name cannot be parsed : " + rfc2253String);
        }
        return commaDNBuilder.toString();
    }

    private boolean requiresDoubleQuoting(String rdnString) {
        return rdnString.contains("\\");
    }

    private String convertToDoubleQuotes(String rdnString) {
        StringBuilder quotedString = new StringBuilder();
        int indexEquals = rdnString.indexOf("=");
        String firstPart = rdnString.substring(0, indexEquals + 1);
        String lastPart = rdnString.substring(indexEquals + 1);
        String secondPart = unEscapeRfc2253RdnSubPart(lastPart);
        return quotedString.append(firstPart).append('"').append(secondPart).append('"').toString();
    }

    String unEscapeRfc2253RdnSubPart(String value) {
        char[] charArray = value.toCharArray();
        boolean previousEscape = false;
        StringBuilder unescapedRdnPart = new StringBuilder();
        for (char currentChar : charArray) {
            if (currentChar != '\\') {
                previousEscape = false;
                unescapedRdnPart.append(currentChar);
            } else if (previousEscape) {
                unescapedRdnPart.append(currentChar);
                previousEscape = false;
            } else {
                previousEscape = true;
            }
        }

        return unescapedRdnPart.toString();
    }

}
