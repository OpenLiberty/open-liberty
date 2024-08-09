/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.classloading.classpath.test.ejb3;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import io.openliberty.classloading.classpath.test.ejb2.InitBean2;


@Singleton
@Startup
@LocalBean
public class InitBean3 {
    private static final Logger logger = Logger.getLogger(InitBean3.class.getName());


    @PostConstruct
    public void initTest() {
        logger.info("--> initialing EBJ " + getClass());
    }
}
