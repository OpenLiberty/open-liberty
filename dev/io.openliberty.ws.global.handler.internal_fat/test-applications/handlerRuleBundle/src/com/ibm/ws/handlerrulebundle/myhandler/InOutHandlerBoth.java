package com.ibm.ws.handlerrulebundle.myhandler;

import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.Handler;

public class InOutHandlerBoth implements Handler {

    @Override
    public void handleFault(GlobalHandlerMessageContext arg0) {
        System.out.println("in RSInHandler1 handleFault method");

    }

    @Override
    public void handleMessage(GlobalHandlerMessageContext msgctxt) throws Exception {

        System.out.println("in InOutHandlerBoth handleMessage method");
        String engineType = msgctxt.getEngineType();
        String flowType = msgctxt.getFlowType();
        System.out.println("get Engine.Type in InOutHandlerBoth:" + engineType);
        System.out.println("get Flow.Type in InOutHandlerBoth:" + flowType);
    }

}
