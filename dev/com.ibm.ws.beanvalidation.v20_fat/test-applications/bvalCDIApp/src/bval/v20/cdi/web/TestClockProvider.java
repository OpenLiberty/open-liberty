/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package bval.v20.cdi.web;

import java.time.Clock;

import javax.inject.Inject;
import javax.validation.ClockProvider;

import org.junit.Assert;

public class TestClockProvider implements ClockProvider {

    @Inject
    BeanValCDIBean bean;

    @Override
    public Clock getClock() {
        // Verify that a CDI bean can be injected into a custom ClockProvider to
        // confirm that the custom clock provider was registered as a CDI managed object.
        System.out.println("Verifying that " + getClass() + " can have a CDI bean injected into it: " + bean);
        Assert.assertNotNull(bean);
        return Clock.systemUTC();
    }

}
