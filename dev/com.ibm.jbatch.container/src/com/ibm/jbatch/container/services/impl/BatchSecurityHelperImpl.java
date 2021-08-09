/*
 * Copyright 2013 International Business Machines Corp.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.jbatch.container.services.impl;

import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Set;

import javax.security.auth.Subject;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.spi.BatchSecurityHelper;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.security.authentication.principals.WSPrincipal;

/**
 * Liberty uses the "submitter" field associated with a JobInstance to
 * record the user that initially created the JobInstance. Subsequent
 * operations on the JobInstance must be done by either the same user
 * or by a user with Admin authority.
 *
 *
 * Note: this class creates dependencies on security bundles, namely
 * com.ibm.websphere.security (SubjectManagerService) and com.ibm.ws.security.credentials
 * (WSPrincipal). These bundles are currently included in batch-1.0.
 *
 *
 */
@Component(property = { "service.vendor=IBM" }, configurationPolicy = ConfigurationPolicy.IGNORE)
public class BatchSecurityHelperImpl implements BatchSecurityHelper {

    /**
     * @return the userId of the Subject currently on the thread.
     */
    @Override
    public String getRunAsUser() {
        return getUserId(getRunAsSubject());
    }

    /**
     * @return the Subject currently active on the thread.
     */
    @Override
    public Subject getRunAsSubject() {
        try {
            return AccessController.doPrivileged(
                                                 new PrivilegedExceptionAction<Subject>() {
                                                     @Override
                                                     public Subject run() throws WSSecurityException {
                                                         return WSSubject.getRunAsSubject();
                                                     }
                                                 });
        } catch (PrivilegedActionException e) {
            throw new BatchContainerRuntimeException(e.getCause());
        }
    }

    /**
     * @return the userId from the WSPrincipal in the given Subject.
     */
    protected String getUserId(Subject subject) {
        Principal wsprincipal = getWSPrincipal(subject);
        return (wsprincipal != null) ? wsprincipal.getName() : null;
    }

    /**
     * @return the WSPrincipal in the given Subject (there should only be one).
     */
    protected WSPrincipal getWSPrincipal(Subject subject) {
        if (subject == null) {
            return null;
        }

        Set<WSPrincipal> principals = subject.getPrincipals(WSPrincipal.class);
        return (principals.isEmpty()) ? null : principals.iterator().next();
    }

}
