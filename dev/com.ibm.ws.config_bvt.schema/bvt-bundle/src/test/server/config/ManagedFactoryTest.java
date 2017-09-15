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
import org.osgi.service.cm.ManagedServiceFactory;

public abstract class ManagedFactoryTest extends Test implements ManagedServiceFactory {

    public ManagedFactoryTest(String name) {
        this(name, 1);
    }

    public ManagedFactoryTest(String name, int count) {
        super(name, count);
    }

    @Override
    public String[] getServiceClasses() {
        return new String[] { ManagedServiceFactory.class.getName() };
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        try {
            configurationUpdated(pid, properties);
        } catch (Throwable e) {
            exception = e;
        } finally {
            latch.countDown();
        }
    }

    public abstract void configurationUpdated(String pid, Dictionary<String, ?> properties) throws Exception;

    @Override
    public void deleted(String pid) {}

}
