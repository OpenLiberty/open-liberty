/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.cache.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;

/**
 * This class is used when deserializing objects so that the context class
 * loader will be used to resolve classes. This is especially important where
 * the objects being deserialized can be non-WebSphere classes.
 * 
 * @ibm-private-in-use
 */

public class WsObjectInputStream extends java.io.ObjectInputStream {

	/**
	 * <p>
	 * This interface is used to resolve OSGi declared serializable classes.
	 * </p>
	 */
	public interface ClassResolver {
		/**
		 * Attempt to load the specified class.
		 * 
		 * @param className
		 *            The classname.
		 * @return The class, or null if not found.
		 */
		public Class resolveClass(String className);
	}

	// begin D179430
	private static final HashMap primClasses = new HashMap(8, 1.0F);

	/** The class resolver */

	protected static ClassResolver resolver;
	static {
		primClasses.put("boolean", boolean.class);
		primClasses.put("byte", byte.class);
		primClasses.put("char", char.class);
		primClasses.put("short", short.class);
		primClasses.put("int", int.class);
		primClasses.put("long", long.class);
		primClasses.put("float", float.class);
		primClasses.put("double", double.class);
		primClasses.put("void", void.class);
	}

	// end D179430

	protected ClassLoader classloader;

	protected String name;

	public WsObjectInputStream(InputStream is) throws IOException {
		super(is);

		classloader = (ClassLoader) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return Thread.currentThread().getContextClassLoader();
					}
				});
	}

	public WsObjectInputStream(InputStream is, ClassLoader cl)
			throws IOException {
		super(is);
		classloader = cl;
	}

	protected Class resolveClass(ObjectStreamClass objStrmClass)
			throws IOException, ClassNotFoundException {
		return resolveClass(objStrmClass.getName());
	}

	private Class resolveClass(String name) throws IOException,
			ClassNotFoundException {
		try {
			this.name = name;
			return (Class) AccessController
					.doPrivileged(loadAction);
		} catch (java.security.PrivilegedActionException pae) {
			Exception wrapped = pae.getException();
			if (wrapped instanceof ClassNotFoundException)
				throw (ClassNotFoundException) wrapped;
			throw new ClassNotFoundException(name);
		}
	}

	java.security.PrivilegedExceptionAction loadAction = new java.security.PrivilegedExceptionAction() {
		public java.lang.Object run() throws Exception {
			try { // begin D179430
				Class clazz = null;
				// If the resolver is set
				if (resolver != null) {
					// use the resolver to load the class.
					clazz = resolver.resolveClass(name);
				}

				// if the class is not loadable
				if (clazz == null) {
					clazz = loadClass(name, classloader); // d296416
				}

				return clazz;
			} catch (ClassNotFoundException cnf) {
				Class c = (Class) primClasses.get(name);
				if (c != null) {
					return c;
				} else {
					throw cnf;
				}
			} // end D179430
		}
	};

	// d296416: Use runtime bundle classloader (current) to resolve a class when
	// the class could not be resolved using the specified classloader.
	// A serializable class in a bundle should specify via
	// <com.ibm.ws.runtime.serializable> bundle extension point
	// that it is deserializable outside the current bundle.
	// NOTE: Looking up current classloader is only a tactical solution,
	// and could be deprecated in future.
	// 
	private java.lang.Class loadClass(String name, ClassLoader loader)
			throws ClassNotFoundException {
		try {
			return Class.forName(name, true, loader);
		} catch (ClassNotFoundException cnf) {
			return Class.forName(name);
		}
	}

	// begin PK29198
	protected Class resolveProxyClass(String[] interfaces) throws IOException,
			ClassNotFoundException {
		if (interfaces.length == 0) {
			throw new ClassNotFoundException("zero-length interfaces array");
		}

		Class nonPublicClass = null;

		Class[] classes = new Class[interfaces.length];
		for (int i = 0; i < interfaces.length; i++) {
			classes[i] = resolveClass(interfaces[i]);

			if ((classes[i].getModifiers() & Modifier.PUBLIC) == 0) {
				// "if more than one non-public interface class loader is
				// encountered, an IllegalAccessError is thrown"
				if (nonPublicClass != null) {
					throw new IllegalAccessError(nonPublicClass + " and "
							+ classes[i] + " both declared non-public");
				}

				nonPublicClass = classes[i];
			}
		}

		// The javadocs for this method say:
		//
		// "Unless any of the resolved interfaces are non-public, this same
		// value of loader is also the class loader passed to
		// Proxy.getProxyClass; if non-public interfaces are present, their
		// class loader is passed instead"
		//
		// Unfortunately, we don't have a single classloader that we can use.
		// Call getClassLoader() on either the non-public class (if any) or the
		// first class.
		proxyClass = nonPublicClass != null ? nonPublicClass : classes[0];
		ClassLoader loader = (ClassLoader) AccessController
				.doPrivileged(proxyClassLoaderAction);

		// "If Proxy.getProxyClass throws an IllegalArgumentException,
		// resolveProxyClass will throw a ClassNotFoundException containing the
		// IllegalArgumentException."
		try {
			return Proxy.getProxyClass(loader, classes);
		} catch (IllegalArgumentException ex) {
			throw new ClassNotFoundException(ex.getMessage(), ex);
		}
	}

	private Class proxyClass;
	PrivilegedAction proxyClassLoaderAction = new PrivilegedAction() {
		public Object run() {
			return proxyClass.getClassLoader();
		}
	};
}
