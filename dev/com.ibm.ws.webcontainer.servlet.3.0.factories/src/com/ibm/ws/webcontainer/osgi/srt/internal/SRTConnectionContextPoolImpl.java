/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.osgi.srt.internal;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.webcontainer.osgi.srt.SRTConnectionContext;
import com.ibm.ws.webcontainer.osgi.srt.SRTConnectionContextPool;

/**
 * A simple pool for SRTConnectionContext objects.
 */
@Component(property = { "service.ranking:Integer=30", "servlet.version=3.0" })
public class SRTConnectionContextPoolImpl implements SRTConnectionContextPool
{
  private ThreadLocal<SRTConnectionContext> head = new ThreadLocal<SRTConnectionContext>();

  public final SRTConnectionContext get()
  {
    SRTConnectionContext context = null;
    SRTConnectionContext headContext = head.get();
    if (headContext != null)
    {
      context = headContext;
      head.set(context.nextContext);
    }

    if (context == null)
    {
      context = new SRTConnectionContext();
    }

    context.nextContext = null;

    return context;
  }

  public final void put(SRTConnectionContext context)
  {
    context.nextContext = head.get();
    head.set(context);
  }
}
