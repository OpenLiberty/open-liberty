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

package io.openliberty.ejbcontainer.fat.singleton.quiesce.war;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.Singleton;

@Singleton(name = "SingletonQuiesceDefault")
public class SingletonQuiesceDefaultBean implements SingletonQuiesce {

    @PostConstruct
    public void postConstruct() {
        System.out.println("PostConstruct:SingletonQuiesce:SingletonQuiesceDefault:");
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("PreDestroy:SingletonQuiesce:SingletonQuiesceDefault:");
    }

    public String getName() {
        return "SingletonQuiesce:SingletonQuiesceDefault";
    }
}
