/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.logging.internal.osgi.stackjoiner.bci;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import com.ibm.ws.logging.internal.osgi.stackjoiner.StackJoinerManager;
import com.ibm.ws.logging.internal.osgi.stackjoiner.boot.templates.ThrowableProxy;

public class ThrowableClassFileTransformer implements ClassFileTransformer {
	
	/**
	 * The name of the Throwable class that will be transformed.
	 */
	public final static String THROWABLE_CLASS_NAME = "java/lang/Throwable";
	
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		byte[] ba = null;
		
		if(!className.equals(THROWABLE_CLASS_NAME)) return ba;
		
		try {
			ba = transformClassIfThrowable(classfileBuffer, className);
		} catch (Exception e) {
			throw e;
		}
		return ba;
	}
	
	@SuppressWarnings("static-access")
	private byte[] transformClassIfThrowable(byte[] cBuffer, String nameOfClass) {
		ClassReader reader = new ClassReader(cBuffer);
		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
		ClassVisitor visitor = new ThrowableClassAdapter(writer);
		reader.accept(visitor, reader.SKIP_FRAMES);
		return writer.toByteArray();
	}
}
