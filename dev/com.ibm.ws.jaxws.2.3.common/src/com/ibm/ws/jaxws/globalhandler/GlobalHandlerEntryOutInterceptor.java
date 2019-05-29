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

import java.util.List;
import java.util.ListIterator;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

import com.ibm.webservices.handler.impl.GlobalHandlerService;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.bus.LibertyApplicationBus;
import com.ibm.wsspi.webservices.handler.Handler;

public class GlobalHandlerEntryOutInterceptor
                extends AbstractJAXWSGlobalHandlerInterceptor<Message> {
    private final GlobalHandlerInterceptor globalHandlerOutInterceptor;
    private static final SAAJOutInterceptor SAAJ_OUT = new SAAJOutInterceptor();
    private static final TraceComponent tc = Tr.register(GlobalHandlerEntryOutInterceptor.class);

    public GlobalHandlerEntryOutInterceptor(String flowType, LibertyApplicationBus.Type bus) {
        super(Phase.PRE_PROTOCOL);
        addBefore(SAAJOutInterceptor.class.getName());
        globalHandlerOutInterceptor = new GlobalHandlerInterceptor(Phase.USER_PROTOCOL, "out", bus);
    }

    @Override
    public void handleMessage(Message message) {

        GlobalHandlerService globalHandlerService = JaxwsGlobalHandlerServiceImpl.globalHandlerServiceSR.getService();
        List<Handler> registeredHandlers1 = globalHandlerService.getJAXWSClientSideInFlowGlobalHandlers();
        List<Handler> registeredHandlers2 = globalHandlerService.getJAXWSClientSideOutFlowGlobalHandlers();
        List<Handler> registeredHandlers3 = globalHandlerService.getJAXWSServerSideInFlowGlobalHandlers();
        List<Handler> registeredHandlers4 = globalHandlerService.getJAXWSServerSideOutFlowGlobalHandlers();
        if (registeredHandlers1.isEmpty() && registeredHandlers2.isEmpty() && registeredHandlers3.isEmpty() && registeredHandlers4.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "no registered global handlers");
            }
            return;

        }
        boolean saajEnabled = globalHandlerService.getSaajFlag();
        if (!chainAlreadyContainsSAAJ((SoapMessage) message) && saajEnabled) {
            message.getInterceptorChain().add(SAAJ_OUT);

        }

        message.getInterceptorChain().add(globalHandlerOutInterceptor);

    }

    private static boolean chainAlreadyContainsSAAJ(SoapMessage message) {
        ListIterator<Interceptor<? extends Message>> listIterator =
                        message.getInterceptorChain().getIterator();
        while (listIterator.hasNext()) {
            if (listIterator.next() instanceof SAAJOutInterceptor) {
                return true;
            }
        }
        return false;
    }

}
