/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.jaxws.cdi.beans;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;

/**
 *
 */
@Dependent
public class Teacher {

    @PostConstruct
    public void postConstruct() {
        System.out.println("Teacher's post constructor called");

    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("Teacher's pre destroy called");
    }

    public String talk() {
        return "I'm teacher.";
    }

}
