/* =============================================================================
 * Copyright (c) 2017,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package com.ibm.ws.jndi.iiop;

import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;

import org.omg.CosNaming.NameComponent;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jndi.WSName;

public enum CosNameUtil {
    ;
    private static final TraceComponent tc = Tr.register(CosNameUtil.class);
    private static final Pattern PERCENT_TRIPLET = Pattern.compile("%(?:[0-9a-f]{2}|[0-9A-F]{2})");
    private static final Pattern ILLEGAL_NAME = Pattern.compile("(?:[^/]\\./|\\.[^/]*\\.|([^/]\\.$))");
    private static final ImmutableBitSet ESCAPE_NOT_NEEDED;
    private static final Pattern ALL_LEGAL_CHARS;

    static {
        final String unescapedChars = (
                "-;/:?@&=+$,_.!~*’()" +
                "0123456789" +
                "abcdefghijklmnopqrstuvwxyz" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        final BitSet escapeNotNeeded = new BitSet();
        for (char c : unescapedChars.toCharArray()) escapeNotNeeded.set(c);

        ESCAPE_NOT_NEEDED = new ImmutableBitSet(escapeNotNeeded);
        ALL_LEGAL_CHARS = Pattern.compile("[" + unescapedChars + "]*");
    }

    public static String escapeCorbanameUrlIfNecessary(String url) {
        final String methodName = "escapeCorbanameUrlIfNecessary(): ";
        if (null == url || !url.startsWith("corbaname:") || url.contains("\\"))
            return url;

        // split on the first hash, which MUST delimit the start of the stringified name
        String[] twoParts = url.split("#", 2);
        // early return if there is no stringified name
        if (twoParts.length < 2)
            return url;

        String stringifiedName = twoParts[1];
        if (stringifiedName.isEmpty())
            return url;

        // check if it contains any percent escapes
        if (PERCENT_TRIPLET.matcher(stringifiedName).find()) {
            // found some - not touching this string
            // if it is badly escaped then errors will arise later
            return url;
        }

        // check for no need of escaping
        if (ALL_LEGAL_CHARS.matcher(stringifiedName).matches()) {
            // no characters need to be URI-escaped
            // so just check for illegal dot patterns
            Matcher matcher = ILLEGAL_NAME.matcher(stringifiedName);
            if (!matcher.find()) return url; // really, nothing needed replacing!
        }

        // escape backslashes and dots
        String sn = stringifiedName.replace("\\", "\\\\").replace(".", "\\.");

        // now for the URI escaping...
        // The CosNaming specification v1.4 2.5.3.3 specifies that:
        //   corbaname URLs use the escape mechanism described in the Internet Engineering
        //   Task Force (IETF) RFC 2396. These escape rules insure that URLs can be transferred
        //   via a variety of transports without undergoing changes. The character escape rules for
        //   the stringified name portion of a corbaname are:
        //
        // The CosNaming spec goes on to say that only the following characters go unescaped:
        // * US-ASCII alphanumeric characters
        // * any of these: ; / : ? @ & = + $ , - _ . ! ~ * ’ ( )
        StringBuilder escaped = new StringBuilder(twoParts[0]).append("#");
        // since we must use an octet-based representation to URI-encode, convert the string into its UTF-8 bytes
        for (byte b : sn.getBytes(StandardCharsets.UTF_8)) {
            if (ESCAPE_NOT_NEEDED.get(b)) {
                escaped.append((char) b);
            } else {
                escaped.append(String.format("%%%02x", 0xFF & b));
            }
        }

        if (tc.isDebugEnabled()) Tr.debug(tc, methodName + "escaped original url " + url + " to " + escaped);
        return escaped.toString();
    }

    static NameComponent[] cosify(WSName name) {
        NameComponent[] cosName = new NameComponent[name.size()];
        for (int i = 0; i < cosName.length; i++)
            cosName[i] = new NameComponent(name.get(i), "");
        return cosName;
    }

    @FFDCIgnore(InvalidNameException.class)
    static <T extends NamingException> T detailed(T toThrow, Exception initCause, NameComponent... nameRemainder) {
        toThrow.initCause(initCause);
        if (nameRemainder != null && nameRemainder.length > 0) {
            try {
                toThrow.setRemainingName(compose(nameRemainder));
            } catch (InvalidNameException e) {
                toThrow.addSuppressed(e);
            }
        }
        return toThrow;
    }

    private static CompositeName compose(NameComponent... nameRemainder) throws InvalidNameException {
        WSName wsName = new WSName();
        for (NameComponent nc: nameRemainder) wsName = wsName.plus(nc.id);
        CompositeName cName = new CompositeName();
        cName.add(wsName.toString());
        return cName;
    }
}
