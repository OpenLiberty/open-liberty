package com.ibm.ws.cdi.extension.spi.test.app;

import javax.enterprise.context.RequestScoped;

import com.ibm.ws.cdi.extension.spi.test.bundle.getclass.interceptor.Intercept;

@RequestScoped
@Intercept
public class AppBean {

    public String toString() {
        return "application bean";
    }

}
