/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.saml2;

import java.util.List;

import com.ibm.websphere.security.saml2.Saml20Token;

/**
 * Brief:
 * Create the API interface of com.ibm.wsspi.security.saml2.UserCredentialResolver
 * 
 * Detail:
 * The UserCredentialResolver maps the SAML assertion to the user credentials in the Subject.
 * The UserCredentialResolver has to be implemented as a single Liberty Service or User Feature in the Liberty Server
 * and multiple UserCredentialResolver services or features will result in unpredictable behaviors.
 * 1) If the mapToUserRegistry attribute in the Service Provider configuration is set to "User", then only mapSAMLAssertionToUser method will be called.
 * The other methods, such as: mapSAMLAssertionToGroups, mapSAMLAssertionToUserUniqueID and mapSAMLAssertionToRealm will be ignored.
 * 2) If the mapToUserRegistry in the Service Provider is set to "No" , the valid value returned from mapSAMLAssertionToUser, mapSAMLAssertionToGroups,
 * mapSAMLAssertionToUserUniqueID and mapSAMLAssertionToRealm. will be used directly without mapping further to the User Registry.
 * 3) If the mapToUserRegistry in the Service Provider is set to "Group" , the valid value returned from mapSAMLAssertionToUser, mapSAMLAssertionToUserUniqueID and
 * mapSAMLAssertionToRealm, will be used directly without mapping further to the User Registry. However the values returned from mapSAMLAssertionToGroups will still be mapped to
 * the User Registry.
 * 4) An invalid value returned from these APIs will be ignore and the Service Provider will continue its regular processes to find a valid value. For example, if the
 * mapSAMLAssertionToUser returns an empty or null string, the Service Provider will continue to get a valid User Name from the SAML Token with its regular processes.
 * 
 */

public interface UserCredentialResolver {

    /**
     * This API maps a Saml20Token into a User Name. A valid user name cannot be null or empty.
     * 
     * @param saml20Token -- the token with SAML Assertion (see com.ibm.wsspi.security.saml2.Saml20Token)
     * @return string -- the user name. If value is null or empty String, the Service Provider will resolve user name with its regular processes.
     * @exception UserIdentityException -- The Service Provider will fail the SAML Token.
     */
    public String mapSAMLAssertionToUser(Saml20Token token) throws UserIdentityException;

    /**
     * This API maps a Saml20Token into a Group list. A valid Group list can not be null or empty. This method is ignored when mapToUserRegistry is set to "User". The Group list
     * will be mapped further to the User Registry when mapToUserRegistry is set to "Group".
     * 
     * @param saml20Token -- the token with SAML Assertion (see com.ibm.wsspi.security.saml2.Saml20Token)
     * @return ArrayList -- the Group list. If value is null or empty list, the Service Provider will continue resolve group list with its processes.
     * @exception UserIdentityException -- The Service Provider will fail the SAML Token.
     */
    public List<String> mapSAMLAssertionToGroups(Saml20Token token) throws UserIdentityException;

    /**
     * This API maps a Saml20Token into a user unique ID. A valid user unique ID can not be null or empty. This method is ignored when mapToUserRegistry is set to "User".
     * 
     * @param saml20Token -- the token with SAML Assertion (see com.ibm.wsspi.security.saml2.Saml20Token)
     * @return string -- a valid user unique ID. If value is null or empty string, the Service Provider will continue resolve unique user id with its regular processes.
     * @exception UserIdentityException -- The Service Provider will fail the SAML Token.
     */
    public String mapSAMLAssertionToUserUniqueID(Saml20Token token) throws UserIdentityException;

    /**
     * This API maps a Saml20Token into a Realm. A valid Realm can not be null or empty. This method is ignored when mapToUserRegistry is set to "User".
     * 
     * @param saml20Token -- the token with SAML Assertion (see com.ibm.wsspi.security.saml2.Saml20Token)
     * @return string -- a valid Realm. If value is null or empty string ,the Service Provider will resolve realm with its regular processes.
     * @exception UserIdentityException -- The Service Provider will fail the SAML Token.
     */
    public String mapSAMLAssertionToRealm(Saml20Token token) throws UserIdentityException;

}
