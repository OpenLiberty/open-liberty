package com.ibm.ws.rsuserbundle2.myhandler;

import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.Handler;

public class RSInHandler3 implements Handler {

    @Override
    public void handleFault(GlobalHandlerMessageContext arg0) {
        System.out.println("in InHandlerRS handleFault method");

    }

    @Override
    public void handleMessage(GlobalHandlerMessageContext msgctxt) throws Exception {

        System.out.println("in InHandlerWS handleMessage method");
        String engineType = msgctxt.getEngineType();
        String flowType = msgctxt.getFlowType();
        System.out.println("get Engine.Type in InHandlerWS:" + engineType);
        System.out.println("get Flow.Type in InHandlerWS:" + flowType);
    }

}
