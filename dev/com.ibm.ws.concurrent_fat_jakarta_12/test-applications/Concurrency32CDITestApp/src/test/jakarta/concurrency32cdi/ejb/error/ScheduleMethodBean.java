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
package test.jakarta.concurrency32cdi.ejb.error;

import jakarta.ejb.Stateless;
import jakarta.enterprise.concurrent.Schedule;

/**
 * This EJB has a method annotated with the Jakarta Concurrency @Schedule annotation.
 * This annotation is only allowed on CDI bean methods, not EJB methods.
 * This application should not be installed.
 */
@Stateless
public class ScheduleMethodBean {

    @Schedule(cron = "* * * * * *")
    public void scheduledTask() {
        System.out.println("This EJB method should never run.");
    }
}
