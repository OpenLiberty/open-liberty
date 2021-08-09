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

package jpadds.web.jpa;

import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.TypedQuery;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import jpadds.entity.DefDSEntity;

@WebServlet(urlPatterns = { "/JPARLServlet" })
public class JPARLServlet extends HttpServlet {
    @Resource
    private UserTransaction tx;

    @PersistenceUnit(unitName = "jpa-rl")
    private EntityManagerFactory emf;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        execRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        execRequest(req, resp);
    }

    private void execRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String testParm = req.getParameter("targetId");
        final String testName = req.getParameter("testName");
        System.out.println("Enter JPARLServlet.execRequest, testName=" + testName + ", targetId = " + testParm);

        EntityManager em = emf.createEntityManager();
        try {
            int id = Integer.valueOf(testParm);
            String jpql = "SELECT e FROM DefDSEntity e";

            TypedQuery<DefDSEntity> query = em.createQuery(jpql, DefDSEntity.class);
            List<DefDSEntity> rs = query.getResultList();
            if (rs == null) {
                throw new ServletException("rs is null.");
            }
            if (rs.size() != 1) {
                throw new ServletException("rs.size() == " + rs.size());
            }
            DefDSEntity entity = rs.get(0);
            if (entity.getId() != id) {
                throw new ServletException("rs.getId() == " + entity.getId() + " not " + id);
            }
        } catch (ServletException se) {
            throw se;
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            em.close();
        }

        System.out.println("TEST GOOD.");
        resp.getOutputStream().println("TEST GOOD.");
    }
}
