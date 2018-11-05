/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.ejb;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Test;

/**
 * Unit tests for javax.ejb.ScheduleExpression serialization.
 */
public class ScheduleExpressionSerializationTest {
    private static final Date start = getStart();
    private static final Date end = getEnd();
    private static final String unittestFilePath = getUnittestFilePath();

    private static Date getStart() {
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("America/Chicago"));
        calendar.set(2018, 9, 24, 15, 35, 49);
        calendar.set(Calendar.MILLISECOND, 142);
        return calendar.getTime();
    }

    private static Date getEnd() {
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("America/Chicago"));
        calendar.set(2018, 10, 25, 15, 35, 20);
        calendar.set(Calendar.MILLISECOND, 27);
        return calendar.getTime();
    }

    /**
     * Tests serialization of the EJB 3.2 version of ScheduleExpression.
     *
     * Verify instances of ScheduleExpression may be serialized and deserialized
     * with default and specified state values.
     */
    @Test
    public void testSerializationEJB32() {
        // Test with default values
        ScheduleExpression se = new ScheduleExpression();
        byte[] bytes = serialize(se);
        // writeToFile("ScheduleExpressionDefaultBytes.txt", bytes);
        verifyEquals(se, deserialize(bytes));

        // Test with specific numeric values
        se.second(5).minute(1).hour(1).dayOfWeek(1).dayOfMonth(6).month(3).timezone("America/Chicago").start(start).end(end);
        bytes = serialize(se);
        // writeToFile("ScheduleExpressionNumericBytes.txt", bytes);
        verifyEquals(se, deserialize(bytes));

        // Test with specific String values
        se.second("10, 20").minute("2-8").hour("10").dayOfWeek("0-7").dayOfMonth("1st Sun-Last Sat").month("May").timezone("GMT-8").start(start).end(null);
        bytes = serialize(se);
        // writeToFile("ScheduleExpressionStringBytes.txt", bytes);
        verifyEquals(se, deserialize(bytes));
    }

    /**
     * Tests deserialization of the EJB 3.1 version of ScheduleExpression.
     *
     * Verify serialized bytes from the EJB 3.1 implementation of ScheduleExpression
     * may be deserialized using the EJB 3.2 implementation; with default and
     * specified state values.
     */
    @Test
    public void testSerializationEJB31() {
        // Test with default values
        ScheduleExpression se = new ScheduleExpression();
        byte[] bytes = readFromFile("ScheduleExpression31DefaultBytes.txt");
        verifyEquals(se, deserialize(bytes));

        // Test with specific numeric values
        se.second(5).minute(1).hour(1).dayOfWeek(1).dayOfMonth(6).month(3).timezone("America/Chicago").start(start).end(end);
        bytes = readFromFile("ScheduleExpression31NumericBytes.txt");
        verifyEquals(se, deserialize(bytes));

        // Test with specific String values
        se.second("10, 20").minute("2-8").hour("10").dayOfWeek("0-7").dayOfMonth("1st Sun-Last Sat").month("May").timezone("GMT-8").start(start).end(null);
        bytes = readFromFile("ScheduleExpression31StringBytes.txt");
        verifyEquals(se, deserialize(bytes));
    }

    private static byte[] serialize(Object obj) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try {
            ObjectOutputStream o = new ObjectOutputStream(b);
            o.writeObject(obj);
            o.flush();
        } catch (IOException ioex) {
            ioex.printStackTrace();
            throw new Error("Unexpected IOException : " + ioex);
        }
        return b.toByteArray();
    }

    private static ScheduleExpression deserialize(byte[] bytes) {
        ScheduleExpression se = null;
        try {
            ByteArrayInputStream b = new ByteArrayInputStream(bytes);
            ObjectInputStream o = new ObjectInputStream(b);
            se = (ScheduleExpression) o.readObject();
        } catch (IOException ioex) {
            ioex.printStackTrace();
            throw new Error("Unexpected IOException : " + ioex);
        } catch (ClassNotFoundException cnfex) {
            cnfex.printStackTrace();
            throw new Error("Unexpected ClassNotFoundException : " + cnfex);
        }
        return se;
    }

    // Used to write the bytes to a file; needed to create the EJB 3.1 version files.
    protected static void writeToFile(String fileName, byte[] bytes) {
        try {
            File file = new File(unittestFilePath + fileName);
            System.out.println(file.getAbsolutePath());
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static byte[] readFromFile(String fileName) {
        byte[] bytes = null;
        try {
            System.out.println(new File(unittestFilePath + fileName).toPath());
            bytes = Files.readAllBytes(new File(unittestFilePath + fileName).toPath());
        } catch (IOException ex1) {
            ex1.printStackTrace();
        }
        return bytes;
    }

    private static String getUnittestFilePath() {
        URL testURL = ScheduleExpressionSerializationTest.class.getResource(ScheduleExpressionSerializationTest.class.getSimpleName() + ".class");
        System.out.println(testURL.getPath());
        String testPath = testURL.getPath();
        String filePrefix = testPath.substring(0, testPath.lastIndexOf("ScheduleExpression")).concat("files/"); // testPath.substring(0, testPath.lastIndexOf("build")).concat("unittest/files/");
        System.out.println(filePrefix);
        return filePrefix;
    }

    private static void verifyEquals(ScheduleExpression se1, ScheduleExpression se2) {
        System.out.println("verifyEquals: " + se1 + " : " + se2);

        assertEquals("DayOfMonth", se1.getDayOfMonth(), se2.getDayOfMonth());
        assertEquals("DayOfWeek", se1.getDayOfWeek(), se2.getDayOfWeek());
        assertEquals("Hour", se1.getHour(), se2.getHour());
        assertEquals("Minute", se1.getMinute(), se2.getMinute());
        assertEquals("Month", se1.getMonth(), se2.getMonth());
        assertEquals("Second", se1.getSecond(), se2.getSecond());
        assertEquals("Timezone", se1.getTimezone(), se2.getTimezone());
        assertEquals("Year", se1.getYear(), se2.getYear());
        assertEquals("Start", se1.getStart(), se2.getStart());
        assertEquals("End", se1.getEnd(), se2.getEnd());
    }
}
