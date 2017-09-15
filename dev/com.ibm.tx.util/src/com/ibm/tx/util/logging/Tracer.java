package com.ibm.tx.util.logging;
/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

public interface Tracer
{
    public void audit(TraceComponent tc, String s);

    public void audit(TraceComponent tc, String s, Object o);

    public void audit(TraceComponent tc, String s, Object[] o);

    public void debug(TraceComponent tc, String s);

    public void debug(TraceComponent tc, String s, Object o);

    public void debug(TraceComponent tc, String s, Object[] o);

    public void entry(TraceComponent tc, String s);

    public void entry(TraceComponent tc, String s, Object o);

    public void entry(TraceComponent tc, String s, Object[] o);

    public void error(TraceComponent tc, String s);

    public void error(TraceComponent tc, String s, Object o);

    public void error(TraceComponent tc, String s, Object[] o);

    public void event(TraceComponent tc, String s);

    public void event(TraceComponent tc, String s, Object o);

    public void event(TraceComponent tc, String s, Object[] o);

    public void exit(TraceComponent tc, String s);

    public void exit(TraceComponent tc, String s, Object o);

    public void exit(TraceComponent tc, String s, Object[] o);

    public void fatal(TraceComponent tc, String s);

    public void fatal(TraceComponent tc, String s, Object o);

    public void fatal(TraceComponent tc, String s, Object[] o);

    public void info(TraceComponent tc, String s);

    public void info(TraceComponent tc, String s, Object o);

    public void info(TraceComponent tc, String s, Object[] o);

    public void warning(TraceComponent tc, String s);

    public void warning(TraceComponent tc, String s, Object o);

    public void warning(TraceComponent tc, String s, Object[] o);

    public TraceComponent register(Class cl, String traceGroup, String nlsFile);

    public TraceComponent register(String s, String traceGroup, String nlsFile);
    
    public void initTrace();
}
