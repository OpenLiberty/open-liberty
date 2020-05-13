/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jakarta.ejb;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

public class EJBExceptionTest {
    private static final EJBException exceptionDefaultConstructor = new EJBException();
    private static final EJBException exceptionWithNullMessage = new EJBException((String) null);
    private static final EJBException exceptionWithMessage = new EJBException("msg");

    private static final EJBException exceptionWithCausedBy = createException(true, false);
    private static final EJBException exceptionWithInitCause = createException(false, true);
    private static final EJBException exceptionWithCausedByAndInitCause = createException(true, true);
    private static final EJBException exceptionWithDifferentCausedByAndInitCause = (EJBException) createException(true, false).initCause(new RuntimeException("initCause"));

    private static EJBException createException(boolean causedBy, boolean initCause) {
        try {
            throw new Exception("cause");
        } catch (Exception ex) {
            EJBException ejbException = causedBy ? new EJBException("msg", ex) : new EJBException("msg");
            if (initCause) {
                ejbException.initCause(ex);
            }
            return ejbException;
        }
    }

    @Test
    public void testGetMessage() {
        Assert.assertEquals(null, exceptionDefaultConstructor.getMessage());
        Assert.assertEquals(null, exceptionWithNullMessage.getMessage());
        Assert.assertEquals("msg", exceptionWithMessage.getMessage());
        Assert.assertEquals("msg; nested exception is: java.lang.Exception: cause", exceptionWithCausedBy.getMessage());
        Assert.assertEquals("msg", exceptionWithInitCause.getMessage());
        Assert.assertEquals("msg; nested exception is: java.lang.Exception: cause", exceptionWithCausedByAndInitCause.getMessage());
        Assert.assertEquals("msg; nested exception is: java.lang.Exception: cause", exceptionWithDifferentCausedByAndInitCause.getMessage());
    }

    @Test
    public void testGetCause() {
        Assert.assertNull(exceptionDefaultConstructor.getCause());
        Assert.assertNull(exceptionWithNullMessage.getCause());
        Assert.assertNull(exceptionWithMessage.getCause());
        Assert.assertNotNull(exceptionWithCausedBy.getCause());
        Assert.assertNotNull(exceptionWithInitCause.getCause());
        Assert.assertNotNull(exceptionWithCausedByAndInitCause.getCause());
        Assert.assertNotNull(exceptionWithDifferentCausedByAndInitCause.getCause());
    }

    @Test
    public void testGetCausedByException() {
        Assert.assertNull(exceptionDefaultConstructor.getCausedByException());
        Assert.assertNull(exceptionWithNullMessage.getCausedByException());
        Assert.assertNull(exceptionWithMessage.getCausedByException());
        Assert.assertSame(exceptionWithCausedBy.getCause(), exceptionWithCausedBy.getCausedByException());
        Assert.assertNull(exceptionWithInitCause.getCausedByException());
        Assert.assertNotNull(exceptionWithCausedByAndInitCause.getCausedByException());
        Assert.assertNotNull(exceptionWithDifferentCausedByAndInitCause.getCausedByException());
    }

    @Test
    public void testInitCause() {
        Exception cause1 = new Exception("cause1");
        Exception cause2 = new Exception("cause2");

        EJBException ejbEx = new EJBException(cause1);
        Assert.assertSame(cause1, ejbEx.getCausedByException());
        Assert.assertSame(cause1, ejbEx.getCause());

        ejbEx.initCause(cause2);
        Assert.assertSame(cause1, ejbEx.getCausedByException());
        Assert.assertSame(cause2, ejbEx.getCause());
    }

    private static final String[] printStackTraces(Throwable t) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        t.printStackTrace(new PrintStream(baos));

        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));

        return new String[] { new String(baos.toByteArray()), sw.toString() };
    }

    private static String highlight(String s) {
        return ">>>" + s + "<<<";
    }

    private void assertStackTrace(String s, String... pieces) throws IOException {
        BufferedReader br = new BufferedReader(new StringReader(s));

        int pos = 0;
        for (String line; (line = br.readLine()) != null;) {
            if (line.startsWith("\tat") && (pos > 0 && pieces[pos - 1].equals("\tat"))) {
                continue;
            }

            if (pos == pieces.length) {
                Assert.fail("unexpected lines" + System.getProperty("line.separator") + highlight(s));
            }

            if (!line.startsWith(pieces[pos])) {
                Assert.fail(highlight(line) + " does not start with piece " +
                            (pos + 1) + " " + highlight(pieces[pos]) +
                            System.getProperty("line.separator") + highlight(s));
            }

            pos++;
        }

        if (pos != pieces.length) {
            Assert.fail("expected piece " + (pos + 1) + System.getProperty("line.separator") + highlight(s));
        }
    }

    @Test
    public void testPrintStackTrace() throws Exception {
        for (String s : printStackTraces(exceptionDefaultConstructor)) {
            assertStackTrace(s, "jakarta.ejb.EJBException", "\tat");
        }

        for (String s : printStackTraces(exceptionWithNullMessage)) {
            assertStackTrace(s, "jakarta.ejb.EJBException", "\tat");
        }

        for (String s : printStackTraces(exceptionWithMessage)) {
            assertStackTrace(s, "jakarta.ejb.EJBException", "\tat");
        }

        for (String s : printStackTraces(exceptionWithCausedBy)) {
            assertStackTrace(s, "jakarta.ejb.EJBException: msg; nested exception",
                             "\tat",
                             "Caused by",
                             "\tat",
                             "\t...");
        }

        for (String s : printStackTraces(exceptionWithInitCause)) {
            assertStackTrace(s,
                             "jakarta.ejb.EJBException",
                             "\tat",
                             "Caused by",
                             "\tat",
                             "\t...");
        }

        for (String s : printStackTraces(exceptionWithCausedByAndInitCause)) {
            assertStackTrace(s,
                             "jakarta.ejb.EJBException: msg; nested exception",
                             "\tat",
                             "Caused by",
                             "\tat",
                             "\t...");
        }

        for (String s : printStackTraces(exceptionWithDifferentCausedByAndInitCause)) {
            assertStackTrace(s,
                             "jakarta.ejb.EJBException: msg; nested exception is: java.lang.Exception: cause",
                             "java.lang.Exception: cause",
                             "\tat",
                             "jakarta.ejb.EJBException: msg; nested exception is: java.lang.Exception: cause",
                             "\tat",
                             "Caused by: java.lang.RuntimeException: initCause",
                             "\t...");
        }
    }
}