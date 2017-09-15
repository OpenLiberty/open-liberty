/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat.fallalloverthefloor.ibmfeature;

import java.util.Map;

/**
 *
 */
public class BrokenFeature {

    /**
     * A method which, after some indirection, throws an exception.
     */
    protected void activate(Map<String, Object> properties) throws Exception {
        thinkAboutThrowingAnException();

    }

    /**
     * @throws ConfigurationReceivedException
     */
    private void thinkAboutThrowingAnException() throws ConfigurationReceivedException {
        reallyThrowAnException();
    }

    /**
     * @throws ConfigurationReceivedException
     */
    private void reallyThrowAnException() throws ConfigurationReceivedException {
        System.out.println("The user feature is about to throw an exception.");
        throw new ConfigurationReceivedException();
    }

    protected static class ConfigurationReceivedException extends Exception {

        /**  */
        private static final long serialVersionUID = 1L;

    }
}
