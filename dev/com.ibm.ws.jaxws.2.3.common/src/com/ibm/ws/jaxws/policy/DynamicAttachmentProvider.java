/*******************************************************************************
 * Copyright (c) 2019, 2026 IBM Corporation and others.
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
package com.ibm.ws.jaxws.policy;

import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;
import com.ibm.ws.jaxws.metadata.JaxWsModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.cxf.ws.policy.PolicyProvider;
import org.apache.cxf.ws.policy.attachment.AbstractPolicyProvider;
import org.apache.cxf.ws.policy.attachment.external.DomainExpression;
import org.apache.cxf.ws.policy.attachment.external.DomainExpressionBuilderRegistry;
import org.apache.cxf.ws.policy.attachment.external.PolicyAttachment;
import org.apache.cxf.ws.policy.attachment.reference.LocalDocumentReferenceResolver;
import org.apache.cxf.ws.policy.attachment.reference.ReferenceResolver;
import org.apache.neethi.Constants;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyReference;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;


@NoJSR250Annotations
public class DynamicAttachmentProvider extends AbstractPolicyProvider implements PolicyProvider {
    private static final TraceComponent tc = Tr.register(DynamicAttachmentProvider.class);

    private String location = null;
    private Collection<PolicyAttachment> attachments;

    public DynamicAttachmentProvider() {
    }

    public DynamicAttachmentProvider(Bus b) {
        super(b);
    }

    public void setLocation(String u) {
        location = u;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public Policy getEffectivePolicy(BindingFaultInfo bfi, Message m) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getEffectivePolicy for BindingFaultInfo", bfi != null ? bfi : null);
        }
        readDocument();
        Policy p = new Policy();
        for (PolicyAttachment pa : attachments) {
            boolean merge = pa.appliesTo(bfi);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "If merges BindingFaultInfo Policy by using this PolicyAttachment", pa.getPolicy().getAttributes(), merge);
            }
            if (merge) {
                p = p.merge(pa.getPolicy());
            }
        }

        return p;
    }

    @Override
    public Policy getEffectivePolicy(BindingMessageInfo bmi, Message m) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getEffectivePolicy for BindingMessageInfo", bmi != null ? bmi.getMessageInfo() : null);
        }
        readDocument();
        Policy p = new Policy();
        for (PolicyAttachment pa : attachments) {
            boolean merge = pa.appliesTo(bmi);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "If merges BindingMessageInfo Policy by using this PolicyAttachment", pa.getPolicy().getAttributes(), merge);
            }
            if (merge) {
                p = p.merge(pa.getPolicy());
            }
        }

        return p;
    }


    @Override
    public Policy getEffectivePolicy(BindingOperationInfo boi, Message m) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getEffectivePolicy for BindingOperationInfo", boi != null ? boi : null);
        }
        readDocument();
        Policy p = new Policy();
        for (PolicyAttachment pa : attachments) {
            boolean merge = pa.appliesTo(boi);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "If merges BindingOperationInfo Policy by using this PolicyAttachment", pa.getPolicy().getAttributes(), merge);
            }
            if (merge) {
                p = p.merge(pa.getPolicy());
            }
        }

        return p;
    }

    @Override
    public Policy getEffectivePolicy(EndpointInfo ei, Message m) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getEffectivePolicy for EndpointInfo", ei != null ? ei : null);
        }
        readDocument();
        Policy p = new Policy();
        for (PolicyAttachment pa : attachments) {
            boolean merge = pa.appliesTo(ei);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "If merges EndpointInfo Policy by using this PolicyAttachment", pa.getPolicy().getAttributes(), merge);
            }
            if (merge) {
                p = p.merge(pa.getPolicy());
            }
        }

        return p;
    }

    @Override
    public Policy getEffectivePolicy(ServiceInfo si, Message m) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getEffectivePolicy for ServiceInfo", si != null ? si.getName() : null);
        }
        readDocument();
        Policy p = new Policy();
        for (PolicyAttachment pa : attachments) {
            boolean merge = pa.appliesTo(si);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "If merges ServiceInfo Policy by using this PolicyAttachment", pa.getPolicy().getAttributes(), merge);
            }
            if (merge) {
                p = p.merge(pa.getPolicy());
            }
        }

        return p;
    }

    void readDocument() {
        if (null != attachments) {
            return;
        }

        // read the document and build the attachments
        attachments = new ArrayList<PolicyAttachment>();
        Document doc = null;
        try {
            InputStream is = null;
            
            // Retrieve the module metadata and Container associated with this Bus.
            // Using the module's Container isolates the resource lookup to the current module archive,
            // preventing classpath shadowing across multiple JARs in an EAR application.
            JaxWsModuleMetaData moduleMetaData = (bus != null) ? bus.getExtension(JaxWsModuleMetaData.class) : null;
            Container moduleContainer = (moduleMetaData != null) ? moduleMetaData.getModuleContainer() : null;

            // 1. Primary lookup: load the policy attachment directly from the current module Container
            if (moduleContainer != null) {
                Entry entry = moduleContainer.getEntry(location);
                if (entry != null) {
                    try {
                        is = entry.adapt(InputStream.class);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Start to load policy from template via moduleContainer: " + location);
                        }
                    } catch (UnableToAdaptException e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Unable to adapt entry to InputStream: " + location, e);
                        }
                    }
                }
            }

            // 2. Fallback: if Container lookup did not resolve an InputStream, use the module's ClassLoader
            if (is == null) {
                ClassLoader moduleClassLoader = (moduleMetaData != null && moduleMetaData.getModuleInfo() != null)
                                                ? moduleMetaData.getModuleInfo().getClassLoader()
                                                : getThreadContextClassLoader();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Start to load policy from template via classLoader: ", moduleClassLoader.getResource(location));
                }
                if (moduleClassLoader != null && moduleClassLoader.getResource(location) != null) {
                    is = moduleClassLoader.getResourceAsStream(location);
                }
            }

            if (null == is) {
                throw new PolicyException(new Exception("Could not open the special policy attachment file because getResourceAsStream is null"));
            }
            
            // Changed from DOMUtils.readXml(InputStream) to StaxUtils.read(InputStream)
            doc = StaxUtils.read(is);
        } catch (Exception ex) {
            throw new PolicyException(ex);
        }

        // Parse <wsp:PolicyAttachment> elements from the document
        for (Element ae : PolicyConstants.findAllPolicyElementsOfLocalName(doc,
                                                                           Constants.ELEM_POLICY_ATTACHMENT)) {
            PolicyAttachment attachment = new PolicyAttachment();

            for (Node nd = ae.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
                if (Node.ELEMENT_NODE != nd.getNodeType()) {
                    continue;
                }
                QName qn = new QName(nd.getNamespaceURI(), nd.getLocalName());
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Parse attachment element", nd.getNamespaceURI(), nd.getLocalName());
                }
                if (Constants.isAppliesToElem(qn)) {
                    Collection<DomainExpression> des = readDomainExpressions((Element) nd);
                    if (des.isEmpty()) {
                        // Forget about this attachment if domain expressions cannot be built
                        continue;
                    }
                    attachment.setDomainExpressions(des);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Size of attachment element for AppliesTo", des.size());
                    }
                } else if (Constants.isPolicyElement(qn)) {
                    Policy p = builder.getPolicy(nd);
                    if (null != attachment.getPolicy()) {
                        p = p.merge(attachment.getPolicy());
                    }
                    attachment.setPolicy(p);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Parse attachment element for Policy", p != null ? p.getAttributes() : null);
                    }
                } else if (Constants.isPolicyRef(qn)) {
                    PolicyReference ref = builder.getPolicyReference(nd);
                    if (null != ref) {
                        Policy p = resolveReference(ref, doc);
                        if (null != attachment.getPolicy()) {
                            p = p.merge(attachment.getPolicy());
                        }
                        attachment.setPolicy(p);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Parse attachment element for PolicyReference", p != null ? p.getAttributes() : null);
                        }
                    }
                } // TODO: wsse:Security child element
            }

            if (null == attachment.getPolicy() || null == attachment.getDomainExpressions()) {
                continue;
            }

            attachments.add(attachment);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Size of final policy attachment", attachments.size());
            }
        }
    }

    Policy resolveReference(PolicyReference ref, Document doc) {
        Policy p = null;
        if (isExternal(ref)) {
            p = resolveExternal(ref, doc.getBaseURI());
        } else {
            p = resolveLocal(ref, doc);
        }
        checkResolved(ref, p);
        return p;
    }

    Policy resolveLocal(PolicyReference ref, Document doc) {
        String relativeURI = ref.getURI().substring(1);
        String absoluteURI = doc.getBaseURI() + ref.getURI();
        Policy resolved = registry.lookup(absoluteURI);
        if (null != resolved) {
            return resolved;
        }
        ReferenceResolver resolver = new LocalDocumentReferenceResolver(doc, builder);
        resolved = resolver.resolveReference(relativeURI);
        if (null != resolved) {
            ref.setURI(absoluteURI);
            registry.register(absoluteURI, resolved);
        }
        return resolved;
    }

    Collection<DomainExpression> readDomainExpressions(Element appliesToElem) {
        Collection<DomainExpression> des = new ArrayList<DomainExpression>();
        for (Node nd = appliesToElem.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "readDomainExpressions by using", nd.getNamespaceURI(), nd.getLocalName());
                }
                if (nd.getLocalName().equals("URI")) {
                    URIDomainExpressionBuilder debr = bus.getExtension(URIDomainExpressionBuilder.class);
                    assert null != debr;
                    DomainExpression de = debr.build((Element) nd);
                    des.add(de);
                } else if (nd.getLocalName().equals("EndpointReference")) {
                    DomainExpressionBuilderRegistry debr = bus.getExtension(DomainExpressionBuilderRegistry.class);
                    assert null != debr;
                    DomainExpression de = debr.build((Element) nd);
                    des.add(de);
                } else {
                    throw new PolicyException(new Exception("No Related DomainExpressionBuilder"));
                }
            }
        }
        return des;
    }

    // for test

    void setAttachments(Collection<PolicyAttachment> a) {
        attachments = a;
    }

    public Collection<PolicyAttachment> getAttachments() {
        return attachments;
    }

    private ClassLoader getThreadContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }

        });
    }
}
