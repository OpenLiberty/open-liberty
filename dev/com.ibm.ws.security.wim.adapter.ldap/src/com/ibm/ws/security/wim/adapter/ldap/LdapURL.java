/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
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

import javax.naming.directory.SearchControls;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.wsspi.security.wim.exception.WIMSystemException;

/**
 * LdapURL class - Reference Implementation of RFC 2255.
 *
 * Contains the parsed details of an LDAP URL information.
 *
 * An LDAP URL begins with the protocol prefix "ldap" and is defined by
 * the following grammar.
 *
 * ldapurl = scheme "://" [hostport] ["/"
 * [dn ["?" [attributes] ["?" [scope]
 * ["?" [filter] ["?" extensions]]]]]]
 * scheme = "ldap"
 * attributes = attrdesc *("," attrdesc)
 * scope = "base" / "one" / "sub"
 * dn = distinguishedName from Section 3 of [1]
 * hostport = hostport from Section 5 of RFC 1738 [5]
 * attrdesc = AttributeDescription from Section 4.1.5 of [2]
 * filter = filter from Section 4 of [4]
 * extensions = extension *("," extension)
 * extension = ["!"] extype ["=" exvalue]
 * extype = token / xtoken
 * exvalue = LDAPString from section 4.1.2 of [2]
 * token = oid from section 4.1 of [3]
 * xtoken = ("X-" / "x-") token
 */
public final class LdapURL extends Object {

    /**
     * The full qualified class name.
     */
    public final static String SCHEMENAME = "ldap";
    public final static String SCHEMENAMESSL = "ldaps";
    private final String ENCODING = "UTF-8";
    private final String ldapurl;
    private String scheme;
    private String hostport;
    private String host;
    private String port;
    private String dn;
    private String attributesBuffer;
    private String[] attributes;
    private String scope;
    private String filter;
    private String[] extensions;

    private boolean parsedOK;
    private String parseMsg;
    private boolean parsedone;
    private int length;
    private static final TraceComponent tc = Tr.register(LdapURL.class);

    public LdapURL(String a_url) {
        this.ldapurl = a_url;
        this.length = a_url.length();
        parsedOK = false;
        parseMsg = "Unparsed";
        parsedone = false;
    }

    public LdapURL() {
        this.ldapurl = null;
        parsedOK = false;
        parseMsg = "Unparsed";
        parsedone = false;
    }

    private void parse() throws UnsupportedEncodingException {
        int colonDoubleSlash = ldapurl.indexOf("://");
        schemeParser(0, colonDoubleSlash);
        if (parsedone)
            return;

        int lastSlash = ldapurl.indexOf("/", colonDoubleSlash + 3);
        hostPortParser(colonDoubleSlash + 3, lastSlash);
        if (parsedone)
            return;

        int firstQ = ldapurl.indexOf("?", lastSlash + 1);
        searchBaseDnParser(lastSlash + 1, firstQ);
        if (parsedone)
            return;

        int secondQ = ldapurl.indexOf("?", firstQ + 1);
        attributesParser(firstQ + 1, secondQ);
        if (parsedone)
            return;

        int thirdQ = ldapurl.indexOf("?", secondQ + 1);
        scopeParser(secondQ + 1, thirdQ);
        if (parsedone)
            return;

        int fourthQ = ldapurl.indexOf("?", thirdQ + 1);
        filterParser(thirdQ + 1, fourthQ);
        if (parsedone)
            return;

        extensionsParser(fourthQ + 1);
        if (parsedone)
            return;

        return;
    }

    private void schemeParser(int beginIndex, int endIndex) {
        if (endIndex > beginIndex) {
            scheme = ldapurl.substring(beginIndex, endIndex);
            if (scheme.compareToIgnoreCase(SCHEMENAME) == 0 || scheme.compareToIgnoreCase(SCHEMENAMESSL) == 0) {
                parseMsg = "scheme parsed";
            } else {
                parseMsg = "invalid ldap url";
                parsedone = true;
            }
        } else {
            parseMsg = "invalid ldap url";
            parsedone = true;
        }

        return;
    }

    private void hostPortParser(int beginIndex, int endIndex) throws UnsupportedEncodingException {
        if (endIndex > beginIndex) {
            hostport = ldapurl.substring(beginIndex, endIndex);
            hostPortSubParser();
            parseMsg = "hostport parsed";
        } else if (endIndex == beginIndex) {
            parseMsg = "no hostport";
        } else if (endIndex == -1) {
            if (length >= beginIndex + 1) {
                hostport = ldapurl.substring(beginIndex);
                hostPortSubParser();
                parseMsg = "hostport parsed";
                parsedOK = true;
            }
            parsedone = true;
        }
        return;
    }

    private void hostPortSubParser() throws UnsupportedEncodingException {
        if (hostport.length() > 0) {
            int colon = hostport.lastIndexOf(":");
            if (colon == -1) {
                host = java.net.URLDecoder.decode(hostport, ENCODING);
            } else {
                String hostBuf = hostport.substring(0, colon);
                host = java.net.URLDecoder.decode(hostBuf, ENCODING);
                String portBuf = hostport.substring(colon + 1);
                port = java.net.URLDecoder.decode(portBuf, ENCODING);
            }
        }
        return;
    }

    private void searchBaseDnParser(int beginIndex, int endIndex) throws UnsupportedEncodingException {
        if (endIndex > beginIndex) {
            String dnBuf = ldapurl.substring(beginIndex, endIndex);
            dn = java.net.URLDecoder.decode(dnBuf, ENCODING);
            parseMsg = "dn parsed";
        } else if (endIndex == beginIndex) {
            // this is not an optional, if first Q mark exists.
            parseMsg = "invalid ldap url";
            parsedone = true;
        } else if (endIndex == -1) {
            if (length >= beginIndex + 1) {
                String dnBuf = ldapurl.substring(beginIndex);
                dn = java.net.URLDecoder.decode(dnBuf, ENCODING);
                parseMsg = "dn parsed";
                parsedOK = true;
            }
            parsedone = true;
        }
        return;
    }

    private void attributesParser(int beginIndex, int endIndex) {
        if (endIndex > beginIndex) {
            attributesBuffer = ldapurl.substring(beginIndex, endIndex);
            attributesSubParser();
            parseMsg = "attributes parsed";
        } else if (endIndex == beginIndex) {
            parseMsg = "no attributes";
        } else if (endIndex == -1) {
            if (length >= beginIndex + 1) {
                attributesBuffer = ldapurl.substring(beginIndex);
                attributesSubParser();
                parseMsg = "attributes parsed";
                parsedOK = true;
            }
            parsedone = true;
        }
        return;
    }

    private void attributesSubParser() {
        attributes = attributesBuffer.split(",");
        return;
    }

    private void scopeParser(int beginIndex, int endIndex) {
        if (endIndex > beginIndex) {
            scope = ldapurl.substring(beginIndex, endIndex);
            parseMsg = "scope parsed";
        } else if (endIndex == beginIndex) {
            parseMsg = "no scope";
        } else if (endIndex == -1) {
            if (length >= beginIndex + 1) {
                scope = ldapurl.substring(beginIndex);
                parseMsg = "scope parsed";
                parsedOK = true;
            }
            parsedone = true;
        }
        return;
    }

    private void filterParser(int beginIndex, int endIndex) throws UnsupportedEncodingException {
        if (endIndex > beginIndex) {
            String filterBuf = ldapurl.substring(beginIndex, endIndex);
            filter = java.net.URLDecoder.decode(filterBuf, ENCODING);
            parseMsg = "filter parsed";
        } else if (endIndex == beginIndex) {
            parseMsg = "no filter";
        } else if (endIndex == -1) {
            if (length >= beginIndex + 1) {
                String filterBuf = ldapurl.substring(beginIndex);
                filter = java.net.URLDecoder.decode(filterBuf, ENCODING);
                parseMsg = "filter parsed";
                parsedOK = true;
            }
            parsedone = true;
        }
        return;
    }

    private void extensionsParser(int fromIndex) {
        if (length >= fromIndex + 1) {
            String extensionsBuffer = ldapurl.substring(fromIndex);

            int beginIndex, endIndex;
            int bufferLen;
            int arrayIndex = 0;

            bufferLen = extensionsBuffer.length();
            for (beginIndex = 0, arrayIndex = 0; bufferLen > beginIndex + 1; beginIndex = endIndex + 1, arrayIndex++) {
                endIndex = extensionsBuffer.indexOf(",", beginIndex);
                if (endIndex > 0)
                    extensions[arrayIndex] = extensionsBuffer.substring(beginIndex, endIndex);
                else if (endIndex == -1) {
                    extensions[arrayIndex] = extensionsBuffer.substring(beginIndex);
                    break;
                }
            }
            parseMsg = "extensions parsed";
        }
        parsedOK = true;
        parsedone = true;
        return;
    }

    /**
     * Returns true of the URL was parsed successfully. If false, the URL
     * is malformed. call <tt>get_parseMSG()</tt> for an indication of what
     * is wring with the URL
     */
    public boolean parsedOK() throws WIMSystemException {
        try {
            if (this.parsedone == false)
                this.parse();
        } catch (UnsupportedEncodingException e) {
            throw new WIMSystemException(WIMMessageKey.GENERIC, Tr.formatMessage(
                                                                                 tc,
                                                                                 WIMMessageKey.GENERIC,
                                                                                 WIMMessageHelper.generateMsgParms(e.toString())));
        } catch (Exception e) {
            e.getMessage();
            this.parsedOK = false;
        }
        return this.parsedOK;
    }

    /**
     * Contains a message informing of any errors encountered during the
     * parsing of the URL
     */
    public String get_parseMsg() {
        return (this.parseMsg);
    }

    /** Returns the full LDAP URL string */
    public String get_url() {
        return (this.ldapurl);
    }

    /**
     * Returns a parsed version of the hostname
     * - i.e. with URL encoding characters removed
     */
    public String get_host() {
        return (this.host);
    }

    /**
     * Returns a parsed version of the port number (if present)
     * - i.e. with URL encoding characters removed
     */
    public String get_port() {
        return (this.port);
    }

    /**
     * Returns a parsed version of the distinguished name
     * - i.e. with URL encoding characters removed
     */
    public String get_dn() {
        return (this.dn);
    }

    /** Returns an array of AttributeType names */
    public String[] get_attributes() {
        if (attributes != null)
            return (this.attributes).clone();
        else
            return new String[0];

    }

    /**
     * Returns a parsed version of the scope
     * one of base, one or sub
     */
    public String get_scope() {
        return (this.scope);
    }

    /**
     * Returns a parsed version of the search filter
     * - i.e. with URL encoding characters removed
     */
    public String get_filter() {
        return (this.filter);
    }

    /** Returns the search scope used in LDAP search */
    public int get_searchScope() {
        int searchScope = SearchControls.OBJECT_SCOPE;
        String scopeBuf = get_scope();
        if (scopeBuf != null) {
            if (scopeBuf.compareToIgnoreCase("base") == 0) {
                searchScope = SearchControls.OBJECT_SCOPE;
            } else if (scopeBuf.compareToIgnoreCase("one") == 0) {
                searchScope = SearchControls.ONELEVEL_SCOPE;
            } else if (scopeBuf.compareToIgnoreCase("sub") == 0) {
                searchScope = SearchControls.SUBTREE_SCOPE;
            }
        }
        return searchScope;
    }

    public String[] get_extensions() {
        return (this.extensions).clone();
    }

    @Override
    public String toString() {
        return ldapurl;
    }
}
