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
package com.ibm.ws.security.oauth20.filter;

import javax.servlet.http.HttpServletRequest;

import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.security.oauth20.TraceConstants;
import com.ibm.ws.security.oauth20.util.UtilConstants;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A completely new TAI filter based on the older (and less flexible) HTTPHeaderFilter. The existing 
 * grammar is supported but extended to support IP ranges in addition to simple text comparisons. In order
 * to make this work effectively, the entire implmentation was completely redone. As a side effect it is now
 * much more flexbible and it should be possible to add new types of comparisons or data types more easily.
 * 
 * The essential approach is that a filter specifies a set of conditions which are met or not met. These
 * conditions are logically ANDed together so that if one condition fails, the entire filter fails. Conditions
 * are separated by the ; operator. Each condition specifies three elements:
 * - the operator (==, !=, %=, ^=, <, >)
 * - the input required element (generally an HTTP header name, but request-url, request-uri & remote-address are special)
 * - the comparison value (generally a string, but IP address ranges are allowed) 
 * 
 * Here are a few examples:
 *              remote-address==192.168.*.*
 *          remote-address==192.168.[7-13].*
 *      request-url!=noSPNEGO;remote-address==192.168.*.*
 *      user-agent%=IE6
 * 
 * Often the value being compared is just a string. However, notice that some examples using IP addresses with
 * wildcarding. These are referred to as the ValueAddressRange type. Possible values are an exact IP address,
 * an IP address ending in wildcards (e.g., '*') or a range specified with []. Ranges are then compared against
 * the input to determine if the comparison holds.
 * 
 * Conditions are represented as an object of type ICondition. The possible conditions are:
 * %=  ContainsCondition - the input contains the comparison value
 * >   GreaterCondition - the input is greater than the comparison value
 * <   LessCondition - the input is less than the comparison value
 * !=  NotContainsCondition - the input does not contain the comparison value
 * ^=  OrCondition - the input contains one of the comparison values
 * ==  EqualCondition - the input is equal to the comparison value
 * 
 * Values are of type IValue, thus conditions comparse IValues. Inputs are converted to IValues. The types are
 *   ValueString
 *   ValueIPAddress
 *   ValueAddressRange
 * 
 * ValueStrings are compared using the usual string comparison functions in java. IP Address range comparisons
 * are interpreted as follows:
 *   %=  
 *   >   the input IP address is numerically above the specified IP address range
 *   <   the input IP address is numerically below the specified IP address range
 *   !=  the input IP address is not *in* specified IP address range (notice we don't mean not equal)
 *   ^=  the input IP address is in one of the specified IP address ranges
 *   ==  the input IP address is *in* the specified IP address range (notice we don't mean equal)
 * 
 */

/**
 * This class was imported from tWAS to make only those changes necessary to
 * run OAuth on Liberty. The mission was not to refactor, restructure, or
 * generally cleanup the code.
 */
public class OAuthResourceProtectionFilter extends CommonHTTPHeaderFilter implements HTTPHeaderFilter {

    static final TraceComponent tc = Tr.register(OAuthResourceProtectionFilter.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public OAuthResourceProtectionFilter(boolean processAll) {
        super();
        super.setProcessAll(processAll);
    }

    public OAuthResourceProtectionFilter(String p, boolean processAll) {
        super(p);
        super.setProcessAll(processAll);
    }

    public boolean init(String s1) {
        super.init(s1);
        boolean initialized = false;
        if (s1 == null) {
            nonFilter = true;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Filter Not Defined");

        }
        else {
            initialized = true;
        }
        return initialized;
    }

    /*
     * This method has two versions to make testing with JUnit easier. This is
     * the "real" method that takes an HttpServletRequest object. It just puts
     * it in a wrapper (allowing for inserting test drivers) and then calls the
     * real code.
     */
    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.web.saml.filter.HTTPHeaderFilter#isAccepted(javax.servlet.http.HttpServletRequest)
     */
    public boolean isAccepted(HttpServletRequest req) {
        if (isOAuthServiceApp(req)) {
            return false;
        }
        return isAccepted(new RealRequestInfo(req));
    }

    private boolean isOAuthServiceApp(HttpServletRequest req) {
        String appName = getApplication(req);
        boolean oauthApp = false;
        if (UtilConstants.OAUTH_SERVICE_APP.equalsIgnoreCase(appName) ||
                (UtilConstants.OIDC_SERVICE_APP.equalsIgnoreCase(appName) &&
                !isProtectedByAccessToken(req))) {
            oauthApp = true;
        }

        return oauthApp;
    }

    /**
     * Returns true if the request is for a URI that is not
     * protected by an access_token, for example the /authorize
     * and /token
     * 
     * @param req HttpServletRequest object
     * @return true/false
     */
    private boolean isProtectedByAccessToken(HttpServletRequest req) {
        String uri = req.getRequestURI();
        return !(uri.endsWith(UtilConstants.TOKEN) || uri.endsWith(UtilConstants.AUTHORIZE));
    }

    private static String getApplication(HttpServletRequest req) {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        WebModuleMetaData wmmd = (WebModuleMetaData) cmd.getModuleMetaData();
        return wmmd.getConfiguration().getApplicationName();
    }

}
