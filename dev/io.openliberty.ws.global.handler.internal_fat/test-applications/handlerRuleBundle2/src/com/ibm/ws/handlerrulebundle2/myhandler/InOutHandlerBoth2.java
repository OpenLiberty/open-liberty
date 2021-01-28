package com.ibm.ws.handlerrulebundle2.myhandler;

import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.Handler;

public class InOutHandlerBoth2 implements Handler {

    @Override
    public void handleFault(GlobalHandlerMessageContext arg0) {
        System.out.println("in InOutHandlerBoth2 handleFault method");

    }

    @Override
    public void handleMessage(GlobalHandlerMessageContext msgctxt) throws Exception {

        System.out.println("in InOutHandlerBoth2 handleMessage method");
        String engineType = msgctxt.getEngineType();
        String flowType = msgctxt.getFlowType();
        System.out.println("get Engine.Type in InOutHandlerBoth2:" + engineType);
        System.out.println("get Flow.Type in InOutHandlerBoth2:" + flowType);
    }

}
