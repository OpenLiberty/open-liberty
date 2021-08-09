/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.shared.util;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;

/**
 * Utility class for Tag classes
 *
 * @version $Name:  $ ($Revision: adfrt/faces/adf-faces-impl/src/main/java/oracle/adfinternal/view/faces/taglib/util/TagUtils.java#1 $) $Date: 11-nov-2005.14:59:38 $
 *
 */
public final class TagUtils
{
  //private static final Log LOG = LogFactory.getLog(TagUtils.class);
  private static final Logger LOG = Logger.getLogger(TagUtils.class.getName());

  private TagUtils()
  {
  }

  public static ValueExpression getValueExpression(String valueExpression, Class<?> expectedType)
  {
    FacesContext context = FacesContext.getCurrentInstance();
    ELContext elContext = context.getELContext();
    ExpressionFactory fact = context.getApplication().getExpressionFactory();
    
    return fact.createValueExpression(elContext, valueExpression, expectedType);
  }

  public static void assertNotNull(Object object)
  {
    if (null == object)
    {
      throw new NullPointerException();
    }
  }

  // Helpful with tag auto generation. Though this isn't really required.
  /**
   * Return the same string. It is there for convenience and makes life easy
   * while auto generating tags.
   * @param value
   * @return
   */
  public static String getString(
    Object value)
  {
    if (value == null)
    {
        return null;
    }

    return value.toString();
  }

  /**
   * String --> boolean
   * @param value
   * @return
   */
  public static boolean getBoolean(
    Object  value)
  {
    if (value == null)
    {
        return false;
    }
    
    if (value instanceof Boolean)
    {
        return ((Boolean) value).booleanValue();
    }

    return Boolean.valueOf(value.toString()).booleanValue();
  }

  /**
   * String --> int
   * @param value
   * @return
   */
  public static int getInteger(
    Object  value)
  {
    if (value == null)
    {
        return 0;
    }

    if (value instanceof Number)
    {
        return ((Number) value).intValue();
    }

    return Integer.valueOf(value.toString()).intValue();

  }

  /**
   * String --> long
   * @param value
   * @return
   */
  public static long getLong(
    Object      value)
  {
    if (value == null)
    {
        return 0;
    }

    return Long.valueOf(value.toString()).longValue();
  }

  /**
   * String --> long
   * @param value
   * @return
   */
  public static double getDouble(
    Object      value)
  {
    if (value == null)
    {
        return 0;
    }

    return Double.valueOf(value.toString()).doubleValue();

  }

  /**
   * String --> long
   * @param value
   * @return
   */
  public static float getFloat(
    Object      value)
  {
    if (value == null)
    {
        return 0;
    }

    return Float.valueOf(value.toString()).floatValue();
  }

  /**
   * These are normally NMTOKEN type in attributes
   * String --> String[]
   * @param value
   * @return
   */
  /**
   * These are normally NMTOKEN type in attributes
   * String --> String[]
   * @param value
   * @return
   */
  public static String[] getStringArray(
    Object  value) throws ParseException
  {
    if (value == null)
    {
        return null;
    }

    return getTokensArray(value.toString());
  }

  /**
   *  ISO Date String --> Date
   * @param value
   * @return
   */
  public static Date getDate(
    Object   value)
  {
    if (value == null)
    {
        return null;
    }

    if (value instanceof Date)
    {
        return ((Date) value);
    }

    return parseISODate(value.toString());
  }

  /**
   * String --> Locale
   * @param value
   * @return
   */
  public static Locale getLocale(
    Object      value)
  {
    if (value == null)
    {
        return null;
    }

    if (value instanceof Locale)
    {
        return ((Locale) value);
    }

    return getLocaleInternal(value.toString());
  }

  /**
   * String --> TimeZone
   * @param value
   * @return

  public static TimeZone getTimeZone(
    String value)
  {
    return DateUtils.getSupportedTimeZone(value);
  }
   */

  public static boolean isValueReference(String expression)
  {
    if (null != expression)
    {
      int start = expression.indexOf("#{");
      if ((start >= 0) && (expression.indexOf('}', start + 1) >= 0))
      {
          return true;
      }
    }

    return false;
  }



  /**
   * Takes a string that is a composite of tokens, extracts tokens delimited
   *  by any whitespace character sequence combination and returns a String
   *  array of such tokens.
   * @throws ParseException In case of invalid character in the specified
   *           composite. The only invalid character is a comma (',').
   */
  private static String[] getTokensArray(String tokenComposite)
    throws ParseException
  {
    if (tokenComposite == null || "".equals(tokenComposite))
    {
        return null;
    }

    return parseNameTokens(tokenComposite);
  }

  /**
   * Parse a string into a java.util.Date object.  The
   * string must be in ISO 9601 format (yyyy-MM-dd).
   * @todo why not throw the exception in a different format?
   *       why do we kill it here and return null?
   */
  static private final Date parseISODate(String stringValue)
  {
    try
    {
      return getDateFormat().parse(stringValue);
    }
    catch (ParseException pe)
    {
      if (LOG.isLoggable(Level.INFO))
      {
        LOG.log(Level.INFO, "CANNOT_PARSE_VALUE_INTO_DATE_WITH_YYYY_MM_DD_PATTERN "+ stringValue, pe);
      }
      return null;
    }
  }
  
  /**
   * Parses a whitespace separated series of name tokens.
   * @param stringValue the full string
   * @return an array of each constituent value, or null
   *  if there are no tokens (that is, the string is empty or
   *  all whitespace)
   */
  static public String[] parseNameTokens(String stringValue)
  {
    if (stringValue == null)
    {
        return null;
    }

    ArrayList<String> list = new ArrayList<String>(5);

    int     length = stringValue.length();
    boolean inSpace = true;
    int     start = 0;
    for (int i = 0; i < length; i++)
    {
      char ch = stringValue.charAt(i);

      // We're in whitespace;  if we've just departed
      // a run of non-whitespace, append a string.
      // Now, why do we use the supposedly deprecated "Character.isSpace()"
      // function instead of "isWhitespace"?  We're following XML rules
      // here for the meaning of whitespace, which specifically
      // EXCLUDES general Unicode spaces.
      if (Character.isWhitespace(ch))
      {
        if (!inSpace)
        {
          list.add(stringValue.substring(start, i));
          inSpace = true;
        }
      }
      // We're out of whitespace;  if we've just departed
      // a run of whitespace, start keeping track of this string
      else
      {
        if (inSpace)
        {
          start = i;
          inSpace = false;
        }
      }
    }

    if (!inSpace)
    {
        list.add(stringValue.substring(start));
    }

    if (list.isEmpty())
    {
        return null;
    }

    return list.toArray(new String[list.size()]);
  }
  

  private static Locale getLocaleInternal(String locale)
  {
    String localeStr = locale.replace('-','_');
    String[] tokens = localeStr.split("[_]", 3);
    Locale locl = null;

    if ( tokens.length == 1)
    {
      locl = new Locale(tokens[0]); //lang
    }
    else if (tokens.length == 2)
    {
      locl = new Locale(tokens[0], tokens[1]); // lang + country
    }
    else if (tokens.length == 3 )
    {
      locl = new Locale(tokens[0], tokens[1], tokens[2]); // lang + country + variant
    }
    else
    {
      if(LOG.isLoggable(Level.WARNING))
      {
          LOG.log(Level.WARNING, "tokens length should not be greater than 3.");
      }
    }
    return locl;
  }

  // We rely strictly on ISO 8601 formats
  private static DateFormat getDateFormat()
  {
    return new SimpleDateFormat("yyyy-MM-dd");
  }
}
