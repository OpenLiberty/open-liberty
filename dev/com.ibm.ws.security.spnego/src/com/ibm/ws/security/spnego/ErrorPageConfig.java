/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.security.spnego.internal.TraceConstants;

public class ErrorPageConfig {
    private final String DEFAULT_SPNEGO_NOT_SUPPORTED_PAGE_CONTENT =
                    TraceNLS.getFormattedMessage(this.getClass(),
                                                 TraceConstants.MESSAGE_BUNDLE,
                                                 "SPNEGO_NOT_SUPPORTED_ERROR_PAGE",
                                                 null,
                                                 "CWWKS4306E: <html><head><title>SPNEGO authentication is not supported.</title></head><body>SPNEGO authentication is not supported on this client browser.</body></html>.");

    private final String DEFAULT_NTLM_TOKEN_RECEIVED_PAGE_CONTENT =
                    TraceNLS.getFormattedMessage(this.getClass(),
                                                 TraceConstants.MESSAGE_BUNDLE,
                                                 "SPNEGO_NTLM_TOKEN_RECEIVED_ERROR_PAGE",
                                                 null,
                                                 "CWWKS4307E: <html><head><title>An NTLM Token was received.</title></head><body>Your client browser configuration is correct, but you have not logged into a supported Windows Domain.<p>Please login to the supported Windows Domain.</html>.");

    private final PageLoader spnegoNotSupportedPageLoader;

    private final PageLoader ntlmTokenReceivedPageLoader;

    public ErrorPageConfig(String spnegoNotSupportedErrorPageURL, String ntlmTokenReceivedErrorPageURL) {
        spnegoNotSupportedPageLoader = new PageLoader(spnegoNotSupportedErrorPageURL, DEFAULT_SPNEGO_NOT_SUPPORTED_PAGE_CONTENT);
        ntlmTokenReceivedPageLoader = new PageLoader(ntlmTokenReceivedErrorPageURL, DEFAULT_NTLM_TOKEN_RECEIVED_PAGE_CONTENT);
    }

    public String getNTLMTokenReceivedPage() {
        return ntlmTokenReceivedPageLoader.getContent();
    }

    public String getNtlmTokenReceivedPageContentType() {
        return ntlmTokenReceivedPageLoader.getContentType();
    }

    public String getNtlmTokenReceivedPageCharset() {
        return ntlmTokenReceivedPageLoader.getEncoding();
    }

    public String getSpnegoNotSupportedPage() {
        return spnegoNotSupportedPageLoader.getContent();
    }

    public String getSpnegoNotSupportedPageContentType() {
        return spnegoNotSupportedPageLoader.getContentType();
    }

    public String getSpnegoNotSupportedPageCharset() {
        return spnegoNotSupportedPageLoader.getEncoding();
    }
}
