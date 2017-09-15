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

package com.ibm.ejs.ras;

import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.sib.unittest.ras.Logger;

public class Tr 
{
  private static final Map<String,TraceComponent> _comps = new HashMap<String,TraceComponent>();
  
	private Tr() {
	}

  public static TraceComponent register(Class clazz)
  {
    return register(clazz.getName());
  }
  
  public static TraceComponent register(Class clazz, String group)
  {
    return register(clazz.getName(),group);
  }
  
  public static TraceComponent register(
      Class aClass,
      String group,
      String resourceBundleName) 
    {
      return register(aClass.getName(),group,resourceBundleName);
    }

  public static synchronized TraceComponent register(
		String name,
		String group,
		String resourceBundleName) 
  {
    TraceComponent answer;
    
    if (_comps.containsKey(name))
     answer =  _comps.get(name);
    else
    {
      answer = new TraceComponent(Logger.getLogger(name, group, resourceBundleName), resourceBundleName);
      _comps.put(name,answer);
    }
		return answer;
	}

  public static synchronized TraceComponent register(String name)
  {
    TraceComponent answer;
    
    if (_comps.containsKey(name))
     answer =  _comps.get(name);
    else
    {
      answer = new TraceComponent(Logger.getLogger(name), "");
      _comps.put(name,answer);
    }
    return answer;
  }
  
  public static synchronized TraceComponent register(String name, String group)
  {
    TraceComponent answer;
    
    if (_comps.containsKey(name))
     answer =  _comps.get(name);
    else
    {
      answer = new TraceComponent(Logger.getLogger(name, group), "");
      _comps.put(name,answer);
    }
    return answer;
  }
  
	public static void registerDumpable(TraceComponent tc, Dumpable d) 
  {
    tc.registerDumpable(d);
	}

	public static final void audit(TraceComponent tc, String msgKey) 
  {
    tc.getLogger().audit(msgKey);
	}

	public static final void audit(
		TraceComponent tc,
		String msgKey,
		Object objs) 
  {
    tc.getLogger().audit(msgKey, objs);
	}

	public static final void debug(TraceComponent tc, String msg) 
  {
    tc.getLogger().debug(msg);
	}

	public static final void debug(
		TraceComponent tc,
		String msg,
		Object objs) 
  {
    tc.getLogger().debug(msg, objs);
	}

	public static final void dump(TraceComponent tc, String msg) 
  {
    tc.getLogger().dump(msg);
    tc.getRegisteredDumpable().resetDump();
    tc.getRegisteredDumpable().dump();
	}

	public static final void dump(TraceComponent tc, String msg, Object objs) 
  {
    tc.getLogger().dump(msg, objs);
    tc.getRegisteredDumpable().resetDump();
    tc.getRegisteredDumpable().dump();
  }

	public static final void error(TraceComponent tc, String msgKey) 
  {
    tc.getLogger().error(msgKey);
	}

	public static final void error(
		TraceComponent tc,
		String msgKey,
		Object objs) 
  {
    tc.getLogger().error(msgKey, objs);
	}

	public static final void event(TraceComponent tc, String msg) 
  {
    tc.getLogger().event(msg);
	}

	public static final void event(
		TraceComponent tc,
		String msg,
		Object objs) 
  {
    tc.getLogger().event(msg, objs);
	}

	public static final void entry(TraceComponent tc, String methodName) 
  {
    tc.getLogger().enter(methodName);
	}

	public static final void entry(
		TraceComponent tc,
		String methodName,
		Object objs) 
  {
    tc.getLogger().enter(methodName, objs);
	}

	public static final void exit(TraceComponent tc, String methodName) 
  {
    tc.getLogger().exit(methodName);
	}

	public static final void exit(
		TraceComponent tc,
		String methodName,
		Object objs) 
  {
    tc.getLogger().exit(methodName, objs);
	}

	public static final void fatal(TraceComponent tc, String msgKey) 
  {
    tc.getLogger().fatal(msgKey);
	}

	public static final void fatal(
		TraceComponent tc,
		String msgKey,
		Object objs) 
  {
    tc.getLogger().fatal(msgKey, objs);
	}

	public static final void info(TraceComponent tc, String msgKey) 
  {
    tc.getLogger().info(msgKey);
	}

	public static final void info(
		TraceComponent tc,
		String msgKey,
		Object objs) 
  {
    tc.getLogger().info(msgKey, objs);
	}

	public static final void service(TraceComponent tc, String msgKey) 
  {
    tc.getLogger().service(msgKey);
	}

	public static final void service(
		TraceComponent tc,
		String msgKey,
		Object objs) 
  {
    tc.getLogger().service(msgKey, objs);
	}

	public static final void uncondEvent(TraceComponent tc, String msg) 
  {
    tc.getLogger().uncondEvent(msg);
	}

	public static final void uncondEvent(
		TraceComponent tc,
		String msg,
		Object objs) 
  {
    tc.getLogger().uncondEvent(msg, objs);
	}

	public static final void uncondFormattedEvent(
		TraceComponent tc,
		String msgKey) 
  {
    tc.getLogger().uncondFormattedEvent(msgKey);
	}

	public static final void uncondFormattedEvent(
		TraceComponent tc,
		String msgKey,
		Object objs) 
  {
    tc.getLogger().uncondFormattedEvent(msgKey, objs);
	}

	public static final void warning(TraceComponent tc, String msgKey) 
  {
    tc.getLogger().warning(msgKey);
	}

	public static final void warning(
		TraceComponent tc,
		String msgKey,
		Object objs) 
  {
    tc.getLogger().warning(msgKey, objs);
	}
}
