/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal.otma;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.zos.channel.wola.internal.natv.WOLANativeUtils;

/**
 *
 */
public class OLAIMSOTMAKeyMapTest {

    private Mockery mockery = null;

    @Before
    public void before() {
        mockery = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
    }

    @After
    public void after() {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testAddToMap() throws Exception {
        final WOLANativeUtils utils = mockery.mock(WOLANativeUtils.class);
        final byte[] returnAnchor = new byte[8];

        // set expecations
        mockery.checking(new Expectations() {
            {
                oneOf(utils).openOTMAConenction(with(any(String.class)), with(any(String.class)), with(any(String.class)));
                will(returnValue(returnAnchor));
            }
        });

        OLAIMSOTMAKeyMap keyMap = new OLAIMSOTMAKeyMap(utils);
        OLAIMSOTMAKeyMap.Key key = new OLAIMSOTMAKeyMap.Key("GROUP", "SERVER", "CLIENT");
        byte[] anchor = keyMap.getOTMAAnchorKey(key);
        assertTrue(Arrays.equals(returnAnchor, anchor));

        anchor = keyMap.getOTMAAnchorKey(key);
        assertTrue(Arrays.equals(returnAnchor, anchor));
    }

    @Test
    public void testRemoveFromMap() throws Exception {
        final WOLANativeUtils utils = mockery.mock(WOLANativeUtils.class);
        final byte[] returnAnchor = new byte[8];
        final byte[] returnAnchor2 = new byte[8];
        returnAnchor2[7] = (byte) 0x01;

        // set expecations
        mockery.checking(new Expectations() {
            {
                oneOf(utils).openOTMAConenction(with(any(String.class)), with(any(String.class)), with(any(String.class)));
                will(returnValue(returnAnchor));

                oneOf(utils).closeOtmaConnection(with(same(returnAnchor)));
                will(returnValue(0));

                oneOf(utils).openOTMAConenction(with(any(String.class)), with(any(String.class)), with(any(String.class)));
                will(returnValue(returnAnchor2));
            }
        });

        OLAIMSOTMAKeyMap keyMap = new OLAIMSOTMAKeyMap(utils);
        OLAIMSOTMAKeyMap.Key key = new OLAIMSOTMAKeyMap.Key("GROUP", "SERVER", "CLIENT");
        byte[] anchor = keyMap.getOTMAAnchorKey(key);
        assertTrue(Arrays.equals(returnAnchor, anchor));

        keyMap.clearOTMAAnchorKey(key, anchor);

        anchor = keyMap.getOTMAAnchorKey(key);
        assertTrue(Arrays.equals(returnAnchor2, anchor));
    }

    @Test
    public void testOpenError() throws Exception {
        final WOLANativeUtils utils = mockery.mock(WOLANativeUtils.class);

        // set expecations
        mockery.checking(new Expectations() {
            {
                oneOf(utils).openOTMAConenction(with(any(String.class)), with(any(String.class)), with(any(String.class)));
                will(throwException(new OTMAException(new int[] { 8, 0, 0, 0, 2 })));
            }
        });

        OLAIMSOTMAKeyMap keyMap = new OLAIMSOTMAKeyMap(utils);
        OLAIMSOTMAKeyMap.Key key = new OLAIMSOTMAKeyMap.Key("GROUP", "SERVER", "CLIENT");

        try {
            keyMap.getOTMAAnchorKey(key);
            throw new Exception("Did not catch expected OTMAException");
        } catch (OTMAException otmae) {
            assertEquals(otmae.getFunctionName(), "otma_open");
            assertEquals(otmae.getReturnCode(), 8);
        }
    }

    @Test
    public void testKeyPermutations() throws Exception {
        final WOLANativeUtils utils = mockery.mock(WOLANativeUtils.class);
        final byte[] returnAnchor0 = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
        final byte[] returnAnchor1 = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01 };
        final byte[] returnAnchor2 = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02 };
        final byte[] returnAnchor3 = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03 };
        final byte[] returnAnchor4 = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04 };
        final byte[] returnAnchor5 = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05 };
        final byte[] returnAnchor6 = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x06 };
        final byte[] returnAnchor7 = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07 };

        final String name1 = "NAME1";
        final String name2 = "NAME2";

        // set expecations
        mockery.checking(new Expectations() {
            {
                oneOf(utils).openOTMAConenction(with(equal(name1)), with(equal(name1)), with(equal(name1)));
                will(returnValue(returnAnchor0));

                oneOf(utils).openOTMAConenction(with(equal(name1)), with(equal(name1)), with(equal(name2)));
                will(returnValue(returnAnchor1));

                oneOf(utils).openOTMAConenction(with(equal(name1)), with(equal(name2)), with(equal(name1)));
                will(returnValue(returnAnchor2));

                oneOf(utils).openOTMAConenction(with(equal(name1)), with(equal(name2)), with(equal(name2)));
                will(returnValue(returnAnchor3));

                oneOf(utils).openOTMAConenction(with(equal(name2)), with(equal(name1)), with(equal(name1)));
                will(returnValue(returnAnchor4));

                oneOf(utils).openOTMAConenction(with(equal(name2)), with(equal(name1)), with(equal(name2)));
                will(returnValue(returnAnchor5));

                oneOf(utils).openOTMAConenction(with(equal(name2)), with(equal(name2)), with(equal(name1)));
                will(returnValue(returnAnchor6));

                oneOf(utils).openOTMAConenction(with(equal(name2)), with(equal(name2)), with(equal(name2)));
                will(returnValue(returnAnchor7));
            }
        });

        OLAIMSOTMAKeyMap keyMap = new OLAIMSOTMAKeyMap(utils);
        OLAIMSOTMAKeyMap.Key key0 = new OLAIMSOTMAKeyMap.Key(name1, name1, name1);
        OLAIMSOTMAKeyMap.Key key1 = new OLAIMSOTMAKeyMap.Key(name1, name2, name1);
        OLAIMSOTMAKeyMap.Key key2 = new OLAIMSOTMAKeyMap.Key(name1, name1, name2);
        OLAIMSOTMAKeyMap.Key key3 = new OLAIMSOTMAKeyMap.Key(name1, name2, name2);
        OLAIMSOTMAKeyMap.Key key4 = new OLAIMSOTMAKeyMap.Key(name2, name1, name1);
        OLAIMSOTMAKeyMap.Key key5 = new OLAIMSOTMAKeyMap.Key(name2, name2, name1);
        OLAIMSOTMAKeyMap.Key key6 = new OLAIMSOTMAKeyMap.Key(name2, name1, name2);
        OLAIMSOTMAKeyMap.Key key7 = new OLAIMSOTMAKeyMap.Key(name2, name2, name2);

        byte[] anchor0 = keyMap.getOTMAAnchorKey(key0);
        byte[] anchor1 = keyMap.getOTMAAnchorKey(key1);
        byte[] anchor2 = keyMap.getOTMAAnchorKey(key2);
        byte[] anchor3 = keyMap.getOTMAAnchorKey(key3);
        byte[] anchor4 = keyMap.getOTMAAnchorKey(key4);
        byte[] anchor5 = keyMap.getOTMAAnchorKey(key5);
        byte[] anchor6 = keyMap.getOTMAAnchorKey(key6);
        byte[] anchor7 = keyMap.getOTMAAnchorKey(key7);

        assertTrue(Arrays.equals(returnAnchor0, anchor0));
        assertTrue(Arrays.equals(returnAnchor1, anchor1));
        assertTrue(Arrays.equals(returnAnchor2, anchor2));
        assertTrue(Arrays.equals(returnAnchor3, anchor3));
        assertTrue(Arrays.equals(returnAnchor4, anchor4));
        assertTrue(Arrays.equals(returnAnchor5, anchor5));
        assertTrue(Arrays.equals(returnAnchor6, anchor6));
        assertTrue(Arrays.equals(returnAnchor7, anchor7));

        anchor0 = keyMap.getOTMAAnchorKey(key0);
        assertTrue(Arrays.equals(returnAnchor0, anchor0));
    }
}
