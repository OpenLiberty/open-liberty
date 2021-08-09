/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonMessageTools;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;

public class FillFormTools {

    private final static Class<?> thisClass = FillFormTools.class;
    public static CommonMessageTools msgUtils = new CommonMessageTools();

    /**
     * Fill in the login form. The user/pass parm fields can vary by provider
     *
     * @param form
     *            - the form to fill in
     * @param settings
     *            - the test case settings to use to fill in on the form
     */
    public WebForm fillRPLoginForm(WebForm form, TestSettings settings) {

        String thisMethod = "fillRPLoginForm";
        msgUtils.printMethodName(thisMethod);

        Log.info(thisClass, thisMethod, "Filling in login form for "
                + settings.getProvider() + ".  Setting " + settings.getUserParm() + ": "
                + settings.getUserName() + " and " + settings.getPassParm() + ": "
                + settings.getUserPassword());

        try {

            if (settings.getUserParm() != null && !settings.getUserParm().isEmpty()) {
                form.setParameter(settings.getUserParm(), settings.getUserName());
            }
            form.setParameter(settings.getPassParm(), settings.getUserPassword());
            Log.info(thisClass, thisMethod, "Form ID: "
                    + form.getID().toString());
            if (Constants.DEBUG) {
                Log.info(thisClass, thisMethod, "Filled in Form: "
                        + form.toString());
            }

        } catch (Exception e) {
            Log.error(thisClass, thisMethod, e,
                    "Could not set the id/pass - calling code should try login and catch failure off of that if necessary!");
        }
        return form;

    }

    public WebForm fillClientForm(String testcase, WebForm form, TestSettings settings) {

        msgUtils.printMethodName("fillClientForm");

        // Set username
        form.setParameter("user_name", settings.getClientName());

        // Set Client ID
        form.setParameter("client_id", settings.getClientID());

        // Set Client Secret
        form.setParameter("client_secret", settings.getClientSecret());

        // Set redirect URL
        form.setParameter("redirect_uri", settings.getClientRedirect());

        // Set authorize endpoint
        form.setParameter("authorize_endpoint", settings.getAuthorizeEndpt());

        // Set token endpoint
        form.setParameter("token_endpoint", settings.getTokenEndpt());

        // Set protected resource
        form.setParameter("resource_endpoint", settings.getProtectedResource());

        // Set state
        form.setParameter("state", settings.getState());

        // Set scope
        form.setParameter("scope", settings.getScope());

        // set response type if specified
        if (settings.getResponseType() != null) {
            Log.info(thisClass, "fillClientForm", "Setting response_type to: ", settings.getResponseType());
            form.setParameter("response_type", settings.getResponseType());
        } else {
            Log.info(thisClass, "fillClientForm", "NOT setting response_type");
        }

        // set nonce  if specified
        if (settings.getNonce() != null) {
            Log.info(thisClass, "fillClientForm", "Setting nonce to: ", settings.getNonce());
            form.setParameter("nonce", settings.getNonce());
        } else {
            Log.info(thisClass, "fillClientForm", "NOT setting nonce");
            form.removeParameter("nonce");
        }

        // set autoauthz
        form.setParameter("autoauthz", settings.getAutoAuthz());

        // set testcase name
        form.setParameter("test_name", testcase);

        if (Constants.DEBUG) {
            String[] parmNames = form.getParameterNames();
            if (parmNames != null) {
                for (String p : parmNames) {
                    Log.info(thisClass, "HtmlUnit fillClientForm", "parameter:  name: " + p + " value: " + form.getParameterValue(p));
                }
            }
        }

        return form;
    }

    public void setAttr(HtmlForm form, String name, String value) {

        //        Log.info(thisClass, "setAttr", "Before: " + name + ": " + form.getAttribute(name).toString());
        final HtmlTextInput attr = (HtmlTextInput) form.getInputByName(name);
        attr.setValueAttribute(value);
        //        form.<HtmlInput> getInputByName(name).setValueAttribute(value);
        //        Log.info(thisClass, "setAttr", "After: " + name + ": " + form.getAttribute(name).toString());
    }

    public HtmlForm fillClientForm(String testcase, HtmlForm form, TestSettings settings) {

        msgUtils.printMethodName("fillClientForm");
        Log.info(thisClass, "fillClientForm", "The Page: " + form.getPage().asText().toString());

        // Set username
        //        form.<HtmlInput> getInputByName("user_name").setValueAttribute(settings.getClientName());
        setAttr(form, "user_name", settings.getClientName());

        // Set Client ID
        setAttr(form, "client_id", settings.getClientID());

        // Set Client Secret
        setAttr(form, "client_secret", settings.getClientSecret());

        // Set redirect URL
        setAttr(form, "redirect_uri", settings.getClientRedirect());

        // Set authorize endpoint
        setAttr(form, "authorize_endpoint", settings.getAuthorizeEndpt());

        // Set token endpoint
        setAttr(form, "token_endpoint", settings.getTokenEndpt());

        // Set protected resource
        setAttr(form, "resource_endpoint", settings.getProtectedResource());

        // Set state
        setAttr(form, "state", settings.getState());

        // Set scope
        setAttr(form, "scope", settings.getScope());

        // set response type if specified
        if (settings.getResponseType() != null) {
            Log.info(thisClass, "fillClientForm", "Setting response_type to: ", settings.getResponseType());
            setAttr(form, "response_type", settings.getResponseType());
        } else {
            Log.info(thisClass, "fillClientForm", "NOT setting response_type");
        }

        // set nonce  if specified
        if (settings.getNonce() != null) {
            Log.info(thisClass, "fillClientForm", "Setting nonce to: ", settings.getNonce());
            setAttr(form, "nonce", settings.getNonce());
        } else {
            Log.info(thisClass, "fillClientForm", "NOT setting nonce");
            form.<HtmlInput> getInputByName("nonce").remove();
        }

        // set autoauthz
        setAttr(form, "autoauthz", settings.getAutoAuthz());

        // set testcase name
        setAttr(form, "test_name", testcase);

        Log.info(thisClass, "fillClientForm", "The Page: " + form.getPage().asText().toString());

        //        if (Constants.DEBUG) {
        //            NamedNodeMap parms = form.getAttributes();
        //            int numItems = parms.getLength();
        //            for (int i = 0; i < numItems; i++) {
        //                Node x = parms.item(i);
        //                Log.info(thisClass, "fillClientForm", "parameter:  name: " + x.getNodeName() + " value: " + x.getNodeValue());
        //
        //            }
        //        }

        return form;
    }

    public WebForm fillClientForm2(WebForm form, TestSettings settings) {

        msgUtils.printMethodName("fillClientForm2");
        String adminUser = settings.getAdminUser();
        if (adminUser != null) {
            // Set username
            form.setParameter("user_id", adminUser);
        }

        String adminPswd = settings.getAdminPswd();
        if (adminPswd != null) {
            // Set password
            form.setParameter("user_pass", adminPswd);
        }

        // Set Client ID
        form.setParameter("client_id", settings.getClientID());

        // Set Client secret
        form.setParameter("client_secret", settings.getClientSecret());

        // Set token endpoint
        form.setParameter("token_endpoint", settings.getTokenEndpt());

        // Set protected resource
        form.setParameter("resource_endpoint", settings.getProtectedResource());

        // Set scope
        form.setParameter("scope", settings.getScope());

        // Set autoauthz
        form.setParameter("autoauthz", settings.getAutoAuthz());

        if (settings.getResponseType() != null) {
            Log.info(thisClass, "fillClientForm2", "Setting response_type to: ", settings.getResponseType());
            form.setParameter("response_type", settings.getResponseType());
        } else {
            Log.info(thisClass, "fillClientForm2", "NOT setting response_type to: ", settings.getResponseType());

        }

        return form;
    }

    public WebForm fillOPLoginForm(WebForm form, TestSettings settings) {

        String thisMethod = "fillOPLoginForm";
        msgUtils.printMethodName(thisMethod);
        Log.info(thisClass, thisMethod, "j_username: " + settings.getAdminUser());
        Log.info(thisClass, thisMethod, "j_password: " + settings.getAdminPswd());
        form.setParameter("j_username", settings.getAdminUser());
        form.setParameter("j_password", settings.getAdminPswd());
        return form;
    }

    public WebRequest fillAuthorizationForm(WebRequest form, TestSettings settings) {

        String thisMethod = "fillAuthorizationForm";
        msgUtils.printMethodName(thisMethod);
        // auto and response_type are currently "hidden" in the jsp's, so no sure what the
        // next 2 lines will do - will add them to settings if/when I see them actually used.
        form.setParameter("auto", "true");
        //form.setParameter("response_type", "token");
        form.setParameter("user_name", settings.getClientName());
        form.setParameter("client_id", settings.getClientID());
        form.setParameter("redirect_uri", settings.getClientRedirect());
        form.setParameter("authorize_endpoint", settings.getAuthorizeEndpt());
        form.setParameter("state", settings.getState());
        form.setParameter("scope", settings.getScope());
        form.setParameter("autoauthz", settings.getAutoAuthz());
        if (settings.getResponseType() != null) {
            Log.info(thisClass, thisMethod, "Setting response_type to: ", settings.getResponseType());
            form.setParameter("response_type", settings.getResponseType());
        } else {
            if (settings.getScope().contains("openid")) {
                Log.info(thisClass, thisMethod, "Setting response_type default of: id_token token");
                form.setParameter("response_type", "id_token token");
            } else {
                Log.info(thisClass, thisMethod, "Setting response_type default of: token");
                form.setParameter("response_type", "token");
            }
        }
        //		if ( settings.getNonce() != null && ! settings.getNonce().isEmpty() ) {
        //			form.setParameter("nonce", settings.getNonce()) ;
        //		}

        // set nonce  if specified
        if (settings.getNonce() != null && !settings.getNonce().isEmpty()) {
            Log.info(thisClass, thisMethod, "Setting nonce to: " + settings.getNonce());
            form.setParameter("nonce", settings.getNonce());
        } else {
            Log.info(thisClass, thisMethod, "NOT setting nonce");
            form.removeParameter("nonce");
        }

        if (Constants.DEBUG) {
            String[] parmNames = form.getRequestParameterNames();
            if (parmNames != null) {
                for (String p : parmNames) {
                    Log.info(thisClass, thisMethod, "parameter:  name: " + p + " value: " + form.getParameter(p));
                }
            }
        }

        return form;
    }

    /**
     * Fill in the openid_identifier on the form. The information to use is in
     * the settings
     *
     * @param form
     *            - the form to fill in
     * @param settings
     *            - the test case settings to use to fill in on the form
     */
    public WebForm fillProviderUrl(WebForm form, TestSettings settings) {

        String thisMethod = "fillProviderUrl";
        msgUtils.printMethodName(thisMethod);

        Log.info(thisClass, thisMethod,
                "Filling in provider form for provider: " + settings.getProvider());
        form.setParameter("openid_identifier", settings.getProvider());

        return form;
    }

}
