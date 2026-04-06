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

import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.PASSWORD_XOR_VALID;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_JASMINE;
import static jakarta.security.enterprise.identitystore.IdentityStore.ValidationType.PROVIDE_GROUPS;
import static jakarta.security.enterprise.identitystore.IdentityStore.ValidationType.VALIDATE;

import jakarta.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import jakarta.security.enterprise.identitystore.InMemoryIdentityStoreDefinition;
import jakarta.security.enterprise.identitystore.InMemoryIdentityStoreDefinition.Credentials;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS Application class with InMemoryIdentityStoreDefinition.
 * This defines the in-memory identity store with one credential and a high priority.
 */
@BasicAuthenticationMechanismDefinition(realmName = "InMemoryIdentityStoreRealm")
@InMemoryIdentityStoreDefinition(
                                 priority = 5,
                                 priorityExpression = "${80/20}", // evaluates to 4
                                 useFor = { VALIDATE, PROVIDE_GROUPS },
                                 value = {
                                           @Credentials(
                                                        callerName = USER_JASMINE,
                                                        password = PASSWORD_XOR_VALID,
                                                        groups = { "caller", "user" }),

                                 })
@ApplicationPath("/")
public class InMemoryIdentityStoreHighestPriorityApplication extends Application {
}
