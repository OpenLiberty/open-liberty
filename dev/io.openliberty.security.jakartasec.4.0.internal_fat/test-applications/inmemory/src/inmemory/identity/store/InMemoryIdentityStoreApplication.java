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

import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.PASSWORD_AES_VALID;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.PASSWORD_HASH_VALID;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.PASSWORD_XOR_INVALID;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.PASSWORD_XOR_VALID;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_BILL;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_FRANK;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_JASMINE;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_JOHNNY;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_LISA;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_SALLY;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.USER_THEO;
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
 * This defines the in-memory identity store with multiple credentials using
 * different password encoding schemes: plain text, XOR, AES, and Hash.
 */
@BasicAuthenticationMechanismDefinition(realmName = "InMemoryIdentityStoreRealm")
@InMemoryIdentityStoreDefinition(
                                 priority = 10,
                                 priorityExpression = "${80/20}", // evaluates to 4
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

                                           // XOR encoded password - invalid groups (no access to API)
                                           @Credentials(
                                                        callerName = USER_BILL,
                                                        password = PASSWORD_XOR_VALID,
                                                        groups = { "foo", "bar" }),

                                           // Bad XOR encoding - intentional error to test error message
                                           @Credentials(
                                                        callerName = USER_JOHNNY,
                                                        password = PASSWORD_XOR_INVALID,
                                                        groups = { "foo", "bar" }),

                                           // Hash encoded password - valid groups
                                           @Credentials(
                                                        callerName = USER_FRANK,
                                                        groups = { "user" },
                                                        password = PASSWORD_HASH_VALID),

                                           // AES encoded password - valid groups
                                           @Credentials(
                                                        callerName = USER_SALLY,
                                                        groups = { "user" },
                                                        password = PASSWORD_AES_VALID),

                                           // valid password - valid groups
                                           @Credentials(
                                                        callerName = USER_THEO,
                                                        groups = { "user" },
                                                        password = VALID_PASSWORD)
                                 })
@ApplicationPath("/")
public class InMemoryIdentityStoreApplication extends Application {
}
