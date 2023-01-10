/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.fat.multiProvider.web;

@SuppressWarnings("serial")
public class CustomSelectionServletWithLocalAuthentication extends CustomSelectionServlet {

    public static final String J_SECURITY_CHECK = "j_security_check";
    public static final String J_USERNAME = "j_username";
    public static final String J_PASSWORD = "j_password";

    protected static String TITLE = "Custom Selection Page with Local Authentication";
    protected static String HEADER_TEXT = "Welcome to the custom social media selection page with local authentication";

    @Override
    String getHtmlBody() {
        StringBuilder html = new StringBuilder();
        html.append("<body>\n");
        html.append("<h1>" + HEADER_TEXT + "</h1>\n");
        html.append(outputRequestParameters());
        html.append(getSubmissionForm());
        html.append(getUserCredentialSubmissionForm());
        html.append("</body>\n");
        return html.toString();
    }

    String getUserCredentialSubmissionForm() {
        StringBuilder html = new StringBuilder();
        html.append("<p>\n");
        html.append("Or sign in using credentials:\n");

        html.append("<form action=\"" + J_SECURITY_CHECK + "\" method=\"POST\">\n");
        html.append(getCredentialInputs());
        html.append(getHiddenRequestParameterInputs());
        html.append("</form>\n");

        return html.toString();
    }

    String getCredentialInputs() {
        StringBuilder html = new StringBuilder();
        html.append(getUsernameInput());
        html.append(getPasswordInput());
        html.append(getSubmitButton());
        return html.toString();
    }

    String getUsernameInput() {
        StringBuilder html = new StringBuilder();
        html.append("<input type=\"text\" name=\"" + J_USERNAME + "\" />\n");
        return html.toString();
    }

    String getPasswordInput() {
        StringBuilder html = new StringBuilder();
        html.append("<input type=\"password\" name=\"" + J_PASSWORD + "\" />\n");
        return html.toString();
    }

    String getSubmitButton() {
        StringBuilder html = new StringBuilder();
        html.append("<input type=\"submit\" value=\"Submit\" />\n");
        return html.toString();
    }

}
