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
package multiple.identity.stores.handlers;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;
import jakarta.security.enterprise.identitystore.IdentityStore.ValidationType;
import jakarta.security.enterprise.identitystore.IdentityStoreHandler;

/**
 * Custom implementation of IdentityStoreHandler for handling multiple identity stores.
 *
 * 1. Validates credentials by iterating through identity stores in priority order
 * (lower priority number = higher priority)
 * 2. Stops at the first store that successfully validates the credential
 * 3. Aggregates groups from all stores that support PROVIDE_GROUPS (all stores supporting PROVIDE_GROUPS are consulted)
 * 4. Handles both VALIDATE and PROVIDE_GROUPS validation types
 *
 */
@ApplicationScoped
public class CustomIdentityStoreHandler implements IdentityStoreHandler {

    private static final String CLASS_NAME = CustomIdentityStoreHandler.class.getName();
    private static final Logger log = Logger.getLogger(CLASS_NAME);
    private static boolean supportGroups = false;

    @Inject
    private Instance<IdentityStore> identityStores;

    /**
     * Validates the given credential using available identity stores.
     *
     * @param credential The credential to validate
     * @return CredentialValidationResult containing the validation result and aggregated groups
     */
    @Override
    public CredentialValidationResult validate(Credential credential) {
        log.info(CLASS_NAME + " - validate called for credential: " + credential.getClass().getSimpleName());

        // Get all available identity stores and sort by priority
        List<IdentityStore> stores = getSortedIdentityStores();

        if (stores.isEmpty()) {
            log.warning(CLASS_NAME + " - No identity stores available");
            return CredentialValidationResult.INVALID_RESULT;
        }

        log.info(CLASS_NAME + " - Found " + stores.size() + " identity store(s)");

        // Step 1: Validate credential using stores that support VALIDATE
        CredentialValidationResult validationResult = validateCredential(credential, stores);

        // Step 2: If validation succeeded, aggregate groups from all stores
        if (validationResult.getStatus() == CredentialValidationResult.Status.VALID) {
            log.info(CLASS_NAME + " - Credential validated successfully for caller: " + validationResult.getCallerPrincipal().getName());

            // Aggregate groups from all stores that support PROVIDE_GROUPS
            Set<String> allGroups = aggregateGroups(stores, validationResult, supportGroups);

            log.info(CLASS_NAME + " - Total aggregated groups: " + allGroups);

            // Return new result with aggregated groups
            return new CredentialValidationResult(validationResult.getCallerPrincipal().getName(), allGroups);
        }

        log.info(CLASS_NAME + " - Credential validation failed");
        return CredentialValidationResult.INVALID_RESULT;
    }

    /**
     * Get all identity stores sorted by priority (ascending order).
     *
     * @return List of identity stores sorted by priority
     */
    private List<IdentityStore> getSortedIdentityStores() {
        List<IdentityStore> stores = new ArrayList<>();

        for (IdentityStore store : identityStores) {
            stores.add(store);
            log.info(CLASS_NAME + " - Found identity store: " + store.getClass().getSimpleName() +
                     " with priority: " + store.priority());
        }

        // Sort by priority (ascending - lower number = higher priority)
        stores.sort(Comparator.comparingInt(IdentityStore::priority));

        return stores;
    }

    /**
     * Validate credential using identity stores that support VALIDATE.
     * Returns the first successful validation result.
     *
     * @param credential The credential to validate
     * @param stores     List of identity stores sorted by priority
     * @return CredentialValidationResult from the first successful validation
     */
    private CredentialValidationResult validateCredential(Credential credential, List<IdentityStore> stores) {
        // Filter stores that support VALIDATE
        List<IdentityStore> validationStores = stores.stream().filter(store -> store.validationTypes().contains(ValidationType.VALIDATE)).collect(Collectors.toList());

        log.info(CLASS_NAME + " - " + validationStores.size() + " store(s) support VALIDATE");

        // Try each store in priority order
        for (IdentityStore store : validationStores) {
            log.info(CLASS_NAME + " - Attempting validation with: " + store.getClass().getSimpleName());

            try {
                CredentialValidationResult result = store.validate(credential);

                if (result.getStatus() == CredentialValidationResult.Status.VALID) {
                    log.info(CLASS_NAME + " - Validation successful with store: " + store.getClass().getSimpleName());
                    supportGroups = true;
                    return result;
                } else {
                    log.info(CLASS_NAME + " - Validation failed with store: " + store.getClass().getSimpleName());
                    supportGroups = false;
                    //return result;
                }
            } catch (Exception e) {
                log.warning(CLASS_NAME + " - Exception during validation with store " +
                            store.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        return CredentialValidationResult.INVALID_RESULT;
    }

    /**
     * Aggregate groups from all identity stores that support PROVIDE_GROUPS.
     * This is mainly for stores that might provide additional groups beyond what was returned during validation.
     *
     * @param identityStores List of all identity stores
     * @param result         The validation result of type CredentialValidationResult
     * @param supportGroups  boolean
     * @return Set of aggregated groups from all stores
     */
    protected Set<String> aggregateGroups(List<IdentityStore> identityStores, CredentialValidationResult result, boolean supportGroups) {
        Set<String> combinedGroups = new HashSet<String>();
        if (supportGroups) {
            Set<String> groups = result.getCallerGroups();
            if (groups != null && !groups.isEmpty()) {
                combinedGroups.addAll(groups);
            }
        }
        for (IdentityStore is : identityStores) {
            Set<IdentityStore.ValidationType> validationTypes = is.validationTypes();
            if (validationTypes != null) {
                boolean isProvideGroups = validationTypes.contains(IdentityStore.ValidationType.PROVIDE_GROUPS);
                boolean isValidate = validationTypes.contains(IdentityStore.ValidationType.VALIDATE);
                log.info("IdentityStore : " + is + ", PROVIDE_GROUPS : " + isProvideGroups + ", VALIDATE : " + isValidate);
                if (isProvideGroups && !isValidate) {
                    Set<String> extraGroups = getGroups(is, result);
                    log.info("IdentityStore : " + is + ", groups : " + extraGroups);
                    if (extraGroups != null && !extraGroups.isEmpty()) {
                        combinedGroups.addAll(extraGroups);
                    }
                }
            }
        }
        return combinedGroups;
    }

    private Set<String> getGroups(final IdentityStore is, final CredentialValidationResult result) {
        // getGroups method is protected by java2 security. Therefore add PrivilegedAction for performance reason.
        PrivilegedAction<Set<String>> action = new PrivilegedAction<Set<String>>() {
            @Override
            public Set<String> run() {
                return is.getCallerGroups(result);
            }
        };
        return AccessController.doPrivileged(action);
    }
}