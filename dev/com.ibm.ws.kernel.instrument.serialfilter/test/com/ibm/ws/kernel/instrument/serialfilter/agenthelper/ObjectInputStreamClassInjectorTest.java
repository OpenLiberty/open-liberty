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
package com.ibm.ws.kernel.instrument.serialfilter.agenthelper;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.*;
import java.lang.reflect.Method;

import static org.objectweb.asm.ClassReader.SKIP_DEBUG;
import static org.objectweb.asm.ClassReader.SKIP_FRAMES;

public class ObjectInputStreamClassInjectorTest {

    private static Method TRANSFORM_METHOD;

    @BeforeClass
    public static void setupTransformMethod() throws Exception {
        TRANSFORM_METHOD = ObjectInputStreamTransformer.class.getDeclaredMethod("transformObjectInputStreamClass", byte[].class);
        TRANSFORM_METHOD.setAccessible(true);
    }

    @Test
    public void testTransformationChangesObjectInputStream() throws Exception {
        byte[] beforeBytes = getObjectInputStreamClassBytes();
        printAsm("build/Before.java", new ClassReader(beforeBytes));
        printBytes("build/Before.class", beforeBytes);
        String before = getAsm(new ClassReader(beforeBytes));
        byte[] afterBytes = (byte[]) TRANSFORM_METHOD.invoke(null, beforeBytes);
        printAsm("build/After.java", new ClassReader(afterBytes));
        printBytes("build/After.class", afterBytes);
        String after = getAsm(new ClassReader(afterBytes));
        Assert.assertTrue("After image should have more (injected) content than before image", after.length() > before.length());
    }

    private static byte[] getObjectInputStreamClassBytes() throws Exception {
        InputStream in = ObjectInputStream.class.getResourceAsStream("ObjectInputStream.class");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int numBytes;
        while (-1 != (numBytes = in.read(buffer))) {
            out.write(buffer, 0, numBytes);
        }
        in.close();
        out.close();
        return out.toByteArray();
    }

    private static void printAsm(String filename, ClassReader cr) throws Exception {
        FileWriter fw = new FileWriter(filename);
        PrintWriter pw = new PrintWriter(fw);
        cr.accept(new TraceClassVisitor(null, new ASMifier(), pw), SKIP_DEBUG + SKIP_FRAMES);
    }

    private static void printBytes(String filename, byte[] bytes) throws Exception {
        FileOutputStream fos =  new FileOutputStream(filename);
        fos.write(bytes);
        fos.close();
    }

    private static String getAsm(ClassReader cr) throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        cr.accept(new TraceClassVisitor(null, new ASMifier(), pw), SKIP_DEBUG + SKIP_FRAMES);
        return sw.toString();
    }
}
