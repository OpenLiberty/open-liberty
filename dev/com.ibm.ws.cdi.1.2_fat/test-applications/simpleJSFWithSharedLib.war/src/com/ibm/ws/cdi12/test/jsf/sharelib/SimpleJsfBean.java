package com.ibm.ws.cdi12.test.jsf.sharelib;

import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.ws.cdi12.test.shared.InjectedHello;

@Named
public class SimpleJsfBean {
    private @Inject
    InjectedHello bean;

    public String getMessage() {
        String response = "SimpleJsfBean";
        if (this.bean != null) {
            response = response + " injected with: " + this.bean.areYouThere(response);
        }
        return response;
    }
}
