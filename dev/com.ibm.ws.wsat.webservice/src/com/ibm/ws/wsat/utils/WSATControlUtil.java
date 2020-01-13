/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.Names;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.wsat.service.Protocol;

/**
 *
 */
public class WSATControlUtil {

    private WSATControlUtil() {

    }

    private static WSATControlUtil instance = null;

    public static WSATControlUtil getInstance() {
        if (instance == null) {
            instance = new WSATControlUtil();
        }
        return instance;
    }

    private static final TraceComponent tc = Tr.register(WSATControlUtil.class, Constants.TRACE_GROUP, null);

    public String trace(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    public Map<String, String> getPropertiesMap(List<Header> list) {
        HashMap<String, String> map = new HashMap<String, String>();
        for (Header h : list) {
            Element ele = (Element) h.getObject();
            QName name = createQNameFromElement(ele);
            if (name != null && name.getNamespaceURI() != null && ele.getFirstChild() != null) {
                if (Constants.WS_WSAT_CTX_REF.getLocalPart().equals(name.getLocalPart()) && Constants.WS_WSAT_CTX_REF.getNamespaceURI().equals(name.getNamespaceURI())) {
                    map.put(Constants.WS_WSAT_CTX_REF.getLocalPart(), ele.getFirstChild().getNodeValue());
                } else if (Constants.WS_WSAT_PART_REF.getLocalPart().equals(name.getLocalPart()) && Constants.WS_WSAT_PART_REF.getNamespaceURI().equals(name.getNamespaceURI())) {
                    map.put(Constants.WS_WSAT_PART_REF.getLocalPart(), ele.getFirstChild().getNodeValue());
                } else if (Names.WSA_FAULTTO_QNAME.getLocalPart().equals(name.getLocalPart()) && Names.WSA_FAULTTO_QNAME.getNamespaceURI().equals(name.getNamespaceURI())) {
                    map.put(Names.WSA_FAULTTO_QNAME.getLocalPart(), ele.getFirstChild().getFirstChild().getNodeValue());
                } else if (Names.WSA_REPLYTO_QNAME.getLocalPart().equals(name.getLocalPart()) && Names.WSA_REPLYTO_QNAME.getNamespaceURI().equals(name.getNamespaceURI())) {
                    map.put(Names.WSA_REPLYTO_QNAME.getLocalPart(), ele.getFirstChild().getFirstChild().getNodeValue());
                }
            }
        }
        return map;
    }

    public ProtocolServiceWrapper getService(WrappedMessageContext wmc) {
        List<Header> headers = CastUtils.cast((List<?>) wmc.getWrappedMessage().get(Header.HEADER_LIST));
        List<Header> migration = new ArrayList<Header>();
        Map<String, String> wsatProperties = WSATControlUtil.getInstance().getPropertiesMap(headers);
        String ctxID = wsatProperties.get(Constants.WS_WSAT_CTX_REF.getLocalPart());
        String partID = wsatProperties.get(Constants.WS_WSAT_PART_REF.getLocalPart());
        AddressingProperties addressProp = (AddressingProperties) wmc
                        .get(org.apache.cxf.ws.addressing.JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_INBOUND);
        EndpointReferenceType replyTo = addressProp.getReplyTo();
        EndpointReferenceType faultTo = addressProp.getFaultTo();
        EndpointReferenceType from = addressProp.getFrom();
        for (Header h : headers) {
            Element ele = (Element) h.getObject();
            QName name = WSATControlUtil.getInstance().createQNameFromElement(ele);
            if (!Names.WSA_NAMESPACE_NAME.equals(name.getNamespaceURI())) {
                migration.add(h);
            }
        }

        if (faultTo == null) {
            faultTo = replyTo;
        }

        Protocol service = WSATOSGIService.getInstance().getProtocolService();

        return new ProtocolServiceWrapper().setFaultTo(faultTo).setReplyTo(replyTo).setFrom(from).setService(service).setTxID(ctxID).setPartID(partID).setMigrationHeaders(migration).setNextStepEPR(replyTo);
    }

    /**
     * @param protocolId
     * @return
     */
    public boolean checkProtocolId(String protocolId) {
        //validate protocol id, either voliate2PC or durable2PC
        // voliate2PC is not supported...
        return protocolId.endsWith(Constants.WS_AT_PROTOCOL);
    }

    /**
     * @param ele
     * @return
     */
    public QName createQNameFromElement(Element ele) {
        if (ele.getLocalName() == null) {
            return null;
        } else if (ele.getNamespaceURI() == null && ele.getPrefix() == null) {
            return new QName(ele.getLocalName());
        } else if (ele.getNamespaceURI() != null)
            if (ele.getPrefix() == null) {
                return new QName(ele.getNamespaceURI(), ele.getLocalName());
            } else
                return new QName(ele.getNamespaceURI(), ele.getLocalName(), ele.getPrefix());
        return null;
    }

}
