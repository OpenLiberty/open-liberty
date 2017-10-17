/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.dynamicBeans;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

/**
 *
 */
public class MyCDIExtension implements Extension {

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        DynamicBean1Bean bean1 = new DynamicBean1Bean();
        event.addBean(bean1);
        DynamicBean2Bean bean2 = new DynamicBean2Bean();
        event.addBean(bean2);
    }

}
