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

package cdi.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Assert;
import org.junit.Test;

import cdi.model.ConvertableWidget;
import cdi.model.LoggingService;
import cdi.model.Widget;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/eli" })
public class ELIServlet extends FATServlet {
    private static final String CLASS_NAME = ELIServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private boolean firstTest = true;

    @Inject // used for checking callbacks to entity listener
    private LoggingService logger;

    @PersistenceContext
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    /**
     * Insert a JPA entity into the database. Verifies:
     * 1) Injection occurred in the entity listener
     * 2) PrePersist method on the entity listener was invoked
     * 3) PostPersist method was invoked
     * 4) Order of invocation is: Injection, then PrePersist, then PostPersist.
     */
    @Test
    public void testInjectionOccursBeforePostConstructAndInsertionCallbacks() throws Exception {
        List<String> loggerMessages = null;
        em.clear();

        insert("circle", "A round widget");

        List<String> searchMessages = createMessageList(new String[] { "prePersist", "postPersist" });
        boolean[] foundMessages = new boolean[searchMessages.size()];
        boolean[] orderCorrect = new boolean[searchMessages.size() - 1];

        loggerMessages = logger.getAndClearMessages();
        for (String msg : loggerMessages) {
            if ("injection failed".equalsIgnoreCase(msg)) {
                Assert.fail("Injection failed to occur before some callback method");
            }

            for (int index = 0; index < searchMessages.size(); index++) {
                if (msg.indexOf(searchMessages.get(index)) >= 0) {
                    foundMessages[index] = true;
                }

                if (index > 0) {
                    if (foundMessages[index] == true && foundMessages[index - 1] == true) {
                        orderCorrect[index - 1] = true;
                    }
                }
            }
        }

        for (int index = 0; index < searchMessages.size(); index++) {
            Assert.assertTrue("Failed to find: \"" + searchMessages.get(index) + "\"", foundMessages[index]);
        }
        for (int index = 0; index < orderCorrect.length; index++) {
            Assert.assertTrue("Callback order incorrect at index: " + index, orderCorrect[index]);
        }
    }

    @Test
    public void testInjectionOccursBeforeInsertAndFindCallbacks() throws Exception {
        List<String> loggerMessages = null;
        em.clear();

        final int id = insert("square", "A widget with four equal sides");

        List<String> searchMessages = createMessageList(new String[] { "prePersist", "postPersist" });
        boolean[] foundMessages = new boolean[searchMessages.size()];
        boolean[] orderCorrect = new boolean[searchMessages.size() - 1];

        loggerMessages = logger.getAndClearMessages();
        for (String msg : loggerMessages) {
            if ("injection failed".equalsIgnoreCase(msg)) {
                Assert.fail("Injection failed to occur before some callback method");
            }

            for (int index = 0; index < searchMessages.size(); index++) {
                if (msg.indexOf(searchMessages.get(index)) >= 0) {
                    foundMessages[index] = true;
                }

                if (index > 0) {
                    if (foundMessages[index] == true && foundMessages[index - 1] == true) {
                        orderCorrect[index - 1] = true;
                    }
                }
            }
        }

        for (int index = 0; index < searchMessages.size(); index++) {
            Assert.assertTrue("Failed to find: \"" + searchMessages.get(index) + "\"", foundMessages[index]);
        }
        for (int index = 0; index < orderCorrect.length; index++) {
            Assert.assertTrue("Callback order incorrect at index: " + index, orderCorrect[index]);
        }

        em.clear();

        find(id);
        searchMessages = createMessageList(new String[] { "postLoad" });
        foundMessages = new boolean[searchMessages.size()];

        loggerMessages = logger.getAndClearMessages();
        for (String msg : loggerMessages) {
            for (int index = 0; index < searchMessages.size(); index++) {
                if (msg.indexOf(searchMessages.get(index)) >= 0) {
                    foundMessages[index] = true;
                }
            }
        }

        for (int index = 0; index < searchMessages.size(); index++) {
            Assert.assertTrue("Failed to find: \"" + searchMessages.get(index) + "\"", foundMessages[index]);
        }
    }

    @Test
    public void testInjectionOccursBeforeInsertAndUpdateCallbacks() throws Exception {
        List<String> loggerMessages = null;
        em.clear();

        final int id = insert("rectangle", "A widget with two long sides and two short sides");

        List<String> searchMessages = createMessageList(new String[] { "prePersist", "postPersist" });
        boolean[] foundMessages = new boolean[searchMessages.size()];
        boolean[] orderCorrect = new boolean[searchMessages.size() - 1];

        loggerMessages = logger.getAndClearMessages();
        for (String msg : loggerMessages) {
            if ("injection failed".equalsIgnoreCase(msg)) {
                Assert.fail("Injection failed to occur before some callback method");
            }

            for (int index = 0; index < searchMessages.size(); index++) {
                if (msg.indexOf(searchMessages.get(index)) >= 0) {
                    foundMessages[index] = true;
                }

                if (index > 0) {
                    if (foundMessages[index] == true && foundMessages[index - 1] == true) {
                        orderCorrect[index - 1] = true;
                    }
                }
            }
        }

        for (int index = 0; index < searchMessages.size(); index++) {
            Assert.assertTrue("Failed to find: \"" + searchMessages.get(index) + "\"", foundMessages[index]);
        }
        for (int index = 0; index < orderCorrect.length; index++) {
            Assert.assertTrue("Callback order incorrect at index: " + index, orderCorrect[index]);
        }

        em.clear();

        update(id, "rectangle", "A widget with two pairs of two equal sides");
        searchMessages = createMessageList(new String[] { "preUpdate", "postUpdate" });
        foundMessages = new boolean[searchMessages.size()];
        orderCorrect = new boolean[searchMessages.size() - 1];

        loggerMessages = logger.getAndClearMessages();
        for (String msg : loggerMessages) {
            for (int index = 0; index < searchMessages.size(); index++) {
                if (msg.indexOf(searchMessages.get(index)) >= 0) {
                    foundMessages[index] = true;
                }

                if (index > 0) {
                    if (foundMessages[index] == true && foundMessages[index - 1] == true) {
                        orderCorrect[index - 1] = true;
                    }
                }
            }
        }

        for (int index = 0; index < searchMessages.size(); index++) {
            Assert.assertTrue("Failed to find: \"" + searchMessages.get(index) + "\"", foundMessages[index]);
        }
        for (int index = 0; index < orderCorrect.length; index++) {
            Assert.assertTrue("Callback order incorrect at index: " + index, orderCorrect[index]);
        }
    }

    @Test
    public void testInjectionOccursBeforeInsertAndRemoveCallbacks() throws Exception {
        List<String> loggerMessages = null;
        em.clear();

        final int id = insert("triangle", "A widget with three sides");

        List<String> searchMessages = createMessageList(new String[] { "prePersist", "postPersist" });
        boolean[] foundMessages = new boolean[searchMessages.size()];
        boolean[] orderCorrect = new boolean[searchMessages.size() - 1];

        loggerMessages = logger.getAndClearMessages();
        for (String msg : loggerMessages) {
            if ("injection failed".equalsIgnoreCase(msg)) {
                Assert.fail("Injection failed to occur before some callback method");
            }

            for (int index = 0; index < searchMessages.size(); index++) {
                if (msg.indexOf(searchMessages.get(index)) >= 0) {
                    foundMessages[index] = true;
                }

                if (index > 0) {
                    if (foundMessages[index] == true && foundMessages[index - 1] == true) {
                        orderCorrect[index - 1] = true;
                    }
                }
            }
        }

        for (int index = 0; index < searchMessages.size(); index++) {
            Assert.assertTrue("Failed to find: \"" + searchMessages.get(index) + "\"", foundMessages[index]);
        }
        for (int index = 0; index < orderCorrect.length; index++) {
            Assert.assertTrue("Callback order incorrect at index: " + index, orderCorrect[index]);
        }

        em.clear();

        delete(id);
        searchMessages = createMessageList(new String[] { "preRemove", "postRemove" });
        foundMessages = new boolean[searchMessages.size()];
        orderCorrect = new boolean[searchMessages.size() - 1];

        loggerMessages = logger.getAndClearMessages();
        for (String msg : loggerMessages) {
            for (int index = 0; index < searchMessages.size(); index++) {
                if (msg.indexOf(searchMessages.get(index)) >= 0) {
                    foundMessages[index] = true;
                }

                if (index > 0) {
                    if (foundMessages[index] == true && foundMessages[index - 1] == true) {
                        orderCorrect[index - 1] = true;
                    }
                }
            }
        }

        for (int index = 0; index < searchMessages.size(); index++) {
            Assert.assertTrue("Failed to find: \"" + searchMessages.get(index) + "\"", foundMessages[index]);
        }
        for (int index = 0; index < orderCorrect.length; index++) {
            Assert.assertTrue("Callback order incorrect at index: " + index, orderCorrect[index]);
        }
    }

    @Test
    public void testConverterCDIInjection() throws Exception {
        List<String> loggerMessages = null;
        em.clear();

        tx.begin();
        ConvertableWidget newEntity = new ConvertableWidget();
        newEntity.setDescription("A widget with 20 sides.");
        newEntity.setName("Icosahedron");
        newEntity.setNumberOfSides(20);
        em.persist(newEntity);
        tx.commit();

        List<String> searchMessages = createMessageList(new String[] { "convertToDatabaseColumn" });
        boolean[] foundMessages = new boolean[searchMessages.size()];
        loggerMessages = logger.getAndClearMessages();

        for (String msg : loggerMessages) {
            if ("injection failed".equalsIgnoreCase(msg)) {
                Assert.fail("Injection failed to occur before some callback method");
            }

            for (int index = 0; index < searchMessages.size(); index++) {
                if (msg.indexOf(searchMessages.get(index)) >= 0) {
                    foundMessages[index] = true;
                }
            }
        }

        for (int index = 0; index < searchMessages.size(); index++) {
            Assert.assertTrue("Failed to find: \"" + searchMessages.get(index) + "\"", foundMessages[index]);
        }

        em.clear();

        ConvertableWidget findEntity = em.find(ConvertableWidget.class, newEntity.getId());
        Assert.assertNotNull(findEntity);
        Assert.assertEquals(20, findEntity.getNumberOfSides());
        Assert.assertEquals("Icosahedron", findEntity.getName());
        Assert.assertEquals("A widget with 20 sides.", findEntity.getDescription());

        searchMessages = createMessageList(new String[] { "convertToEntityAttribute" });
        foundMessages = new boolean[searchMessages.size()];
        loggerMessages = logger.getAndClearMessages();

        for (String msg : loggerMessages) {
            if ("injection failed".equalsIgnoreCase(msg)) {
                Assert.fail("Injection failed to occur before some callback method");
            }

            for (int index = 0; index < searchMessages.size(); index++) {
                if (msg.indexOf(searchMessages.get(index)) >= 0) {
                    foundMessages[index] = true;
                }
            }
        }

        for (int index = 0; index < searchMessages.size(); index++) {
            Assert.assertTrue("Failed to find: \"" + searchMessages.get(index) + "\"", foundMessages[index]);
        }
    }

    // Support methods

    private List<String> createMessageList(String[] arr) {
        List<String> list = new ArrayList<String>(arr.length);

        if (firstTest) {
            list.add("injection");
            list.add("postConstruct");
            firstTest = false;
        }

        list.addAll(Arrays.asList(arr));

        return list;
    }

    private int insert(String name, String description) throws Exception {
        svLogger.logp(Level.INFO, CLASS_NAME, "insert", "enter name=" + name + ", desc=" + description);
        Widget w = null;
        try {
            tx.begin();
            w = new Widget();
            w.setName(name);
            w.setDescription(description);
            em.persist(w);
            svLogger.logp(Level.INFO, CLASS_NAME, "insert", "persisted " + w);
        } finally {
            tx.commit();
            svLogger.logp(Level.INFO, CLASS_NAME, "insert", "exit");
        }
        return w.getId();
    }

    private void update(int id, String name, String description) throws Exception {
        svLogger.logp(Level.INFO, CLASS_NAME, "update", "enter id=" + id + ", name=" + name + ", desc=" + description);
        try {
            tx.begin();

            Widget w = em.find(Widget.class, id);
            svLogger.logp(Level.INFO, CLASS_NAME, "update", "found " + w);
            w.setName(name);
            w.setDescription(description);
            em.persist(w);
            svLogger.logp(Level.INFO, CLASS_NAME, "update", "updated to " + w);
        } finally {
            tx.commit();
            svLogger.logp(Level.INFO, CLASS_NAME, "update", "exit");
        }
    }

    private void delete(int id) throws Exception {
        svLogger.logp(Level.INFO, CLASS_NAME, "delete", "enter id=" + id);
        try {
            tx.begin();
            Widget w = em.find(Widget.class, id);
            svLogger.logp(Level.INFO, CLASS_NAME, "delete", "found " + w);
            em.remove(w);
            svLogger.logp(Level.INFO, CLASS_NAME, "delete", "deleted " + w);
        } finally {
            tx.commit();
            svLogger.logp(Level.INFO, CLASS_NAME, "delete", "exit");
        }
    }

    private void find(int id) throws Exception {
        svLogger.logp(Level.INFO, CLASS_NAME, "find", "enter id=" + id);
        try {
            tx.begin();
            Widget w = em.find(Widget.class, id);
            svLogger.logp(Level.INFO, CLASS_NAME, "find", "found " + w);
        } finally {
            tx.commit();
            svLogger.logp(Level.INFO, CLASS_NAME, "find", "exit");
        }
    }

    private void clearAll() throws Exception {
        svLogger.logp(Level.INFO, CLASS_NAME, "clearAll", "enter");
        try {
            tx.begin();
            Query q = em.createNativeQuery("delete from Widget");
            int rows = q.executeUpdate();
            svLogger.logp(Level.INFO, CLASS_NAME, "clearAll", "deleted " + rows + " rows");
        } finally {
            tx.commit();
            svLogger.logp(Level.INFO, CLASS_NAME, "clearAll", "exit");
        }
    }

    @Override
    public void init() throws ServletException {
        checkForTransformer();
    }

    private void checkForTransformer() {
        Class<?>[] interfaces = Widget.class.getInterfaces();
        for (Class<?> i : interfaces) {
            if (i.getName().contains("eclipse")) {
                return;
            }
        }
        throw new RuntimeException("Entity class " + Widget.class.getName() + " should implement more than zero "
                                   + "EclipseLink interfaces. Most likely a transformer problem! Found : "
                                   + Arrays.toString(interfaces));
    }
}
