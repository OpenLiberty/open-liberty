/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.filter.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class represents an IP Address range and handles "comparison" operations against them. It is used
 * by ValueAddressRange to complete the abstraction.
 * 
 * An IPAddressRange is really just two IP addresses: a lower address and an upper address (which are numerically
 * related). IPV4 addresses are just a 32 bit number while IPV6 addresses are a 128bit number. To make this
 * code as simple and portable as possible, it lets the native JDK InetAddress class do the parsing of addresses
 * whenever possible. The native JDK of course understands IPV4 and V6 formats.
 * 
 * However, since this class supports *ranges* not single addresses, it has to parse the IP address to some degree
 * in order to expand the wildcards/ranges. For the purposes of this class, an IP address is a series of
 * decimal or hex digits separated by colon (:) or dot (.). Each element in this list is an address element.
 * If the element contains a special range character (* or []) then this class handles the interpretation and
 * expansion. Otherwise, that native JDK does the work. More concretely, an input address range is parsed and
 * expanded into two addresses (upper and lower) and the JDK is left to interpret that. Thus:
 * 192.168.1.3 becomes 192.168.1.3 and 192.168.1.3
 * 192.168.*.* becomes 192.168.0.0 and 192.168.255.255
 * 192.17.[3-9].* becomes 192.17.3.0 and 192.17.9.255
 * 1080:0:0:0:8:800:200C:417A becomes 1080:0:0:0:8:800:200C:417A and 1080:0:0:0:8:800:200C:417A
 * 1080:0:0:0:8:800:200C:* becomes 1080:0:0:0:8:800:200C:0 and 1080:0:0:0:8:800:200C:FFFF
 * 
 * Note: It is possible to specify IP addresses that aren't actually legal, but that's
 * irrelevant for our purposes. We are merely trying to match (presumed value) IP addresses against a range.
 * 
 * Essentially, we are just processing strings, not understanding the address. Note that we require that after
 * the first wildcard or range specifier that the remainder of the address be wildcards. Thus, a range must
 * consist of exactly *one* range, not mulitple ranges. Thus, for example, this is illegal:
 * 192.[2-3].[9-12].* - this is more than one IP address
 * 
 * Once the InetAddress objects are complete, we can support various comparison operations by directly looking
 * at their byte representations: either 4 bytes or 16 bytes.
 * 
 */
public class IPAddressRange {
    private static final TraceComponent tc = Tr.register(IPAddressRange.class);

    InetAddress ipHigher;
    InetAddress ipLower;
    String ipRange;

    /**
     * construct an IP address range from the input range string.
     * 
     * @param iprange
     * @throws FilterException
     */
    public IPAddressRange(String iprange) throws FilterException {
        super();

        ipRange = iprange;

        /*
         * We start with two empty strings to represent the upper and lower IP addresses. We then pull apart
         * the input range and copy it piece by piece into these two strings. We just don't copy the wildcard
         * or range values as those are valid IP addresses. Those we convert using the rules discussed above.
         */
        String lowerAddr = new String();
        String upperAddr = new String();
//        String lowerAddr = "";
//        String upperAddr = "";

        StringTokenizer tokens = new StringTokenizer(iprange, ".:", true);

        boolean foundRange = false;

        /*
         * IPV6 address elements use FFFF as their upper value as opposed to 255 for IPV4 addresses. We start
         * by assuming this is an IPV6 address and then switch to IPV4 when we find IPV4 delimeters (e.g., '.').
         * Note that an IP address can actually contain both IPV6 elements and IPV4 elements as specified by the
         * RFC. This code handles that natually.
         */
        String currentTop = "FFFF";
        while (tokens.hasMoreTokens()) {
            String ipPiece = tokens.nextToken();
            if (ipPiece.equals(":")) {
                //really nothing to do, just put on output strings
                //IPv6 address can start with a series of colons, so we just ignore if they do.
                upperAddr += ipPiece;
                lowerAddr += ipPiece;
                continue;
            }

            /*
             * ok, got a real ip element. This can't be a period since IPV4 addresses must start with a number.
             * So what's the delimeter following this number (might be null)
             */
            String delim;
            if (tokens.hasMoreElements())
                delim = tokens.nextToken(); //more to do next time
            else
                delim = ""; //we are at the end

            //get rid of extra spaces just in case of human input error
            ipPiece = ipPiece.trim();

            /*
             * if we are really processing our first IPv4 string portion, switch
             * the currentTop to 255. After the first dot the remained of the address must be IPV4 format so
             * 255 is now the final upper bound.
             */
            if (".".equals(delim)) {
                currentTop = "255";
            }

            if (foundRange) {
                /*
                 * Everything after the first range *MUST* be a wildcard, so need to enforce that.
                 * Just count tokens and expand.
                 */
                if (!ipPiece.equals("*")) {
                    String msg = TraceNLS.getFormattedMessage(this.getClass(),
                                                              TraceConstants.MESSAGE_BUNDLE,
                                                              "AUTH_FILTER_MALFORMED_IP_RANGE",
                                                              new Object[] { ipPiece },
                                                              "CWWKS1754E: Malformed IP range specified. Found {0} rather than a wildcard.");

                    throw new FilterException(msg);
                }

                /*
                 * Since it's a wildard, just add the maximum to the upper and zero to the lower. Wildcard is
                 * basically the same as [0-255] for IPV4 addresses.
                 */

                upperAddr += currentTop;
                lowerAddr += "0";

            } else if ((ipPiece.startsWith("[")) && (ipPiece.endsWith("]"))) {
                //it's a range [N-N]

                //get first part of range and put on lower ip
                int dash = ipPiece.indexOf('-');
                String startStr = ipPiece.substring(1, dash);
                lowerAddr += startStr;

                //get second part of range and put on upper ip
                int bracket = ipPiece.lastIndexOf(']');
                String lastStr = ipPiece.substring(dash + 1, bracket);
                upperAddr += lastStr;

                foundRange = true;
            } else if (ipPiece.equals("*")) {
                //whole range
                upperAddr += currentTop;
                lowerAddr += "0";
                foundRange = true;
            } else {
                //just a number so just add to the two IPs
                upperAddr += ipPiece;
                lowerAddr += ipPiece;
            }

            //we've put the numbers onto the two IPs, now add the delimiter before we go around again.
            upperAddr += delim;
            lowerAddr += delim;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "upperAddr is " + upperAddr);
                Tr.debug(tc, "lowerAddr is " + lowerAddr);
            }
        }

        /*
         * we've now created the two IPV4/IPV6 address strings that represent the range. Now, just make them
         * real IP addresses so I can compare them later.
         */
        try {
            ipHigher = InetAddress.getByName(upperAddr);
        } catch (UnknownHostException e) {
            String msg = TraceNLS.getFormattedMessage(this.getClass(),
                                                      TraceConstants.MESSAGE_BUNDLE,
                                                      "AUTH_FILTER_MALFORMED_IP_RANGE",
                                                      new Object[] { upperAddr },
                                                      "CWWKS1755E: Unknown host exception raised for IP address {0}.");

            throw new FilterException(msg, e);
        }
        try {
            ipLower = InetAddress.getByName(lowerAddr);
        } catch (UnknownHostException e) {
            String msg = TraceNLS.getFormattedMessage(this.getClass(),
                                                      TraceConstants.MESSAGE_BUNDLE,
                                                      "AUTH_FILTER_MALFORMED_IP_RANGE",
                                                      new Object[] { lowerAddr },
                                                      "CWWKS1755E: Unknown host exception raised for IP address {0}.");

            throw new FilterException(msg, e);
        }
    }

    /**
     * Is the given ip address in this range? Basically check to see if it is below ipHigher and above ipLower.
     * To simplify the logic we actually check to see if it is *not* above higher or below lower thus avoiding
     * another check for equality.
     * 
     * @param ip
     * @return
     */
    public boolean inRange(InetAddress ip) {
        if (greaterThan(ip, ipHigher))
            return false;
        else if (lessThan(ip, ipLower))
            return false;
        else
            return true;
    }

    /**
     * Is the given ip address numericaly above the address range?
     * Is it above ipHigher?
     */
    public boolean aboveRange(InetAddress ip) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "aboveRange, ip is " + ip);
            Tr.debug(tc, "aboveRange, ip is " + ip);
        }
        return greaterThan(ip, ipHigher);
    }

    /**
     * Is the given ip address numerically below the address range?
     * Is it below ipLower?
     * 
     * @param ip
     * @return
     */
    public boolean belowRange(InetAddress ip) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "belowRange, ip is " + ip);
            Tr.debug(tc, "belowRange, ipLower is " + ipLower);
        }
        return lessThan(ip, ipLower);
    }

    /**
     * Numerically compare two IP addresses. Is a1 > a2?
     * 
     * @param a1
     * @param a2
     * @return
     */
    static public boolean greaterThan(InetAddress a1, InetAddress a2) {
        return (compare(a1, a2) > 0);
    }

    /**
     * Numerically compare two IP addresses. Is a1 < a2?
     * 
     * @param a1
     * @param a2
     * @return
     */
    static public boolean lessThan(InetAddress a1, InetAddress a2) {
        return (compare(a1, a2) < 0);
    }

    /**
     * The key comparison function. This method is modeled about the Java string comparitor. It takes two IP
     * addresses and converts both to byte arrays. It then comparisons the bytes one at a time (numerically).
     * If the bytes are the same, we continue. If they differ, we return 1 indicated that a1 is larger and -1
     * indicating that a2 is larger. If we run out of bytes, we return 0 indicating they are equal.
     * 
     * Simple enough, but we have to handle a special case: this is a numeric comparison of two possibly
     * different length addresses. Thus, a 4 byte IPv4 address could be "the same" as a 16 byte IPv6 address
     * if the 12 extra bytes are all zeros. So, we have to skip over those as needed.
     * 
     * 
     * @param a1
     * @param a2
     * @return
     */
    static public int compare(InetAddress a1, InetAddress a2) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "compare, a1 is " + a1);
            Tr.debug(tc, "compare, a2 is " + a2);
        }
        byte[] byte1 = a1.getAddress();
        byte[] byte2 = a2.getAddress();

        int len;
        int base1 = 0;
        int base2 = 0;

        /**
         * if the byte arrays aren't the same length, see if the extra bytes are all zeros. If they are, we can
         * just skip over them and compare the remaining bytes. If any extra byte is not zero, then the
         * corresponding byte array must be mumerically larger so return that fact.
         */
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "compare, byte1.length is " + byte1.length);
            Tr.debug(tc, "compare, byte2.length is " + byte2.length);
        }
        if (byte1.length > byte2.length) {
            //byte1 is longer
            len = byte2.length;
            base1 = byte1.length - len;

            //if byte starts with zeros, continue. If it doesn't, return 1 indicating byte1 is numerically larger
            if (!isZeros(byte1, byte1.length - len))
                return 1;
        } else if (byte1.length < byte2.length) {
            //byte2 is longer
            len = byte1.length;
            base2 = byte2.length - len;

            //if byte starts with zeros, continue. If it doesn't, return 1 indicating byte2 is numerically larger
            if (!isZeros(byte2, byte2.length - len))
                return -1;
        } else {
            len = byte1.length; //they are the same
        }

        /*
         * now that we've skipped over the extra bytes (all zero), compare the rest of the array byte by byte.
         */
        for (int i = 0; i < len; base1++, base2++, i++) {

            //equal??
            if (byte1[base1] == byte2[base2])
                continue;

            /*
             * We have to be careful here to perform unsigned comparisons. Unfortunately, Java interpretes
             * high bit bytes as negative which messes thing up. So, we convert them to integers and then
             * add 256 if needed. Yuck.
             */

            int b1 = byte1[base1];
            if (b1 < 0)
                b1 += 256;
            int b2 = byte2[base2];
            if (b2 < 0)
                b2 += 256;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "compare, b1 is " + b1);
                Tr.debug(tc, "compare, b2 is " + b2);
            }
            if (b1 > b2)
                return 1;
            else
                return -1;
        }
        return 0;
    }

    static private boolean isZeros(byte[] bytes, int len) {
        for (int i = 0; i < len; i++) {
            if (bytes[i] != 0)
                return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return ipRange;
    }
}
