/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package multiple.identity.stores;

import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.DB_USER_ALICE;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.PASSWORD_XOR_VALID;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_JASMINE;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_LISA;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.VALID_PASSWORD;
import static jakarta.security.enterprise.identitystore.IdentityStore.ValidationType.PROVIDE_GROUPS;
import static jakarta.security.enterprise.identitystore.IdentityStore.ValidationType.VALIDATE;

import jakarta.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import jakarta.security.enterprise.identitystore.InMemoryIdentityStoreDefinition;
import jakarta.security.enterprise.identitystore.InMemoryIdentityStoreDefinition.Credentials;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS Application class with both InMemoryIdentityStoreDefinition and a custom database identity store.
 * This application tests the priority mechanism when multiple identity stores of different types exist.
 *
 * The in-memory identity store has priority 100 (higher priority, lower number).
 * The custom database identity store has priority 200 (lower priority, higher number).
 *
 * Expected behaviour:
 * - Users defined in the in-memory store should be validated by the in-memory store first
 * - Users only in the database store should be validated by the database store
 * - If a user exists in both stores, the in-memory store (higher priority) should be used
 */
@BasicAuthenticationMechanismDefinition(realmName = "MultipleIdentityStoresRealm")
@InMemoryIdentityStoreDefinition(
                                 priority = 100,
                                 useFor = { VALIDATE, PROVIDE_GROUPS },
                                 value = {
                                           // Plain text password - valid groups
                                           @Credentials(
                                                        callerName = USER_JASMINE,
                                                        password = VALID_PASSWORD,
                                                        groups = { "caller", "user" }),

                                           // XOR encoded password - valid groups
                                           @Credentials(
                                                        callerName = USER_LISA,
                                                        password = PASSWORD_XOR_VALID,
                                                        groups = { "caller", "user" }),

                                           // User that also exists in database store with different password
                                           // This should take precedence due to higher priority
                                           @Credentials(
                                                        callerName = DB_USER_ALICE,
                                                        password = VALID_PASSWORD,
                                                        groups = { "memoryuser", "user" })
                                 })
@ApplicationPath("/")
public class MultipleIdentityStoresApplication extends Application {
}