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
package org.apache.myfaces.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Private subclass of LogRecord to hide MyfacesLogger in the stack trace.
 */
class MyfacesLogRecord extends LogRecord 
{
  public MyfacesLogRecord(Level level, String msg)
  {
    super(level, (msg == null) ? "" : msg);
    _needToInferCaller = true;
  }


  @Override
  public String getSourceClassName()
  {
    if (_needToInferCaller)
    {
        _inferCaller();
    }

    return super.getSourceClassName();
  }

  @Override
  public void setSourceClassName(String sourceClassName)
  {
    _needToInferCaller = false;
    super.setSourceClassName(sourceClassName);
  }

  @Override
  public String getSourceMethodName()
  {
    if (_needToInferCaller)
    {
        _inferCaller();
    }

    return super.getSourceMethodName();
  }

  @Override
  public void setSourceMethodName(String sourceMethodName)
  {
    _needToInferCaller = false;
    super.setSourceMethodName(sourceMethodName);
  }

  // Private method to infer the caller's class and method names
  private void _inferCaller()
  {
    _needToInferCaller = false;
    // Get the stack trace.
    StackTraceElement stack[] = (new Throwable()).getStackTrace();
    // First, search back to a method in the Logger class.
    int i = 0;

    while (i < stack.length)
    {
      StackTraceElement frame = stack[i];
      String cname = frame.getClassName();
      if (cname.equals(_JDK_LOG_CLASS) ||
          cname.equals(_MYFACES_LOG_CLASS))
      {
          break;
      }

      i++;
    }

    // Now search for the first frame before the "Logger" class.
    while (i < stack.length)
    {
      StackTraceElement frame = stack[i];
      String cname = frame.getClassName();

      if (cname.equals(_JDK_LOG_CLASS) ||
          cname.equals(_MYFACES_LOG_CLASS))
      {
        i++;
        continue;
      }

      String mname = frame.getMethodName();
      if ("log".equals(mname) ||
          "_log".equals(mname))
      {
        i++;
        continue;
      }

      // We've found the relevant frame.
      setSourceClassName(cname);
      setSourceMethodName(mname);
      return;

    }

    // Forcibly set both to null, so the LogRecord superclass
    // doesn't try to detect them on its own
    setSourceClassName(null);
    setSourceMethodName(null);
  }

  transient private boolean _needToInferCaller;


  private static final String _JDK_LOG_CLASS = 
    "java.util.logging.Logger";
  
  private static final String _MYFACES_LOG_CLASS =
    "org.apache.myfaces.logging.MyfacesLogger";

  private static final long serialVersionUID = 1L;
}

