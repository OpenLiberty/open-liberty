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
package com.ibm.ws.jbatch.rest.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialExpiredException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.WSJobRepository;
import com.ibm.jbatch.container.ws.BatchGroupSecurityHelper;
import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.authentication.utility.SubjectHelper;

@Component(property = { "service.vendor=IBM" }, configurationPolicy = ConfigurationPolicy.IGNORE)
public class BatchGroupSecurityHelperImpl implements BatchGroupSecurityHelper {
		
    public Set<String> getGroupsForSubject(Subject subject) {
        SubjectHelper sh = new SubjectHelper();
        WSCredential cred;

        try {

            // this next block is not quite right
            // need to get the group name out from the groupID and this does not quite do it
            cred = sh.getWSCredential(subject);
            List<String> groupIDs = new ArrayList<String>();
            Set<String> groups = new HashSet<String>();
            groupIDs = cred.getGroupIds();
            String aGroupName;

            Iterator it = groupIDs.iterator();
            while (it.hasNext()) {
                aGroupName = (String) it.next();
                //adding a long group name from the repository
                groups.add(aGroupName);
            }

            return groups;
        } catch (CredentialDestroyedException cdex) {
            throw new BatchContainerRuntimeException(cdex);
        } catch (CredentialExpiredException ceex) {
            throw new BatchContainerRuntimeException(ceex);
        } catch (Exception ex) {
            throw new BatchContainerRuntimeException(ex);
        }

    }
}
