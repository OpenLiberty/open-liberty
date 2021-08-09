/*******************************************************************************
 * Copyright (c) 1998, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejs.ras.hpel;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The TraceNLSResolver manages caching TraceNLS objects. As far as is possible,
 * this caching mechanism will ensure that any given bundle is loaded exactly
 * once in a process lifetime. (We are at the mercy of users creating malformed
 * Locale objects).
 * <p>
 * A ResourceBundle is uniquely speficied by the combination of both a bundle
 * name and a Locale. Therefore the bundle "myMessages" for French and English
 * Locales are different objects.
 * <p>
 * We expect that all operations will be in the default Locale for the process.
 * However java allows the user to set (change) the default Locale at will. It
 * is also conceivable that the user of this class may want to retrieve a
 * message from a bundle in the non-default Locale. This class will support that
 * behavior, since we cannot predict future usage. At present we know of no such
 * usages.
 * <p>
 * For efficiency, there are two levels of caching of TraceNLS instances. A fast
 * path is provided when instances for the default Locale, which we expect to be
 * the norm. Non-default Locale support will require a more complex caching
 * algorithm.
 * <p>
 * For each Locale, we will use a Hashtable to cache TraceNLS instances keyed by
 * the ResourceBundle name. We will also use a Hashtable to cache the Locale
 * caches.
 * <p>
 * Caching of the loaded ResourceBundles by Locale is also performed.
 * <p>
 * While this caching algorithm may seem a little complex, as with all caching
 * the expectation is that the lookup for instantiated objects will be as
 * efficient as possible and the penalty for that performance is complexity and
 * footprint.
 * <p>
 * The methods of this class expect that the specified ResourceBundle name is
 * correctly formed. That is, it is a fully package-qualified name from where
 * the ResourceBundle resides on the classpath.
 * <p>
 * Each TraceNLS instance encapsulates or contains a reference to a
 * ResourceBundle loaded in memory.
 * <p>
 * WARNING Since this mechanism is used by the RAS service to localize messages
 * passed to the RAS service, this class MUST NOT be instrumented with RAS APIs
 * that will result in localization of messages.
 */
public class TraceNLSResolver {
    /**
     * Use Tr with caution, as it can cause recursion. Currently enabled with
     * quiet (method parameter) and result of makeNoise().
     * 
     * @see #makeNoise()
     */
    // protected static TraceComponent tc =
    // com.ibm.websphere.ras.Tr.register(com.ibm.ejs.ras.internal.TraceNLSResolver.class);

    /**
     * Name of property used to enable noise-making when messages can't be
     * formatted. Used with "quiet".
     */
    public static final String DEBUG_TRACE_NLS_PROPERTY = "com.ibm.ejs.ras.debugTraceNLSResolver";

    /** Lazy-initialized value of debug property */
    private static Boolean debugTraceNLSResolver = null;

    /** If true, the resolver will trace with Tr */
    protected static boolean makeNoise = false;

    /** Error message used when it is not possible to format a message. */
    protected static final String nullKey = "null", svNullBundleName = "Resource Bundle name is null, key = {0}", svBundleNotLoaded = "Unable to load ResourceBundle {0}", svNullKeyMessage = "Null key passed while using ResourceBundle {0}", svMalformedMessage = "No message text associated with key {0} in bundle {1}";

    /** Lazy-initialized TraceResolver instance */
    private static TraceNLSResolver instance = new TraceNLSResolver();

    /** Instance of Finder */
    private static StackFinder finder = null;

    public final static TraceNLSResolver getInstance() {
        if (debugTraceNLSResolver == null) {
            debugTraceNLSResolver = Boolean.getBoolean(DEBUG_TRACE_NLS_PROPERTY);
            makeNoise = debugTraceNLSResolver.booleanValue();
        }

        return instance;
    }

    /**
     * Get a reference to the specified ResourceBundle and look up the specified
     * key. If the key has no value, use the defaultString instead. If so
     * indicated, format the text using the specified arguments. Such formatting
     * is done using the java.text.MessageFormat class. Substitution parameters
     * are handled according to the rules of that class. Most notably, that
     * class does special formatting for native java Date and Number objects.
     * <p>
     * If an error occurs in obtaining a reference to the ResourceBundle, or in
     * looking up the key, this method will take appropriate actions. This may
     * include returning a non-null error message.
     * <p>
     * 
     * @param aClass
     *            the class representing the caller of the method-- used for
     *            loading the right resource bundle
     * @param bundle
     *            the ResourceBundle to use for lookups. Null is tolerated. If
     *            null is passed, the resource bundle will be looked up from
     *            bundleName. If not null, bundleName must match.
     * @param bundleName
     *            the fully qualified name of the ResourceBundle. Must not be
     *            null.
     * @param key
     *            the key to use in the ResourceBundle lookup. Must not be null.
     * @param args
     *            substitution parameters that are inserted into the message
     *            text. Null is tolerated
     * @param defaultString
     *            text to use if the localized text cannot be found. Must not be
     *            null.
     * @param locale
     *            the Locale object to use when looking up the ResourceBundle.
     *            If null is passed, the default Locale will be used.
     *            <p>
     * @param quiet
     *            indicates whether or not errors will be logged when
     *            encountered, and must be used in conjunction with com.ibm.
     * @return a non-null message that is localized and formatted as
     *         appropriate.
     */
    public String getMessage(Class<?> aClass, ResourceBundle bundle, String bundleName, String key, Object[] args, String defaultString, boolean format, Locale locale, boolean quiet) {
        String returnValue = null;

        if (locale == null)
            locale = Locale.getDefault();

        try {
            // Retrieve a reference to the ResourceBundle and do the lookup on
            // the key.
            if (bundle == null)
                bundle = getResourceBundle(aClass, bundleName, locale);

            returnValue = bundle.getString(key);

            // The lookup may have returned empty string if key was found, but
            // there is no value.
            if (returnValue.equals("")) {
                // Log this occurrence, it needs to be fixed.
                if (!quiet)
                    logEvent(svMalformedMessage, new Object[] { key, bundleName });

                // Determine which value to continue with. Default text takes
                // priority over key.
                if (defaultString == null)
                    returnValue = key;
                else
                    returnValue = defaultString;
            }

            // We have a non-null returnValue, either from the lookup or we are
            // using the default text.
            if (format == false)
                return returnValue;
            else
                return getFormattedMessage(returnValue, args);
        } catch (RuntimeException re) {
            // sort our error conditions here. Order dependencies in following
            // logic
            if (bundleName == null) {
                // Caller passed a null bundleName argument. Log a message to
                // let the developer know there is a bug in his code. Proceed.
                // Need to determine what String to use. For this condition,
                // defaultText takes priority over key. One must be non-null.
                if ((key == null) && (defaultString == null)) {
                    if (!quiet)
                        logEvent(svNullBundleName, new Object[] { nullKey });

                    return MessageFormat.format(svNullBundleName, new Object[] { nullKey });
                }

                if (defaultString == null) {
                    if (!quiet)
                        logEvent(svNullBundleName, new Object[] { defaultString });

                    returnValue = key;
                } else {
                    if (!quiet)
                        logEvent(svNullBundleName, new Object[] { key });

                    returnValue = defaultString;
                }

                if (format == false)
                    return returnValue;
                else
                    return getFormattedMessage(returnValue, args);
            } // end null resourceBundleName

            if (bundle == null) {
                // ResourceBundle lookup failed. Log a message so the problem
                // can be fixed.
                if (!quiet)
                    logEvent(svBundleNotLoaded, new Object[] { bundleName });

                // Proceed. Need to determine what String to use. For this
                // condition, defaultText takes priority over key. One must be
                // non-null.
                if ((key == null) && (defaultString == null))
                    return MessageFormat.format(svBundleNotLoaded, new Object[] { bundleName });

                if (defaultString == null)
                    returnValue = key;
                else
                    returnValue = defaultString;

                if (format == false)
                    return returnValue;
                else
                    return getFormattedMessage(returnValue, args);
            } // end null ResourceBundle

            if (key == null) {
                // ResourceBundle was loaded, caller passed a null key. Log a
                // message so it can be fixed.
                if (!quiet)
                    logEvent(svNullKeyMessage, new Object[] { bundleName });

                // Proceed. For this condition, defaultText must be non-null.
                if (defaultString == null)
                    return MessageFormat.format(svNullKeyMessage, new Object[] { bundleName });
                else
                    returnValue = defaultString;

                if (format == false)
                    return returnValue;
                else
                    return getFormattedMessage(returnValue, args);
            } // end null key

            // Must be a keyNotFound in Bundle condition.
            if (!quiet)
                logEvent(svMalformedMessage, new Object[] { key, bundleName });

            // Proceed. For this condition, key cannot be null.
            if (defaultString == null)
                returnValue = key;
            else
                returnValue = defaultString;
            if (format == false)
                return returnValue;
            else
                return getFormattedMessage(returnValue, args);
        }
    }

    public String getFormattedMessage(String message, Object[] args) {
        // if there are no arguments just return the message
        if (args == null || message == null)
            return message;

        // format the message
        String formattedMessage = null;
        try {
            formattedMessage = MessageFormat.format(message, args);
        } catch (IllegalArgumentException e) {
            // tolerate this - just return the original message
            return message;
        }

        return formattedMessage;
    }

    /**
     * Looks up the specified ResourceBundle
     * 
     * This method first uses the classloader used to pass in the class argument
     * (if non-null), then it tries the current classloader, then it tries the
     * context classloader.
     * 
     * @param aClass
     *            the class representing the caller of the method-- used for
     *            loading the right resource bundle: preferably NOT null (as
     *            that is fastest, and works with OSGi, etc.)
     * @param bundleName
     *            the fully qualified name of the ResourceBundle. Must not be
     *            null (will throw an NPE).
     * @param locale
     *            the Locale object to use when looking up the ResourceBundle.
     *            If null is passed, the default Locale will be used.
     * @return ResourceBundle
     * @throws RuntimeExceptions
     *             caused by MissingResourceException or NullPointerException
     *             where resource bundle or classloader cannot be loaded
     */
    public ResourceBundle getResourceBundle(Class<?> aClass, String bundleName, Locale locale) {
        ResourceBundle bundle = null;
        ClassLoader classLoader = null;

        if (bundleName == null) // instead of waiting for ResourceBundle to
                                // throw the NPE, do it now
            throw new NullPointerException("Unable to load resource bundle: null bundleName");

        if (locale == null)
            locale = Locale.getDefault();

        // TODO: add resource bundle cache.. ?

        // yikes! TRY to figure out the class from the callstack--
        // have to do this every time aClass is null coming in, which is
        // definitely not optimal, but at least makes sure the resource bundle
        // is loaded from the right place
        if (aClass == null) {
            if (finder == null)
                finder = StackFinder.getInstance();

            if (finder != null)
                aClass = finder.getCaller();
        }

        if (aClass != null) {
            // If aClass is NOT null (it was passed in, or we found it),
            // use its classloader first to try loading the resource bundle
            try {
                classLoader = aClass.getClassLoader();
                bundle = ResourceBundle.getBundle(bundleName, locale, classLoader);
            } catch (RuntimeException re) {
                logEvent("Unable to load {0} from {1} (from class {2}) in {3}; caught exception: {4}", new Object[] { bundleName, classLoader, aClass, locale, re });
            }
        }

        if (bundle == null) {
            // If the bundle wasn't found using the class' classloader,
            // try the default classloader (in OSGi, will be in the RAS
            // bundle..)
            try {
                bundle = ResourceBundle.getBundle(bundleName, locale);
            } catch (RuntimeException re) {
                logEvent("Unable to load {0} from {1} in {2}; caught exception: {3}", new Object[] { bundleName, classLoader, locale, re });

                try {
                    // Try the context classloader
                    classLoader = (ClassLoader) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                        public Object run() throws Exception {
                            return Thread.currentThread().getContextClassLoader();
                        }
                    });

                    bundle = ResourceBundle.getBundle(bundleName, locale, classLoader);
                } catch (PrivilegedActionException pae) {
                    logEvent("Unable to load {0} from {1} in {2}; caught exception: {3}", new Object[] { bundleName, classLoader, locale, pae });
                    throw new RuntimeException("Unable to get context classloader", pae);
                }
            }
        }

        return bundle;
    }

    /**
     * Common method to use Tr to log that something above couldn't be resolved.
     * This method further checks whether or not the
     * <code>com.ibm.ejs.ras.debugTraceNLSResolver</code> property has been set
     * before calling Tr to log the event.
     * 
     * @param message
     *            Event message
     * @param args
     *            Parameters for message formatter
     */
    protected final static void logEvent(String message, Object[] args) {
        // if ( makeNoise && tc.isEventEnabled() )
        // {
        // if ( args == null )
        // com.ibm.websphere.ras.Tr.event(tc, message);
        // else
        // com.ibm.websphere.ras.Tr.event(tc, MessageFormat.format(message,
        // args));
        // }
        System.err.println("com.ibm.ejs.ras.hpel.TraceNLSResolver: "+MessageFormat.format(message, args));
        Thread.dumpStack();
    }
}
