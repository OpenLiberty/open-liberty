/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 *
 */
public class manual_IntrospectRequester {
    //import com.ibm.ws.security.oauth20.util.Base64;
    private static final String Authorization_Header = "Authorization";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String BEARER = "Bearer "; //yes, white space is required
    static String JKS_LOCATION = "./securitykeys/commonSslClientDefault.jks";

    protected static TrustManager[] trustAllCerts = null;
    static {
        //setupSSLClient();
    }

    String clientId = null;
    String clientSecret = null;
    String opUrl = null;
    String accessToken = null;
    String tokenType = null;
    HttpServletRequest req = null;
    HttpServletResponse res = null;

    public manual_IntrospectRequester(HttpServletRequest req, HttpServletResponse res,
            String clientId, String clientSecret,
            String opUrl,
            String accessToken, String tokenType) {
        this.req = req;
        this.res = res;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.opUrl = opUrl;
        this.accessToken = accessToken;
        this.tokenType = tokenType;
    }

    public void submitIntrospect(StringBuffer sb) throws WebTrustAssociationFailedException {

        String contents = "token=" + accessToken;

        Map<String, String> responseMap = null;

        try {
            responseMap = postToIntrospectionEndpoint(opUrl, contents, clientId, clientSecret, sb);
            String returnCode = responseMap.get("responseCode");
            String response = responseMap.get("responseMsg");

            sb.append("\n\n\nCalled introspect");
            sb.append("\n  TokenIntrospection returnCode:" + returnCode);
            sb.append("\n  TokenIntrospection responseMessage:" + response);

            System.out.println("TokenIntrospection returnCode:" + returnCode);
            System.out.println("TokenIntrospection responseMessage:" + response);

            JSONObject jobj = null;
            try {
                jobj = JSONObject.parse(response);
            } catch (Exception e) {
                e.printStackTrace();
            }

            sb.append("\n\nParsed JSONObject of introspect");

            if (returnCode.equals("200")) {

                Boolean valid = ((Boolean) jobj.get("active"));
                if (valid != null && valid.booleanValue()) {
                    String scope = (String) jobj.get("scope");
                    String user = (String) jobj.get("sub");
                    String realm = (String) jobj.get("realmName");
                    String uniqueSecurityName = (String) jobj.get("uniqueSecurityName");

                    //For dataPower
                    /*
                     * String scope = (String) jobj.get("scope");
                     * String user = (String) jobj.get("resource_owner");
                     * String realm = opUrl;
                     * String uniqueSecurityName = user;
                     */

                    Subject subject = new Subject();
                    Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
                    hashtable.put("scope", scope);

                    String uniqueID = new StringBuffer("user:").append(realm).append("/").append(uniqueSecurityName).toString();
                    sb.append("\n  TokenIntrospection uniqueId:" + uniqueID);
                    sb.append("\n  TokenIntrospection user:" + user);
                    sb.append("\n  TokenIntrospection realm:" + realm);

                    System.out.println("TokenIntrospection uniqueId:" + uniqueID);
                    System.out.println("TokenIntrospection user:" + user);
                    System.out.println("TokenIntrospection realm:" + realm);
                    hashtable.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, uniqueID);
                    hashtable.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, user);
                    hashtable.put(AttributeNameConstants.WSCREDENTIAL_REALM, realm);
                    subject.getPublicCredentials().add(hashtable);
                    //taiResult = TAIResult.create(HttpServletResponse.SC_OK, user, subject);

                } else {
                    System.out.println("TokenIntrospection this must had been return from a customized introspect user feature:" + returnCode);
                    System.out.println("TokenIntrospection It does not return active but returns " + response);
                    sb.append("\n\n  TokenIntrospection this must had been return from a customized introspect user feature:" + returnCode);
                    sb.append("\n  TokenIntrospection It does not return active but returns " + response);
                    //taiResult = TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED);

                }
            } else if (returnCode.equals("400")) {
                String errorMsg = response;
                if (jobj != null) {
                    errorMsg = jobj.serialize();
                }
                res.addHeader("WWW-Authenticate", "Bearer realm=\"default\", error=" + errorMsg);
                sb.append("\n\n  Responding with code: " + HttpServletResponse.SC_BAD_REQUEST);

                System.out.println("Responding with code: " + HttpServletResponse.SC_BAD_REQUEST);
                //taiResult = TAIResult.create(HttpServletResponse.SC_BAD_REQUEST);

            } else {
                res.addHeader("WWW-Authenticate", "Bearer realm=\"default\", error=\"invalid_token\"");
                sb.append("\n\n  Responding with code: " + returnCode);

                System.out.println("Responding with code: " + returnCode);
                //taiResult = TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED);
            }

        } catch (Exception e) {
            sb.append("\n\nhit unexpected exception:" + e);
            e.printStackTrace();
            throw new WebTrustAssociationFailedException(e.getMessage());
        }
    }

    protected HashMap<String, String> postToIntrospectionEndpoint(String opUrl, String contents, String clientId, String clientSecret, StringBuffer sb) throws Exception {

        String method = "POST";
        String output = null;
        InputStream inputStream = null;
        InputStream errorStream = null;
        OutputStream outputStream = null;
        HashMap<String, String> returnMap = new HashMap<String, String>();
        String userPass = clientId + ":" + clientSecret;
        String userPassBase64 = Base64.encode(userPass.getBytes());

        String basicAuth = "Basic " + userPassBase64;
        sb.append("\n " + basicAuth + " userPass:" + userPass);
        sb.append("\n content:" + contents);

        try {

            HttpURLConnection connection = null;
            if (opUrl.startsWith("https")) {
                connection = getSecuredConnection(method, opUrl);
            } else {
                URL aurl = new URL(opUrl);
                connection = (HttpURLConnection) aurl.openConnection();
            }

            connection.setDoOutput(true);
            if (contents != null) {
                connection.setDoInput(true);
            }
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod(method);
            connection.setConnectTimeout(60 * 10000);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

            connection.setRequestProperty("Authorization", basicAuth);

            connection.connect();

            if (contents != null) {
                outputStream = connection.getOutputStream();
                outputStream.write(contents.getBytes());
                outputStream.flush();
            }

            int responseCode = connection.getResponseCode();
            System.out.println("Response code: " + responseCode);

            if (responseCode < 400) {
                inputStream = connection.getInputStream();
                if (inputStream != null) {
                    output = getData(inputStream);
                    System.out.println("Response output: " + output);
                }
            } else {
                errorStream = connection.getErrorStream();
                if (errorStream != null) {
                    output = getData(errorStream);
                    System.out.println("Response output: " + output);
                }
            }

            returnMap.put("responseCode", Integer.toString(responseCode));
            returnMap.put("responseMsg", output);

        } catch (IOException e) {

            throw new Exception("Failed to make a request to OP server", e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
                if (errorStream != null) {
                    errorStream.close();
                }
            } catch (Exception e) {
                System.out.println("Failed to close the streams opened during connection to OP server, exception:[" + e.getMessage() + "]");

            }
        }

        return returnMap;
    }

    protected String getBearerAccessToken(HttpServletRequest req) {
        String hdrValue = req.getHeader(Authorization_Header);
        if (hdrValue != null && hdrValue.startsWith(BEARER)) {

            hdrValue = hdrValue.substring(7);
        }
        else {
            hdrValue = req.getHeader(ACCESS_TOKEN);
            if (hdrValue == null || hdrValue.trim().length() == 0) {
                hdrValue = req.getParameter(ACCESS_TOKEN);
            }

        }
        return hdrValue;
    }

    protected static HttpsURLConnection getSecuredConnection(String method, String opUrl) throws IOException {
        //import com.ibm.websphere.ssl.JSSEHelper;
        //JSSEHelper jsseHelper = JSSEHelper.getInstance();

        //SSLSocketFactory factory = null;
        //try {
        //    SSLContext context = jsseHelper.getSSLContext(null, null, null);
        //    factory = context.getSocketFactory();
        //} catch (Exception e) {
        //
        //    throw new IOException("Failed to get SSL socket factory for connection to OP server", e);
        //}

        URL aurl = new URL(opUrl);
        HttpsURLConnection conn = (HttpsURLConnection) aurl.openConnection();
        //conn.setSSLSocketFactory(factory);

        return conn;
    }

    public static String getData(InputStream inStream) throws IOException {

        if (inStream == null) {

            throw new IOException("parameter passed to this method is null");
        }

        BufferedReader reader = null;
        StringBuilder requestData = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                requestData.append(line);
            }
        } catch (IOException e) {

            throw new IOException("Failed to read data from input stream", e);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        String retVal = requestData.toString();

        return retVal;
    }

    /**
     * Perform setup for testing with SSL connections: TrustManager, hostname
     * verifier, ...
     */
    private static void setupSSLClient() {

        String protocols = "SSLv3,TLSv1";
        if (!System.getProperty("java.specification.version").startsWith("1.7"))
            protocols += ",TLSv1.1,TLSv1.2";
        System.setProperty("com.ibm.jsse2.disableSSLv3", "false");
        System.setProperty("https.protocols", protocols);
        Security.setProperty("jdk.tls.disabledAlgorithms", "");
        System.out.println("Enabled SSLv3. https.protocls=" + protocols);

        try {
            KeyManager keyManagers[] = null;

            // if the System.Properties already set up the keystore, initialize
            // it
            String ksPath = System.getProperty("javax.net.ssl.keyStore");
            if (ksPath != null && ksPath.length() > 0) {
                String ksPassword = System
                        .getProperty("javax.net.ssl.keyStorePassword");
                String ksType = System
                        .getProperty("javax.net.ssl.keyStoreType");
                System.out.println("setup Keymanager: " +
                        "ksPath=" + ksPath + " ksPassword=" + ksPassword + " ksType=" + ksType);
                if (ksPassword != null && ksType != null) {
                    KeyManagerFactory kmFactory = KeyManagerFactory
                            .getInstance(KeyManagerFactory.getDefaultAlgorithm());

                    File ksFile = new File(ksPath);
                    KeyStore keyStore = KeyStore.getInstance(ksType);
                    FileInputStream ksStream = new FileInputStream(ksFile);
                    keyStore.load(ksStream, ksPassword.toCharArray());

                    kmFactory.init(keyStore, ksPassword.toCharArray());
                    keyManagers = kmFactory.getKeyManagers();
                }
            }

            // Create a trust manager that does not validate certificate chains
            /* */
            trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs,
                        String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs,
                        String authType) {
                }
            } };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(keyManagers, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            @SuppressWarnings("unused")
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            /* setup jdk ssl */
            System.out.println("Setting trustStore to "
                    + JKS_LOCATION);
            System.setProperty("javax.net.ssl.trustStore",
                    JKS_LOCATION);
            System.setProperty("javax.net.ssl.trustStorePassword",
                    // "changeit");
                    "LibertyClient");
            System.setProperty("javax.net.debug", "ssl");
            System.out.println("javax.net.debug is set to: "
                    + System.getProperty("javax.net.debug"));

        } catch (Exception e) {
            System.out.println("static initializer: " +
                    "Unable to set default TrustManager:" + e);
            throw new RuntimeException("Unable to set default TrustManager", e);
        } finally {
            System.setProperty("javax.net.ssl.keyStore", ""); // reset the System property to empty string on keyStore settings for next test suite
        }

    }

    public static class Base64 {
        /*
         * static public void main( String[] args ){
         * if( args.length < 1 ) {
         * System.out.println( "Format: SimpleEncode <string>" );
         * System.exit( 0 );
         * }
         * try{
         * for( int iI = 0; iI < args.length; iI++ ){
         * System.out.println( "\"" + args[ iI ] + "\"=\"" + sEncode(args[iI]) + "\"");
         * }
         * } catch( Exception e ){
         * e.printStackTrace( System.out );
         * }
         * }
         */
        private static final char[] S_BASE64CHAR = {
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
                'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
                'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd',
                'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
                'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
                'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', '+', '/'
        };
        private static final char S_BASE64PAD = '=';
        private static final byte[] S_DECODETABLE = new byte[128];
        static {
            for (int i = 0; i < S_DECODETABLE.length; i++)
                S_DECODETABLE[i] = Byte.MAX_VALUE; // 127
            for (int i = 0; i < S_BASE64CHAR.length; i++)
                // 0 to 63
                S_DECODETABLE[S_BASE64CHAR[i]] = (byte) i;
        }

        /**
         * Returns base64 representation of specified byte array.
         */
        final public static String encode(byte[] data) {
            return encode(data, 0, data.length);
        }

        /**
         * Returns base64 representation of specified byte array.
         */
        final public static String encode(byte[] data, int off, int len) {
            if (len <= 0)
                return "";
            char[] out = new char[len / 3 * 4 + 4];
            int rindex = off;
            int windex = 0;
            int rest = len;
            while (rest >= 3) {
                int i = ((data[rindex] & 0xff) << 16)
                        + ((data[rindex + 1] & 0xff) << 8)
                        + (data[rindex + 2] & 0xff);
                out[windex++] = S_BASE64CHAR[i >> 18];
                out[windex++] = S_BASE64CHAR[(i >> 12) & 0x3f];
                out[windex++] = S_BASE64CHAR[(i >> 6) & 0x3f];
                out[windex++] = S_BASE64CHAR[i & 0x3f];
                rindex += 3;
                rest -= 3;
            }
            if (rest == 1) {
                int i = data[rindex] & 0xff;
                out[windex++] = S_BASE64CHAR[i >> 2];
                out[windex++] = S_BASE64CHAR[(i << 4) & 0x3f];
                out[windex++] = S_BASE64PAD;
                out[windex++] = S_BASE64PAD;
            } else if (rest == 2) {
                int i = ((data[rindex] & 0xff) << 8) + (data[rindex + 1] & 0xff);
                out[windex++] = S_BASE64CHAR[i >> 10];
                out[windex++] = S_BASE64CHAR[(i >> 4) & 0x3f];
                out[windex++] = S_BASE64CHAR[(i << 2) & 0x3f];
                out[windex++] = S_BASE64PAD;
            }
            return new String(out, 0, windex);
        }

        static private String symbol = "Basic ";

        public static String sEncode(String str) {
            try {
                byte[] bytes = str.getBytes("utf-8");
                String strRet = encode(bytes);
                return symbol + strRet;
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
            return "ERROREncode";
        }
    }

}
