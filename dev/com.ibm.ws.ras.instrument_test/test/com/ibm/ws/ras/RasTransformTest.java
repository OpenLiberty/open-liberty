/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ras;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

import com.ibm.ws.ras.instrument.internal.main.LibertyRuntimeTransformer;

//TODO add tests that make sure classes are instrumented with trace.
@SuppressWarnings("deprecation")
public class RasTransformTest extends LibertyRuntimeTransformer {
	
	private StaticTraceTestTransformer staticTransform = new StaticTraceTestTransformer();
	
	private static final HashMap<String, Integer> majorCodeMap = new HashMap<>();
	
	@BeforeClass
	public static void setup() {
        //Adapted from https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-4.html#jvms-4.1
        majorCodeMap.put("1.0.2", 45);
        majorCodeMap.put("1.1", 45);
        majorCodeMap.put("1.2", 46);
        majorCodeMap.put("1.3", 47);
        majorCodeMap.put("1.4", 48);
        majorCodeMap.put("5.0", 49);
        majorCodeMap.put("6", 50);
        majorCodeMap.put("7", 51);
        majorCodeMap.put("8", 52);
        majorCodeMap.put("9", 53);
        majorCodeMap.put("10", 54);
        majorCodeMap.put("11", 55);
        majorCodeMap.put("12", 56);
        majorCodeMap.put("13", 57);
        majorCodeMap.put("14", 58);
        majorCodeMap.put("15", 59);
        majorCodeMap.put("16", 60);
        majorCodeMap.put("17", 61);
        majorCodeMap.put("18", 62);
        majorCodeMap.put("19", 63);
        majorCodeMap.put("20", 64);
        majorCodeMap.put("21", 65);
        majorCodeMap.put("22", 66);
	}
	
	/**
	 * Ensure the class file is compiled at the correct byte code version
	 * 
	 * @param filename - path to class file to be loaded
	 * @param expected - the expected major version code
	 * @return true if the class file is compiled to the correct byte code version, false otherwise.
	 */
	private boolean assertByteCodeVersion(String filename, int expected) {
        try ( DataInputStream in = new DataInputStream(new FileInputStream(filename)) ) {
            int magic = in.readInt();

            if(magic != 0xcafebabe) {
                System.out.println(filename + " is not a valid class!");
                return false;
            }

            int minor = in.readUnsignedShort();
            int major = in.readUnsignedShort();

            System.out.println("Major version: " + major);
            System.out.println("Minor version: " + minor);

            if(expected != major) {
                System.out.println("Expected major version of " + expected + " but got " + major);
                return false;
            }
            
            return true;
        } catch (IOException e) {
        	e.printStackTrace();
        	return false;
        }
	}
	
	private Class<?> makeAClassFromBytes(String name, byte[] classBytes, String packageName) throws ClassNotFoundException {
		Map<String, byte[]> classDefs = new HashMap<String, byte[]>();
		classDefs.put(name, classBytes);
		
		ByteArrayClassLoader testLoader = new ByteArrayClassLoader(getClass().getClassLoader(), classDefs);
		testLoader.setPackage(packageName);
		
		
		Class<?> loadedClass = testLoader.loadClass(name); 
		
		
		try {
			testLoader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return loadedClass;
	}
	
	private byte[] makeBytesFromFile(String path) {
		File clazz = new File(path);
		byte[] data = null;
		
		try {
			FileInputStream fis = new FileInputStream(clazz);
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	
			int chunk;
			data = new byte[(int) clazz.length()];
			while ((chunk = fis.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, chunk);
			}
		
			data = buffer.toByteArray();
			
			fis.close();
		} catch (IOException e) {

		}
		
		return data;
	}
	
	private String dumpClassViaASM(byte[] classBytes) {
        StringWriter sw = null;
        sw = new StringWriter();
        ClassReader reader = new ClassReader(classBytes);
        ClassNode directory = new ClassNode(Opcodes.ASM9);
        ClassVisitor visitor = new TraceClassVisitor(directory, new PrintWriter(sw));
        
        try {
            reader.accept(visitor, ClassReader.SKIP_DEBUG);
            
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return sw.toString();
	}
	
	public String unrollStack(StackTraceElement[] trace) {
		StringBuilder traceString = new StringBuilder();
		
		for (StackTraceElement element : trace) {
			traceString.append(element.toString() + '\n');
		}
		
		return traceString.toString();
	}
	
	@Test
	public void testJava6BasicInjection() {
		assumeTrue(Integer.parseInt(System.getProperty("java.specification.version")) >= 6);
		String classPath = "test/test data/com/ibm/example/bytecode/HelloWorldJava6.class";
		assertTrue(assertByteCodeVersion(classPath, majorCodeMap.get("6")));
		
		try {
			String classUnderTest = "com.ibm.example.bytecode.HelloWorldJava6";
			Class<?> transformedClass = transformClass(classUnderTest, classPath);
			
			transformedClass.newInstance();		
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	
	@Test
	public void testJava7BasicInjection() {
		assumeTrue(Integer.parseInt(System.getProperty("java.specification.version")) >= 7);
		String classPath = "test/test data/com/ibm/example/bytecode/HelloWorldJava7.class";
		assertTrue(assertByteCodeVersion(classPath, majorCodeMap.get("7")));
		
		try {
			String classUnderTest = "com.ibm.example.bytecode.HelloWorldJava7";
			Class<?> transformedClass = transformClass(classUnderTest, classPath);
			
			transformedClass.newInstance();		
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	
	@Test
	public void testJava7StaticInit() {
		assumeTrue(Integer.parseInt(System.getProperty("java.specification.version")) >= 7);
		String classPath = "test/test data/com/ibm/example/bytecode/HelloWorldJava7StaticInit.class";
		assertTrue(assertByteCodeVersion(classPath, majorCodeMap.get("7")));
		
		try {
			String classUnderTest = "com.ibm.example.bytecode.HelloWorldJava7StaticInit";
			Class<?> transformedClass = transformClass(classUnderTest, classPath);
			
			transformedClass.newInstance();		
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	
	
	@Test
	public void testJava8Basic() {
		assumeTrue(Integer.parseInt(System.getProperty("java.specification.version")) >= 8);
		String classPath = "test/test data/com/ibm/example/bytecode/HelloWorldJava8.class";
		assertTrue(assertByteCodeVersion(classPath, majorCodeMap.get("8")));
		
		try {
			String classUnderTest = "com.ibm.example.bytecode.HelloWorldJava8";
			Class<?> transformedClass = transformClass(classUnderTest, classPath);
			
			transformedClass.newInstance();		
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	
	@Test
	public void testJava8Lambdas() {
		assumeTrue(Integer.parseInt(System.getProperty("java.specification.version")) >= 8);
		String classPath = "test/test data/com/ibm/example/bytecode/HelloWorldJava8Lambdas.class";
		assertTrue(assertByteCodeVersion(classPath, majorCodeMap.get("8")));
		
		try {
			String classUnderTest = "com.ibm.example.bytecode.HelloWorldJava8Lambdas";
			Class<?> transformedClass = transformClass(classUnderTest, classPath);
			
			transformedClass.newInstance();		
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	
	@Test
	public void testJava8StaticInit() {
		assumeTrue(Integer.parseInt(System.getProperty("java.specification.version")) >= 8);
		String classPath = "test/test data/com/ibm/example/bytecode/HelloWorldJava8StaticInit.class";
		assertTrue(assertByteCodeVersion(classPath, majorCodeMap.get("8")));
		
		try {
			String classUnderTest = "com.ibm.example.bytecode.HelloWorldJava8StaticInit";
			Class<?> transformedClass = transformClass(classUnderTest, classPath);
			
			transformedClass.newInstance();		
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	
//	@Test TODO this test is failing
	public void testJava8StaticInitLambdasWithInnerClass() {
		assumeTrue(Integer.parseInt(System.getProperty("java.specification.version")) >= 8);
		String classPath = "test/test data/com/ibm/example/bytecode/HelloWorldJava8StaticLambdas.class";
		assertTrue(assertByteCodeVersion(classPath, majorCodeMap.get("8")));
		
		try {
			String classUnderTest = "com.ibm.example.bytecode.HelloWorldJava8StaticLambdas";
			String classUnderTestInnerClass = "com.ibm.example.bytecode.HelloWorldJava8StaticLambdas$Converter";

			byte[] preTransformBytes = makeBytesFromFile(classPath);
			byte[] preTransformBytesInnerClass = makeBytesFromFile(classPath + "$Converter.class");
			
			Map<String, byte[]> classDefsPreTransform = new HashMap<String, byte[]>();
			classDefsPreTransform.put(classUnderTest, preTransformBytes);
			classDefsPreTransform.put(classUnderTestInnerClass, preTransformBytesInnerClass);
			
			ByteArrayClassLoader testLoaderPreTransform = new ByteArrayClassLoader(getClass().getClassLoader(), classDefsPreTransform);
			testLoaderPreTransform.setPackage("com.ibm.example.bytecode");
			
			Class<?> loadedClassPreTransform = testLoaderPreTransform.loadClass(classUnderTest); 
			loadedClassPreTransform.newInstance();
			
			byte[] classUnderTestBytes = transformedClassBytes(classUnderTest, classPath, preTransformBytes);
			byte[] classUnderTestBytesInnerClass = transformedClassBytes(classUnderTestInnerClass, classPath + "$Converter.class", preTransformBytesInnerClass);
			
			Map<String, byte[]> classDefs = new HashMap<String, byte[]>();
			classDefs.put(classUnderTest, classUnderTestBytes);
			classDefs.put(classUnderTestInnerClass, classUnderTestBytesInnerClass);
			
			ByteArrayClassLoader testLoader = new ByteArrayClassLoader(getClass().getClassLoader(), classDefs);
			testLoader.setPackage("com.ibm.example.bytecode");
			Class<?> loadedClass = testLoader.loadClass(classUnderTest); 
			
			loadedClass.newInstance();

			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	
	@Test
	public void testJava8LambdaInjection() {
		assumeTrue(Integer.parseInt(System.getProperty("java.specification.version")) >= 8);
		String classPath = "test/test data/com/ibm/example/bytecode/HelloWorldJava8Lambdas.class";
		assertTrue(assertByteCodeVersion(classPath, majorCodeMap.get("8")));
		
		try {
			String classUnderTest = "com.ibm.example.bytecode.HelloWorldJava8Lambdas";
			Class<?> transformedClass = transformClass(classUnderTest, classPath);
			
			transformedClass.newInstance();		
			
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	
	@Test //TODO expand this test to actually test Java 11 features https://openjdk.org/jeps/323
	public void testJava11Basic() {
		assumeTrue(Integer.parseInt(System.getProperty("java.specification.version")) >= 11);
		String classPath = "test/test data/com/ibm/example/bytecode/HelloWorldJava11.class";
		assertTrue(assertByteCodeVersion(classPath, majorCodeMap.get("11")));
		
		try {
			String classUnderTest = "com.ibm.example.bytecode.HelloWorldJava11";
			Class<?> transformedClass = transformClass(classUnderTest, classPath);
			
			transformedClass.newInstance();		
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	
	@Test //TODO expand this test to actually test Java 17 features https://openjdk.org/jeps/409
	public void testJava17Basic() {
		assumeTrue(Integer.parseInt(System.getProperty("java.specification.version")) >= 17);
		String classPath = "test/test data/com/ibm/example/bytecode/HelloWorldJava17.class";
		assertTrue(assertByteCodeVersion(classPath, majorCodeMap.get("17")));
		
		try {
			String classUnderTest = "com.ibm.example.bytecode.HelloWorldJava17";
			Class<?> transformedClass = transformClass(classUnderTest, classPath);
			
			transformedClass.newInstance();		
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	
	//TODO expand this test to actually test Java 21 features https://openjdk.org/jeps/441
	@Test 
	public void testJava21Basic() {
		assumeTrue(Integer.parseInt(System.getProperty("java.specification.version")) >= 21);
		String classPath = "test/test data/com/ibm/example/bytecode/HelloWorldJava21.class";
		assertTrue(assertByteCodeVersion(classPath, majorCodeMap.get("21")));
		
		try {
			String classUnderTest = "com.ibm.example.bytecode.HelloWorldJava21";
			Class<?> transformedClass = transformClass(classUnderTest, classPath);
			
			transformedClass.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}

	
	private void writeStringToFile(String toWrite, String path) {
		File dir = new File(path).getParentFile();
		dir.mkdirs();
		
		try (PrintWriter file = new PrintWriter(path)) {
			file.println(toWrite);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	//turn this on to get detailed ASM output in the project folder.
	public final boolean dumpTrace = false;
	
	private byte[] transformedClassBytes(String className, String path, byte[] pre) {
		byte[] transformedClass = null;

		try {
			//LibertyRuntimeTransformer.setInjectAtTransform(false);
			
			byte[] staticInject = staticTransform.transform(className, pre);
			byte[] postDynamic = LibertyRuntimeTransformer.transform(pre, false);
			
			//Case that tries to fully emulate what the runtime ends up doing.
			if (staticInject == null) {
				staticInject = pre;
			}
			byte[] post = LibertyRuntimeTransformer.transform(staticInject, false);
			
			String preTransform = dumpClassViaASM(pre);
			//System.out.println("Pre transform class: " + preTransform);
			
			String postStaticTransform = dumpClassViaASM(staticInject);
			//System.out.println("Static transform class: " + postStaticTransform);
			
			String postDynamicTransform = dumpClassViaASM(postDynamic);
			//System.out.println("Post transform class: " + postTransform);
			
			String postStaticAndDynamicTransform = dumpClassViaASM(post);
			
			if (dumpTrace) {
				writeStringToFile(preTransform, "build/results/Debug-Pre-"+className);
				writeStringToFile(postStaticTransform, "build/results/Debug-Static-"+className);
				writeStringToFile(postDynamicTransform, "build/results/Debug-PostDyn-"+className);
				writeStringToFile(postStaticAndDynamicTransform, "build/results/Debug-PostStaticDyn-"+className);
			}
			transformedClass = post;
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} 
		
		return transformedClass;
		
	}
	
	private Class<?> transformClass(String className, String path) {
		Class<?> transformedClass = null;

		try {
			byte[] pre = makeBytesFromFile(path);
			assertTrue("isTransformPossible returned false, likely the bytecode level is not supported yet and needs to be tested", 
					invokeIsTransformPossible(pre));
			transformedClass = makeAClassFromBytes(className, transformedClassBytes(className, path, pre), "com.ibm.example.bytecode");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} 
		
		return transformedClass;
	}
	
	/**
	 * Invokes LibertyRuntimeTransformer.isTransformPossible() which is a private method not accessable from child class.
	 * 
	 * @return the result of invoking the method
	 * @throws Exception if method cannot be invoked
	 */
	private boolean invokeIsTransformPossible(byte[] bytes) throws Exception {
		Method isTransformPossible = LibertyRuntimeTransformer.class.getDeclaredMethod("isTransformPossible", byte[].class);
		isTransformPossible.setAccessible(true);
		return (isTransformPossible.invoke(null, bytes) == null);
	}
	
}

