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
package com.ibm.ws.cdi12.test.current.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Extension;

public class MyDeploymentVerifier implements Extension {

    private static List<String> messages = new ArrayList<String>();

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        event.addBean(new CDICurrentTestBean());
    }

    public void afterDeploymentValidation(@Observes AfterDeploymentValidation event) {
        BeanManager beanManager = CDI.current().getBeanManager();

        Set<Bean<?>> beans = beanManager.getBeans(CDICurrent.class, DefaultLiteral.INSTANCE);
        if (beans != null && beans.size() == 1) {
            Bean<?> bean = beans.iterator().next();
            if (bean.getBeanClass() == CDICurrent.class) {
                messages.add("SUCCESS");
            }
            else {
                messages.add("FAIL: Bean Class = " + bean.getBeanClass());
            }
        }
        else {
            messages.add("FAIL: number of beans = " + beans == null ? "NULL" : "" + beans.size());
        }
    }

    public static List<String> getMessages() {
        return messages;
    }
}
