/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejs.ras;

import com.ibm.ws.logging.internal.StackFinder;

/**
 * Static accessor/wrapper around configurable delegate.
 * 
 * Translated message generation via these methods:
 * <ul>
 * <li><code>info(...)</code></li>
 * <li><code>audit(...)</code></li>
 * <li><code>warning(...)</code></li>
 * <li><code>error(...)</code></li>
 * <li><code>fatal(...)</code></li>
 * <li><code>service(...)</code></li>
 * </ul>
 * Untranslated trace generation via these methods:
 * <ul>
 * <li><code>entry(...)</code></li>
 * <li><code>exit(...)</code></li>
 * <li><code>debug(...)</code></li>
 * <li><code>dump(...)</code></li>
 * <li><code>event(...)</code></li>
 * </ul>
 * 
 * <p>This class exists in support of binary compatibility with
 * the full profile &quot;Tr&quot;. Code that does not need to run in
 * the full profile should switch to {@link com.ibm.websphere.ras.Tr}.
 * 
 * @see com.ibm.websphere.ras.Tr
 * @see com.ibm.websphere.ras.TraceComponent
 * @see com.ibm.websphere.ras.annotation.TraceOptions
 */
public class Tr {
    /**
     * @param name
     * @return TraceComponent
     * @deprecated Use version with class parameter instead
     * @see #register(Class)
     */
    @Deprecated
    public static TraceComponent register(String name) {
        return register(name, null, null);
    }

    /**
     * @param name
     * @param group
     * @return TraceComponent
     * @deprecated Use version with class parameter instead
     * @see #register(Class, String)
     */
    @Deprecated
    public static TraceComponent register(String name, String group) {
        return register(name, group, null);
    }

    /**
     * @param group
     * @param bundle
     * @return TraceComponent
     * @deprecated Use version with class parameter instead
     * @see #register(Class, String, String)
     */
    @Deprecated
    public static TraceComponent register(String name, String group, String bundle) {
        if (name.contains(".logwriter")) {
            String[] groups = new String[2];
            groups[0] = name;
            groups[1] = group;

            Class<?> aClass = null;
            StackFinder finder = StackFinder.getInstance();
            aClass = finder.getCaller(name);

            // There should be no way for this to happen
            if (aClass == null)
                throw new IllegalStateException("Unable to find the caller on the call stack");

            TraceComponent tc = new TraceComponent(name, aClass, groups, bundle);
            com.ibm.websphere.ras.TrConfigurator.registerTraceComponent(tc);
            return tc;
        } else {
            Class<?> aClass = null;
            StackFinder finder = StackFinder.getInstance();
            aClass = finder.getCaller(name);

            // There should be no way for this to happen
            if (aClass == null)
                throw new IllegalStateException("Unable to find the caller on the call stack");

            return register(aClass, group, bundle);
        }
    }

    /**
     * Register the provided class with the trace service.
     * 
     * @param aClass
     * @return TraceComponent
     */
    public static TraceComponent register(Class<?> aClass) {
        return register(aClass, null, null);
    }

    /**
     * Register the provided class with the trace service and assign it to the
     * provided group name.
     * 
     * @param aClass
     * @param group
     * @return TraceComponent
     */
    public static TraceComponent register(Class<?> aClass, String group) {
        return register(aClass, group, null);
    }

    /**
     * Register the provided class with the trace service and assign it to the
     * provided group name. Translated messages will attempt to use the input
     * message bundle source.
     * 
     * @param aClass
     * @param group
     * @param bundle
     * @return TraceComponent
     */
    public static TraceComponent register(Class<?> aClass, String group, String bundle) {
        TraceComponent tc = new TraceComponent(aClass, group, bundle);
        com.ibm.websphere.ras.TrConfigurator.registerTraceComponent(tc);
        return tc;
    }

    /**
     * Register a dumpable object for the input component with the trace
     * service.
     * 
     * @param tc
     * @param d
     */
    public static void registerDumpable(TraceComponent tc, Dumpable d) {}

    /**
     * Print the provided message if the input trace component allows audit
     * level messages.
     * 
     * @param tc
     * @param msg
     */
    public static final void audit(TraceComponent tc, String msg) {
        com.ibm.websphere.ras.Tr.audit(tc, msg);
    }

    /**
     * Print the provided message if the input trace component allows audit
     * level messages.
     * 
     * @param tc
     * @param msg
     * @param obj
     */
    public static final void audit(TraceComponent tc, String msg, Object obj) {
        if (obj != null && obj instanceof Object[]) {
            com.ibm.websphere.ras.Tr.audit(tc, msg, (Object[]) obj);
        } else {
            com.ibm.websphere.ras.Tr.audit(tc, msg, obj);
        }
    }

    /**
     * Print the provided message if the input trace component allows debug
     * level messages.
     * 
     * @param tc
     * @param msg
     */
    public static final void debug(TraceComponent tc, String msg) {
        com.ibm.websphere.ras.Tr.debug(tc, msg);
    }

    /**
     * Print the provided message if the input trace component allows debug
     * level messages.
     * 
     * @param tc
     * @param msg
     * @param obj
     */
    public static final void debug(TraceComponent tc, String msg, Object obj) {
        if (obj != null && obj instanceof Object[]) {
            com.ibm.websphere.ras.Tr.debug(tc, msg, (Object[]) obj);
        } else {
            com.ibm.websphere.ras.Tr.debug(tc, msg, obj);
        }
    }

    /**
     * Print the provided message if the input trace component allows dump level
     * messages.
     * 
     * @param tc
     * @param msg
     */
    public static final void dump(TraceComponent tc, String msg) {
        com.ibm.websphere.ras.Tr.dump(tc, msg);
    }

    /**
     * Print the provided message if the input trace component allows dump level
     * messages.
     * 
     * @param tc
     * @param msg
     * @param obj
     */
    public static final void dump(TraceComponent tc, String msg, Object obj) {
        if (obj != null && obj instanceof Object[]) {
            com.ibm.websphere.ras.Tr.dump(tc, msg, (Object[]) obj);
        } else {
            com.ibm.websphere.ras.Tr.dump(tc, msg, obj);
        }
    }

    /**
     * Print the provided message if the input trace component allows entry
     * level messages.
     * 
     * @param tc
     * @param methodName
     */
    public static final void entry(TraceComponent tc, String methodName) {
        com.ibm.websphere.ras.Tr.entry(tc, methodName);
    }

    /**
     * Print the provided message if the input trace component allows entry
     * level messages.
     * 
     * @param tc
     * @param methodName
     * @param obj
     */
    public static final void entry(TraceComponent tc, String methodName, Object obj) {
        if (obj != null && obj instanceof Object[]) {
            com.ibm.websphere.ras.Tr.entry(tc, methodName, (Object[]) obj);
        } else {
            com.ibm.websphere.ras.Tr.entry(tc, methodName, obj);
        }
    }

    /**
     * Print the provided message if the input trace component allows error
     * level messages.
     * 
     * @param tc
     * @param msg
     */
    public static final void error(TraceComponent tc, String msg) {
        com.ibm.websphere.ras.Tr.error(tc, msg);
    }

    /**
     * Print the provided message if the input trace component allows error
     * level messages.
     * 
     * @param tc
     * @param msg
     * @param obj
     */
    public static final void error(TraceComponent tc, String msg, Object obj) {
        if (obj != null && obj instanceof Object[]) {
            com.ibm.websphere.ras.Tr.error(tc, msg, (Object[]) obj);
        } else {
            com.ibm.websphere.ras.Tr.error(tc, msg, obj);
        }
    }

    /**
     * Print the provided message if the input trace component allows event
     * level messages.
     * 
     * @param tc
     * @param msg
     */
    public static final void event(TraceComponent tc, String msg) {
        com.ibm.websphere.ras.Tr.event(tc, msg);
    }

    /**
     * Print the provided message if the input trace component allows event
     * level messages.
     * 
     * @param tc
     * @param msg
     * @param obj
     */
    public static final void event(TraceComponent tc, String msg, Object obj) {
        if (obj != null && obj instanceof Object[]) {
            com.ibm.websphere.ras.Tr.event(tc, msg, (Object[]) obj);
        } else {
            com.ibm.websphere.ras.Tr.event(tc, msg, obj);
        }
    }

    /**
     * Print the provided message if the input trace component allows exit level
     * messages.
     * 
     * @param tc
     * @param methodName
     */
    public static final void exit(TraceComponent tc, String methodName) {
        com.ibm.websphere.ras.Tr.exit(tc, methodName);
    }

    /**
     * Print the provided message if the input trace component allows exit level
     * messages.
     * 
     * @param tc
     * @param methodName
     * @param obj
     */
    public static final void exit(TraceComponent tc, String methodName, Object obj) {
        com.ibm.websphere.ras.Tr.exit(tc, methodName, obj);
    }

    /**
     * Print the provided message if the input trace component allows fatal
     * level messages.
     * 
     * @param tc
     * @param msg
     */
    public static final void fatal(TraceComponent tc, String msg) {
        com.ibm.websphere.ras.Tr.fatal(tc, msg);
    }

    /**
     * Print the provided message if the input trace component allows fatal
     * level messages.
     * 
     * @param tc
     * @param msg
     * @param obj
     */
    public static final void fatal(TraceComponent tc, String msg, Object obj) {
        if (obj != null && obj instanceof Object[]) {
            com.ibm.websphere.ras.Tr.fatal(tc, msg, (Object[]) obj);
        } else {
            com.ibm.websphere.ras.Tr.fatal(tc, msg, obj);
        }
    }

    /**
     * Print the provided message if the input trace component allows info level
     * messages.
     * 
     * @param tc
     * @param msg
     */
    public static final void info(TraceComponent tc, String msg) {
        com.ibm.websphere.ras.Tr.info(tc, msg);
    }

    /**
     * Print the provided message if the input trace component allows info level
     * messages.
     * 
     * @param tc
     * @param msg
     * @param obj
     */
    public static final void info(TraceComponent tc, String msg, Object obj) {
        if (obj != null && obj instanceof Object[]) {
            com.ibm.websphere.ras.Tr.info(tc, msg, (Object[]) obj);
        } else {
            com.ibm.websphere.ras.Tr.info(tc, msg, obj);
        }
    }

    /**
     * Print the provided message if the input trace component allows service
     * level messages.
     * 
     * // Note in WAS code states 'service now maps to audit per JSR47'
     * 
     * @param tc
     * @param msg
     */
    public static final void service(TraceComponent tc, String msg) {
        com.ibm.websphere.ras.Tr.audit(tc, msg);
    }

    /**
     * Print the provided message if the input trace component allows service
     * level messages.
     * 
     * @param tc
     * @param msg
     * @param obj
     */
    public static final void service(TraceComponent tc, String msg, Object obj) {
        if (obj != null && obj instanceof Object[]) {
            com.ibm.websphere.ras.Tr.audit(tc, msg, (Object[]) obj);
        } else {
            com.ibm.websphere.ras.Tr.audit(tc, msg, obj);
        }
    }

    /**
     * Print the provided message if the input trace component allows warning
     * level messages.
     * 
     * @param tc
     * @param msg
     */
    public static final void warning(TraceComponent tc, String msg) {
        com.ibm.websphere.ras.Tr.warning(tc, msg);
    }

    /**
     * Print the provided message if the input trace component allows warning
     * level messages.
     * 
     * @param tc
     * @param msg
     * @param obj
     */
    public static final void warning(TraceComponent tc, String msg, Object obj) {
        if (obj != null && obj instanceof Object[]) {
            com.ibm.websphere.ras.Tr.warning(tc, msg, (Object[]) obj);
        } else {
            com.ibm.websphere.ras.Tr.warning(tc, msg, obj);
        }
    }

    /*
     * Unconditional tracing is not supported but will be mapped to error
     * tracing for binary support
     */
    public static final void uncondEvent(TraceComponent tc, String msg) {
        com.ibm.websphere.ras.Tr.event(tc, msg);
    }

    public static final void uncondEvent(TraceComponent tc, String msg, Object objs) {
        // No support for unconditional tracing; map to error
        if (objs != null && objs instanceof Object[]) {
            com.ibm.websphere.ras.Tr.event(tc, msg, (Object[]) objs);
        } else {
            com.ibm.websphere.ras.Tr.event(tc, msg, objs);
        }
    }

    public static final void uncondFormattedEvent(TraceComponent tc, String msgKey) {
        com.ibm.websphere.ras.Tr.info(tc, msgKey);
    }

    public static final void uncondFormattedEvent(TraceComponent tc, String msgKey, Object objs) {
        if (objs != null && objs instanceof Object[]) {
            com.ibm.websphere.ras.Tr.info(tc, msgKey, (Object[]) objs);
        } else {
            com.ibm.websphere.ras.Tr.info(tc, msgKey, objs);
        }
    }

    /**
     * Static-only method invocation, prevent instantiation by blocking the
     * default ctor (allow test subclassing).
     */
    protected Tr() {
        // nothing to do
    }
}