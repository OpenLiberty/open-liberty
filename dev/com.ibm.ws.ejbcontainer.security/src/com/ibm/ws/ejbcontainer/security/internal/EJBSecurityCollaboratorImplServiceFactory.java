/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.security.internal;

import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.authorization.jacc.JaccService;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.ready.SecurityReadyService;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;

import java.util.Map;

/**
 * ServiceFactory which constructs EJBSecurityCollaboratorImpl directly
 * instead of constructing it via OSGi reflection. Reflection will
 * cause a NoClassDefFound error if optional deprecated classes like
 * java.security.Identity are not present in the JRE
 */
public class EJBSecurityCollaboratorImplServiceFactory implements ServiceFactory {

    private final EJBSecurityCollaboratorImpl service;

    public EJBSecurityCollaboratorImplServiceFactory(SubjectManager subjectManager) {
        service = new EJBSecurityCollaboratorImpl(subjectManager);
    }

    @Override
    public Object getService(Bundle bundle, ServiceRegistration registration) {
        return service;
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {

    }

    protected void activate(ComponentContext cc, Map<String, Object> props) {
        service.activate(cc, props);
    }

    protected void modified(Map<String, Object> newProperties) {
        service.modified(newProperties);
    }

    protected void deactivate(ComponentContext cc) {
        service.deactivate(cc);
    }

    protected void setCredentialService(ServiceReference<CredentialsService> ref) {
        service.setCredentialService(ref);
    }

    protected void unsetCredentialService(ServiceReference<CredentialsService> ref) {
        service.unsetCredentialService(ref);
    }

    protected void setSecurityService(ServiceReference<SecurityService> ref) {
        service.setSecurityService(ref);
    }

    protected void unsetSecurityService(ServiceReference<SecurityService> ref) {
        service.unsetSecurityService(ref);
    }

    @Reference
    protected void setSecurityReadyService(SecurityReadyService ref) {
        service.setSecurityReadyService(ref);
    }

    protected void unsetSecurityReadyService(SecurityReadyService ref) {
        service.unsetSecurityReadyService(ref);
    }

    protected void setUnauthenticatedSubjectService(ServiceReference<UnauthenticatedSubjectService> ref) {
        service.setUnauthenticatedSubjectService(ref);
    }

    protected void unsetUnauthenticatedSubjectService(ServiceReference<UnauthenticatedSubjectService> ref) {
        service.unsetUnauthenticatedSubjectService(ref);
    }

    protected void setJaccService(ServiceReference<JaccService> reference) {
        service.setJaccService(reference);
    }

    protected void unsetJaccService(ServiceReference<JaccService> reference) {
        service.unsetJaccService(reference);
    }
}
