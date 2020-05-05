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
package com.ibm.ws.security.saml.sso20.internal.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.impl.KnownSamlUrl;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

/**
 *
 */
public class InitialRequestUtil extends KnownSamlUrl {
    private static final TraceComponent tc = Tr.register(InitialRequestUtil.class,
                                                         TraceConstants.TRACE_GROUP,
                                                         TraceConstants.MESSAGE_BUNDLE);

    /**
     * @param string
     */
    public String updateInitialRequestCookieNameWithRelayState(String append) {

        return (Constants.WAS_IR_COOKIE + append);
    }

    /**
     * @param irBytes
     * @param ssoService
     * @return
     */
    public String digestInitialRequestCookieValue(String irBytes, SsoSamlService ssoService) {

        String retVal = new String(irBytes);
        String key_alias_pass = "samlsp";
        String default_ks_pass = ssoService.getDefaultKeyStorePassword();
        if (default_ks_pass != null) {
            key_alias_pass.concat(default_ks_pass);
        }

        String tmpStr = new String(irBytes);
        tmpStr = tmpStr.concat("_").concat(key_alias_pass);
        retVal = retVal.concat("_").concat(com.ibm.ws.security.saml.sso20.internal.utils.HashUtils.digest(tmpStr)); // digest of serialized initial request and default keystore password

        return retVal;
    }

    /**
     * @param initialrequest_cookie_value
     */
    @FFDCIgnore({ IndexOutOfBoundsException.class })
    public String getInitialRequestCookie(String initialrequest_cookie_value, SsoSamlService ssoservice) {
        String initial = null;

        try {
            int lastindex = initialrequest_cookie_value.lastIndexOf("_");
            if (lastindex < 1) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "The cookie may have been tampered with.");
                    if (lastindex < 0) {
                        Tr.debug(tc, "The cookie does not contain an underscore.");
                    }
                    if (lastindex == 0) {
                        Tr.debug(tc, "The cookie does not contain a value before the underscore.");
                    }
                }
            }
            initial = initialrequest_cookie_value.substring(0, lastindex);
            String testCookie = digestInitialRequestCookieValue(initial, ssoservice);

            if (!initialrequest_cookie_value.equals(testCookie)) {
                String msg = "The value for the inital request cookie [" + "cookieName" + "] failed validation.";
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, msg);
                }
                initial = null;
            }
        } catch (IndexOutOfBoundsException e) {
            // anything wrong indicated the requestParameter cookie is not right or is not in right format
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "unexpected exception:", e);
            }
        }
        return initial;
    }

    /**
     * @param ir
     */
    public HttpRequestInfo createHttpRequestInfoFromInitialRequest(InitialRequest ir) {

        HttpRequestInfo httprequestinfo = null;
        if (ir.getRequestUrlWithEncodedQueryString() != null) {
            httprequestinfo = new HttpRequestInfo(ir.getRequestUrl(), ir.getRequestUrlWithEncodedQueryString(), ir.getMethod(), ir.getInResponseToId(), ir.getFormLogoutExitPage(), ir.getPostParamsMap());
        }
        return httprequestinfo;

    }

    /**
     * @param serializedInitialRequest
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @FFDCIgnore({ IOException.class })
    public InitialRequest handleDeserializingInitialRequest(String serializedInitialRequest) throws IOException, ClassNotFoundException {

        InitialRequest ir = null;
        if (serializedInitialRequest != null) {
            ByteArrayInputStream bis = new ByteArrayInputStream(serializedInitialRequest.getBytes("UTF-8"));
            ObjectInput in = null;

            try {
                in = new ObjectInputStream(bis);
                ir = (InitialRequest) in.readObject();

            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {
                    // ignore close exception
                }
            }
        }

        return ir;
    }

    /**
     * @param relayState
     * @throws SamlException
     */
    @FFDCIgnore({ IOException.class, ClassNotFoundException.class })
    public HttpRequestInfo recreateHttpRequestInfo(String relayState, HttpServletRequest request, HttpServletResponse response, SsoSamlService ssoService) throws SamlException {
        HttpRequestInfo requestInfo = null;

        String initialrequest_cookie_name = updateInitialRequestCookieNameWithRelayState(relayState);
        String initialrequest_cookie_value_digest = RequestUtil.getCookieId((IExtendedRequest) request, response, initialrequest_cookie_name);
        String serializedInitialRequest = getInitialRequestCookie(initialrequest_cookie_value_digest, ssoService);
        InitialRequest ir = null;
        try {
            ir = handleDeserializingInitialRequest(serializedInitialRequest);
        } catch (ClassNotFoundException e) {
            throw new SamlException(e);
        } catch (IOException e) {
            throw new SamlException(e);
        }
        if (ir != null) {
            ir.setPostParamsMap(request);
            requestInfo = createHttpRequestInfoFromInitialRequest(ir);
        }

        return requestInfo;
    }

    /**
     * @param req 
     * @param resp 
     * @param targetId
     * @param targetId2 
     * @param ssoService
     */
    public String handleSerializingInitialRequest(HttpServletRequest req, HttpServletResponse resp, String targetId, SsoSamlService ssoService) {
       
        InitialRequest ir = null;
        //byte[] irBytes = null;
        String irBytes = null;
        try {
            ir = new InitialRequest(req);
        } catch (SamlException e) {
            
        }
       
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
          try {
            out = new ObjectOutputStream(bos);
            out.writeObject(ir);
            out.flush();
            irBytes = bos.toString("UTF-8");//bos.toByteArray();
        } catch (IOException e) {
           
        }     
         
        } finally {
          try {
            bos.close();
          } catch (IOException ex) {
            
          }
        }
        if (irBytes != null) {
            String initialrequest_cookie_name = updateInitialRequestCookieNameWithRelayState(targetId);
            String initialrequest_cookie_value = digestInitialRequestCookieValue(irBytes, ssoService);
            RequestUtil.createCookie(req, resp, initialrequest_cookie_name, initialrequest_cookie_value);
        }
        return irBytes;
    }

}
