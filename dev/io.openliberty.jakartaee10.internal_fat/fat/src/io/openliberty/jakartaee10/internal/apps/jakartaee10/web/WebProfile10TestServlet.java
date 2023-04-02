/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package io.openliberty.jakartaee10.internal.apps.jakartaee10.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.jakartaee10.internal.apps.jakartaee10.web.cdi.BasicCDIBean;
import io.openliberty.jakartaee10.internal.apps.jakartaee10.web.jpa.UserEntity;
import io.openliberty.jakartaee10.internal.apps.jakartaee10.web.jsonb.Team;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.spi.JsonbProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.UserTransaction;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

@SuppressWarnings("serial")
@WebServlet("/WebProfile10TestServlet")
public class WebProfile10TestServlet extends FATServlet {

    @Inject
    BasicCDIBean basicBean;

    @PersistenceContext(unitName = "JPAPU")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Test
    public void testCDI() throws Exception {
        assertEquals(BasicCDIBean.MSG, basicBean.sayHi());
    }

    @Test
    public void testJAXRS(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        try {
            String url = "http://" + req.getServerName() + ':' + req.getServerPort() + "/webProfile10App/rest/test";
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
    public void testJPAandJSONB() throws Exception {
        Jsonb jsonb = JsonbBuilder.create();

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

        String entityJSON = jsonb.toJson(findEntity);
        System.out.println("JPA POJO --> JSON: " + entityJSON);
        assertTrue("JPA entity converted to JSON did not contain expected data: " + entityJSON, entityJSON.contains("\"id\":"));// ID value is generated
        assertTrue("JPA entity converted to JSON did not contain expected data: " + entityJSON, entityJSON.contains("\"strData\":\"Foo Bar\""));
        assertTrue("JPA entity converted to JSON did not contain expected data: " + entityJSON, entityJSON.contains("\"version\":")); // version number is generated
        assertTrue("JPA entity converted to JSON was not the expected length: " + entityJSON, 40 <= entityJSON.length() && entityJSON.length() <= 46);
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