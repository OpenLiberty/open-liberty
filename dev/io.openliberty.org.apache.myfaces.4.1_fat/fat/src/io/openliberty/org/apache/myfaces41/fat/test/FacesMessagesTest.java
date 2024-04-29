/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces41.fat.test;

import static jakarta.faces.application.FacesMessage.SEVERITY_INFO;
import static jakarta.faces.application.FacesMessage.SEVERITY_WARN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.print.attribute.standard.Severity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import jakarta.faces.application.FacesMessage;

/*
 * https://github.com/jakartaee/faces/issues/1823
 * FacesMessage: implement equals(), hashcode(), toString()
 */
@RunWith(FATRunner.class)
public class FacesMessagesTest {

    //These two are the same
    private static final FacesMessage MESSAGE_1_1 = new FacesMessage(SEVERITY_INFO, "Message", "Details");
    private static final FacesMessage MESSAGE_1_2 = new FacesMessage(SEVERITY_INFO, "Message", "Details");

    //These differ in each parameter
    private static final FacesMessage MESSAGE_1_3 = new FacesMessage(SEVERITY_WARN, "Message", "Details");
    private static final FacesMessage MESSAGE_1_4 = new FacesMessage(SEVERITY_INFO, "InfoMessage", "Details");
    private static final FacesMessage MESSAGE_1_5 = new FacesMessage(SEVERITY_INFO, "Message", "Key_Details");



    private static final FacesMessage MESSAGE_2_1 = new FacesMessage(SEVERITY_WARN, "Message2", "Details2");
    private static final FacesMessage MESSAGE_2_2 = new FacesMessage(SEVERITY_WARN, "Message2", "Details2");

    public static class CustomFacesMessage extends FacesMessage {
        private static final long serialVersionUID = 1L;

        public CustomFacesMessage(Severity severity, String summary, String detail) {
            super(severity, summary, detail);
        }
    }

    protected static final Class<?> c = FacesMessagesTest.class;

    private static final Logger LOG = Logger.getLogger(FacesMessagesTest.class.getName());

    @Rule
    public TestName name = new TestName();

    /**
     *  Verify that FacesMessage.equals() works.
     *  Two FacesMesages with the same arguments should be equal.
     * 
     * @throws Exception
     */
    @Test
    public void testFacesMessagesEquals() throws Exception {
        // Messages with the same information should be equal
        assertTrue("Should have been equal", MESSAGE_1_1.equals(MESSAGE_1_2));
        assertTrue("Should have been equal", MESSAGE_2_1.equals(MESSAGE_2_2));

        // Info differs in each arguement 
        assertFalse("Should have been different", MESSAGE_1_1.equals(MESSAGE_1_3));
        assertFalse("Should have been different", MESSAGE_1_1.equals(MESSAGE_1_4));
        assertFalse("Should have been different", MESSAGE_1_1.equals(MESSAGE_1_5));

        // Messages with different information should not be equal
        assertFalse("Should have been different", MESSAGE_1_2.equals(MESSAGE_2_2));
        assertFalse("Should have been different", MESSAGE_1_1.equals(MESSAGE_2_1));
    }

    /**
     *  Verify that FacesMessage.hashCode() works. 
     *  Two FacesMesages with the same arguments should have the same hashcode. 
     * 
     * @throws Exception
     */
    @Test
    public void testFacesMessagesHashCode() throws Exception {
        // Messages with the same information should be equal
        assertEquals("Should be equal", MESSAGE_1_1.hashCode(), MESSAGE_1_2.hashCode());
        assertEquals("Should be equal", MESSAGE_2_1.hashCode(), MESSAGE_2_2.hashCode());

        // These should differ
        assertNotEquals(MESSAGE_1_1.hashCode(), MESSAGE_1_3.hashCode());
        assertNotEquals(MESSAGE_1_1.hashCode(), MESSAGE_1_4.hashCode());
        assertNotEquals(MESSAGE_1_1.hashCode(), MESSAGE_1_5.hashCode());

        // Messages with different information should not be equal
        assertNotEquals(MESSAGE_1_2.hashCode(), MESSAGE_2_2.hashCode());
        assertNotEquals(MESSAGE_1_1.hashCode(), MESSAGE_2_1.hashCode());
    }

    /**
     *  Verify that FacesMessage.toString() works as expected.
     *  Comparing to the expected format. 
     * 
     * @throws Exception
     */
    @Test
    public void testFacesMessageToString() throws Exception {
        assertEquals("Does not match!" , "FacesMessage[severity='INFO', summary='Message', detail='Details']", MESSAGE_1_1.toString());
    }



    private void assertNotEquals(Object left, Object right) {
        assertFalse("The 2 objects were the same when they should not have been", left.equals(right));
    }
}
