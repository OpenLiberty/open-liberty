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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.common.logging.LogUtils;
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
    private static final Logger LOG = LogUtils.getLogger(WrapperClassOutInterceptor.class);  // Liberty Change issue #26529
    public WrapperClassOutInterceptor() {
        super(Phase.PRE_LOGICAL);
    }

    public void handleMessage(Message message) throws Fault {
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);   // Liberty Change issue #26529
        Exchange ex = message.getExchange();
        BindingOperationInfo bop = ex.getBindingOperationInfo();

        MessageInfo messageInfo = message.get(MessageInfo.class);
        if (messageInfo == null || bop == null || !bop.isUnwrapped()) {
            if(isFinestEnabled)  {
                LOG.finest("MessageInfo(messageInfo):" + messageInfo + " or BindingOperationInfo(bop):" + bop + " is null or BindingOperationInfo(bop) is wrapped: " + !bop.isUnwrapped() + ". Returning.");   // Liberty Change issue #26529
            } 
            return;
        }

        BindingOperationInfo newbop = bop.getWrappedOperation();
        MessageInfo wrappedMsgInfo;
        if (isRequestor(message)) {     // Liberty Change issue #26529
            wrappedMsgInfo = newbop.getInput().getMessageInfo();
        } else {
            wrappedMsgInfo = newbop.getOutput().getMessageInfo();
        }

        Class<?> wrapped = null;
        if (wrappedMsgInfo.getMessagePartsNumber() > 0) {
            wrapped = wrappedMsgInfo.getFirstMessagePart().getTypeClass();
        }
        if(isFinestEnabled)  {
            LOG.finest("Type class of first mesage part: " + wrapped);   // Liberty Change issue #26529
        } 
        if (wrapped != null) {
            MessagePartInfo firstMessagePart = wrappedMsgInfo.getFirstMessagePart();
            MessageContentsList objs = MessageContentsList.getContentsList(message);
            WrapperHelper helper = firstMessagePart.getProperty("WRAPPER_CLASS", WrapperHelper.class);
            if(isFinestEnabled)  {
                LOG.finest("WrapperHelper(helper) that is obtained from first MessagePartInfo WRAPPER_CLASS property: " + helper);   // Liberty Change issue #26529
            } 
            if (helper == null) {
                helper = getWrapperHelper(message, messageInfo, wrappedMsgInfo, wrapped, firstMessagePart);
                if(isFinestEnabled)  {
                    LOG.finest("WrapperHelper(helper) was null. It is obtained from getWrapperHelper method: " + helper);   // Liberty Change issue #26529
                } 
            }
            if (helper == null) {
                if(isFinestEnabled)  {
                    LOG.finest("WrapperHelper(helper) is still null. Returning.");   // Liberty Change issue #26529
                } 
                return;
            }

            try {
                MessageContentsList newObjs = new MessageContentsList();
                if (ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, message)
                    && helper instanceof AbstractWrapperHelper) {
                    ((AbstractWrapperHelper)helper).setValidate(true);
                    if(isFinestEnabled)  {
                        LOG.finest("Validation is set to true for AbstractWrapperHelper.");   // Liberty Change issue #26529
                    } 
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
                if(isFinestEnabled)  {
                    LOG.finest("New MessageContentsList is set into Message: " + newObjs);   // Liberty Change issue #26529
                }
            } catch (Fault f) {
                throw f;
            } catch (Exception e) {
                throw new Fault(e);
            }

            // we've now wrapped the object, so use the wrapped binding op
            ex.put(BindingOperationInfo.class, newbop);
            if(isFinestEnabled)  {
                LOG.finest("New BindingOperationInfo is set into Exchange: " + newbop);   // Liberty Change issue #26529
            }
            
            if (messageInfo == bop.getOperationInfo().getInput()) {
                message.put(MessageInfo.class, newbop.getOperationInfo().getInput());
                message.put(BindingMessageInfo.class, newbop.getInput());
            } else if (messageInfo == bop.getOperationInfo().getOutput()) {
                message.put(MessageInfo.class, newbop.getOperationInfo().getOutput());
                message.put(BindingMessageInfo.class, newbop.getOutput());
            }
            if(isFinestEnabled)  {
                LOG.finest("MessageInfo that is put into message: " + message.get(MessageInfo.class));   // Liberty Change issue #26529
                LOG.finest("BindingMessageInfo that is put into message: " + message.get(BindingMessageInfo.class));   // Liberty Change issue #26529                
            }
        }
    }


    private synchronized WrapperHelper getWrapperHelper(Message message,
                                           MessageInfo messageInfo,
                                           MessageInfo wrappedMessageInfo,
                                           Class<?> wrapClass,
                                           MessagePartInfo messagePartInfo) {
        WrapperHelper helper = messagePartInfo.getProperty("WRAPPER_CLASS", WrapperHelper.class);
        if(LOG.isLoggable(Level.FINEST))  {
            LOG.finest("WrapperHelper that is obtained from first MessagePartInfo WRAPPER_CLASS property: " + helper);   // Liberty Change issue #26529
        } 
        if (helper == null) {
            Service service = ServiceModelUtil.getService(message.getExchange());
            DataBinding dataBinding = service.getDataBinding();
            if (dataBinding instanceof WrapperCapableDatabinding) {
                helper = createWrapperHelper((WrapperCapableDatabinding)dataBinding,
                                             messageInfo, wrappedMessageInfo, wrapClass);
                messagePartInfo.setProperty("WRAPPER_CLASS", helper);
                if(LOG.isLoggable(Level.FINEST))  {
                    LOG.finest("WrapperHelper was null. It is obtained from getWrapperHelper method: " + helper + " and it's set in WRAPPER_CLASS property of messagePartInfo");   // Liberty Change issue #26529
                } 
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
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);   // Liberty Change issue #26529
        List<String> partNames = new ArrayList<>();
        List<String> elTypeNames = new ArrayList<>();
        List<Class<?>> partClasses = new ArrayList<>();
        QName wrapperName = null;
        for (MessagePartInfo p : wrappedMessageInfo.getMessageParts()) {
            if (p.getTypeClass() == wrapperClass) {
                wrapperName = p.getElementQName();
                if(isFinestEnabled)  {
                    LOG.finest("MessagePartInfo(p) class matches wrapper class name that is obtained from parameter value. Corresponding element QName: " + wrapperName);   // Liberty Change issue #26529
                }
                break; // Liberty Change: we need to break the for loop here since we find what we are looking for 
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
            if(isFinestEnabled)  {
                LOG.finest("Added messagePartInfo local part: " + partNames.get(p.getIndex()) + " is added to part names");   // Liberty Change issue #26529
            }
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
        if(isFinestEnabled)  {
            LOG.finest("Added part names: " + partNames);   // Liberty Change issue #26529
            LOG.finest("Added element type names: " + elTypeNames);   // Liberty Change issue #26529
            LOG.finest("Added type classes: " + elTypeNames);   // Liberty Change issue #26529
        }
        return dataBinding.createWrapperHelper(wrapperClass,
                                               wrapperName,
                                               partNames,
                                               elTypeNames,
                                               partClasses);
    }
}
