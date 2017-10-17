package com.ibm.ws.cdi12.test.jsp;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

@RequestScoped
@Named
public class SimpleJspBean {

    public String getMessage() {
        String response = "Test Sucessful!";
        return response;
    }
}
