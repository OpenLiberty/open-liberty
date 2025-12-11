/*******************************************************************************
 * Copyright (c) 2014, 2025 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.v32.fat.passivation;

import java.util.logging.Logger;

import javax.annotation.PreDestroy;
import javax.ejb.PrePassivate;
import javax.ejb.Stateful;

@Stateful
public class PassivationCapableBean {
    private final transient Logger logger = Logger.getLogger(getClass().getName());

    @PrePassivate
    public void prePassivate() {
        logger.info("@PrePassivate: " + this);
    }

    @PreDestroy
    public void preDestroy() {
        logger.info("@PreDestroy: " + this);
    }
}
