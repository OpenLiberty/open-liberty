/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxrs.fat.microProfileApp;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.Claims;

import com.ibm.ws.security.jwt.fat.mpjwt.CommonMicroProfileApp;

// http://localhost:8010/microProfileApp/rest/ClaimInjection/MicroProfileClaimInjectionApp
// allow the same methods to invoke GET, POST, PUT, ... invocation type determines which gets invoked.
@SuppressWarnings("rawtypes")
public class ClaimInjectionAllTypesMicroProfileApp extends CommonMicroProfileApp {

    // Raw types ----------------------------------------------------------
    @Inject
    @Claim(standard = Claims.raw_token)
    private String rawTokenRaw;

    @Inject
    @Claim(standard = Claims.upn)
    private String upnRaw;

    @Inject
    @Claim(standard = Claims.iss)
    private String issRaw;

    @Inject
    @Claim(standard = Claims.sub)
    private String subRaw;

    @Inject
    @Claim(standard = Claims.iat)
    private Long iatRaw;

    @Inject
    @Claim(standard = Claims.exp)
    private Long expRaw;

    @Inject
    @Claim(standard = Claims.aud)
    private Set<String> audRaw;

    @Inject
    @Claim(standard = Claims.groups)
    private Set<String> groupsRaw;

    @Inject
    @Claim(standard = Claims.email_verified)
    private Boolean emailVerifiedRaw;

    @Inject
    @Claim(standard = Claims.jti)
    private String jtiRaw;

    // ClaimValue types ---------------------------------------------------
    @Inject
    @Claim(standard = Claims.raw_token)
    private ClaimValue<String> rawTokenClaimValue;

    @Inject
    @Claim("upn")
    private ClaimValue<String> upnClaimValue;

    @Inject
    @Claim(standard = Claims.iss)
    private ClaimValue<String> issClaimValue;

    @Inject
    @Claim(standard = Claims.sub)
    private ClaimValue<String> subClaimValue;

    @Inject
    @Claim(standard = Claims.iat)
    private ClaimValue<Long> iatClaimValue;

    @Inject
    @Claim(standard = Claims.exp)
    private ClaimValue<Long> expClaimValue;

    @Inject
    @Claim("aud")
    private ClaimValue<Set<String>> audClaimValue;

    @Inject
    @Claim("groups")
    private ClaimValue<Set<String>> groupsClaimValue;

    @Inject
    @Claim("email_verified")
    private ClaimValue<Boolean> emailVerifiedClaimValue;

    @Inject
    @Claim(standard = Claims.jti)
    private ClaimValue<String> jtiClaimValue;

    @Inject
    @Claim("iat")
    private ClaimValue<Long> dupIssuedAt;

    // ClaimValue Objects -----------------------------------------------
    @Inject
    @Claim(standard = Claims.raw_token)
    private ClaimValue rawTokenClaimValueObject;

    @Inject
    @Claim("upn")
    private ClaimValue upnClaimValueObject;

    @Inject
    @Claim(standard = Claims.iss)
    private ClaimValue issClaimValueObject;

    @Inject
    @Claim("sub")
    private ClaimValue subClaimValueObject;

    @Inject
    @Claim(standard = Claims.iat)
    private ClaimValue iatClaimValueObject;

    @Inject
    @Claim(standard = Claims.exp)
    private ClaimValue expClaimValueObject;

    @Inject
    @Claim("aud")
    private ClaimValue audClaimValueObject;

    @Inject
    @Claim("groups")
    private ClaimValue groupsClaimValueObject;

    @Inject
    @Claim("email_verified")
    private ClaimValue emailVerifiedClaimValueObject;

    @Inject
    @Claim("jti")
    private ClaimValue jtiClaimValueObject;

    // ClaimValue Optional ----------------------------------------------------
    @Inject
    @Claim(standard = Claims.upn)
    private ClaimValue<Optional<String>> upnClaimValueOptional;

    @Inject
    @Claim(standard = Claims.raw_token)
    private ClaimValue<Optional<String>> rawTokenClaimValueOptional;
    @Inject
    @Claim("sub")
    private ClaimValue<Optional<String>> subClaimValueOptional;

    @Inject
    @Claim(standard = Claims.iss)
    private ClaimValue<Optional<String>> issClaimValueOptional;

    @Inject
    @Claim("jti")
    private ClaimValue<Optional<String>> jtiClaimValueOptional;

    @Inject
    @Claim(standard = Claims.iat)
    private ClaimValue<Optional<Long>> iatClaimValueOptional;

    @Inject
    @Claim(standard = Claims.exp)
    private ClaimValue<Optional<Long>> expClaimValueOptional;

    @Inject
    @Claim("aud")
    private ClaimValue<Optional<Set<String>>> audClaimValueOptional;

    @Inject
    @Claim("groups")
    private ClaimValue<Optional<Set<String>>> groupsClaimValueOptional;

    @Inject
    @Claim("email_verified")
    private ClaimValue<Optional<Boolean>> emailVerifiedClaimValueOptional;

    // extra claims - not validating values, but, making sure app loads, ...
    @Inject
    @Claim("auth_time")
    private ClaimValue<Optional<Long>> authTimeClaimValueOptional;

    @Inject
    @Claim("custom-missing")
    private ClaimValue<Optional<Long>> customMissingClaimValueOptional;

    // Instance types ---------------------------------------------------------
    @Inject
    @Claim(standard = Claims.raw_token)
    private Instance<String> rawTokenInstance;

    @Inject
    @Claim(standard = Claims.upn)
    private Instance<String> upnInstance;

    @Inject
    @Claim(standard = Claims.iss)
    private Instance<String> issInstance;

    @Inject
    @Claim(standard = Claims.sub)
    private Instance<String> subInstance;

    @Inject
    @Claim(standard = Claims.iat)
    private Instance<Long> iatInstance;

    @Inject
    @Claim(standard = Claims.exp)
    private Instance<Long> expInstance;

    @Inject
    @Claim("aud")
    private Instance<Set<String>> audInstance;

    @Inject
    @Claim("groups")
    private Instance<Set<String>> groupsInstance;

    @Inject
    @Claim("email_verified")
    private Instance<Boolean> emailVerifiedInstance;

    @Inject
    @Claim(standard = Claims.jti)
    private Instance<String> jtiInstance;

    // Instance types - Optional-------------------------------------------------
    @Inject
    @Claim(standard = Claims.raw_token)
    private Instance<Optional<String>> rawTokenInstanceOptional;

    @Inject
    @Claim(standard = Claims.upn)
    private Instance<Optional<String>> upnInstanceOptional;

    @Inject
    @Claim(standard = Claims.iss)
    private Instance<Optional<String>> issInstanceOptional;

    @Inject
    @Claim(standard = Claims.sub)
    private Instance<Optional<String>> subInstanceOptional;

    @Inject
    @Claim(standard = Claims.iat)
    private Instance<Optional<Long>> iatInstanceOptional;

    @Inject
    @Claim(standard = Claims.exp)
    private Instance<Optional<Long>> expInstanceOptional;

    @Inject
    @Claim("aud")
    private Instance<Optional<Set<String>>> audInstanceOptional;

    @Inject
    @Claim("groups")
    private Instance<Optional<Set<String>>> groupsInstanceOptional;

    @Inject
    @Claim("email_verified")
    private Instance<Optional<Boolean>> emailVerifiedInstanceOptional;

    @Inject
    @Claim(standard = Claims.jti)
    private Instance<Optional<String>> jtiInstanceOptional;

    // Optional --------------------------------------------------------------
    @Inject
    @Claim(standard = Claims.raw_token)
    private Optional<String> rawTokenOptional;

    @Inject
    @Claim(standard = Claims.upn)
    private Optional<String> upnOptional;

    @Inject
    @Claim(standard = Claims.iss)
    private Optional<String> issOptional;

    @Inject
    @Claim(standard = Claims.sub)
    private Optional<String> subOptional;

    @Inject
    @Claim(standard = Claims.iat)
    private Optional<Long> iatOptional;

    @Inject
    @Claim(standard = Claims.exp)
    private Optional<Long> expOptional;

    @Inject
    @Claim("aud")
    private Optional<Set<String>> audOptional;

    @Inject
    @Claim("groups")
    private Optional<Set<String>> groupsOptional;

    @Inject
    @Claim("email_verified")
    private Optional<Boolean> emailVerifiedOptional;

    @Inject
    @Claim(standard = Claims.jti)
    private Optional<String> jtiOptional;

    // JsonValues  -----------------------------------------------------------
    @Inject
    @Claim(standard = Claims.raw_token)
    private JsonString rawTokenJsonp;

    @Inject
    @Claim(standard = Claims.upn)
    private JsonString upnJsonp;

    @Inject
    @Claim(standard = Claims.iss)
    private JsonString issJsonp;

    @Inject
    @Claim(standard = Claims.sub)
    private JsonString subJsonp;

    @Inject
    @Claim(standard = Claims.iat)
    private JsonNumber iatJsonp;

    @Inject
    @Claim(standard = Claims.exp)
    private JsonNumber expJsonp;

    @Inject
    @Claim("aud")
    private JsonArray audJsonp;

    @Inject
    @Claim("groups")
    private JsonArray groupsJsonp;

    // Spec can't support at the current time
    // enable as appropriate if/when possible
    //    @Inject
    //    @Claim("email_verified")
    //  private JsonObject/JsonValue/??? emailVerifiedJsonp;

    @Inject
    @Claim(standard = Claims.jti)
    private JsonString jtiJsonp;

    @Inject
    @Claim("roles")
    private JsonArray rolesJsonp;

    @Inject
    @Claim("customObject")
    private JsonObject customObjectJsonp;

    // JsonValues - Instance --------------------------------------------------
    @Inject
    @Claim(standard = Claims.raw_token)
    private Instance<JsonString> rawTokenJsonpInstance;

    @Inject
    @Claim(standard = Claims.upn)
    private Instance<JsonString> upnJsonpInstance;

    @Inject
    @Claim(standard = Claims.iss)
    private Instance<JsonString> issJsonpInstance;

    @Inject
    @Claim(standard = Claims.sub)
    private Instance<JsonString> subJsonpInstance;

    @Inject
    @Claim(standard = Claims.iat)
    private Instance<JsonNumber> iatJsonpInstance;

    @Inject
    @Claim(standard = Claims.exp)
    private Instance<JsonNumber> expJsonpInstance;

    @Inject
    @Claim("aud")
    private Instance<JsonArray> audJsonpInstance;

    @Inject
    @Claim("groups")
    private Instance<JsonArray> groupsJsonpInstance;

    // Spec can't support at the current time
    // enable as appropriate if/when possible
    //    @Inject
    //    @Claim("email_verified")
    //    private Instance<JsonObject/JsonValue/???> emailVerifiedJsonpInstance;

    @Inject
    @Claim(standard = Claims.jti)
    private Instance<JsonString> jtiJsonpInstance;

    @Inject
    @Claim("roles")
    private Instance<JsonArray> rolesJsonpInstance;

    @Inject
    @Claim("customObject")
    private Instance<JsonObject> customObjectJsonpInstance;

    // JsonValues - ClaimValue --------------------------------------------------
    @Inject
    @Claim(standard = Claims.raw_token)
    private ClaimValue<JsonString> rawTokenJsonpClaimValue;

    @Inject
    @Claim(standard = Claims.upn)
    private ClaimValue<JsonString> upnJsonpClaimValue;

    @Inject
    @Claim(standard = Claims.iss)
    private ClaimValue<JsonString> issJsonpClaimValue;

    @Inject
    @Claim(standard = Claims.sub)
    private ClaimValue<JsonString> subJsonpClaimValue;

    @Inject
    @Claim(standard = Claims.iat)
    private ClaimValue<JsonNumber> iatJsonpClaimValue;

    @Inject
    @Claim(standard = Claims.exp)
    private ClaimValue<JsonNumber> expJsonpClaimValue;

    @Inject
    @Claim("aud")
    private ClaimValue<JsonArray> audJsonpClaimValue;

    @Inject
    @Claim("groups")
    private ClaimValue<JsonArray> groupsJsonpClaimValue;

    // Spec can't support at the current time
    // enable as appropriate if/when possible
    //    @Inject
    //    @Claim("email_verified")
    //    private ClaimValue<JsonObject/JsonValue/???> emailVerifiedJsonpClaimValue;

    @Inject
    @Claim(standard = Claims.jti)
    private ClaimValue<JsonString> jtiJsonpClaimValue;

    @Inject
    @Claim("roles")
    private ClaimValue<JsonArray> rolesJsonpClaimValue;

    @Inject
    @Claim("customObject")
    private ClaimValue<JsonObject> customObjectJsonpClaimValue;

    // JsonValues - Optional --------------------------------------------------
    @Inject
    @Claim(standard = Claims.raw_token)
    private Optional<JsonString> rawTokenJsonpOptional;

    @Inject
    @Claim(standard = Claims.upn)
    private Optional<JsonString> upnJsonpOptional;

    @Inject
    @Claim(standard = Claims.iss)
    private Optional<JsonString> issJsonpOptional;

    @Inject
    @Claim(standard = Claims.sub)
    private Optional<JsonString> subJsonpOptional;

    @Inject
    @Claim(standard = Claims.iat)
    private Optional<JsonNumber> iatJsonpOptional;

    @Inject
    @Claim(standard = Claims.exp)
    private Optional<JsonNumber> expJsonpOptional;

    @Inject
    @Claim("aud")
    private Optional<JsonArray> audJsonpOptional;

    @Inject
    @Claim("groups")
    private Optional<JsonArray> groupsJsonpOptional;

    // Spec can't support at the current time
    // enable as appropriate if/when possible
    //    @Inject
    //    @Claim("email_verified")
    //    private Optional<JsonObject/JsonValue/???> emailVerifiedJsonpOptional;

    @Inject
    @Claim(standard = Claims.jti)
    private Optional<JsonString> jtiJsonpOptional;

    @Inject
    @Claim("roles")
    private Optional<JsonArray> rolesJsonpOptional;

    @Inject
    @Claim("customObject")
    private Optional<JsonObject> customObjectJsonpOptional;

    // Provider - Optional JsonValues --------------------------------------------
    @Inject
    @Claim(standard = Claims.raw_token)
    private Provider<Optional<JsonString>> rawTokenJsonpOptionalProvider;

    @Inject
    @Claim(standard = Claims.upn)
    private Provider<Optional<JsonString>> upnJsonpOptionalProvider;

    @Inject
    @Claim(standard = Claims.iss)
    private Provider<Optional<JsonString>> issJsonpOptionalProvider;

    @Inject
    @Claim(standard = Claims.sub)
    private Provider<Optional<JsonString>> subJsonpOptionalProvider;

    @Inject
    @Claim(standard = Claims.iat)
    private Provider<Optional<JsonNumber>> iatJsonpOptionalProvider;

    @Inject
    @Claim(standard = Claims.exp)
    private Provider<Optional<JsonNumber>> expJsonpOptionalProvider;

    @Inject
    @Claim("aud")
    private Provider<Optional<JsonArray>> audJsonpOptionalProvider;

    @Inject
    @Claim("groups")
    private Provider<Optional<JsonArray>> groupsJsonpOptionalProvider;

    // Spec can't support at the current time
    // enable as appropriate if/when possible
    //    @Inject
    //    @Claim("email_verified")
    //    private Provider<Optional<JsonObject/JsonValue/???>> emailVerifiedJsonpOptionalProvider;

    @Inject
    @Claim(standard = Claims.jti)
    private Provider<Optional<JsonString>> jtiJsonpOptionalProvider;

    @Inject
    @Claim("roles")
    private Provider<Optional<JsonArray>> rolesJsonpOptionalProvider;

    @Inject
    @Claim("customObject")
    private Provider<Optional<JsonObject>> customObjectJsonpOptionalProvider;

    // ClaimValue - Optional JsonValues --------------------------------------------
    @Inject
    @Claim(standard = Claims.raw_token)
    private ClaimValue<Optional<JsonString>> rawTokenJsonpOptionalClaimValue;

    @Inject
    @Claim(standard = Claims.upn)
    private ClaimValue<Optional<JsonString>> upnJsonpOptionalClaimValue;

    @Inject
    @Claim(standard = Claims.iss)
    private ClaimValue<Optional<JsonString>> issJsonpOptionalClaimValue;

    @Inject
    @Claim(standard = Claims.sub)
    private ClaimValue<Optional<JsonString>> subJsonpOptionalClaimValue;

    @Inject
    @Claim(standard = Claims.iat)
    private ClaimValue<Optional<JsonNumber>> iatJsonpOptionalClaimValue;

    @Inject
    @Claim(standard = Claims.exp)
    private ClaimValue<Optional<JsonNumber>> expJsonpOptionalClaimValue;

    @Inject
    @Claim("aud")
    private ClaimValue<Optional<JsonArray>> audJsonpOptionalClaimValue;

    @Inject
    @Claim("groups")
    private ClaimValue<Optional<JsonArray>> groupsJsonpOptionalClaimValue;

    // Spec can't support at the current time
    // enable as appropriate if/when possible
    //    @Inject
    //    @Claim("email_verified")
    //    private ClaimValue<Optional<JsonObject/JsonValue/???>> emailVerifiedJsonpOptionalClaimValue;

    @Inject
    @Claim(standard = Claims.jti)
    private ClaimValue<Optional<JsonString>> jtiJsonpOptionalClaimValue;

    @Inject
    @Claim("roles")
    private ClaimValue<Optional<JsonArray>> rolesJsonpOptionalClaimValue;

    @Inject
    @Claim("customObject")
    private ClaimValue<Optional<JsonObject>> customObjectJsonpOptionalClaimValue;

    /*****************************************************************************************/
    @Override
    protected String doWorker(String requestType) {

        StringBuffer sb = new StringBuffer();
        try {

            Utils.writeLine(sb, "in the new code");

            // send the instance values to the print method - they should always be current/accurate
            Utils.writeLine(sb, Utils.printValues(this.getClass().getCanonicalName(), null, requestType,
                    Utils.instanceToString(upnInstance), Utils.instanceToString(rawTokenInstance), Utils.instanceToString(issInstance), Utils.instanceToString(subInstance),
                    Utils.instanceToJavaType(jtiInstance), Utils.instanceToString(expInstance), Utils.instanceToString(iatInstance), Utils.instanceToJavaType(audInstance), Utils.instanceToJavaType(groupsInstance), null));

        } catch (Exception e) {
            System.out.println("In catch of ClaimInection (print standard values)");
            e.printStackTrace();
        }

        try {

            // Now, print the values for each claim as we obtain those via the supported methods/means
            // Make sure that they all match and log when they don't

            Utils.printDivider(sb);
            Utils.printDivider(sb);

            Utils.writeLine(sb, "Print values from multiple forms");

            // raw_token
            List<Utils.ValueList> rawTokenList = null;
            rawTokenList = Utils.addValueListEntry(rawTokenList, "Raw type", rawTokenRaw);
            rawTokenList = Utils.addValueListEntry(rawTokenList, "Claim Value", Utils.claimValueToString(rawTokenClaimValue));
            rawTokenList = Utils.addValueListEntry(rawTokenList, "Claim Value - Object", Utils.claimValueToString(rawTokenClaimValueObject));
            rawTokenList = Utils.addValueListEntry(rawTokenList, "Claim Value - Optional", Utils.claimValueOptionalToString(rawTokenClaimValueOptional));
            rawTokenList = Utils.addValueListEntry(rawTokenList, "Instance", Utils.instanceToString(rawTokenInstance));
            rawTokenList = Utils.addValueListEntry(rawTokenList, "Instance - Optional", Utils.instanceOptionalToString(rawTokenInstanceOptional));
            rawTokenList = Utils.addValueListEntry(rawTokenList, "Optional", Utils.optionalToString(rawTokenOptional));
            rawTokenList = Utils.addValueListEntry(rawTokenList, "JsonValue", Utils.jsonValueToString(rawTokenJsonp));
            rawTokenList = Utils.addValueListEntry(rawTokenList, "JsonValue - Instance", Utils.instanceToString(rawTokenJsonpInstance));
            //            rawTokenList = Utils.addValueListEntry(rawTokenList, "JsonValue - ClaimValue", Utils.claimValueJsonStringToString(rawTokenJsonpClaimValue));
            rawTokenList = Utils.addValueListEntry(rawTokenList, "JsonValue - ClaimValue", Utils.claimValueToString(rawTokenJsonpClaimValue));
            rawTokenList = Utils.addValueListEntry(rawTokenList, "JsonValue - Optional", Utils.optionalToString(rawTokenJsonpOptional));
            rawTokenList = Utils.addValueListEntry(rawTokenList, "Provider - Optional JsonValue", Utils.providerOptionalToString(rawTokenJsonpOptionalProvider));
            rawTokenList = Utils.addValueListEntry(rawTokenList, "ClaimValue - Optional - JsonValue", Utils.claimValueOptionalToString(rawTokenJsonpOptionalClaimValue));
            Utils.writeLine(sb, Utils.compareStrings(rawTokenRaw, Utils.claimsToString(Claims.raw_token), rawTokenList));

            // upn
            List<Utils.ValueList> upnList = null;
            upnList = Utils.addValueListEntry(upnList, "Raw type", upnRaw);
            upnList = Utils.addValueListEntry(upnList, "Claim Value", Utils.claimValueToString(upnClaimValue));
            upnList = Utils.addValueListEntry(upnList, "Claim Value - Object", Utils.claimValueToString(upnClaimValueObject));
            upnList = Utils.addValueListEntry(upnList, "Claim Value - Optional", Utils.claimValueOptionalToString(upnClaimValueOptional));
            upnList = Utils.addValueListEntry(upnList, "Instance", Utils.instanceToString(upnInstance));
            upnList = Utils.addValueListEntry(upnList, "Instance - Optional", Utils.instanceOptionalToString(upnInstanceOptional));
            upnList = Utils.addValueListEntry(upnList, "Optional", Utils.optionalToString(upnOptional));
            upnList = Utils.addValueListEntry(upnList, "JsonValue", Utils.jsonValueToString(upnJsonp));
            upnList = Utils.addValueListEntry(upnList, "JsonValue - Instance", Utils.instanceToString(upnJsonpInstance));
            upnList = Utils.addValueListEntry(upnList, "JsonValue - ClaimValue", Utils.claimValueToString(upnJsonpClaimValue));
            upnList = Utils.addValueListEntry(upnList, "JsonValue - Optional", Utils.optionalToString(upnJsonpOptional));
            upnList = Utils.addValueListEntry(upnList, "Provider - Optional JsonValue", Utils.providerOptionalToString(upnJsonpOptionalProvider));
            upnList = Utils.addValueListEntry(upnList, "ClaimValue - Optional - JsonValue", Utils.claimValueOptionalToString(upnJsonpOptionalClaimValue));
            Utils.writeLine(sb, Utils.compareStrings(upnRaw, Utils.claimsToString(Claims.upn), upnList));

            // issuer
            List<Utils.ValueList> issuerList = null;
            issuerList = Utils.addValueListEntry(issuerList, "Raw type", issRaw);
            issuerList = Utils.addValueListEntry(issuerList, "Claim Value", Utils.claimValueToString(issClaimValue));
            issuerList = Utils.addValueListEntry(issuerList, "Claim Value - Object", Utils.claimValueToString(issClaimValueObject));
            issuerList = Utils.addValueListEntry(issuerList, "Claim Value - Optional", Utils.claimValueOptionalToString(issClaimValueOptional));
            issuerList = Utils.addValueListEntry(issuerList, "Instance", Utils.instanceToString(issInstance));
            issuerList = Utils.addValueListEntry(issuerList, "Instance - Optional", Utils.instanceOptionalToString(issInstanceOptional));
            issuerList = Utils.addValueListEntry(issuerList, "Optional", Utils.optionalToString(issOptional));
            issuerList = Utils.addValueListEntry(issuerList, "JsonValue", Utils.jsonValueToString(issJsonp));
            issuerList = Utils.addValueListEntry(issuerList, "JsonValue - Instance", Utils.instanceToString(issJsonpInstance));
            issuerList = Utils.addValueListEntry(issuerList, "JsonValue - ClaimValue", Utils.claimValueToString(issJsonpClaimValue));
            issuerList = Utils.addValueListEntry(issuerList, "JsonValue - Optional", Utils.optionalToString(issJsonpOptional));
            issuerList = Utils.addValueListEntry(issuerList, "Provider - Optional JsonValue", Utils.providerOptionalToString(issJsonpOptionalProvider));
            issuerList = Utils.addValueListEntry(issuerList, "ClaimValue - Optional - JsonValue", Utils.claimValueOptionalToString(issJsonpOptionalClaimValue));
            Utils.writeLine(sb, Utils.compareStrings(issRaw, Utils.claimsToString(Claims.iss), issuerList));

            // subject
            List<Utils.ValueList> subjectList = null;
            subjectList = Utils.addValueListEntry(subjectList, "Raw type", subRaw);
            subjectList = Utils.addValueListEntry(subjectList, "Claim Value", Utils.claimValueToString(subClaimValue));
            subjectList = Utils.addValueListEntry(subjectList, "Claim Value - Object", Utils.claimValueToString(subClaimValueObject));
            subjectList = Utils.addValueListEntry(subjectList, "Claim Value - Optional", Utils.claimValueOptionalToString(subClaimValueOptional));
            subjectList = Utils.addValueListEntry(subjectList, "Instance", Utils.instanceToString(subInstance));
            subjectList = Utils.addValueListEntry(subjectList, "Instance - Optional", Utils.instanceOptionalToString(subInstanceOptional));
            subjectList = Utils.addValueListEntry(subjectList, "Optional", Utils.optionalToString(subOptional));
            subjectList = Utils.addValueListEntry(subjectList, "JsonValue", Utils.jsonValueToString(subJsonp));
            subjectList = Utils.addValueListEntry(subjectList, "JsonValue - Instance", Utils.instanceToString(subJsonpInstance));
            subjectList = Utils.addValueListEntry(subjectList, "JsonValue - ClaimValue", Utils.claimValueToString(subJsonpClaimValue));
            subjectList = Utils.addValueListEntry(subjectList, "JsonValue - Optional", Utils.optionalToString(subJsonpOptional));
            subjectList = Utils.addValueListEntry(subjectList, "Provider - Optional JsonValue", Utils.providerOptionalToString(subJsonpOptionalProvider));
            subjectList = Utils.addValueListEntry(subjectList, "ClaimValue - Optional - JsonValue", Utils.claimValueOptionalToString(subJsonpOptionalClaimValue));
            Utils.writeLine(sb, Utils.compareStrings(subRaw, Utils.claimsToString(Claims.sub), subjectList));

            // jti
            List<Utils.ValueList> jtiList = null;
            jtiList = Utils.addValueListEntry(jtiList, "Raw type", jtiRaw);
            jtiList = Utils.addValueListEntry(jtiList, "Claim Value", Utils.claimValueToString(jtiClaimValue));
            jtiList = Utils.addValueListEntry(jtiList, "Claim Value - Object", Utils.claimValueToString(jtiClaimValueObject));
            jtiList = Utils.addValueListEntry(jtiList, "Claim Value - Optional", Utils.claimValueOptionalToString(jtiClaimValueOptional));
            jtiList = Utils.addValueListEntry(jtiList, "Instance", Utils.instanceToString(jtiInstance));
            jtiList = Utils.addValueListEntry(jtiList, "Instance - Optional", Utils.instanceOptionalToString(jtiInstanceOptional));
            jtiList = Utils.addValueListEntry(jtiList, "Optional", Utils.optionalToString(jtiOptional));
            jtiList = Utils.addValueListEntry(jtiList, "JsonValue", Utils.jsonValueToString(jtiJsonp));
            jtiList = Utils.addValueListEntry(jtiList, "JsonValue - Instance", Utils.instanceToString(jtiJsonpInstance));
            jtiList = Utils.addValueListEntry(jtiList, "JsonValue - ClaimValue", Utils.claimValueToString(jtiJsonpClaimValue));
            jtiList = Utils.addValueListEntry(jtiList, "JsonValue - Optional", Utils.optionalToString(jtiJsonpOptional));
            jtiList = Utils.addValueListEntry(jtiList, "Provider - Optional JsonValue", Utils.providerOptionalToString(jtiJsonpOptionalProvider));
            jtiList = Utils.addValueListEntry(jtiList, "ClaimValue - Optional - JsonValue", Utils.claimValueOptionalToString(jtiJsonpOptionalClaimValue));
            Utils.writeLine(sb, Utils.compareStrings(jtiRaw, Utils.claimsToString(Claims.jti), jtiList));

            // issuedAtTime
            List<Utils.ValueList> issuedAtTimeList = null;
            issuedAtTimeList = Utils.addValueListEntry(issuedAtTimeList, "Raw type", Utils.longToString(iatRaw));
            issuedAtTimeList = Utils.addValueListEntry(issuedAtTimeList, "Claim Value", Utils.claimValueToString(iatClaimValue));
            issuedAtTimeList = Utils.addValueListEntry(issuedAtTimeList, "Claim Value - Object", Utils.claimValueToString(iatClaimValueObject));
            issuedAtTimeList = Utils.addValueListEntry(issuedAtTimeList, "Claim Value - Optional", Utils.claimValueOptionalToString(iatClaimValueOptional));
            issuedAtTimeList = Utils.addValueListEntry(issuedAtTimeList, "Instance", Utils.instanceToString(iatInstance));
            issuedAtTimeList = Utils.addValueListEntry(issuedAtTimeList, "Instance - Optional", Utils.instanceOptionalToString(iatInstanceOptional));
            issuedAtTimeList = Utils.addValueListEntry(issuedAtTimeList, "Optional", Utils.optionalToString(iatOptional));
            issuedAtTimeList = Utils.addValueListEntry(issuedAtTimeList, "JsonValue", Utils.jsonValueToString(iatJsonp));
            issuedAtTimeList = Utils.addValueListEntry(issuedAtTimeList, "JsonValue - Instance", Utils.instanceToString(iatJsonpInstance));
            issuedAtTimeList = Utils.addValueListEntry(issuedAtTimeList, "JsonValue - ClaimValue", Utils.claimValueToString(iatJsonpClaimValue));
            issuedAtTimeList = Utils.addValueListEntry(issuedAtTimeList, "JsonValue - Optional", Utils.optionalToString(iatJsonpOptional));
            issuedAtTimeList = Utils.addValueListEntry(issuedAtTimeList, "Provider - Optional JsonValue", Utils.providerOptionalToString(iatJsonpOptionalProvider));
            issuedAtTimeList = Utils.addValueListEntry(issuedAtTimeList, "ClaimValue - Optional - JsonValue", Utils.claimValueOptionalToString(iatJsonpOptionalClaimValue));
            Utils.writeLine(sb, Utils.compareStrings(Utils.longToString(iatRaw), Utils.claimsToString(Claims.iat), issuedAtTimeList));

            // expirationTime
            List<Utils.ValueList> expirationTimeList = null;
            expirationTimeList = Utils.addValueListEntry(expirationTimeList, "Raw type", Utils.longToString(expRaw));
            expirationTimeList = Utils.addValueListEntry(expirationTimeList, "Claim Value", Utils.claimValueToString(expClaimValue));
            expirationTimeList = Utils.addValueListEntry(expirationTimeList, "Claim Value - Object", Utils.claimValueToString(expClaimValueObject));
            expirationTimeList = Utils.addValueListEntry(expirationTimeList, "Claim Value - Optional", Utils.claimValueOptionalToString(expClaimValueOptional));
            expirationTimeList = Utils.addValueListEntry(expirationTimeList, "Instance", Utils.instanceToString(expInstance));
            expirationTimeList = Utils.addValueListEntry(expirationTimeList, "Instance - Optional", Utils.instanceOptionalToString(expInstanceOptional));
            expirationTimeList = Utils.addValueListEntry(expirationTimeList, "Optional", Utils.optionalToString(expOptional));
            expirationTimeList = Utils.addValueListEntry(expirationTimeList, "JsonValue", Utils.jsonValueToString(expJsonp));
            expirationTimeList = Utils.addValueListEntry(expirationTimeList, "JsonValue - Instance", Utils.instanceToString(expJsonpInstance));
            expirationTimeList = Utils.addValueListEntry(expirationTimeList, "JsonValue - ClaimValue", Utils.claimValueToString(expJsonpClaimValue));
            expirationTimeList = Utils.addValueListEntry(expirationTimeList, "JsonValue - Optional", Utils.optionalToString(expJsonpOptional));
            expirationTimeList = Utils.addValueListEntry(expirationTimeList, "Provider - Optional JsonValue", Utils.providerOptionalToString(expJsonpOptionalProvider));
            expirationTimeList = Utils.addValueListEntry(expirationTimeList, "ClaimValue - Optional - JsonValue", Utils.claimValueOptionalToString(expJsonpOptionalClaimValue));
            Utils.writeLine(sb, Utils.compareStrings(Utils.longToString(expRaw), Utils.claimsToString(Claims.exp), expirationTimeList));

            // audiences
            List<Utils.ValueList> audiencesList = null;
            audiencesList = Utils.addValueListEntry(audiencesList, "Raw type", audRaw);
            audiencesList = Utils.addValueListEntry(audiencesList, "Claim Value", Utils.getValueToSet(audClaimValue));
            audiencesList = Utils.addValueListEntry(audiencesList, "Claim Value - Object", Utils.getValueToSet(audClaimValueObject));
            audiencesList = Utils.addValueListEntry(audiencesList, "Claim Value - Optional", Utils.claimValueOptionalToSet(audClaimValueOptional));
            audiencesList = Utils.addValueListEntry(audiencesList, "Instance", Utils.instanceToJavaType(audInstance));
            audiencesList = Utils.addValueListEntry(audiencesList, "Instance - Optional", Utils.instanceOptionalSetStringToSet(audInstanceOptional));
            audiencesList = Utils.addValueListEntry(audiencesList, "Optional", Utils.optionalToJavaType(audOptional));
            audiencesList = Utils.addValueListEntry(audiencesList, "JsonValue", Utils.jsonArrayToSet(audJsonp));
            audiencesList = Utils.addValueListEntry(audiencesList, "JsonValue - Instance", Utils.instanceJsonArrayToSet(audJsonpInstance));
            audiencesList = Utils.addValueListEntry(audiencesList, "JsonValue - ClaimValue", Utils.claimValueJsonArrayToSet(audJsonpClaimValue));
            audiencesList = Utils.addValueListEntry(audiencesList, "JsonValue - Optional", Utils.optionalJsonArrayToSet(audJsonpOptional));
            audiencesList = Utils.addValueListEntry(audiencesList, "Provider - Optional JsonValue", Utils.providerOptionalJsonArrayToSet(audJsonpOptionalProvider));
            audiencesList = Utils.addValueListEntry(audiencesList, "ClaimValue - Optional - JsonValue", Utils.claimValueOptionalJsonArrayToSet(audJsonpOptionalClaimValue));
            Utils.writeLine(sb, Utils.compareSets(audRaw, Utils.claimsToString(Claims.aud), audiencesList));

            // groups
            List<Utils.ValueList> groupsList = null;
            groupsList = Utils.addValueListEntry(groupsList, "Raw type", groupsRaw);
            groupsList = Utils.addValueListEntry(groupsList, "Claim Value", Utils.getValueToSet(groupsClaimValue));
            groupsList = Utils.addValueListEntry(groupsList, "Claim Value - Object", Utils.getValueToSet(groupsClaimValueObject));
            groupsList = Utils.addValueListEntry(groupsList, "Claim Value - Optional", Utils.claimValueOptionalToSet(groupsClaimValueOptional));
            groupsList = Utils.addValueListEntry(groupsList, "Instance", Utils.instanceToJavaType(groupsInstance));
            groupsList = Utils.addValueListEntry(groupsList, "Instance - Optional", Utils.instanceOptionalSetStringToSet(groupsInstanceOptional));
            groupsList = Utils.addValueListEntry(groupsList, "Optional", Utils.optionalToJavaType(groupsOptional));
            groupsList = Utils.addValueListEntry(groupsList, "JsonValue", Utils.jsonArrayToSet(groupsJsonp));
            groupsList = Utils.addValueListEntry(groupsList, "JsonValue - Instance", Utils.instanceJsonArrayToSet(groupsJsonpInstance));
            groupsList = Utils.addValueListEntry(groupsList, "JsonValue - ClaimValue", Utils.claimValueJsonArrayToSet(groupsJsonpClaimValue));
            groupsList = Utils.addValueListEntry(groupsList, "JsonValue - Optional", Utils.optionalJsonArrayToSet(groupsJsonpOptional));
            groupsList = Utils.addValueListEntry(groupsList, "Provider - Optional JsonValue", Utils.providerOptionalJsonArrayToSet(groupsJsonpOptionalProvider));
            groupsList = Utils.addValueListEntry(groupsList, "ClaimValue - Optional - JsonValue", Utils.claimValueOptionalJsonArrayToSet(groupsJsonpOptionalClaimValue));
            Utils.writeLine(sb, Utils.compareSets(groupsRaw, Utils.claimsToString(Claims.groups), groupsList));

            // email_verification
            List<Utils.ValueList> emailVerifiedList = null;
            emailVerifiedList = Utils.addValueListEntry(emailVerifiedList, "Raw type", Utils.booleanToString(emailVerifiedRaw));
            emailVerifiedList = Utils.addValueListEntry(emailVerifiedList, "Claim Value", Utils.claimValueToString(emailVerifiedClaimValue));
            emailVerifiedList = Utils.addValueListEntry(emailVerifiedList, "Claim Value - Object", Utils.claimValueToString(emailVerifiedClaimValueObject));
            emailVerifiedList = Utils.addValueListEntry(emailVerifiedList, "Claim Value - Optional", Utils.claimValueOptionalToString(emailVerifiedClaimValueOptional));
            emailVerifiedList = Utils.addValueListEntry(emailVerifiedList, "Instance", Utils.instanceToString(emailVerifiedInstance));
            emailVerifiedList = Utils.addValueListEntry(emailVerifiedList, "Instance - Optional", Utils.instanceOptionalToString(emailVerifiedInstanceOptional));
            emailVerifiedList = Utils.addValueListEntry(emailVerifiedList, "Optional", Utils.optionalToString(emailVerifiedOptional));
            //            emailVerifiedList = Utils.addValueListEntry(emailVerifiedList, "JsonValue", emailVerifiedClaim, Utils.jsonObjectToBoolean(emailVerifiedJsonp).toString());
            //            emailVerifiedList = Utils.addValueListEntry(emailVerifiedList, "JsonValue - Instance", emailVerifiedClaim, Utils.instanceJsonArrayToSet(emailVerifiedJsonpInstance));
            //            emailVerifiedList = Utils.addValueListEntry(emailVerifiedList, "JsonValue - ClaimValue", emailVerifiedClaim, Utils.claimValueJsonArrayToSet(emailVerifiedJsonpClaimValue));
            //            emailVerifiedList = Utils.addValueListEntry(emailVerifiedList, "JsonValue - Optional", emailVerifiedClaim, Utils.optionalJsonArrayToSet(emailVerifiedJsonpOptional));
            //            emailVerifiedList = Utils.addValueListEntry(emailVerifiedList, "Provider - Optional JsonValue", emailVerifiedClaim, Utils.providerOptionalJsonArrayToSet(emailVerifiedJsonpOptionalProvider));
            //            emailVerifiedList = Utils.addValueListEntry(emailVerifiedList, "ClaimValue - Optional - JsonValue", emailVerifiedClaim, Utils.claimValueOptionalJsonArrayToSet(emailVerifiedJsonpOptionalClaimValue));
            Utils.writeLine(sb, Utils.compareStrings(Utils.booleanToString(emailVerifiedRaw), Utils.claimsToString(Claims.email_verified), emailVerifiedList));
        } catch (Exception e) {
            System.out.println("In catch of ClaimInection");
            e.printStackTrace();
            return ("Hit the catch in " + this.getClass().getCanonicalName() + ".  Check the server logs");
        }

        System.out.println(sb.toString());
        return (sb.toString());
    }

}
