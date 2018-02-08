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
package com.ibm.ws.security.javaeesec.cdi.extensions;

import java.util.Hashtable;
import java.util.ArrayList;

import javax.enterprise.inject.spi.CDI;
import javax.security.auth.Subject;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.IdentityStoreHandler;

import com.ibm.websphere.ras.annotation.Sensitive;

import com.ibm.ws.security.authentication.JavaEESecurityService;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.javaeesec.cdi.beans.Utils;
import com.ibm.ws.security.javaeesec.properties.ModulePropertiesUtils;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;


/**
 *
 */
@Component(service = { JavaEESecurityService.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class JavaEESecurityServiceImpl implements JavaEESecurityService {
    Utils utils = new Utils();

    @Activate
    protected void activate(ComponentContext cc) {}

    @Deactivate
    protected void deactivate(ComponentContext cc) {}

    /**
     * Returns the partial subject for hashtable login
     *
     * @param username
     * @param password
     *
     * @return the partial subject which can be used for hashtable login if username and password are valid.
     * @throws com.ibm.ws.security.authentication.AuthenticationException
     */
    @Override
    public Subject createLoginHashtable(String username, @Sensitive String password) throws AuthenticationException {
        if (getModulePropertiesUtils().isHttpAuthenticationMechanism()) {
            IdentityStoreHandler identityStoreHandler = utils.getIdentityStoreHandler(getCDI());
            if (identityStoreHandler != null) {
                Subject inSubject = new Subject();
                Hashtable<String, Object> subjectHashtable = utils.createNewSubjectHashtable(inSubject);
                UsernamePasswordCredential credential = new UsernamePasswordCredential(username, password);
                AuthenticationStatus status = utils.validateWithIdentityStore("defaultRealm", inSubject, credential, identityStoreHandler);
                if (status != AuthenticationStatus.SUCCESS) {
                    throw new AuthenticationException("Authentication by IdentityStoreHandler was failed.");
                }
                return inSubject;
            } else {
                throw new AuthenticationException("IdentityStoreHandler does not exist.");
            }
        } else {
            throw new AuthenticationException("HttpAuthenticationMechansim is not used in this module.");
        }
    }

    /**
     * Returns whether an IdentiyStoreHander is available for validation.
     *
     * @return whether an identityStoreHander is available.
     */
    @Override
    public boolean isIdentityStoreHanderAvailable() {
        return getModulePropertiesUtils().isHttpAuthenticationMechanism() && (utils.getIdentityStoreHandler(getCDI()) != null);
    }

    @SuppressWarnings("rawtypes")
    protected CDI getCDI() {
        return CDI.current();
    }

    protected ModulePropertiesUtils getModulePropertiesUtils() {
        return ModulePropertiesUtils.getInstance();
    }

}
