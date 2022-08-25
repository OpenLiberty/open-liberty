/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.internal;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * The default thread factory to use when one is not specified.
 */
final class VirtualThreadFactoryImpl implements ThreadFactory {
  private final static TraceComponent tc = Tr.register(VirtualThreadFactoryImpl.class);
  private final AtomicInteger createdThreadCount = new AtomicInteger();

  /**
   * The name of the executor associated with this factory.
   */
  private final String executorName;

  /**
   * The context class loader to associate with newly created threads.
   */
  private final ClassLoader contextClassLoader;

  private  static MethodHandle name;
  private  static MethodHandle unstarted;
  private  static MethodHandle ofVirtual;

  private Object builder;

  private static Class<?> builderClass;

  static {
    doInit();
  }

  @FFDCIgnore(Exception.class)
  private static void doInit() {
    try {
        builderClass = Class.forName("java.lang.Thread$Builder$OfVirtual");

        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        name = lookup.findVirtual(builderClass, "name", MethodType.methodType(builderClass, String.class));
        unstarted = lookup.findVirtual(builderClass, "unstarted", MethodType.methodType(Thread.class, Runnable.class));

        ofVirtual = lookup.findStatic(Thread.class, "ofVirtual", MethodType.methodType(builderClass));
    } catch (Exception e) {
    }
  }

  /**
   * Create a thread factory that creates threads associated with a
   * named thread group.
   *
   * @param executorName the name of the owning executor that serves as
   * @param threadGroupName the name of the thread group to create
   */
  @FFDCIgnore({RuntimeException.class, Error.class, Throwable.class})
  VirtualThreadFactoryImpl(final String executorName) {
      this.executorName = executorName;
      this.contextClassLoader = Thread.currentThread().getContextClassLoader();

      try {
        builder = ofVirtual.invoke();
      } catch (RuntimeException re) {
        throw (RuntimeException)re;
      } catch (Error e) {
        throw (Error)e;
      } catch (Throwable t) {
        // do nothing since the called method doesn't throw checked exceptions
      }
  }

  /**
   * Create a new thread.
   *
   * @param runnable the task to run
   */
  @Override
  public synchronized Thread newThread(final Runnable runnable) {
    try {
      int threadId = createdThreadCount.incrementAndGet();
      final String threadName = executorName + "-thread-" + threadId;
      name.invoke(builder, threadName);
      Thread thread  = (Thread) unstarted.invoke(builder, runnable);
      thread.setContextClassLoader(contextClassLoader);
      return thread;
    } catch (RuntimeException re) {
      throw (RuntimeException)re;
    } catch (Error e) {
      throw (Error)e;
    } catch (Throwable t) {
      // do nothing since the called method doesn't throw checked exceptions
      return null;
    }
  }

  @FFDCIgnore(UnsupportedOperationException.class)
  public static boolean isSupported() {
    try {
      return builderClass != null && ofVirtual.invoke() != null;
    } catch (UnsupportedOperationException e) {
      // This is a normal situation if preview features are not enabled so ignore
    } catch (Throwable t) {
    }
    return false;
  }
}
