/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package discovery.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/.well-known/openid-configuration")
public class DiscoveryServlet extends HttpServlet {

    private String discData = null;

    private static final long serialVersionUID = -217476984908088827L;

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        System.out.println("In Discovery to save token.");

        Map<String, String[]> parms = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parms.entrySet()) {
            System.out.println("Parm: " + entry.getKey() + " value: " + entry.getValue());
        }
        discData = request.getParameter("UpdatedDiscoveryData");

        System.out.println("Saving updated discovery data: " + discData);

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // ServletOutputStream ps = response.getOutputStream();

        System.out.println("In Discovery override");

//        Enumeration<String> headers = request.getHeaderNames();
//        System.out.println("headers: " + headers.toString());
//
//        Map<String, String[]> parms = request.getParameterMap();
//        System.out.println("Parm set size: " + parms.size());
//        for (Entry<String, String[]> parm : parms.entrySet()) {
//            System.out.println("Discovery: parm: " + parm.getKey() + " value: " + parm.getValue());
//        }
//
//        response.sendRedirect("https://localhost:8920/oidc/endpoint/OP1/.well-known/openid-configuration");
//
//        System.out.println("In Discovery override - status is: " + response.getStatus());
//
//        System.out.println(response.getContentType());
//        if (response.getContentType() != null && response.getContentType().contains("application/json")) {
//            System.out.println("Have Json response");
//        }
//        System.out.println(response.toString());
//
//        ServletOutputStream foo = response.getOutputStream();
//        System.out.println(foo.toString());

//        httpUtils.getHttpJsonRequest(sslSocketFactory, "https://localhost:8920/oidc/endpoint/OP1/.well-known/openid-configuration", false, true);
//        JsonObject testDiscoveryResponseContent = addUserInfoSigAlg();

        writeResponse(response, discData, "json");
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
        System.out.println("Discovery returning discovery (in format " + format + ") : " + returnString);
        pw.write(returnString);
        pw.flush();
    }

    protected JsonObject addUserInfoSigAlg() {
        JsonObjectBuilder updatedDiscData = Json.createObjectBuilder();
        JsonObject updatedresponse = null;

        try {
            updatedDiscData.add("introspection_endpoint", "https://localhost:8920/oidc/endpoint/OP19/introspect");
            updatedDiscData.add("coverage_map_endpoint", "https://localhost:8920/oidc/endpoint/OP1/coverage_map");
            updatedDiscData.add("issuer", "https://localhost:8920/oidc/endpoint/OP1");
            updatedDiscData.add("authorization_endpoint", "https://localhost:8920/oidc/endpoint/OP1/authorize");
            updatedDiscData.add("token_endpoint", "https://localhost:8920/oidc/endpoint/OP1/token");
            updatedDiscData.add("jwks_uri", "https://localhost:8920/oidc/endpoint/OP1/jwk");
            updatedDiscData.add("response_types_supported", Json.createArrayBuilder().add("code").add("token").add("id_token token"));
            updatedDiscData.add("subject_types_supported", Json.createArrayBuilder().add("public"));
            updatedDiscData.add("id_token_signing_alg_values_supported", Json.createArrayBuilder().add("RS256"));
            updatedDiscData.add("userinfo_endpoint", "https://localhost:8920/oidc/endpoint/OP1/userinfo");
            updatedDiscData.add("registration_endpoint", "https://localhost:8920/oidc/endpoint/OP1/registration");
            updatedDiscData.add("scopes_supported", Json.createArrayBuilder().add("openid").add("general").add("profile").add("email").add("address").add("phone"));
            updatedDiscData.add("claims_supported",
                                Json.createArrayBuilder().add("sub").add("groupIds").add("name").add("preferred_username").add("picture").add("locale").add("email").add("profile"));
            updatedDiscData.add("response_modes_supported", Json.createArrayBuilder().add("query").add("fragment").add("form_post"));
            updatedDiscData.add("grant_types_supported",
                                Json.createArrayBuilder().add("authorization_code").add("implicit").add("refresh_token").add("client_credentials").add("password").add("urn:ietf:params:oauth:grant-type:jwt-bearer"));
            updatedDiscData.add("token_endpoint_auth_methods_supported", Json.createArrayBuilder().add("client_secret_post").add("client_secret_basic"));
            updatedDiscData.add("display_values_supported", Json.createArrayBuilder().add("page"));
            updatedDiscData.add("claim_types_supported", Json.createArrayBuilder().add("normal"));
            updatedDiscData.add("claims_parameter_supported", false);
            updatedDiscData.add("request_parameter_supported", false);
            updatedDiscData.add("request_uri_parameter_supported", false);
            updatedDiscData.add("require_request_uri_registration", false);
            updatedDiscData.add("check_session_iframe", "https://localhost:8920/oidc/endpoint/OP1/check_session_iframe");
            updatedDiscData.add("end_session_endpoint", "https://localhost:8920/oidc/endpoint/OP1/end_session");
            updatedDiscData.add("revocation_endpoint", "https://localhost:8920/oidc/endpoint/OP1/revoke");
            updatedDiscData.add("app_passwords_endpoint", "https://localhost:8920/oidc/endpoint/OP1/app-passwords");
            updatedDiscData.add("app_tokens_endpoint", "https://localhost:8920/oidc/endpoint/OP1/app-tokens");
            updatedDiscData.add("personal_token_mgmt_endpoint", "https://localhost:8920/oidc/endpoint/OP1/personalTokenManagement");
            updatedDiscData.add("users_token_mgmt_endpoint", "https://localhost:8920/oidc/endpoint/OP1/usersTokenManagement");
            updatedDiscData.add("client_mgmt_endpoint", "https://localhost:8920/oidc/endpoint/OP1/clientManagement");
            updatedDiscData.add("code_challenge_methods_supported", Json.createArrayBuilder().add("plain").add("S256"));
            updatedDiscData.add("backchannel_logout_supported", true);
            updatedDiscData.add("backchannel_logout_session_supported", true);
            updatedresponse = updatedDiscData.build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return updatedresponse;
    }

}
