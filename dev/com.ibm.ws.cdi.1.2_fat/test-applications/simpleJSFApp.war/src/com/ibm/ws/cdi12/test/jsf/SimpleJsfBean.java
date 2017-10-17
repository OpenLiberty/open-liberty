package com.ibm.ws.cdi12.test.jsf;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class SimpleJsfBean {
    private @Inject
    OtherJsfBean bean;

    public String getMessage() {
        String response = "Hello from SimpleJsfBean";
        if (this.bean != null) {
            response = response + " injected with: " + this.bean.getValue();
        }
        return response;
    }
}
