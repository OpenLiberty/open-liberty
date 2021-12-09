/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.fat.common.apps.amrbuilder;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

import java.io.IOException;
import java.io.PrintWriter;
import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.websphere.security.jwt.JwtBuilder;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatConstants;

/**
 * A servlet which to add security attributes
 */
public class AmrTestBuilderServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    public static final String PARAM_BUILDER_ID = "builder_id";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }

    protected void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();
        writer.println("ServletName: AmrBuilderServlet");

        StringBuffer sb = new StringBuffer();
        try {
            setSecurityAttribute(sb);
            JwtToken jwt = generateJwt(req);
            resp.addCookie(new Cookie(JwtFatConstants.JWT_COOKIE_NAME, jwt.compact()));
            writeLine(sb, "Built JWT claims" + JwtDecoder(jwt.compact()));
        } catch (NoClassDefFoundError ne) {
            // For OSGI App testing (EBA file), we expect this exception for all packages that are not public
            writeLine(sb, "NoClassDefFoundError for SubjectManager: " + ne);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        writer.write(sb.toString());
        writer.flush();
        writer.close();
    }

    protected void setSecurityAttribute(StringBuffer sb) throws Exception {
        WSCredential callerCredential = WSSubject.getCallerSubject().getPublicCredentials(WSCredential.class).iterator().next();
        if (callerCredential != null) {
            callerCredential.set("amrtest", "amrValue");
            System.out.println("amrtest is set in WSCredential");
        } else {
            writeLine(sb, "callerCredential: null");
        }
    }

    private JwtToken generateJwt(HttpServletRequest request) throws Exception {
        System.out.println("Generating a JWT...");

        String builderConfigId = request.getParameter(PARAM_BUILDER_ID);
        System.out.println("Got builder config ID: [" + builderConfigId + "]");

        JwtToken jwt = buildJwt(builderConfigId);
        System.out.println("Built JWT claims" + JwtDecoder(jwt.compact()));
        return jwt;
    }

    private JwtToken buildJwt(String builderConfigId) throws Exception {
        JwtBuilder builder = JwtBuilder.create(builderConfigId);
        return builder.buildJwt();
    }

    /**
     * "Writes" the msg out to the client. This actually appends the msg
     * and a line delimiters to the running StringBuffer. This is necessary
     * because if too much data is written to the PrintWriter before the
     * logic is done, a flush() may get called and lock out changes to the
     * response.
     *
     * @param sb
     *            Running StringBuffer
     * @param msg
     *            Message to write
     */
    protected void writeLine(StringBuffer sb, String msg) {
        sb.append(msg + "\n");
    }

    protected String JwtDecoder(String jwt) throws UnsupportedEncodingException {
        //String jwt="eyJraWQiOiJxaWhZLW5lVHlKd0tNWG81c29yT3F3bkhCWHpvb2JrakJ3aXlkZnZvVzJzIiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJ0b2tlbl90eXBlIjoiQmVhcmVyIiwic3ViIjoib2lkY0xvZ2luVXNlciIsIm5hbWUiOiJvaWRjTG9naW5Vc2VyIiwiaXNzIjoiaHR0cHM6Ly8xMC4zNC4xLjQwOjgwMjAvand0L2dvb2RKd3RCdWlsZGVyIiwiZXhwIjoxNTc1NDc1NzM2LCJpYXQiOjE1NzU0Njg1MzZ9.j3bswEPqsOR4zVd97mf2E6aoU5A6A/rwIxcND8dgDEmUt7PsryV9YBVd0YpiWzRXMzgg1uPTKg2MHAG3smEGKo7pcuihWfdhyejW3pXiC2iTT/KrjGa8+2sCEolRTxbRRroNy2LvTPDF5qU4tPLSg2T91ZArTF4h4ZGsalX/+mcvgf1ATvvrEpo9dgsNJcijKsJWf6NsucI85fluAmHskkqNfTWB+uJIUG21O/qyZoArTs8Q7YD1jjplaN4Fg7Hp7RO6ngcYvlEp9z0WVV70CGKE58n0NFNo6uKVGYdmLU3DIIF/VOyhZU6G68zosu0WQuVWPdrC/p5NZNUeSOuXqA";
        String[] parts = jwt.split("\\.");       
        String ENCODING="UTF-8";
        byte[] ba = parts[1].getBytes(ENCODING);
        return new String(Base64.getDecoder().decode(ba));    
    }

}
