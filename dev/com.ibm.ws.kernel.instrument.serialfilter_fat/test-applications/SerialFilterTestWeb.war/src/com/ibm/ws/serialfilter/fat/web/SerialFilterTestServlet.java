/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.serialfilter.fat.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.serialfilter.fat.object.allowed.Test1;
import com.ibm.ws.serialfilter.fat.object.allowed.Test2;
import com.ibm.ws.serialfilter.fat.object.denied.Test3;

import componenttest.app.FATServlet;

/**
 */
@WebServlet("/SerialFilterTestServlet")
public class SerialFilterTestServlet extends FATServlet {
    /**
     *
     */
    public void AllAllowed(HttpServletRequest request,
                           HttpServletResponse response) throws Exception {
        System.out.println("AllAllowed");
        testDeserialize(true, true, true);
    }

    public void Test1Allowed(HttpServletRequest request,
                             HttpServletResponse response) throws Exception {
        System.out.println("Test1Allowed");
        testDeserialize(true, false, false);
    }

    public void Test1And2Allowed(HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        System.out.println("Test1And2Allowed");
        testDeserialize(true, true, false);
    }

    public void TestAllAllowed(HttpServletRequest request,
                               HttpServletResponse response) throws Exception {
        System.out.println("TestAllAllowed");
        testDeserialize(true, true, true);
    }

    public void AllDenied(HttpServletRequest request,
                          HttpServletResponse response) throws Exception {
        System.out.println("AllDenied");
        testDeserialize(false, false, false);
    }

    public void ProhibitedCaller(HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        System.out.println("ProhibitedCaller");
        Test1 t1 = new Test1(10, "test1object");
        ByteArrayInputStream ba = new ByteArrayInputStream(serialize(t1));
        ObjectInputStream in = new ObjectInputStream(ba);

        try {
            in.readObject();
            throw new Exception("ProhibitedCaller: InvalidClassException should be caught.");
        } catch (InvalidClassException e) {
            System.out.println("ProhibitedCaller: InvalidClassException is caught which is expected.");

        } finally {
            in.close();
            ba.close();
        }
        return;
    }

    private void testDeserialize(boolean test1Allowed, boolean test2Allowed, boolean test3Allowed) throws Exception {
        testDeserializeTest1(test1Allowed);
        testDeserializeTest2(test2Allowed);
        testDeserializeTest3(test3Allowed);
    }

    private void testDeserializeTest1(boolean allowed) throws Exception {
        Test1 t1 = new Test1(10, "test1object");
        byte[] sObj = serialize(t1);
        try {
            Test1 t2 = (Test1) deserialize(sObj);
            if (!allowed) {
                throw new Exception("test1 deserialization should fail.");
            }
            System.out.println("test1 : getInt : " + t2.getInt() + ", getString : " + t2.getString());
        } catch (InvalidClassException ice) {
            System.out.println("Test1 InvalidClassException is caught. allowed : " + allowed);
            if (allowed) {
                throw new Exception("test1 deserialization should succeed.");
            }
        }
    }

    private void testDeserializeTest2(boolean allowed) throws Exception {
        Test2 t1 = new Test2(20, "test2object");
        byte[] sObj = serialize(t1);
        try {
            Test2 t2 = (Test2) deserialize(sObj);
            if (!allowed) {
                throw new Exception("test2 deserialization should fail.");
            }
            System.out.println("test2 : getInt : " + t2.getInt() + ", getString : " + t2.getString());
        } catch (InvalidClassException ice) {
            System.out.println("Test2 InvalidClassException is caught. allowed : " + allowed);
            if (allowed) {
                throw new Exception("test2 deserialization should succeed.");
            }
        }
    }

    private void testDeserializeTest3(boolean allowed) throws Exception {
        Test3 t1 = new Test3(30, "test3object");
        byte[] sObj = serialize(t1);
        try {
            Test3 t2 = (Test3) deserialize(sObj);
            if (!allowed) {
                throw new Exception("test3 deserialization should fail.");
            }
            System.out.println("test3 : getInt : " + t2.getInt() + ", getString : " + t2.getString());
        } catch (InvalidClassException ice) {
            System.out.println("Test3 InvalidClassException is caught. allowed : " + allowed);
            if (allowed) {
                throw new Exception("test3 deserialization should succeed.");
            }
        }
    }

    private byte[] serialize(Object obj) throws Exception {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(ba);
        out.writeObject(obj);
        out.close();
        return ba.toByteArray();
    }

    private Object deserialize(byte[] input) throws Exception {
        ByteArrayInputStream ba = new ByteArrayInputStream(input);
        ObjectInputStream in = new ObjectInputStream(ba);

        Object obj = in.readObject();

        in.close();
        ba.close();
        return obj;
    }
}