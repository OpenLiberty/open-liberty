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

package com.ibm.ws.jaxws.globalhandler;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.cxf.jaxws.handler.logical.LogicalMessageContextImpl;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceModelUtil;

public abstract class AbstractJAXWSGlobalHandlerInterceptor<T extends Message> extends AbstractPhaseInterceptor<T> {

    protected AbstractJAXWSGlobalHandlerInterceptor(String phase) {
        super(phase);
    }

    protected void setupBindingOperationInfo(Exchange exch, Object data) {
        if (exch.get(BindingOperationInfo.class) == null) {
            QName opName = getOpQName(exch, data);
            if (opName == null) {
                return;
            }
            BindingOperationInfo bop = ServiceModelUtil
                            .getOperationForWrapperElement(exch, opName, false);
            if (bop == null) {
                bop = ServiceModelUtil.getOperation(exch, opName);
            }
            if (bop != null) {
                exch.put(BindingOperationInfo.class, bop);
                exch.put(OperationInfo.class, bop.getOperationInfo());
                if (bop.getOutput() == null) {
                    exch.setOneWay(true);
                }
            }

        }
    }

    protected QName getOpQName(Exchange ex, Object data) {
        LogicalMessageContextImpl sm = (LogicalMessageContextImpl) data;
        Source src = sm.getMessage().getPayload();
        if (src instanceof DOMSource) {
            DOMSource dsrc = (DOMSource) src;
            String ln = dsrc.getNode().getLocalName();
            String ns = dsrc.getNode().getNamespaceURI();
            return new QName(ns, ln);
        }
        return null;
    }

}
