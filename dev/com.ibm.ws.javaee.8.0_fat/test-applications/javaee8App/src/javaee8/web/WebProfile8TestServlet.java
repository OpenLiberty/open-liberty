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
package javaee8.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.spi.JsonbProvider;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.Test;

import componenttest.app.FATServlet;
import javaee8.web.cdi.BasicCDIBean;
import javaee8.web.jpa.UserEntity;
import javaee8.web.jsonb.Team;

@SuppressWarnings("serial")
@WebServlet("/WebProfile8TestServlet")
public class WebProfile8TestServlet extends FATServlet {

    @Inject
    BasicCDIBean basicBean;

    @PersistenceContext(unitName = "JPAPU")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Test
    public void testCDI() throws Exception {
        assertEquals("Hello world", basicBean.sayHi());
    }

    @Test
    public void testJAXRS(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        try {
            String url = "http://" + req.getServerName() + ':' + req.getServerPort() + "/javaee8App/rest/test";
            System.out.println("Making a PATCH request to URL: " + url);
            String result = client.target(url)
                            .request()
                            .method("PATCH", String.class);
            assertEquals("patch-success", result);
        } finally {
            client.close();
        }
    }

    @Test
    public void testJsonb() throws Exception {
        // Convert Java Object --> JSON
        Team zombies = new Team();
        zombies.name = "Zombies";
        zombies.size = 9;
        zombies.winLossRatio = 0.85f;
        Jsonb jsonb = JsonbProvider.provider().create().build();
        String teamJson = jsonb.toJson(zombies);
        System.out.println("POJO --> JSON: " + teamJson);
        assertTrue(teamJson.contains("\"name\":\"Zombies\""));
        assertTrue(teamJson.contains("\"size\":9"));
        assertTrue(teamJson.contains("\"winLossRatio\":0.8"));

        // Convert JSON --> Java Object
        Team rangers = jsonb.fromJson("{\"name\":\"Rangers\",\"size\":7,\"winLossRatio\":0.6}", Team.class);
        System.out.println("JSON --> POJO: " + rangers.name);
        assertEquals("Rangers", rangers.name);
        assertEquals(7, rangers.size);
        assertEquals(0.6f, rangers.winLossRatio, 0.01f);
    }

    @Test
    public void testJPA() throws Exception {
        tx.begin();
        UserEntity entity = new UserEntity("Foo Bar");
        em.persist(entity);
        tx.commit();

        em.clear();
        UserEntity findEntity = em.find(UserEntity.class, entity.id);
        assertNotNull(findEntity);
        assertNotSame(entity, findEntity);
        assertEquals(entity.id, findEntity.id);
        assertEquals("Foo Bar", findEntity.strData);
    }

    @Test
    public void testJavaEEDefaultResource() throws Exception {
        DataSource ds = InitialContext.doLookup("java:comp/DefaultDataSource");
        try (Connection con = ds.getConnection()) {
            System.out.println("Got JDBC connection from: " + con.getMetaData().getDatabaseProductName());
        }
    }

}