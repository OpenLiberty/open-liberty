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

import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;

/* ************************************************************************** */
/**
 * A SplittingPrintStream is a PrintStream that sends its output to two
 * other PrintStreams
 *
 */
/* ************************************************************************** */
public class SplittingPrintStream extends java.io.PrintStream
{
  /** The first PrintStream (aka the console) */
  private java.io.PrintStream _console;

  /** The second PrintStream (aka the contents of a window pane) */
  private java.io.PrintStream _pane;

  /* -------------------------------------------------------------------------- */
  /* SplittingPrintStream constructor
  /* -------------------------------------------------------------------------- */
  /**
   * Construct a new SplittingPrintStream.
   *
   * @param ps1 The first PrintStream
   * @param ps2 the second PrintStream
   */
  public SplittingPrintStream(java.io.PrintStream ps1, java.io.PrintStream ps2)
  {
    super(System.out); // Note we have to provide a non-null output stream - but we're not going to use it!
    _console = ps1;
    _pane = ps2;
  }

  /* -------------------------------------------------------------------------- */
  /* println method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#println(java.lang.String)
   * @param s The string to be printed to both streams
   */
  public void println(String s)
  {
    _console.println(s);
    _pane.println(s);
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
    _console.println();
    _pane.println();
  }

  /* -------------------------------------------------------------------------- */
  /* println method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#println(boolean)
   * @param b The boolean to be printed to both streams
   */
  public void println(boolean b)
  {
    _console.println(b);
    _pane.println(b);
  }

  /* -------------------------------------------------------------------------- */
  /* println method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#println(char)
   * @param c The character to be printed to both streams
   */
  public void println(char c)
  {
    _console.println(c);
    _pane.println(c);
  }

  /* -------------------------------------------------------------------------- */
  /* println method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#println(char[])
   * @param c The char array to be printed to both streams
   */
  public void println(char[] c)
  {
    _console.println(c);
    _pane.println(c);
  }

  /* -------------------------------------------------------------------------- */
  /* println method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#println(double)
   * @param d The double to be printed to both streams
   */
  public void println(double d)
  {
    _console.println(d);
    _pane.println(d);
  }

  /* -------------------------------------------------------------------------- */
  /* println method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#println(float)
   * @param f The float to be printed to both streams
   */
  public void println(float f)
  {
    _console.println(f);
    _pane.println(f);
  }

  /* -------------------------------------------------------------------------- */
  /* println method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#println(int)
   * @param i The int to be printed to both streams
   */
  public void println(int i)
  {
    _console.println(i);
    _pane.println(i);
  }

  /* -------------------------------------------------------------------------- */
  /* println method
   /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#println(long)
   * @param l The long to be printed to both streams
   */
  public void println(long l)
  {
    _console.println(l);
    _pane.println(l);
  }

  /* -------------------------------------------------------------------------- */
  /* println method
   /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#println(java.lang.Object)
   * @param o The object to be printed to both streams
   */
  public void println(Object o)
  {
    _console.println(o);
    _pane.println(o);
  }

  /* -------------------------------------------------------------------------- */
  /* print method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#print(java.lang.String)
   * @param s The string to be printed to both streams
   */
  public void print(String s)
  {
    _console.print(s);
    _pane.print(s);
  }

  /* -------------------------------------------------------------------------- */
  /* print method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#print(boolean)
   * @param b The boolean to be printed to both streams
   */
  public void print(boolean b)
  {
    _console.print(b);
    _pane.print(b);
  }

  /* -------------------------------------------------------------------------- */
  /* print method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#print(char)
   * @param c The char to be printed to both streams
   */
  public void print(char c)
  {
    _console.print(c);
    _pane.print(c);
  }

  /* -------------------------------------------------------------------------- */
  /* print method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#print(int)
   * @param i The int to be printed to both streams
   */
  public void print(int i)
  {
    _console.print(i);
    _pane.print(i);
  }

  /* -------------------------------------------------------------------------- */
  /* print method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#print(long)
   * @param l The long to be printed to both streams
   */
  public void print(long l)
  {
    _console.print(l);
    _pane.print(l);
  }

  /* -------------------------------------------------------------------------- */
  /* print method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#print(float)
   * @param f The float to be printed to both streams
   */
  public void print(float f)
  {
    _console.print(f);
    _pane.print(f);
  }

  /* -------------------------------------------------------------------------- */
  /* print method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#print(double)
   * @param d The double to be printed to both streams
   */
  public void print(double d)
  {
    _console.print(d);
    _pane.print(d);
  }

  /* -------------------------------------------------------------------------- */
  /* print method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#print(char[])
   * @param s The char array to be printed to both streams
   */
  public void print(char s[])
  {
    _console.print(s);
    _pane.print(s);
  }

  /* -------------------------------------------------------------------------- */
  /* print method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#print(char[])
   * @param o The object to be printed to both streams
   */
  public void print(Object o)
  {
    _console.print(o);
    _pane.print(o);
  }
  
  
  /* -------------------------------------------------------------------------- */
  /* checkError method
   /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#checkError()
   * @return boolean true if the print stream has encountered a problem
   */
  public boolean checkError()
  {
    return super.checkError() || _console.checkError() || _pane.checkError();
  }

  /* -------------------------------------------------------------------------- */
  /* close method
   /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.Closeable#close()
   * 
   */
  public void close()
  {
    _console.close();
    _pane.close();
  }
  /* -------------------------------------------------------------------------- */
  /* flush method
   /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.Flushable#flush()
   * 
   */
  public void flush()
  {
    _console.flush();
    _pane.flush();
  }
  /* -------------------------------------------------------------------------- */
  /* format method
   /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#format(java.util.Locale, java.lang.String, java.lang.Object[])
   * @param arg0
   * @param arg1
   * @param arg2
   * @return this print stream
   */
  public PrintStream format(Locale arg0, String arg1, Object... arg2)
  {
    _console.format(arg0,arg1,arg2);
    _pane.format(arg0, arg1, arg2);
    return this;
  }
  /* -------------------------------------------------------------------------- */
  /* format method
   /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#format(java.lang.String, java.lang.Object[])
   * @param arg0
   * @param arg1
   * @return this print stream
   */
  public PrintStream format(String arg0, Object... arg1)
  {
    _console.format(arg0, arg1);
    _pane.format(arg0, arg1);
    return this;
  }
  /* -------------------------------------------------------------------------- */
  /* printf method
   /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#printf(java.util.Locale, java.lang.String, java.lang.Object[])
   * @param arg0
   * @param arg1
   * @param arg2
   * @return this print stream
   */
  public PrintStream printf(Locale arg0, String arg1, Object... arg2)
  {
    _console.printf(arg0,arg1,arg2);
    _pane.printf(arg0,arg1,arg2);
    return this;
  }
  /* -------------------------------------------------------------------------- */
  /* printf method
   /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#printf(java.lang.String, java.lang.Object[])
   * @param arg0
   * @param arg1
   * @return this print stream
   */
  public PrintStream printf(String arg0, Object... arg1)
  {
    _console.printf(arg0,arg1);
    _pane.printf(arg0,arg1);
    return this;
  }
  /* -------------------------------------------------------------------------- */
  /* setError method
   /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.PrintStream#setError()
   * 
   */
  protected void setError()
  {
    super.setError();  
  }
  
  /* -------------------------------------------------------------------------- */
  /* write method
   /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.OutputStream#write(byte[], int, int)
   * @param arg0
   * @param arg1
   * @param arg2
   */
  public void write(byte[] arg0, int arg1, int arg2)
  {
    _console.write(arg0,arg1,arg2);
    _pane.write(arg0,arg1,arg2);
  }
  /* -------------------------------------------------------------------------- */
  /* write method
   /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.OutputStream#write(int)
   * @param arg0
   */
  public void write(int arg0)
  {
    _console.write(arg0);
    _pane.write(arg0);
  }
  /* -------------------------------------------------------------------------- */
  /* write method
   /* -------------------------------------------------------------------------- */
  /**
   * @see java.io.OutputStream#write(byte[])
   * @param arg0
   * @throws IOException
   */
  public void write(byte[] arg0) throws IOException
  {
    _console.write(arg0);
    _pane.write(arg0);
  }
}
