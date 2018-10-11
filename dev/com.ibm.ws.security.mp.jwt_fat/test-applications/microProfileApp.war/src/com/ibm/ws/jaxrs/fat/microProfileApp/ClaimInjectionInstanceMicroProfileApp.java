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

import java.util.Optional;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonString;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;

import com.ibm.ws.security.jwt.fat.mpjwt.CommonMicroProfileApp;

// http://localhost:8010/microProfileApp/rest/ClaimInjection/MicroProfileClaimInjectionApp
// allow the same methods to invoke GET, POST, PUT, ... invocation type determines which gets invoked.

public class ClaimInjectionInstanceMicroProfileApp extends CommonMicroProfileApp {

    // Provider types ---> Changed to Instance in latest spec
    @Inject
    @Claim(standard = Claims.upn)
    private Instance<String> instanceUpn;

    @Inject
    @Claim(standard = Claims.raw_token)
    private Instance<String> instanceRawToken;

    @Inject
    @Claim(standard = Claims.sub)
    private Instance<String> instanceSubject;

    @Inject
    @Claim(standard = Claims.iss)
    private Instance<String> instanceIssuer;

    @Inject
    @Claim(standard = Claims.jti)
    private Instance<String> instanceJti;

    @Inject
    @Claim(standard = Claims.iat)
    private Instance<Long> instanceIat;

    @Inject
    @Claim(standard = Claims.exp)
    private Instance<Long> instanceExp;

    @Inject
    @Claim("aud")
    private Instance<Set<String>> instanceAudiences;

    @Inject
    @Claim("groups")
    private Instance<Set<String>> instanceGroups;

    /******************/
    @Inject
    @Claim(standard = Claims.sub)
    private Instance<Optional<String>> providerOptSubject;

    @Inject
    @Claim("aud")
    private Instance<Optional<Set<String>>> providerOptAud;
    /******************************/

    // Provider with JsonValue types ---> Changed to Instance in latest spec
    @Inject
    @Claim(standard = Claims.sub)
    private Instance<JsonString> providerJsonpSubject;

    @Inject
    @Claim("aud")
    private Instance<JsonArray> providerJsonAud;

    @Override
    protected String doWorker(String requestType) {

        StringBuffer sb = new StringBuffer();
        try {

            Utils.writeLine(sb, "in the new code");

            // send the instance values to the print method - they should always be current/accurate
            Utils.writeLine(sb, Utils.printValues(this.getClass().getCanonicalName(), null, requestType,
                    Utils.instanceToString(instanceUpn), Utils.instanceToString(instanceRawToken), Utils.instanceToString(instanceIssuer), Utils.instanceToString(instanceSubject),
                    Utils.instanceToString(instanceJti), Utils.instanceToString(instanceExp), Utils.instanceToString(instanceIat), Utils.instanceToJavaType(instanceAudiences), Utils.instanceToJavaType(instanceGroups), null));

        } catch (Exception e) {
            System.out.println("In catch of ClaimInection (print standard values)");
            e.printStackTrace();
        }

        return (sb.toString());
    }

}
