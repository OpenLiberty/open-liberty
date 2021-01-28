package com.ibm.ws.rsuserbundle1.myhandler;

import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.Handler;

public class RSClientOutHander implements Handler {

    @Override
    public void handleFault(GlobalHandlerMessageContext arg0) {
        System.out.println("in RSInHandler1 handleFault method");

    }

    @Override
    public void handleMessage(GlobalHandlerMessageContext msgctxt) throws Exception {

        System.out.println("in RSClientOutHander handleMessage method");
        String engineType = msgctxt.getEngineType();
        String flowType = msgctxt.getFlowType();
        System.out.println("get Engine.Type in RSClientOutHander:" + engineType);
        System.out.println("get Flow.Type in RSClientOutHander:" + flowType);
    }

}
