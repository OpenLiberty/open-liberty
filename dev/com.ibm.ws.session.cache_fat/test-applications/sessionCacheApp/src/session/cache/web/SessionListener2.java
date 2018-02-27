/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package session.cache.web;

import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

@WebListener
public class SessionListener2 implements HttpSessionListener {
    public static final LinkedBlockingQueue<String> created = new LinkedBlockingQueue<String>();
    public static final LinkedBlockingQueue<String> destroyed = new LinkedBlockingQueue<String>();

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        created.add(event.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        destroyed.add(event.getSession().getId());
    }
}
