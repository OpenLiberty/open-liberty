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
package com.ibm.websphere.ws.sib.unittest.ras;

import java.util.Arrays;

/**
 * <p>This class represents a record written to the trace. It is used for
 *   performing assertions based on log output.
 * </p>
 *
 * <p>SIB build component: sib.unittest.ras</p>
 *
 * <p>Note this is not suitable for use as the key into a HashMap or HashSet.</p>
 *
 * @author nottinga
 * @version 1.4
 * @since 1.0
 */
public final class LogRecord
{
  /** The severity of the log record */
  private Severity _severity;
  /** The message key output */
  private String _msgKey;
  /** The inserts for the message */
  private Object _objs;
  /** An exception created at construction that is used to guide the developer to the logging code */
  private final Exception _stackTrace;

  /** A constant empty array used when objs is passed in as null */
  private static Object[] EMPTY_ARRAY = new Object[0];

  /* ------------------------------------------------------------------------ */
  /* LogRecord method
  /* ------------------------------------------------------------------------ */
  /**
   * Constructor for a log record.
   *
   * @param severity The severity of the log record
   * @param msgKey   The message key.
   * @param objs     the objects.
   */
  public LogRecord(Severity severity, String msgKey, Object objs)
  {
    _severity = severity;
    _msgKey = msgKey;

    if (objs != null)
    {
      _objs = objs;
    }
    else
    {
      _objs = EMPTY_ARRAY;
    }

    _stackTrace = new Exception("The log entry occurred with this exception stack trace.");
  }

  /* ------------------------------------------------------------------------ */
  /* equals method
  /* ------------------------------------------------------------------------ */
  /**
   * This method makes the assumption that it is not passed a multi dimensioned
   * array.
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj)
  {
    if (obj == this) return true;
    if (obj == null) return false;
    if (!(obj instanceof LogRecord)) return false;

    LogRecord other = (LogRecord)obj;

    if (other._severity != _severity) return false;
    if (!other._msgKey.equals(_msgKey)) return false;


    // If they are both object (not array) do a simple comparison.
    if (!other._objs.getClass().isArray() && !_objs.getClass().isArray())
    {
      return _objs.equals(other._objs);
    }

    // if either is not an array, the objects are not equal.
    if (!other._objs.getClass().isArray() || !_objs.getClass().isArray()) return false;

    // We know they are both arrays, do a deep array
    if (other._objs.getClass().getComponentType().isPrimitive() &&
        _objs.getClass().getComponentType().isPrimitive())
    {
      if (_objs instanceof boolean[] && other._objs instanceof boolean[])
      {
        return Arrays.equals((boolean[])_objs, (boolean[])other._objs);
      }
      else if (_objs instanceof byte[] && other._objs instanceof byte[])
      {
        return Arrays.equals((byte[])_objs, (byte[])other._objs);
      }
      else if (_objs instanceof char[] && other._objs instanceof char[])
      {
        return Arrays.equals((char[])_objs, (char[])other._objs);
      }
      else if (_objs instanceof double[] && other._objs instanceof double[])
      {
        return Arrays.equals((double[])_objs, (double[])other._objs);
      }
      else if (_objs instanceof float[] && other._objs instanceof float[])
      {
        return Arrays.equals((float[])_objs, (float[])other._objs);
      }
      else if (_objs instanceof int[] && other._objs instanceof int[])
      {
        return Arrays.equals((int[])_objs, (int[])other._objs);
      }
      else if (_objs instanceof long[] && other._objs instanceof long[])
      {
        return Arrays.equals((long[])_objs, (long[])other._objs);
      }
      else if (_objs instanceof short[] && other._objs instanceof short[])
      {
        return Arrays.equals((short[])_objs, (short[])other._objs);
      }
      else
      {
        return false;
      }
    }

    // if either one is primitive then they are not both primitive, so return false.
    if (other._objs.getClass().getComponentType().isPrimitive() ||
        _objs.getClass().getComponentType().isPrimitive())
    {
      return false;
    }

    // Aha we have two array, which are of class types.

    // At this point _objs and other._objs may not be Object[]s so we have
    // to copy them into object arrays and do a comparison.

    Object[] objsArray = (Object[])_objs;
    Object[] otherObjsArray = (Object[])other._objs;

    if (objsArray.length != otherObjsArray.length) return false;

    return Arrays.equals((Object[])_objs, (Object[])other._objs);
  }

  /* ------------------------------------------------------------------------ */
  /* hashCode method                                                          */
  /* ------------------------------------------------------------------------ */
  /**
   * Return a hashCode for this LogRecord
   *
   * @return int The hashCode
   */
  /* ------------------------------------------------------------------------ */
  public int hashCode()
  {
    int answer = 0;

    if (_severity != null) answer = _severity.hashCode();

    if (_msgKey   != null) answer = answer * 1000003 + _msgKey.hashCode();

    return answer;
  }


  /* ------------------------------------------------------------------------ */
  /* getMsgKey method
  /* ------------------------------------------------------------------------ */
  /**
   * This method returns the current value of _msgKey
   *
   * @return String
   */
  public String getMsgKey()
  {
    return _msgKey;
  }

  /* ------------------------------------------------------------------------ */
  /* getObjs method
  /* ------------------------------------------------------------------------ */
  /**
   * This method returns the current value of _objs
   *
   * @return Object
   */
  public Object getObjs()
  {
    return _objs;
  }

  /* ------------------------------------------------------------------------ */
  /* getSeverity method
  /* ------------------------------------------------------------------------ */
  /**
   * This method returns the current value of _severity
   *
   * @return Severity
   */
  public Severity getSeverity()
  {
    return _severity;
  }

  /* ------------------------------------------------------------------------ */
  /* getStackTrace method
  /* ------------------------------------------------------------------------ */
  /**
   * This method returns the current value of _stackTrace
   *
   * @return Exception
   */
  public Exception getStackTrace()
  {
    return _stackTrace;
  }

  /* ------------------------------------------------------------------------ */
  /* toString method
  /* ------------------------------------------------------------------------ */
  /**
   * @see Object#toString()
   */
  public String toString()
  {
    StringBuffer buffer = new StringBuffer();

    buffer.append("[severity=");
    buffer.append(_severity);
    buffer.append(", message key=");
    buffer.append(_msgKey);
    buffer.append("]");

    return buffer.toString();
  }
}
