/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

package io.openliberty.ejbcontainer.fat.singleton.quiesce.web;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import io.openliberty.ejbcontainer.fat.singleton.quiesce.ejb.SingletonQuiesce;

@Startup
@Singleton(name = "StartupSingletonQuiesceBnd")
@DependsOn("StartupSingletonQuiesceDD")
public class StartupSingletonQuiesceBndBean implements SingletonQuiesce {

    @PostConstruct
    public void postConstruct() {
        System.out.println("PostConstruct:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceBnd:");
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceBnd:");
    }

    @Override
    public String getName() {
        return "SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceBnd";
    }
}