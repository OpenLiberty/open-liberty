/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/////////////////////////////////////////////////////////////////////////
//
// Copyright University of Southampton IT Innovation Centre, 2009
//
// Copyright in this library belongs to the University of Southampton
// University Road, Highfield, Southampton, UK, SO17 1BJ
//
// This software may not be used, sold, licensed, transferred, copied
// or reproduced in whole or in part in any manner or form or in or
// on any media by any person other than in accordance with the terms
// of the License Agreement supplied with the software, or otherwise
// without the prior written consent of the copyright owners.
//
// This software is distributed WITHOUT ANY WARRANTY, without even the
// implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
// PURPOSE, except where stated in the License Agreement supplied with
// the software.
//
//  Created By :            Dominic Harries
//  Created Date :          2009-03-31
//  Created for Project :   BEinGRID
//
/////////////////////////////////////////////////////////////////////////


package org.apache.cxf.ws.policy;

import java.util.Collection;
import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.Extensible;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.ws.policy.attachment.external.PolicyAttachment;
import org.apache.neethi.Constants;
import org.apache.neethi.Policy;

public class ServiceModelPolicyUpdater {
    private EndpointInfo ei;

    public ServiceModelPolicyUpdater(EndpointInfo ei) {
        this.ei = ei;
    }

    public void addPolicyAttachments(Collection<PolicyAttachment> attachments) {
        for (PolicyAttachment pa : attachments) {
            boolean policyUsed = false;
            String policyId = pa.getPolicy().getId();

            // Add wsp:PolicyReference to wsdl:binding/wsdl:operation
            for (BindingOperationInfo boi : ei.getBinding().getOperations()) {
                if (pa.appliesTo(boi)) {
                    addPolicyRef(boi, policyId);
                    // Add it to wsdl:portType/wsdl:operation too
                    addPolicyRef(ei.getInterface().getOperation(boi.getName()), policyId);
                    policyUsed = true;
                }
            }

            // Add wsp:Policy to top-level wsdl:definitions
            if (policyUsed) {
                addPolicy(pa.getPolicy());
            }
        }
    }

    private void addPolicyRef(Extensible ext, String policyId) {
        Document doc = DOMUtils.createDocument();
        Element el = doc.createElementNS(Constants.URI_POLICY_13_NS, Constants.ELEM_POLICY_REF);
        el.setPrefix(Constants.ATTR_WSP);
        el.setAttribute(Constants.ATTR_URI, "#" + policyId);

        UnknownExtensibilityElement uee = new UnknownExtensibilityElement();
        uee.setElementType(new QName(Constants.URI_POLICY_13_NS, Constants.ELEM_POLICY_REF));
        uee.setElement(el);
        uee.setRequired(true);

        ext.addExtensor(uee);
    }

    private void addPolicy(Policy p) {
        try {
            W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
            p.serialize(writer);
            Element policyEl = writer.getDocument().getDocumentElement();

            // Remove xmlns:xmlns attribute which Xerces chokes on
            policyEl.removeAttribute("xmlns:xmlns");

            UnknownExtensibilityElement uee = new UnknownExtensibilityElement();
            uee.setElementType(new QName(Constants.URI_POLICY_13_NS, Constants.ELEM_POLICY));
            uee.setElement(policyEl);

            ei.getService().addExtensor(uee);
        } catch (XMLStreamException ex) {
            throw new RuntimeException("Could not serialize policy", ex);
        }
    }
}
