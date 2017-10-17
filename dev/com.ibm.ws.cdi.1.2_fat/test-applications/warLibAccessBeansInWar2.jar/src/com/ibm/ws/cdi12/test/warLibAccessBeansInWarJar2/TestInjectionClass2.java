package com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar2;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

/**
 *
 */
@Dependent
public class TestInjectionClass2 {

    @Inject
    WarBeanInterface2 bean;

    public String getMessage() {

        return ("TestInjectionClass2: " + bean.getBeanMessage());
    }

}
