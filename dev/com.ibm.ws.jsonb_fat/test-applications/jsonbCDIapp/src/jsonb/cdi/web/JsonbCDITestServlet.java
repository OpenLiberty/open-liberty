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
package jsonb.cdi.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/JsonbCDITestServlet")
public class JsonbCDITestServlet extends FATServlet {

    @Inject
    CDIBean bean;

    @Inject
    Jsonb jsonb;

    @Inject
    Jsonb jsonb2;

    @Test
    public void testReuseInjectedJsonb() {
        assertNotNull(jsonb);
        assertNotNull(jsonb2);
        assertNotNull(bean.getJsonb());
        assertEquals("Two Jsonb injection points should inject the same object instance", jsonb, jsonb2);
        assertEquals("Two Jsonb injection points should inject the same object instance", jsonb, bean.getJsonb());
    }

    @Test
    public void testJsonbDemo() throws Exception {
        // Convert Java Object --> JSON
        Team zombies = new Team();
        zombies.name = "Zombies";
        zombies.size = 9;
        zombies.winLossRatio = 0.85f;
        String teamJson = jsonb.toJson(zombies);
        System.out.println(teamJson);
        assertTrue(teamJson.contains("\"name\":\"Zombies\""));
        assertTrue(teamJson.contains("\"size\":9"));
        assertTrue(teamJson.contains("\"winLossRatio\":0.8"));

        // Convert JSON --> Java Object
        Team rangers = jsonb.fromJson("{\"name\":\"Rangers\",\"size\":7,\"winLossRatio\":0.6}", Team.class);
        System.out.println(rangers.name);
        assertEquals("Rangers", rangers.name);
        assertEquals(7, rangers.size);
        assertEquals(0.6f, rangers.winLossRatio, 0.01f);
    }
}
