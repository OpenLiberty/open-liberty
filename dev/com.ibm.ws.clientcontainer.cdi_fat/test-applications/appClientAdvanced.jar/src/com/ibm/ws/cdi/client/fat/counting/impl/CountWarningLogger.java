/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.client.fat.counting.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import com.ibm.ws.cdi.client.fat.counting.CountWarning;

/**
 * A bean that watches for CountWarning events and prints to stdout.
 */
@ApplicationScoped
public class CountWarningLogger {

    public void printWarning(@Observes CountWarning warning) {
        System.out.println("Warning: " + warning.getCount() + " countable methods have been executed");
    }

}
