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

package org.apache.cxf.jaxws.interceptors;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.databinding.AbstractWrapperHelper;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.WrapperCapableDatabinding;
import org.apache.cxf.databinding.WrapperHelper;
import org.apache.cxf.helpers.ServiceUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;

public class WrapperClassOutInterceptor extends AbstractPhaseInterceptor<Message> {
    public WrapperClassOutInterceptor() {
        super(Phase.PRE_LOGICAL);
    }

    public void handleMessage(Message message) throws Fault {
        Exchange ex = message.getExchange();
        BindingOperationInfo bop = ex.getBindingOperationInfo();

        MessageInfo messageInfo = message.get(MessageInfo.class);
        if (messageInfo == null || bop == null || !bop.isUnwrapped()) {
            return;
        }

        BindingOperationInfo newbop = bop.getWrappedOperation();
        MessageInfo wrappedMsgInfo;
        if (Boolean.TRUE.equals(message.get(Message.REQUESTOR_ROLE))) {
            wrappedMsgInfo = newbop.getInput().getMessageInfo();
        } else {
            wrappedMsgInfo = newbop.getOutput().getMessageInfo();
        }

        Class<?> wrapped = null;
        if (wrappedMsgInfo.getMessagePartsNumber() > 0) {
            wrapped = wrappedMsgInfo.getFirstMessagePart().getTypeClass();
        }

        if (wrapped != null) {
            MessagePartInfo firstMessagePart = wrappedMsgInfo.getFirstMessagePart();
            MessageContentsList objs = MessageContentsList.getContentsList(message);
            WrapperHelper helper = firstMessagePart.getProperty("WRAPPER_CLASS", WrapperHelper.class);
            if (helper == null) {
                helper = getWrapperHelper(message, messageInfo, wrappedMsgInfo, wrapped, firstMessagePart);
            }
            if (helper == null) {
                return;
            }

            try {
                MessageContentsList newObjs = new MessageContentsList();
                if (ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, message)
                    && helper instanceof AbstractWrapperHelper) {
                    ((AbstractWrapperHelper)helper).setValidate(true);
                }
                Object o2 = helper.createWrapperObject(objs);
                newObjs.put(firstMessagePart, o2);

                for (MessagePartInfo p : messageInfo.getMessageParts()) {
                    if (Boolean.TRUE.equals(p.getProperty(ReflectionServiceFactoryBean.HEADER))) {
                        MessagePartInfo mpi = wrappedMsgInfo.getMessagePart(p.getName());
                        if (objs.hasValue(p)) {
                            newObjs.put(mpi, objs.get(p));
                        }
                    }
                }

                message.setContent(List.class, newObjs);
            } catch (Fault f) {
                throw f;
            } catch (Exception e) {
                throw new Fault(e);
            }

            // we've now wrapped the object, so use the wrapped binding op
            ex.put(BindingOperationInfo.class, newbop);

            if (messageInfo == bop.getOperationInfo().getInput()) {
                message.put(MessageInfo.class, newbop.getOperationInfo().getInput());
                message.put(BindingMessageInfo.class, newbop.getInput());
            } else if (messageInfo == bop.getOperationInfo().getOutput()) {
                message.put(MessageInfo.class, newbop.getOperationInfo().getOutput());
                message.put(BindingMessageInfo.class, newbop.getOutput());
            }
        }
    }


    private synchronized WrapperHelper getWrapperHelper(Message message,
                                           MessageInfo messageInfo,
                                           MessageInfo wrappedMessageInfo,
                                           Class<?> wrapClass,
                                           MessagePartInfo messagePartInfo) {
        WrapperHelper helper = messagePartInfo.getProperty("WRAPPER_CLASS", WrapperHelper.class);
        if (helper == null) {
            Service service = ServiceModelUtil.getService(message.getExchange());
            DataBinding dataBinding = service.getDataBinding();
            if (dataBinding instanceof WrapperCapableDatabinding) {
                helper = createWrapperHelper((WrapperCapableDatabinding)dataBinding,
                                             messageInfo, wrappedMessageInfo, wrapClass);
                messagePartInfo.setProperty("WRAPPER_CLASS", helper);
            }
        }
        return helper;
    }

    private void ensureSize(List<?> lst, int idx) {
        while (idx >= lst.size()) {
            lst.add(null);
        }
    }

    private WrapperHelper createWrapperHelper(WrapperCapableDatabinding dataBinding,
                                              MessageInfo messageInfo,
                                              MessageInfo wrappedMessageInfo,
                                              Class<?> wrapperClass) {
        List<String> partNames = new ArrayList<>();
        List<String> elTypeNames = new ArrayList<>();
        List<Class<?>> partClasses = new ArrayList<>();
        QName wrapperName = null;
        for (MessagePartInfo p : wrappedMessageInfo.getMessageParts()) {
            if (p.getTypeClass() == wrapperClass) {
                wrapperName = p.getElementQName();
            }
        }

        for (MessagePartInfo p : messageInfo.getMessageParts()) {
            if (p.getTypeClass() == null) {
                //WSDL part wasn't mapped to a parameter
                continue;
            }
            ensureSize(partNames, p.getIndex());
            ensureSize(elTypeNames, p.getIndex());
            ensureSize(partClasses, p.getIndex());

            partNames.set(p.getIndex(), p.getName().getLocalPart());

            final String elementType;
            if (p.getTypeQName() == null) {
                // handling anonymous complex type
                elementType = null;
            } else {
                elementType = p.getTypeQName().getLocalPart();
            }

            elTypeNames.set(p.getIndex(), elementType);
            partClasses.set(p.getIndex(), p.getTypeClass());
        }
        return dataBinding.createWrapperHelper(wrapperClass,
                                               wrapperName,
                                               partNames,
                                               elTypeNames,
                                               partClasses);
    }
}
