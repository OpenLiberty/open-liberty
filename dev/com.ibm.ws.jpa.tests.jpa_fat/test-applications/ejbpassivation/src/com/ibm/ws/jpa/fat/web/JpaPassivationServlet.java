/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.fat.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;

import org.junit.Test;

import com.ibm.ws.jpa.fat.data.PassivateEntity;
import com.ibm.ws.jpa.fat.data.SessionIdCache;
import com.ibm.ws.jpa.fat.ejb.StatefulSessionBeanLocal;

import componenttest.app.FATServlet;

/**
 *
 */
@SuppressWarnings("serial")
public class JpaPassivationServlet extends FATServlet {
    public static final String THIS_IS_JPA_SERVLET = "This is JpaPassivationServlet.";
    private final static String BeanName = "java:comp/env/ejb/StatefulSessionBean";

    @Test
    public void testBasicPassivation() throws Exception {
        SessionIdCache.clearAll();

        final InitialContext ic = new InitialContext();
        StatefulSessionBeanLocal sBean = (StatefulSessionBeanLocal) ic.lookup(BeanName);
        assertNotNull(sBean);

        final String beanId = sBean.getSessionId();
        assertTrue(SessionIdCache.sessionList.contains(beanId));
        SessionIdCache.removeActivatePassivate(beanId);

        final String description = "This is a test with beanId = " + beanId;
        final String mutatedDescription = "MUTATION:" + description;

        final PassivateEntity newEntity = sBean.newEntity(description);
        assertNotNull(newEntity);
        assertTrue(SessionIdCache.passivateList.contains(beanId));
        assertTrue(SessionIdCache.activateList.contains(beanId));
        SessionIdCache.removeActivatePassivate(beanId);

        final PassivateEntity findEntity = sBean.findEntity(newEntity.getId());
        assertNotNull(findEntity);
        assertNotSame(newEntity, findEntity);
        assertTrue(SessionIdCache.passivateList.contains(beanId));
        assertTrue(SessionIdCache.activateList.contains(beanId));
        SessionIdCache.removeActivatePassivate(beanId);

        findEntity.setDescription(mutatedDescription);
        final PassivateEntity updatedEntity = sBean.updateEntity(findEntity);
        assertNotNull(updatedEntity);
        assertNotSame(updatedEntity, findEntity);
        assertTrue(SessionIdCache.passivateList.contains(beanId));
        assertTrue(SessionIdCache.activateList.contains(beanId));
        SessionIdCache.removeActivatePassivate(beanId);

        sBean.removeEntity(updatedEntity);
        assertTrue(SessionIdCache.passivateList.contains(beanId));
        assertTrue(SessionIdCache.activateList.contains(beanId));

        sBean.remove();
//        writer.println("SUCCESS:testBasicPassivation");
    }
}
