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

package persistence_fat.consumer.web;

import java.io.StringWriter;
import java.util.Arrays;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.transaction.RollbackException;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;
import persistence_fat.consumer.ejb.PersonBean;
import persistence_fat.consumer.model.Person;
import persistence_fat.persistence.test.Consumer;
import persistence_fat.persistence.test.Consumer.DATABASE;

@WebServlet("/*")
@SuppressWarnings("serial")
public class ConsumerServlet extends FATServlet {
    @EJB
    PersonBean bean;

    @Resource(lookup = "consumerFactory")
    protected Consumer consumer;

    @Resource
    UserTransaction _ut;

    private static final int LOWER_CHAR = 0;
    private static final int UPPER_CHAR = 127;
    static {
        StringBuilder sb = new StringBuilder();
        for (int i = LOWER_CHAR; i < UPPER_CHAR; i++) {
            sb.append((char) i);
        }
        ALL_VALID_CHARS = sb.toString();
    }
    private static final String ALL_VALID_CHARS;

    @Test
    public void countCars() throws Exception {
        consumer.createTables();
        long personId = testPersist();

        int cars = consumer.getNumCars(personId);
        if (10 != cars) {
            throw new Exception("Expected 10 cars for person.id:" + personId + ", but found " + cars);
        }
    }

    private long testPersist() throws Exception {
        _ut.begin();
        long personId = consumer.persistPerson(false, 10);
        _ut.commit();
        Person p = bean.findPersonById(personId);
        if (p == null) {
            throw new RuntimeException("didn't find person with id " + personId);
        } else {
            System.out.println("ConsumerServlet.testPersist found Person.id=" + +personId);
        }
        return personId;
    }

    @Test
    public void testPersistUnicodeFilteredStringBoundaries() throws Exception {
        consumer.updateUnicodeConfig(Boolean.FALSE);
        try {
            // lowest char -- valid
            String str = Character.valueOf(((char) (LOWER_CHAR))).toString();
            if (!persistString(str, null)) {
                throw new RuntimeException("Should have been able to persist a string with " + str);
            }
            str = Character.valueOf(((char) (UPPER_CHAR))).toString();
            if (!persistString(str, null)) {
                throw new RuntimeException("Should have been able to persist a string with " + str);
            }
            str = Character.valueOf(((char) (UPPER_CHAR + 1))).toString();
            if (persistString(str, RollbackException.class)) {
                throw new RuntimeException("Shouldn't have been able to persist a string with " + str);
            }
            str = Character.valueOf(((char) (UPPER_CHAR - 1))).toString();
            if (!persistString(str, RollbackException.class)) {
                throw new RuntimeException("Should have been able to persist a string with " + str);
            }
        } finally {
            consumer.clearTempConfig();
        }
    }

    /**
     * This is arguably an invalid test. It will only work on platforms that support unicode.
     */
    private void testPersistUnicodeUnspecifiedFilter() throws Exception {
        consumer.updateUnicodeConfig(null);
        try {
            _ut.begin();
            String invalidInput = "\u0274\u0275";
            // 'Latin Letter Small Capital N'+'Latin Small Letter Barred O
            long personId = consumer.persistPerson(false, 10, invalidInput);
            _ut.commit();
        } finally {
            consumer.clearTempConfig();
        }
    }

    /**
     * This is arguably an invalid test. It will only work on platforms that support unicode.
     */
    @Test
    public void testPersistUnicodeNoFilter() throws Exception {
        consumer.updateUnicodeConfig(Boolean.TRUE);
        try {
            _ut.begin();
            String invalidInput = "\u0274\u0275";
            // 'Latin Letter Small Capital N'+'Latin Small Letter Barred O
            long personId = consumer.persistPerson(false, 10, invalidInput);
            _ut.commit();
        } finally {
            consumer.clearTempConfig();
        }
    }

    @Test // This will fail against databases that don't support unicode
    public void testPersistUnicodeFiltered() throws Exception {
        consumer.updateUnicodeConfig(Boolean.FALSE);
        try {
            _ut.begin();
            String invalidInput = "\u0274\u0275";
            // 'Latin Letter Small Capital N'+'Latin Small Letter Barred O
            try {
                long personId = consumer.persistPerson(false, 10, invalidInput);
                _ut.commit();
            } catch (RollbackException e) {
                // expected
                return;
            }
            throw new RuntimeException("Shouldn't have been able to persist unicode as it is explicitly disabled");
        } finally {
            consumer.clearTempConfig();
        }
    }

    @Test
    public void testPersistAllValidStrings() throws Exception {
        _ut.begin();
        long personId = consumer.persistPerson(false, 10, ALL_VALID_CHARS);
        _ut.commit();
        // Use jpa to find person, validate String data is expected
        Person p = bean.findPersonById(personId);
        if (p == null) {
            throw new RuntimeException("didn't find person with id " + personId);
        }
        String found = p.getData();
        if (!ALL_VALID_CHARS.equals(found)) {
            throw new RuntimeException("Expected person(" + personId + ") to have data(" + ALL_VALID_CHARS
                                       + "), but found (" + found + ").");
        }
    }

    private void testPersistNullString() throws Exception {
        _ut.begin();
        long personId = consumer.persistPerson(false, 10, null);
        _ut.commit();
        // Use jpa to find person, validate String data is expected
        Person p = bean.findPersonById(personId);
        if (p == null) {
            throw new RuntimeException("didn't find person with id " + personId);
        }
        String found = p.getData();
        if (found != null) {
            throw new RuntimeException("Expected person(" + personId + ") to have data(" + "null" + "), but found ("
                                       + found + ").");
        }
    }

    private void testPersistEmptyString() throws Exception {
        _ut.begin();
        long personId = consumer.persistPerson(false, 10, "");
        _ut.commit();
        // Use jpa to find person, validate String data is expected
        Person p = bean.findPersonById(personId);
        if (p == null) {
            throw new RuntimeException("didn't find person with id " + personId);
        }
        String found = p.getData();
        if (found != "") {
            throw new RuntimeException("Expected person(" + personId + ") to have data(" + "{empty}"
                                       + "), but found (" + found + ").");
        }
    }

    // http://unicode-table.com/en/#0275
    @Test
    public void testQueryInvalidStrig() {
        try {
            String str = "\u0274\u0275";
            consumer.queryPersonDataLiteral("\u0274\u0275");
            throw new RuntimeException("Shouldn't have been able to execute a query with " + str + " in it.");
        } catch (RuntimeException e) {
            // expected
        }

        try {
            String str = "\u0274\u0275";
            consumer.queryPersonDataParameter("\u0274\u0275");
            throw new RuntimeException("Shouldn't have been able to execute a query with " + str + " in it.");
        } catch (RuntimeException e) {
            // expected
        }
    }

    @Test
    public String testGetPersonName() throws Exception {
        _ut.begin();
        long personId = consumer.persistPerson(false, 10);
        _ut.commit();

        String[] psNames = consumer.getPersonName(personId);
        if (psNames[0] == null || psNames[1] == null) {
            throw new Exception("Found a null name. " + Arrays.toString(psNames));
        }
        if (!psNames[0].equals("first") || !psNames[1].equals("last")) {
            throw new Exception("expected first, last but found " + psNames[0] + ", " + psNames[1]);
        }
        Person jpaPerson = bean.findPersonById(personId);
        String[] jpaNames = new String[] { jpaPerson.getFirst(), jpaPerson.getLast() };

        if (!Arrays.equals(jpaNames, psNames)) {
            throw new Exception("Person names didn't match up. Expected " + Arrays.toString(jpaNames)
                                + " but found " + Arrays.toString(psNames));
        }
        return Arrays.toString(psNames);
    }

    // @Test -- this recreates defect 159874
    public void testSerializable() throws Exception {
        _ut.begin();

        Person p = new Person(77, "first", "last");
        // Pass an instance of our
        long personId = consumer.persistPerson(false, 10, " data", p);

        _ut.commit();

        Person found = (Person) consumer.getPersonSerializableData(personId);
        if (!found.equals(p)) {
            throw new Exception("Person Serializable didn't match up. Expected " + p + " but found " + found);
        }
    }

    @Test
    public void testGenerateDDL() throws Exception {
        StringWriter writer = new StringWriter();
        try {
            consumer.generateDDL(writer, DATABASE.DERBY);
            verifyDDL(writer, "");

            consumer.generateDDL(writer, DATABASE.JAVADB);
            verifyDDL(writer, "");

            writer = new StringWriter();
            consumer.generateDDL(writer, DATABASE.DB2);
            verifyDDL(writer, ";");

            writer = new StringWriter();
            consumer.generateDDL(writer, DATABASE.INFORMIX);
            verifyDDL(writer, ";");

            writer = new StringWriter();
            consumer.generateDDL(writer, DATABASE.SQLSERVER);
            verifyDDL(writer, ";");

            writer = new StringWriter();
            consumer.generateDDL(writer, DATABASE.ORACLE);
            verifyDDL(writer, ";");
        } finally {
            writer.close();
        }
    }

    private void verifyDDL(StringWriter writer, String lineTerminator) throws Exception {
        String[] ddl = writer.getBuffer().toString().split("\\n");
        String firstLine = ddl[0];
        if (firstLine == null || firstLine.equals("")) {
            throw new Exception("No DDL statements were generated");
        }

        if (!firstLine.startsWith("CREATE TABLE")) {
            throw new Exception("Missing create table statement");
        }

        for (String line : ddl) {
            line = line.trim();
            if (!line.endsWith(lineTerminator) && !line.isEmpty()) {
                System.err.println(Arrays.toString(ddl));
                throw new Exception("The DDL statements do not terminate with valid token " + lineTerminator
                                    + "\nline=" + line);
            }
        }
    }

    /**
     * Returns true if a Person is persisted successfully or false otherwise.
     */
    private boolean persistString(String str, Class<? extends Exception> expectedException) {
        try {
            _ut.begin();

            long personId = consumer.persistPerson(false, 10, str);

            _ut.commit();
        } catch (Exception e) {
            if (expectedException == null) {
                throw new RuntimeException(ConsumerServlet.class
                                           + ".persistString() - didn't expected an exception, but encountered ", e);
            }
            // Make sure we got the right exception
            if (expectedException.isAssignableFrom(e.getClass())
                || expectedException.isAssignableFrom(e.getCause().getClass())) {
                return false;
            }
            throw new RuntimeException(ConsumerServlet.class
                                       + ".persistString() - didn't get expected exception. Expected: " + expectedException, e);
        }
        return true;
    }

}
