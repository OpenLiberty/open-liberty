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
package com.ibm.ws.cdi12.test.rootClassLoader.extension;

import java.util.Random;
import java.util.Timer;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;

/**
 *
 */
public class MyExtension implements Extension {
    void afterBeanDiscovery(@Observes AfterBeanDiscovery abd) {
        Bean<Random> randomBean = new RandomBean();
        abd.addBean(randomBean);
        Bean<Timer> timerBean = new TimerBean();
        abd.addBean(timerBean);
        Bean<String> osNameBean = new OSNameBean();
        abd.addBean(osNameBean);
    }
}
