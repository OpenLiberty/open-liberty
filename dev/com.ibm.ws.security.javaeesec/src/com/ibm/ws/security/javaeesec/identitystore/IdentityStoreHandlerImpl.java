/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.javaeesec.identitystore;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStoreHandler;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.javaeesec.CDIHelper;

@Default
@ApplicationScoped
public class IdentityStoreHandlerImpl implements IdentityStoreHandler {

    private static final TraceComponent tc = Tr.register(IdentityStoreHandlerImpl.class);
    /**
     * list of identityStore sorted by priority. the first is the highest and the last is the lowest.
     */
    private final TreeSet<IdentityStore> identityStores = new TreeSet<IdentityStore>(priorityComparator);

    public IdentityStoreHandlerImpl() {}

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.identitystore.IdentityStoreHandler#validate(javax.security.enterprise.credential.Credential)
     */
    @Override
    public CredentialValidationResult validate(Credential credential) {
        return validate(identityStores, credential);
    }

    public CredentialValidationResult validate(Set<IdentityStore> identityStores, Credential credential) {
        CredentialValidationResult firstInvalid = null;
        CredentialValidationResult result = CredentialValidationResult.NOT_VALIDATED_RESULT;
        boolean supportGroups = false;
        boolean isValidated = false;
        if (identityStores.isEmpty()) {
            scanIdentityStores(identityStores);
        }
        if (!identityStores.isEmpty()) {
            for (IdentityStore is : identityStores) {
                if (is.validationTypes().contains(IdentityStore.ValidationType.VALIDATE)) {
                    isValidated = true;
                    result = is.validate(credential);
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "validation status : " + result.getStatus() + ", identityStore : " + is);
                    }
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
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "IdentityStore ID : " + result.getIdentityStoreId() + ", CallerPrincipal : " + (result.getCallerPrincipal() != null ? result.getCallerPrincipal().getName() : "null") +  ", CallerDN : " + result.getCallerDn() + ", CallerUniqueId : " + result.getCallerUniqueId() + ", Groups : " +  groups);
                }
                result = new CredentialValidationResult(result.getIdentityStoreId(), result.getCallerPrincipal(), result.getCallerDn(), result.getCallerUniqueId(), groups);
            } else if (firstInvalid != null) {
                result = firstInvalid;
            } else if (!isValidated) {
                Tr.error(tc, "JAVAEESEC_ERROR_NO_VALIDATION");
                result = CredentialValidationResult.NOT_VALIDATED_RESULT;
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No IdentityStore bean is registered. Returning NOT_VALIDATED.");
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "validation status : " + result.getStatus());
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
            if (validationTypes.size() == 1 && validationTypes.contains(IdentityStore.ValidationType.PROVIDE_GROUPS)) {
                Set<String> extraGroups = getGroups(is, result);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "IdentityStore : " + is + ", groups : " + extraGroups);
                }
                if (extraGroups != null && !extraGroups.isEmpty()) {
                    combinedGroups.addAll(extraGroups);
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

    protected CDI getCDI() {
        return CDI.current();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void scanIdentityStores(Set<IdentityStore> identityStores) {
        CDI cdi = getCDI();
        Instance<IdentityStore> identityStoreInstances = cdi.select(IdentityStore.class);
        if (identityStoreInstances != null) {
            for (IdentityStore identityStore : identityStoreInstances) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "IdentityStore from extension: " + identityStore + ", validationTypes : " + identityStore.validationTypes() + ", priority : " + identityStore.priority());
                }
                identityStores.add(identityStore);
            }
        }

        // If the mechanism is from the extension, then the identity stores from the application need to be found using the app's bean manager.
        if (cdi.getBeanManager().equals(CDIHelper.getBeanManager()) == false) {
            for (IdentityStore identityStore : CDIHelper.getBeansFromCurrentModule(IdentityStore.class)) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "IdentityStore from module: " + identityStore + ", validationTypes : " + identityStore.validationTypes() + ", priority : " + identityStore.priority());
                }
                identityStores.add(identityStore);
            }
        }

        if (identityStores.isEmpty()) {
            Tr.error(tc, "JAVAEESEC_ERROR_NO_IDENTITYSTORES");
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Number of identityStore : " + identityStores.size());
        }
        return;
    }

    private final static Comparator<IdentityStore> priorityComparator = new Comparator<IdentityStore>() {
        // Sort priority in ascending order
        // Comparator returns 0 if both objects are identical.
        @Override
        public int compare(IdentityStore o1, IdentityStore o2) {
            int result =1;
            if (o1.equals(o2)) {
                result = 0;
            } else if (o1.priority() < o2.priority()) {
                result = -1;
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "compare", new Object[] { o1, o2, result, this });
            }
            return result;
        }
    };

    protected TreeSet<IdentityStore> getIdentityStores() {
        return identityStores;
    }
}
