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
package com.ibm.ws.sib.msgstore.test.list;

/*
 * Change activity:
 *
 *  Reason           Date    Origin   Description
 * --------------- -------- -------- ------------------------------------------
 *                 11/11/05 schofiel  Original
 * 278082          03/01/06 schofiel  Rework link position in lists and cursor availability
 * 344089          14/02/06 gareth    Improve debug in case of failures
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.js.test.LoggingTestCase;
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.msgstore.list.BehindRefList;

public final class BehindRefListTest extends LoggingTestCase
{
    public BehindRefListTest() {}

    public static TestSuite suite()
    {
        return new TestSuite(BehindRefListTest.class);
    }

    public final void testInsert()
    {
        BehindRefList brl;
        String toString;

        brl = new BehindRefList();
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5]}", toString.equals("{[5]}"));

        brl = new BehindRefList().append(null);
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5]}", toString.equals("{[5]}"));

        brl = new BehindRefList().append(new AIL(10));
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][10]}", toString.equals("{[5][10]}"));
        brl = new BehindRefList().append(new AIL(10));
        brl.insert(new AIL(10));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10]}", toString.equals("{[10]}"));
        brl = new BehindRefList().append(new AIL(10));
        brl.insert(new AIL(15));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][15]}", toString.equals("{[10][15]}"));

        brl = new BehindRefList().append(null).append(null);
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5]}", toString.equals("{[5]}"));

        brl = new BehindRefList().append(null).append(new AIL(20));
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][20]}", toString.equals("{[5][20]}"));
        brl = new BehindRefList().append(null).append(new AIL(20));
        brl.insert(new AIL(20));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20]}", toString.equals("{[#][20]}"));
        brl = new BehindRefList().append(null).append(new AIL(20));
        brl.insert(new AIL(25));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][25]}", toString.equals("{[#][20][25]}"));

        brl = new BehindRefList().append(new AIL(10)).append(null);
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][10]}", toString.equals("{[5][10]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null);
        brl.insert(new AIL(20));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20]}", toString.equals("{[10][20]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null);
        brl.insert(new AIL(25));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][25]}", toString.equals("{[10][25]}"));

        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20));
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][10][20]}", toString.equals("{[5][10][20]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20));
        brl.insert(new AIL(10));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20]}", toString.equals("{[10][20]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20));
        brl.insert(new AIL(15));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][15][20]}", toString.equals("{[10][15][20]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20));
        brl.insert(new AIL(20));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20]}", toString.equals("{[10][20]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20));
        brl.insert(new AIL(25));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][25]}", toString.equals("{[10][20][25]}"));

        brl = new BehindRefList().append(null).append(null).append(null);
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5]}", toString.equals("{[5]}"));

        brl = new BehindRefList().append(null).append(null).append(new AIL(30));
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][30]}", toString.equals("{[5][30]}"));
        brl = new BehindRefList().append(null).append(null).append(new AIL(30));
        brl.insert(new AIL(30));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#][30]}", toString.equals("{[#][#][30]}"));
        brl = new BehindRefList().append(null).append(null).append(new AIL(30));
        brl.insert(new AIL(35));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#][30][35]}", toString.equals("{[#][#][30][35]}"));

        brl = new BehindRefList().append(null).append(new AIL(20)).append(null);
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][20]}", toString.equals("{[5][20]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(null);
        brl.insert(new AIL(20));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20]}", toString.equals("{[#][20]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(null);
        brl.insert(new AIL(35));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][35]}", toString.equals("{[#][20][35]}"));

        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30));
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][20][30]}", toString.equals("{[5][20][30]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30));
        brl.insert(new AIL(20));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][30]}", toString.equals("{[#][20][30]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30));
        brl.insert(new AIL(25));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][25][30]}", toString.equals("{[#][20][25][30]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30));
        brl.insert(new AIL(30));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][30]}", toString.equals("{[#][20][30]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30));
        brl.insert(new AIL(35));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][30][35]}", toString.equals("{[#][20][30][35]}"));

        brl = new BehindRefList().append(new AIL(10)).append(null).append(null);
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][10]}", toString.equals("{[5][10]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(null);
        brl.insert(new AIL(10));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10]}", toString.equals("{[10]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(null);
        brl.insert(new AIL(35));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][35]}", toString.equals("{[10][35]}"));

        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30));
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][10][30]}", toString.equals("{[5][10][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30));
        brl.insert(new AIL(10));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][30]}", toString.equals("{[10][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30));
        brl.insert(new AIL(15));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][15][30]}", toString.equals("{[10][15][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30));
        brl.insert(new AIL(30));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][30]}", toString.equals("{[10][#][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30));
        brl.insert(new AIL(35));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][30][35]}", toString.equals("{[10][#][30][35]}"));

        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null);
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][10][20]}", toString.equals("{[5][10][20]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null);
        brl.insert(new AIL(10));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20]}", toString.equals("{[10][20]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null);
        brl.insert(new AIL(15));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][15][20]}", toString.equals("{[10][15][20]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null);
        brl.insert(new AIL(20));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20]}", toString.equals("{[10][20]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null);
        brl.insert(new AIL(35));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][35]}", toString.equals("{[10][20][35]}"));

        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30));
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][10][20][30]}", toString.equals("{[5][10][20][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30));
        brl.insert(new AIL(10));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30]}", toString.equals("{[10][20][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30));
        brl.insert(new AIL(15));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][15][20][30]}", toString.equals("{[10][15][20][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30));
        brl.insert(new AIL(20));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30]}", toString.equals("{[10][20][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30));
        brl.insert(new AIL(25));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][25][30]}", toString.equals("{[10][20][25][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30));
        brl.insert(new AIL(30));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30]}", toString.equals("{[10][20][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30));
        brl.insert(new AIL(35));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30][35]}", toString.equals("{[10][20][30][35]}"));

        brl = new BehindRefList().append(null).append(null).append(null).append(null);
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5]}", toString.equals("{[5]}"));

        brl = new BehindRefList().append(null).append(null).append(null).append(new AIL(40));
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][40]}", toString.equals("{[5][40]}"));
        brl = new BehindRefList().append(null).append(null).append(null).append(new AIL(40));
        brl.insert(new AIL(40));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#][#][40]}", toString.equals("{[#][#][#][40]}"));
        brl = new BehindRefList().append(null).append(null).append(null).append(new AIL(40));
        brl.insert(new AIL(45));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#][#][40][45]}", toString.equals("{[#][#][#][40][45]}"));

        brl = new BehindRefList().append(null).append(null).append(new AIL(30)).append(null);
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][30]}", toString.equals("{[5][30]}"));
        brl = new BehindRefList().append(null).append(null).append(new AIL(30)).append(null);
        brl.insert(new AIL(30));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#][30]}", toString.equals("{[#][#][30]}"));
        brl = new BehindRefList().append(null).append(null).append(new AIL(30)).append(null);
        brl.insert(new AIL(45));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#][30][45]}", toString.equals("{[#][#][30][45]}"));

        brl = new BehindRefList().append(null).append(null).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][30][40]}", toString.equals("{[5][30][40]}"));
        brl = new BehindRefList().append(null).append(null).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(30));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#][30][40]}", toString.equals("{[#][#][30][40]}"));
        brl = new BehindRefList().append(null).append(null).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(35));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#][30][35][40]}", toString.equals("{[#][#][30][35][40]}"));
        brl = new BehindRefList().append(null).append(null).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(40));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#][30][40]}", toString.equals("{[#][#][30][40]}"));
        brl = new BehindRefList().append(null).append(null).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(45));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#][30][40][45]}", toString.equals("{[#][#][30][40][45]}"));

        brl = new BehindRefList().append(null).append(new AIL(20)).append(null).append(null);
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][20]}", toString.equals("{[5][20]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(null).append(null);
        brl.insert(new AIL(20));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20]}", toString.equals("{[#][20]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(null).append(null);
        brl.insert(new AIL(45));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][45]}", toString.equals("{[#][20][45]}"));

        brl = new BehindRefList().append(null).append(new AIL(20)).append(null).append(new AIL(40));
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][20][40]}", toString.equals("{[5][20][40]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(null).append(new AIL(40));
        brl.insert(new AIL(20));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][40]}", toString.equals("{[#][20][40]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(null).append(new AIL(40));
        brl.insert(new AIL(25));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][25][40]}", toString.equals("{[#][20][25][40]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(null).append(new AIL(40));
        brl.insert(new AIL(40));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][#][40]}", toString.equals("{[#][20][#][40]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(null).append(new AIL(40));
        brl.insert(new AIL(45));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][#][40][45]}", toString.equals("{[#][20][#][40][45]}"));

        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30)).append(null);
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][20][30]}", toString.equals("{[5][20][30]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30)).append(null);
        brl.insert(new AIL(20));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][30]}", toString.equals("{[#][20][30]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30)).append(null);
        brl.insert(new AIL(25));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][25][30]}", toString.equals("{[#][20][25][30]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30)).append(null);
        brl.insert(new AIL(30));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][30]}", toString.equals("{[#][20][30]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30)).append(null);
        brl.insert(new AIL(45));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][30][45]}", toString.equals("{[#][20][30][45]}"));

        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][20][30][40]}", toString.equals("{[5][20][30][40]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(20));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][30][40]}", toString.equals("{[#][20][30][40]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(25));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][25][30][40]}", toString.equals("{[#][20][25][30][40]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(30));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][30][40]}", toString.equals("{[#][20][30][40]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(35));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][30][35][40]}", toString.equals("{[#][20][30][35][40]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(40));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][30][40]}", toString.equals("{[#][20][30][40]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(45));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][30][40][45]}", toString.equals("{[#][20][30][40][45]}"));

        brl = new BehindRefList().append(new AIL(10)).append(null).append(null).append(null);
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][10]}", toString.equals("{[5][10]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(null).append(null);
        brl.insert(new AIL(10));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10]}", toString.equals("{[10]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(null).append(null);
        brl.insert(new AIL(45));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][45]}", toString.equals("{[10][45]}"));

        brl = new BehindRefList().append(new AIL(10)).append(null).append(null).append(new AIL(40));
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][10][40]}", toString.equals("{[5][10][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(null).append(new AIL(40));
        brl.insert(new AIL(10));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][40]}", toString.equals("{[10][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(null).append(new AIL(40));
        brl.insert(new AIL(15));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][15][40]}", toString.equals("{[10][15][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(null).append(new AIL(40));
        brl.insert(new AIL(40));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][#][40]}", toString.equals("{[10][#][#][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(null).append(new AIL(40));
        brl.insert(new AIL(45));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][#][40][45]}", toString.equals("{[10][#][#][40][45]}"));

        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30)).append(null);
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][10][30]}", toString.equals("{[5][10][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30)).append(null);
        brl.insert(new AIL(10));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][30]}", toString.equals("{[10][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30)).append(null);
        brl.insert(new AIL(15));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][15][30]}", toString.equals("{[10][15][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30)).append(null);
        brl.insert(new AIL(30));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][30]}", toString.equals("{[10][#][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30)).append(null);
        brl.insert(new AIL(45));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][30][45]}", toString.equals("{[10][#][30][45]}"));

        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][10][30][40]}", toString.equals("{[5][10][30][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(10));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][30][40]}", toString.equals("{[10][30][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(15));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][15][30][40]}", toString.equals("{[10][15][30][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(30));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][30][40]}", toString.equals("{[10][#][30][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(35));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][30][35][40]}", toString.equals("{[10][#][30][35][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(40));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][30][40]}", toString.equals("{[10][#][30][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(45));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][30][40][45]}", toString.equals("{[10][#][30][40][45]}"));

        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null).append(null);
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][10][20]}", toString.equals("{[5][10][20]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null).append(null);
        brl.insert(new AIL(10));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20]}", toString.equals("{[10][20]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null).append(null);
        brl.insert(new AIL(15));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][15][20]}", toString.equals("{[10][15][20]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null).append(null);
        brl.insert(new AIL(20));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20]}", toString.equals("{[10][20]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null).append(null);
        brl.insert(new AIL(45));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][45]}", toString.equals("{[10][20][45]}"));

        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null).append(new AIL(40));
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][10][20][40]}", toString.equals("{[5][10][20][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null).append(new AIL(40));
        brl.insert(new AIL(10));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][40]}", toString.equals("{[10][20][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null).append(new AIL(40));
        brl.insert(new AIL(15));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][15][20][40]}", toString.equals("{[10][15][20][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null).append(new AIL(40));
        brl.insert(new AIL(20));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][40]}", toString.equals("{[10][20][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null).append(new AIL(40));
        brl.insert(new AIL(25));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][25][40]}", toString.equals("{[10][20][25][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null).append(new AIL(40));
        brl.insert(new AIL(40));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][#][40]}", toString.equals("{[10][20][#][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null).append(new AIL(40));
        brl.insert(new AIL(45));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][#][40][45]}", toString.equals("{[10][20][#][40][45]}"));

        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(null);
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][10][20][30]}", toString.equals("{[5][10][20][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(null);
        brl.insert(new AIL(10));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30]}", toString.equals("{[10][20][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(null);
        brl.insert(new AIL(15));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][15][20][30]}", toString.equals("{[10][15][20][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(null);
        brl.insert(new AIL(20));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30]}", toString.equals("{[10][20][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(null);
        brl.insert(new AIL(25));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][25][30]}", toString.equals("{[10][20][25][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(null);
        brl.insert(new AIL(30));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30]}", toString.equals("{[10][20][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(null);
        brl.insert(new AIL(45));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30][45]}", toString.equals("{[10][20][30][45]}"));

        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(5));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[5][10][20][30][40]}", toString.equals("{[5][10][20][30][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(10));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30][40]}", toString.equals("{[10][20][30][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(15));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][15][20][30][40]}", toString.equals("{[10][15][20][30][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(20));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30][40]}", toString.equals("{[10][20][30][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(25));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][25][30][40]}", toString.equals("{[10][20][25][30][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(30));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30][40]}", toString.equals("{[10][20][30][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(35));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30][35][40]}", toString.equals("{[10][20][30][35][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(40));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30][40]}", toString.equals("{[10][20][30][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        brl.insert(new AIL(45));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30][40][45]}", toString.equals("{[10][20][30][40][45]}"));
    }

    public final void testAppend()
    {
        BehindRefList brl;
        String toString;

        // GEN
        brl = new BehindRefList();
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {}", toString.equals("{}"));

        // GEN
        brl = new BehindRefList().append(null);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#]}", toString.equals("{[#]}"));
        brl = new BehindRefList().append(new AIL(10));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10]}", toString.equals("{[10]}"));

        // GEN
        brl = new BehindRefList().append(null).append(null);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#]}", toString.equals("{[#][#]}"));
        brl = new BehindRefList().append(null).append(new AIL(20));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20]}", toString.equals("{[#][20]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#]}", toString.equals("{[10][#]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20]}", toString.equals("{[10][20]}"));

        // GEN
        brl = new BehindRefList().append(null).append(null).append(null);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#][#]}", toString.equals("{[#][#][#]}"));
        brl = new BehindRefList().append(null).append(null).append(new AIL(30));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#][30]}", toString.equals("{[#][#][30]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(null);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][#]}", toString.equals("{[#][20][#]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][30]}", toString.equals("{[#][20][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(null);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][#]}", toString.equals("{[10][#][#]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][30]}", toString.equals("{[10][#][30]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][#]}", toString.equals("{[10][20][#]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30]}", toString.equals("{[10][20][30]}"));

        // GEN
        brl = new BehindRefList().append(null).append(null).append(null).append(null);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#][#][#]}", toString.equals("{[#][#][#][#]}"));
        brl = new BehindRefList().append(null).append(null).append(null).append(new AIL(40));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#][#][40]}", toString.equals("{[#][#][#][40]}"));
        brl = new BehindRefList().append(null).append(null).append(new AIL(30)).append(null);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#][30][#]}", toString.equals("{[#][#][30][#]}"));
        brl = new BehindRefList().append(null).append(null).append(new AIL(30)).append(new AIL(40));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#][30][40]}", toString.equals("{[#][#][30][40]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(null).append(null);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][#][#]}", toString.equals("{[#][20][#][#]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(null).append(new AIL(40));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][#][40]}", toString.equals("{[#][20][#][40]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30)).append(null);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][30][#]}", toString.equals("{[#][20][30][#]}"));
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][20][30][40]}", toString.equals("{[#][20][30][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(null).append(null);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][#][#]}", toString.equals("{[10][#][#][#]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(null).append(new AIL(40));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][#][40]}", toString.equals("{[10][#][#][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30)).append(null);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][30][#]}", toString.equals("{[10][#][30][#]}"));
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30)).append(new AIL(40));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][30][40]}", toString.equals("{[10][#][30][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null).append(null);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][#][#]}", toString.equals("{[10][20][#][#]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null).append(new AIL(40));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][#][40]}", toString.equals("{[10][20][#][40]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(null);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30][#]}", toString.equals("{[10][20][30][#]}"));
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30][40]}", toString.equals("{[10][20][30][40]}"));
    }

    public final void testGetFirstNoRemove()
    {
        BehindRefList brl;
        AbstractItemLink ail;
        String toString;

        // GEN
        brl = new BehindRefList();
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {}", toString.equals("{}"));
        assertNull(ail);

        // GEN
        brl = new BehindRefList().append(null);
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {}", toString.equals("{}"));
        assertNull(ail);
        brl = new BehindRefList().append(new AIL(10));
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10]}", toString.equals("{[10]}"));
        assertEquals(ail.getSequence(), 10);

        // GEN
        brl = new BehindRefList().append(null).append(null);
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {}", toString.equals("{}"));
        assertNull(ail);
        brl = new BehindRefList().append(null).append(new AIL(20));
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[20]}", toString.equals("{[20]}"));
        assertEquals(ail.getSequence(), 20L);
        brl = new BehindRefList().append(new AIL(10)).append(null);
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#]}", toString.equals("{[10][#]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20));
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20]}", toString.equals("{[10][20]}"));
        assertEquals(ail.getSequence(), 10L);

        // GEN
        brl = new BehindRefList().append(null).append(null).append(null);
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {}", toString.equals("{}"));
        assertNull(ail);
        brl = new BehindRefList().append(null).append(null).append(new AIL(30));
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[30]}", toString.equals("{[30]}"));
        assertEquals(ail.getSequence(), 30L);
        brl = new BehindRefList().append(null).append(new AIL(20)).append(null);
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[20][#]}", toString.equals("{[20][#]}"));
        assertEquals(ail.getSequence(), 20L);
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30));
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[20][30]}", toString.equals("{[20][30]}"));
        assertEquals(ail.getSequence(), 20L);
        brl = new BehindRefList().append(new AIL(10)).append(null).append(null);
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][#]}", toString.equals("{[10][#][#]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30));
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][30]}", toString.equals("{[10][#][30]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null);
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][#]}", toString.equals("{[10][20][#]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30));
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30]}", toString.equals("{[10][20][30]}"));
        assertEquals(ail.getSequence(), 10L);

        // GEN
        brl = new BehindRefList().append(null).append(null).append(null).append(null);
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {}", toString.equals("{}"));
        assertNull(ail);
        brl = new BehindRefList().append(null).append(null).append(null).append(new AIL(40));
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[40]}", toString.equals("{[40]}"));
        assertEquals(ail.getSequence(), 40L);
        brl = new BehindRefList().append(null).append(null).append(new AIL(30)).append(null);
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[30][#]}", toString.equals("{[30][#]}"));
        assertEquals(ail.getSequence(), 30L);
        brl = new BehindRefList().append(null).append(null).append(new AIL(30)).append(new AIL(40));
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[30][40]}", toString.equals("{[30][40]}"));
        assertEquals(ail.getSequence(), 30L);
        brl = new BehindRefList().append(null).append(new AIL(20)).append(null).append(null);
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[20][#][#]}", toString.equals("{[20][#][#]}"));
        assertEquals(ail.getSequence(), 20L);
        brl = new BehindRefList().append(null).append(new AIL(20)).append(null).append(new AIL(40));
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[20][#][40]}", toString.equals("{[20][#][40]}"));
        assertEquals(ail.getSequence(), 20L);
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30)).append(null);
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[20][30][#]}", toString.equals("{[20][30][#]}"));
        assertEquals(ail.getSequence(), 20L);
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[20][30][40]}", toString.equals("{[20][30][40]}"));
        assertEquals(ail.getSequence(), 20L);
        brl = new BehindRefList().append(new AIL(10)).append(null).append(null).append(null);
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][#][#]}", toString.equals("{[10][#][#][#]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(null).append(null).append(new AIL(40));
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][#][40]}", toString.equals("{[10][#][#][40]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30)).append(null);
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][30][#]}", toString.equals("{[10][#][30][#]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30)).append(new AIL(40));
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][#][30][40]}", toString.equals("{[10][#][30][40]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null).append(null);
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][#][#]}", toString.equals("{[10][20][#][#]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null).append(new AIL(40));
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][#][40]}", toString.equals("{[10][20][#][40]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(null);
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30][#]}", toString.equals("{[10][20][30][#]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        ail = brl.getFirst(false);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[10][20][30][40]}", toString.equals("{[10][20][30][40]}"));
        assertEquals(ail.getSequence(), 10L);
    }

    public final void testGetFirstRemove()
    {
        BehindRefList brl;
        AbstractItemLink ail;
        String toString;

        // GEN
        brl = new BehindRefList();
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {}", toString.equals("{}"));
        assertNull(ail);

        // GEN
        brl = new BehindRefList().append(null);
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {}", toString.equals("{}"));
        assertNull(ail);
        brl = new BehindRefList().append(new AIL(10));
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {}", toString.equals("{}"));
        assertEquals(ail.getSequence(), 10L);

        // GEN
        brl = new BehindRefList().append(null).append(null);
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {}", toString.equals("{}"));
        assertNull(ail);
        brl = new BehindRefList().append(null).append(new AIL(20));
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {}", toString.equals("{}"));
        assertEquals(ail.getSequence(), 20L);
        brl = new BehindRefList().append(new AIL(10)).append(null);
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#]}", toString.equals("{[#]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20));
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[20]}", toString.equals("{[20]}"));
        assertEquals(ail.getSequence(), 10L);

        // GEN
        brl = new BehindRefList().append(null).append(null).append(null);
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {}", toString.equals("{}"));
        assertNull(ail);
        brl = new BehindRefList().append(null).append(null).append(new AIL(30));
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {}", toString.equals("{}"));
        assertEquals(ail.getSequence(), 30L);
        brl = new BehindRefList().append(null).append(new AIL(20)).append(null);
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#]}", toString.equals("{[#]}"));
        assertEquals(ail.getSequence(), 20L);
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30));
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[30]}", toString.equals("{[30]}"));
        assertEquals(ail.getSequence(), 20L);
        brl = new BehindRefList().append(new AIL(10)).append(null).append(null);
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#]}", toString.equals("{[#][#]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30));
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][30]}", toString.equals("{[#][30]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null);
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[20][#]}", toString.equals("{[20][#]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30));
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[20][30]}", toString.equals("{[20][30]}"));
        assertEquals(ail.getSequence(), 10L);

        // GEN
        brl = new BehindRefList().append(null).append(null).append(null).append(null);
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {}", toString.equals("{}"));
        assertNull(ail);
        brl = new BehindRefList().append(null).append(null).append(null).append(new AIL(40));
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {}", toString.equals("{}"));
        assertEquals(ail.getSequence(), 40L);
        brl = new BehindRefList().append(null).append(null).append(new AIL(30)).append(null);
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#]}", toString.equals("{[#]}"));
        assertEquals(ail.getSequence(), 30L);
        brl = new BehindRefList().append(null).append(null).append(new AIL(30)).append(new AIL(40));
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[40]}", toString.equals("{[40]}"));
        assertEquals(ail.getSequence(), 30L);
        brl = new BehindRefList().append(null).append(new AIL(20)).append(null).append(null);
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#]}", toString.equals("{[#][#]}"));
        assertEquals(ail.getSequence(), 20L);
        brl = new BehindRefList().append(null).append(new AIL(20)).append(null).append(new AIL(40));
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][40]}", toString.equals("{[#][40]}"));
        assertEquals(ail.getSequence(), 20L);
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30)).append(null);
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[30][#]}", toString.equals("{[30][#]}"));
        assertEquals(ail.getSequence(), 20L);
        brl = new BehindRefList().append(null).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[30][40]}", toString.equals("{[30][40]}"));
        assertEquals(ail.getSequence(), 20L);
        brl = new BehindRefList().append(new AIL(10)).append(null).append(null).append(null);
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#][#]}", toString.equals("{[#][#][#]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(null).append(null).append(new AIL(40));
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][#][40]}", toString.equals("{[#][#][40]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30)).append(null);
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][30][#]}", toString.equals("{[#][30][#]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(null).append(new AIL(30)).append(new AIL(40));
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[#][30][40]}", toString.equals("{[#][30][40]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null).append(null);
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[20][#][#]}", toString.equals("{[20][#][#]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(null).append(new AIL(40));
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[20][#][40]}", toString.equals("{[20][#][40]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(null);
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[20][30][#]}", toString.equals("{[20][30][#]}"));
        assertEquals(ail.getSequence(), 10L);
        brl = new BehindRefList().append(new AIL(10)).append(new AIL(20)).append(new AIL(30)).append(new AIL(40));
        ail = brl.getFirst(true);
        toString = brl.toString();
        assertTrue("BRL was: " + toString + ", should be: {[20][30][40]}", toString.equals("{[20][30][40]}"));
        assertEquals(ail.getSequence(), 10L);
    }

    public static class AIL extends AbstractItemLink
    {
        long _sequence; // Both position and sequence in this test which doesn't use LinkedLists

        public AIL(long sequence)
        {
            _sequence = sequence;
        }

        @Override
        public String xmlTagName()
        {
            return "AIL";
        }

        @Override
        public long getPosition()
        {
            return _sequence;
        }

        @Override
        public long getSequence()
        {
            return _sequence;
        }

        @Override
        public String toString()
        {
            return Long.toString(_sequence);
        }
    }

}
