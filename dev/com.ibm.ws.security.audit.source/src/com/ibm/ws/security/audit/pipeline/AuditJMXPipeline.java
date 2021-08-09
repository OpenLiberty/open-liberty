/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.audit.pipeline;

import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.boot.jmx.service.MBeanServerForwarderDelegate;
import com.ibm.ws.kernel.boot.jmx.service.MBeanServerPipeline;
import com.ibm.ws.security.audit.Audit;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.audit.AuditService;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM" })
public final class AuditJMXPipeline extends MBeanServerForwarderDelegate {
    private static final TraceComponent tc = Tr.register(AuditJMXPipeline.class);

    static final String KEY_MBEAN_SERVER_PIPELINE = "mbeanServerPipeline";

    public static final String MBEAN_CLASSES = "com.ibm.ws.jmx.delayed.MBeanClasses";

    private static final String KEY_AUDIT_SERVICE = "auditService";
    protected final AtomicServiceReference<AuditService> auditServiceRef = new AtomicServiceReference<AuditService>(KEY_AUDIT_SERVICE);

    private MBeanServerPipeline pipeline;

    private AuditService auditService;

    public AuditJMXPipeline() {}

    @Reference
    protected void setMBeanServerPipeline(MBeanServerPipeline pipeline) {
        this.pipeline = pipeline;
    }

    protected void unsetMBeanServerPipeline(MBeanServerPipeline pipeline) {
        this.pipeline = null;
    }

    private void insertJMXSecurityFilter() {
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
        if (tc.isDebugEnabled())
            Tr.debug(tc, "activing AuditJMXPipeline");
        insertJMXSecurityFilter();
    }

    private void removeJMXSecurityFilter() {
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
    }

    @Override
    public int getPriority() {
        return 1;
    }

    //
    // MBeanServer methods
    //

    @Override
    @FFDCIgnore(InstanceNotFoundException.class)
    public void addNotificationListener(ObjectName name,
                                        NotificationListener listener,
                                        NotificationFilter filter,
                                        Object handback) throws InstanceNotFoundException {
        try {
            super.addNotificationListener(name, listener, filter, handback);
        } catch (InstanceNotFoundException e) {
            emitJMXNotificationEvent(name, listener, filter, handback, "addNotificationListener", "failure", "Instance of MBean not found");
            throw e;
        }
        emitJMXNotificationEvent(name, listener, filter, handback, "addNotificationListener", "success", "Successful add of notification listener");

    }

    @Override
    @FFDCIgnore(InstanceNotFoundException.class)
    public void addNotificationListener(ObjectName name, ObjectName listener,
                                        NotificationFilter filter,
                                        Object handback) throws InstanceNotFoundException {
        try {
            super.addNotificationListener(name, listener, filter, handback);
        } catch (InstanceNotFoundException e) {
            emitJMXNotificationEvent(name, listener, filter, handback, "addNotificationListener", "failure", "Instance of MBean not found");
            throw e;
        }
        emitJMXNotificationEvent(name, listener, filter, handback, "addNotificationListener", "success", "Successful add of notification listener");

    }

    @Override
    @FFDCIgnore({ ReflectionException.class, InstanceAlreadyExistsException.class, MBeanRegistrationException.class, MBeanException.class, NotCompliantMBeanException.class })
    public ObjectInstance createMBean(String className,
                                      ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
        ObjectInstance oi = null;
        try {
            oi = super.createMBean(className, name);
        } catch (ReflectionException e) {
            emitJMXMBeanCreateAction(name, className, null, null, null, "createMBean", "failure", "Class definition not found for MBean");
            throw e;
        } catch (InstanceAlreadyExistsException e) {
            emitJMXMBeanCreateAction(name, className, null, null, null, "createMBean", "failure", "Instance of MBean already exists");
            throw e;
        } catch (MBeanRegistrationException e) {
            emitJMXMBeanCreateAction(name, className, null, null, null, "createMBean", "failure", "MBean registration failure");
            throw e;
        } catch (MBeanException e) {
            emitJMXMBeanCreateAction(name, className, null, null, null, "createMBean", "failure", "MBean constructor exception");
            throw e;
        } catch (NotCompliantMBeanException e) {
            emitJMXMBeanCreateAction(name, className, null, null, null, "createMBean", "failure", "Not compliant MBean");
            throw e;
        }
        emitJMXMBeanCreateAction(name, className, null, null, null, "createMBean", "success", "Successful create of MBean");
        return oi;
    }

    @Override
    @FFDCIgnore({ ReflectionException.class, InstanceAlreadyExistsException.class, MBeanRegistrationException.class, MBeanException.class, NotCompliantMBeanException.class })
    public ObjectInstance createMBean(String className, ObjectName name,
                                      ObjectName loaderName) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        ObjectInstance oi = null;
        try {
            oi = super.createMBean(className, name, loaderName);
            emitJMXMBeanCreateAction(name, className, loaderName, null, null, "createMBean", "success", "Successful create of MBean");
        } catch (ReflectionException e) {
            emitJMXMBeanCreateAction(name, className, loaderName, null, null, "createMBean", "failure", "Class definition not found for MBean");
            throw e;
        } catch (InstanceAlreadyExistsException e) {
            emitJMXMBeanCreateAction(name, className, loaderName, null, null, "createMBean", "failure", "Instance of MBean already exists");
            throw e;
        } catch (MBeanRegistrationException e) {
            emitJMXMBeanCreateAction(name, className, loaderName, null, null, "createMBean", "failure", "MBean registration failure");
            throw e;
        } catch (MBeanException e) {
            emitJMXMBeanCreateAction(name, className, loaderName, null, null, "createMBean", "failure", "MBean constructor exception");
            throw e;
        } catch (NotCompliantMBeanException e) {
            emitJMXMBeanCreateAction(name, className, loaderName, null, null, "createMBean", "failure", "Not compliant MBean");
            throw e;
        }
        return oi;
    }

    @Override
    @FFDCIgnore({ ReflectionException.class, InstanceAlreadyExistsException.class, MBeanRegistrationException.class, MBeanException.class, NotCompliantMBeanException.class })
    public ObjectInstance createMBean(String className, ObjectName name,
                                      Object[] params,
                                      String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
        ObjectInstance oi = null;
        try {
            oi = super.createMBean(className, name, params, signature);
        } catch (ReflectionException e) {
            emitJMXMBeanCreateAction(name, className, null, params, signature, "createMBean", "failure", "Class definition not found for MBean");
            throw e;
        } catch (InstanceAlreadyExistsException e) {
            emitJMXMBeanCreateAction(name, className, null, params, signature, "createMBean", "failure", "Instance of MBean already exists");
            throw e;
        } catch (MBeanRegistrationException e) {
            emitJMXMBeanCreateAction(name, className, null, params, signature, "createMBean", "failure", "MBean registration failure");
            throw e;
        } catch (MBeanException e) {
            emitJMXMBeanCreateAction(name, className, null, params, signature, "createMBean", "failure", "MBean constructor exception");
            throw e;
        } catch (NotCompliantMBeanException e) {
            emitJMXMBeanCreateAction(name, className, null, params, signature, "createMBean", "failure", "Not compliant MBean");
            throw e;
        }
        emitJMXMBeanCreateAction(name, className, null, params, signature, "createMBean", "success", "Successful create of MBean");
        return oi;
    }

    @Override
    @FFDCIgnore({ ReflectionException.class, InstanceAlreadyExistsException.class, MBeanRegistrationException.class, MBeanException.class, NotCompliantMBeanException.class })
    public ObjectInstance createMBean(String className, ObjectName name,
                                      ObjectName loaderName, Object[] params,
                                      String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        ObjectInstance oi = null;
        try {
            oi = super.createMBean(className, name, loaderName, params, signature);
            emitJMXMBeanCreateAction(name, className, loaderName, params, signature, "createMBean", "success", "Successful create of MBean");
        } catch (ReflectionException e) {
            emitJMXMBeanCreateAction(name, className, loaderName, params, signature, "createMBean", "failure", "Class definition not found for MBean");
            throw e;
        } catch (InstanceAlreadyExistsException e) {
            emitJMXMBeanCreateAction(name, className, loaderName, params, signature, "createMBean", "failure", "Instance of MBean already exists");
            throw e;
        } catch (MBeanRegistrationException e) {
            emitJMXMBeanCreateAction(name, className, loaderName, params, signature, "createMBean", "failure", "MBean registration failure");
            throw e;
        } catch (MBeanException e) {
            emitJMXMBeanCreateAction(name, className, loaderName, params, signature, "createMBean", "failure", "MBean constructor exception");
            throw e;
        } catch (NotCompliantMBeanException e) {
            emitJMXMBeanCreateAction(name, className, loaderName, params, signature, "createMBean", "failure", "Not compliant MBean");
            throw e;
        }
        return oi;
    }

    @Override
    @FFDCIgnore({ MBeanException.class, AttributeNotFoundException.class, InstanceNotFoundException.class, ReflectionException.class })
    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {

        Object oi = null;
        try {
            oi = super.getAttribute(name, attribute);
        } catch (MBeanException e) {
            emitJMXMBeanAttributeAction(name, attribute, "getAttribute", "failure", "MBean constructor exception");
            throw e;
        } catch (AttributeNotFoundException e) {
            emitJMXMBeanAttributeAction(name, attribute, "getAttribute", "failure", "Attribute not found");
            throw e;
        } catch (InstanceNotFoundException e) {
            emitJMXMBeanAttributeAction(name, attribute, "getAttribute", "failure", "Instance of MBean not found");
            throw e;
        } catch (ReflectionException e) {
            emitJMXMBeanAttributeAction(name, attribute, "getAttribute", "failure", "Class definition not found for MBean");
            throw e;
        }
        emitJMXMBeanAttributeAction(name, attribute.concat(" = ").concat(oi.toString()), "getAttribute", "success", "Successful retrieval of MBean attribute");
        return oi;
    }

    @Override
    @FFDCIgnore({ InstanceNotFoundException.class, ReflectionException.class })
    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {

        AttributeList al = null;
        try {
            al = super.getAttributes(name, attributes);
        } catch (InstanceNotFoundException e) {
            emitJMXMBeanAttributeAction(name, attributes, "getAttributes", "failure", "Instance of MBean not found");
            throw e;
        } catch (ReflectionException e) {
            emitJMXMBeanAttributeAction(name, attributes, "getAttributes", "failure", "Class definition not found for MBean");
            throw e;
        }
        if (al.isEmpty())
            emitJMXMBeanAttributeAction(name, attributes, "getAttributes", "failure", "Unsuccessful retrieval of attributes");

        else
            emitJMXMBeanAttributeAction(name, al, "getAttributes", "success", "Successful retrieval of MBean attributes");
        return al;
    }

    @Override
    @FFDCIgnore({ MBeanException.class, InstanceNotFoundException.class, ReflectionException.class })
    public Object invoke(ObjectName name, String operationName,
                         Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException {
        Object oi = null;
        try {
            oi = super.invoke(name, operationName, params, signature);
            emitJMXMBeanInvokeEvent(name, operationName, params, signature, "invoke", "success", "Successful MBean invoke operation");
        } catch (ReflectionException e) {
            emitJMXMBeanInvokeEvent(name, operationName, params, signature, "invoke", "failure", "Class definition not found for MBean");
            throw e;
        } catch (InstanceNotFoundException e) {
            emitJMXMBeanInvokeEvent(name, operationName, params, signature, "invoke", "failure", "Instance of MBean not found");
            throw e;
        } catch (MBeanException e) {
            emitJMXMBeanInvokeEvent(name, operationName, params, signature, "invoke", "failure", "MBean constructor exception");
            throw e;
        }
        return oi;
    }

    @Override
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        Set<ObjectInstance> oi = null;
        try {
            oi = super.queryMBeans(name, query);
        } catch (Exception e) {
            emitJMXMBeanQueryEvent(name, query, "queryMBeans", "failure", e.getMessage());
        }
        if (oi != null && oi.isEmpty())
            emitJMXMBeanQueryEvent(name, query, "queryMBeans", "failure", "Instance of MBean not found");
        else
            emitJMXMBeanQueryEvent(name, query, "queryMBeans", "success", "Successful query of MBeans");
        return oi;
    }

    @Override
    @FFDCIgnore({ InstanceAlreadyExistsException.class, MBeanRegistrationException.class, NotCompliantMBeanException.class })
    public ObjectInstance registerMBean(Object object, ObjectName name) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        ObjectInstance oi = null;
        try {
            oi = super.registerMBean(object, name);
        } catch (InstanceAlreadyExistsException e) {
            emitJMXMBeanRegisterEvent(name, object, "registerMBean", "failure", "Instance of MBean already exists");
            throw e;
        } catch (MBeanRegistrationException e) {
            emitJMXMBeanRegisterEvent(name, object, "registerMBean", "failure", "MBean registration failure");
            throw e;
        } catch (NotCompliantMBeanException e) {
            emitJMXMBeanRegisterEvent(name, object, "registerMBean", "failure", "Not compliant MBean");
            throw e;
        }
        emitJMXMBeanRegisterEvent(name, object, "registerMBean", "success", "Successful MBean registration");

        return oi;
    }

    @Override
    @FFDCIgnore({ InstanceNotFoundException.class, ListenerNotFoundException.class })
    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
        try {
            super.removeNotificationListener(name, listener);
        } catch (InstanceNotFoundException e) {
            emitJMXNotificationEvent(name, listener, null, null, "removeNotificationListener", "failure", "Instance of MBean not found");
            throw e;
        } catch (ListenerNotFoundException e) {
            emitJMXNotificationEvent(name, listener, null, null, "removeNotificationListener", "failure", "Instance of notification listener not found");
            throw e;
        }
        emitJMXNotificationEvent(name, listener, null, null, "removeNotificationListener", "success", "Successful remove of notification listener");

    }

    @Override
    @FFDCIgnore({ InstanceNotFoundException.class, ListenerNotFoundException.class })
    public void removeNotificationListener(ObjectName name,
                                           NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException {
        try {
            super.removeNotificationListener(name, listener);
        } catch (InstanceNotFoundException e) {
            emitJMXNotificationEvent(name, listener, null, null, "removeNotificationListener", "failure", "Instance of MBean not found");
            throw e;
        } catch (ListenerNotFoundException e) {
            emitJMXNotificationEvent(name, listener, null, null, "removeNotificationListener", "failure", "Instance of notification listener not found");
            throw e;
        }
        emitJMXNotificationEvent(name, listener, null, null, "removeNotificationListener", "success", "Successful remove of notification listener");

    }

    @Override
    @FFDCIgnore({ InstanceNotFoundException.class, ListenerNotFoundException.class })
    public void removeNotificationListener(ObjectName name,
                                           ObjectName listener,
                                           NotificationFilter filter,
                                           Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        try {
            super.removeNotificationListener(name, listener, filter, handback);
        } catch (InstanceNotFoundException e) {
            emitJMXNotificationEvent(name, listener, null, null, "removeNotificationListener", "failure", "Instance of MBean not found");
            throw e;
        } catch (ListenerNotFoundException e) {
            emitJMXNotificationEvent(name, listener, null, null, "removeNotificationListener", "failure", "Instance of notification listener not found");
            throw e;
        }
        emitJMXNotificationEvent(name, listener, filter, handback, "removeNotificationListener", "success", "Successful remove of notification listener");

    }

    @Override
    @FFDCIgnore({ InstanceNotFoundException.class, ListenerNotFoundException.class })
    public void removeNotificationListener(ObjectName name,
                                           NotificationListener listener,
                                           NotificationFilter filter,
                                           Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        try {
            super.removeNotificationListener(name, listener, filter, handback);
        } catch (InstanceNotFoundException e) {
            emitJMXNotificationEvent(name, listener, null, null, "removeNotificationListener", "failure", "Instance of MBean not found");
            throw e;
        } catch (ListenerNotFoundException e) {
            emitJMXNotificationEvent(name, listener, null, null, "removeNotificationListener", "failure", "Instance of notification listener not found");
            throw e;
        }
        emitJMXNotificationEvent(name, listener, filter, handback, "removeNotificationListener", "success", "Successful remove of notification listener");

    }

    @Override
    @FFDCIgnore({ AttributeNotFoundException.class, MBeanException.class, InvalidAttributeValueException.class })
    public void setAttribute(ObjectName name,
                             Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        try {
            super.setAttribute(name, attribute);
        } catch (InstanceNotFoundException e) {
            emitJMXMBeanAttributeAction(name, attribute, "setAttribute", "failure", "Instance of MBean not found");
            throw e;
        } catch (AttributeNotFoundException e) {
            emitJMXMBeanAttributeAction(name, attribute, "setAttribute", "failure", "Attribute not found");
            throw e;
        } catch (InvalidAttributeValueException e) {
            emitJMXMBeanAttributeAction(name, attribute, "setAttribute", "failure", "Invalid attribute value specified");
            throw e;
        } catch (MBeanException e) {
            emitJMXMBeanAttributeAction(name, attribute, "setAttribute", "failure", "MBean constructor exception");
            throw e;
        } catch (ReflectionException e) {
            emitJMXMBeanAttributeAction(name, attribute, "setAttribute", "failure", "Class definition not found for MBean");
            throw e;
        }

        StringBuffer buf = new StringBuffer();
        emitJMXMBeanAttributeAction(name, (buf.append(attribute.getName()).append(" = ").append(attribute.getValue())).toString(), "setAttribute", "success",
                                    "Successful set of MBean attribute");
    }

    @Override
    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
        AttributeList al = null;
        try {
            al = super.setAttributes(name, attributes);
        } catch (InstanceNotFoundException e) {
            emitJMXMBeanAttributeAction(name, attributes, "setAttributes", "failure", "Instance of MBean not found");
            throw e;
        } catch (ReflectionException e) {
            emitJMXMBeanAttributeAction(name, attributes, "setAttributes", "failure", "Class definition not found for MBean");
            throw e;
        }
        if (al.isEmpty())
            emitJMXMBeanAttributeAction(name, attributes, "setAttributes", "failure", "Could not set MBean attributes");
        else
            emitJMXMBeanAttributeAction(name, attributes, "setAttributes", "success", "Successful set of MBean attributes");
        return al;
    }

    @Override
    @FFDCIgnore({ InstanceNotFoundException.class, MBeanRegistrationException.class })
    public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {

        try {
            //if (!unregisterMBeanIfDelayed(name)) {
            super.unregisterMBean(name);
            //}
        } catch (InstanceNotFoundException e) {
            emitJMXMBeanRegisterEvent(name, null, "unregisterMBean", "failure", "Instance of MBean not found");
            throw e;
        } catch (MBeanRegistrationException e) {
            emitJMXMBeanRegisterEvent(name, null, "unregisterMBean", "failure", "MBean registration failure");
            throw e;
        }
        emitJMXMBeanRegisterEvent(name, null, "unregisterMBean", "success", "Successful MBean unregistration");

    }

    public void emitJMXNotificationEvent(ObjectName name, Object listener, NotificationFilter filter, Object handback, String action, String outcome, String outcomeReason) {
        Audit.audit(Audit.EventID.JMX_NOTIFICATION_01, name, listener, filter, handback, action, outcome, outcomeReason);
    }

    public void emitJMXMBeanAttributeAction(ObjectName name, Object attrs, String action, String outcome, String outcomeReason) {
        if (auditService != null) {
            if (action.equals("getAttribute") && ((String) attrs).contains("Cpu")) {
            } else {
                Audit.audit(Audit.EventID.JMX_MBEAN_ATTRIBUTES_01, name, attrs, action, outcome, outcomeReason);
            }
        }
    }

    public void emitJMXMBeanCreateAction(ObjectName name, String className, ObjectName loaderName, Object[] params, String[] signature, String action,
                                         String outcome, String outcomeReason) {
        if (auditService != null)
            Audit.audit(Audit.EventID.JMX_MBEAN_01, name, className, loaderName, null, params, signature, null, action, outcome, outcomeReason);
    }

    public void emitJMXMBeanInvokeEvent(ObjectName name, String operationName, Object[] params, String[] signature, String action, String outcome, String outcomeReason) {
        if (auditService != null)
            Audit.audit(Audit.EventID.JMX_MBEAN_01, name, null, null, operationName, params, signature, null, action, outcome, outcomeReason);
    }

    public void emitJMXMBeanQueryEvent(ObjectName name, QueryExp query, String action, String outcome, String outcomeReason) {
        if (auditService != null)
            Audit.audit(Audit.EventID.JMX_MBEAN_01, name, null, null, null, null, null, query, action, outcome, outcomeReason);
    }

    public void emitJMXMBeanRegisterEvent(ObjectName name, Object object, String action, String outcome, String outcomeReason) {
        if (auditService != null)
            Audit.audit(Audit.EventID.JMX_MBEAN_REGISTER_01, name, object, action, outcome, outcomeReason);
    }

    @Reference(name = KEY_AUDIT_SERVICE, service = AuditService.class)
    protected void setAuditService(AuditService auditService) {
        this.auditService = auditService;
    }

    protected void unsetAuditService(AuditService auditService) {
        this.auditService = null;
    }

    protected AuditService getAuditService() {
        return this.auditService;
    }
}