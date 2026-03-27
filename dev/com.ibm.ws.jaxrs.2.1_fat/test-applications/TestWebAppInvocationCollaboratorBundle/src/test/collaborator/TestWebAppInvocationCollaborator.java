/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

package test.collaborator;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.osgi.service.component.annotations.Component;

import com.ibm.wsspi.webcontainer.collaborator.WebAppInvocationCollaborator;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.servlet.ServletRequestExtended;

/**
 *
 */
@Component(service = { WebAppInvocationCollaborator.class },
           immediate = true,
           property = { "service.vendor=IBM" })
public class TestWebAppInvocationCollaborator implements WebAppInvocationCollaborator {

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.webcontainer.collaborator.WebAppInvocationCollaborator#preInvoke(com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData)
     */
    @Override
    public void preInvoke(WebComponentMetaData metaData) {
        System.out.println("TestWebAppInvocationCollaborator preInvoke (metaData ");


    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.webcontainer.collaborator.WebAppInvocationCollaborator#postInvoke(com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData)
     */
    @Override
    public void postInvoke(WebComponentMetaData metaData) {
        System.out.println("TestWebAppInvocationCollaborator postInvoke(metaData) ");


    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.webcontainer.collaborator.WebAppInvocationCollaborator#preInvoke(com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData, javax.servlet.ServletRequest,
     * javax.servlet.ServletResponse)
     */
    @Override
    public void preInvoke(WebComponentMetaData metaData, ServletRequest req, ServletResponse res) {
        System.out.println("TestWebAppInvocationCollaborator preInvoke ");


    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.webcontainer.collaborator.WebAppInvocationCollaborator#postInvoke(com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData, javax.servlet.ServletRequest,
     * javax.servlet.ServletResponse)
     */
    @Override
    public void postInvoke(WebComponentMetaData metaData, ServletRequest req, ServletResponse res) {
        System.out.println("TestWebAppInvocationCollaborator postInvoke ");
        Thread.dumpStack();
        
        Throwable currentException = ((ServletRequestExtended) req).getCurrentException();
        if (currentException != null) {
            System.out.println("TestWebAppInvocationCollaborator CurrentException = " + currentException);
        }

    }

}
