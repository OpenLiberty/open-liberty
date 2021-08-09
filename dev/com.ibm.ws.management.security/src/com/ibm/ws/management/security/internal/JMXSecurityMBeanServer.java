/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.management.security.internal;

import java.util.ArrayList;
import java.util.List;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.boot.jmx.service.MBeanServerForwarderDelegate;
import com.ibm.ws.kernel.boot.jmx.service.MBeanServerPipeline;
import com.ibm.ws.management.security.ManagementSecurityConstants;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authorization.AuthorizationService;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Handle authorization MBean Server when security is enabled.
 */
public class JMXSecurityMBeanServer extends MBeanServerForwarderDelegate {
    private static final TraceComponent tc = Tr.register(JMXSecurityMBeanServer.class);

    static final String KEY_MBEAN_SERVER_PIPLINE = "mBeanServerPipeline";
    static final String KEY_SECURITY_SERVICE = "securityService";
    private final AtomicServiceReference<MBeanServerPipeline> pipelineRef = new AtomicServiceReference<MBeanServerPipeline>(KEY_MBEAN_SERVER_PIPLINE);
    private final AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);

    final List<String> requiredRoles = new ArrayList<String>();

    public JMXSecurityMBeanServer() {
        // At this time, we only have one required role: Administrator
        requiredRoles.add(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME);
    }

    protected synchronized void setMBeanServerPipeline(ServiceReference<MBeanServerPipeline> ref) {
        pipelineRef.setReference(ref);
    }

    protected synchronized void unsetMBeanServerPipeline(ServiceReference<MBeanServerPipeline> ref) {
        pipelineRef.unsetReference(ref);
    }

    protected synchronized void setSecurityService(ServiceReference<SecurityService> ref) {
        securityServiceRef.setReference(ref);
    }

    protected synchronized void unsetSecurityService(ServiceReference<SecurityService> ref) {
        securityServiceRef.unsetReference(ref);
    }

    private void insertJMXSecurityFilter() {
        MBeanServerPipeline pipeline = pipelineRef.getService();
        if (!pipeline.contains(this)) {
            if (!pipeline.insert(this)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Insertion of " + this.getClass().getCanonicalName() + " into MBeanServerPipeline failed");
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, this.getClass().getCanonicalName() + " already exists in MBeanServerPipeline");
            }
        }
    }

    /**
     * Insert the JMX security filter upon activation. This will only
     * happen if we have both the MBeanServerPipeline and the SecurityService.
     * 
     * @param cc
     */
    protected synchronized void activate(ComponentContext cc) {
        pipelineRef.activate(cc);
        securityServiceRef.activate(cc);

        insertJMXSecurityFilter();
    }

    private void removeJMXSecurityFilter() {
        MBeanServerPipeline pipeline = pipelineRef.getService();
        if (pipeline.contains(this)) {
            if (!pipeline.remove(this)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Removal of " + this.getClass().getCanonicalName() + " into MBeanServerPipeline failed");
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, this.getClass().getCanonicalName() + " already removed from MBeanServerPipeline");
            }
        }
    }

    /**
     * Remove the JMX security filter upon deactivation.
     * 
     * @param cc
     */
    protected synchronized void deactivate(ComponentContext cc) {
        removeJMXSecurityFilter();

        pipelineRef.deactivate(cc);
        securityServiceRef.deactivate(cc);
    }

    /* MBeanServerForwarderDelegate related methods */

    /**
     * {@inheritDoc} <p>
     * Authorization Security MBean Server always returns the highest
     * priority.
     */
    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    // protecting the operations

    /**
     * Check that the caller (on the thread) is authorized.
     * <p>
     * This logic is handled by the core security services.
     * 
     * @return {@code true} if the caller is authorized, {@code false} otherwise.
     */
    private boolean isAuthorized() {
        SecurityService securityService = securityServiceRef.getService();
        AuthorizationService authzService = securityService.getAuthorizationService();
        boolean isAuthorized = authzService.isAuthorized(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                                         requiredRoles, null);
        return isAuthorized;
    }

    /**
     * Throwing a SecurityException as not all of the methods that need
     * protection throw an MBeanException. We can change this if we need to.
     * 
     * @throws SecurityException
     */
    private void throwAuthzException() throws SecurityException {
        SubjectManager subjectManager = new SubjectManager();
        String name = "UNAUTHENTICATED";
        if (subjectManager.getInvocationSubject() != null) {
            name = subjectManager.getInvocationSubject().getPrincipals().iterator().next().getName();
        }
        Tr.audit(tc, "MANAGEMENT_SECURITY_AUTHZ_FAILED", name, "MBeanAccess", requiredRoles);
        String message = Tr.formatMessage(tc, "MANAGEMENT_SECURITY_AUTHZ_FAILED", name, "MBeanAccess", requiredRoles);
        throw new SecurityException(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getAttribute(ObjectName name, String attribute)
                    throws MBeanException, AttributeNotFoundException,
                    InstanceNotFoundException, ReflectionException {
        if (!isAuthorized()) {
            throwAuthzException();
        }
        return super.getAttribute(name, attribute);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AttributeList getAttributes(ObjectName name, String[] attributes)
                    throws InstanceNotFoundException, ReflectionException {
        if (!isAuthorized()) {
            throwAuthzException();
        }
        return super.getAttributes(name, attributes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttribute(ObjectName name, Attribute attribute)
                    throws InstanceNotFoundException, AttributeNotFoundException,
                    InvalidAttributeValueException, MBeanException,
                    ReflectionException {
        if (!isAuthorized()) {
            throwAuthzException();
        }
        super.setAttribute(name, attribute);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AttributeList setAttributes(ObjectName name,
                                       AttributeList attributes)
                    throws InstanceNotFoundException, ReflectionException {
        if (!isAuthorized()) {
            throwAuthzException();
        }
        return super.setAttributes(name, attributes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(ObjectName name, String operationName,
                         Object params[], String signature[])
                    throws InstanceNotFoundException, MBeanException,
                    ReflectionException {
        if (!isAuthorized()) {
            throwAuthzException();
        }
        return super.invoke(name, operationName, params, signature);
    }

}
