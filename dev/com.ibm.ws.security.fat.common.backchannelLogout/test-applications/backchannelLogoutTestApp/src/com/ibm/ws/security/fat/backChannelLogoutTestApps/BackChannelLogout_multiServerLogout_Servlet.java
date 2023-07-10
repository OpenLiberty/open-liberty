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
package com.ibm.ws.security.fat.backChannelLogoutTestApps;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.security.fat.backChannelLogoutTestApps.utils.BackChannelLogout_SidAndEndpointKeeper;

public class BackChannelLogout_multiServerLogout_Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final String logoutTokenParm = "logout_token";
    private final String ClearLoginSids = "clear_login_sids";
    private final String Sid = "sid";
    private final String BclEndpoint = "bcl_endpoint";

    //    private static Map<String, String> sidMap = new HashMap<String, String>();

    private static ArrayList<BackChannelLogout_SidAndEndpointKeeper> sidKeeper;

    public BackChannelLogout_multiServerLogout_Servlet() {
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //        Map<String, String[]> parms = req.getParameterMap();
        //        for (Map.Entry<String, String[]> entry : parms.entrySet()) {
        //            System.out.println("Parm: " + entry.getKey() + " value: " + entry.getValue());
        //        }

        String clear = req.getParameter(ClearLoginSids);
        if (clear != null && clear.equals("true")) {
            System.out.println("Clear sids and bcl endpoints");
            //            sidMap = new HashMap<String, String>();
            sidKeeper = new ArrayList<BackChannelLogout_SidAndEndpointKeeper>();
        } else {
            String sid = req.getParameter(Sid);
            String bclEndpoint = req.getParameter(BclEndpoint);
            System.out.println("Save new sid (" + sid + ") and bcl endpoint (" + bclEndpoint + ")");

            //            addToMap(sid, bclEndpoint);
            addToList(sid, bclEndpoint);
            //            System.out.println("Before Map has " + sidMap.size() + " entries");
            //            sidMap.put(sid, bclEndpoint);
            //            System.out.println("After Map has " + sidMap.size() + " entries");
            //            for (Map.Entry<String, String> entry : sidMap.entrySet()) {
            //                System.out.println("Map: " + entry.getKey() + " value: " + entry.getValue());
            //            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("Get");
        handleRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("Post");
        handleRequest(req, resp);
    }

    private synchronized void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String[]> parms = req.getParameterMap();
        //        for (Map.Entry<String, String[]> entry : parms.entrySet()) {
        //            System.out.println("Parm: " + entry.getKey() + " value: " + entry.getValue());
        //        }

        //        PrintWriter writer = resp.getWriter();
        //        if (sidMap.isEmpty()) {
        //            System.out.println("There were no entries in the sidMap");
        //            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        //            return;
        //        }
        //
        //        System.out.println("There were no entries in the sidKeeper");
        if (sidKeeper.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String logoutToken = req.getParameter(logoutTokenParm);

        //        if (newLogoutToken != null) {
        //            logoutToken = newLogoutToken;
        //            System.out.println("Saving new logout_token.");
        //        } else {
        //            System.out.println("NO logout_token - we'll return the logout_token saved on the previous call which should have been from the back channel request.");
        //        }

        if (logoutToken == null) {
            System.out.println("backChannelLogoutMultiServer - logout_token: NOT SET");
            //            writer.println("logout_token: NOT SET");
        } else {
            System.out.println("backChannelLogoutMultiServer - logout_token: " + logoutToken);
            try {
                String[] jwtParts = logoutToken.split(Pattern.quote("."));
                //                System.out.println("jwtParts[1]: " + jwtParts[1]);
                String decodedPayload = new String(Base64.getDecoder().decode(jwtParts[1]), "UTF-8");
                //                System.out.println("decoded: " + decodedPayload);
                JsonObject jwtPayloadJson = Json.createReader(new StringReader(decodedPayload)).readObject();

                //                Set<String> payloadKeys = jwtPayloadJson.keySet();
                String sid = jwtPayloadJson.get("sid").toString().replace("\"", "");
                //                                for (String key : payloadKeys) {
                //                                    pw.println("Header: Key: " + key + " value: " + jwtPayloadJson.get(key));
                //                                }

                //                JsonObject jwtPayloadJson = Json.createReader(new StringReader(StringUtils.newStringUtf8(Base64.decodeBase64(jwtParts[2])))).readObject();
                //                String sid = jwtPayloadJson.get("sid").toString();
                //                System.out.println("jwtPayloadJson: " + jwtPayloadJson.toString());
                //                System.out.println("sid: " + sid);
                //                String theEntry = sidMap.get(sid);

                for (int i = 0; i < sidKeeper.size(); i++) {
                    if ((sidKeeper.get(i).getSid()).equals(sid)) {
                        System.out.println("Keeper: " + sidKeeper.get(i).getSid() + " value: " + sidKeeper.get(i).getBclEndpoint());
                        String endpoint = sidKeeper.get(i).getBclEndpoint();
                        if (endpoint != null) {
                            System.out.print("For sid: " + sidKeeper.get(i).getSid() + " - Will invoke " + endpoint + " with the passed in logout_token: " + logoutToken);
                            System.out.println("");
                            StringBuffer sb = new StringBuffer("");
                            sb.append(endpoint);
                            //                    for (Map.Entry<String, String[]> entry : parms.entrySet()) {
                            ////                        System.out.println("Parm: " + entry.getKey() + " value: " + entry.getValue());
                            //                        sb.append("?" + entry.getKey() + "=" + entry.getValue());
                            //                    }
                            sb.append("?" + logoutTokenParm + "=" + logoutToken);
                            String url = sb.toString();
                            System.out.println("Redirect: " + url);
                            //                    String urlEncoded = resp.encodeRedirectURL(url);

                            sendPostRedirect(url);

                            //                        } else {
                            //                            System.out.println("A bcl endpoint was not saved for sid: " + sid);
                        }

                    }
                }
                //
                //                Set<Entry<String, JsonValue>> set = jwtPayloadJson.entrySet();
                //                set.get
                //
                //
                //                JwtTokenForTest jwtToken = new JwtTokenForTest(logoutToken);
                //                System.out.println("Payload: " + jwtToken.getJsonPayload());

                //                if (theEntry != null) {
                //                    //                    Encoder foo = Base64.getEncoder() ;
                //                    //                    foo.encode(logoutToken.getBytes())
                //                    System.out.print("Will invoke " + theEntry + " with the passed in logoutToken: " + logoutToken);
                //                    System.out.println("");
                //                    StringBuffer sb = new StringBuffer("");
                //                    sb.append(theEntry);
                //                    //                    for (Map.Entry<String, String[]> entry : parms.entrySet()) {
                //                    ////                        System.out.println("Parm: " + entry.getKey() + " value: " + entry.getValue());
                //                    //                        sb.append("?" + entry.getKey() + "=" + entry.getValue());
                //                    //                    }
                //                    sb.append("?" + logoutTokenParm + "=" + logoutToken);
                //                    String url = sb.toString();
                //                    System.out.println("Redirect: " + url);
                //                    //                    String urlEncoded = resp.encodeRedirectURL(url);
                //
                //                    sendPostRedirect(url);
                //                    //                    sendPostRedirect(theEntry, logoutToken);
                //                    //                    resp.sendRedirect(urlEncoded);
                //                } else {
                //                    System.out.println("A bcl endpoint was not saved for sid: " + sid);
                //                }

            } catch (Exception e) {
                throw new ServletException("Error parsing the logout token: " + e.getMessage());
            }
            //            writer.println("logout_token: " + logoutToken);
        }

        //        writer.flush();
        //        writer.close();
    }

    //    private synchronized void sendPostRedirect(String url, String logoutToken) throws ServletException, IOException, Exception {
    private synchronized void sendPostRedirect(String url) throws ServletException, IOException, Exception {

        String method = "POST";
        //        String output = null;
        //        InputStream inputStream = null;
        //        InputStream errorStream = null;
        //        OutputStream outputStream = null;
        //        HashMap<String, String> returnMap = new HashMap<String, String>();
        //        String userPass = clientId + ":" + clientSecret;
        //        String userPassBase64 = Base64.encode(userPass.getBytes());
        //
        //        String basicAuth = "Basic " + userPassBase64;
        //        sb.append("\n " + basicAuth + " userPass:" + userPass);
        //        sb.append("\n content:" + contents);

        try {

            HttpURLConnection connection = getSecuredConnection(method, url);

            connection.setDoOutput(true);
            //            if (contents != null) {
            //                connection.setDoInput(true);
            //            }
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod(method);
            connection.setConnectTimeout(60 * 10000);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

            //            connection.setRequestProperty(logoutTokenParm, logoutToken);
            //          connection.setRequestProperty("Authorization", basicAuth);

            connection.connect();

            //            if (contents != null) {
            //                outputStream = connection.getOutputStream();
            //                outputStream.write(contents.getBytes());
            //                outputStream.flush();
            //            }

            int responseCode = connection.getResponseCode();
            System.out.println("Response code: " + responseCode);

        } catch (IOException e) {

            throw new Exception("Failed to make a request to OP server", e);
        }
        //        URL obj = new URL(url);
        //        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        //        con.setConnectTimeout(60000);
        //        con.setRequestMethod("POST");
        //        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        //        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        //
        //        // For post only - start
        //        con.setDoOutput(true);
        //        OutputStream os = con.getOutputStream();
        //        os.write(("?auth=ssor&TransportKey=" + ssorTransportKey).getBytes());
        //        os.flush();
        //        os.close();
        //
        //        int responseCode = con.getResponseCode();
    }

    protected static HttpsURLConnection getSecuredConnection(String method, String opUrl) throws IOException {

        URL aurl = new URL(opUrl);
        HttpsURLConnection conn = (HttpsURLConnection) aurl.openConnection();

        return conn;
    }

    //    protected void addToMap(String sid, String bclEndpoint) throws IOException {
    //
    //        System.out.println("Before Map has " + sidMap.size() + " entries");
    //        sidMap.put(sid, bclEndpoint);
    //        System.out.println("After Map has " + sidMap.size() + " entries");
    //        for (Map.Entry<String, String> entry : sidMap.entrySet()) {
    //            System.out.println("Map: " + entry.getKey() + " value: " + entry.getValue());
    //        }
    //
    //    }
    //
    protected void addToList(String sid, String bclEndpoint) throws IOException {

        System.out.println("Before Map has " + sidKeeper.size() + " entries");
        sidKeeper.add(new BackChannelLogout_SidAndEndpointKeeper(sid, bclEndpoint));
        System.out.println("After Map has " + sidKeeper.size() + " entries");
        for (int i = 0; i < sidKeeper.size(); i++) {
            System.out.println("Keeper: " + sidKeeper.get(i).getSid() + " value: " + sidKeeper.get(i).getBclEndpoint());
        }

    }
}
