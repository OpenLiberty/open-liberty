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

/* ************************************************************************** */
/**
 * A Logging PrintStream diverts all output to a given Logger.
 * (Note that only the 1.4 methods are supported - calling a Java 5 method like
 * printf probably won't work until we fix it)
 *
 */
/* ************************************************************************** */
public class LoggingPrintStream extends java.io.PrintStream
{
  /** The logger to which to divert the print stream */
  private java.util.logging.Logger _logger;

  /** The name to be passed to the logger when passing the output */
  private String _name;

  /** The current line of output passing through this PrintStream */
  private StringBuffer _currentLine = new StringBuffer();
  /** Thread local storing a variable used to prevent infinate recursion */
  private ThreadLocal<Boolean> _threadLocal = new ThreadLocal<Boolean>()
  {
    @Override
    protected Boolean initialValue()
    {
      return true;
    }
  };

  /* -------------------------------------------------------------------------- */
  /* LoggingPrintStream constructor
  /* -------------------------------------------------------------------------- */
  /**
   * Construct a new LoggingPrintStream.
   *
   * @param name    The name of the PrintStream to be passed to the logger
   * @param logger  The logger to which to divert the PrintStream
   */
  public LoggingPrintStream(String name, java.util.logging.Logger logger)
  {
    super(System.out); // Note we have to provide a non-null output stream - but we're not going to use it!
    _logger = logger;
    _name = name;
  }

  /* -------------------------------------------------------------------------- */
  /* println method
  /* -------------------------------------------------------------------------- */
  /**
   * Complete the current line of text with a String and add to the logger
   *
   * @see java.io.PrintStream#println(java.lang.String)
   * @param s The string that completes the line of text
   */
  public void println(String s)
  {
    _currentLine.append(s);
    sendCompletedLineToLogger();
  }

  /* -------------------------------------------------------------------------- */
  /* println method
  /* -------------------------------------------------------------------------- */
  /**
   * Complete the current line of text with a char array and add to the logger
   *
   * @see java.io.PrintStream#println(char[])
   * @param c The char array that completes the line of text
   */
  public void println(char[] c)
  {
    _currentLine.append(c);
    sendCompletedLineToLogger();
  }

  /* -------------------------------------------------------------------------- */
  /* println method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#println()
   *
   */
  public void println()
  {
    sendCompletedLineToLogger();
  }

  /* -------------------------------------------------------------------------- */
  /* sendCompletedLineToLogger method
  /* -------------------------------------------------------------------------- */
  /**
   * Send a completed line to the logger and then reset it to an empty
   */
  private void sendCompletedLineToLogger()
  {
    if (_threadLocal.get())
    {
      try
      {
        _threadLocal.set(false);
        _logger.logp(Level.INFORMATION, _name, "", "{0}",_currentLine.toString());
      }
      finally
      {
        _threadLocal.set(true);
      }
      
    }
    _currentLine = new StringBuffer();
  }

  /* -------------------------------------------------------------------------- */
  /* println method
  /* -------------------------------------------------------------------------- */
  /**
   * Complete the current line of text with a boolean and add to the logger
   *
   * @see java.io.PrintStream#println(boolean)
   * @param b The boolean that completes the line of text
   */
  public void println(boolean b)
  {
    println(Boolean.toString(b));
  }

  /* -------------------------------------------------------------------------- */
  /* println method
  /* -------------------------------------------------------------------------- */
  /**
   * Complete the current line of text with a char and add to the logger
   *
   * @see java.io.PrintStream#println(char)
   * @param c The character that completes the line of text
   */
  public void println(char c)
  {
    println(Character.toString(c));
  }

  /* -------------------------------------------------------------------------- */
  /* println method
  /* -------------------------------------------------------------------------- */
  /**
   * Complete the current line of text with a double and add to the logger
   *
   * @see java.io.PrintStream#println(double)
   * @param d The double that completes the line of text
   */
  public void println(double d)
  {
    println(Double.toString(d));
  }

  /* -------------------------------------------------------------------------- */
  /* println method
  /* -------------------------------------------------------------------------- */
  /**
   * Complete the current line of text with a float and add to the logger
   *
   * @see java.io.PrintStream#println(float)
   * @param f The float that completes the line of text
   */
  public void println(float f)
  {
    println(Float.toString(f));
  }

  /* -------------------------------------------------------------------------- */
  /* println method
  /* -------------------------------------------------------------------------- */
  /**
   * Complete the current line of text with an int and add to the logger
   *
   * @see java.io.PrintStream#println(int)
   * @param i The int that completes the line of text
   */
  public void println(int i)
  {
    println(Integer.toString(i));
  }

  /* -------------------------------------------------------------------------- */
  /* println method
  /* -------------------------------------------------------------------------- */
  /**
   * Complete the current line of text with a long and add to the logger
   *
   * @see java.io.PrintStream#println(long)
   * @param i The int that completes the line of text
   */
  public void println(long l)
  {
    println(Long.toString(l));
  }

  /* -------------------------------------------------------------------------- */
  /* println method
  /* -------------------------------------------------------------------------- */
  /**
   * Complete the current line of text with an object and add to the logger
   *
   * @see java.io.PrintStream#println(java.lang.Object)
   * @param i The int that completes the line of text
   */
  public void println(Object o)
  {
    println(o != null ? o.toString() : "null");
  }

  /* -------------------------------------------------------------------------- */
  /* print method
  /* -------------------------------------------------------------------------- */
  /**
   * Add a string to the current line of text
   *
   * @see java.io.PrintStream#print(java.lang.String)
   * @param s
   */
  public void print(String s)
  {
    _currentLine.append(s);
  }

  /* -------------------------------------------------------------------------- */
  /* print method
  /* -------------------------------------------------------------------------- */
  /**
   * Add a boolean to the current line of text
   *
   * @see java.io.PrintStream#print(boolean)
   * @param b
   */
  public void print(boolean b)
  {
    print(Boolean.toString(b));
  }

  /* -------------------------------------------------------------------------- */
  /* print method
  /* -------------------------------------------------------------------------- */
  /**
   * Add a character to the current line of text
   *
   * @see java.io.PrintStream#print(char)
   * @param c
   */
  public void print(char c)
  {
    print(Character.toString(c));
  }

  /* -------------------------------------------------------------------------- */
  /* print method
  /* -------------------------------------------------------------------------- */
  /**
   * Add an integer to the current line of text
   *
   * @see java.io.PrintStream#print(int)
   * @param i
   */
  public void print(int i)
  {
    print(Integer.toString(i));
  }

  /* -------------------------------------------------------------------------- */
  /* print method
  /* -------------------------------------------------------------------------- */
  /**
   * Add a long to the current line of text
   *
   * @see java.io.PrintStream#print(long)
   * @param l
   */
  public void print(long l)
  {
    print(Long.toString(l));
  }

  /* -------------------------------------------------------------------------- */
  /* print method
  /* -------------------------------------------------------------------------- */
  /**
   * Add a float to the current line of text
   *
   * @see java.io.PrintStream#print(float)
   * @param f
   */
  public void print(float f)
  {
    print(Float.toString(f));
  }

  /* -------------------------------------------------------------------------- */
  /* print method
  /* -------------------------------------------------------------------------- */
  /**
   * Add a double to the current line of text
   *
   * @see java.io.PrintStream#print(double)
   * @param d
   */
  public void print(double d)
  {
    print(Double.toString(d));
  }

  /* -------------------------------------------------------------------------- */
  /* print method
  /* -------------------------------------------------------------------------- */
  /**
   * Add an array of characters to the current line of text
   *
   * @see java.io.PrintStream#print(char[])
   * @param s
   */
  public void print(char s[])
  {
    _currentLine.append(s);// Better to just append the array rather than toString and then convert back to a char[] inside StringBuffer!
  }

  /* -------------------------------------------------------------------------- */
  /* print method
  /* -------------------------------------------------------------------------- */
  /**
   * Add an object to the current line of text
   *
   * @see java.io.PrintStream#print(java.lang.Object)
   * @param o
   */
  public void print(Object o)
  {
    print(o != null ? o.toString() : "null");
  }
}
