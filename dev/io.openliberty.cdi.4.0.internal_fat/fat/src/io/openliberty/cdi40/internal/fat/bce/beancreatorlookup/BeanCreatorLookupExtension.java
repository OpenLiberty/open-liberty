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
package io.openliberty.cdi40.internal.fat.bce.beancreatorlookup;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;

public class BeanCreatorLookupExtension implements BuildCompatibleExtension {

    @Synthesis
    public void synthesizeBean(SyntheticComponents synth) {
        synth.addBean(SyntheticBean.class)
             .type(SyntheticBean.class)
             .scope(ApplicationScoped.class)
             .createWith(SyntheticCreator.class);
    }

    public static class SyntheticCreator implements SyntheticBeanCreator<SyntheticBean> {

        @Override
        public SyntheticBean create(Instance<Object> lookup, Parameters params) {

            // Attempt to lookup and call a bean using the Instance<Object>
            lookup.select(DependantBean.class).get().test();

            return () -> "synthetic";
        }

    }

}
