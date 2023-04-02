/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.ws.cdi20.fat.apps.configurator.bean;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.configurator.BeanConfigurator;

public class AfterBeanDiscoveryObserver implements Extension {

    public void observer(@Observes AfterBeanDiscovery abd, BeanManager bm) {

        BeanConfigurator<Brick> brick = abd.addBean();
        brick.beanClass(Brick.class);
        brick.addType(Brick.class);
        brick.addQualifier(BuildingMaterial.BuildingMaterialLiteral.INSTANCE);
        brick.produceWith(obj -> new Brick());
    }
}