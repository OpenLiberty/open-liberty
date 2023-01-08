/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.cdi40.internal.fat.bce.basicwar;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Shutdown;
import jakarta.enterprise.event.Startup;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.ScannedClasses;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;

public class BasicBCEExtension implements BuildCompatibleExtension {

    @Discovery
    public void addBean(ScannedClasses classes) {
        classes.add(TestBeanRegistered.class.getName());
    }

    @Synthesis
    public void synthesizeBean(SyntheticComponents synth) {
        synth.addBean(TestBean.class)
             .type(TestBean.class)
             .scope(ApplicationScoped.class)
             .createWith(SyntheticCreator.class);
    }

    public static class SyntheticCreator implements SyntheticBeanCreator<TestBean> {

        @Override
        public TestBean create(Instance<Object> lookup, Parameters params) {
            return () -> "synthetic";
        }

    }

    public void observeStartup(@Observes Startup startupEvent) {
        new Exception().printStackTrace();
    }

    public void observeShutdown(@Observes Shutdown shutdownEvent) {
        new Exception().printStackTrace();
    }

    public void observeInit(@Observes @Initialized(ApplicationScoped.class) Object event) {
        new Exception().printStackTrace();
    }

}
