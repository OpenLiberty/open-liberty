/*******************************************************************************
 * Copyright (c) 1997, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.filter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class CommonHTTPHeaderFilter {

    private static final TraceComponent tc = Tr.register(CommonHTTPHeaderFilter.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    protected boolean nonFilter = false;
    protected boolean processAll = false;
    protected static final String APPLICATION_NAMES = "applicationNames";
    protected static final String REFERRER = "Referer";
    protected List<ICondition> filterCondition = new LinkedList<ICondition>();

    public CommonHTTPHeaderFilter() {
        super();
    }

    public CommonHTTPHeaderFilter(String s1) {
        super();
        init(s1);
    }

    public boolean init(String s1) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, s1);
        if (s1 == null) {
            nonFilter = true;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Filter Not Defined");
            if (tc.isEntryEnabled())
                Tr.exit(tc, "init", Boolean.toString(false));
            return false;
        }

        // break up conditions around semicolon
        StringTokenizer st1 = new StringTokenizer(s1, ";");
        StringTokenizer st2 = null;
        String s2 = null;

        while (st1.hasMoreTokens()) {
            s2 = st1.nextToken();
            // break up individual condition based on its three parts
            st2 = new StringTokenizer(s2, "^=!<>%");

            // first token is the HTTP header name
            String key = st2.nextToken();
            if (!st2.hasMoreTokens()) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, s2);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "init", Boolean.toString(false));
                return false; // if no second token we have a problem
            }

            // second token is the value
            String valueString = st2.nextToken();

            // between the tokens there is the operand
            String operand = s2.substring(key.length(), s2.length() - valueString.length()).trim();

            // IP addresses are special
            boolean ipAddress = false;
            if ("remote-address".equals(key))
                ipAddress = true;

            if (tc.isDebugEnabled())
                Tr.debug(tc, "isValid", "Adding " + key + " " + operand + " " + valueString);

            // This is awful but existing api doesn't allow for an exception
            // So, I have the throw a runtimeexception if I can't parse the condition
            try {
                ICondition condition = makeCondition(key, operand, valueString, ipAddress);
                filterCondition.add(condition);
            } catch (FilterException e) {
                throw new RuntimeException(e);
            }

        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "init", Boolean.toString(true));
        return true;
    }

    protected boolean isAccepted(IRequestInfo req) {
        String reason = "TAI will intercept request.";
        boolean isAccepted = false;
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isAccepted");

        if (processAll) {
            reason = "processAll is true, therefore we always intercept.";
            if (tc.isEntryEnabled()) {
                Tr.exit(tc, "isAccepted", isAccepted + " " + reason);
            }
            return true;
        }

        String HTTPheader = null;

        // cycle through the filter settings
        Iterator<ICondition> iter = filterCondition.iterator();
        while (iter.hasNext()) {
            ICondition cond = iter.next();
            HTTPheader = req.getHeader(cond.getKey());

            boolean ipAddress = false;

            // First, if the header argument is a null, then extract either the
            // "remote-address" or the "request-url" values from the request
            // object if the key uses either of these "tags"
            if (HTTPheader == null) {
                if (cond.getKey().equals("remote-address")) {
                    // there is no remote-address HTTP header, we get it from
                    // the HTTP request
                    HTTPheader = req.getRemoteAddr();
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "isAccepted", "HTTPheader obtained from 'remote-address' " + HTTPheader);
                    ipAddress = true;
                } else if (cond.getKey().equals("request-url")) {
                    // there is no request-url HTTP header, we get it from the
                    // HTTP request
                    String queryString = req.getQueryString();
                    if (queryString != null) {
                        HTTPheader = req.getRequestURL().toString() + "?" + queryString;
                    } else {
                        HTTPheader = req.getRequestURL().toString();
                    }

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "isAccepted", "HTTPheader obtained from 'request-url' " + HTTPheader);
                } else if (cond.getKey().equals("request-uri")) {
                    // there is no request-url HTTP header, we get it from the
                    // HTTP request

                    HTTPheader = req.getRequestURI();

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "isAccepted", "HTTPheader obtained from 'request-uri' " + HTTPheader);
                } else if (cond.getKey().equals(REFERRER)) {
                    // there is no request-url HTTP header, we get it from the
                    // HTTP request
                    HTTPheader = req.getReferer();

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "isAccepted", "HTTPheader obtained from 'Referer' " + HTTPheader);
                } else if (cond.getKey().equals(APPLICATION_NAMES)) {
                    // there is no request-url HTTP header, we get it from the
                    // HTTP request
                    String queryString = req.getApplicationName();
                    if (queryString != null) {
                        HTTPheader = queryString;
                    }

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "isAccepted", "ApplicationName:" + HTTPheader);
                } else {
                    if (cond instanceof NotContainsCondition) {
                        continue;
                    }
                    reason = "No HTTPheader found, and no 'remote-address' or 'request-url' rule used - do not Intercept.";
                    isAccepted = false;
                    break; // if no header found, the condition fails
                }
            }

            /*
             * Now we have the request element for comparison, now we execute the condition.
             */
            if (tc.isDebugEnabled())
                Tr.debug(tc, "isAccepted", "Checking condition:" + cond);
            try {
                IValue compareValue;
                if (ipAddress) {
                    compareValue = new ValueIPAddress(HTTPheader);
                } else {
                    compareValue = new ValueString(HTTPheader);
                }
                boolean answer = cond.checkCondition(compareValue);

                if (answer == false) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "isAccepted", "check failed, returning false. TAI will not intercept");
                    isAccepted = false;
                    break;
                } else {
                    isAccepted = true;
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "isAccepted", "check passed, continuing to next condition");
                }
            } catch (FilterException e) {
                throw new RuntimeException(e);
            }
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "isAccepted", "TAI will intercept request");
            Tr.exit(tc, "isAccepted", isAccepted + " " + reason);
        }
        return isAccepted;
    }

    /**
     * Given the three parts (key, operand, value) make a proper condition object. Notice that I need to know
     * if the value should be interpreted as an IP address.
     *
     * Basically just look at the operator string and create the correct condition. Only the OR involved any
     * complicated processing since there are multiple values for OR.
     *
     * @param key
     * @param operand
     * @param valueString
     * @param ipAddress
     * @return
     * @throws FilterException
     */
    private ICondition makeCondition(String key, String operand, String valueString, boolean ipAddress) throws FilterException {
        if (operand.equals("==")) {
            return new EqualCondition(key, makeValue(valueString, ipAddress));
        } else if (operand.equals("!=")) {
            return new NotContainsCondition(key, makeValue(valueString, ipAddress));
        } else if (operand.equals("^=")) {
            // or involves an | delimited set of values
            OrCondition cond = new OrCondition(key);
            StringTokenizer tokens = new StringTokenizer(valueString, "|");
            while (tokens.hasMoreTokens()) {
                String token = tokens.nextToken();
                cond.addValue(makeValue(token, ipAddress));
            }
            return cond;
        } else if (operand.equals("%=")) {
            return new ContainsCondition(key, makeValue(valueString, ipAddress));
        } else if (operand.equals("<")) {
            return new LessCondition(key, makeValue(valueString, ipAddress));
        } else if (operand.equals(">")) {
            return new GreaterCondition(key, makeValue(valueString, ipAddress));
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "init", operand);

            throw new FilterException(TraceNLS.getFormattedMessage(this.getClass(),
                    TraceConstants.MESSAGE_BUNDLE,
                    "security.tai.malformed.filter.operator",
                    new Object[] { operand },
                    "CWTAI0019E: Filter operator should be one of ''=='', ''!='', ''%='', ''^='', ''>'' or ''<''. Operator used was {0}."));
        }
    }

    /**
     * Helper to make the value for the condition. It's either a IP address (ValueAddressRange) or a
     * string (ValueString).
     *
     * @param value
     * @param ipAddress
     * @return
     * @throws FilterException
     */
    private IValue makeValue(String value, boolean ipAddress) throws FilterException {
        if (ipAddress)
            return new ValueAddressRange(value);
        return new ValueString(value);
    }

    public void setProcessAll(boolean b) {
        processAll = b;
    }

    public boolean noFilter() {
        return nonFilter;
    }

}
