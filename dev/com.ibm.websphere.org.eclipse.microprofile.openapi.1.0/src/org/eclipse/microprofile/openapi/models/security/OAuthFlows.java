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

package org.eclipse.microprofile.openapi.models.security;

import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.Extensible;

/**
 * Configuration of the supported OAuthFlows
 *
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/3.0.0-rc2/versions/3.0.md#oauthFlowsObject">OAuthFlows Object</a>
 */
public interface OAuthFlows extends Constructible, Extensible {

    /**
     * This method returns the implicit property from OAuthFlows instance.
     * 
     * @return OAuthFlow implicit
     **/
    OAuthFlow getImplicit();

    /**
     * This method sets the implicit property of OAuthFlows instance to the given implicit argument.
     * 
     * @param implicit the OauthFlow instance
     */
    void setImplicit(OAuthFlow implicit);

    /**
     * This method sets the implicit property of OAuthFlows instance to the given implicit argument and returns the modified instance.
     * 
     * @param implicit the OauthFlow instance
     * @return OAuthFlows instance with the set implicit property
     */
    OAuthFlows implicit(OAuthFlow implicit);

    /**
     * OAuth Resource Owner Password flow
     * <p>
     * This method returns the password property from OAuthFlows instance.
     * </p> 
     * @return OAuthFlow password
     **/
    OAuthFlow getPassword();

    /**
     * OAuth Resource Owner Password flow
     * <p>
     * This method sets the password property of OAuthFlows instance to the given password argument.
     * </p> 
     * @param password the OAuthFlow instance
     */
    void setPassword(OAuthFlow password);

    /**
     * OAuth Resource Owner Password flow
     * <p>
     * This method sets the password property of an OAuthFlows instance to the given password argument and returns the modified instance.
     * </p>
     * @param password the OauthFlow instance
     * @return OAuthFlows instance with the set password property
     */
    OAuthFlows password(OAuthFlow password);

    /**
     * OAuth Client Credential flow; previously called application in OpenAPI 2.0
     * <p>
     * This method returns the clientCredentials property from OAuthFlows instance.
     * </p>
     * @return OAuthFlow clientCredentials
     **/
    OAuthFlow getClientCredentials();

    /**
     * OAuth Client Credential flow; previously called application in OpenAPI 2.0
     * <p>
     * This method sets the clientCredentials property of OAuthFlows instance to the given clientCredentials argument.
     * </p> 
     * @param clientCredentials the OauthFlow instance
     */
    void setClientCredentials(OAuthFlow clientCredentials);

    /**
     * OAuth Client Credential flow; previously called application in OpenAPI 2.0
     * <p>
     * This method sets the clientCredentials property of OAuthFlows instance to the given clientCredentials argument and returns the modified
     * instance.
     * </p> 
     * @param clientCredentials the OauthFlow instance
     * @return OAuthFlows instance with the set clientCredentials property
     */
    OAuthFlows clientCredentials(OAuthFlow clientCredentials);

    /**
     * OAuth Authorization Code flow; previously called accessCode in OpenAPI 2.0
     * <p>
     * This method returns the authorizationCode property from OAuthFlows instance.
     * </p> 
     * @return OAuthFlow authorizationCode
     **/
    OAuthFlow getAuthorizationCode();

    /**
     * OAuth Authorization Code flow; previously called accessCode in OpenAPI 2.0
     * <p>
     * This method sets the authorizationCode property of OAuthFlows instance to the given authorizationCode argument.
     * </p>
     * @param authorizationCode the OauthFlow instance
     */
    void setAuthorizationCode(OAuthFlow authorizationCode);

    /**
     * OAuth Authorization Code flow; previously called accessCode in OpenAPI 2.0
     * <p>
     * This method sets the authorizationCode property of OAuthFlows instance to the given authorizationCode argument and returns the modified
     * instance.
     * </p> 
     * @param authorizationCode the OauthFlow instance
     * @return OAuthFlows instance with the set authorizationCode property
     */
    OAuthFlows authorizationCode(OAuthFlow authorizationCode);

}