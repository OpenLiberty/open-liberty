/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.saml.sso20.metadata;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoHandler;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;

public class MetadataHandler implements SsoHandler {
    private static TraceComponent tc = Tr.register(MetadataHandler.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);

    public MetadataHandler() {
    }

    @Override
    public Constants.SamlSsoVersion getSamlVersion() {
        return Constants.SamlSsoVersion.SAMLSSO20;
    }

    @Override
    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response,
                              SsoRequest samlRequest,
                              Map<String, Object> parameters) throws SamlException {
        SsoSamlService samlService = (SsoSamlService) parameters.get(Constants.KEY_SAML_SERVICE);
        SsoConfig samlConfig = samlService.getConfig();
        String filename = "spMetadata.xml";
        SecurityService securityService = (SecurityService) parameters.get(Constants.KEY_SECURITY_SERVICE);
        if (tc.isDebugEnabled()) {
            String authFilterId = samlConfig.getAuthFilterId();
            Tr.debug(tc, "handleRequest(Metadata):" +
                         " providerId:" + samlService.getProviderId() +
                         " request:" + request +
                         " response:" + response +
                         " samlRequest:" + samlRequest +
                         " samlService:" + samlService +
                         " securityService:" + securityService +
                         " authFilterId:" + authFilterId);
        }

        try {
            response.setContentType("text/xml");
            response.setHeader("Content-Disposition", "attachment;filename=\""
                                                      + filename + "\"");

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), response.getCharacterEncoding()));

            SpMetadataBuilder spMetadataBuilder = new SpMetadataBuilder(samlService);
            String metadataData = spMetadataBuilder.buildSpMetadata(request);

            bw.write(metadataData);
            bw.flush();
            bw.close();

        } catch (IOException e) {
            // TODO: This is not implemented yet
        }

    }

}
