/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.net.ssl.HttpsURLConnection;

import componenttest.topology.impl.LibertyServer;

public class HttpsRequest {

    private final String url;
    private int expectedResponseCode = HttpsURLConnection.HTTP_OK;
    private String reqMethod = "GET";
    private String json = null;
    private String basicAuth = null;
    private final Map<String, String> props = new HashMap<String, String>();

    public HttpsRequest(String url) {
        this.url = url;
    }

    public HttpsRequest(LibertyServer server, String... pathParts) {
        String base = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort();
        for (String part : pathParts)
            base += part;
        this.url = base;
    }

    /**
     * The HTTP request method. Default method is GET.
     */
    public HttpsRequest method(String method) {
        this.reqMethod = method;
        return this;
    }

    /**
     * Add a HTTP request property name and value using HttpUrlConnection.setRequestProperty()
     */
    public HttpsRequest requestProp(String key, String value) {
        props.put(key, value);
        return this;
    }

    /**
     * Set the expected response code. Default is HTTP_OK
     */
    public HttpsRequest expectCode(int expectedResponse) {
        this.expectedResponseCode = expectedResponse;
        return this;
    }

    /**
     * Set the json data to send with the request.
     */
    public HttpsRequest jsonBody(String json) {
        this.json = json;
        return this;
    }

    public HttpsRequest basicAuth(String user, String pass) {
        try {
            String userPass = user + ':' + pass;
            String base64Auth = javax.xml.bind.DatatypeConverter.printBase64Binary((userPass).getBytes("UTF-8"));
            this.basicAuth = "Basic " + base64Auth;
        } catch (UnsupportedEncodingException e) {
            // nothing to be done
        }
        return this;
    }

    /**
     * Make an HTTPS request and receive the response as the specified type.
     * The following types are valid parameters:
     * <ul>
     * <li>java.lang.String</li>
     * <li>javax.json.JsonArray</li>
     * <li>javax.json.JsonObject</li>
     * <li>javax.json.JsonStructure</li>
     * </ul>
     */
    public <T> T run(Class<T> type) throws Exception {
        System.out.println(reqMethod + ' ' + url);

        HttpsURLConnection con = (HttpsURLConnection) new URL(url).openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestMethod(reqMethod);

            if (type.getPackage().equals("javax.json"))
                con.setRequestProperty("Content-Type", "application/json");
            else
                con.setRequestProperty("Content-Type", "text/html");

            if (json != null) {
                con.setRequestProperty("Accept", "application/json");
                OutputStream out = con.getOutputStream();
                out.write(json.getBytes("UTF-8"));
                out.close();
            }

            if (basicAuth != null)
                con.setRequestProperty("Authorization", basicAuth);

            if (props != null)
                for (Map.Entry<String, String> entry : props.entrySet())
                    con.setRequestProperty(entry.getKey(), entry.getValue());

            int responseCode = con.getResponseCode();
            if (responseCode != expectedResponseCode)
                throw new Exception("Unexpected response (See HTTP_* constant values on HttpURLConnection): " + responseCode);

            if (responseCode / 100 == 2) { // response codes in the 200s mean success
                if (JsonArray.class.equals(type))
                    return type.cast(Json.createReader(con.getInputStream()).readArray());
                else if (JsonObject.class.equals(type))
                    return type.cast(Json.createReader(con.getInputStream()).readObject());
                else if (JsonStructure.class.equals(type))
                    return type.cast(Json.createReader(con.getInputStream()).read());
                else if (String.class.equals(type)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    InputStream in = con.getInputStream();
                    int numBytesRead;
                    for (byte[] b = new byte[8192]; (numBytesRead = in.read(b)) != -1;)
                        out.write(b, 0, numBytesRead);
                    in.close();
                    return type.cast(out.toString("UTF-8"));
                } else
                    throw new IllegalArgumentException(type.getName());
            } else
                return null;
        } finally {
            con.disconnect();
        }
    }
}
