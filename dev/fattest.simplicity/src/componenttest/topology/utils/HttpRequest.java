package componenttest.topology.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.net.ssl.HttpsURLConnection;

import componenttest.topology.impl.LibertyServer;

public class HttpRequest {

    private final String url;
    private final Set<Integer> expectedResponseCode = new HashSet<Integer>();
    private String reqMethod = "GET";
    private String json = null;
    private String basicAuth = null;
    private final Map<String, String> props = new HashMap<String, String>();
    private Integer timeout;
    private boolean silent = false;
    private int responseCode = -1;

    public HttpRequest(String url) {
        this.url = url;
    }

    public HttpRequest(LibertyServer server, String... pathParts) {
        String base = "http://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort();
        for (String part : pathParts)
            base += part;
        this.url = base;
    }

    /**
     * The HTTP request method. Default method is GET.
     */
    public HttpRequest method(String method) {
        this.reqMethod = method;
        return this;
    }

    /**
     * Add a HTTP request property name and value using HttpUrlConnection.setRequestProperty()
     */
    public HttpRequest requestProp(String key, String value) {
        props.put(key, value);
        return this;
    }

    /**
     * Set the expected response code. Default is HTTP_OK
     */
    public HttpRequest expectCode(int expectedResponse) {
        this.expectedResponseCode.add(expectedResponse);

        return this;
    }

    /**
     * Set the json data to send with the request.
     */
    public HttpRequest jsonBody(String json) {
        this.json = json;
        return this;
    }

    public HttpRequest timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public HttpRequest silent() {
        this.silent = true;
        return this;
    }

    public HttpRequest basicAuth(String user, String pass) {
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
        if (!silent) {
            System.out.println(reqMethod + ' ' + url);
        }

        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();

        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestMethod(reqMethod);

            if ("GET".equals(con.getRequestMethod()) && json != null) {
                throw new IllegalStateException("Writing a JSON body to a GET request will force the connection to be switched to a POST request at the JDK layer.");
            }

            if (json != null) {
                con.setRequestProperty("Content-Type", "application/json");
                OutputStream out = con.getOutputStream(); //This line will change a GET request to a POST request
                out.write(json.getBytes("UTF-8"));
                out.close();
            } else {
                con.setRequestProperty("Content-Type", "text/html");
            }

            if (type.getPackage().toString().startsWith("javax.json"))
                con.setRequestProperty("Accept", "application/json");

            if (basicAuth != null)
                con.setRequestProperty("Authorization", basicAuth);

            if (props != null)
                for (Map.Entry<String, String> entry : props.entrySet())
                    con.setRequestProperty(entry.getKey(), entry.getValue());

            if (timeout != null) {
                con.setConnectTimeout(timeout);
                con.setReadTimeout(timeout);
            }

            responseCode = con.getResponseCode();
            if (expectedResponseCode.isEmpty()) {
                expectCode(HttpsURLConnection.HTTP_OK);
            }
            if (!expectedResponseCode.contains(responseCode)) {
                throw new Exception("Unexpected response (See HTTP_* constant values on HttpURLConnection): " + responseCode);
            }
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
            } else if (con.getErrorStream() != null) {
                if (JsonArray.class.equals(type))
                    return type.cast(Json.createReader(con.getErrorStream()).readArray());
                else if (JsonObject.class.equals(type))
                    return type.cast(Json.createReader(con.getErrorStream()).readObject());
                else if (JsonStructure.class.equals(type))
                    return type.cast(Json.createReader(con.getErrorStream()).read());
                else if (String.class.equals(type)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    InputStream in = con.getErrorStream();
                    int numBytesRead;
                    for (byte[] b = new byte[8192]; (numBytesRead = in.read(b)) != -1;)
                        out.write(b, 0, numBytesRead);
                    in.close();
                    return type.cast(out.toString("UTF-8"));
                } else
                    throw new IllegalArgumentException(type.getName());
            } else {
                return null;
            }
        } finally {
            con.disconnect();
        }
    }

    public int getResponseCode() {
        return responseCode;
    }
}
