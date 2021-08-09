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
package com.ibm.ws.sib.unittest.ras;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * <p>This formatter converts the trace into an WAS advanced trace format. This
 *   means that protrace can be used to find out what went wrong.
 * </p>
 *
 * <p>SIB build component: mm.logger</p>
 *
 * @author nottinga
 * @version 1.1
 * @since 1.0
 */
public class AdvancedFormatter extends Formatter
{
  /** A cache of the line separator. */
  public static final String lineSeparator = System.lineSeparator();
  
  /** Today - assumes trace will NOT across a day boundary - only used to determine DST anyway! */
  private static final Date TODAY = new Date();
  
  /**
   * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
   */
  public String format(LogRecord record)
  {
    StringBuffer buffer = new StringBuffer();
    
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(record.getMillis());
    
    buffer.append('[');
    int value = cal.get(Calendar.DATE);
    if (value < 10) buffer.append('0');
    buffer.append(value);
    buffer.append('/');
    value = cal.get(Calendar.MONTH) + 1;
    if (value < 10) buffer.append('0');
    buffer.append(value);
    buffer.append('/');
    buffer.append(cal.get(Calendar.YEAR));
    buffer.append(' ');
    value = cal.get(Calendar.HOUR_OF_DAY);
    if (value < 10) buffer.append('0');
    buffer.append(value);
    buffer.append(':');
    value = cal.get(Calendar.MINUTE);
    if (value < 10) buffer.append('0');
    buffer.append(value);
    buffer.append(':');
    value = cal.get(Calendar.SECOND);
    if (value < 10) buffer.append('0');
    buffer.append(value);
    buffer.append(':');
    value = cal.get(Calendar.MILLISECOND);
    if (value < 100) buffer.append('0');
    if (value < 10) buffer.append('0');
    buffer.append(value);
    buffer.append(' ');
    TimeZone tz = cal.getTimeZone();
    buffer.append(tz.getDisplayName(tz.inDaylightTime(TODAY),TimeZone.SHORT));
    buffer.append(']');
    buffer.append(' ');
    buffer.append(paddThreadID(record.getThreadID()));
    buffer.append(' ');
    buffer.append(' ');
   // buffer.append(" UOW=null source=");
    char entryType = getEntryTypeChar(record.getLevel());

    String methodName = record.getSourceMethodName();
   
    buffer.append(record.getSourceClassName());
    buffer.append(" ");

    buffer.append(entryType); // Sort this out so it gives a real indication of what happened.

    if (methodName != null && !"".equals(methodName))
    {
      buffer.append("  ");

      //buffer.append(" method=");
      buffer.append(methodName);
    }
    //buffer.append(" org=IBM prod=WebSphere Platform Messaging component=Mediation Monkey thread=[main]"); // only have one thread in mediation monkey.
    if (entryType != '1')
    {
      buffer.append(lineSeparator);
    }
    else 
      buffer.append(" ");
    
    Object[] params = record.getParameters();
    
    if (params!= null && params.length == 1 && params[0] instanceof Object[])
      params = (Object[])params[0];
    
    String[] parms;
    if (params != null)
    {
      parms = new String[params.length];
      for (int i = 0; i < params.length; i++)
      {
        if (params[i] != null)
        {
          parms[i] = "parm" + i + '=' + params[i].toString();
        }
        else
        {
          parms[i] = "parm" + i + "=<null>";
        }
      }
    }
    else
    {
      parms = new String[0];
    }

    String message = record.getMessage();
    
    buffer.append(MessageFormat.format(message, (Object[])parms));
   
    buffer.append(lineSeparator);
    
    return buffer.toString();
  }
  
  /* ------------------------------------------------------------------------ */
  /* getEntryTypeChar method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method works out the char to be written into the log for the given
   * level.
   * 
   * @param theLevel the level to get the character for.
   * @return The character.
   */
  private char getEntryTypeChar(java.util.logging.Level theLevel)
  {
    char c = '?';
    
    if (theLevel == Level.ENTRY)
    {
      c = '>';
    } 
    else if (theLevel == Level.EXIT || theLevel == Level.THROWING)
    {
      c = '<';
    }
    else if (theLevel == Level.CAUGHT)
    {
      c = 'C';
    }
    else if (theLevel == Level.INFO)
    {
      c = 'I';
    }
    else if (theLevel == Level.EVENT)
    {
      c = '1';
    }
    
    return c;
  }
  
  /* ------------------------------------------------------------------------ */
  /* paddThreadID method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method ensures that the thread id is 8 characters long.
   * 
   * @param id The id of the thread. 
   * @return   The string version of 8 characters in length.
   */
  private StringBuffer paddThreadID(int id)
  {
    String sID = Integer.toString(id);
    StringBuffer buffer = new StringBuffer();
    
    if (sID.length() > 8)
    {
      buffer.append(sID.substring(0, 7));
    }
    else if (sID.length() < 8)
    {
      for (int i = 0; i < 8 - sID.length(); i++)
      {
        buffer.append('0');
      }
      buffer.append(sID);
    }
    else
    {
      buffer.append(sID);
    }
    
    return buffer;
  }
}
