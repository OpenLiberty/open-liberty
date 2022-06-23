package com.ibm.ws.ddTimeout.web;

import javax.ejb.Stateless;

@Stateless
public class TestEJB {

    public void method() throws InterruptedException {
        // Global transaction timeout is set to 20s in the deployment descriptor
        Thread.sleep(30000);
    }
}