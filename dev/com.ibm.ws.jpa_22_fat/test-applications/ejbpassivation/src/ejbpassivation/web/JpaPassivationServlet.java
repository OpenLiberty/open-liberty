/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package ejbpassivation.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import componenttest.app.FATServlet;
import ejbpassivation.data.PassivateEntity;
import ejbpassivation.data.SessionIdCache;
import ejbpassivation.ejb.StatefulSessionBeanLocal;
import ejbpassivation.ejb.StatefulSessionPUBeanLocal;

@SuppressWarnings("serial")
public class JpaPassivationServlet extends FATServlet {
    public static final String THIS_IS_JPA_SERVLET = "This is JpaPassivationServlet.";
    private final static String BeanName = "java:comp/env/ejb/StatefulSessionBean";
    private final static String PUBeanName = "java:comp/env/ejb/StatefulSessionPUBean";

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        writer.println(THIS_IS_JPA_SERVLET);

        String testMethod = request.getParameter("testMethod");
        try {
            if ("testBasicPassivation".equalsIgnoreCase(testMethod)) {
                testBasicPassivation(writer);
            } else if ("testBasicPUPassivation".equalsIgnoreCase(testMethod)) {
                testBasicPUPassivation(writer);
            }
        } catch (Exception e) {
            e.printStackTrace(writer);
            writer.println();
            writer.println("TEST FAILED!!!");
        }

    }

    private void testBasicPassivation(PrintWriter writer) throws Exception {
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
        writer.println("SUCCESS:testBasicPassivation");
    }

    private void testBasicPUPassivation(PrintWriter writer) throws Exception {
        SessionIdCache.clearAll();

        final InitialContext ic = new InitialContext();
        StatefulSessionPUBeanLocal sBean = (StatefulSessionPUBeanLocal) ic.lookup(PUBeanName);
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
        writer.println("SUCCESS:testBasicPUPassivation");
    }
}
