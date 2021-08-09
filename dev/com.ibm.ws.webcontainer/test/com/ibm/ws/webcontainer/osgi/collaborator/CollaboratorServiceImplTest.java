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
package com.ibm.ws.webcontainer.osgi.collaborator;

import static org.junit.Assert.assertNotNull;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.wsspi.webcontainer.collaborator.IWebAppSecurityCollaborator;

public class CollaboratorServiceImplTest {

    private final Mockery context = new Mockery();
    final private ComponentContext cctx = context.mock(ComponentContext.class);
    final private ServiceReference<IWebAppSecurityCollaborator> serviceRef = context.mock(ServiceReference.class);
    // final private ServiceReference<IWebAppSecurityCollaborator> featureColaboratorServiceRef = context.mock(ServiceReference.class);
    final private IWebAppSecurityCollaborator collabType1 = context.mock(IWebAppSecurityCollaborator.class);

    @Test
    public void testOneCollab() throws Exception {
        String securityType1 = "com.ibm.type1";
        String securityType2 = "com.ibm.type2";
        CollaboratorServiceImpl cService = new CollaboratorServiceImpl();

        context.checking(new Expectations()
        {
            {
                one(serviceRef).getProperty("com.ibm.ws.security.type");
                will(returnValue("com.ibm.ws.feature"));
                one(serviceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                one(serviceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                one(cctx).locateService("webAppSecurityCollaborator", serviceRef);
                will(returnValue(collabType1));
            }
        });

        cService.setWebAppSecurityCollaborator(serviceRef);
        cService.activate(cctx);
        IWebAppSecurityCollaborator collab1 = cService.getWebAppSecurityCollaborator(securityType1);
        assertNotNull("collab type1 should not be null", collab1);

        context.checking(new Expectations()
        {
            {
                one(serviceRef).getProperty("com.ibm.ws.security.type");
                will(returnValue("com.ibm.type2"));
                one(serviceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                one(serviceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                one(cctx).locateService("webAppSecurityCollaborator", serviceRef);
                will(returnValue(collabType1));
            }
        });
        IWebAppSecurityCollaborator collab2 = cService.getWebAppSecurityCollaborator(securityType2);
        assertNotNull("collab type2 should be null", collab2);
    }
}
