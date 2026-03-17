/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package inmemory.identity.store;

import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_JASMINE;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.VALID_PASSWORD;
import static jakarta.security.enterprise.identitystore.IdentityStore.ValidationType.PROVIDE_GROUPS;
import static jakarta.security.enterprise.identitystore.IdentityStore.ValidationType.VALIDATE;

import jakarta.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import jakarta.security.enterprise.identitystore.InMemoryIdentityStoreDefinition;
import jakarta.security.enterprise.identitystore.InMemoryIdentityStoreDefinition.Credentials;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS Application class with InMemoryIdentityStoreDefinition.
 * This defines the in-memory identity store
 * The priorityExpression is invalid and should result in the CWWKS2603W warning and the default being set instead
 */
@BasicAuthenticationMechanismDefinition(realmName = "InMemoryIdentityStoreRealm")
@InMemoryIdentityStoreDefinition(
                                 priority = 10,
                                 priorityExpression = "SOME RUBBISH", // Gives warning and defaults to 1. Cannot be resolved to a property so it throws an ELException in the InMemoryIdentityStoreDefinitionWrapper
                                 useFor = { VALIDATE, PROVIDE_GROUPS },
                                 value = {
                                           // Plain text password - valid groups
                                           @Credentials(
                                                        callerName = USER_JASMINE,
                                                        password = VALID_PASSWORD,
                                                        groups = { "caller", "user" }),
                                 })
@ApplicationPath("/")
public class ELExceptionInMemoryIdentityStoreApplication extends Application {
}
