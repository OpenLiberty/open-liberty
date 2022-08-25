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

 import com.ibm.websphere.ras.Tr;
 import com.ibm.websphere.ras.TraceComponent;
 import com.ibm.websphere.ras.annotation.Trivial;

 /**
  * The default thread factory to use when one is not specified.
  */
public final class ThreadFactoryBuilder {
  private final static TraceComponent tc = Tr.register(ThreadFactoryBuilder.class);

  private static final boolean virtualThreadsSupported = VirtualThreadFactoryImpl.isSupported();

  public static ThreadFactory create(String name, String tgName, boolean virtual) {
    if (virtualThreadsSupported && virtual) {
      return new VirtualThreadFactoryImpl(name);
    } else {
      return new ThreadFactoryImpl(name, tgName);
    }
  }
}
