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
package org.apache.cxf.management.interceptor;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.management.counters.Counter;
import org.apache.cxf.management.counters.CounterRepository;
import org.apache.cxf.management.counters.MessageHandlingTimeRecorder;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;

public abstract class AbstractMessageResponseTimeInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractMessageResponseTimeInterceptor.class);
    private static final String QUESTION_MARK = "?";
    private static final String ESCAPED_QUESTION_MARK = "\\?";

    Logger log = Logger.getLogger(AbstractMessageResponseTimeInterceptor.class.getName());
    
    AbstractMessageResponseTimeInterceptor(String phase) {
        super(phase);
    }

    protected boolean isClient(Message msg) {
        return msg != null && Boolean.TRUE.equals(msg.get(Message.REQUESTOR_ROLE));
    }

    protected void beginHandlingMessage(Exchange ex) {
        if (null == ex) {
            return;
        }

        MessageHandlingTimeRecorder mhtr = ex.get(MessageHandlingTimeRecorder.class);
        if (null != mhtr) {
            mhtr.beginHandling();
        } else {
            mhtr = new MessageHandlingTimeRecorder(ex);
            mhtr.beginHandling();
        }
    }

    protected void endHandlingMessage(Exchange ex) {
        if (null == ex) {
            return;
        }
        MessageHandlingTimeRecorder mhtr = ex.get(MessageHandlingTimeRecorder.class);
        if (null != mhtr) {
            mhtr.endHandling();
            mhtr.setFaultMode(ex.get(FaultMode.class));
            increaseCounter(ex, mhtr);

        } // else can't get the MessageHandling Infor
    }

    protected void setOneWayMessage(Exchange ex) {
        MessageHandlingTimeRecorder mhtr = ex.get(MessageHandlingTimeRecorder.class);
        if (null == mhtr) {
            mhtr = new MessageHandlingTimeRecorder(ex);
        } else {
            mhtr.endHandling();
        }
        mhtr.setOneWay(true);
        increaseCounter(ex, mhtr);
    }

    private void increaseCounter(Exchange ex, MessageHandlingTimeRecorder mhtr) {
        Bus bus = ex.getBus();
        if (null == bus) {
            LOG.log(Level.INFO, "CAN_NOT_GET_BUS_FROM_EXCHANGE");
            bus = BusFactory.getThreadDefaultBus();
        }
        CounterRepository cr = bus.getExtension(CounterRepository.class);

        if (null == cr) {
            LOG.log(Level.WARNING, "NO_COUNTER_REPOSITORY");
            return;
        }
        ObjectName serviceCountername = this.getServiceCounterName(ex);
        cr.increaseCounter(serviceCountername, mhtr);

        ObjectName operationCounter = this.getOperationCounterName(ex, serviceCountername);
        cr.increaseCounter(operationCounter, mhtr);
    }
    
    protected ObjectName getServiceCounterName(Exchange ex) {
        Bus bus = ex.getBus();
        StringBuilder buffer = new StringBuilder();
        ObjectName serviceCounterName = null;
        if (ex.get("org.apache.cxf.management.service.counter.name") != null) {
            buffer.append((String)ex.get("org.apache.cxf.management.service.counter.name"));
            try {
                serviceCounterName = new ObjectName(buffer.toString());
            } catch (MalformedObjectNameException e) {
                LOG.log(Level.WARNING, "CREATE_COUNTER_OBJECTNAME_FAILED", e);
            }
        } else {
            Service service = ex.getService();
            Endpoint endpoint = ex.getEndpoint();
            serviceCounterName = (ObjectName)endpoint.get("javax.management.ObjectName");
            if (serviceCounterName == null) {
                String serviceName = "\"" + escapePatternChars(service.getName().toString()) + "\"";
                String portName = "\"" + endpoint.getEndpointInfo().getName().getLocalPart() + "\"";

                buffer.append(ManagementConstants.DEFAULT_DOMAIN_NAME).append(':');
                buffer.append(ManagementConstants.BUS_ID_PROP).append('=').append(bus.getId()).append(',');
                Message message = ex.getOutMessage();
                if (isClient(message)) {
                    buffer.append(ManagementConstants.TYPE_PROP).append('=')
                        .append(Counter.PERFORMANCE_COUNTER).append(".Client,");
                } else {
                    buffer.append(ManagementConstants.TYPE_PROP).append('=')
                        .append(Counter.PERFORMANCE_COUNTER).append(".Server,");
                }
                buffer.append(ManagementConstants.SERVICE_NAME_PROP).append('=').append(serviceName).append(',');

                buffer.append(ManagementConstants.PORT_NAME_PROP).append('=').append(portName);

                try {
                    serviceCounterName = new ObjectName(buffer.toString());
                    endpoint.put("javax.management.ObjectName", serviceCounterName);
                } catch (MalformedObjectNameException e) {
                    LOG.log(Level.WARNING, "CREATE_COUNTER_OBJECTNAME_FAILED", e);
                }
            }
        }
        return serviceCounterName;

    }

    protected boolean isServiceCounterEnabled(Exchange ex) {
        Bus bus = ex.getBus();
        CounterRepository counterRepo = bus.getExtension(CounterRepository.class);
        if (counterRepo == null || skipCounter(ex)) { // Liberty change - skipCounter is added to prevent IllegalArgumentException thrown when there are special characters in namespace 
            return false;
        }
        ObjectName serviceCounterName = getServiceCounterName(ex);
        Counter serviceCounter = counterRepo.getCounter(serviceCounterName);
        //If serviceCounter is null, we need to wait ResponseTimeOutInterceptor to create it , hence set to true
        return serviceCounter == null || serviceCounter.isEnabled();
    }

    protected ObjectName getOperationCounterName(Exchange ex, ObjectName sericeCounterName) {
        BindingOperationInfo bop = ex.getBindingOperationInfo();
        OperationInfo opInfo = bop == null ? null : bop.getOperationInfo();
        if (opInfo != null) {
            ObjectName o = opInfo.getProperty("javax.management.ObjectName", ObjectName.class);
            if (o != null) {
                return o;
            }
        }
        String operationName = opInfo == null ? null : "\"" + opInfo.getName().getLocalPart() + "\"";

        if (operationName == null) {
            Object nameProperty = ex.get("org.apache.cxf.resource.operation.name");
            if (nameProperty != null) {
                operationName = "\"" + escapePatternChars(nameProperty.toString()) + "\"";
            }
        }
        StringBuilder buffer = new StringBuilder(sericeCounterName.toString());
        if (operationName != null) {
            buffer.append(',').append(ManagementConstants.OPERATION_NAME_PROP).append('=').append(operationName);
        }
        String operationCounterName = buffer.toString();
        ObjectName operationCounter = null;
        try {
            operationCounter = new ObjectName(operationCounterName);
            if (opInfo != null) {
                opInfo.setProperty("javax.management.ObjectName", operationCounter);
            }
        } catch (MalformedObjectNameException e) {
            LOG.log(Level.WARNING, "CREATE_COUNTER_OBJECTNAME_FAILED", e);
        }
        return operationCounter;

    }
    protected String escapePatternChars(String value) {
        // This can be replaced if really needed with pattern-based matching
        if (value.lastIndexOf(QUESTION_MARK) != -1) {
            value = value.replace(QUESTION_MARK, ESCAPED_QUESTION_MARK);
        }
        return value;
    }
    
    // Liberty change begin
    private boolean skipCounter(Exchange ex)       {
        Object interfaceObject = ex.get("javax.xml.ws.wsdl.interface");
        Object portService = ex.get("javax.xml.ws.wsdl.service");
        Object portObject = ex.get("javax.xml.ws.wsdl.port");
        Object portOperation = ex.get("javax.xml.ws.wsdl.operation");
        
        return checkForSpecialCharacters(interfaceObject) || checkForSpecialCharacters(portService) || checkForSpecialCharacters(portObject) || checkForSpecialCharacters(portOperation);
    }
    
    private boolean checkForSpecialCharacters(Object obj) {
        if(obj != null && obj instanceof QName)     {
            String namespaceURI = ((QName) obj).getNamespaceURI();
            if (!namespaceURI.matches("[a-zA-Z0-9./:?_]*$")) {
                return true;
            }
        }
        return false;
    }
    // Liberty change end
}
