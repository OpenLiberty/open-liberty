/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejs.ras;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.ws.logging.internal.TraceNLSBundleClassLoader;
import com.ibm.ws.logging.internal.StackFinder;
import com.ibm.ws.logging.internal.TraceNLSResolver;

/**
 * The TraceNLS class is a simple wrapper for a ResourceBundle object. The
 * wrapper mechanism is used for efficiency to allow caching of loaded
 * ResourceBundles. As far as is possible, this caching mechanism will ensure
 * that any given bundle is loaded exactly once in a process lifetime. (We are
 * at the mercy of users creating malformed Locale objects).
 * <p>
 * The methods of this class expect that the specified ResourceBundle name is
 * correctly formed. That is, it is a fully package-qualified name from where
 * the ResourceBundle resides on the classpath.
 * <p>
 * Each TraceNLS instance encapsulates or contains a reference to a
 * ResourceBundle loaded in memory. A static internally referenced component
 * performs the caching and retrieval of the instances of this class.
 * <p>
 * WARNING Since this mechanism is used by the RAS service to localize messages
 * passed to the RAS service, this class MUST NOT be instrumented with RAS APIs
 * that will result in localization of messages.
 */
public class TraceNLS {
    /** Used to actually do the work */
    static TraceNLSResolver resolver = null;

    /** Instance of Finder */
    static StackFinder finder = null;

    /** Name of the ResourceBundle for this instance as passed from caller. */
    protected String ivBundleName = null;

    /** Class of caller: retrieved from TraceComponent or during getTraceNLS */
    protected Class<?> caller = null;

    /**
     * Retrieve a TraceNLS instance for the specified ResourceBundle, after
     * first calculating the class of the caller via a stack walk (a direct call
     * passing in the class is preferred).
     * 
     * @param bundleName
     *            the package-qualified name of the ResourceBundle. The caller
     *            MUST guarantee that this is not null.
     *            <p>
     * @return a TraceNLS object. Null is never returned.
     * 
     * @deprecated Use the signature that includes the class object instead
     * @see #getTraceNLS(Class, String)
     */
    @Deprecated
    public static TraceNLS getTraceNLS(String bundleName) {
        if (resolver == null)
            resolver = TraceNLSResolver.getInstance();

        if (finder == null)
            finder = StackFinder.getInstance();

        Class<?> caller = null;

        if (finder != null)
            caller = finder.getCaller();

        return new TraceNLS(caller, bundleName);
    }

    /**
     * Retrieve a TraceNLS instance for the specified ResourceBundle.
     * <p>
     * If the specified ResourceBundle is not found, a TraceNLS object is
     * returned. Subsequent method calls on that instance will return a "bundle
     * not found" message as appropriate.
     * <p>
     * 
     * @param caller
     *            Class object requesting the NLS bundle instance
     * @param bundleName
     *            the package-qualified name of the ResourceBundle. The caller
     *            MUST guarantee that this is not null.
     *            <p>
     * @return a TraceNLS object. Null is never returned.
     */
    public static TraceNLS getTraceNLS(Class<?> caller, String bundleName) {
        if (resolver == null)
            resolver = TraceNLSResolver.getInstance();
        // TODO: CACHE
        return new TraceNLS(caller, bundleName);
    }

    /**
     * Construct a TraceNLS instance for the specified ResourceBundle.
     * 
     * @param caller
     *            Class object requesting the NLS bundle instance
     * @param bundleName
     *            the package-qualified name of the ResourceBundle. Must NOT be
     *            null.
     */
    private TraceNLS(Class<?> caller, String bundleName) {
        this.caller = caller;
        ivBundleName = bundleName;
    }

    /**
     * Retrieve the localized text corresponding to the specified key from the
     * ResourceBundle that this instance wrappers. If the text cannot be found
     * for any reason, an appropriate error message is returned instead.
     * <p>
     * 
     * @param key
     *            the key to use in the ResourceBundle lookup. Null is
     *            tolerated.
     * @return the appropriate non-null message.
     */
    public String getString(String key) {
        return resolver.getMessage(caller, null, ivBundleName, key, null, null, false, null, false);
    }

    /**
     * Retrieve the localized text corresponding to the specified key from the
     * ResourceBundle represented by this TraceNLS instance. If the text cannot
     * be found for any reason and the defaultString is non-null, then the
     * defaultString is returned. Otherwise an appropriate error message is
     * returned instead.
     * <p>
     * 
     * @param key
     *            the key to use in the ResourceBundle lookup. Null is
     *            tolerated.
     * @param defaultString
     *            text to return if text cannot be found. Null is tolerated.
     * @return the appropriate non-null message
     */
    public String getString(String key, String defaultString) {
        return resolver.getMessage(caller, null, ivBundleName, key, null, defaultString, false, null, false);
    }

    /**
     * Return the message obtained by looking up the localized text indicated by
     * the key in the ResourceBundle wrapped by this instance, then formatting
     * the message using the specified arguments as substitution parameters.
     * <p>
     * The message is formatted using the java.text.MessageFormat class.
     * Substitution parameters are handled according to the rules of that class.
     * Most noteably, that class does special formatting for native java Date and
     * Number objects.
     * <p>
     * If an error occurs in obtaining the localized text corresponding to this
     * key, then the defaultString is used as the message text. If all else fails,
     * this class will provide one of the default English messages to indicate
     * what occurred.
     * <p>
     * 
     * @param key
     *            the key to use in the ResourceBundle lookup. Null is tolerated
     * @param args
     *            substitution parameters that are inserted into the message
     *            text. Null is tolerated
     * @param defaultString
     *            text to use if the localized text cannot be found. Null is
     *            tolerated
     *            <p>
     * @return a non-null message that is localized and formatted as
     *         appropriate.
     */
    public String getFormattedMessage(String key, Object[] args, String defaultString) {
        return resolver.getMessage(caller, null, ivBundleName, key, args, defaultString, true, null, false);
    }

    /**
     * Return the message obtained by looking up the localized text indicated by
     * the key in the ResourceBundle wrapped by this instance, then formatting
     * the message using the specified arguments as substitution parameters.
     * <p>
     * The message is formatted using the java.text.MessageFormat class.
     * Substitution parameters are handled according to the rules of that class.
     * Most noteably, that class does special formatting for native java Date and
     * Number objects.
     * <p>
     * If an error occurs in obtaining the localized text corresponding to this
     * key, then the defaultString is used as the message text. If all else fails,
     * this class will provide one of the default English messages to indicate
     * what occurred.
     * <p>
     * 
     * @param key
     *            the key to use in the ResourceBundle lookup. Null is tolerated
     * @param args
     *            substitution parameters that are inserted into the message
     *            text. Null is tolerated
     * @param defaultString
     *            text to use if the localized text cannot be found. Null is
     *            tolerated
     * @param quiet
     *            indicates whether or not errors will be logged when
     *            encountered
     *            <p>
     * @return a non-null message that is localized and formatted as
     *         appropriate.
     */
    public String getFormattedMessage(String key, Object[] args, String defaultString, boolean quiet) {
        if (resolver == null)
            resolver = TraceNLSResolver.getInstance();

        return resolver.getMessage(caller, null, ivBundleName, key, args, defaultString, true, null, quiet);
    }

    // ////////////////////////////////////////
    //
    // Methods that define the cache-less model
    //
    // ///////////////////////////////////////

    /**
     * Retrieve the localized text corresponding to the specified key in the
     * specified ResourceBundle. If an error is encountered, an appropriate
     * error message is returned instead.
     * <p>
     * 
     * @param caller
     *            Class object calling this method
     * @param bundleName
     *            the fully qualified name of the ResourceBundle. Must not be
     *            null.
     * @param key
     *            the key to use in the ResourceBundle lookup. Must not be null.
     * @return the value corresponding to the specified key in the specified
     *         ResourceBundle, or the appropriate non-null error message.
     */
    public static String getStringFromBundle(Class<?> caller, String bundleName, String key) {
        if (resolver == null)
            resolver = TraceNLSResolver.getInstance();

        return resolver.getMessage(caller, null, bundleName, key, null, null, false, null, false);
    }

    /**
     * @deprecated Use version that includes class parameter
     * @see #getStringFromBundle(Class, String, String)
     */
    @Deprecated
    public static String getStringFromBundle(String bundleName, String key) {
        if (resolver == null)
            resolver = TraceNLSResolver.getInstance();

        return resolver.getMessage((Class<?>) null, null, bundleName, key, null, null, false, null, false);
    }

    // -----------------------------------------------------------------------

    /**
     * Retrieve the localized text corresponding to the specified key in the
     * specified ResourceBundle. If the text cannot be found for any reason, the
     * defaultString is returned. If an error is encountered, an appropriate
     * error message is returned instead.
     * <p>
     * 
     * @param caller
     *            Class object calling this method
     * @param bundleName
     *            the fully qualified name of the ResourceBundle. Must not be
     *            null.
     * @param key
     *            the key to use in the ResourceBundle lookup. Must not be null.
     * @param defaultString
     *            text to return if text cannot be found. Must not be null.
     * @return the value corresponding to the specified key in the specified
     *         ResourceBundle, or the appropriate non-null error message.
     */
    public static String getStringFromBundle(Class<?> caller, String bundleName, String key, String defaultString) {
        if (resolver == null)
            resolver = TraceNLSResolver.getInstance();

        return resolver.getMessage(caller, null, bundleName, key, null, defaultString, false, null, false);
    }

    /**
     * @deprecated Use version that includes class parameter
     * @see #getStringFromBundle(Class, String, String, String)
     */
    @Deprecated
    public static String getStringFromBundle(String bundleName, String key, String defaultString) {
        if (resolver == null)
            resolver = TraceNLSResolver.getInstance();

        return resolver.getMessage((Class<?>) null, null, bundleName, key, null, defaultString, false, null, false);
    }

    // -----------------------------------------------------------------------

    /**
     * Return the message obtained by looking up the localized text
     * corresponding to the specified key in the specified ResourceBundle and
     * formatting the resultant text using the specified substitution arguments.
     * <p>
     * The message is formatted using the java.text.MessageFormat class.
     * Substitution parameters are handled according to the rules of that class.
     * Most noteably, that class does special formatting for native java Date and
     * Number objects.
     * <p>
     * If an error occurs in obtaining the localized text corresponding to this
     * key, then the defaultString is used as the message text. If all else fails,
     * this class will provide one of the default English messages to indicate
     * what occurred.
     * <p>
     * 
     * @param caller
     *            Class object calling this method
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
     *            <p>
     * @return a non-null message that is localized and formatted as
     *         appropriate.
     */
    public static String getFormattedMessage(Class<?> caller, String bundleName, String key, Object[] args, String defaultString) {
        return TraceNLSResolver.getInstance().getMessage(caller, null, bundleName, key, args, defaultString, true, null, false);
    }

    /**
     * @deprecated Use version that includes class parameter
     * @see #getFormattedMessage(Class, String, String, Object[], String)
     */
    @Deprecated
    public static String getFormattedMessage(String bundleName, String key, Object[] args, String defaultString) {
        return TraceNLSResolver.getInstance().getMessage((Class<?>) null, null, bundleName, key, args, defaultString, true, null, false);
    }

    // -----------------------------------------------------------------------

    /**
     * Return the message obtained by looking up the localized text
     * corresponding to the specified key in the specified ResourceBundle and
     * formatting the resultant text using the specified substitution arguments.
     * <p>
     * The message is formatted using the java.text.MessageFormat class.
     * Substitution parameters are handled according to the rules of that class.
     * Most noteably, that class does special formatting for native java Date and
     * Number objects.
     * <p>
     * If an error occurs in obtaining the localized text corresponding to this
     * key, then the defaultString is used as the message text. If all else fails,
     * this class will provide one of the default English messages to indicate
     * what occurred.
     * <p>
     * 
     * @param caller
     *            Class object calling this method
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
     * @param quiet
     *            indicates whether or not errors will be logged when
     *            encountered
     *            <p>
     * @return a non-null message that is localized and formatted as
     *         appropriate.
     */
    public static String getFormattedMessage(Class<?> caller, String bundleName, String key, Object[] args, String defaultString, boolean quiet) {
        return TraceNLSResolver.getInstance().getMessage(caller, null, bundleName, key, args, defaultString, true, null, quiet);
    }

    /**
     * @deprecated Use version that includes class parameter
     * @see #getFormattedMessage(Class, String, String, Object[], String, boolean)
     */
    @Deprecated
    public static String getFormattedMessage(String bundleName, String key, Object[] args, String defaultString, boolean quiet) {
        return TraceNLSResolver.getInstance().getMessage((Class<?>) null, null, bundleName, key, args, defaultString, true, null, quiet);
    }

    // -----------------------------------------------------------------------

    /**
     * Retrieve the localized text corresponding to the specified key in the
     * specified ResourceBundle using the specified Locale. If an error is
     * encountered, an appropriate error message is returned instead.
     * <p>
     * 
     * @param caller
     *            Class object calling this method
     * @param bundleName
     *            the fully qualified name of the ResourceBundle. Must not be
     *            null.
     * @param key
     *            the key to use in the ResourceBundle lookup. Must not be null.
     * @param locale
     *            the Locale object to use when looking up the ResourceBundle.
     *            Must not be null.
     * @return the value corresponding to the specified key in the specified
     *         ResourceBundle, or the appropriate non-null error message.
     */
    public static String getStringFromBundle(Class<?> caller, String bundleName, String key, Locale locale) {
        return TraceNLSResolver.getInstance().getMessage(caller, null, bundleName, key, null, null, false, locale, false);
    }

    /**
     * @deprecated Use version that includes class parameter
     * @see #getStringFromBundle(Class, String, String, Locale)
     */
    @Deprecated
    public static String getStringFromBundle(String bundleName, String key, Locale locale) {
        if (resolver == null)
            resolver = TraceNLSResolver.getInstance();

        return TraceNLSResolver.getInstance().getMessage((Class<?>) null, null, bundleName, key, null, null, false, locale, false);
    }

    // -----------------------------------------------------------------------

    /**
     * Retrieve the localized text corresponding to the specified key in the
     * specified ResourceBundle using the specified Locale. If the text cannot
     * be found for any reason, the defaultString is returned. If an error is
     * encountered, an appropriate error message is returned instead.
     * <p>
     * 
     * @param caller
     *            Class object calling this method
     * @param bundleName
     *            the fully qualified name of the ResourceBundle. Must not be
     *            null.
     * @param key
     *            the key to use in the ResourceBundle lookup. Must not be null.
     * @param locale
     *            the Locale object to use when looking up the ResourceBundle.
     *            Must not be null.
     * @param defaultString
     *            text to return if text cannot be found. Must not be null.
     * @return the value corresponding to the specified key in the specified
     *         ResourceBundle, or the appropriate non-null error message.
     */
    public static String getStringFromBundle(Class<?> caller, String bundleName, String key, Locale locale, String defaultString) {
        return TraceNLSResolver.getInstance().getMessage(caller, null, bundleName, key, null, defaultString, false, locale, false);
    }

    /**
     * @deprecated Use version that includes class parameter
     * @see #getStringFromBundle(Class, String, String, Locale, String)
     */
    @Deprecated
    public static String getStringFromBundle(String bundleName, String key, Locale locale, String defaultString) {
        return TraceNLSResolver.getInstance().getMessage((Class<?>) null, null, bundleName, key, null, defaultString, false, locale, false);
    }

    // -----------------------------------------------------------------------

    /**
     * Retrieve the localized text corresponding to the specified key in the
     * specified ResourceBundle using the specified Locale. If an error is
     * encountered, an appropriate error message is returned instead.
     * <p>
     * 
     * @param caller
     *            Class object calling this method
     * @param bundle
     *            the ResourceBundle to use for lookups. Null is tolerated. If
     *            null is passed, the resource bundle will be looked up from
     *            bundleName. If not null, bundleName must match.
     * @param bundleName
     *            the fully qualified name of the ResourceBundle. Must not be
     *            null.
     * @param key
     *            the key to use in the ResourceBundle lookup. Must not be null.
     * @param locale
     *            the Locale object to use when looking up the ResourceBundle.
     *            Must not be null.
     * @return the value corresponding to the specified key in the specified
     *         ResourceBundle, or the appropriate non-null error message.
     */
    public static String getStringFromBundle(Class<?> caller, ResourceBundle bundle, String bundleName, String key, Locale locale) {
        return TraceNLSResolver.getInstance().getMessage(caller, bundle, bundleName, key, null, null, false, locale, false);
    }

    /**
     * @deprecated Use version that includes class parameter
     * @see #getStringFromBundle(Class, ResourceBundle, String, String, Locale)
     */
    @Deprecated
    public static String getStringFromBundle(ResourceBundle bundle, String bundleName, String key, Locale locale) {
        return TraceNLSResolver.getInstance().getMessage((Class<?>) null, bundle, bundleName, key, null, null, false, locale, false);
    }

    // -----------------------------------------------------------------------

    /**
     * Retrieve the localized text corresponding to the specified key in the
     * specified ResourceBundle using the specified Locale. If the text cannot
     * be found for any reason, the defaultString is returned. If an error is
     * encountered, an appropriate error message is returned instead.
     * <p>
     * 
     * @param caller
     *            Class object calling this method
     * @param bundle
     *            the ResourceBundle to use for lookups. Null is tolerated. If
     *            null is passed, the resource bundle will be looked up from
     *            bundleName. If not null, bundleName must match.
     * @param bundleName
     *            the fully qualified name of the ResourceBundle. Must not be
     *            null.
     * @param key
     *            the key to use in the ResourceBundle lookup. Must not be null.
     * @param locale
     *            the Locale object to use when looking up the ResourceBundle.
     *            Must not be null.
     * @param defaultString
     *            text to return if text cannot be found. Must not be null.
     * @return the value corresponding to the specified key in the specified
     *         ResourceBundle, or the appropriate non-null error message.
     */
    public static String getStringFromBundle(Class<?> caller, ResourceBundle bundle, String bundleName, String key, Locale locale, String defaultString) {
        return TraceNLSResolver.getInstance().getMessage(caller, bundle, bundleName, key, null, defaultString, false, locale, false);
    }

    /**
     * @deprecated Use version that includes class parameter
     * @see #getStringFromBundle(Class, ResourceBundle, String, String, Locale, String)
     */
    @Deprecated
    public static String getStringFromBundle(ResourceBundle bundle, String bundleName, String key, Locale locale, String defaultString) {
        return TraceNLSResolver.getInstance().getMessage((Class<?>) null, bundle, bundleName, key, null, defaultString, false, locale, false);
    }

    // -----------------------------------------------------------------------

    /**
     * Return the message obtained by looking up the localized text
     * corresponding to the specified key in the specified ResourceBundle using
     * the specified Locale and formatting the resultant text using the
     * specified substitution arguments.
     * <p>
     * The message is formatted using the java.text.MessageFormat class.
     * Substitution parameters are handled according to the rules of that class.
     * Most noteably, that class does special formatting for native java Date and
     * Number objects.
     * <p>
     * If an error occurs in obtaining the localized text corresponding to this
     * key, then the defaultString is used as the message text. If all else fails,
     * this class will provide one of the default English messages to indicate
     * what occurred.
     * <p>
     * 
     * @param caller
     *            Class object calling this method
     * @param bundleName
     *            the fully qualified name of the ResourceBundle. Must not be
     *            null.
     * @param key
     *            the key to use in the ResourceBundle lookup. Must not be null.
     * @param locale
     *            the Locale object to use when looking up the ResourceBundle.
     *            Must not be null.
     * @param args
     *            substitution parameters that are inserted into the message
     *            text. Null is tolerated
     * @param defaultString
     *            text to use if the localized text cannot be found. Must not be
     *            null.
     *            <p>
     * @return a non-null message that is localized and formatted as
     *         appropriate.
     */
    public static String getFormattedMessage(Class<?> caller, String bundleName, String key, Locale locale, Object[] args, String defaultString) {
        return TraceNLSResolver.getInstance().getMessage(caller, null, bundleName, key, args, defaultString, true, locale, false);
    }

    /**
     * @deprecated Use version that includes class parameter
     * @see #getFormattedMessage(Class, String, String, Locale, Object[], String)
     */
    @Deprecated
    public static String getFormattedMessage(String bundleName, String key, Locale locale, Object[] args, String defaultString) {
        return TraceNLSResolver.getInstance().getMessage((Class<?>) null, null, bundleName, key, args, defaultString, true, locale, false);
    }

    // -----------------------------------------------------------------------

    /**
     * Return the message obtained by looking up the localized text
     * corresponding to the specified key in the specified ResourceBundle using
     * the specified Locale and formatting the resultant text using the
     * specified substitution arguments.
     * <p>
     * The message is formatted using the java.text.MessageFormat class.
     * Substitution parameters are handled according to the rules of that class.
     * Most noteably, that class does special formatting for native java Date and
     * Number objects.
     * <p>
     * If an error occurs in obtaining the localized text corresponding to this
     * key, then the defaultString is used as the message text. If all else fails,
     * this class will provide one of the default English messages to indicate
     * what occurred.
     * <p>
     * 
     * @param caller
     *            Class object calling this method
     * @param bundleName
     *            the fully qualified name of the ResourceBundle. Must not be
     *            null.
     * @param key
     *            the key to use in the ResourceBundle lookup. Must not be null.
     * @param locale
     *            the Locale object to use when looking up the ResourceBundle.
     *            Must not be null.
     * @param args
     *            substitution parameters that are inserted into the message
     *            text. Null is tolerated
     * @param defaultString
     *            text to use if the localized text cannot be found. Must not be
     *            null.
     * @param quiet
     *            indicates whether or not errors will be logged when
     *            encountered
     *            <p>
     * @return a non-null message that is localized and formatted as
     *         appropriate.
     */
    public static String getFormattedMessage(Class<?> caller, String bundleName, String key, Locale locale, Object[] args, String defaultString, boolean quiet) {
        return TraceNLSResolver.getInstance().getMessage(caller, null, bundleName, key, args, defaultString, true, locale, quiet);
    }

    /**
     * @deprecated Use version that includes class parameter
     * @see #getFormattedMessage(Class, String, String, Locale, Object[], String, boolean)
     */
    @Deprecated
    public static String getFormattedMessage(String bundleName, String key, Locale locale, Object[] args, String defaultString, boolean quiet) {
        return TraceNLSResolver.getInstance().getMessage((Class<?>) null, null, bundleName, key, args, defaultString, true, locale, quiet);
    }

    // -----------------------------------------------------------------------

    /**
     * Return the formatted message obtained by substituting parameters passed
     * into a message
     * 
     * @param localizedMessage
     *            the message into which parameters will be substituted
     * @param args
     *            the arguments that will be substituted into the message
     * @param quiet
     *            indicates whether or not errors will be logged when
     *            encountered
     * @return String a message with parameters substituted in as appropriate
     */
    public static String getFormattedMessageFromLocalizedMessage(String localizedMessage, Object[] args, boolean quiet) {
        return TraceNLSResolver.getInstance().getFormattedMessage(localizedMessage, args);
    }

    /**
     * Return the message obtained by looking up the localized text
     * corresponding to the specified key in the specified ResourceBundle using
     * the specified Locale and formatting the resultant text using the
     * specified substitution arguments.
     * <p>
     * The message is formatted using the java.text.MessageFormat class.
     * Substitution parameters are handled according to the rules of that class.
     * Most noteably, that class does special formatting for native java Date and
     * Number objects.
     * <p>
     * If an error occurs in obtaining the localized text corresponding to this
     * key, then the defaultString is used as the message text. If all else fails,
     * this class will provide one of the default English messages to indicate
     * what occurred.
     * <p>
     * 
     * @param aClass
     *            Class object calling this method
     * @param bundle
     *            the ResourceBundle to use for lookups. Null is tolerated. If
     *            null is passed, the resource bundle will be looked up from
     *            bundleName. If not null, bundleName must match.
     * @param bundleName
     *            the fully qualified name of the ResourceBundle. Must not be
     *            null.
     * @param aClass
     *            the class representing the caller of the method-- used for
     *            loading the right resource bundle
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

    public static String getFormattedMessage(Class<?> aClass, ResourceBundle bundle, String bundleName,
                                             String key, Object[] args, String defaultString,
                                             Locale locale,
                                             boolean quiet) {

        return TraceNLSResolver.getInstance().getMessage(aClass,
                                                         bundle,
                                                         bundleName,
                                                         key,
                                                         args,
                                                         defaultString,
                                                         true,
                                                         locale,
                                                         quiet);

    }

    /**
     * Return the Base (English) resource bundle since we do not explicitly ship the _en bundle.
     * 
     * @param aClass
     *            the class utilizing the resource bundle
     * 
     * @param aBundleName
     *            the name of the resource bundle
     * 
     * @return ResourceBundle
     *         the base resource bundle
     */

    public static ResourceBundle getBaseResourceBundle(Class<?> aClass, String aBundleName) {

        ResourceBundle bundle = null;
        final Class<?> clazz = aClass;
        final String bundleName = aBundleName;

        // Need a special classloader trick to load the ENGLISH messages (from the base bundle).
        // See BaseResourceBundleClassLoader for more info.
        ClassLoader cl = AccessController.doPrivileged(
                        new PrivilegedAction<ClassLoader>() {
                            public ClassLoader run() {
                                return new TraceNLSBundleClassLoader(clazz.getClassLoader(), bundleName);
                            }
                        });

        bundle = ResourceBundle.getBundle(bundleName, Locale.ENGLISH, cl);
        return bundle;

    }

    // -----------------------------------------------------------------------

    /**
     * Looks up the specified ResourceBundle
     * 
     * This method first uses the current classLoader to find the
     * ResourceBundle. If that fails, it uses the context classLoader.
     * 
     * @param caller
     *            Class object calling this method
     * @param bundleName
     *            the fully qualified name of the ResourceBundle. Must not be
     *            null.
     * @param locale
     *            the Locale object to use when looking up the ResourceBundle.
     *            Must not be null.
     * @return ResourceBundle
     * @throws RuntimeExceptions
     *             caused by MissingResourceException or NullPointerException
     *             where resource bundle or classloader cannot be loaded
     */
    public static ResourceBundle getResourceBundle(Class<?> caller, String bundleName, Locale locale) {
        return TraceNLSResolver.getInstance().getResourceBundle(caller, bundleName, locale);
    }

    /**
     * @deprecated Use version that includes class parameter
     * @see #getResourceBundle(Class, String, Locale)
     */
    @Deprecated
    public static ResourceBundle getResourceBundle(String bundleName, Locale locale) {
        return TraceNLSResolver.getInstance().getResourceBundle((Class<?>) null, bundleName, locale);
    }
}