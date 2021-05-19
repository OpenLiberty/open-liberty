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
package com.ibm.ws.security.saml.sso20.internal.utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.internal.encoder.Base64Coder;

import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;


/**
 *
 */
public class InitialRequest implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;
    
    private transient static final TraceComponent tc = Tr.register(InitialRequest.class,
                                                                   TraceConstants.TRACE_GROUP,
                                                                   TraceConstants.MESSAGE_BUNDLE);
   
    
    String reqUrl = null; // the pure requestURL without queries or fragments. Can be compared when restore
    String requestURL = null; // The requestURL with query string
    String method = null;
    String strInResponseToId;
    boolean isFormLogoutExitPage = false;
    String formLogoutExitPage = null;
    String postParams = "";
    HashMap savedPostParams = null;
     
    public static final String ATTRIB_HASH_MAP = "ServletRequestWrapperHashmap"; 
    public static final String COOKIE_NAME_SAVED_PARAMS = "WASSamlParams_IR_";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_GET = "GET";
    public static final int LENGTH_INT = 4;
    
    private static final int OFFSET_DATA = 1;
    private static final String CHARSET_NAME = "UTF-8";
    private static final int postParamSaveSize = 16000;

    
 public InitialRequest(HttpServletRequest request, String reqUrl, String requestURL, String method, String inResponseTo, String formlogout, HashMap savedPostParams) throws SamlException {
        
        this.reqUrl = reqUrl; 
        this.requestURL = requestURL; 
        this.method = method; 
        this.strInResponseToId = inResponseTo;
        this.formLogoutExitPage = formlogout; 
        if (this.formLogoutExitPage != null) {
            isFormLogoutExitPage = true;
        }
        if (METHOD_POST.equalsIgnoreCase(this.method) && this.formLogoutExitPage == null) {
            try {
                this.savedPostParams = savedPostParams;
                postParams = serializePostParams((IExtendedRequest)request);
            } catch (IllegalStateException | IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "An exception getting InputStreamData : ", new Object[] { e });
                }
                throw new SamlException(e);
            } 
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Request: method (" + this.method + ") savedParams:" + this.savedPostParams);
        }
    }

    public String getFormLogoutExitPage() {
        return this.formLogoutExitPage;
    }

    public String getInResponseToId() {
        return this.strInResponseToId;
    }

   
    public String getRequestUrl() {
        return this.reqUrl;
    }
    
    public String getRequestUrlWithEncodedQueryString() {
        return this.requestURL;
    }
    
    public String getMethod() {
        return this.method;
    }
    
    public HashMap getPostParamsMap() {
        return this.savedPostParams;
    }
    
    public void setPostParamsMap(HttpServletRequest request) {
        if (METHOD_POST.equalsIgnoreCase(this.method) && formLogoutExitPage == null) {
            try {
                this.savedPostParams = deserializePostParams(postParams.getBytes(CHARSET_NAME), (IExtendedRequest)request);
            } catch (IllegalStateException | IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "An exception getting InputStreamData : ", new Object[] { e });
                }
            } 
        }      
    }
 
    
    private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException 
    {       
        reqUrl = aInputStream.readUTF();
        requestURL = aInputStream.readUTF();
        method = aInputStream.readUTF();
        strInResponseToId = aInputStream.readUTF();
        isFormLogoutExitPage = aInputStream.readBoolean();
        if (isFormLogoutExitPage) {
            formLogoutExitPage = aInputStream.readUTF();
        } else if(METHOD_POST.equalsIgnoreCase(this.method)) {
            formLogoutExitPage = null;
            postParams = aInputStream.readUTF();
        }
    }
 
    private void writeObject(ObjectOutputStream aOutputStream) throws IOException 
    {
        aOutputStream.writeUTF(reqUrl);
        aOutputStream.writeUTF(requestURL);
        aOutputStream.writeUTF(method);
        aOutputStream.writeUTF(strInResponseToId);
        aOutputStream.writeBoolean(isFormLogoutExitPage);
        if (isFormLogoutExitPage) {
            aOutputStream.writeUTF(formLogoutExitPage);
        } else if(METHOD_POST.equalsIgnoreCase(this.method)) {
            aOutputStream.writeUTF(postParams);
        }
    }
    
    /**
     * serialize Post parameters.
     * The code doesn't expect that the req is null.
     * The format of the data is as follows:
     * <based64 encoded data[0] from serializeInputStreamData()>.<base 64 encoded data[1]>. and so on.
     */
    public String serializePostParams(IExtendedRequest req) throws IOException, UnsupportedEncodingException, IllegalStateException {
            String output = null;
            
            if (this.savedPostParams != null) {
                    long size = req.sizeInputStreamData(this.savedPostParams);                 
                    long total = size + LENGTH_INT;
                    
                    //long postParamSaveSize = webAppSecurityConfig.getPostParamCookieSize();
                    if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "length:" + total + "  maximum length:" + postParamSaveSize);
                    }

                    if (total < postParamSaveSize) { // give up to enocde if the data is too large (>16K)
                            byte[][] data = req.serializeInputStreamData(this.savedPostParams);
                            StringBuffer sb = new StringBuffer();
                            
                            for (int i = 0; i < data.length; i++) {
                                    if (i != 0) {
                                        sb.append(".");
                                    }
                                    sb.append(toStringFromByteArray(Base64Coder.base64Encode(data[i])));
                            }
                            output = sb.toString();
                            if (tc.isDebugEnabled()) {
                                    Tr.debug(tc, "encoded length:" + output.length());
                            }

                    }
                    if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "encoded POST parameters: " + output);
                    }
            }
            return output;
    }

    /**
     * deserialize Post parameters.
     * The code doesn't expect that the req, cookieValueBytes, or reqURL is null.
     */
    private HashMap deserializePostParams(byte[] paramsbytes, IExtendedRequest req) throws IOException, UnsupportedEncodingException, IllegalStateException {
        HashMap output = null;
        List<byte[]> data = splitBytes(paramsbytes, (byte) '.');
        int total = data.size();
        if (total >= OFFSET_DATA) {
            byte[][] bytes = new byte[total][];
            for (int i = 0; i < total; i++) {
                bytes[i] = Base64Coder.base64Decode(data.get(i));
            }
            output = req.deserializeInputStreamData(bytes);
        } else {
            throw new IllegalStateException("The data of the post param cookie is too short. The data might be truncated.");
        }
        return output;
    }

    /**
     * split the byte array with the specified delimiter. the expectation here is tha the byte array is a byte representation
     * of cookie value which was base64 encoded data.
     */
    private List<byte[]> splitBytes(byte[] array, byte delimiter) {
        List<byte[]> byteArrays = new ArrayList<byte[]>();
        int begin = 0;

        for (int i = 0; i < array.length; i++) {
            // first find delimiter.
            while (i < array.length && array[i] != delimiter) {
                i++;
            }
            byteArrays.add(Arrays.copyOfRange(array, begin, i));
            begin = i + 1;
        }
        return byteArrays;
    }

    public static String toStringFromByteArray(byte[] b) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0, len = b.length; i < len; i++) {
            sb.append((char) (b[i] & 0xff));
        }
        String str = sb.toString();
        return str;
    }
   
}
