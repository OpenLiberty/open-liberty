/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.ldap;

import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.wim.util.UniqueNameHelper;
import com.ibm.wsspi.security.wim.exception.CertificateMapperException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.exception.WIMSystemException;
import com.ibm.wsspi.security.wim.model.Entity;

public class LdapHelper {

    /**
     * Register the class to trace service.
     */
    private final static TraceComponent tc = Tr.register(LdapHelper.class);

    static private String sdformatMillisec = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private static SoftReference<Map<?, ?>> softDnCache = new SoftReference<Map<?, ?>>(new ConcurrentHashMap<String, Map<String, String>>());

    @Trivial
    public static String getOctetString(byte[] bytesArr) {
        StringBuffer retVal = new StringBuffer();
        String dblByte = "";

        for (int i = 0; i < bytesArr.length; i++) {
            dblByte = Integer.toHexString(bytesArr[i] & 0xff);
            if (dblByte.length() == 1) {
                dblByte = "0" + dblByte;
            }
            retVal.append(dblByte);
        }
        return retVal.toString();
    }

    /**
     * Returns a date String from the specified Date object. It is expected to be called before passing the date to SDO set method.
     *
     * For example:
     * date = new Date();
     * entity.set("registerDate", SDOHelper.getDateString(date));
     *
     * Input: 2005-05-04T09:34:18.444-0400
     * Output: 2005-05-04T09:34:18.444-04:00
     *
     * @param date The Date object.
     * @return A date String that converted from Date object.
     */
    public static String getDateString(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(sdformatMillisec);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        StringBuffer dateBuffer = new StringBuffer(sdf.format(date));
        // above string is in this format
        //    2005-05-04T09:34:18.444-0400
        // convert it to
        //    2005-05-04T09:34:18.444-04:00
        dateBuffer.insert(dateBuffer.length() - 2, ":");
        return dateBuffer.toString();
    }

    /**
     * Called by LDAP search method. to get the full Distinguished Name from search result and search base.
     * Some LDAP server returns the full DN in search result, some only returns RDN. The method is used to
     * get the full DN in both case.
     *
     * @param DN The Distinguished Name returned by the search result, may be a RDN.
     * @param searchRoot the Distinguished Name of the search base.
     *
     * @return The full Distinguished Name.
     */
    public static String prepareDN(String DN, String searchRoot) {
        if (DN == null || DN.trim().length() == 0) {
            return searchRoot;
        }
        // unescape double blackslashes
        DN = unescapeDoubleBackslash(DN);
        DN = unescapeSingleQuote(DN); // fix login failure when single quote (') is in userid
        DN = UniqueNameHelper.unescapeSpaces(DN);

        //process special character enclosing double quotes
        int length = DN.length();
        if ((DN.charAt(0) == '\"') && (DN.charAt(length - 1) == '\"')) {
            DN = DN.substring(1, length - 1);
        }

        // Remove server URL suffix if the DN is referral from another LDAP server.
        if (DN.startsWith("/")) {
            int pos1 = DN.indexOf(':');
            if (pos1 > -1) {
                int pos2 = DN.indexOf('/', pos1);
                if (pos2 > 0)
                    DN = DN.substring(pos2 + 1);
            }

        } else if (DN.toLowerCase().startsWith("ldap://")) {
            boolean parsed = false;

            try {
                // Use the Ldap URL parser to ensure that %xx gets decoded
                LdapURL ldapURL = new LdapURL(DN);
                if (ldapURL.parsedOK()) {
                    DN = ldapURL.get_dn();
                    parsed = true;
                }
            } catch (Exception excp) {
            }

            if (!parsed) {
                int pos1 = DN.indexOf(':', "ldap://".length());
                if (pos1 > 0) {
                    int pos2 = DN.indexOf("/", pos1);
                    if (pos2 > 0)
                        DN = DN.substring(pos2 + 1);
                }
            }
        } else if (DN.toLowerCase().startsWith("ldaps://")) {
            boolean parsed = false;

            try {
                // Use the Ldap URL parser to ensure that %xx gets decoded
                LdapURL ldapURL = new LdapURL(DN);
                if (ldapURL.parsedOK()) {
                    DN = ldapURL.get_dn();
                    parsed = true;
                }
            } catch (Exception excp) {
            }

            if (!parsed) {
                int pos1 = DN.indexOf(':', "ldaps://".length());
                if (pos1 > 0) {
                    int pos2 = DN.indexOf("/", pos1);
                    if (pos2 > 0)
                        DN = DN.substring(pos2 + 1);
                }
            }
        }
        if (searchRoot == null || searchRoot.trim().length() == 0) {
            return DN;
        }
        StringTokenizer stDN = new StringTokenizer(DN, LdapConstants.LDAP_DN_SEPARATOR);
        StringTokenizer stSearchRoot = new StringTokenizer(searchRoot, LdapConstants.LDAP_DN_SEPARATOR);
        String lastDNToken = null;
        String lastSearchRootToken = null;

        while (stDN.hasMoreTokens()) {
            lastDNToken = stDN.nextToken();
        }
        while (stSearchRoot.hasMoreTokens()) {
            lastSearchRootToken = stSearchRoot.nextToken();
        }
        if (lastDNToken != null)
            lastDNToken = lastDNToken.trim();
        else
            lastDNToken = "";
        if (lastSearchRootToken != null) {
            lastSearchRootToken = lastSearchRootToken.trim();
        } else {
            lastSearchRootToken = "";
        }
        if (!lastDNToken.equalsIgnoreCase(lastSearchRootToken))
            if (DN.length() > 0) {
                DN = DN + LdapConstants.LDAP_DN_SEPARATOR + searchRoot;
            } else
                DN = searchRoot;
        return DN;

    }

    public static String unescapeDoubleBackslash(String in) {
        char[] chars = in.toCharArray();
        int end = chars.length;
        StringBuffer out = new StringBuffer(in.length());
        for (int i = 0; i < end; i++) {
            if ((chars[i] == '\\') && (i + 1 < end) && chars[i + 1] == '\\') {
                ++i; // skip backslash

            }
            out.append(chars[i]);
        }

        return new String(out);
    }

    public static String unescapeSingleQuote(String in) {
        char[] chars = in.toCharArray();
        int end = chars.length;
        StringBuffer out = new StringBuffer(in.length());
        for (int i = 0; i < end; i++) {
            if ((chars[i] == '\\') && (i + 1 < end) && chars[i + 1] == '\'') {
                ++i; // skip backslash

            }
            out.append(chars[i]);
        }

        return new String(out);
    }

    /**
     * Return an array of RDN attributes of the given DN in lower case form.
     *
     * @param dn
     * @return
     */
    public static String[] getRDNAttributes(String dn) {
        String rdnstr = getRDN(dn);
        StringTokenizer st = new StringTokenizer(rdnstr.toLowerCase(), "+");
        List<String> list = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String rdn = st.nextToken();
            int index = rdn.indexOf('=');
            if (index > -1) {
                list.add(rdn.substring(0, index));
            }
        }
        return list.toArray(new String[0]);
    }

    /**
     * Gets the RDN from the specified DN
     * For example "uid=persona" will be returned for given "uid=persona,cn=users,dc=yourco,dc=com".
     *
     * @param DN The DN
     * @return The RDN
     */
    public static String getRDN(String DN) {
        if (DN == null || DN.trim().length() == 0) {
            return DN;
        }
        String RDN = null;
        try {
            LdapName name = new LdapName(DN);
            if (name.size() == 0) {
                return DN;
            }
            RDN = name.get(name.size() - 1);
        } catch (InvalidNameException e) {
            e.getMessage();
            DN = DN.trim();
            int pos1 = DN.indexOf(',');
            while (DN.charAt(pos1 - 1) == '\\') {
                pos1 = DN.indexOf(',', pos1 + 1);
            }
            if (pos1 > -1) {
                RDN = DN.substring(0, pos1).trim();
            } else {
                RDN = DN;
            }
        }
        return RDN;
    }

    public static String replaceRDN(String dn, String[] rdnAttrTypes, String[] rdnAttrValues) {
        if (rdnAttrTypes == null || rdnAttrValues == null || rdnAttrTypes.length != rdnAttrValues.length
            || rdnAttrTypes.length == 0) {
            return dn;
        }

        /*
         * Strip the parent DN from the end of the DN. We want just the leading RDN.
         */
        String parentDN = getParentDN(dn);
        if (!parentDN.isEmpty()) {
            dn = dn.replace("," + parentDN, "");
        }

        /*
         * Retrieve the values for the leading RDN.
         */
        String[] oldRDNValues = getRDNValues(dn);

        /*
         * Iterate over all the RDN attribute types.
         */
        StringBuffer rdn = new StringBuffer();
        for (int i = 0; i < rdnAttrTypes.length; i++) {
            /*
             * Multi-attribute RDNs require a '+' between each attribute type and value.
             */
            if (i != 0) {
                rdn.append("+");
            }

            /*
             * If there is a new value, replace the value of the RDN.
             */
            String newRDNValue = rdnAttrValues[i];
            if (newRDNValue == null) {
                newRDNValue = oldRDNValues[i];
            }
            rdn.append(rdnAttrTypes[i] + "=" + Rdn.escapeValue(newRDNValue));
        }

        /*
         * Append the parent DN back onto the end of the DN.
         */
        if (parentDN.isEmpty()) {
            dn = rdn.toString();
        } else {
            dn = rdn.append("," + parentDN).toString();
        }

        return dn;
    }

    public static String getParentDN(String DN) {
        String parentDN = null;
        if (DN == null || DN.trim().length() == 0) {
            parentDN = "";
        }
        try {
            LdapName name = new LdapName(DN);
            if (name.size() == 0) {
                parentDN = "";
            }
            name.remove(name.size() - 1);
            parentDN = name.toString();
        } catch (InvalidNameException e) {
            e.getMessage();
            DN = DN.trim();
            int pos1 = DN.indexOf(',');
            if (DN.charAt(pos1 - 1) == '\\') {
                pos1 = DN.indexOf(pos1, ',');
            }
            if (pos1 > -1) {
                parentDN = DN.substring(pos1 + 1).trim();
            } else {
                parentDN = "";
            }
        }
        return parentDN;
    }

    public static String[] getRDNValues(String rdnStr) {
        StringTokenizer st = new StringTokenizer(rdnStr.toLowerCase(), "+");
        List<String> list = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String rdn = st.nextToken();
            int index = rdn.indexOf('=');
            if (index > -1) {
                list.add(rdn.substring(index + 1));
            }
        }
        return list.toArray(new String[0]);
    }

    public static boolean isUnderBases(String dn, String... bases) {
        return UniqueNameHelper.isDNUnderBaseEntry(dn, bases);
    }

    /**
     * Validates and returns the valid and formatted LDAP Distinguished Name (DN) from the given DN.
     * Extra spaces will be removed from the DN during formatting.
     * If the specified DN does not satisfy the LDAP DN syntax rule, null will be returned.
     *
     * @param dn The DN to be formatted.
     *
     * @return The formatted DN. null will be returned if the specified DN is invalid.
     */
    @FFDCIgnore(InvalidNameException.class)
    @SuppressWarnings("unchecked")
    public static String getValidDN(String dn) {
        Map<String, Map<String, String>> dnCache = null;
        if (dn == null) {
            return null;
        }

        if (softDnCache.get() == null) {
            softDnCache = new SoftReference<Map<?, ?>>(new ConcurrentHashMap<String, Map<String, String>>());
        }

        dnCache = (Map<String, Map<String, String>>) softDnCache.get();

        //Get the current domain name
        // TODO:: Domain name is 'default'
        // String sDomainName = DomainManagerUtils.getDomainName();
        String sDomainName = "default";

        //Get the dnCache for the current domain
        if (dnCache != null && dnCache.get(sDomainName) == null) {
            dnCache.put(sDomainName, new ConcurrentHashMap<String, String>(256));
        }
        String result = null;
        if (dnCache != null)
            result = dnCache.get(sDomainName).get(dn);

        if (result == null) {
            try {
                result = new LdapName(dn).toString();
                dnCache.get(sDomainName).put(dn, result);
            } catch (InvalidNameException e) {
                result = null;
            }
        }
        return result;
    }

    /**
     * Gets the LdapURL array from the given dynamic member attribute.
     *
     * @param attr The dynamic member attribute.
     * @return The LdapURL array.
     * @throws WIMException
     */
    static public LdapURL[] getLdapURLs(Attribute attr) throws WIMException {
        final String METHODNAME = "getLdapURLs";

        LdapURL[] ldapURLs = new LdapURL[0];
        if (attr != null) {
            List<LdapURL> ldapURLList = new ArrayList<LdapURL>(attr.size());
            try {
                for (NamingEnumeration<?> enu = attr.getAll(); enu.hasMoreElements();) {
                    LdapURL ldapURL = new LdapURL((String) (enu.nextElement()));
                    if (ldapURL.parsedOK()) {
                        ldapURLList.add(ldapURL);
                    } else {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, METHODNAME + " Member URL query: " + ldapURL.get_url() + " is invalid and ingored.");
                        }
                    }
                }
            } catch (NamingException e) {
                throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(
                                                                                              tc,
                                                                                              WIMMessageKey.NAMING_EXCEPTION,
                                                                                              WIMMessageHelper.generateMsgParms(e.toString(true))));
            }
            ldapURLs = ldapURLList.toArray(ldapURLs);
        }
        return ldapURLs;
    }

    public static boolean isEntityTypeInList(String entityType, List<String> entityTypes) throws WIMException {
        if (entityTypes != null && entityTypes.size() > 0) {

            for (int i = 0; i < entityTypes.size(); i++) {

                String thisType = entityTypes.get(i);
                HashSet<?> hs = Entity.getSubEntityTypes(thisType);
                if (thisType.equals(entityType) || (hs != null && hs.contains(entityType))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getDNFromAttributes(Attributes attrs) throws WIMSystemException {
        try {
            Attribute attr = attrs.remove(LdapConstants.LDAP_DN);
            if (attr != null) {
                return (String) attr.get();
            } else {
                return null;
            }
        } catch (NamingException e) {
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(
                                                                                          tc,
                                                                                          WIMMessageKey.NAMING_EXCEPTION,
                                                                                          WIMMessageHelper.generateMsgParms(e.toString(true))));
        }
    }

    public static String encodeAttribute(String attributeValue, String encoding) {
        int i = 0;
        char[] str = attributeValue.toCharArray();
        int size = str.length;
        StringBuilder result = new StringBuilder();
        while (i < size) {
            char c = str[i];
            // If the character cannot be converted transitively into both cases then they need to be encoded
            if (Character.toLowerCase(c) != Character.toLowerCase(Character.toUpperCase(c))) {
                byte[] bytes = null;
                try {
                    bytes = ("" + c).getBytes(encoding);
                } catch (UnsupportedEncodingException e) {
                }
                if (bytes != null)
                    for (int j = 0; j < bytes.length; j++) {
                        byte b = bytes[j];
                        result.append("\\");
                        String hexString = Integer.toHexString(b);
                        result.append(hexString.substring(hexString.length() - 2));
                    }
            } else
                result.append(c);
            i++;
        }

        return result.toString();
    }

    public static Attribute getIngoreCaseAttribute(Attributes attrs, String attrName) {
        if (attrs == null || attrName == null) {
            return null;
        }
        for (NamingEnumeration<?> neu = attrs.getIDs(); neu.hasMoreElements();) {
            String cachedAttr = (String) neu.nextElement();
            boolean findAttr = false;
            if (attrName.equalsIgnoreCase(cachedAttr)) {
                findAttr = true;
            } else {
                int pos = cachedAttr.indexOf(";");
                if (pos > 0 && attrName.equalsIgnoreCase(cachedAttr.substring(0, pos))) {
                    findAttr = true;
                }
            }
            if (findAttr) {
                return attrs.get(cachedAttr);
            }
        }
        return null;
    }

    /**
     * Helper method used by trace to print out detail informaiton about the LDAP <code>javax.naming.directory.SearchControl</code>.
     *
     * @param searchControls The <code>SearchControl</code> object needs to print out.
     *
     * @return The print out string of the <code>SearchControl</code> object.
     */
    public static String printSearchControls(SearchControls searchControls) {
        StringBuffer result = new StringBuffer();
        result.append("[searchScope: ").append(searchControls.getSearchScope());
        result.append(", timeLimit: ").append(searchControls.getTimeLimit());
        result.append(", countLimit: ").append(searchControls.getCountLimit());
        result.append(", returningObjFlag: ").append(searchControls.getReturningObjFlag());
        result.append(", returningAttributes: ").append(
                                                        WIMTraceHelper.printObjectArray(searchControls.getReturningAttributes())).append("]");
        return result.toString();

    }

    /**
     * Get a unique key for a certificate.
     */
    public static String getUniqueKey(X509Certificate cert) {
        // TBD - Would like to use public key instead of subject name, but
        // cert.getPublicKey().getEncoded() appears to return different
        // values for each call, using the same certificate??
        StringBuffer key = new StringBuffer("subjectDN:");
        key.append(cert.getSubjectX500Principal().getName()).append("issuerDN:").append(cert.getIssuerX500Principal().getName());
        // TODO::
        // return Base64Coder.base64Encode(getDigest(key.toString()));
        return null;
    }

    /**
     * Given 'input', return the digest version.
     */
    static String getDNSubField(String varName, String DN) throws CertificateMapperException {
        if (varName.equals("DN")) {
            return DN; // return the whole DN
        }
        // Parse the DN looking for 'varName'
        StringTokenizer st = new StringTokenizer(DN);
        for (;;) {
            String name, value;
            try {
                name = st.nextToken(",= ");
                value = st.nextToken(",");
                if (value != null) {
                    value = value.substring(1);
                }
            } catch (NoSuchElementException e) {
                e.getMessage();
                break;
            }
            if (name.equals(varName)) {
                return value;
            }
        }

        throw new CertificateMapperException(WIMMessageKey.UNKNOWN_DN_FIELD, Tr.formatMessage(
                                                                                              tc,
                                                                                              WIMMessageKey.UNKNOWN_DN_FIELD,
                                                                                              WIMMessageHelper.generateMsgParms(varName)));
    }

    static public boolean containIgnorecaseValue(Attribute attr, String value) throws WIMSystemException {
        if (attr == null || value == null) {
            return false;
        }
        try {
            for (NamingEnumeration<?> neu = attr.getAll(); neu.hasMoreElements();) {
                String thisValue = (String) neu.nextElement();
                if (value.equalsIgnoreCase(thisValue)) {
                    return true;
                }
            }
        } catch (NamingException e) {
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(
                                                                                          tc,
                                                                                          WIMMessageKey.NAMING_EXCEPTION,
                                                                                          WIMMessageHelper.generateMsgParms(e.toString(true))));
        }
        return false;
    }

    /**
     * Handle special character '?' (\u00df) which will be transformed to "SS" during String.toUpperCase().
     *
     * @param dn
     * @return
     */
    public static String toUpperCase(String dn) {
        if (dn == null)
            return null;

        String newDN = null;
        if (dn.indexOf('\u00df') != -1) {
            // convert each character in DN to upper case except '?'(\u00df)
            char[] chars = new char[dn.length()];
            for (int index = 0; index < dn.length(); index++) {
                char c = dn.charAt(index);
                if (c == '\u00df') {
                    chars[index] = c;
                } else {
                    chars[index] = Character.toUpperCase(c);
                }
            }
            newDN = new String(chars);
        } else {
            newDN = dn.toUpperCase();
        }
        return newDN;
    }

    public static Object getStringLdapValue(Object value, LdapAttribute ldapAttr, String ldapType) throws WIMSystemException {
        Object ldapValue = null;
        String strValue = (String) value;
        String syntax = LdapConstants.LDAP_ATTR_SYNTAX_STRING;
        if (ldapAttr != null) {
            syntax = ldapAttr.getSyntax();
        }

        if (LdapConstants.LDAP_ATTR_SYNTAX_UNICODEPWD.equalsIgnoreCase(syntax)) {
            // The the LDAP attribute is unicodePwd, need to convert it to special byte array format.s
            ldapValue = encodePassword(strValue);
        } else {
            if (LdapConstants.LDAP_ATTR_SYNTAX_OCTETSTRING.equalsIgnoreCase(syntax)) {
                try {
                    ldapValue = getOctetString(strValue);
                } catch (NumberFormatException nfe) {
                    throw new WIMSystemException(WIMMessageKey.SYSTEM_EXCEPTION, Tr.formatMessage(
                                                                                                  tc,
                                                                                                  WIMMessageKey.SYSTEM_EXCEPTION,
                                                                                                  WIMMessageHelper.generateMsgParms(nfe.getMessage())));
                }
            } else {
                // If LDAP data type is not specified or is String, return String value.
                ldapValue = strValue;
            }
        }
        return ldapValue;
    }

    /**
     * Encode the input password from String format to special byte array format so that it can be stored in
     * attribute <code>unicodePwd</code> in Active Directory.
     *
     * @param password The password to be encoded.
     *
     * @return The encoded byte array which can be stored in <code>unicodePwd</code> attribute in Active Directory.
     */
    public static byte[] encodePassword(String password) throws WIMSystemException {
        try {
            return ("\"" + password + "\"").getBytes("UTF-16LE");
        } catch (Exception e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.GENERIC, WIMMessageHelper.generateMsgParms(e.getMessage()));
            throw new WIMSystemException(WIMMessageKey.GENERIC, msg, e);
        }
    }

    public static byte[] getOctetString(String hexStr) {
        byte[] byteArr = new byte[hexStr.length() / 2];
        int count = 0;
        for (int i = 0; i < hexStr.length() - 1; i = i + 2) {
            String retVal = hexStr.substring(i, i + 2);
            int intVal = Integer.parseInt(retVal, 16);
            byteArr[count] = (byte) (intVal);
            count++;
        }
        return byteArr;
    }

    public static Object getDateLdapValue(Object value, LdapAttribute ldapAttr, String ldapType) throws WIMSystemException {
        try {
            String vStr = value.toString();
            Date date = null;
            boolean isUTCDate = (vStr.indexOf("Z") != -1);
            boolean containsMillisec = (vStr.indexOf(".") != -1);
            String ldapValue = null;

            if (isUTCDate) {
                // For remote mode calls, the date set by SDOHelper.getDateString()
                // is changed to the EMF UTC format.  This occurs because the datagraph
                // is serialized/de-serialized as it's transmitted from client to server
                if (containsMillisec)
                    date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(vStr);
                else
                    date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(vStr);
            } else {
                // Use the format for SCIM
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd't'HH:mm:ss");
                date = format.parse(vStr);

                // Normalize the date to UTC - PK89272
                // Forces consistent date format to be stored in repository
                // This will help in search queries that involve dates (ex. activeDate > 20090630151859.000734Z)
                date = convertDatetoGMT(date);
                isUTCDate = true;
            }

            if (LdapConstants.IDS_LDAP_SERVER.equalsIgnoreCase(ldapType)) {
                if (isUTCDate) {
                    ldapValue = new SimpleDateFormat("yyyyMMddHHmmss.SSSSSS'Z'").format(date);
                } else {
                    ldapValue = new SimpleDateFormat("yyyyMMddHHmmss.SSSSSSZ").format(date);
                }
            } else if (LdapConstants.SUN_LDAP_SERVER.equalsIgnoreCase(ldapType)
                       || LdapConstants.DOMINO_LDAP_SERVER.equalsIgnoreCase(ldapType)
                       || LdapConstants.NOVELL_LDAP_SERVER.equalsIgnoreCase(ldapType)) {
                if (isUTCDate) {
                    ldapValue = new SimpleDateFormat("yyyyMMddHHmmss'Z'").format(date);
                } else {
                    ldapValue = new SimpleDateFormat("yyyyMMddHHmmssZ").format(date);
                }
            } else {
                if (isUTCDate) {
                    return new SimpleDateFormat("yyyyMMddHHmmss.S'Z'").format(date);
                } else {
                    return new SimpleDateFormat("yyyyMMddHHmmss.SZ").format(date);
                }
            }
            return ldapValue;
        } catch (java.text.ParseException e) {
            throw new WIMSystemException(WIMMessageKey.SYSTEM_EXCEPTION, Tr.formatMessage(
                                                                                          tc,
                                                                                          WIMMessageKey.SYSTEM_EXCEPTION,
                                                                                          WIMMessageHelper.generateMsgParms(e.toString())));
        }
    }

    private static Date convertDatetoGMT(Date date) {
        TimeZone tz = TimeZone.getDefault();
        Date ret = new Date(date.getTime() - tz.getRawOffset());

        // if we are now in DST, back off by the delta.
        // Note that we are checking the GMT date, this is the KEY.
        if (tz.inDaylightTime(ret)) {
            Date dstDate = new Date(ret.getTime() - tz.getDSTSavings());

            // check to make sure we have not crossed back into standard time
            // this happens when we are on the cusp of DST (7pm the day before the change for PDT)
            if (tz.inDaylightTime(dstDate))
                ret = dstDate;
        }

        return ret;
    }

    public static Object getIntLdapValue(Object value, LdapAttribute ldapAttr, String ldapType) {
        Object ldapValue = null;
        if (value instanceof Integer) {
            ldapValue = value.toString();
        }
        ldapValue = value.toString();
        return ldapValue;
    }

    /*
     * Parse the map descriptor.
     * Return an array of tokens.
     * Copy from com.ibm.ws.security.registry.ldap.CertificateMapper
     */
    public static String[] parseFilterDescriptor(String mapDesc) throws CertificateMapperException {
        if (mapDesc == null || mapDesc.isEmpty()) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.INVALID_CERTIFICATE_FILTER, mapDesc);
            throw new CertificateMapperException(WIMMessageKey.INVALID_CERTIFICATE_FILTER, msg);
        }

        int prev_idx, cur_idx, end_idx;
        ArrayList<String> list = new ArrayList<String>();
        for (prev_idx = cur_idx = 0, end_idx = mapDesc.length(); cur_idx < end_idx; prev_idx = cur_idx) {
            boolean isSquareBrace = false;
            if (mapDesc.contains("$[")) {
                isSquareBrace = true;
                cur_idx = mapDesc.indexOf("$[", prev_idx);
            } else {
                cur_idx = mapDesc.indexOf("${", prev_idx);
            }
            if (cur_idx == -1) {
                if (prev_idx < end_idx) {
                    list.add(mapDesc.substring(prev_idx));
                }
                break;
            }
            if (prev_idx < cur_idx) {
                list.add(mapDesc.substring(prev_idx, cur_idx));
            }
            prev_idx = cur_idx;
            if (isSquareBrace) {
                cur_idx = mapDesc.indexOf("]", prev_idx);
            } else {
                cur_idx = mapDesc.indexOf("}", prev_idx);
            }
            if (cur_idx == -1) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.INVALID_CERTIFICATE_FILTER, mapDesc);
                throw new CertificateMapperException(WIMMessageKey.INVALID_CERTIFICATE_FILTER, msg);
            }
            cur_idx++;
            list.add(mapDesc.substring(prev_idx, cur_idx));
        }
        String[] mapDescEles = new String[list.size()];
        for (int idx = 0; idx < list.size(); idx++) {
            mapDescEles[idx] = list.get(idx);
        }
        return mapDescEles;
    }

    /**
     * Gets the short form the scope from the string form of the scope
     *
     * @param scope A string represent a scope ('direct', 'nested' and 'all').
     * @return The short represent a scope (0, 1, 2)
     */
    public static short getMembershipScope(String scope) {
        if (scope != null) {
            scope = scope.trim();
            if (LdapConstants.LDAP_DIRECT_GROUP_MEMBERSHIP_STRING.equalsIgnoreCase(scope)) {
                return LdapConstants.LDAP_DIRECT_GROUP_MEMBERSHIP;
            } else if (LdapConstants.LDAP_NESTED_GROUP_MEMBERSHIP_STRING.equalsIgnoreCase(scope)) {
                return LdapConstants.LDAP_NESTED_GROUP_MEMBERSHIP;
            } else if (LdapConstants.LDAP_ALL_GROUP_MEMBERSHIP_STRING.equalsIgnoreCase(scope)) {
                return LdapConstants.LDAP_ALL_GROUP_MEMBERSHIP;
            } else {
                return LdapConstants.LDAP_DIRECT_GROUP_MEMBERSHIP;
            }
        } else {
            return LdapConstants.LDAP_DIRECT_GROUP_MEMBERSHIP;
        }
    }

    /**
     * Returns array of RDN attribute types from the given RDN string.
     * RDN string may contain multiple RDNs separated by "+".
     * For example, "uid=persona+mail=persona@mail.com" string will return [uid=persona, mail=persona@mail.com].
     *
     * @param rdnStr the RDN string
     * @return the array of separated RDNs.
     */
    public static String[] getRDNs(String rdnStr) {
        return UniqueNameHelper.getRDNs(rdnStr);
    }

    /**
     * Whether the specified attribute name is contained in the attributes.
     *
     * @param attrName The name of the attribute
     * @param attrs The Attributes object
     * @return true if the Attributes contain the attribute name (case-insensitive); false otherwise
     */
    public static boolean inAttributes(String attrName, Attributes attrs) {
        for (NamingEnumeration<String> neu = attrs.getIDs(); neu.hasMoreElements();) {
            String attrId = neu.nextElement();
            if (attrId.equalsIgnoreCase(attrName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clone the specified attribute with a new name.
     *
     * @param newAttrName The new name of the attribute
     * @param attr The Attribute object to be cloned.
     * @return The cloned Attribute object with the new name.
     * @throws WIMSystemException
     */
    public static Attribute cloneAttribute(String newAttrName, Attribute attr) throws WIMSystemException {
        Attribute newAttr = new BasicAttribute(newAttrName);
        try {
            for (NamingEnumeration<?> neu = attr.getAll(); neu.hasMoreElements();) {
                newAttr.add(neu.nextElement());
            }
        } catch (NamingException e) {
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true))));
        }
        return newAttr;
    }

    /**
     * Convert the byte array to Active Directory GUID format.
     *
     * @return
     */
    public static String convertToDashedString(byte[] objectGUID) {
        StringBuilder displayStr = new StringBuilder();

        displayStr.append(prefixZeros(objectGUID[3] & 0xFF));
        displayStr.append(prefixZeros(objectGUID[2] & 0xFF));
        displayStr.append(prefixZeros(objectGUID[1] & 0xFF));
        displayStr.append(prefixZeros(objectGUID[0] & 0xFF));
        displayStr.append("-");
        displayStr.append(prefixZeros(objectGUID[5] & 0xFF));
        displayStr.append(prefixZeros(objectGUID[4] & 0xFF));
        displayStr.append("-");
        displayStr.append(prefixZeros(objectGUID[7] & 0xFF));
        displayStr.append(prefixZeros(objectGUID[6] & 0xFF));
        displayStr.append("-");
        displayStr.append(prefixZeros(objectGUID[8] & 0xFF));
        displayStr.append(prefixZeros(objectGUID[9] & 0xFF));
        displayStr.append("-");
        displayStr.append(prefixZeros(objectGUID[10] & 0xFF));
        displayStr.append(prefixZeros(objectGUID[11] & 0xFF));
        displayStr.append(prefixZeros(objectGUID[12] & 0xFF));
        displayStr.append(prefixZeros(objectGUID[13] & 0xFF));
        displayStr.append(prefixZeros(objectGUID[14] & 0xFF));
        displayStr.append(prefixZeros(objectGUID[15] & 0xFF));

        return displayStr.toString();
    }

    private static String prefixZeros(int value) {
        if (value <= 15) {
            StringBuilder sb = new StringBuilder("0");
            sb.append(Integer.toHexString(value));

            return sb.toString();
        }

        return Integer.toHexString(value);
    }
}
