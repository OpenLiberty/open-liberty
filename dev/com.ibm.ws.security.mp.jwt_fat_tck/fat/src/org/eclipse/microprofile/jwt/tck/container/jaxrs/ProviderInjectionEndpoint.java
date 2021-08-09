/*
 * Copyright (c) 2016-2017 Contributors to the Eclipse Foundation
 *
 *  See the NOTICE file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.eclipse.microprofile.jwt.tck.container.jaxrs;

import java.util.Optional;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;

@Path("/endp")
@RolesAllowed({ "Echoer", "Tester" })
@RequestScoped
public class ProviderInjectionEndpoint {
    @Inject
    @Claim("raw_token")
    private Provider<String> rawToken;
    @Inject
    @Claim("iss")
    private Provider<String> issuer;
    @Inject
    @Claim("jti")
    private Provider<String> jti;
    @Inject
    @Claim("aud")
    private Provider<Set<String>> aud;
    @Inject
    @Claim("iat")
    private Provider<Long> issuedAt;
    @Inject
    @Claim("sub")
    private Provider<Optional<String>> optSubject;
    @Inject
    @Claim("auth_time")
    private Provider<Optional<Long>> authTime;
    @Inject
    @Claim("custom-missing")
    private Provider<Optional<Long>> custom;
    @Inject
    @Claim("customString")
    private Provider<String> customString;
    @Inject
    @Claim("customInteger")
    private Provider<JsonNumber> customInteger;
    @Inject
    @Claim("customDouble")
    private Provider<JsonNumber> customDouble;

    @GET
    @Path("/verifyInjectedIssuer")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedIssuer(@QueryParam("iss") String iss) {
        boolean pass = false;
        String msg;
        String issValue = issuer.get();
        if (issValue == null || issValue.length() == 0) {
            msg = Claims.iss.name() + "value is null or empty, FAIL";
        } else if (issValue.equals(iss)) {
            msg = Claims.iss.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.iss.name(), issValue, iss);
        }
        JsonObject result = Json.createObjectBuilder()
                        .add("pass", pass)
                        .add("msg", msg)
                        .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedRawToken")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedRawToken(@QueryParam("raw_token") String rt) {
        boolean pass = false;
        String msg;
        // raw_token
        String rawTokenValue = rawToken.get();
        if (rawTokenValue == null || rawTokenValue.length() == 0) {
            msg = Claims.raw_token.name() + "value is null or empty, FAIL";
        } else if (rawTokenValue.equals(rt)) {
            msg = Claims.raw_token.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.raw_token.name(), rawTokenValue, rt);
        }
        JsonObject result = Json.createObjectBuilder()
                        .add("pass", pass)
                        .add("msg", msg)
                        .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedJTI")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedJTI(@QueryParam("jti") String jwtID) {
        boolean pass = false;
        String msg;
        // jti
        String jtiValue = jti.get();
        if (jtiValue == null || jtiValue.length() == 0) {
            msg = Claims.jti.name() + "value is null or empty, FAIL";
        } else if (jtiValue.equals(jwtID)) {
            msg = Claims.jti.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.jti.name(), jtiValue, jwtID);
        }
        JsonObject result = Json.createObjectBuilder()
                        .add("pass", pass)
                        .add("msg", msg)
                        .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedAudience")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedAudience(@QueryParam("aud") String audience) {
        boolean pass = false;
        String msg;
        // aud
        Set<String> audValue = aud.get();
        if (audValue == null || audValue.size() == 0) {
            msg = Claims.aud.name() + "value is null or empty, FAIL";
        } else if (audValue.contains(audience)) {
            msg = Claims.aud.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.aud.name(), audValue, audience);
        }
        JsonObject result = Json.createObjectBuilder()
                        .add("pass", pass)
                        .add("msg", msg)
                        .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedIssuedAt")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedIssuedAt(@QueryParam("iat") Long iat) {
        boolean pass = false;
        String msg;
        // iat
        Long iatValue = issuedAt.get();
        if (iatValue == null || iatValue.intValue() == 0) {
            msg = Claims.iat.name() + "value is null or empty, FAIL";
        } else if (iatValue.equals(iat)) {
            msg = Claims.iat.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.iat.name(), iatValue, iat);
        }
        JsonObject result = Json.createObjectBuilder()
                        .add("pass", pass)
                        .add("msg", msg)
                        .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedOptionalSubject")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedOptionalSubject(@QueryParam("sub") String subject) {
        boolean pass = false;
        String msg;
        // sub
        Optional<String> optSubValue = optSubject.get();
        if (optSubValue == null || !optSubValue.isPresent()) {
            msg = Claims.sub.name() + " value is null or missing, FAIL";
        } else if (optSubValue.get().equals(subject)) {
            msg = Claims.sub.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.sub.name(), optSubValue, subject);
        }
        JsonObject result = Json.createObjectBuilder()
                        .add("pass", pass)
                        .add("msg", msg)
                        .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedOptionalAuthTime")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedOptionalAuthTime(@QueryParam("auth_time") Long authTime) {
        boolean pass = false;
        String msg;
        // auth_time
        Optional<Long> optAuthTimeValue = this.authTime.get();
        if (optAuthTimeValue == null || !optAuthTimeValue.isPresent()) {
            msg = Claims.auth_time.name() + " value is null or missing, FAIL";
        } else if (optAuthTimeValue.get().equals(authTime)) {
            msg = Claims.auth_time.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.auth_time.name(), optAuthTimeValue, authTime);
        }
        JsonObject result = Json.createObjectBuilder()
                        .add("pass", pass)
                        .add("msg", msg)
                        .build();
        return result;
    }

    /**
     * Verify that values exist and that types match the corresponding Claims enum
     *
     * @return a series of pass/fail statements regarding the check for each injected claim
     */
    @GET
    @Path("/verifyInjectedOptionalCustomMissing")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedOptionalCustomMissing() {
        boolean pass = false;
        String msg;
        // custom-missing
        Optional<Long> customValue = custom.get();
        if (customValue == null) {
            msg = "custom-missing value is null, FAIL";
        } else if (!customValue.isPresent()) {
            msg = "custom-missing PASS";
            pass = true;
        } else {
            msg = String.format("custom: %s != %s", null, customValue.get());
        }
        JsonObject result = Json.createObjectBuilder()
                        .add("pass", pass)
                        .add("msg", msg)
                        .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedCustomString")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedCustomString(@QueryParam("value") String value) {
        boolean pass = false;
        String msg;
        // iat
        String customValue = customString.get();
        if (customValue == null || customValue.length() == 0) {
            msg = "customString value is null or empty, FAIL";
        } else if (customValue.equals(value)) {
            msg = "customString PASS";
            pass = true;
        } else {
            msg = String.format("customString: %s != %s", customValue, value);
        }
        JsonObject result = Json.createObjectBuilder()
                        .add("pass", pass)
                        .add("msg", msg)
                        .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedCustomInteger")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedCustomInteger(@QueryParam("value") Long value) {
        boolean pass = false;
        String msg;
        // iat
        Object test = customInteger.get();
        System.out.printf("+++ verifyInjectedCustomInteger, JsonNumber.class.CL: %s\n",
                          JsonNumber.class.getClassLoader());
        System.out.printf("+++ customInteger.CL: %s\n",
                          test.getClass().getClassLoader());
        Class[] ifaces = test.getClass().getInterfaces();
        for (Class iface : ifaces) {
            System.out.printf("%s: %s\n", iface, iface.getClassLoader());
        }
        JsonNumber customValue = JsonNumber.class.cast(test);
        if (customValue == null || customValue.isIntegral() == false) {
            msg = "customInteger value is null or not integral, FAIL";
        } else if (customValue.longValueExact() == value) {
            msg = "customInteger PASS";
            pass = true;
        } else {
            msg = String.format("customInteger: %d != %d", customValue, value);
        }
        JsonObject result = Json.createObjectBuilder()
                        .add("pass", pass)
                        .add("msg", msg)
                        .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedCustomDouble")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedCustomDouble(@QueryParam("value") Double value) {
        boolean pass = false;
        String msg;
        // iat
        JsonNumber customValue = customDouble.get();
        if (customValue == null) {
            msg = "customDouble value is null, FAIL";
        } else if (Math.abs(customValue.doubleValue() - value.doubleValue()) < 0.00001) {
            msg = "customDouble PASS";
            pass = true;
        } else {
            msg = String.format("customDouble: %s != %.8f", customValue, value);
        }
        JsonObject result = Json.createObjectBuilder()
                        .add("pass", pass)
                        .add("msg", msg)
                        .build();
        return result;
    }

}
