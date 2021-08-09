/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb;

import java.io.Serializable;

import javax.ejb.Timer;

public interface StatelessTimedLocalObject {
    /**
     * Utility method that may be used to create a Timer when a Timer is
     * required to perform a test, but cannot be created directly by
     * the bean performing the test. For example, if the bean performing
     * the test does not implement the TimedObject interface. <p>
     *
     * Local interface only! <p>
     *
     * Used by test : {@link TimerMDBOperationsTest#test01} <p>
     *
     * @param info info parameter passed through to the createTimer call
     *
     * @return Timer created with 1 minute duration and specified info.
     **/
    public Timer createTimer(Serializable info);
}
