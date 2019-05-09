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

package com.ibm.websphere.ras;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.ibm.websphere.ras.annotation.TraceOptions;
import com.ibm.ws.logging.internal.TraceNLSResolver;
import com.ibm.ws.logging.internal.TraceSpecification;
import com.ibm.wsspi.logprovider.TrService;

/**
 * This class provides the public interface to the message and trace logging
 * facility.
 * <p>
 * A Message is an informational record that is output to the console. Messages
 * must use localization (national language) support. The caller must supply a
 * message key and appropriate substitution parameters when the request to log a
 * message is made. The national language support of the runtime will find the
 * message text corresponding to the message key in the appropriate message
 * properties file and will format the message with the supplied substitution
 * parameters before displaying the message to the end user.
 * <p>
 * A Trace entry is an informational record that is intended to be used by
 * developers and service personnel. As such, a trace record may be more complex
 * or verbose than its message counterpart and may contain information that is
 * meaningful only to a developer. Neither localization support nor Java
 * TextMessage formatting is provided for trace entries. The text passed by the
 * caller is treated as raw text. Parameters passed by the caller are not
 * treated as substitution parameters and are displayed in a manner that
 * individual trace delegates deem appropriate (typically through use of
 * toString()).
 * <p>
 * Translated message generation is provided through the following set of static
 * final convenience methods. See the documentation for the individual methods
 * for usage detail.
 * 
 * <ul>
 * <li><code>info(...)</code></li>
 * <li><code>audit(...)</code></li>
 * <li><code>warning(...)</code></li>
 * <li><code>error(...)</code></li>
 * <li><code>fatal(...)</code></li>
 * </ul>
 * Untranslated diagnosic trace generation is provided through the following set
 * of static final convenience methods. See the documentation for the individual
 * methods for usage detail.
 * <ul>
 * <li><code>entry(...)</code></li>
 * <li><code>exit(...)</code></li>
 * <li><code>debug(...)</code></li>
 * <li><code>dump(...)</code></li>
 * <li><code>event(...)</code></li>
 * </ul>
 * <p>
 * Before using these convenience methods, a component must register with this
 * service via one of the <code>register()</code> methods and obtain a
 * <code>TraceComponent</code> object in return. The component registers by
 * supplying a <code>Class</code>. The caller may use the returned
 * <code>TraceComponent</code> object to emit trace and messages from an
 * individual class or from a logical component that spans multiple classes. The
 * package qualified name of the <code>Class</code> is obtained from the
 * <code>Class</code> object and used as the registration name. The registration
 * name is used to determine whether messages and trace are enabled for that
 * particular TraceComponent.
 * <p>
 * This service also provides the ability to combine <code>TraceComponent</code>
 * s into logical trace groups. These trace groups can be enabled or disabled
 * (using configuration facilities) as a single entity, as can any singular
 * component. Versions of the <code>register()</code> method are provided that
 * allow the group name to be specified at register time.
 * <p>
 * Once a <code>TraceComponent</code> is obtained by a caller, that caller can
 * use the convenience methods provided by this service. The
 * <code>TraceComponent</code> is a required parameter of the convenience
 * methods.
 * <p>
 * Here are two simple usage examples. The first demonstrates how to register a
 * class with the trace manager and the second demonstrates how to generate a
 * debug trace call.
 * <p>
 * 
 * <pre>
 * <code>
 * private static TraceComponent tc = Tr.register(ComponentImpl.class,
 * ComponentConstants.TRACE_GROUP,
 * ComponentConstants.NLS_BUNDLE);
 * </code>
 * ...
 * <code>
 * if (tc.isDebugEnabled())
 * Tr.debug(tc, "Hello from the trace system.");
 * </code>
 * </pre>
 * <p>
 */

public class Tr {

    /**
     * The current active trace specification. Initialize to the default trace
     * specification (should be *=info, but leave that to TraceSpecification).
     */
    static volatile TraceSpecification activeTraceSpec = new TraceSpecification("", null, false);

    /** Set of all trace components */
    static final Set<TraceComponent> allTraceComponents = Collections.newSetFromMap(new WeakHashMap<TraceComponent, Boolean>());

    /** queue of TraceComponents waiting to be added to allTraceComponents **/
    private static final Queue<TraceComponent> newTracecomponents = new ConcurrentLinkedQueue<TraceComponent>();

    /** Lock protects the allTraceComponents set **/
    private static final Lock allTraceComponentsLock = new ReentrantLock();

    /**
     * Register the provided class with the trace service. This method will look
     * for a trace group name, list of trace group names, and a message bundle
     * name specified through the {@link com.ibm.websphere.ras.annotation.TraceOptions} annotation. The
     * annotation can be specified at the class and/or the package level; the
     * class-level annotation will be given priority. If both traceGroup and
     * traceGroups are specified, traceGroups will be given priority.
     * 
     * @param aClass
     *            a valid <code>Class</code> to register a component for with
     *            the trace manager. The className is obtained from the Class
     *            and is used as the name in the registration process.
     * @return TraceComponent the <code>TraceComponent</code> corresponding to
     *         the name of the specified class.
     */
    public static TraceComponent register(Class<?> aClass) {
        TraceComponent tc;

        /*
         * Check for TraceOptions annotations to specify trace groups and/or
         * message bundle name. Class-level annotation takes priority over
         * package-level.
         */
        TraceOptions options = aClass.getAnnotation(TraceOptions.class);
        if (options == null) {
            options = aClass.getPackage().getAnnotation(TraceOptions.class);
        }

        String name = aClass.getName();
        if (options == null) {
            tc = new TraceComponent(name, aClass, (String) null, null);
        } else {
            String[] traceGroups = options.traceGroups();
            if (traceGroups.length == 0) {
                String traceGroup = options.traceGroup();
                if (traceGroup.trim().isEmpty()) {
                    traceGroups = TraceComponent.EMPTY_STRING_ARRAY;
                } else {
                    traceGroups = new String[] { traceGroup };
                }
            }

            tc = new TraceComponent(name, aClass, traceGroups, false, options.messageBundle());
        }

        registerTraceComponent(tc);
        return tc;
    }

    /**
     * Register the provided name with the trace service and assign it to the
     * provided group name.
     * 
     * @param name
     *            a <code>String</code> to register a component for with
     *            the trace manager. The name is used in the registration
     *            process.
     * @param aClass
     *            a valid <code>Class</code> to register a component for with
     *            the trace manager. The class is used for location of
     *            resource bundles.
     * @param group
     *            the name of the group that the named component is a member of.
     *            Null is allowed. If null is passed, the name is not added to a
     *            group. Once added to a group, there is no corresponding
     *            mechanism to remove a component from a group.
     * @return TraceComponent the <code>TraceComponent</code> corresponding to
     *         the name of the specified name.
     */
    public static TraceComponent register(String name, Class<?> aClass, String group) {
        return register(name, aClass, group, null);
    }

    /**
     * Register the provided name with the trace service and assign it to the
     * provided groups.
     * 
     * @param name
     *            a <code>String</code> to register a component for with
     *            the trace manager. The name is used in the registration
     *            process.
     * @param aClass
     *            a valid <code>Class</code> to register a component for with
     *            the trace manager. The class is used for location of
     *            resource bundles.
     * @param groups
     *            a list of the groups that the named component is a member of.
     *            Null is allowed. If null is passed, the name is not added to a
     *            group. Once added to a group, there is no corresponding
     *            mechanism to remove a component from a group.
     * @return TraceComponent the <code>TraceComponent</code> corresponding to
     *         the name of the specified name.
     */
    public static TraceComponent register(String name, Class<?> aClass, String[] groups) {
        return register(name, aClass, groups, null);
    }

    /**
     * Register the provided class with the trace service and assign it to the
     * provided group name.
     * 
     * @param aClass
     *            a valid <code>Class</code> to register a component for with
     *            the trace manager. The className is obtained from the Class
     *            and is used as the name in the registration process.
     * @param group
     *            the name of the group that the named component is a member of.
     *            Null is allowed. If null is passed, the name is not added to a
     *            group. Once added to a group, there is no corresponding
     *            mechanism to remove a component from a group.
     * @return TraceComponent the <code>TraceComponent</code> corresponding to
     *         the name of the specified class.
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
     *            a valid <code>Class</code> to register a component for with
     *            the trace manager. The className is obtained from the Class
     *            and is used as the name in the registration process.
     * @param group
     *            the name of the group that the named component is a member of.
     *            Null is allowed. If null is passed, the name is not added to a
     *            group. Once added to a group, there is no corresponding
     *            mechanism to remove a component from a group.
     * @param bundle
     *            the name of the message properties file to use when providing
     *            national language support for messages logged by this
     *            component. All messages for this component must be found in
     *            this file.
     * @return TraceComponent the <code>TraceComponent</code> corresponding to
     *         the name of the specified class.
     */
    public static TraceComponent register(Class<?> aClass, String group, String bundle) {
        TraceComponent tc = new TraceComponent(aClass.getName(), aClass, group, bundle);
        registerTraceComponent(tc);

        return tc;
    }

    /**
     * Register the provided name with the trace service and assign it to the
     * provided group name. Translated messages will attempt to use the input
     * message bundle source.
     * 
     * @param name
     *            a <code>String</code> to register a component for with
     *            the trace manager. The name is used in the registration
     *            process.
     * @param aClass
     *            a valid <code>Class</code> to register a component for with
     *            the trace manager. The class is used for location of
     *            resource bundles.
     * @param group
     *            the name of the group that the named component is a member of.
     *            Null is allowed. If null is passed, the name is not added to a
     *            group. Once added to a group, there is no corresponding
     *            mechanism to remove a component from a group.
     * @param bundle
     *            the name of the message properties file to use when providing
     *            national language support for messages logged by this
     *            component. All messages for this component must be found in
     *            this file.
     * @return TraceComponent the <code>TraceComponent</code> corresponding to
     *         the name of the specified name.
     */
    public static TraceComponent register(String name, Class<?> aClass, String group, String bundle) {
        TraceComponent tc = new TraceComponent(name, aClass, group, bundle);
        registerTraceComponent(tc);

        return tc;
    }

    /**
     * Register the provided name with the trace service and assign it to the
     * provided groups. Translated messages will attempt to use the input
     * message bundle source.
     * 
     * @param name
     *            a <code>String</code> to register a component for with
     *            the trace manager. The name is used in the registration
     *            process.
     * @param aClass
     *            a valid <code>Class</code> to register a component for with
     *            the trace manager. The class is used for location of
     *            resource bundles.
     * @param groups
     *            the name of the groups that the named component is a member of.
     *            Null is allowed. If null is passed, the name is not added to a
     *            group. Once added to a group, there is no corresponding
     *            mechanism to remove a component from a group.
     * @param bundle
     *            the name of the message properties file to use when providing
     *            national language support for messages logged by this
     *            component. All messages for this component must be found in
     *            this file.
     * @return TraceComponent the <code>TraceComponent</code> corresponding to
     *         the name of the specified name.
     */
    public static TraceComponent register(String name, Class<?> aClass, String[] groups, String bundle) {
        TraceComponent tc = new TraceComponent(name, aClass, groups, bundle);
        registerTraceComponent(tc);

        return tc;
    }

    /**
     * Print the provided translated message if the input trace component allows
     * audit level messages.
     * 
     * @param tc
     *            the non-null <code>TraceComponent</code> the event is
     *            associated with.
     * @param msgKey
     *            the message key identifying an NLS message for this event.
     *            This message must be in the resource bundle currently
     *            associated with the <code>TraceComponent</code>.
     * @param objs
     *            a number of <code>Objects</code> to include as substitution
     *            text in the message. The number of objects passed must equal
     *            the number of substitution parameters the message expects.
     *            Null is tolerated.
     */
    public static final void audit(TraceComponent tc, String msgKey, Object... objs) {
        TrConfigurator.getDelegate().audit(tc, msgKey, objs);
    }

    /**
     * If debug level diagnostic trace is enabled for the specified
     * <code>TraceComponent</code>, log the provided trace point.
     * 
     * @param tc
     *            the non-null <code>TraceComponent</code> the event is
     *            associated with.
     * @param msg
     *            text to include in the event. No translation or conversion is
     *            performed.
     * @param objs
     *            a variable number (zero to n) of <code>Objects</code>.
     *            toString() is called on each object and the results are
     *            appended to the message.
     */
    public static final void debug(TraceComponent tc, String msg, Object... objs) {
        TrConfigurator.getDelegate().debug(tc, msg, objs);
    }

    /**
     * If debug level diagnostic trace is enabled for the specified
     * <code>TraceComponent</code>, log the provided trace point.
     * 
     * @param id
     *            The object writing the trace point; this will be output into
     *            the trace
     * @param tc
     *            the non-null <code>TraceComponent</code> the event is
     *            associated with. as the identity hashcode of the object
     *            encoded into Hex
     * @param msg
     *            text to include in the event. No translation or conversion is
     *            performed.
     * @param objs
     *            a variable number (zero to n) of <code>Objects</code>.
     *            toString() is called on each object and the results are
     *            appended to the message.
     */
    public static final void debug(Object id, TraceComponent tc, String msg, Object... objs) {
        TrConfigurator.getDelegate().debug(tc, id, msg, objs);
    }

    /**
     * Print the provided trace point if the input trace component allows dump
     * level messages.
     * 
     * @param tc
     *            the non-null <code>TraceComponent</code> the event is
     *            associated with.
     * @param msg
     *            text to include in the event. No translation or conversion is
     *            performed.
     * @param objs
     *            a variable number (zero to n) of <code>Objects</code>.
     *            toString() is called on each object and the results are
     *            appended to the message.
     */
    public static final void dump(TraceComponent tc, String msg, Object... objs) {
        TrConfigurator.getDelegate().dump(tc, msg, objs);
    }

    /**
     * Print the provided trace point if the input trace component allows entry
     * level messages.
     * 
     * @param tc
     *            the non-null <code>TraceComponent</code> the event is
     *            associated with.
     * @param methodName
     * @param objs
     *            a variable number (zero to n) of <code>Objects</code>.
     *            toString() is called on each object and the results are
     *            appended to the message.
     */
    public static final void entry(TraceComponent tc, String methodName, Object... objs) {
        TrConfigurator.getDelegate().entry(tc, methodName, objs);
    }

    /**
     * Print the provided trace point if the input trace component allows entry
     * level messages.
     * 
     * @param id
     *            The object writing the trace point; this will be output into
     *            the trace
     * @param tc
     *            the non-null <code>TraceComponent</code> the event is
     *            associated with. as the identity hashcode of the object
     *            encoded into Hex
     * @param methodName
     * @param objs
     *            a variable number (zero to n) of <code>Objects</code>.
     *            toString() is called on each object and the results are
     *            appended to the message.
     */
    public static final void entry(Object id, TraceComponent tc, String methodName, Object... objs) {
        TrConfigurator.getDelegate().entry(tc, id, methodName, objs);
    }

    /**
     * Print the provided translated message if the input trace component allows
     * error level messages.
     * 
     * @param tc
     *            the non-null <code>TraceComponent</code> the event is
     *            associated with.
     * @param msgKey
     *            the message key identifying an NLS message for this event.
     *            This message must be in the resource bundle currently
     *            associated with the <code>TraceComponent</code>.
     * @param objs
     *            a number of <code>Objects</code> to include as substitution
     *            text in the message. The number of objects passed must equal
     *            the number of substitution parameters the message expects.
     *            Null is tolerated.
     */
    public static final void error(TraceComponent tc, String msgKey, Object... objs) {
        TrConfigurator.getDelegate().error(tc, msgKey, objs);
    }

    /**
     * Print the provided trace point if the input trace component allows event
     * level messages.
     * 
     * @param tc
     *            the non-null <code>TraceComponent</code> the event is
     *            associated with.
     * @param msg
     *            text to include in the event. No translation or conversion is
     *            performed.
     * @param objs
     *            a variable number (zero to n) of <code>Objects</code>.
     *            toString() is called on each object and the results are
     *            appended to the message.
     */
    public static final void event(TraceComponent tc, String msg, Object... objs) {
        TrConfigurator.getDelegate().event(tc, msg, objs);
    }

    /**
     * Print the provided trace point if the input trace component allows event
     * level messages.
     * 
     * @param id
     *            The object writing the trace point; this will be output into
     *            the trace
     * @param tc
     *            the non-null <code>TraceComponent</code> the event is
     *            associated with. as the identity hashcode of the object
     *            encoded into Hex
     * @param msg
     *            text to include in the event. No translation or conversion is
     *            performed.
     * @param objs
     *            a variable number (zero to n) of <code>Objects</code>.
     *            toString() is called on each object and the results are
     *            appended to the message.
     */
    public static final void event(Object id, TraceComponent tc, String msg, Object... objs) {
        TrConfigurator.getDelegate().event(tc, id, msg, objs);
    }

    /**
     * Print the provided trace point if the input trace component allows exit
     * level messages.
     * 
     * @param tc
     *            the non-null <code>TraceComponent</code> the event is
     *            associated with.
     * @param methodName
     */
    public static final void exit(TraceComponent tc, String methodName) {
        TrConfigurator.getDelegate().exit(tc, methodName);
    }

    /**
     * Print the provided trace point if the input trace component allows exit
     * level messages.
     * 
     * @param id
     *            The object writing the trace point; this will be output into
     *            the trace
     * @param tc
     *            the non-null <code>TraceComponent</code> the event is
     *            associated with. as the identity hashcode of the object
     *            encoded into Hex
     * @param methodName
     * @param objs
     */
    public static final void exit(Object id, TraceComponent tc, String methodName) {
        TrConfigurator.getDelegate().exit(tc, id, methodName);
    }

    /**
     * Print the provided trace point if the input trace component allows exit
     * level messages.
     * 
     * @param tc
     *            the non-null <code>TraceComponent</code> the event is
     *            associated with.
     * @param methodName
     * @param o
     */
    public static final void exit(TraceComponent tc, String methodName, Object o) {
        TrConfigurator.getDelegate().exit(tc, methodName, o);
    }

    /**
     * Print the provided trace point if the input trace component allows exit
     * level messages.
     * 
     * @param id
     *            The object writing the trace point; this will be output into
     *            the trace
     * @param tc
     *            the non-null <code>TraceComponent</code> the event is
     *            associated with. as the identity hashcode of the object
     *            encoded into Hex
     * @param methodName
     * @param o
     */
    public static final void exit(Object id, TraceComponent tc, String methodName, Object o) {
        TrConfigurator.getDelegate().exit(tc, id, methodName, o);
    }

    /**
     * Print the provided translated message if the input trace component allows
     * fatal level messages.
     * 
     * @param tc
     *            the non-null <code>TraceComponent</code> the event is
     *            associated with.
     * @param msgKey
     *            the message key identifying an NLS message for this event.
     *            This message must be in the resource bundle currently
     *            associated with the <code>TraceComponent</code>.
     * @param objs
     *            a number of <code>Objects</code> to include as substitution
     *            text in the message. The number of objects passed must equal
     *            the number of substitution parameters the message expects.
     *            Null is tolerated.
     */
    public static final void fatal(TraceComponent tc, String msgKey, Object... objs) {
        TrConfigurator.getDelegate().fatal(tc, msgKey, objs);
    }

    /**
     * Print the provided translated message if the input trace component allows
     * info level messages.
     * 
     * @param tc
     *            the non-null <code>TraceComponent</code> the event is
     *            associated with.
     * @param msgKey
     *            the message key identifying an NLS message for this event.
     *            This message must be in the resource bundle currently
     *            associated with the <code>TraceComponent</code>.
     * @param objs
     *            a number of <code>Objects</code> to include as substitution
     *            text in the message. The number of objects passed must equal
     *            the number of substitution parameters the message expects.
     *            Null is tolerated.
     */
    public static final void info(TraceComponent tc, String msgKey, Object... objs) {
        TrConfigurator.getDelegate().info(tc, msgKey, objs);
    }

    /**
     * Print the provided translated message if the input trace component allows
     * warning level messages.
     * 
     * @param tc
     *            the non-null <code>TraceComponent</code> the event is
     *            associated with.
     * @param msgKey
     *            the message key identifying an NLS message for this event.
     *            This message must be in the resource bundle currently
     *            associated with the <code>TraceComponent</code>.
     * @param objs
     *            a number of <code>Objects</code> to include as substitution
     *            text in the message. The number of objects passed must equal
     *            the number of substitution parameters the message expects.
     *            Null is tolerated.
     */
    public static final void warning(TraceComponent tc, String msgKey, Object... objs) {
        TrConfigurator.getDelegate().warning(tc, msgKey, objs);
    }

    /**
     * Translate a message in the context of the input trace component using
     * the default locale. This method is typically used to provide translated
     * messages that might help resolve an exception that is surfaced to a user.
     * 
     * @param tc
     *            the non-null <code>TraceComponent</code> of the message
     * @param msgKey
     *            the message key identifying an NLS message for this event.
     *            This message must be in the resource bundle currently
     *            associated with the <code>TraceComponent</code>.
     * @param objs
     *            a number of <code>Objects</code> to include as substitution
     *            text in the message. The number of objects passed must equal
     *            the number of substitution parameters the message expects.
     *            Null is tolerated.
     * @return
     *         the translated message
     */
    public static final String formatMessage(TraceComponent tc, String msgKey, Object... objs) {
        return formatMessage(tc, Locale.getDefault(), msgKey, objs);
    }

    /**
     * Like {@link #formatMessage(TraceComponent, List, String, Object...)},
     * but takes a single Locale.
     * 
     * @see #formatMessage(TraceComponent, List, String, Object...)
     */
    public static String formatMessage(TraceComponent tc, Locale locale, String msgKey, Object... objs) {
        return formatMessage(tc, (locale == null) ? null : Collections.singletonList(locale), msgKey, objs);
    }

    /**
     * Like {@link #formatMessage(TraceComponent, List, String, Object...)},
     * but takes an Enumeration of Locales.
     * 
     * @see #formatMessage(TraceComponent, List, String, Object...)
     */
    public static String formatMessage(TraceComponent tc, Enumeration<Locale> locales, String msgKey, Object... objs) {
        return formatMessage(tc, (locales == null) ? null : Collections.list(locales), msgKey, objs);
    }

    /**
     * Translate a message in the context of the input trace component. This
     * method is typically used to provide translated messages that might help
     * resolve an exception that is surfaced to a user.
     * 
     * @param tc
     *            the non-null <code>TraceComponent</code> of the message
     * @param locales
     *            the possible locales to use for translation. Locales from the
     *            front of the list are preferred over Locales from the back of
     *            the list. If the list is null or empty, the default Locale
     *            will be used.
     * @param msgKey
     *            the message key identifying an NLS message for this event.
     *            This message must be in the resource bundle currently
     *            associated with the <code>TraceComponent</code>.
     * @param objs
     *            a number of <code>Objects</code> to include as substitution
     *            text in the message. The number of objects passed must equal
     *            the number of substitution parameters the message expects.
     *            Null is tolerated.
     * @return
     *         the translated message
     */
    public static final String formatMessage(TraceComponent tc, List<Locale> locales, String msgKey, Object... objs) {
        // WsLogRecord.createWsLogRecord + BaseTraceService.formatMessage

        // The best odds for finding the resource bundle are with using the
        // classloader that loaded the associated class to begin with. Start
        // there.
        ResourceBundle rb;
        String msg;
        try {
            rb = TraceNLSResolver.getInstance().getResourceBundle(tc.getTraceClass(), tc.getResourceBundleName(), locales);
            msg = rb.getString(msgKey);
        } catch (Exception ex) {
            // no FFDC required
            msg = msgKey;
        }

        if (msg.contains("{0")) {
            return MessageFormat.format(msg, objs);
        }

        return msg;
    }

    /**
     * Support for com.ibm.ejs.ras.Tr register methods
     */
    static void registerTraceComponent(TraceComponent tc) {
        tc.setTraceSpec(activeTraceSpec);

        TrService activeDelegate = TrConfigurator.getDelegate();
        activeDelegate.register(tc);

        TrConfigurator.traceComponentRegistered(tc);

        // Add the new TraceComponent to the queue of new trace components
        newTracecomponents.add(tc);
        processNewTraceComponents();
        // There is a tiny window where the new trace component never gets processed,
        // however that isn't a problem as we only care that the set is correct is at the
        // beginning of our setTraceSpec method and it does process the list first
    }

    /**
     * package-private: Set the trace specification of the service to the input
     * value.
     * 
     * @param spec
     */
    static void setTraceSpec(TraceSpecification spec) {
        if (spec == null)
            return;

        // We must have the lock to proceed
        allTraceComponentsLock.lock();
        try {
            // Since we have the lock, we must process any new trace components and
            // if we do it now, those new components will also get the trace spec given to them.
            processNewTraceComponents();

            if (activeTraceSpec != null && activeTraceSpec.equals(spec)) {
                // equivalent trace specification -- nothing to do here.
                return;
            }
            activeTraceSpec = spec;

            // update all of the current trace components
            for (TraceComponent t : allTraceComponents) {
                t.setTraceSpec(activeTraceSpec);
            }
            // Update the static flag for whether or not any trace is enabled
            // no guarantee about when threads will see this update..  
            TraceComponent.setAnyTracingEnabled(activeTraceSpec.isAnyTraceEnabled());

            // Log the new trace level. This is done after updating TCs to
            // ensure the message goes to trace.log as necessary.
            // =enabled is redundant, and apparently discouraged, so we hide it for message display.
            Tr.info(TraceSpecification.getTc(), "MSG_TRACE_STATE_CHANGED", activeTraceSpec.toDisplayString());

            // Okay as that processing could have taken some time, we need to catch any new
            // trace components that might have appeared (which does leave a tiny window, but that
            // will either be caught on the next setTraceSpec or never processed (because we never
            // needed it).
            processNewTraceComponents();
        } finally {
            allTraceComponentsLock.unlock();
        }
    }

    /**
     * private: If we already have (or can get) the allTraceComponentsLock
     * process the new trace components
     */
    private static void processNewTraceComponents() {
        if (allTraceComponentsLock.tryLock()) {
            try {
                TraceComponent tc = newTracecomponents.poll();
                while (tc != null) {
                    allTraceComponents.add(tc);
                    tc = newTracecomponents.poll();
                }
            } finally {
                allTraceComponentsLock.unlock();
            }
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