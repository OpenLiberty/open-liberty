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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.ibm.ws.security.fat.common.Constants;

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

    protected final String clientSecret = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger";
    private static final String BEGIN_PRIV_KEY = "-----BEGIN PRIVATE KEY-----";
    private static final String END_PRIV_KEY = "-----END PRIVATE KEY-----";
    private static final String BEGIN_EC_PRIV_KEY = "-----BEGIN EC PRIVATE KEY-----";
    private static final String END_EC_PRIV_KEY = "-----END EC PRIVATE KEY-----";

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

    // override this method to handle hs256, rs256, or rs512
    String createJwtResponse(String accessToken) throws Exception {
        // default SigAlg is RS256
        return getRS256Jws(accessToken);
//        return getHS256Jws(clientSecret, accessToken);
//        return getRS512Jws(accessToken);
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

    private String getRS256Jws(String accessToken) throws Exception {
        String headerAndPayload = encode(getJwsHeader(Constants.SIGALG_RS256)) + "." + encode(getMinimumClaims(accessToken));
        String key = readKeyFromFile("RS256private-key.pem");
        String signature = getRSSigned(headerAndPayload, key, "SHA256withRSA");
        return headerAndPayload + "." + signature;
    }

    protected String getRS384Jws(String accessToken) throws Exception {
        String headerAndPayload = encode(getJwsHeader(Constants.SIGALG_RS384)) + "." + encode(getMinimumClaims(accessToken));
        String key = readKeyFromFile("RS384private-key.pem");
        String signature = getRSSigned(headerAndPayload, key, "SHA384withRSA");
        return headerAndPayload + "." + signature;
    }

    protected String getRS512Jws(String accessToken) throws Exception {
        String headerAndPayload = encode(getJwsHeader(Constants.SIGALG_RS512)) + "." + encode(getMinimumClaims(accessToken));
        String key = readKeyFromFile("RS512private-key.pem");
        String signature = getRSSigned(headerAndPayload, key, "SHA512withRSA");
        return headerAndPayload + "." + signature;
    }

    protected String getES256Jws(String accessToken) throws Exception {
        String headerAndPayload = encode(getJwsHeader(Constants.SIGALG_ES256)) + "." + encode(getMinimumClaims(accessToken));
        String key = readKeyFromFile("ES256private-key.pem");
        String signature = getESSigned(headerAndPayload, key, "SHA256WithECDSA");
        return headerAndPayload + "." + signature;
    }

    protected String getES384Jws(String accessToken) throws Exception {
        String headerAndPayload = encode(getJwsHeader(Constants.SIGALG_ES384)) + "." + encode(getMinimumClaims(accessToken));
        String key = readKeyFromFile("ES384private-key.pem");
        String signature = getESSigned(headerAndPayload, key, "SHA384WithECDSA");
        return headerAndPayload + "." + signature;
    }

    protected String getES512Jws(String accessToken) throws Exception {
        String headerAndPayload = encode(getJwsHeader(Constants.SIGALG_ES512)) + "." + encode(getMinimumClaims(accessToken));
        String key = readKeyFromFile("ES512private-key.pem");
        String signature = getESSigned(headerAndPayload, key, "SHA512WithECDSA");
        return headerAndPayload + "." + signature;
    }

    protected String getHS256Jws(String secret, String accessToken) throws Exception {
        String headerAndPayload = encode(getJwsHeader(Constants.SIGALG_HS256)) + "." + encode(getMinimumClaims(accessToken));
        String signature = getHSSignature(headerAndPayload, secret, "HmacSHA256");
        return headerAndPayload + "." + signature;
    }

    protected String getHS384Jws(String secret, String accessToken) throws Exception {
        String headerAndPayload = encode(getJwsHeader(Constants.SIGALG_HS384)) + "." + encode(getMinimumClaims(accessToken));
        String signature = getHSSignature(headerAndPayload, secret, "HmacSHA384");
        return headerAndPayload + "." + signature;
    }

    protected String getHS512Jws(String secret, String accessToken) throws Exception {
        String headerAndPayload = encode(getJwsHeader(Constants.SIGALG_HS512)) + "." + encode(getMinimumClaims(accessToken));
        String signature = getHSSignature(headerAndPayload, secret, "HmacSHA512");
        return headerAndPayload + "." + signature;
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

    private String getRSSigned(String input, String key, String instance) throws Exception {
        return getSigned(input, key, instance, "RSA");
    }

    private String getESSigned(String input, String key, String instance) throws Exception {
        return getSigned(input, key, instance, "EC");
    }

    private String getPSSigned(String input, String key, String instance) throws Exception {
        return getSigned(input, key, instance, "PS");
    }

    private String getSigned(String input, String inKey, String instance, String type) throws Exception {

        String beginStr = BEGIN_PRIV_KEY;
        String endStr = END_PRIV_KEY;

        if (type.equals("EC")) {
            beginStr = BEGIN_EC_PRIV_KEY;
            endStr = END_EC_PRIV_KEY;
        }

        String key = inKey.replaceAll("\n", "");
        int beginIndex = key.indexOf(beginStr) + beginStr.length();
        int endIndex = key.indexOf(endStr);
//        Log.info(thisClass, thisMethod, "begin: " + beginIndex);
//        Log.info(thisClass, thisMethod, "end: " + endIndex);
        String base64 = key.substring(beginIndex, endIndex).trim();

//        String base64 = key.replaceAll(endStr, "").replaceAll(beginStr, "").replaceAll("\n", "");
        System.out.println("chc - theKey: " + base64);
        byte[] decode = Base64.getDecoder().decode(base64);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decode);
        KeyFactory kf = KeyFactory.getInstance(type);

        Signature privateSignature = Signature.getInstance(instance);
        privateSignature.initSign(kf.generatePrivate(spec));
        privateSignature.update(input.getBytes("UTF-8"));
        byte[] s = privateSignature.sign();
        return Base64.getEncoder().encodeToString(s);

    }

//    private String getRSSigned(String input, String inKey, String instance) throws Exception {
////      return getSigned(input, key, instance, "RSA");
//      String key = inKey.replaceAll("\n", "");
//      int beginIndex = key.indexOf(BEGIN_PRIV_KEY) + BEGIN_PRIV_KEY.length();
//      int endIndex = key.indexOf(BEGIN_PRIV_KEY);
//      String base64 = key.substring(beginIndex, endIndex).trim();
//
//      System.out.println("chc - theKey: " + base64);
//      byte[] decode = Base64.getDecoder().decode(base64);
//
//      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decode);
//      KeyFactory kf = KeyFactory.getInstance("RSA");
//
//      Signature privateSignature = Signature.getInstance(instance);
//      privateSignature.initSign(kf.generatePrivate(spec));
//      privateSignature.update(input.getBytes("UTF-8"));
//      byte[] s = privateSignature.sign();
//      return Base64.getEncoder().encodeToString(s);
//  }
//
//  private String getESSigned(String input, String inKey, String instance) throws Exception {
////      return getSigned(input, key, instance, "EC");
//
//      String key = inKey.replaceAll("\n", "");
//      int beginIndex = key.indexOf(BEGIN_EC_PRIV_KEY) + BEGIN_EC_PRIV_KEY.length();
//      int endIndex = key.indexOf(BEGIN_EC_PRIV_KEY);
//      String base64 = key.substring(beginIndex, endIndex).trim();
//
//      System.out.println("chc - theKey: " + base64);
//      byte[] decode = Base64.getDecoder().decode(base64);
//
//      ECGenParameterSpec spec = new ECGenParameterSpec("secp256k1");
//      KeyFactory kf = KeyFactory.getInstance("EC");
//
//      Signature privateSignature = Signature.getInstance(instance);
//      privateSignature.initSign(kf.generatePrivate((KeySpec) spec));
//      privateSignature.update(input.getBytes("UTF-8"));
//      byte[] s = privateSignature.sign();
//      return Base64.getEncoder().encodeToString(s);
//
//  }
    public String readKeyFromFile(String theFile) throws Exception {
        return new String(Files.readAllBytes(Paths.get(theFile)));
    }

    private String getHSSignature(String input, String secret, String instance) throws Exception {
        byte[] secretBytes = secret.getBytes("UTF-8");
        Mac mac = Mac.getInstance(instance);
        SecretKeySpec keySpec = new SecretKeySpec(secretBytes, instance);
        mac.init(keySpec);
        byte[] hashBytes = mac.doFinal(input.getBytes("UTF-8"));
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
