/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.iiop;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.spi.ObjectFactory;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;

@Component(configurationPolicy=IGNORE,property={"service.vendor=ibm","osgi.jndi.url.scheme=corbaname"})
public class CorbanameUrlContextFactory extends UrlContextFactory implements ObjectFactory, ApplicationRecycleComponent {
    static class Escaper{
        static final TraceComponent tc = Tr.register(CorbanameUrlContextFactory.Escaper.class);
        private static final Pattern PERCENT_TRIPLET = Pattern.compile("%(?:[0-9a-f]{2}|[0-9A-F]{2})");
        private static final Pattern ALL_LEGAL_CHARS = Pattern.compile("[;/:?@&=+\\$,\\-_.!~*’()0-9A-Za-z]*");
        private static final Pattern ILLEGAL_NAME = Pattern.compile("(?:[^/]\\./|\\.[^/]*\\.|[^/]\\.$)");
        private static final BitSet ESCAPE_NOT_NEEDED = new BitSet(256);
        static {
            for (char c : ";/:?@&=+$,-_.!~*’()".toCharArray()) {
                ESCAPE_NOT_NEEDED.set(c);
            }
            for (char c = '0'; c <= 0xFF; c++) {
                if (Character.isAlphabetic(c) || Character.isDigit(c)) {
                    ESCAPE_NOT_NEEDED.set(c);
                }
            }
        }
        public static String escapeCorbanameUrlIfNecessary(String url) {
            final String methodName = "escapeCorbanameUrlIfNecessary(): ";
            if (url == null || !!!url.startsWith("corbaname:") || url.contains("\\"))
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
                if (!!!matcher.find()) {
                    // really, nothing needed replacing!
                    return url;
                }
            }

            StringBuilder sn = new StringBuilder();

            for (String n : stringifiedName.split("/", -1)) {
                // escape backslashes
                n = n.replaceAll("\\\\", "\\\\" + "\\\\");

                // escape dots
                n = n.replaceAll("\\.", "\\\\.");

                // 
                sn.append(n).append("/");
            }

            sn.setLength(sn.length() - 1);

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
            for (byte b : sn.toString().getBytes(StandardCharsets.UTF_8)) {
                if (ESCAPE_NOT_NEEDED.get(b)) {
                    escaped.append((char) b);
                } else {
                    escaped.append(String.format("%%%02x", 0xFF & b));
                }
            }

            if (tc.isDebugEnabled()) Tr.debug(tc, methodName + "escaped original url " + url + " to " + escaped);
            return escaped.toString();
        }

        
    }
    
    
}
