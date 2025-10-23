/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.web.eclipselink;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

/**
 * A singleton startup EJB that is included inside the war file.
 */
@Startup
@Singleton
public class DataTestEJB {

    @Inject
    Animals animals;

    @PostConstruct
    public void init() {
        System.out.println("Singleton Startup EJB PostConstruct");
        animals.findAll();
    }
}
