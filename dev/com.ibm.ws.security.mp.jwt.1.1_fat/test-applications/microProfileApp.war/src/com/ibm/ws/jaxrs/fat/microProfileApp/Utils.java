/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxrs.fat.microProfileApp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.inject.Provider;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.security.auth.Subject;

import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
// import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;

// public class MicroProfileApp extends HttpServlet {

public class Utils {

    public static Class<?> thisClass = Utils.class;
    public static final String newLine = System.getProperty("line.separator");

    /**
     * Ojbect to carry the various Claim Injection values. The values contained will be compared to ensure that values from the
     * various injection methods are all set the same
     *
     * @author chrisc
     *
     */
    public class ValueList {

        String obtainedBy = null;
        String stringValue = null;
        Set<?> setValue = null;

        public String getObtainedBy() {
            return obtainedBy;
        }

        public String getStringValue() {
            return stringValue;
        }

        public Set<?> getSetValue() {
            return setValue;
        }

        public ValueList(String inObtainedBy, String inStringValue, Set<?> inSetValue) {
            obtainedBy = inObtainedBy;
            stringValue = inStringValue;
            setValue = inSetValue;
        }
    }

    /**
     * Add either a String or Set member to the valueList
     *
     * @param vList
     *            - existing list to add to
     * @param inObtainedBy
     *            - The type of Claim Injection (ClaimValue, Optional, Instance, ...) used to logging by compare methods
     * @param inValue
     *            - the String value
     * @param inSetValue
     *            - the Set value
     * @return - returns an updated ValueList
     * @throws Exception
     */
    public static List<ValueList> addValueListEntry(List<ValueList> vList, String inObtainedBy, String inValue, Set<?> inSetValue) throws Exception {
        try {
            if (vList == null) {
                vList = new ArrayList<ValueList>();
            }
            vList.add(new Utils().new ValueList(inObtainedBy, inValue, inSetValue));
            return vList;
        } catch (Exception e) {
            Log.info(thisClass, "addValueListEntry", "Error occured while trying to set an expectation during test setup");
            throw e;
        }
    }

    /**
     * Add a String to the valueList
     *
     * @param vList
     *            - existing list to add to
     * @param inObtainedBy
     *            - The type of Claim Injection (ClaimValue, Optional, Instance, ...) used to logging by compare methods
     * @param inValue
     *            - the String value
     * @return - returns an updated ValueList
     * @throws Exception
     */
    public static List<ValueList> addValueListEntry(List<ValueList> vList, String inObtainedBy, String inValue) throws Exception {
        try {
            if (vList == null) {
                vList = new ArrayList<ValueList>();
            }
            vList.add(new Utils().new ValueList(inObtainedBy, inValue, null));
            return vList;
        } catch (Exception e) {
            Log.info(thisClass, "addValueListEntry", "Error occured while trying to set an expectation during test setup");
            throw e;
        }
    }

    /**
     * Add a Set member to the valueList
     *
     * @param vList
     *            - existing list to add to
     * @param inObtainedBy
     *            - The type of Claim Injection (ClaimValue, Optional, Instance, ...) used to logging by compare methods
     * @param inSetValue
     *            - the Set value
     * @return - returns an updated ValueList
     * @throws Exception
     */
    public static List<ValueList> addValueListEntry(List<ValueList> vList, String inObtainedBy, Set<?> inSetValue) throws Exception {
        try {
            if (vList == null) {
                vList = new ArrayList<ValueList>();
            }
            vList.add(new Utils().new ValueList(inObtainedBy, null, inSetValue));
            return vList;
        } catch (Exception e) {
            Log.info(thisClass, "addValueListEntry", "Error occured while trying to set an expectation during test setup");
            throw e;
        }
    }

    /**
     * Run the jsonWebToken apis - use the printValues method to record the results - the calling test cases will compare those
     * values against the origin mpJwt
     *
     * @param appName
     *            - The name of the app that obtained the value
     * @param jsonWebToken
     *            - the string form of the jsonWebToken
     * @param requestType
     *            - The request type - SecurityContext, Injection (of jsonWebToken), or Claim Injection
     * @return - a string containing the formatted output (to be returned in the response as well as logged in the server log)
     */
    public static String runApis(String appName, JsonWebToken jsonWebToken, String requestType) {
        try {

            return printValues(appName, jsonWebToken, requestType,
                    jsonWebToken.getName(),
                    jsonWebToken.getRawToken(),
                    jsonWebToken.getIssuer(),
                    jsonWebToken.getSubject(),
                    jsonWebToken.getTokenID(),
                    longToString(jsonWebToken.getExpirationTime()),
                    longToString(jsonWebToken.getIssuedAtTime()),
                    jsonWebToken.getAudience(),
                    jsonWebToken.getGroups(),
                    jsonWebToken.getClaimNames());

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Caught an exception trying to load request parameters." + e.toString());
            return "Error running apis - " + e.toString();
        }
    }

    /**
     * Records the values for various claims in a format that the tests expect. The test apps use this to record the claim values
     * they've obtained. The test cases expect to find these values in a common form. (<claim>: <value>) These values will be
     * compared against what was in the original mpJwt.
     *
     * @param appName
     *            - The name of the app that obtained the value
     * @param jsonWebToken
     *            - the string form of the jsonWebToken
     * @param requestType
     *            - The request type - SecurityContext, Injection (of jsonWebToken), or Claim Injection
     * @param upn
     *            - upn claim value
     * @param token
     *            - string form of the jsonWebtoken
     * @param issuer
     *            - issuer claim value
     * @param subject
     *            - sub claim value
     * @param jwtId
     *            - jwtId claim value
     * @param exp
     *            - exp claim value
     * @param iat
     *            - iat claim value
     * @param audiences
     *            - aud claim value
     * @param groups
     *            - groups claim value
     * @param claims
     *            - list of claims - retrieved via api
     * @return - a string containing the formatted output (to be returned in the response as well as logged in the server log)
     */
    public static String printValues(String appName, JsonWebToken jsonWebToken, String requestType, String upn, String token, String issuer, String subject, String jwtId,
            String exp, String iat, Set<String> audiences, Set<String> groups, Set<String> claims) {
        try {

            StringBuffer sb = new StringBuffer();

            writeLine(sb, newLine + "Using " + requestType + " Method");
            writeLine(sb, "Got to the MicroProfile " + appName + " App");

            printCallerCred(sb);

            // run String methods
            writeLine(sb, MpJwtFatConstants.JWT_BUILDER_NAME_ATTR + ": " + upn);
            writeLine(sb, MpJwtFatConstants.JWT_BUILDER_TOKEN + token);
            writeLine(sb, MpJwtFatConstants.JWT_BUILDER_ISSUER + issuer);
            writeLine(sb, MpJwtFatConstants.JWT_BUILDER_SUBJECT + subject);
            writeLine(sb, MpJwtFatConstants.JWT_BUILDER_JWTID + jwtId);

            // run long methods
            writeLine(sb, MpJwtFatConstants.JWT_BUILDER_EXPIRATION + exp);
            writeLine(sb, MpJwtFatConstants.JWT_BUILDER_ISSUED_AT + iat);

            // run Set methods
            if (audiences != null) {
                Iterator<String> iter = audiences.iterator();
                while (iter.hasNext()) {
                    writeLine(sb, MpJwtFatConstants.JWT_BUILDER_AUDIENCE + iter.next());
                }
            } else {
                writeLine(sb, MpJwtFatConstants.JWT_BUILDER_AUDIENCE + "null");
            }

            writeLine(sb, "RAW: " + MpJwtFatConstants.PAYLOAD_GROUPS + ": " + groups);
            if (groups == null || groups.isEmpty()) {
                writeLine(sb, MpJwtFatConstants.PAYLOAD_GROUPS + ": null");
            } else {
                Iterator<String> iterr = groups.iterator();
                while (iterr.hasNext()) {
                    writeLine(sb, MpJwtFatConstants.PAYLOAD_GROUPS + ": " + iterr.next());
                }
            }

            if (jsonWebToken != null) {
                Iterator<String> iterrr = claims.iterator();
                while (iterrr.hasNext()) {
                    String key = iterrr.next();
                    // run Object method
                    writeLine(sb, MpJwtFatConstants.JWT_BUILDER_CLAIM + key);
                    // this line is more for FYI - it's not checked by the test code
                    writeLine(sb, MpJwtFatConstants.JWT_BUILDER_CLAIM + "key: " + key + " value: " + jsonWebToken.getClaim(key));
                }
            }

            System.out.println(sb.toString());
            return (sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Caught an exception trying to load request parameters." + e.toString());
            return "Error running apis - " + e.toString();
        }
    }

    public static void printCallerCred(StringBuffer sb) throws Exception {
        Subject callerSubject = WSSubject.getCallerSubject();
        writeLine(sb, "callerSubject: " + callerSubject);

        if (callerSubject != null) {
            WSCredential callerCredential = callerSubject.getPublicCredentials(WSCredential.class).iterator().next();
            if (callerCredential != null) {
                writeLine(sb, "callerCredential: " + callerCredential);
            } else {
                writeLine(sb, "callerCredential: null");
            }
        } else {
            writeLine(sb, "callerCredential: null");
        }
    }

    /******************** Methods to convert various instance types to native types ***************/
    /* methods are needed to handle cases where the instance is null */
    /**********************************************************************************************/
    /* instance - basic types */
    public static <T> T instanceToJavaType(Instance<T> rawValue) throws Exception {
        if (rawValue == null) {
            return null;
        } else {
            return rawValue.get();
        }
    }

    public static <T> String instanceToString(Instance<T> rawValue) throws Exception {

        return convertToString(instanceToJavaType(rawValue));

    }

    /* instance - optional types */
    public static <T> String instanceOptionalToString(Instance<Optional<T>> rawValue) throws Exception {
        if (rawValue == null) {
            return null;
        } else {
            return optionalToString(rawValue.get());
        }
    }

    public static Set<String> instanceOptionalSetStringToSet(Instance<Optional<Set<String>>> rawValue) throws Exception {

        if (rawValue == null) {
            return null;
        } else {
            return optionalToJavaType(rawValue.get());
        }
    }

    /* instance - json */
    public static Set<String> instanceJsonArrayToSet(Instance<JsonArray> rawValue) throws Exception {

        if (rawValue == null) {
            return new HashSet<String>();
        } else {
            return jsonArrayToSet(rawValue.get());
        }
    }

    /******************** Methods to convert various ClaimValue types to native types *************/
    /* methods are needed to handle cases where the ClaimValue is null */
    /**********************************************************************************************/
    /* ClaimValue - optional */
    public static <T> String claimValueOptionalToString(ClaimValue<Optional<T>> rawValue) throws Exception {

        if (rawValue == null) {
            return null;
        } else {
            return optionalToString(rawValue.getValue());
        }
    }

    public static Set<String> claimValueOptionalToSet(ClaimValue<Optional<Set<String>>> rawValue) throws Exception {

        if (rawValue == null) {
            return null;
        } else {
            return optionalToJavaType(rawValue.getValue());
        }
    }

    public static Set<String> claimValueOptionalJsonArrayToSet(ClaimValue<Optional<JsonArray>> rawValue) throws Exception {

        if (rawValue == null) {
            return new HashSet<String>();
        } else {
            return optionalJsonArrayToSet(rawValue.getValue());
        }
    }

    public static Set<String> claimValueJsonArrayToSet(ClaimValue<JsonArray> rawValue) throws Exception {

        if (rawValue == null) {
            return new HashSet<String>();
        } else {
            return jsonArrayToSet(rawValue.getValue());
        }
    }

    /******************** Methods to convert various optional types to native types *************/
    /* methods are needed to handle cases where the optional value is null */
    /**********************************************************************************************/
    /* optional - basic types */
    public static <T> T optionalToJavaType(Optional<T> rawValue) throws Exception {
        if (rawValue == null || !rawValue.isPresent()) {
            return null;
        } else {
            return rawValue.get();
        }
    }

    public static <T> String optionalToString(Optional<T> rawValue) throws Exception {
        return convertToString(optionalToJavaType(rawValue));
    }

    public static Set<String> optionalJsonArrayToSet(Optional<JsonArray> rawValue) throws Exception {

        if (rawValue == null || !rawValue.isPresent()) {
            return new HashSet<String>();
        } else {
            return jsonArrayToSet(rawValue.get());
        }
    }

    /******************** Methods to convert various Jsonp types to native types *************/
    /* methods are needed to handle cases where the Jsonp is null */
    /**********************************************************************************************/
    public static String jsonValueToString(JsonValue rawValue) throws Exception {
        if (rawValue == null) {
            return null;
        } else {
            return convertToString(rawValue);
        }
    }

    // so far, only using this with Set<String>
    public static Set<String> jsonArrayToSet(JsonArray rawValue) throws Exception {

        if (rawValue == null) {
            return new HashSet<String>();
        } else {

            Set<Object> mySet = Arrays.stream(rawValue.toArray()).collect(Collectors.toSet());
            Set<String> newSet = new HashSet<String>();
            Iterator<Object> iter = mySet.iterator();
            while (iter.hasNext()) {
                String theValue = jsonValueToString((JsonString) iter.next());
                newSet.add(theValue);
            }
            return newSet;
        }
    }

    /******************** Methods to convert various Jsonp types to native types *************/
    /* methods are needed to handle cases where the Jsonp is null */
    /**********************************************************************************************/
    public static <T> String providerOptionalToString(Provider<Optional<T>> rawValue) throws Exception {

        if (rawValue == null) {
            return null;
        } else {
            return optionalToString(rawValue.get());
        }
    }

    public static Set<String> providerOptionalJsonArrayToSet(Provider<Optional<JsonArray>> rawValue) throws Exception {

        if (rawValue == null) {
            return new HashSet<String>();
        } else {
            return optionalJsonArrayToSet(rawValue.get());
        }
    }

    /******************** Methods to invoke getValue and convert to native types ******************/
    /* methods are needed to handle cases where the ClaimValue is null */
    /**********************************************************************************************/
    public static String claimValueToString(ClaimValue<?> theClaim) throws Exception {
        if (theClaim == null) {
            return null;
        } else {
            Object theValue = theClaim.getValue();
            if (theValue == null) {
                return null;
            }
            return convertToString(theValue);
        }
    }

    public static Set<?> getValueToSet(ClaimValue<?> theClaim) throws Exception {
        if (theClaim == null) {
            return null;
        } else {
            Object theValue = theClaim.getValue();
            if (theValue == null) {
                return null;
            }
            return (Set<?>) theValue;
        }
    }

    /********************************************************************************/
    /**
     * method to convert claim name to a string
     *
     * @param rawClaim
     *            - raw claim name
     * @return - claim name as string
     * @throws Exception
     */
    public static String claimsToString(Claims rawClaim) throws Exception {

        if (rawClaim == null) {
            return null;
        } else {
            return rawClaim.toString();
        }
    }

    /**
     * Compares Strings to one another - compare each String in the passed in list to the "master". The master
     * will at another point be compared against what was in the original mpJwt - so, in the end, we'll be comparing
     * all list members against the original token.
     * The list members are Strings or String representations that we obtained via various forms of Claim Injection -
     * the goal is to make sure that we get the correct value each way.
     *
     * @param master
     *            - the Set that we'll compare against the original mpJwt (should be instance claim)
     * @param theClaim
     *            - the claim name (mainly for logging)
     * @param valueList
     *            - the list of values (in this case Strings) that we'll compare
     * @return - returns a build up String recording the results of the comparisons
     * @throws Exception
     */
    public static String compareStrings(String master, String theClaim, List<ValueList> valueList) throws Exception {

        StringBuffer sb = new StringBuffer();
        Boolean doesMatch = true;

        printDivider(sb);
        writeLine(sb, "Comparing values for: " + theClaim);
        writeLine(sb, "Comparing values against: " + master);
        for (ValueList vList : valueList) {
            writeLine(sb, vList.getObtainedBy() + ": " + vList.getStringValue());
            if (master == null) {
                // compare null against individual strings
                if (master != vList.getStringValue()) {
                    writeLine(sb, "------------ Values DO NOT Match -----------");
                    doesMatch = false;
                }
            } else {
                if (vList.getStringValue() == null || !master.equals(vList.getStringValue())) {
                    writeLine(sb, "------------ Values DO NOT Match -----------");
                    doesMatch = false;
                }
            }
        }
        if (doesMatch) {
            writeLine(sb, "------------ All Values appear to Match for claim: " + theClaim + " -----------");
        }
        printDivider(sb);
        System.out.println(sb.toString());
        return (sb.toString());
    }

    /**
     * Compares Sets to one another - compare each Set in the passed in list to the "master". The master
     * will at another point be compared against what was in the original mpJwt - so, in the end, we'll be comparing
     * all list members against the original token.
     * The list members are Sets that we obtained via various forms of Claim Injection - the goal is to make sure that
     * we get the correct value each way.
     *
     * @param master
     *            - the Set that we'll compare against the original mpJwt (should be instance claim)
     * @param theClaim
     *            - the claim name (mainly for logging)
     * @param valueList
     *            - the list of values (in this case Sets) that we'll compare
     * @return - returns a build up String recording the results of the comparisons
     * @throws Exception
     */
    public static String compareSets(Set<?> master, String theClaim, List<ValueList> valueList) throws Exception {

        StringBuffer sb = new StringBuffer();
        Boolean doesMatch = true;

        printDivider(sb);
        writeLine(sb, "Comparing values for: " + theClaim);
        for (ValueList vList : valueList) {
            writeLine(sb, vList.getObtainedBy() + ": " + vList.getSetValue());
            if (master == null) {
                // compare null against individual sets
                if (master != vList.getSetValue() && !vList.getSetValue().isEmpty()) {
                    writeLine(sb, "------------ Values DO NOT Match -----------");
                    doesMatch = false;
                }
            } else {
                if (vList.getSetValue() == null || !master.containsAll(vList.getSetValue())) {
                    writeLine(sb, "------------ Values DO NOT Match -----------");
                    doesMatch = false;
                } else {
                    if (!(vList.getSetValue()).containsAll(master)) {
                        writeLine(sb, "------------ Values DO NOT Match -----------");
                        doesMatch = false;
                    }
                }
            }
        }
        if (doesMatch) {
            writeLine(sb, "------------ All Values appear to Match for claim: " + theClaim + " -----------");
        }
        printDivider(sb);
        System.out.println(sb.toString());
        return (sb.toString());
    }

    /************************** Common tooling *****************************/
    public static String booleanToString(Boolean theBool) throws Exception {

        if (theBool == null) {
            return null;
        } else {
            return Boolean.toString(theBool);
        }

    }

    public static String longToString(Long theLong) throws Exception {
        if (theLong == null) {
            return null;
        } else {
            return theLong.toString();
        }
    }

    public static <T> String convertToString(T tempVal) throws Exception {
        if (tempVal == null) {
            return null;
        }
        if (tempVal instanceof String) {
            return (String) tempVal;
        }
        if (tempVal instanceof Long) {
            return longToString((Long) tempVal);
        }
        if (tempVal instanceof Boolean) {
            return booleanToString((Boolean) tempVal);
        }
        if (tempVal instanceof JsonValue) {
            if (((JsonValue) tempVal).getValueType().equals(ValueType.STRING)) {
                return ((JsonString) tempVal).getString();
            }
            if (((JsonValue) tempVal).getValueType().equals(ValueType.NUMBER)) {
                return ((JsonNumber) tempVal).toString();
            }
        }
        // right now, we should only be passing values that are of type String, Long and Boolean
        // if we end up with something else, it's probably and oops on the test writer's part
        return "Unknown Type";
    }

    public static void writeLine(StringBuffer sb, String msg) {
        sb.append(msg + newLine);
    }

    public static void printDivider(StringBuffer sb) {

        Utils.writeLine(sb, "--------------------------------------------------------------------------------");
    }

}
