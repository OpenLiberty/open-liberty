/**
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 * Copyright 2017 SmartBear Software
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.microprofile.openapi.annotations.security;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configuration details for a supported OAuth Flow.
 * 
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#oauthFlowObject"> OAuth Flow Object</a>
 **/
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface OAuthFlow {
    /**
     * The authorization URL to be used for this flow. 
     * <p>
     * This is a REQUIRED property and MUST be in the form of a URL. 
     * Applies to oauth2 ("implicit", "authorizationCode") type.
     * </p>
     * @return authorization URL for this flow
     **/
    String authorizationUrl() default "";

    /**
     * The token URL to be used for this flow. 
     * <p>
     * This is a REQUIRED property and MUST be in the form of a URL. 
     * Applies to oauth2 ("password", "clientCredentials", "authorizationCode") type.
     * </p>
     * @return token URL for this flow
     **/
    String tokenUrl() default "";

    /**
     * The URL to be used for obtaining refresh tokens.
     * <p> 
     * This MUST be in the form of a URL. Applies to oauth2 type.
     * </p>
     * @return URL for obtaining refresh tokens
     **/
    String refreshUrl() default "";

    /**
     * This is a REQUIRED property.
     * <p>
     * The available scopes for the OAuth2 security scheme. 
     * Applies to oauth2 type.
     * </p>
     * @return scopes available for this security scheme
     **/
    OAuthScope[] scopes() default {};

}
