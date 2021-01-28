package com.ibm.ws.userbundle2.myhandler;

import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.Handler;
import com.ibm.wsspi.webservices.handler.HandlerConstants;

public class TestHandler1InBundle2 implements Handler {

    @Override
    public void handleFault(GlobalHandlerMessageContext arg0) {
        System.out.println("handle fault in TestHandler1InBundle2");

    }

    @Override
    public void handleMessage(GlobalHandlerMessageContext msgctxt) throws Exception {

        if (msgctxt.getFlowType().equalsIgnoreCase(HandlerConstants.FLOW_TYPE_OUT)) {
            System.out.println("handle outbound message in TestHandler1InBundle2");
        } else {
            System.out.println("handle inbound message in TestHandler1InBundle2");
        }

    }

}
