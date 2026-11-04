
package com.ibm.ws.test;

import java.util.Map;

import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * Expose a REST endpoint using a BELL service that implements REST Handler SPI.
 */
public class BellEndpoint implements RESTHandler {

    public BellEndpoint() {
        System.out.println("BellEndpoint.<ctor>: new instance");
    }

    Map<String, String> bellProps = null;

    // Required to receive configured properties
    public void updateBell(Map<String,String> props) {
        bellProps = props;
        System.out.println("BellEndpoint.updateBell: injected props=" + bellProps);
    }

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) {
        System.out.println("BellEndpoint.handleRequest: hello " + bellProps.get("hello"));
    }
}

