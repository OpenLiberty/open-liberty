/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web.war.identitystorehandler;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStoreHandler;

@Default
@ApplicationScoped
public class CustomIdentityStoreHandler implements IdentityStoreHandler {

    protected static String sourceClass = CustomIdentityStoreHandler.class.getName();
    private final Logger logger = Logger.getLogger(sourceClass);

    public CustomIdentityStoreHandler() {
        logger.info("CustomIdentityStoreHandler is being used.");
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.identitystore.IdentityStoreHandler#validate(javax.security.enterprise.credential.Credential)
     */
    @Override
    public CredentialValidationResult validate(Credential credential) {
        logger.entering(sourceClass, "validate", credential);
        CredentialValidationResult result = validate(getIdentityStores(), credential);
        logger.exiting(sourceClass, "validate", result);
        return result;
    }

    public CredentialValidationResult validate(Set<IdentityStore> identityStores, Credential credential) {
        CredentialValidationResult firstInvalid = null;
        CredentialValidationResult result = CredentialValidationResult.NOT_VALIDATED_RESULT;
        boolean supportGroups = false;
        boolean isValidated = false;
        if (!identityStores.isEmpty()) {
            for (IdentityStore is : identityStores) {
                if (is.validationTypes().contains(IdentityStore.ValidationType.VALIDATE)) {
                    isValidated = true;
                    result = is.validate(credential);
                    logger.info("validation status : " + result.getStatus() + ", identityStore : " + is);
                    if (result.getStatus() == CredentialValidationResult.Status.VALID) {
                        if (is.validationTypes().contains(IdentityStore.ValidationType.PROVIDE_GROUPS)) {
                            supportGroups = true;
                        }
                        break;
                    } else if (result.getStatus() == CredentialValidationResult.Status.INVALID) {
                        if (firstInvalid == null) {
                            firstInvalid = result;
                        }
                    }
                }
            }
            // at this point, result contains the result of the last IdentityStore.
            if (result != null && result.getStatus() == CredentialValidationResult.Status.VALID) {
                Set<String> groups = getGroups(identityStores, result, supportGroups);
                logger.info("IdentityStore ID : " + result.getIdentityStoreId() + ", CallerPrincipal : "
                             + (result.getCallerPrincipal() != null ? result.getCallerPrincipal().getName() : "null") + ", CallerDN : " + result.getCallerDn()
                             + ", CallerUniqueId : " + result.getCallerUniqueId() + ", Groups : " + groups);
                result = new CredentialValidationResult(result.getIdentityStoreId(), result.getCallerPrincipal(), result.getCallerDn(), result.getCallerUniqueId(), groups);
            } else if (firstInvalid != null) {
                result = firstInvalid;
            } else if (!isValidated) {
                result = CredentialValidationResult.NOT_VALIDATED_RESULT;
            }
        }
        return result;
    }

    protected Set<String> getGroups(Set<IdentityStore> identityStores, CredentialValidationResult result, boolean supportGroups) {
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
                logger.info("IdentityStore : " + is + ", PROVIDE_GROUPS : " + isProvideGroups + ", VALIDATE : " + isValidate);
                if (isProvideGroups && !isValidate) {
                    Set<String> extraGroups = getGroups(is, result);
                    logger.info("IdentityStore : " + is + ", groups : " + extraGroups);
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

    protected CDI<Object> getCDI() {
        return CDI.current();
    }

    protected Set<IdentityStore> getIdentityStores() {
        Set<IdentityStore> stores = new TreeSet<IdentityStore>(priorityComparator);
        scanIdentityStores(stores);
        return stores;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void scanIdentityStores(Set<IdentityStore> identityStores) {
        CDI cdi = getCDI();
        Instance<IdentityStore> identityStoreInstances = cdi.select(IdentityStore.class);
        if (identityStoreInstances != null) {
            for (IdentityStore identityStore : identityStoreInstances) {
                logger.info("IdentityStore from the CDI: " + identityStore + ", validationTypes : " + identityStore.validationTypes() + ", priority : "
                                 + identityStore.priority());
                identityStores.add(identityStore);
            }
        }

        logger.info("Number of identityStore : " + identityStores.size());
        return;
    }

    private final static Comparator<IdentityStore> priorityComparator = new Comparator<IdentityStore>() {
        // Sort priority in ascending order
        // Comparator returns 0 if both objects are identical.
        @Override
        public int compare(IdentityStore o1, IdentityStore o2) {
            int result = 1;
            if (o1.equals(o2)) {
                result = 0;
            } else if (o1.priority() < o2.priority()) {
                result = -1;
            }
            return result;
        }
    };

}
