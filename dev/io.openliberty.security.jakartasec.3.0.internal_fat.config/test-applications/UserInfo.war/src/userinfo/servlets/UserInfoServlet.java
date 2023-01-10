/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package userinfo.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/UserInfo")
public class UserInfoServlet extends HttpServlet {

    private static final long serialVersionUID = -417476984908088827L;

    private final String clientSecret = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger";

    String getAccessToken(HttpServletRequest request) throws Exception {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            throw new Exception("Missing Authorization header in request.");
        }
        if (!authHeader.startsWith("Bearer ")) {
            throw new Exception("Authorization header in request does not contain a bearer token: [" + authHeader + "].");
        }
        return authHeader.substring("Bearer ".length());
    }

    String createJwtResponse(String accessToken) throws Exception {
        return getHS256Jws(clientSecret, accessToken);
    }

    void writeResponse(HttpServletResponse response, String returnString, String format) throws IOException {
        String cacheControlValue = response.getHeader("Cache-Control");
        if (cacheControlValue != null &&
            !cacheControlValue.isEmpty()) {
            cacheControlValue = cacheControlValue + ", " + "no-store";
        } else {
            cacheControlValue = "no-store";
        }
        response.setHeader("Cache-Control", cacheControlValue);
        response.setHeader("Pragma", "no-cache");

        response.setContentType("application/" + format);
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter pw;
        pw = response.getWriter();
        System.out.println("userinfo returning userinfo (in format " + format + ") : " + returnString);
        pw.write(returnString);
        pw.flush();
    }

    private String getHS256Jws(String secret, String accessToken) throws Exception {
        String headerAndPayload = encode(getHS256Header()) + "." + encode(getMinimumClaims(accessToken));
        String signature = getHS256Signature(headerAndPayload, secret);
        return headerAndPayload + "." + signature;
    }

    private JsonObject getHS256Header() {
        return getJwsHeader("HS256");
    }

    private JsonObject getJwsHeader(String alg) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("typ", "JWT");
        builder.add("alg", alg);
        return builder.build();
    }

    protected String getGroupId() {
        return "groupIds";
    }

    protected JsonArray createGroups() {
        System.out.println("UserInfoServlet createGroups");

        return Json.createArrayBuilder().add("all").add("group1").add("group2").add("group3").build();
    }

    protected String getSub() {
        return "sub";
    }

    protected String createSub() {
        System.out.println("UserInfoServlet createSub");

        return "testuser";
    }

    protected String getName() {
        return "name";
    }

    protected String createName() {
        System.out.println("UserInfoServlet createName");

        return "testuser";
    }

    protected JsonObject getMinimumClaims(String accessToken) {
        JsonObjectBuilder claims = Json.createObjectBuilder();
        claims.add(getSub(), createSub());
        claims.add(getGroupId(), createGroups());
        // Hard coded port is ok here since we won't use the iss from userinfo
        claims.add("iss", "https://localhost:8920/oidc/endpoint/OP1");
        claims.add(getName(), createName());
        claims.add("access_token", accessToken);
        System.out.println("getMinimumClaims - claims: " + claims.toString());
        return claims.build();
    }

    private String getHS256Signature(String input, String secret) throws Exception {
        byte[] secretBytes = secret.getBytes("UTF-8");
        Mac hs256Mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secretBytes, "HmacSHA256");
        hs256Mac.init(keySpec);
        byte[] hashBytes = hs256Mac.doFinal(input.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hashBytes);
    }

    private String encode(Object input) throws UnsupportedEncodingException {
        return Base64.getEncoder().encodeToString(input.toString().getBytes("UTF-8"));
    }

    protected void recordWhichApp() {

        System.out.println(ServletMessageConstants.USERINFO + getShortName(this.getClass().getSuperclass().getName()));
        System.out.println(ServletMessageConstants.USERINFO + getShortName(this.getClass().getName()));

    }

    protected String getShortName(String longClassName) {

        if (longClassName != null) {
            String[] splitClassName = longClassName.split("\\.");
            return splitClassName[splitClassName.length - 1];
        }
        return null;

    }
}
