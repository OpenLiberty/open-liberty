/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejs.ras;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class TraceNLS
{
  private ResourceBundle _bundle;
  public static TraceNLS getTraceNLS(String bundleName)
  {
    return new TraceNLS(bundleName);
  }

  private TraceNLS(String bundleName)
  {
    _bundle = getResourceBundle(bundleName, Locale.getDefault());
  }

  /**
   * Retrieve the localized text corresponding to the specified key from the ResourceBundle
   * that this instance wrappers. If the text cannot be found for any reason, an appropriate
   * error message is returned instead.
   * <p>
   * @param key the key to use in the ResourceBundle lookup. Null is tolerated.
   * @return the appropriate non-null message. 
   */
  public String getString(String key)
  {
    return worker(_bundle, null, key, null, null, false, null);
  }

  /**
   * Retrieve the localized text corresponding to the specified key from the ResourceBundle
   * represented by this TraceNLS instance. If the text cannot be found for any reason and
   * the defaultString is non-null, then the defaultString is returned. Otherwise an appropriate
   * error message is returned instead.
   * <p>
   * @param key the key to use in the ResourceBundle lookup. Null is tolerated.
   * @param defaultString text to return if text cannot be found. Null is tolerated.
   * @return the appropriate non-null message
   */
  public String getString(String key, String defaultString)
  {
    return worker(_bundle, null, key, null, defaultString, false, null);
  }

  /**
   * Return the message obtained by looking up the localized text indicated by the key
   * in the ResourceBundle wrapped by this instance, then formatting the message using
   * the specified arguments as substitution parameters.
   * <p>
   * The message is formatted using the java.text.MessageFormat class. Substitution
   * parameters are handled according to the rules of that class. Most noteably,
   * that class does special formatting for native java Date and Number objects.
   * <p>
   * If an error occurs in obtaining the localized text corresponding to this key, then the
   * defaultString is used as the message text. If all else fails, this class will provide
   * one of the default English messages to indicate what occurred.
   * <p>
   * @param key the key to use in the ResourceBundle lookup. Null is  tolerated
   * @param args substitution parameters that are inserted into the message text. Null is tolerated
   * @param defaultString text to use if the localized text cannot be found. Null is tolerated
   * <p>
   * @return a non-null message that is localized and formatted as appropriate.
   */
  public String getFormattedMessage(String key, Object[] args,
      String defaultString)
  {
    return worker(_bundle, null, key, args, defaultString, true, null);
  }

  /**
   * Return the message obtained by looking up the localized text indicated by the key
   * in the ResourceBundle wrapped by this instance, then formatting the message using
   * the specified arguments as substitution parameters.
   * <p>
   * The message is formatted using the java.text.MessageFormat class. Substitution
   * parameters are handled according to the rules of that class. Most noteably,
   * that class does special formatting for native java Date and Number objects.
   * <p>
   * If an error occurs in obtaining the localized text corresponding to this key, then the
   * defaultString is used as the message text. If all else fails, this class will provide
   * one of the default English messages to indicate what occurred.
   * <p>
   * @param key the key to use in the ResourceBundle lookup. Null is  tolerated
   * @param args substitution parameters that are inserted into the message text. Null is tolerated
   * @param defaultString text to use if the localized text cannot be found. Null is tolerated
   * @param quiet indicates whether or not errors will be logged when encountered
   * <p>
   * @return a non-null message that is localized and formatted as appropriate.
   */
  public String getFormattedMessage(String key, Object[] args,
      String defaultString, boolean quiet)
  {
    return worker(_bundle, null, key, args, defaultString, true, null);
  }

  //////////////////////////////////////////
  //
  // Methods that define the cachelss model
  //
  /////////////////////////////////////////

  /**
   * Retrieve the localized text corresponding to the specified key in the specified
   * ResourceBundle. If an error is encountered, an appropriate error message is returned
   * instead.
   * <p>
   * @param bundleName the fully qualified name of the ResourceBundle. Must not be null.
   * @param key the key to use in the ResourceBundle lookup. Must not be null.
   * @return the value corresponding to the specified key in the specified ResourceBundle, or the
   *  appropriate non-null error message. 
   */
  public static String getStringFromBundle(String bundleName, String key)
  {
    return worker(null, bundleName, key, null, null, false, null);
  }

  /**
   * Retrieve the localized text corresponding to the specified key in the specified
   * ResourceBundle. If the text cannot be found for any reason, the defaultString is
   * returned. If an error is encountered, an appropriate error message is returned
   * instead.
   * <p>
   * @param bundleName the fully qualified name of the ResourceBundle. Must not be null.
   * @param key the key to use in the ResourceBundle lookup. Must not be null.
   * @param defaultString text to return if text cannot be found. Must not be null.
   * @return the value corresponding to the specified key in the specified ResourceBundle, or the
   *  appropriate non-null error message.
   */
  public static String getStringFromBundle(String bundleName, String key,
      String defaultString)
  {
    return worker(null, bundleName, key, null, defaultString, false, null);
  }

  /**
   * Return the message obtained by looking up the localized text corresponding to the
   * specified key in the specified ResourceBundle and formatting the resultant text using
   * the specified substitution arguments.
   * <p>
   * The message is formatted using the java.text.MessageFormat class. Substitution
   * parameters are handled according to the rules of that class. Most noteably,
   * that class does special formatting for native java Date and Number objects.
   * <p>
   * If an error occurs in obtaining the localized text corresponding to this key, then the
   * defaultString is used as the message text. If all else fails, this class will provide
   * one of the default English messages to indicate what occurred.
   * <p>
   * @param bundleName the fully qualified name of the ResourceBundle. Must not be null.
   * @param key the key to use in the ResourceBundle lookup. Must not be null.
   * @param args substitution parameters that are inserted into the message text. Null is tolerated
   * @param defaultString text to use if the localized text cannot be found. Must not be null.
   * <p>
   * @return a non-null message that is localized and formatted as appropriate.
   */
  public static String getFormattedMessage(String bundleName, String key,
      Object[] args, String defaultString)
  {
    return worker(null, bundleName, key, args, defaultString, true, null);
  }

  /**
   * Return the message obtained by looking up the localized text corresponding to the
   * specified key in the specified ResourceBundle and formatting the resultant text using
   * the specified substitution arguments.
   * <p>
   * The message is formatted using the java.text.MessageFormat class. Substitution
   * parameters are handled according to the rules of that class. Most noteably,
   * that class does special formatting for native java Date and Number objects.
   * <p>
   * If an error occurs in obtaining the localized text corresponding to this key, then the
   * defaultString is used as the message text. If all else fails, this class will provide
   * one of the default English messages to indicate what occurred.
   * <p>
   * @param bundleName the fully qualified name of the ResourceBundle. Must not be null.
   * @param key the key to use in the ResourceBundle lookup. Must not be null.
   * @param args substitution parameters that are inserted into the message text. Null is tolerated
   * @param defaultString text to use if the localized text cannot be found. Must not be null.
   * @param quiet indicates whether or not errors will be logged when encountered
   * <p>
   * @return a non-null message that is localized and formatted as appropriate.
   */
  public static String getFormattedMessage(String bundleName, String key,
      Object[] args, String defaultString, boolean quiet)
  {
    return worker(null, bundleName, key, args, defaultString, true, null);
  }

  /**
   * Retrieve the localized text corresponding to the specified key in the specified
   * ResourceBundle using the specified Locale. If an error is encountered, an appropriate error
   * message is returned instead.
   * <p>
   * @param bundleName the fully qualified name of the ResourceBundle. Must not be null.
   * @param key the key to use in the ResourceBundle lookup. Must not be null.
   * @param locale the Locale object to use when looking up the ResourceBundle. Must not be null.
   * @return the value corresponding to the specified key in the specified ResourceBundle, or the
   *  appropriate non-null error message. 
   */
  public static String getStringFromBundle(String bundleName, String key,
      Locale locale)
  {
    return worker(null, bundleName, key, null, null, false, locale);
  }

  /**
   * Retrieve the localized text corresponding to the specified key in the specified
   * ResourceBundle using the specified Locale. If the text cannot be found for any reason, the
   * defaultString is returned. If an error is encountered, an appropriate error message is
   * returned instead.
   * <p>
   * @param bundleName the fully qualified name of the ResourceBundle. Must not be null.
   * @param key the key to use in the ResourceBundle lookup. Must not be null.
   * @param locale the Locale object to use when looking up the ResourceBundle. Must not be null.
   * @param defaultString text to return if text cannot be found. Must not be null.
   * @return the value corresponding to the specified key in the specified ResourceBundle, or the
   *  appropriate non-null error message.
   */
  public static String getStringFromBundle(String bundleName, String key,
      Locale locale, String defaultString)
  {
    return worker(null, bundleName, key, null, defaultString, false, locale);
  }

  /**
   * Retrieve the localized text corresponding to the specified key in the specified
   * ResourceBundle using the specified Locale. If an error is encountered, an appropriate error
   * message is returned instead.
   * <p>
   * @param bundle the ResourceBundle to use for lookups.  Null is tolerated.  If null is passed,
   *  the resource bundle will be looked up from bundleName.  If not null, bundleName must match.
   * @param bundleName the fully qualified name of the ResourceBundle. Must not be null.
   * @param key the key to use in the ResourceBundle lookup. Must not be null.
   * @param locale the Locale object to use when looking up the ResourceBundle. Must not be null.
   * @return the value corresponding to the specified key in the specified ResourceBundle, or the
   *  appropriate non-null error message. 
   */
  public static String getStringFromBundle(ResourceBundle bundle,
      String bundleName, String key, Locale locale)
  {
    return worker(bundle, bundleName, key, null, null, false, locale);
  }

  /**
   * Retrieve the localized text corresponding to the specified key in the specified
   * ResourceBundle using the specified Locale. If the text cannot be found for any reason, the
   * defaultString is returned. If an error is encountered, an appropriate error message is
   * returned instead.
   * <p>
   * @param bundle the ResourceBundle to use for lookups.  Null is tolerated.  If null is passed,
   *  the resource bundle will be looked up from bundleName.  If not null, bundleName must match.
   * @param bundle the ResourceBundle to use for the lookup.  Null is tolerated.  
   * @param bundleName the fully qualified name of the ResourceBundle. Must not be null.
   * @param key the key to use in the ResourceBundle lookup. Must not be null.
   * @param locale the Locale object to use when looking up the ResourceBundle. Must not be null.
   * @param defaultString text to return if text cannot be found. Must not be null.
   * @return the value corresponding to the specified key in the specified ResourceBundle, or the
   *  appropriate non-null error message.
   */
  public static String getStringFromBundle(ResourceBundle bundle,
      String bundleName, String key, Locale locale, String defaultString)
  {
    return worker(bundle, bundleName, key, null, defaultString, false, locale);
  }

  /**
   * Return the message obtained by looking up the localized text corresponding to the
   * specified key in the specified ResourceBundle using the specified Locale and formatting the
   * resultant text using the specified substitution arguments.
   * <p>
   * The message is formatted using the java.text.MessageFormat class. Substitution
   * parameters are handled according to the rules of that class. Most noteably,
   * that class does special formatting for native java Date and Number objects.
   * <p>
   * If an error occurs in obtaining the localized text corresponding to this key, then the
   * defaultString is used as the message text. If all else fails, this class will provide
   * one of the default English messages to indicate what occurred.
   * <p>
   * @param bundleName the fully qualified name of the ResourceBundle. Must not be null.
   * @param key the key to use in the ResourceBundle lookup. Must not be null.
   * @param locale the Locale object to use when looking up the ResourceBundle. Must not be null.
   * @param args substitution parameters that are inserted into the message text. Null is tolerated
   * @param defaultString text to use if the localized text cannot be found. Must not be null.
   * <p>
   * @return a non-null message that is localized and formatted as appropriate.
   */
  public static String getFormattedMessage(String bundleName, String key,
      Locale locale, Object[] args, String defaultString)
  {
    return worker(null, bundleName, key, args, defaultString, true, locale);
  }

  /**
   * Return the message obtained by looking up the localized text corresponding to the
   * specified key in the specified ResourceBundle using the specified Locale and formatting the
   * resultant text using the specified substitution arguments.
   * <p>
   * The message is formatted using the java.text.MessageFormat class. Substitution
   * parameters are handled according to the rules of that class. Most noteably,
   * that class does special formatting for native java Date and Number objects.
   * <p>
   * If an error occurs in obtaining the localized text corresponding to this key, then the
   * defaultString is used as the message text. If all else fails, this class will provide
   * one of the default English messages to indicate what occurred.
   * <p>
   * @param bundleName the fully qualified name of the ResourceBundle. Must not be null.
   * @param key the key to use in the ResourceBundle lookup. Must not be null.
   * @param locale the Locale object to use when looking up the ResourceBundle. Must not be null.
   * @param args substitution parameters that are inserted into the message text. Null is tolerated
   * @param defaultString text to use if the localized text cannot be found. Must not be null.
   * @param quiet indicates whether or not errors will be logged when encountered
   * <p>
   * @return a non-null message that is localized and formatted as appropriate.
   */
  public static String getFormattedMessage(String bundleName, String key,
      Locale locale, Object[] args, String defaultString, boolean quiet)
  {
    return worker(null, bundleName, key, args, defaultString, true, locale);
  }

  /**
   * Return the formatted message obtained by substituting parameters passed into a message
   * @param localizedMessage the message into which parameters will be substituted
   * @param args the arguments that will be substituted into the message
   * @param quiet indicates whether or not errors will be logged when encountered
   * @return String a message with parameters substituted in as appropriate
   */
  public static String getFormattedMessageFromLocalizedMessage(
      String localizedMessage, Object[] args)
  {
    return workerFormatLocalizedMessage(localizedMessage, args);
  }

  /**
   * A private worker method that the public methods delegate to.
   * <p>
   * Get a reference to the specified ResourceBundle and look up the specified key. If the
   * key has no value, use the defaultString instead. If so indicated, format the text using
   * the specified arguments. Such formatting is done using the java.text.MessageFormat class.
   * Substitution parameters are handled according to the rules of that class. Most noteably,
   * that class does special formatting for native java Date and Number objects.
   * <p>
   * If an error occurs in obtaining a reference to the ResourceBundle, or in looking up the
   * key, this method will take appropriate actions. This may include returning a non-null error
   * message.
   * <p>
   * @param bundle the ResourceBundle to use for lookups.  Null is tolerated.  If null is passed,
   *  the resource bundle will be looked up from bundleName.  If not null, bundleName must match.
   * @param bundleName the fully qualified name of the ResourceBundle. Must not be null.
   * @param key the key to use in the ResourceBundle lookup. Must not be null.
   * @param args substitution parameters that are inserted into the message text. Null is tolerated
   * @param defaultString text to use if the localized text cannot be found. Must not be null.
   * @param locale the Locale object to use when looking up the ResourceBundle. If null is passed,
   *  the default Locale will be used.
   * <p>
   * @return a non-null message that is localized and formatted as appropriate.
   */
  private static String worker(ResourceBundle bundle, String bundleName,
      String key, Object[] args, String defaultString, boolean format,
      Locale locale)
  {
    String returnValue = null;
    
    if (locale == null)
      locale = Locale.getDefault();

    try
    {
      // Retrieve a reference to the ResourceBundle and do the lookup on the key.
      if (bundle == null)
        bundle = getResourceBundle(bundleName, locale);

      returnValue = bundle.getString(key);
      
      // The lookup may have returned empty string if key was found, but there is no value.
      if (returnValue.equals(""))
      {
        // TODO do something here to fail the test.
        // Determine which value to continue with. Default text takes priority over key.
        if (defaultString == null)
          returnValue = key;
        else
          returnValue = defaultString;
      }

      // We have a non-null returnValue, either from the lookup or we are using the default text.
      if (format == false)
        return returnValue;
      else
        //return MessageFormat.format(returnValue, args);
        return workerFormatLocalizedMessage(returnValue, args);
    }
    catch (RuntimeException re)
    {
      // TODO do something here to fail the unittest.
      return "";
    }
  }

  private static String workerFormatLocalizedMessage(String message,
      Object[] args)
  {
    // if there are no arguments just return the message
    if (args == null)
      return message;

    // format the message
    String formattedMessage = null;
    try
    {
      formattedMessage = MessageFormat.format(message, args);
    }
    catch (IllegalArgumentException e)
    {
      // tolerate this - just return the original message
      return message;
    }

    return formattedMessage;
  }

  /**
   * Looks up the specified ResourceBundle
   * 
   * This method first uses the current classLoader to find the ResourceBundle.  If
   * that fails, it uses the context classLoader.
   * 
   * @param bundleName
   * @param locale
   * @return ResourceBundle
   * @throws RuntimeExceptions caused by MissingResourceException or NullPointerException
   *  where resource bundle or classloader cannot be loaded
   */
  public static ResourceBundle getResourceBundle(String bundleName,
      Locale locale)
  {
    ResourceBundle resourceBundle = null;
    if (locale == null)
      locale = Locale.getDefault();

    try
    {
      // Try using our classloader, this will only work if the bundle
      // is a properties file.
      resourceBundle = ResourceBundle.getBundle(bundleName, locale);
    }
    catch (MissingResourceException mre)
    {
      try
      {
        // If we have not found the resource bundle yet
        // Try getting it using the Thread context classloader. This will allow
        // us to load resource bundles in applications. We probably will always
        // need to try this.
        resourceBundle = ResourceBundle.getBundle(bundleName, locale, Thread
            .currentThread().getContextClassLoader());
      }
      catch (MissingResourceException mre2)
      {
        try
        {
          Properties prop = new Properties();
          ByteArrayOutputStream byteout = new ByteArrayOutputStream();
          prop.store(byteout, "");
          ByteArrayInputStream bytein = new ByteArrayInputStream(byteout.toByteArray());
          resourceBundle = new PropertyResourceBundle(bytein);
        }
        catch (IOException ioe)
        {
          throw new RuntimeException(ioe);
        }
      }
    }
    
    return resourceBundle;
  }
}