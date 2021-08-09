/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// %Z% %I% %W% %G% %U% [%H% %T%]
package com.ibm.ejs.ras.hpel;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author rohits This class find various resource bundles, so it can load
 *         message keys The class first searches Thread context class loader
 *         then it searches System Class Loader then it walks up the stack to
 *         first class that called logging, uses its classloader to get bundle
 */
public class Messages {

	/**
	 * The bundle name to load
	 */
    private final String ivBundleName;
	/**
	 * The default locale
	 */
	private Locale ivLocale = Locale.getDefault();
	/**
	 * A instance of class to walk up the stack
	 */
	private StackFinder finder = null;

	private final static String CLASS_NAME = "com.ibm.ejs.ras.hpel.Messages";

	/**
	 * A logger to log messages
	 */
	// private final static Logger logger = Logger.getLogger(CLASS_NAME);

	// private final static boolean quiet =
	// Boolean.getBoolean("com.ibm.ejs.ras.hpel.Messages");

	/**
	 * Construct a Messages instance for the specified ResourceBundle.
	 * <p>
	 * 
	 * @param bundleName
	 *            the package-qualified name of the ResourceBundle. Must NOT be
	 *            null.
	 */
	private Messages(String bundleName) {
		ivBundleName = bundleName;
	}

	/**
	 * Retrieve a string from the bundle
	 * 
	 * @param resourceBundleName
	 *            the package-qualified name of the ResourceBundle. Must NOT be
	 *            null
	 * @param msgKey
	 *            the key to lookup in the bundle, if null rawMessage will be
	 *            returned
	 * @param tmpLocale
	 *            Locale to use for the bundle, can be null
	 * @param rawMessage
	 *            The default message to use if the message key is not found
	 * @return The value of msg key lookup, or the value of raw message
	 */
	public static String getStringFromBundle(String resourceBundleName, String msgKey, Locale tmpLocale, String rawMessage) {
		return getStringFromBundle(null, resourceBundleName, msgKey, tmpLocale, rawMessage);
	}

	/**
	 * Retrieve a string from the bundle and format it using the parameters
	 * 
	 * @param traceString
	 *            localized string to be formatted
	 * @param newParms
	 *            parameters to populate positional parameters in localized
	 *            message
	 * @param b
	 *            whether to return the initial string if formatting fails
	 * @return formatted and localized message
	 */
	public static String getFormattedMessageFromLocalizedMessage(String traceString, Object[] newParms, boolean b) {
		String retVal = "";
		try {
			retVal = MessageFormat.format(traceString, newParms);
			if (null == retVal && b) {
				retVal = traceString;
				// System.out.println("Using default key");
			}
		} catch (IllegalArgumentException e) {
			// ignore formatting errors. otherwise server may not start
			retVal = traceString;
			// if (quiet) {
			// logger.log(Level.SEVERE, "Exception formatting key", e);
			// }
		}

		return retVal;

	}

	/**
	 * Retrieve a string from the bundle
	 * 
	 * @param resourceBundle
	 *            A instance of the resource bundle to use. Both resourceBundle
	 *            and resourceBundleName cannot be null
	 * @param resourceBundleName
	 *            the package-qualified name of the ResourceBundle. Both
	 *            resourceBundle and resourceBundleName cannot be null
	 * @param messageKey
	 *            The key to lookup in the bundles
	 * @param locale
	 *            the locale to use for lookup of bundle
	 * @param defaultMessage
	 *            the default message
	 * @return the value of message key or the default message
	 */
	public static String getStringFromBundle(ResourceBundle resourceBundle, String resourceBundleName, String messageKey, Locale locale, String defaultMessage) {
		/*
		 * if the resource bundle is null, use the resourceBundleName to locate
		 * the bundle
		 */
		if (null == resourceBundle) {
			Messages m = getTraceNLS(resourceBundleName);
			/*
			 * if locale is null use default
			 */
			if (null != locale) {
				m.ivLocale = locale;
			}
			resourceBundle = m.getResourceBundle();
		}

		String retVal = defaultMessage;
		try {

			String tmpRetVal = resourceBundle == null ? null : resourceBundle.getString(messageKey);

			if (null != tmpRetVal) {
				retVal = tmpRetVal;
				// } else {
				// if (quiet) {
				// logger.log(Level.FINEST,
				// "Cannot find message key {2}, in resource bundle {0} for locale {1}, will return the message key.",
				// new Object[] { resourceBundle, resourceBundle == null ?
				// "null resource bundle" :
				// resourceBundle.getLocale().toString(), messageKey });
				// if (logger.isLoggable(Level.FINEST)) {
				// logger.log(Level.FINEST, "stack trace is ", new Exception());
				// }
				// }
			}
		} catch (MissingResourceException e) {
			// if (quiet) {
			// logger.log(Level.FINEST,
			// "Cannot find message key {2}, in resource bundle {0} for locale {1}, due to exception {3}, will return the message key.",
			// new Object[] { resourceBundle,
			// resourceBundle.getLocale().toString(), messageKey, e });
			// }
		}

		return retVal;
	}

	/**
	 * Retrieve a message and format it with the parameters
	 * 
	 * @param messageKey
	 *            Message key to use
	 * @param param
	 *            Object to use for formatting
	 * @param defaultKey
	 *            The default key to use in case message key is not found
	 * @return message formatted with the parameters
	 */
	public String getFormattedMessage(String messageKey, Object[] param, String defaultKey) {
		String key = getString(messageKey);
		String retVal = MessageFormat.format(key, param);
		return retVal;
	}

	/**
	 * Retrieve a message key from the resource bundle
	 * 
	 * @param messageKey
	 *            Message Key to locate
	 * @return localized message in resource bundle associated with this message
	 *         key
	 */
	public String getString(String messageKey) {

		String retVal = messageKey;
		ResourceBundle rb = getResourceBundle();

		if (null != rb) {
			try {
				String tmpRetVal = rb.getString(messageKey);

				if (null != tmpRetVal) {
					retVal = tmpRetVal;
					// } else {
					// if (quiet) {
					//
					// logger.log(Level.FINEST,
					// "Cannot find message key {2}, in resource bundle {0} for locale {1}, will return the message key.",
					// new Object[] { ivBundleName, ivLocale.toString(),
					// messageKey });
					// if (logger.isLoggable(Level.FINEST)) {
					// logger.log(Level.FINEST, "stack trace is ", new
					// Exception());
					// }
					// }
				}

			} catch (MissingResourceException e) {
				// if (quiet) {
				//
				// logger.log(Level.FINEST,
				// "Cannot find message key {2}, in resource bundle {0} for locale {1}, due to exception {3}, will return the message key.",
				// new Object[] { ivBundleName, ivLocale.toString(), messageKey,
				// e });
				// }
			}
			// } else {
			// if (quiet) {
			//
			// // info so this can be fixed
			// logger.log(Level.FINEST,
			// "Cannot find message key {2}, in resource bundle {0} for locale {1}, will return the message key.",
			// new Object[] { ivBundleName, ivLocale.toString(), messageKey });
			// if (logger.isLoggable(Level.FINEST)) {
			// logger.log(Level.FINEST, "stack trace is ", new Exception());
			// }
			// }
		}

		return retVal;
	}

	/**
	 * Private message to locate resource bundle
	 * 
	 * @return
	 */
	private ResourceBundle getResourceBundle() {
		ResourceBundle rb = null;

		/*
		 * make sure we have a valid bundle name
		 */
		if (null != ivBundleName && !"".equals(ivBundleName.trim())) {
			ClassLoader classLoader = null;

			/*
			 * get the context classloader to lookup bundle
			 */
			try {
                classLoader = AccessController.doPrivileged(new PrivilegedExceptionAction<ClassLoader>() {
					public ClassLoader run() throws Exception {
						return Thread.currentThread().getContextClassLoader();
					}
				});
			} catch (PrivilegedActionException e) {
				// convert to a runtime exception since callers already have
				// to deal
				// with that type
				throw new java.lang.RuntimeException(e.getMessage());
			}

			/*
			 * if there is a context classloader
			 * OPTION 1 - look in context classloader
			 */
			if (null != classLoader) {

				try {
					rb = ResourceBundle.getBundle(ivBundleName, ivLocale, classLoader);

				} catch (MissingResourceException e) {
					// if (quiet) {
					//
					// logger.log(Level.FINEST,
					// "Cannot find resource bundle {0} for locale {1} in Thread context loader, will try system class loader",
					// new Object[] { ivBundleName, ivLocale.toString() });
					// }
				}
			}

			/*
			 * OPTION 2: Climb up the stack and get class that loaded the class
			 *            Use this classloader to get bundle. This helps with OSGI bundle
			 */
			if(null == rb) {
				// if (quiet) {
				//
				// logger.log(Level.FINEST,
				// "Cannot find resource bundle {0} for locale {1} in System Class loader, will try callee classloader",
				// new Object[] { ivBundleName, ivLocale.toString() });
				rb = locateBundleFromCallee();
				// }

			}

			/*
			 * OPTION 3: Look in system classloader
			 */
			if (null == rb) {
				try {
					rb = ResourceBundle.getBundle(ivBundleName, ivLocale);
				} catch (MissingResourceException e2) {
					/*
					 * left empty on purpose, since we cannot log
					 */
				}
			}
			

		}
		return rb;
	}

	/**
	 * Call the stack finder to get the callee of the logging api, then use the
	 * classloader from class to retrieve resource bundle
	 * 
	 * @return The resoruce bundle from callees classpath or null
	 */
	private ResourceBundle locateBundleFromCallee() {
		ResourceBundle rb = null;

		if (finder == null) {
			finder = StackFinder.getInstance();
		}

		final Class<?> aClass = finder.getCaller();

		if (aClass != null) {
			// If aClass is NOT null (it was passed in, or we found it),
			// use its classloader first to try loading the resource
			// bundle
			ClassLoader classLoader = null;
			try {
                classLoader = AccessController.doPrivileged(new PrivilegedExceptionAction<ClassLoader>() {
					public ClassLoader run() throws Exception {
						return aClass.getClassLoader();
					}
				});
			} catch (PrivilegedActionException e) {
				// convert to a runtime exception since callers already have
				// to deal
				// with that type
				throw new java.lang.RuntimeException(e.getMessage());
			}

			try {
				rb = ResourceBundle.getBundle(ivBundleName, ivLocale, classLoader);
			} catch (RuntimeException re) {
				// if (quiet) {
				//
				// logger.log(Level.FINEST,
				// "Unable to load {0} from {1} (from class {2}) in {3}; caught exception: {4}",
				// new Object[] { ivBundleName, classLoader, aClass, ivLocale,
				// re });
				// }
			}
			// } else {
			// if (quiet) {
			//
			// logger.log(Level.FINEST, "Unable to load {0} class was null", new
			// Object[] { ivBundleName });
			// }
		}
		return rb;
	}

	/**
	 * @param string
	 *            bundle name, This name cannot be null, has to fully package
	 *            qualified
	 * @return A instance of this class
	 */
	public static Messages getTraceNLS(String string) {
		return new Messages(string);
	}

}
