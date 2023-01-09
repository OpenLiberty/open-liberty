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
package org.gjwatts.liberty;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.gjwatts.liberty.Child;
import org.gjwatts.liberty.SealedClassTest;

@Path("/")
@ApplicationScoped
public class TestService {

    @Resource
    UserTransaction tx;

    @PersistenceContext(unitName = "JPAPU")
    private EntityManager em;

    @Inject
    SampleBean bean;

    @GET
    public String test() {
        try {
            log(">>> ENTER");
            doTest();
            log("<<< EXIT SUCCESSFUL");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            e.printStackTrace(new PrintWriter(sb));
            log("<<< EXIT FAILED");
        }
        String result = sb.toString();
        sb = new StringWriter();
        return result;
    }

    private void doTest() throws Exception {
        log("Hello world");

        assertEquals("Hello world!", bean.sayHello());

        testJPA();
        testSwitchExpressions();

        String result = EdDSATest.test();
        log(result);
        assertEquals("Successfully created an EdDSA KeyFactory", result);

        result = SealedClassTest.test();
        log(result);
        assertEquals("Hello from the child", result);

        log("Text block literal is:");
        log(query);

        // Java 16 specific test - Pattern matching for instanceof (JEPS 394) -> https://openjdk.java.net/jeps/394
        // allows assignment variable during instanceof check for immediate use afterwords without casting 
        Object obj = "Basketballs bounce busily";

        if (obj instanceof String s) {
            log(s);
            assertEquals("Basketballs bounce busily", s);
        } else {
            throw new Exception("instanceof test failed");
        }

        // Java 16 specific test - Records (JEPS 395) -> https://openjdk.java.net/jeps/395
        result = RecordTest.test();
        log(result);
        assertEquals("Minnesota has a population of 5640000, was founded in 1858 and the capital is St. Paul.  The best city is Rochester.", result);

        log("Goodbye world");
    }

    public static enum DAY {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    private void testSwitchExpressions() {
        DAY day = DAY.valueOf("MONDAY");
        switch (day) {
            case MONDAY, FRIDAY, SUNDAY -> log("Case A");
            case TUESDAY -> log("Case B");
            case THURSDAY, SATURDAY -> log("Case C");
            case WEDNESDAY -> log("Case D");
        }
        log("testSwitchExpressions COMPLETE");
    }

    String query = """
                   SELECT `EMP_ID`, `LAST_NAME` FROM `EMPLOYEE_TB`
                   WHERE `CITY` = 'INDIANAPOLIS'
    ORDER BY`EMP_ID`,`LAST_NAME`;""";

    private StringWriter sb = new StringWriter();

    private void log(String msg) {
        System.out.println(msg);
        sb.append(msg);
        sb.append("<br/>");
    }

    public void testJPA() throws Exception {
        tx.begin();
        Book b = new Book();
        b.author = "Bob";
        b.id = 1;
        b.pages = 100;
        b.title = "The Joy of Painting";
        em.persist(b);
        tx.commit();

        em.clear();
        Book findEntity = em.find(Book.class, b.id);
        if (b == findEntity)
            throw new RuntimeException("Instance found from EntityManger should not be same instance that was persisted");
        assertEquals(b.id, findEntity.id);
        assertEquals(b.title, findEntity.title);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual))
            throw new RuntimeException("Expected <" + expected + "> but instead got <" + actual + ">");
    }

}
