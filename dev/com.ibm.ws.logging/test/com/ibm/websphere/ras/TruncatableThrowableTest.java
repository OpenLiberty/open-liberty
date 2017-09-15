/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ras;

import static com.ibm.ws.logging.internal.InternalPackageChecker.checkIfPackageIsInSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

import com.ibm.websphere.ras.dummyinternal.DummyInternalClass;
import com.ibm.websphere.ras.dummyspec.AnotherDummySpecClass;
import com.ibm.websphere.ras.dummyspec.ExceptionMaker;
import com.ibm.websphere.ras.dummyspec.SomeDummySpecClass;
import com.ibm.ws.logging.internal.PackageProcessor;

/**
 *
 */
public class TruncatableThrowableTest implements ExceptionMaker {

    /**  */
    private static final String CAUSED_BY = "Caused by";

    /**  */
    private static final String AT_INTERNAL_CLASSES = "at [internal classes]";

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public final TestRule outputRule = outputMgr;
    private JUnit4Mockery context;
    private PackageProcessor processor;

    public final Set<String> internalPackages = new HashSet<String>();
    public final Set<String> specPackages = new HashSet<String>();

    @Before
    public void setUp() {

        context = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        // Define some sensible default internalPackages
        internalPackages.clear();
        internalPackages.add("com.ibm.websphere.ras.dummyinternal");
        internalPackages.add("org.junit.runners");
        internalPackages.add("sun.reflect");
        internalPackages.add("java.lang.reflect");
        internalPackages.add("test.common");
        internalPackages.add("org.junit.rules");
        internalPackages.add("org.junit.rules");
        internalPackages.add("org.junit.internal.runners.statements");
        internalPackages.add("org.junit.runners.model");
        internalPackages.add("org.junit.internal.runners.model");

        specPackages.add("com.ibm.websphere.ras.dummyspec");
        specPackages.add("org.eclipse.jdt.internal.junit4.runner");
        specPackages.add("org.eclipse.jdt.internal.junit.runner");
        specPackages.add("org.apache.tools.ant.taskdefs.optional.junit");
        specPackages.add("junit.framework");

        processor = context.mock(PackageProcessor.class);
        context.checking(new Expectations() {
            {
                allowing(processor).isIBMPackage(with(any(String.class)));
                will(checkIfPackageIsInSet(internalPackages));
                allowing(processor).isSpecOrThirdPartyOrBootDelegationPackage(with(any(String.class)));
                will(checkIfPackageIsInSet(specPackages));

            }
        });
        PackageProcessor.setProcessor(processor);
    }

    @Test
    public void testPrintTrimmedStack() {
        Exception e = new ContrivedException();
        TruncatableThrowable t = new TruncatableThrowable(e);
        t.printStackTrace();
        // Make sure some kind of stack was produced 
        assertTrue("The stack trace should have included the exception class name.", outputMgr.checkForLiteralStandardErr(ContrivedException.class.getName()));
        assertTrue("The stack trace should have included the originating class name.", outputMgr.checkForLiteralStandardErr(this.getClass().getName()));
        // Make sure the stack was truncated - we'll do more detailed assertions in other tests
        assertFalse("The stack trace should not include our TestRule.", outputMgr.checkForLiteralStandardErr("SharedOutputManager"));
        assertTrue("The stack trace should include the message saying it's truncated.", outputMgr.checkForLiteralStandardErr(AT_INTERNAL_CLASSES));
        assertTrue("The stack trace should not have any dangling code after the message saying it is truncated.", outputMgr.getCapturedErr().trim().endsWith(AT_INTERNAL_CLASSES));
    }

    @Test
    public void testPrintTrimmedStackWithSpecifiedStream() {
        Exception e = new ContrivedException();
        TruncatableThrowable t = new TruncatableThrowable(e);
        t.printStackTrace(System.out);
        // Make sure the output went to stdout, not stederr
        assertFalse("System err shouldn't show evidence of a stack trace that was sent to system out.", outputMgr.checkForLiteralStandardErr(ContrivedException.class.getName()));
        assertFalse("System err shouldn't show evidence of a stack trace that was sent to system out.", outputMgr.checkForLiteralStandardErr(this.getClass().getName()));
        // Make sure the stack was truncated - we'll do more detailed assertions in other tests
        assertFalse("System err shouldn't show evidence of a stack trace that was sent to system out.", outputMgr.checkForLiteralStandardErr(AT_INTERNAL_CLASSES));
        // Make sure some kind of stack was produced 
        assertTrue("The stack trace should have gone to stdout and included the exception class name.", outputMgr.checkForLiteralStandardOut(ContrivedException.class.getName()));
        assertTrue("The stack trace should have gone to stdout and included the originating class name.", outputMgr.checkForLiteralStandardOut(this.getClass().getName()));
        // Make sure the stack was truncated - we'll do more detailed assertions in other tests
        assertFalse("The stack trace should not include our TestRule.", outputMgr.checkForLiteralStandardOut("SharedOutputManager"));
        assertTrue("The stack trace should gone to stdout and include the message saying it's truncated.", outputMgr.checkForLiteralStandardOut(AT_INTERNAL_CLASSES));
    }

    @Test
    public void testPrintTrimmedStackToPrintWriter() {
        final StringWriter s = new StringWriter();
        PrintWriter writer = new PrintWriter(s);
        Exception e = new ContrivedException();
        TruncatableThrowable t = new TruncatableThrowable(e);
        t.printStackTrace(writer);
        // Make sure some kind of stack was produced 
        assertTrue("The stack trace should have included the exception class name:" + s, s.toString().contains(ContrivedException.class.getName()));
        assertTrue("The stack trace should have included the originating class name:" + s, s.toString().contains(this.getClass().getName()));
        assertFalse("The stack trace should not include our TestRule. The stack trace was:" + s, s.toString().contains("SharedOutputManager"));
        assertTrue("The stack trace should include the message saying it's truncated:" + s, s.toString().contains(AT_INTERNAL_CLASSES));
    }

    @Test
    public void testPrintUntrimmedStackIsIdenticalToNormalCase() {
        final StringWriter os = new StringWriter();
        PrintWriter owriter = new PrintWriter(os);
        final StringWriter s = new StringWriter();
        PrintWriter writer = new PrintWriter(s);
        Exception e = new ContrivedException();
        // Clear the private packages so we don't trim
        internalPackages.clear();
        TruncatableThrowable t = new TruncatableThrowable(e);
        // Dump the stack traces to strings
        e.printStackTrace(owriter);
        t.printStackTrace(writer);
        assertEquals("The formatting of an untrimmed stack (trimmed to " + ") should be identical to the original exception.", os.toString(), s.toString());
    }

    @Test
    public void testPrintTrimmedStackIncludesUserCodeInMiddleOfStack() {
        // Remove one of the internal classes so we get a gap in the middle of the stack
        internalPackages.remove("org.junit.internal.runners.statements");
        internalPackages.remove("test.common");
        Exception e = new ContrivedException();
        TruncatableThrowable t = new TruncatableThrowable(e);
        t.printStackTrace();
        // Make sure some kind of stack was produced 
        assertTrue("The stack trace should have included the exception class name.", outputMgr.checkForLiteralStandardErr(ContrivedException.class.getName()));
        assertTrue("The stack trace should have included the originating class name.", outputMgr.checkForLiteralStandardErr(this.getClass().getName()));
        assertEquals("The stack trace should include several messages saying it's truncated.", 2, countLiteralStandardErr(AT_INTERNAL_CLASSES));
    }

    /**
     * Tests that we don't lose trivial nested exceptions.
     */
    @Test
    public void testPrintTrimmedStackWithNestedException() {
        Exception e = new ContrivedException(new IllegalArgumentException());
        TruncatableThrowable t = new TruncatableThrowable(e);
        t.printStackTrace();
        assertFalse("The stack trace should not include our TestRule (anywhere).", outputMgr.checkForLiteralStandardErr("SharedOutputManager"));
        assertTrue("The stack trace should include the message saying it's truncated.", outputMgr.checkForLiteralStandardErr(AT_INTERNAL_CLASSES));
        assertTrue("The stack trace should include a little 'caused by' prefix.", outputMgr.checkForLiteralStandardErr("Caused by: "));
        assertEquals("The stack trace should include only one 'caused by' prefix.", 1, countLiteralStandardErr("Caused by: "));
        assertTrue("The stack trace should include the original exception.", outputMgr.checkForLiteralStandardErr("ContrivedException"));
        assertTrue("The stack trace should include the nested exception.", outputMgr.checkForLiteralStandardErr("IllegalArgumentException"));
        assertEquals("The stack trace should only include one at [internal classes]", 1, countLiteralStandardErr(AT_INTERNAL_CLASSES));

    }

    /**
     * Tests that we don't lose nested exceptions even when there are several of them.
     */
    @Test
    public void testPrintTrimmedStackWithMultipleNestedExceptions() {
        Exception nestedException1 = new IllegalArgumentException();
        Exception nestedException2 = new ArbitraryException(nestedException1);
        Exception nestedException3 = new AnotherException(nestedException2);

        Exception e = new ContrivedException(nestedException3);
        TruncatableThrowable t = new TruncatableThrowable(e);
        t.printStackTrace();
        assertFalse("The stack trace should not include our TestRule (anywhere).", outputMgr.checkForLiteralStandardErr("SharedOutputManager"));
        assertTrue("The stack trace should include the message saying it's truncated.", outputMgr.checkForLiteralStandardErr(AT_INTERNAL_CLASSES));
        assertTrue("The stack trace should include the original exception.", outputMgr.checkForLiteralStandardErr("ContrivedException"));
        assertTrue("The stack trace should include the first nested exception.", outputMgr.checkForLiteralStandardErr("IllegalArgumentException"));
        assertTrue("The stack trace should include the second nested exception.", outputMgr.checkForLiteralStandardErr("ArbitraryException"));
        assertTrue("The stack trace should include the third nested exception.", outputMgr.checkForLiteralStandardErr("AnotherException"));
        assertEquals("The stack trace should include three 'caused by' prefixes.", 3, countLiteralStandardErr("Caused by: "));
        assertEquals("The stack trace should only include one at [internal classes]", 1, countLiteralStandardErr("\tat [internal classes]"));

    }

    /**
     * Tests that we don't handle nested exceptions properly when there's very little overlap between
     * the original exception and the nested one
     */
    @Test
    public void testPrintTrimmedStackWithMultipleNestedExceptionsAndNoOverlap() {
        Random seededRandom = new Random(333);
        // Make an exception which is totally unrecognisable to us
        Exception nestedException = new ExceptionWithDifferentStackTrace(seededRandom);

        Exception e = new ContrivedException(nestedException);
        TruncatableThrowable t = new TruncatableThrowable(e);
        t.printStackTrace();
        assertFalse("The stack trace should not include our TestRule (anywhere).", outputMgr.checkForLiteralStandardErr("SharedOutputManager"));
        assertTrue("The stack trace should include the message saying it's truncated.", outputMgr.checkForLiteralStandardErr(AT_INTERNAL_CLASSES));
        assertTrue("The stack trace should include the original exception.", outputMgr.checkForLiteralStandardErr("ContrivedException"));
        assertTrue("The stack trace should include the nested exception.", outputMgr.checkForLiteralStandardErr("ExceptionWithDifferentStackTrace"));
        assertEquals("The stack trace should include one 'caused by' prefixes.", 1, countLiteralStandardErr("Caused by: "));
        assertFalse("The nested exception should be trimmed to above the internal package", outputMgr.checkForLiteralStandardErr("badbad"));
    }

    @Test
    public void testPrintTrimmedStackWithInternalClassesInTheMiddle() {
        Exception nestedException1 = new IllegalArgumentException();
        Exception nestedException2 = new ArbitraryException(nestedException1);
        Exception nestedException3 = new AnotherException(nestedException2);

        Exception e = new ContrivedException(nestedException3);
        TruncatableThrowable t = new TruncatableThrowable(e);
        t.printStackTrace();
        assertFalse("The stack trace should not include our TestRule (anywhere).", outputMgr.checkForLiteralStandardErr("SharedOutputManager"));
        assertTrue("The stack trace should include the message saying it's truncated.", outputMgr.checkForLiteralStandardErr(AT_INTERNAL_CLASSES));
        assertTrue("The stack trace should include the original exception.", outputMgr.checkForLiteralStandardErr("ContrivedException"));
        assertTrue("The stack trace should include the first nested exception.", outputMgr.checkForLiteralStandardErr("IllegalArgumentException"));
        assertTrue("The stack trace should include the second nested exception.", outputMgr.checkForLiteralStandardErr("ArbitraryException"));
        assertTrue("The stack trace should include the third nested exception.", outputMgr.checkForLiteralStandardErr("AnotherException"));
        assertEquals("The stack trace should include three 'caused by' prefixes.", 3, countLiteralStandardErr("Caused by: "));
        assertEquals("The stack trace should only include one at [internal classes]", 1, countLiteralStandardErr(AT_INTERNAL_CLASSES));

    }

    /**
     * Spec classes sandwiched by IBM code should be trimmed
     */
    @Test
    public void testPrintTrimmedStackWithSpecClassesInTheMiddle() {
        Exception e = new DummyInternalClass().callback(new SomeDummySpecClass(new DummyInternalClass()));
        TruncatableThrowable t = new TruncatableThrowable(e);
        t.printStackTrace();
        assertTrue("The stack trace should include some exception",
                   outputMgr.checkForLiteralStandardErr("Exception"));
        assertTrue("The stack trace should include some evidence of stack traces",
                   outputMgr.checkForLiteralStandardErr("\tat"));
        assertFalse("The stack trace should not include the spec package, since it was internal code that called it.",
                    outputMgr.checkForLiteralStandardErr(SomeDummySpecClass.class.getName()));
    }

    /**
     * Spec classes sandwiched by IBM code should be trimmed
     */
    @Test
    public void testPrintTrimmedStackWithSpecClassesInTheTop() {
        Exception e = new SomeDummySpecClass(new AnotherDummySpecClass(this)).constructException();
        TruncatableThrowable t = new TruncatableThrowable(e);
        t.printStackTrace();
        assertTrue("The stack trace should include the spec package, since it was user code that called it.",
                    outputMgr.checkForLiteralStandardErr(AnotherDummySpecClass.class.getName()));
        assertTrue("The stack trace should include everything in the spec package, since it was user code that called it.",
                   outputMgr.checkForLiteralStandardErr(SomeDummySpecClass.class.getName()));
    }

    /**
     * Tests that we don't produce six-million 'Caused by:" clauses.
     */
    @Test
    public void testPrintTrimmedStackWithMultipleNestedRedundantExceptionsGetRedundantMiddleBitsTrimmed() {
        Exception nestedException = new IllegalArgumentException();
        for (int i = 0; i < 8; i++) {
            nestedException = new ArbitraryException(nestedException);
        }
        Exception e = new ContrivedException(nestedException);
        TruncatableThrowable t = new TruncatableThrowable(e);
        t.printStackTrace();
        assertFalse("The stack trace should not include our TestRule (anywhere).", outputMgr.checkForLiteralStandardErr("SharedOutputManager"));
        assertTrue("The stack trace should include the message saying it's truncated.", outputMgr.checkForLiteralStandardErr(AT_INTERNAL_CLASSES));
        assertTrue("The stack trace should include the original exception.", outputMgr.checkForLiteralStandardErr("ContrivedException"));
        assertTrue("The stack trace should include the first nested exception.", outputMgr.checkForLiteralStandardErr("IllegalArgumentException"));
        assertEquals("The stack trace should only include once reference to the ArbitraryException", 1,
                     countLiteralStandardErr("ArbitraryException"));
        assertEquals("Even though there are lots of nested exceptions, the stack trace should only include two 'caused by' prefixes.", 2,
                     countLiteralStandardErr(CAUSED_BY));
        assertTrue("The stack trace should flag the ArbitraryException as the repeated one.",
                   outputMgr.checkForLiteralStandardErr("Caused by (repeated) ... : " + this.getClass().getName() + "$ArbitraryException"));

        assertEquals("The stack trace should only include one at [internal classes]", 1, countLiteralStandardErr("\tat [internal classes]"));

    }

    /**
     * Tests that we don't produce six-million 'Caused by:" clauses.
     */
    @Test
    public void testPrintTrimmedStackWithMultipleNestedRedundantExceptionsGetTrimmed() {
        Exception nestedException = new IllegalArgumentException();
        for (int i = 0; i < 8; i++) {
            nestedException = new ArbitraryException(nestedException);
        }
        TruncatableThrowable t = new TruncatableThrowable(nestedException);
        t.printStackTrace();
        assertFalse("The stack trace should not include our TestRule (anywhere).", outputMgr.checkForLiteralStandardErr("SharedOutputManager"));
        assertTrue("The stack trace should include the message saying it's truncated.", outputMgr.checkForLiteralStandardErr(AT_INTERNAL_CLASSES));
        assertTrue("The stack trace should include the original exception.", outputMgr.checkForLiteralStandardErr("ArbitraryException"));
        assertTrue("The stack trace should include the root nested exception.", outputMgr.checkForLiteralStandardErr("IllegalArgumentException"));
        assertEquals("Even though there are lots of nested exceptions, the stack trace should only include one 'caused by' prefix.", 1,
                     countLiteralStandardErr(CAUSED_BY));
        assertEquals("Even though there are lots of nested exceptions, the stack trace should only include one listing of the nested exception.", 1,
                     countLiteralStandardErr("ArbitraryException"));
        assertEquals("The stack trace should only include one at [internal classes]", 1, countLiteralStandardErr("\tat [internal classes]"));

    }

    @Test
    public void testGetStackTrace() {
        Exception e = new ContrivedException();
        TruncatableThrowable t = new TruncatableThrowable(e);
        StackTraceElement[] stackElements = t.getStackTrace();
        // We better have a stack, but not a huge one
        // (We can't output Arrays.toString() of the stack element, or JUnit parses it and thinks it's an error we've thrown
        assertEquals("The stack trace should have the expected number of elements in it. ", 3, stackElements.length);

        // Make sure some kind of stack was produced 
        assertEquals("The stack trace should have included the originating class name:" + stackElements[0], this.getClass().getName(), stackElements[0].getClassName());

        // The stack trace should include (and end with) the [internal classes] element
        boolean foundInternalClasses = false;
        boolean foundDanglingClasses = false;
        for (StackTraceElement el : stackElements) {
            if (el.getClassName().equals("[internal classes]")) {
                foundInternalClasses = true;
            } else if (foundInternalClasses) {
                foundDanglingClasses = true;
            }
        }
        assertTrue("The stack trace should have an element representing the truncated classes.", foundInternalClasses);
        assertFalse("The stack trace should not have any dangling code after the message saying it is truncated.", foundDanglingClasses);
    }

    /**
     * Tests that bad things don't happen if we try to truncate to a class which isn't in the
     * stack trace.
     */
    @Test
    public void testPrintTrimmedStackWithNonExistentPrivatePackages() {
        final StringWriter s = new StringWriter();
        PrintWriter writer = new PrintWriter(s);
        Exception e = new ContrivedException();
        internalPackages.clear();
        internalPackages.add("nonexistent.package");
        TruncatableThrowable t = new TruncatableThrowable(e);
        t.printStackTrace(writer);
        assertFalse("The stack trace should not be truncated if our private packages don't exist. The stack trace was: " + s, s.toString().contains(AT_INTERNAL_CLASSES));
        assertTrue("The stack trace should not be truncated if our private packages don't exist. The stack trace was:  " + s, s.toString().contains("JUnit"));
    }

    @Test
    public void testPrintTrimmedStackWithNullException() {
        final StringWriter s = new StringWriter();
        PrintWriter writer = new PrintWriter(s);
        TruncatableThrowable t = new TruncatableThrowable(null);
        t.printStackTrace(writer);
        // Not much we can assert here, other than that we didn't want an NPE

    }

    /**
     * Stack traces where the nested exception is NoClassDefFoundError and the nested
     * exception is ClassNotFoundException don't need to have both parts printed.
     * 
     */
    @Test
    public void testNestedClassDefExceptionsAreNotPrinted() {
        // An arbitrary class to not find
        Class<?> notFoundClass = HashSet.class;
        // The ClassNotFoundException gets just the class name (with dots) as the argument
        Exception cause = new ClassNotFoundException(notFoundClass.getName());
        // The NoClassDefFoundError has slashes in the classname
        NoClassDefFoundError symptom = new NoClassDefFoundError(notFoundClass.getName().replace('.', '/'));
        symptom.initCause(cause);
        TruncatableThrowable t = new TruncatableThrowable(symptom);
        t.printStackTrace();
        // The first exception should get passed through
        String capturedErr = outputMgr.getCapturedErr();
        assertTrue("The exception didn't get printed at all. Output was (may be formatted as a stack trace): " + capturedErr, capturedErr.contains("NoClassDefFoundError"));
        assertTrue("The exception wasn't trimmed. Output was (may be formatted as a stack trace): " + capturedErr, capturedErr.contains(AT_INTERNAL_CLASSES));
        assertFalse("The exception cause should not be printed, since it doesn't add value. Output was (may be formatted as a stack trace): " + capturedErr,
                    capturedErr.contains(CAUSED_BY));
        assertFalse("The exception cause should not be printed, since the nested exception is redundant. Output was (may be formatted as a stack trace): " + capturedErr,
                    capturedErr.contains("ClassNotFoundException"));
    }

    /**
     * Stack traces where the nested exception is NoClassDefFoundError and the nested
     * exception is ClassNotFoundException don't need to have both parts printed.
     * 
     * We sometimes see exceptions of the form
     * java.lang.NoClassDefFoundError: Ljavax/persistence/EntityManager;
     */
    @Test
    public void testNestedClassDefExceptionsAreNotPrintedForObjectFormattedClass() {
        // An arbitrary class to not find
        Class<?> notFoundClass = HashSet.class;
        // The ClassNotFoundException gets just the class name (with dots) as the argument
        Exception cause = new ClassNotFoundException(notFoundClass.getName());
        // The NoClassDefFoundError has slashes in the classname
        NoClassDefFoundError symptom = new NoClassDefFoundError("L" + notFoundClass.getName().replace('.', '/') + ";");
        symptom.initCause(cause);
        TruncatableThrowable t = new TruncatableThrowable(symptom);
        t.printStackTrace();
        // The first exception should get passed through
        String capturedErr = outputMgr.getCapturedErr();
        assertTrue("The exception didn't get printed at all. Output was (may be formatted as a stack trace): " + capturedErr, capturedErr.contains("NoClassDefFoundError"));
        assertTrue("The exception wasn't trimmed. Output was (may be formatted as a stack trace): " + capturedErr, capturedErr.contains(AT_INTERNAL_CLASSES));
        assertFalse("The exception cause should not be printed, since it doesn't add value. Output was (may be formatted as a stack trace): " + capturedErr,
                    capturedErr.contains(CAUSED_BY));
        assertFalse("The exception cause should not be printed, since the nested exception is redundant. Output was (may be formatted as a stack trace): " + capturedErr,
                    capturedErr.contains("ClassNotFoundException"));
    }

    /**
     * Stack traces where the nested exception is NoClassDefFoundError and the nested
     * exception is ClassNotFoundException don't need to have both parts printed.
     * 
     * We sometimes see exceptions of the form
     * java.lang.ClassNotFoundException: javax.persistence.EntityManager
     */
    @Test
    public void testNestedClassDefExceptionsAreNotPrintedWhenExceptionNameIncludedInMessage() {
        // An arbitrary class to not find
        Class<?> notFoundClass = HashSet.class;
        // The ClassNotFoundException gets just the class name (with dots) as the argument
        Exception cause = new ClassNotFoundException("java.lang.ClassNotFoundException: " + notFoundClass.getName());
        // The NoClassDefFoundError has slashes in the classname
        NoClassDefFoundError symptom = new NoClassDefFoundError("L" + notFoundClass.getName().replace('.', '/') + ";");
        symptom.initCause(cause);
        TruncatableThrowable t = new TruncatableThrowable(symptom);
        t.printStackTrace();
        // The first exception should get passed through
        String capturedErr = outputMgr.getCapturedErr();
        assertTrue("The exception didn't get printed at all. Output was (may be formatted as a stack trace): " + capturedErr, capturedErr.contains("NoClassDefFoundError"));
        assertTrue("The exception wasn't trimmed. Output was (may be formatted as a stack trace): " + capturedErr, capturedErr.contains(AT_INTERNAL_CLASSES));
        assertFalse("The exception cause should not be printed, since it doesn't add value. Output was (may be formatted as a stack trace): " + capturedErr,
                    capturedErr.contains(CAUSED_BY));
        assertFalse("The exception cause should not be printed, since the nested exception is redundant. Output was (may be formatted as a stack trace): " + capturedErr,
                    capturedErr.contains("ClassNotFoundException"));
    }

    /**
     * We shouldn't get over-enthusiastic stripped out causes for NoClassDefFoundError exceptions
     * if the nested cause isn't redundant.
     */
    @Test
    public void testNestedClassDefExceptionsArePrintedWhenTheClassesAreDifferent() {
        // An arbitrary class to not find
        Class<?> notFoundClass = HashSet.class;
        // The ClassNotFoundException gets just the class name (with dots) as the argument
        ClassNotFoundException cause = new ClassNotFoundException("somedifferent" + notFoundClass.getName());
        // The NoClassDefFoundError has slashes in the classname
        NoClassDefFoundError symptom = new NoClassDefFoundError(notFoundClass.getName().replace('.', '/'));

        symptom.initCause(cause);
        TruncatableThrowable t = new TruncatableThrowable(symptom);
        t.printStackTrace();
        // The first exception should get passed through
        String capturedErr = outputMgr.getCapturedErr();
        assertTrue("The exception didn't get printed at all. Output was (may be formatted as a stack trace): " + capturedErr, capturedErr.contains("NoClassDefFoundError"));
        assertTrue("The exception wasn't trimmed. Output was (may be formatted as a stack trace): " + capturedErr, capturedErr.contains(AT_INTERNAL_CLASSES));
        assertTrue("The exception cause should be printed, since the classes aren't the same. Output was (may be formatted as a stack trace): " + capturedErr,
                    capturedErr.contains(CAUSED_BY));
        assertTrue("The exception cause should be printed, since the nested exception isn't redundant. Output was (may be formatted as a stack trace): " + capturedErr,
                   capturedErr.contains("ClassNotFoundException"));
    }

    /**
     * We shouldn't get over-enthusiastic stripped out causes for NoClassDefFoundError exceptions
     * if the nested cause isn't redundant.
     */
    @Test
    public void testNestedClassDefExceptionsArePrintedWhenTheNestedExceptionIsNotNoClassDefFoundError() {
        // An arbitrary class to not find
        Class<?> notFoundClass = HashSet.class;
        // The ClassNotFoundException gets just the class name (with dots) as the argument
        Exception cause = new Exception(notFoundClass.getName());
        // The NoClassDefFoundError has slashes in the classname
        NoClassDefFoundError symptom = new NoClassDefFoundError(notFoundClass.getName().replace('.', '/'));
        symptom.initCause(cause);
        TruncatableThrowable t = new TruncatableThrowable(symptom);
        t.printStackTrace();
        // The first exception should get passed through
        String capturedErr = outputMgr.getCapturedErr();
        assertTrue("The exception didn't get printed at all. Output was (may be formatted as a stack trace): " + capturedErr, capturedErr.contains("NoClassDefFoundError"));
        assertTrue("The exception wasn't trimmed. Output was (may be formatted as a stack trace): " + capturedErr, capturedErr.contains(AT_INTERNAL_CLASSES));
        assertTrue("The exception cause should be printed, since the nested exception isn't redundant. Output was (may be formatted as a stack trace): " + capturedErr,
                    capturedErr.contains(CAUSED_BY));
        assertTrue("The exception cause should be printed, since the nested exception isn't redundant. Output was (may be formatted as a stack trace): " + capturedErr,
                   capturedErr.contains("Exception"));
    }

    private int countLiteralStandardErr(String literal) {
        // Use a search rather than split() to avoid regex issues
        int count = 0;
        String output = outputMgr.getCapturedErr();
        int lastIndex = output.indexOf(literal);

        while (lastIndex != -1) {
            count++;
            lastIndex = output.indexOf(literal, (lastIndex + literal.length() - 1));
        }
        return count;
    }

    @Override
    public Exception constructException() {
        return new ContrivedException();
    }

    private static class ContrivedException extends Exception {
        /**
         * 
         */
        public ContrivedException() {
            super();
        }

        public ContrivedException(Throwable nestedException) {
            super("no exception name in the message to sneakily make our assertions pass", nestedException);
        }

        private static final long serialVersionUID = 1L;

    }

    private static class AnotherException extends Exception {

        public AnotherException(Throwable nestedException) {
            super("another message with nothing in the message to sneakily make our assertions pass", nestedException);
        }

        private static final long serialVersionUID = 1L;

    }

    private static class ArbitraryException extends Exception {

        public ArbitraryException(Throwable nestedException) {
            super("another message with nothing in the message to sneakily make our assertions pass", nestedException);
        }

        private static final long serialVersionUID = 1L;

    }

    /**
     * An exception whose stack trace elements bear no resemblance to any of the
     * other stack traces which might naturally come out of this code.
     */
    private class ExceptionWithDifferentStackTrace extends Exception {

        private static final long serialVersionUID = 1L;
        private final Random random;
        private final boolean registerSomePackages;

        /**
         * @param seededRandom a seeded random, so that test results are always deterministic, even if tests are run
         *            in a different order
         */
        public ExceptionWithDifferentStackTrace(Random seededRandom) {
            this(seededRandom, true);
        }

        public ExceptionWithDifferentStackTrace(Random seededRandom, boolean registerSomePackages) {
            super();
            random = seededRandom;
            this.registerSomePackages = registerSomePackages;
        }

        @Override
        public StackTraceElement[] getStackTrace() {
            List<StackTraceElement> list = new ArrayList<StackTraceElement>();
            for (int i = 0; i < 10; i++) {
                list.add(new StackTraceElement(randomString(), randomString(), randomString() + ".java", random.nextInt(2000)));
            }
            // Add in some code that we recognise it can get truncated
            list.add(new StackTraceElement("cutpointhere.SomeClass", "someMethod", "SomeClass.java", 123));
            list.add(new StackTraceElement("com.ibm.badbadbad.ShouldBeTrimmed", "something", "ShouldBeTrimmed.java", 123));
            if (registerSomePackages) {
                internalPackages.add("cutpointhere");
                specPackages.add("com.ibm.badbadbad");
            }
            for (int i = 0; i < 10; i++) {
                String packageName = randomString();
                list.add(new StackTraceElement(packageName + "." + randomString(), randomString(), randomString() + ".java", random.nextInt(2000)));
                specPackages.add(packageName);
            }

            return list.toArray(new StackTraceElement[0]);
        }

        private String randomString() {
            StringBuilder s = new StringBuilder();
            int length = random.nextInt(20) + 3;
            int aOffset = 'a';
            for (int i = 0; i < length; i++) {
                s.append((char) (aOffset + random.nextInt(26)));
            }
            return s.toString();
        }
    }

}
