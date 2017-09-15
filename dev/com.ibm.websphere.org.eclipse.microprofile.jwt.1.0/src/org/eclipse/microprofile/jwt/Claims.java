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
package org.eclipse.microprofile.jwt;


import javax.json.JsonObject;
import java.util.Set;

/**
 * This enum represents the standardized claims that the MP-JWT specification allows for in terms of interoperability.
 * For every claim in this enum, an MP-JWT implementation must return a value of the indicated type from
 * {@link JsonWebToken#getClaim(String)} method. An implementation is free to include
 * any additional claims, and users of {@link JsonWebToken#getClaim(String)} can expect that the JSON-P corresponding
 * Java type is seen based on the JSON type of the claim.
 *
 * The set of included claims is defined by IANA, see https://www.iana.org/assignments/jwt/jwt.xhtml
 */
public enum Claims {
    // The base set of required claims that MUST have non-null values in the JsonWebToken
    iss("Issuer", String.class),
    sub("Subject", String.class),
    aud("Audience", Set.class),
    exp("Expiration Time", Long.class),
    iat("Issued At Time", Long.class),
    jti("JWT ID", String.class),
    upn("MP-JWT specific unique principal name", String.class),
    groups("MP-JWT specific groups permission grant", Set.class),
    raw_token("MP-JWT specific original bearer token", String.class),

    // The IANA registered, but MP-JWT optional claims
    nbf("Not Before", Long.class),
    auth_time("Time when the authentication occurred", Long.class),
    updated_at("Time the information was last updated", Long.class),
    azp("Authorized party - the party to which the ID Token was issued", String.class),
    nonce("Value used to associate a Client session with an ID Token", String.class),
    at_hash("Access Token hash value", Long.class),
    c_hash("Code hash value", Long.class),

    full_name("Full name", String.class),
    family_name("Surname(s) or last name(s)", String.class),
    middle_name("Middle name(s)", String.class),
    nickname("Casual name", String.class),
    given_name("Given name(s) or first name(s)", String.class),
    preferred_username("Shorthand name by which the End-User wishes to be referred to", String.class),
    email("Preferred e-mail address", String.class),
    email_verified("True if the e-mail address has been verified; otherwise false", Boolean.class),

    gender("Gender", String.class),
    birthdate("Birthday", String.class),
    zoneinfo("Time zone", String.class),
    locale("Locale", String.class),
    phone_number("Preferred telephone number", String.class),
    phone_number_verified("True if the phone number has been verified; otherwise false", Boolean.class),
    address("Preferred postal address", JsonObject.class),
    acr("Authentication Context Class Reference", String.class),
    amr("Authentication Methods References", String.class),
    sub_jwk("Public key used to check the signature of an ID Token", JsonObject.class),
    cnf("Confirmation", String.class),
    sip_from_tag("SIP From tag header field parameter value", String.class),
    sip_date("SIP Date header field value", String.class),
    sip_callid("SIP Call-Id header field value", String.class),
    sip_cseq_num("SIP CSeq numeric header field parameter value", String.class),
    sip_via_branch("SIP Via branch header field parameter value", String.class),
    orig("Originating Identity String", String.class),
    dest("Destination Identity String", String.class),
    mky("Media Key Fingerprint String", String.class),

    jwk("JSON Web Key Representing Public Key", JsonObject.class),
    jwe("Encrypted JSON Web Key", String.class),
    kid("Key identifier", String.class),
    jku("JWK Set URL", String.class),

    UNKNOWN("A catch all for any unknown claim", Void.class)
    ;

    private String description;
    private Class<?> type;
    Claims(String description, Class<?> type) {
        this.description = description;
        this.type = type;
    }

    /**
     * @return A desccription for the claim
     */
    public String getDescription() {
        return description;
    }

    /**
     * The required type of the claim
     * @return type of the claim
     */
    public Class<?> getType() {
        return type;
    }
}
