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
package sessionListener;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 *
 */
public class MYSessionListener implements HttpSessionListener {

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpSessionListener#sessionCreated(javax.servlet.http.HttpSessionEvent)
     */
    @Override
    public void sessionCreated(HttpSessionEvent event) {
        System.out.println("MYSessionListener sessionCreated");
        Thread.dumpStack();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpSessionListener#sessionDestroyed(javax.servlet.http.HttpSessionEvent)
     */
    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        System.out.println("MYSessionListener sessionDestroyed");
        Thread.dumpStack();
//        Enumeration<String> names = event.getSession().getAttributeNames();
//        while (names.hasMoreElements()) {
//            System.out.println("MYSessionListener addAttrValueOnDestroy");
//            CalculateListenerInvoke.addAttrValueOnDestroy(event.getSession().getAttribute(names.nextElement()));
//        }
    }

}
