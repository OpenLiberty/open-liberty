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
