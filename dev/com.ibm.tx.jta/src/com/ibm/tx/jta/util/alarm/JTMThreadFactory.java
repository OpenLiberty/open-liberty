package com.ibm.tx.jta.util.alarm;

/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * ThreadFactory for the JTM AlarmManager. Creates threads using a default
 * thread factory but makes them daemon threads so we don't prevent the JVM
 * from shutting down and so our shutdown hook will work.
 * 
 * It's way too early to be using trace in here btw.
 */
public class JTMThreadFactory implements ThreadFactory
{
    private final static ClassLoader THIS_CLASSLOADER = JTMThreadFactory.class.getClassLoader();
    private final ThreadFactory _factory = Executors.defaultThreadFactory();

    @Override
    public Thread newThread(final Runnable r)
    {
        return AccessController.doPrivileged(new PrivilegedAction<Thread>() {

            @Override
            public Thread run() {
                final Thread t = _factory.newThread(r);
                t.setDaemon(true);
                t.setContextClassLoader(THIS_CLASSLOADER);
                return t;
            }
        });
    }
}