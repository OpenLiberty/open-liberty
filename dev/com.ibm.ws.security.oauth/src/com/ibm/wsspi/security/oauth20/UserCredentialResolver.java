/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.oauth20;

import java.util.List;

/**
 * 
 * Create the SPI interface of com.ibm.wsspi.security.oauth.UserCredentialResolver
 * 
 * 
 * The UserCredentialResolver maps the Access token to the user credentials in the Subject.
 * The UserCredentialResolver has to be implemented as a single Liberty Service or User Feature in the Liberty Server
 * and multiple UserCredentialResolver services or features will result in unpredictable behavior.
 * 1) If the mapIdentityToRegistryUser attribute in the OpenID Connect Client configuration is set to "true", then only mapToUser method will be called.
 * The other methods, such as: mapToGroups, mapToUserUniqueID and mapToRealm will be ignored.
 * 2) If the mapIdentityToRegistryUser  attribute in the OpenID Connect Client configuration is set to "false" , then the valid values (must be non null and non empty string) returned from mapToUser, mapToGroups,
 * mapToUserUniqueID and mapToRealm will be used. We do not need the User Registry in this case.
 * 3) An invalid value returned from these APIs will be ignored and the authentication process will continue with the defaults. For example, if the
 * mapToUser returns an empty or null string, then the runtime will try to get the user name from the authentication token using the claim name which is set by the userIdentifier attribute or the default claim "sub".
 * 
 */

public interface UserCredentialResolver {

    /**
     * This method is for mapping the authentication token with the user name. A valid user name cannot be null or empty.
     * 
     * @param tokenString -- the oAuth authentication token string in the JSON format. Example: 
     * {"exp":1460058764,"sub":"testuser","realmName":"BasicRealm","scope":"openid scope2 scope1","grant_type":"refresh_token","iss":"http:\/\/localhost:8940\/oidc\/endpoint\/tokenissuer","uniqueSecurityName":"testuser","active":true,"token_type":"Bearer","client_id":"client01","iat":1460058759}
     * @return string -- the user name. If this value is null or empty, then the runtime will resolve the user name using the default.
     * For example, runtime will try to get the user information using the "sub" claim in the token.
     * @exception UserIdentityException - authentication process will fail 
     */
    public String mapToUser(String tokenString) throws UserIdentityException;

    /**
     * This method is for mapping the authentication token with the list of groups. A valid list of groups cannot be null or empty. This method is ignored when the mapIdentityToRegistryUser is set to "true". 
     * 
     * @param tokenString -- the oAuth authentication token string in the JSON format. Example:
     * {"sub":"testuser","iss":"http:\/\/localhost:8940\/oidc\/endpoint\/tokenissuer","groupIds":[ "testuserdepartment","administrators" ]}
     * @return ArrayList -- the list of groups. If this value is null or empty, then the runtime will resolve the user name using the default. 
     * For example, runtime will try to get the group information using the "groupIds" claim in the token.
     * @exception UserIdentityException -- authentication process will fail.
     */
    public List<String> mapToGroups(String tokenString) throws UserIdentityException;

    /**
     * This method is for mapping the authentication token with the user unique ID. A valid user unique ID cannot be null or empty. This method is ignored when the mapIdentityToRegistryUser is set to "true".
     * 
     * @param tokenString -- the oAuth authentication token string in the JSON format. Example:
     * {"exp":1460058764,"sub":"testuser","realmName":"BasicRealm","scope":"openid scope2 scope1","grant_type":"refresh_token","iss":"http:\/\/localhost:8940\/oidc\/endpoint\/tokenissuer","uniqueSecurityName":"testuser","active":true,"token_type":"Bearer","client_id":"client01","iat":1460058759}
     * @return string -- a valid user unique ID. If this value is null or empty, then the runtime will resolve the unique user id using the default.
     * For example, runtime will try to get the unique user id using the "uniqueSecurityName" claim in the token.
     * @exception UserIdentityException -- authentication process will fail.
     */
    public String mapToUserUniqueID(String tokenString) throws UserIdentityException;

    /**
     * This method is for mapping the authentication token with the Realm. A valid Realm cannot be null or empty. This method is ignored when the mapIdentityToRegistryUser is set to "true".
     * 
     * @param tokenString -- the oAuth authentication token string in the JSON format. Example:
     * {"exp":1460058764,"sub":"testuser","realmName":"BasicRealm","scope":"openid scope2 scope1","grant_type":"refresh_token","iss":"http:\/\/localhost:8940\/oidc\/endpoint\/tokenissuer","uniqueSecurityName":"testuser","active":true,"token_type":"Bearer","client_id":"client01","iat":1460058759}
     * @return string -- a valid Realm. If this value is null or empty, then the runtime will resolve the unique user id using the default.
     * For example, runtime will try to get the realm information using the OpenID Connect Client configuration attribute "realmName" or the "realmName" claim in the token.
     * @exception UserIdentityException -- authentication process will fail.
     */
    public String mapToRealm(String tokenString) throws UserIdentityException;

}
