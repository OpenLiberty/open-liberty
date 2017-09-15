/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config;

import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public abstract class ManagedTest extends Test implements ManagedService {

    public ManagedTest(String name) {
        super(name);
    }

    @Override
    public String[] getServiceClasses() {
        return new String[] { ManagedService.class.getName() };
    }

    @Override
    public void updated(Dictionary properties) throws ConfigurationException {
        try {
            configurationUpdated(properties);
        } catch (Throwable e) {
            exception = e;
        } finally {
            latch.countDown();
        }
    }

    public abstract void configurationUpdated(Dictionary properties) throws Exception;

}
