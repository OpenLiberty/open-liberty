/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
