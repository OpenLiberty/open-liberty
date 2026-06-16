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

import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.DB_PASSWORD;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.DB_USER_ALICE;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.DB_USER_CHARLIE;
import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.DB_USER_RORY;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;

/**
 * Custom database-like identity store implementation for testing.
 * This simulates a database identity store with hard-coded credentials.
 * It has a lower priority (higher number) than the in-memory identity store,
 * so the in-memory store should be used first for credential validation.
 */
@ApplicationScoped
public class CustomDatabaseIdentityStore implements IdentityStore {

    private static final String CLASS_NAME = CustomDatabaseIdentityStore.class.getName();
    private static final Logger log = Logger.getLogger(CLASS_NAME);

    public CredentialValidationResult validate(UsernamePasswordCredential credential) {
        log.info(CLASS_NAME + " - validate called for user: " + credential.getCaller());

        String username = credential.getCaller();
        String password = credential.getPasswordAsString();
        Set<String> groups = new HashSet<>();

        // Simulate database lookup
        if (DB_USER_ALICE.equals(username) && DB_PASSWORD.equals(password)) {
            log.info(CLASS_NAME + " - User " + username + " validated successfully from database store");
            groups.add("dbuser");
            groups.add("caller");
            return new CredentialValidationResult(username, groups);
        } else if (DB_USER_RORY.equals(username) && DB_PASSWORD.equals(password)) {
            log.info(CLASS_NAME + " - User " + username + " validated successfully from database store");
            groups.add("dbuser");
            groups.add("user");
            return new CredentialValidationResult(username, groups);
        } else if (DB_USER_CHARLIE.equals(username) && DB_PASSWORD.equals(password)) {
            log.info(CLASS_NAME + " - User " + username + " validated successfully from database store");
            groups.add("dbadmin");
            groups.add("user");
            return new CredentialValidationResult(username, groups);
        }

        log.info(CLASS_NAME + " - User " + username + " validation failed in database store");
        return CredentialValidationResult.INVALID_RESULT;
    }

    @Override
    public int priority() {
        log.info(CLASS_NAME + " - priority: 200");
        // Higher priority number = lower priority
        // This should be checked AFTER the in-memory store (which has priority 100)
        return 200;
    }

    @Override
    public Set<ValidationType> validationTypes() {
        log.info(CLASS_NAME + " - validationTypes: VALIDATE and PROVIDE_GROUPS");
        return EnumSet.of(ValidationType.VALIDATE, ValidationType.PROVIDE_GROUPS);
    }
}