/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.pmi.server;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Backbone of Internationalization within the WebSphere Application
 * Server. All globalization resources should flow through this class.
 * 
 * The process for a developer internationalizing their code is as
 * follows:
 * <ul>
 * <li>
 * Create an instance of this class, passing in the resource
 * bundle, and optionally the locale, as parameters. One can
 * also specify whether or not the resource bundle name is fully
 * qualified (includes the package name). See the constructors
 * for more information.
 * </li>
 * <li>
 * Call the getString method to retrieve the string from the
 * resource bundle that was loaded by the constructor. See the
 * getString methods for more information.
 * </li>
 * </ul>
 * 
 * <b> Comments: </b>
 * <ul>
 * Probably we should extend from PropertyResourceBundle class, but
 * I don't want to change the interface. (Qinhua)
 * </ul>
 **/
public class NLS {
    /**
     * Registers a trace object for this class with the Trace
     * subsystem.
     **/
    private static TraceComponent tc = Tr.register(NLS.class);
    // DGH20040823: Begin fix for defect 208167
    // private static final NLS messages = new NLS("messages");
    // DGH20040823: Begin fix for defect 218797 - Source provided by Tom Musta
    // private static final NLS messages = new NLS(baseClassPackage + ".messages");
    private static NLS messages = null;

    private static NLS getMessages() {
        if (messages == null) {
            messages = new NLS("messages");
        }
        return messages;
    }

    // DGH20040823: End fix for defect 218797
    // DGH20040823: End fix for defect 208167
    /**
     * Default package name for resource bundles. If the fully
     * qualified package name was not passed in as part of the resource
     * bundle name (String), and / or the constructor containing the
     * fullyQualified parameter is not used, the code tries to load
     * the resource bundle using this package name. At the time of
     * writing, the baseClassPackage was "com.ibm.ejs.resources".
     **/
    static final String baseClassPackage = "com.ibm.ejs.resources";
    /**
     * The name of the bundle that will be loaded. Can be either a
     * filename or a filename prepended with a package name.
     **/
    private String bundleName;
    /**
     * Represents the resource bundle that was loaded.
     **/
    private ResourceBundle bundle = null;

    /**
     * Vanilla Constructor. It is assumed that the default locale will
     * be used. Calls Explicit Constructor (passing in default locale).
     * 
     * @param bundleName Name of the resource bundle to be loaded.
     * 
     **/
    public NLS(String bundleName) {
        this(bundleName, Locale.getDefault());
    }

    /**
     * Explicit Constructor. Assumes that the resource bundle was
     * passed in with the package name explicitly specified. If that
     * fails, tries to load with baseClassPackage (at the time of this
     * writing, the baseClassPackage was "com.ibm.ejs.resources". If
     * that fails, throws a MissingResourceException.
     * 
     * @param bundleName Name of the bundle to be loaded.
     * @param locale locale in which the bundle should be loaded.
     * 
     **/
    public NLS(String bundleName, Locale locale) {
        // Defect 85483.1 should change the entire method to the
        // "this" call below.
        // this(bundleName, Locale, false);
        // Temp workaround begin.  This should be removed by defect 85483.1.
        if (tc.isEntryEnabled())
            Tr.entry(tc, "NLS, " + bundleName + '/' + locale);
        this.bundleName = bundleName;
        // Defect 88346: Replace the try/catch with the one below, as per suggestion in defect
        // Defect 92864 Back out change for defect 88346
        /*
         * try {
         * String fullBundleName = bundleName;
         * bundle = ResourceBundle.getBundle(fullBundleName,locale);
         * } catch (MissingResourceException e) {
         * try {
         * String fullBundleName = baseClassPackage + "." + bundleName;
         * bundle = ResourceBundle.getBundle(fullBundleName,locale);
         * } catch (MissingResourceException ex) {
         * if (tc.isEventEnabled())
         * Tr.warning(tc, "Encountered an error while loading class PropertyResourceBundle " +
         * "from a JAR file {0}", new Object[] {ex});
         * }
         * }
         */
        /*
         * try {
         * String fullBundleName = baseClassPackage + "." + bundleName;
         * bundle = ResourceBundle.getBundle(fullBundleName,locale);
         * } catch (MissingResourceException ex) {
         * if (tc.isEventEnabled())
         * Tr.warning(tc, "Encountered an error while loading class PropertyResourceBundle " +
         * "from a JAR file {0}", new Object[] {ex});
         * }
         */
        // Going back to the original
        try {
            String fullBundleName = bundleName;
            bundle = TraceNLS.getResourceBundle(fullBundleName, locale);
        } catch (MissingResourceException e) {
            // DGH20040823: Begin Defect 208167
            // Ffdc.log(e, this, "com.ibm.ejs.sm.client.ui.NLS.NLS", "166", this);
            // DGH20040823: End Defect 208167
            try {
                String fullBundleName = baseClassPackage + "." + bundleName;
                bundle = TraceNLS.getResourceBundle(fullBundleName, locale);
            } catch (MissingResourceException ex) {
                if (tc.isEventEnabled()) {
                    // DGH20040823: Begin fix for defect 218797 - Source provided by Tom Musta
                    // String msg = messages.getString("NLS.resourceBundleError",
                    //                                "Encountered an error while loading class PropertyResourceBundle from a JAR file {0}");
                    String msg = getMessages().getString("NLS.resourceBundleError",
                                                         "Encountered an error while loading class PropertyResourceBundle from a JAR file {0}");
                    // DGH20040823: End fix for defect 218797 - Source provided by Tom Musta
                    Tr.warning(tc, msg, new Object[] { ex });
                }
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "NLS");
        // Temp workaround end (Should be removed by defect 85483.1).
    }

    /**
     * Constructor for fully qualified resource bundle name. If true
     * was passed in for the fullyQualified parameter, it is assumed
     * that the package was passed in as part of the bundleName, and
     * that it is correct. If the the package is incorrect or omitted,
     * the bundle will not be found, and a MissingResourceException
     * will be thrown.
     * 
     * This constructor assumes that the default locale will be used.
     * 
     * @param bundleName Name of the bundle to be loaded.
     * @param fullyQualified True if the package name was included in
     *            the bundleName, false if only the filename
     *            was included.
     **/
    public NLS(String bundleName, boolean fullyQualified) {
        this(bundleName, Locale.getDefault(), fullyQualified, true);
    }

    /**
     * Constructor with implicit FFDC logging, if true was passed in for the
     * fullyQualified parameter, it is assumed that the package was
     * passed in as the bundleName, and that it is correct. If the
     * the package is incorrect or omitted, the bundle will not be
     * found, and a MissingResourceException will be thrown.
     * 
     * @param bundleName Name of the bundle to be loaded.
     * @param locale Locale in which the resource bundle is to
     *            be loaded.
     * @param fullyQualified True if the package name was included in
     *            the bundleName, false if only the filename
     *            was included.
     **/
    public NLS(String bundleName, Locale locale, boolean fullyQualified) {
        this(bundleName, locale, fullyQualified, true);
    }

    /**
     * Explicit Constructor. If true was passed in for the
     * fullyQualified parameter, it is assumed that the package was
     * passed in as the bundleName, and that it is correct. If the
     * the package is incorrect or omitted, the bundle will not be
     * found, and a MissingResourceException will be thrown.
     * 
     * @param bundleName Name of the bundle to be loaded.
     * @param locale Locale in which the resource bundle is to
     *            be loaded.
     * @param fullyQualified True if the package name was included in
     *            the bundleName, false if only the filename
     *            was included.
     * @param logFFDC True if a missing resource expception should
     *            be logged to the FFDC log.
     **/
    public NLS(String bundleName, Locale locale, boolean fullyQualified, boolean logFFDC) {
        // Trace
        if (tc.isEntryEnabled())
            Tr.entry(tc, "NLS, " + bundleName + '/' + locale);
        // Initialize bundleName.
        this.bundleName = bundleName;
        try {
            String fullBundleName;
            // If the bundle name was fully qualified, then use it,
            // otherwise prepend the default package to the bundle
            // name.
            fullBundleName = fullyQualified ? bundleName :
                             baseClassPackage + "." + bundleName;
            // Load the bundle.
            bundle = TraceNLS.getResourceBundle(fullBundleName, locale);
        } catch (MissingResourceException ex) {
            if (logFFDC) {
                if (tc.isEventEnabled()) {
                    // DGH20040823: Begin fix for defect 218797 - Source provided by Tom Musta
                    String msg = getMessages().getString("NLS.resourceBundleError",
                                                         "Encountered an error while loading class PropertyResourceBundle from a JAR file {0}");
                    // String msg = messages.getString("NLS.resourceBundleError",
                    //                                "Encountered an error while loading class PropertyResourceBundle from a JAR file {0}");
                    // DGH20040823: End fix for defect 218797
                    Tr.warning(tc, msg, new Object[] { ex });
                }
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "NLS");
    }

    /**
     * Explicit Constructor. Assumes that the resource bundle was
     * passed in with the package name explicitly specified. If that
     * fails, tries to load with baseClassPackage (at the time of this
     * writing, the baseClassPackage was "com.ibm.ejs.resources". If
     * that fails, throws a MissingResourceException.
     * 
     * @param bundleName Name of the bundle to be loaded.
     * @param locale locale in which the bundle should be loaded.
     * @param cl ClassLoader to use
     **/
    public NLS(String bundleName, Locale locale, ClassLoader cl) {
        // Trace
        if (tc.isEntryEnabled())
            Tr.entry(tc, "NLS, " + bundleName + '/' + locale);
        // Initialize bundleName.
        this.bundleName = bundleName;
        try {
            // Load the bundle.
            bundle = ResourceBundle.getBundle(bundleName, locale, cl);
        } catch (Exception ex) {
            // No FFDC code needed, we will FFDC later if we still fail to load 
            // the bundle
            try {
                bundle = TraceNLS.getResourceBundle(bundleName, locale);
            } catch (Exception ex2) {
                if (tc.isEventEnabled()) {
                    // DGH20040823: Begin fix for defect 218797 - Source provided by Tom Musta
                    // String msg = messages.getString("NLS.resourceBundleError",
                    //                                 "Encountered an error while loading class PropertyResourceBundle from a JAR file {0}");
                    String msg = getMessages().getString("NLS.resourceBundleError",
                                                         "Encountered an error while loading class PropertyResourceBundle from a JAR file {0}");
                    // DGH20040823: End fix for defect 218797 - Source provided by Tom Musta
                    Tr.warning(tc, msg, new Object[] { ex });
                }
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "NLS");
    }

    /**
     * Overrides ResourceBundle.getString. Adds some error checking to ensure that
     * we got a non-null key and resource bundle.
     * 
     * @param key Name portion of "name=value" pair.
     * @return rtn The resource string.
     * 
     **/
    public String getString(String key) throws MissingResourceException {
        // Error Checking Trace.  Ensure that we got a valid key
        if (key == null) {
            if (tc.isEventEnabled()) {

                // DGH20040823: Begin fix for defect 218797 - Source provided by Tom Musta
                // String msg = messages.getString("NLS.nullKey",
                //                                 "Null lookup key passed to NLS");
                String msg = getMessages().getString("NLS.nullKey",
                                                     "Null lookup key passed to NLS");
                // DGH20040823: End fix for defect 218797 - Source provided by Tom Musta
                Tr.warning(tc, msg);
            }
            return null;
        }
        // Error Checking Trace.  Ensure that we got a valid bundle.
        if (bundle == null) {
            if (tc.isEventEnabled()) {
                // DGH20040823: Begin fix for defect 218797 - Source provided by Tom Musta
                // String msg = messages.getString("NLS.noBundles",
                //                                 "Encountered an internal error. No valid bundles.");
                String msg = getMessages().getString("NLS.noBundles",
                                                     "Encountered an internal error. No valid bundles.");
                // DGH20040823: End fix for defect 218797 - Source provided by Tom Musta
                Tr.warning(tc, msg);
            }
            throw new MissingResourceException(bundleName, bundleName, key);
        }
        // Return Code.
        String rtn = null;
        // Get the resource (string).
        rtn = bundle.getString(key);
        return rtn;
    }

    /**
     * Overrides ResourceBundle.getString. Adds some error checking to ensure that
     * we got a non-null key and resource bundle.
     * 
     * Adds default string functionality. If the key for the resource
     * was not found, return the default string passed in. This way
     * something displays, even if it is in English.
     * 
     * @param key Name portion of "name=value" pair.
     * @param defaultString String to return if the key was null, or if
     *            resource was not found
     *            (MissingResourceException thrown).
     * 
     **/
    public String getString(String key, String defaultString) {
        if (key == null) {
            return defaultString;
        }
        try {
            String result = getString(key);
            return result;
        } catch (MissingResourceException e) {
            return defaultString;
        }
    }

    /**
     * An easy way to pass variables into the messages. Provides a
     * consistent way of formatting the {0} type parameters. Returns
     * the formatted resource, or if it is not found, the formatted
     * default string that was passed in.
     * 
     * @param key Resource lookup key
     * @param args Variables to insert into the string.
     * @param defaultString Default string to use if the resource
     *            is not found.
     **/
    public String getFormattedMessage(String key, Object[] args, String defaultString) {
        try {
            String result = getString(key);
            return MessageFormat.format(result, args);
        } catch (MissingResourceException e) {
            return MessageFormat.format(defaultString, args);
        }
    }

    /**
     * Not sure why this is here. Looks like it is a replacement for
     * Integer.getInteger with error checking for the key (non-null
     * check).
     **/
    public int getInteger(String key, int defaultValue) {
        int result = defaultValue;
        try {
            if (key != null)
                result = getInteger(key);
        } catch (MissingResourceException e) {
        }
        return result;
    }

    /**
     * Not sure why this is here. Looks like it is a replacement for
     * Integer.getInteger.
     **/
    public int getInteger(String key) throws MissingResourceException {
        String result = getString(key);
        try {
            return Integer.parseInt(result);
        } catch (NumberFormatException nfe) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Unable to parse " + result + " as Integer.");
            }
            // DGH20040823: Begin fix for defect 218797 - Source provided by Tom Musta
            // String msg = messages.getString("NLS.integerParseError",
            //                                 "Unable to parse as integer.");
            String msg = getMessages().getString("NLS.integerParseError",
                                                 "Unable to parse as integer.");
            // DGH20040823: End fix for defect 218797 - Source provided by Tom Musta
            throw new MissingResourceException(msg, bundleName, key);
        }
    }

    /**
     * @return Current Locale being used by the NLS class.
     **/
    public Locale getLocale() {
        return bundle.getLocale();
    }

    /**
     * @return The name of the current resource bundle.
     **/
    public String getName() {
        return bundleName;
    }

    /**
     * Use instead of ResourceBundle.getString. Adds some error
     * checking to ensure that we got a non-null key and resource
     * bundle.
     * 
     * @param key Name portion of "name=value" pair.
     * @return rtn The resource string.
     * 
     * @deprecated Use <code>getString(String key)</code> instead
     * 
     **/
    public String getKey(String key) throws MissingResourceException {
        return getString(key);
    }

    /**
     * Used instead of ResourceBundle.getString. Adds some error
     * checking to ensure that we got a non-null key and resource
     * bundle.
     * 
     * Adds default string functionality. If the key for the resource
     * was not found, return the default string passed in. This way
     * something displays, even if it is in English.
     * 
     * @param key Name portion of "name=value" pair.
     * @param defaultString String to return if the key was null, or if
     *            resource was not found
     *            (MissingResourceException thrown).
     * 
     * @deprecated Use <code>getString(String key, String defaultString)</code> instead
     **/
    public String getKey(String key, String defaultString) {
        return getString(key, defaultString);
    }

    /**
     * 
     * Returns true if resource bundle is loaded successfully
     */
    public boolean isResourceLoaded() {
        return (bundle == null) ? false : true;
    }
}
