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

package com.ibm.ws.ras;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

import com.ibm.ws.ras.instrument.internal.main.LibertyRuntimeTransformer;

public class RasTransformTest extends LibertyRuntimeTransformer {
	
	private StaticTraceTestTransformer staticTransform = new StaticTraceTestTransformer();
	
	private Class makeAClassFromBytes(String name, byte[] classBytes, String packageName) throws ClassNotFoundException {
		Map<String, byte[]> classDefs = new HashMap<String, byte[]>();
		classDefs.put(name, classBytes);
		
		ByteArrayClassLoader testLoader = new ByteArrayClassLoader(getClass().getClassLoader(), classDefs);
		testLoader.setPackage(packageName);
		
		
		Class loadedClass = testLoader.loadClass(name); 
		
		
		try {
			testLoader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
        ClassNode directory = new ClassNode(Opcodes.ASM7);
        ClassVisitor visitor = new TraceClassVisitor(directory, new PrintWriter(sw));
        
        try {
            reader.accept(visitor, ClassReader.SKIP_DEBUG);
            
        } catch (Throwable t) {
            IOException ioe = new IOException("Unable to trace class data: " + t.getMessage(), t);
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
		try {
			String classUnderTest = "com.ibm.example.bytecode.HelloWorldJava6";
			Class transformedClass = transformClass(classUnderTest, "test/test data/com/ibm/example/bytecode/HelloWorldJava6.class");
			
			transformedClass.newInstance();		
		} catch (Exception e) {
			System.out.println("Exception: " + e.getStackTrace().toString());
		}
		
	}
	
	@Test
	public void testJava7BasicInjection() {
		try {
			String classUnderTest = "com.ibm.example.bytecode.HelloWorldJava7";
			Class transformedClass = transformClass(classUnderTest, "test/test data/com/ibm/example/bytecode/HelloWorldJava7.class");
			
			transformedClass.newInstance();		
		} catch (Exception e) {
			System.out.println("Exception: " + e.getStackTrace().toString());
		}
		
	}
	
	@Test
	public void testJava7StaticInit() {
		try {
			String classUnderTest = "com.ibm.example.bytecode.HelloWorldJava7StaticInit";
			Class transformedClass = transformClass(classUnderTest, "test/test data/com/ibm/example/bytecode/HelloWorldJava7StaticInit.class");
			
			transformedClass.newInstance();		
		} catch (Exception e) {
			System.out.println("Exception: " + e.getStackTrace().toString());
		}
		
	}
	
	
	//Turn the java8 tests on when I figure out how to get this Unit running conditionally only on java8 tests.
	//@Test
	public void testJava8Basic() {
		try {
			String classUnderTest = "com.ibm.example.bytecode.HelloWorldJava8";
			Class transformedClass = transformClass(classUnderTest, "test/test data/com/ibm/example/bytecode/HelloWorldJava8.class");
			
			transformedClass.newInstance();		
		} catch (Exception e) {
			System.out.println("Exception: " + e.getStackTrace().toString());
		}
		
	}
	
	//@Test
	public void testJava8Lambdas() {
		try {
			String classUnderTest = "com.ibm.example.bytecode.HelloWorldJava8Lambdas";
			Class transformedClass = transformClass(classUnderTest, "test/test data/com/ibm/example/bytecode/HelloWorldJava8Lambdas.class");
			
			transformedClass.newInstance();		
		} catch (Exception e) {
			System.out.println("Exception: " + e.getStackTrace().toString());
		}
		
	}
	
	//@Test
	public void testJava8StaticInit() {
		try {
			String classUnderTest = "com.ibm.example.bytecode.HelloWorldJava8StaticInit";
			Class transformedClass = transformClass(classUnderTest, "test/test data/com/ibm/example/bytecode/HelloWorldJava8StaticInit.class");
			
			transformedClass.newInstance();		
		} catch (Exception e) {
			System.out.println("Exception: " + e.getStackTrace().toString());
		}
		
	}
	
	//@Test
	public void testJava8StaticInitLambdasWithInnerClass() {
		try {
			String classUnderTest = "com.ibm.example.bytecode.HelloWorldJava8StaticLambdas";
			String classUnderTestInnerClass = "com.ibm.example.bytecode.HelloWorldJava8StaticLambdas$Converter";

			byte[] preTransformBytes = makeBytesFromFile("test/test data/com/ibm/example/bytecode/HelloWorldJava8StaticLambdas.class");
			byte[] preTransformBytesInnerClass = makeBytesFromFile("test/test data/com/ibm/example/bytecode/HelloWorldJava8StaticLambdas$Converter.class");
			
			Map<String, byte[]> classDefsPreTransform = new HashMap<String, byte[]>();
			classDefsPreTransform.put(classUnderTest, preTransformBytes);
			classDefsPreTransform.put(classUnderTestInnerClass, preTransformBytesInnerClass);
			
			ByteArrayClassLoader testLoaderPreTransform = new ByteArrayClassLoader(getClass().getClassLoader(), classDefsPreTransform);
			testLoaderPreTransform.setPackage("com.ibm.example.bytecode");
			
			Class loadedClassPreTransform = testLoaderPreTransform.loadClass(classUnderTest); 
			loadedClassPreTransform.newInstance();

			
			
			byte[] classUnderTestBytes = transformedClassBytes(classUnderTest, "test/test data/com/ibm/example/bytecode/HelloWorldJava8StaticLambdas.class");
			byte[] classUnderTestBytesInnerClass = transformedClassBytes(classUnderTestInnerClass, "test/test data/com/ibm/example/bytecode/HelloWorldJava8StaticLambdas$Converter.class");
			
			Map<String, byte[]> classDefs = new HashMap<String, byte[]>();
			classDefs.put(classUnderTest, classUnderTestBytes);
			classDefs.put(classUnderTestInnerClass, classUnderTestBytesInnerClass);
			
			ByteArrayClassLoader testLoader = new ByteArrayClassLoader(getClass().getClassLoader(), classDefs);
			testLoader.setPackage("com.ibm.example.bytecode");
			Class loadedClass = testLoader.loadClass(classUnderTest); 
			
			loadedClass.newInstance();

			
		} catch (Exception e) {
			System.out.println("Exception: " + e.getMessage() + " " + unrollStack(e.getStackTrace()));
		}
		
	}
	
	//@Test
	public void testJava8LambdaInjection() {
		try {
			String classUnderTest = "com.ibm.example.bytecode.HelloWorldJava8Lambdas";
			Class transformedClass = transformClass(classUnderTest, "test/test data/com/ibm/example/bytecode/HelloWorldJava8Lambdas.class");
			
			transformedClass.newInstance();		
			
			
		} catch (Exception e) {
			System.out.println("Exception: " +unrollStack(e.getStackTrace()));
		}
		
	}
	

	
	private void writeStringToFile(String toWrite, String path) {
		PrintWriter file;
		try {
			file = new PrintWriter(path);
			file.println(toWrite);
			file.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void writeBytesToFile(byte[] toWrite, String path) {
		try {
			FileOutputStream fos = new FileOutputStream(path);
			fos.write(toWrite);
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	//turn this on to get detailed ASM output in the project folder.
	public final boolean dumpTrace = false;
	
	private byte[] transformedClassBytesJava8Injector(String className, String path) {
		byte[] transformedClass = null;

		try {
			//LibertyRuntimeTransformer.setInjectAtTransform(false);
			
			String classUnderTest = className;
			byte[] pre = makeBytesFromFile(path);
			byte[] staticInject = staticTransform.transform(pre);
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
				writeStringToFile(preTransform, "Debug-Pre-"+className);
				writeStringToFile(postStaticTransform, "Debug-Static-"+className);
				writeStringToFile(postDynamicTransform, "Debug-PostDyn-"+className);
				writeStringToFile(postStaticAndDynamicTransform, "Debug-PostStaticDyn-"+className);
			}
			transformedClass = post;
		} catch (Exception e) {
			System.out.println("Exception: " + e.getStackTrace().toString());
			Assert.assertTrue(false);
		} 
		
		return transformedClass;
		
	}
	
	private byte[] transformedClassBytes(String className, String path) {
		byte[] transformedClass = null;

		try {
			//LibertyRuntimeTransformer.setInjectAtTransform(false);
			
			String classUnderTest = className;
			byte[] pre = makeBytesFromFile(path);
			byte[] staticInject = staticTransform.transform(pre);
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
				writeStringToFile(preTransform, "Debug-Pre-"+className);
				writeStringToFile(postStaticTransform, "Debug-Static-"+className);
				writeStringToFile(postDynamicTransform, "Debug-PostDyn-"+className);
				writeStringToFile(postStaticAndDynamicTransform, "Debug-PostStaticDyn-"+className);
			}
			transformedClass = post;
		} catch (Exception e) {
			System.out.println("Exception: " + e.getStackTrace().toString());
			Assert.assertTrue(false);
		} 
		
		return transformedClass;
		
	}
	
	private Class transformClass(String className, String path) {
		Class transformedClass = null;

		try {
			transformedClass = makeAClassFromBytes(className, transformedClassBytes(className, path), "com.ibm.example.bytecode");
		} catch (Exception e) {
			System.out.println("Exception: " + e.getStackTrace().toString());
			Assert.assertTrue(false);
		} 
		
		return transformedClass;
	}
	
}

