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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * The essential approach is that a filter specifies a set of conditions which are met or not met. These
 * conditions are logically ANDed together so that if one condition fails, the entire filter fails. Conditions
 * are separated by the ; operator. Each condition specifies three elements:
 * - the operator symbol(==, !=, %=, ^=, <, >)
 * - the matchType using word for operator(equals, notEqual, contains, notContains, lessThan, greaterThan)
 * - Note: we do not support symbol and word in the same configuration.
 * - the input required element (generally an HTTP header name, but request-url & remote-address are special)
 * - the comparison value (generally a string, but IP address ranges are allowed)
 * 
 * Here are a few examples:
 * remote-address==192.168.*.*
 * remote-address==192.168.[7-13].*
 * request-url!=noSPNEGO;remote-address==192.168.*.*
 * user-agent%=IE6
 * 
 * Often the value being compared is just a string. However, notice that some examples using IP addresses with
 * wildcarding. These are referred to as the ValueAddressRange type. Possible values are an exact IP address,
 * an IP address ending in wildcards (e.g., '*') or a range specified with []. Ranges are then compared against
 * the input to determine if the comparison holds.
 * 
 * Conditions are represented as an object of type ICondition. The possible conditions are:
 * %= ContainsCondition - the input contains the comparison value
 * > GreaterCondition - the input is greater than the comparison value
 * < LessCondition - the input is less than the comparison value
 * != NotContainsCondition - the input does not contain the comparison value
 * ^= OrCondition - the input contains one of the comparison values
 * == EqualCondition - the input is equal to the comparison value
 * 
 * Values are of type IValue, thus conditions compare IValues. Inputs are converted to IValues. The types are
 * ValueString
 * ValueIPAddress
 * ValueAddressRange
 * 
 * ValueStrings are compared using the usual string comparison functions in java. IP Address range comparisons
 * are interpreted as follows:
 * %=
 * > the input IP address is numerically above the specified IP address range
 * < the input IP address is numerically below the specified IP address range
 * != the input IP address is not *in* specified IP address range (notice we don't mean not equal)
 * ^= the input IP address is in one of the specified IP address ranges
 * == the input IP address is *in* the specified IP address range (notice we don't mean equal)
 * 
 */

public class CommonFilter {

    private static final TraceComponent tc = Tr.register(CommonFilter.class);

    private boolean processAll = false;
    static final String REMOTE_ADDRESS = "remote-address";
    static final String REQUEST_URL = "request-url";
    static final String KEY_HOST = "Host";
    static final String KEY_USER_AGENT = "User-Agent";
    protected static final String APPLICATION_NAMES = "applicationNames"; //OAUTH application supports

    /*
     * filter conditions are stored in a list
     * each element of the list is a Condition of 3 values
     * key operand value
     */
    protected List<ICondition> filterCondition = new LinkedList<ICondition>();

    CommonFilter(AuthFilterConfig authFilterConfig) {
        initialize(authFilterConfig);
    }

    /**
     * Pass the filter string so the implementation can read any of the
     * properties
     * 
     * @param filterString -
     *            set of rules to be used by the filter
     * @return true if no problem occured during parsing of filterString false
     *         otherwise
     */
    public boolean init(String s1) {
        if (s1 == null) {
            Tr.error(tc, "AUTH_FILTER_INIT_NULL_STRING");
            return false;
        }

        //break up conditions around semicolon
        StringTokenizer st1 = new StringTokenizer(s1, ";");
        StringTokenizer st2 = null;
        String s2 = null;

        while (st1.hasMoreTokens()) {

            s2 = st1.nextToken();

            //break up individual condition based on its three parts
            st2 = new StringTokenizer(s2, "^=!<>%");

            //first token is the HTTP header name
            String key = st2.nextToken();
            if (!st2.hasMoreTokens()) {
                Tr.error(tc, "AUTH_FILTER_MALFORMED_CONDITION", new Object[] { s1, s2, null });
                return false; // if no second token we have a problem
            }

            //second token is the value
            String valueString = st2.nextToken();

            // between the tokens there is the operand
            String operand = s2.substring(key.length(),
                                          s2.length() - valueString.length()).trim();

            //IP addresses are special
            boolean ipAddress = false;
            if (REMOTE_ADDRESS.equals(key))
                ipAddress = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Adding " + key + " " + operand + " " + valueString);
            }

            //This is awful but existing api doesn't allow for an exception
            // So, I have the throw a runtimeexception if I can't parse the condition
            try {
                ICondition condition = makeConditionWithSymbolOperand(key, operand, valueString,
                                                                      ipAddress);
                filterCondition.add(condition);
            } catch (FilterException e) {
                throw new RuntimeException(e);
            }

        }
        return true;
    }

    protected void initialize(AuthFilterConfig filterConfig) {
        buildICondition(filterConfig.getWebApps(), AuthFilterConfig.KEY_WEB_APP, AuthFilterConfig.KEY_NAME, false);
        buildICondition(filterConfig.getRequestUrls(), REQUEST_URL, AuthFilterConfig.KEY_URL_PATTERN, false);
        buildICondition(filterConfig.getRemoteAddresses(), REMOTE_ADDRESS, AuthFilterConfig.KEY_IP, true);
        buildICondition(filterConfig.getHosts(), KEY_HOST, AuthFilterConfig.KEY_NAME, false);
        buildICondition(filterConfig.getUserAgents(), KEY_USER_AGENT, AuthFilterConfig.KEY_AGENT, false);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "combine filter conditions: " + filterCondition.toString());
        }
    }

    protected void buildICondition(List<Properties> filterElements, String elementName, String attrName, boolean ipAddress) {
        if (filterElements != null && !filterElements.isEmpty()) {
            Iterator<Properties> iter = filterElements.iterator();
            while (iter.hasNext()) {
                try {
                    Properties props = iter.next();
                    ICondition condition = makeConditionWithMatchType(elementName, props.getProperty(AuthFilterConfig.KEY_MATCH_TYPE),
                                                                      props.getProperty(attrName), ipAddress);
                    filterCondition.add(condition);
                } catch (FilterException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Un-expected exception for processing " + elementName, e);
                    }
                }
            }
        }
    }

    /**
     * Given the three parts (key, operand, value) make a proper condition object. Notice that I need to know
     * if the value should be interpreted as an IP address. There some service that use symbol and others use word
     * for operand/matchType.
     * 
     * Basically just look at the operator string and create the correct condition. Only the OR involved any
     * complicated processing since there are multiple values for OR.
     * 
     * Note: the old configuration use symbols ==, !=, ^=, %=, <, >
     * On Liberty: only OAUTH support the symbols configuration but currently do not call this code
     */
    private ICondition makeConditionWithSymbolOperand(String key, String operand, String valueString,
                                                      boolean ipAddress) throws FilterException {
        if (operand.equals("==")) {
            return new EqualCondition(key, makeValue(valueString, ipAddress), operand);
        } else if (operand.equals("!=")) {
            NotContainsCondition cond = new NotContainsCondition(key, operand);
            processOrValues(valueString, ipAddress, cond);
            return cond;
        } else if (operand.equals("^=")) {
            OrCondition cond = new OrCondition(key, operand);
            processOrValues(valueString, ipAddress, cond);
            return cond;
        } else if (operand.equals("%=")) {
            return new ContainsCondition(key, makeValue(valueString, ipAddress), operand);
        } else if (operand.equals("<")) {
            return new LessCondition(key, makeValue(valueString, ipAddress), operand);
        } else if (operand.equals(">")) {
            return new GreaterCondition(key, makeValue(valueString, ipAddress), operand);
        } else {
            Tr.error(tc, "AUTH_FILTER_MALFORMED_SYMBOL_MATCH_TYPE", new Object[] { operand });
            throw new FilterException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                   TraceConstants.MESSAGE_BUNDLE,
                                                                   "AUTH_FILTER_MALFORMED_SYMBOL_MATCH_TYPE",
                                                                   null,
                                                                   "CWWKS4352E: The filter match type should be one of: ==, !=, %=, > or <. The match type used was {0}."));
        }
    }

    /**
     * Given the three parts (key, operand, value) make a proper condition object. Notice that I need to know
     * if the value should be interpreted as an IP address. There some service that use symbol and others use word
     * for operand/matchType.
     * 
     * Basically just look at the operator string and create the correct condition. Only the OR involved any
     * complicated processing since there are multiple values for OR.
     * 
     * Note: the new configuration use words such as equals, notContain, contains, greaterThan or lessThan
     */
    private ICondition makeConditionWithMatchType(String key, String operand, String valueString,
                                                  boolean ipAddress) throws FilterException {
        if (operand.equalsIgnoreCase(AuthFilterConfig.MATCH_TYPE_EQUALS)) {
            return new EqualCondition(key, makeValue(valueString, ipAddress), operand);
        } else if (operand.equalsIgnoreCase(AuthFilterConfig.MATCH_TYPE_NOT_CONTAIN)) {
            NotContainsCondition cond = new NotContainsCondition(key, operand);
            processOrValues(valueString, ipAddress, cond);
            return cond;
        } else if (operand.equalsIgnoreCase(AuthFilterConfig.MATCH_TYPE_CONTAINS)) {
            OrCondition cond = new OrCondition(key, operand);
            processOrValues(valueString, ipAddress, cond);
            return cond;
        } else if (operand.equalsIgnoreCase(AuthFilterConfig.MATCH_TYPE_LESS_THAN)) {
            return new LessCondition(key, makeValue(valueString, ipAddress), operand);
        } else if (operand.equalsIgnoreCase(AuthFilterConfig.MATCH_TYPE_GREATER_THAN)) {
            return new GreaterCondition(key, makeValue(valueString, ipAddress), operand);
        } else {
            Tr.error(tc, "AUTH_FILTER_MALFORMED_WORD_MATCH_TYPE", new Object[] { operand });
            throw new FilterException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                   TraceConstants.MESSAGE_BUNDLE,
                                                                   "AUTH_FILTER_MALFORMED_WORD_MATCH_TYPE",
                                                                   null,
                                                                   "CWWKS4353E: The filter match type should be one of: equals, notContain, contains, greaterThan or lessThan. The match type used was {0}."));
        }
    }

    /**
     * @param valueString
     * @param ipAddress
     * @param cond
     * @throws FilterException
     */
    private void processOrValues(String valueString, boolean ipAddress, OrCondition cond) throws FilterException {
        //or involves an | delimited set of values
        StringTokenizer tokens = new StringTokenizer(valueString, "|");
        while (tokens != null && tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            cond.addValue(makeValue(token, ipAddress));
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
    private IValue makeValue(String value, boolean ipAddress)
                    throws FilterException {
        if (ipAddress)
            return new ValueAddressRange(value);
        return new ValueString(value);
    }

    /**
     * Indicates if TAI should intercept request, based on pre-defined rules. Basically just execute
     * the conditions created earlier.
     * 
     * @param IRequestInfo
     * @return true if the request passes the filter criteria otherwise false
     */
    public boolean isAccepted(IRequestInfo req) {
        boolean answer = true;
        if (processAll) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "processAll is true, therefore we always intercept.");
            }
            return answer;
        }

        String HTTPheader = null;

        Iterator<ICondition> iter = filterCondition.iterator();
        while (iter.hasNext()) {
            ICondition cond = iter.next();
            HTTPheader = req.getHeader(cond.getKey());

            boolean ipAddress = false;

            // First, if the header argument is a null, then extract either the
            // "remote-address" or the "request-url" values from the request
            // object if the key uses either of these "tags"
            if (HTTPheader == null) {
                String key = cond.getKey();
                if (key.equals(REMOTE_ADDRESS)) {
                    HTTPheader = req.getRemoteAddr();
                    ipAddress = true;
                } else if (key.equals(REQUEST_URL)) {
                    HTTPheader = req.getRequestURL();
                } else if (key.equals(AuthFilterConfig.KEY_WEB_APP) || key.equals(APPLICATION_NAMES)) {
                    HTTPheader = req.getApplicationName();
                } else if (cond instanceof NotContainsCondition) {
                    continue;
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "No HTTPheader found, and no 'remote-address' or 'request-url' or 'requestApp' rule used - do not Intercept.");
                    }
                    return false; // if no header found, the condition fails
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Checking condition {0} {1}.", new Object[] { cond, HTTPheader });
            }
            try {
                IValue compareValue;
                if (ipAddress) {
                    compareValue = new ValueIPAddress(HTTPheader);
                } else {
                    compareValue = new ValueString(HTTPheader);
                }
                answer = cond.checkCondition(compareValue);
                if (!answer) {
                    break;
                }

            } catch (FilterException e) {
                throw new RuntimeException(e);
            }
        }
        return answer;
    }

    /**
     * Optionally use this method to indicate that all requests to this filter
     * will be processed.
     * 
     * @param -
     *        true will cause all calls to isAccepted() to return true
     */
    public void setProcessAll(boolean b) {
        processAll = b;
    }
}
