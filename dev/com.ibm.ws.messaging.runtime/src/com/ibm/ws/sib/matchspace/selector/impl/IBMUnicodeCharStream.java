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
package com.ibm.ws.sib.matchspace.selector.impl;

import com.ibm.ws.sib.matchspace.utils.FFDC;

/**An implementation of interface CharStream, where the stream is assumed to
 * contain unicode characters.
 *
 */
public final class IBMUnicodeCharStream implements CharStream
{
  private static final Class cclass = IBMUnicodeCharStream.class;
  
  public static final boolean staticFlag = false;

  public int bufpos = -1;
  int bufsize;
  int available;
  int tokenBegin;
  private int bufline[];
  private int bufcolumn[];

  private int column = 0;
  private int line = 1;

  private java.io.Reader inputStream;
  private boolean streamClosed = false;
  
  private boolean prevCharIsCR = false;
  private boolean prevCharIsLF = false;

  private char[] nextCharBuf;
  private char[] buffer;
  private int maxNextCharInd = 0;
  private int nextCharInd = -1;
  private int inBuf = 0;

  private final void ExpandBuff(boolean wrapAround)
  {
    char[] newbuffer = new char[bufsize + 2048];
    int newbufline[] = new int[bufsize + 2048];
    int newbufcolumn[] = new int[bufsize + 2048];

    try
    {
      if (wrapAround)
      {
        System.arraycopy(
          buffer,
          tokenBegin,
          newbuffer,
          0,
          bufsize - tokenBegin);
        System.arraycopy(buffer, 0, newbuffer, bufsize - tokenBegin, bufpos);
        buffer = newbuffer;

        System.arraycopy(
          bufline,
          tokenBegin,
          newbufline,
          0,
          bufsize - tokenBegin);
        System.arraycopy(bufline, 0, newbufline, bufsize - tokenBegin, bufpos);
        bufline = newbufline;

        System.arraycopy(
          bufcolumn,
          tokenBegin,
          newbufcolumn,
          0,
          bufsize - tokenBegin);
        System.arraycopy(
          bufcolumn,
          0,
          newbufcolumn,
          bufsize - tokenBegin,
          bufpos);
        bufcolumn = newbufcolumn;

        bufpos += (bufsize - tokenBegin);
      }
      else
      {
        System.arraycopy(
          buffer,
          tokenBegin,
          newbuffer,
          0,
          bufsize - tokenBegin);
        buffer = newbuffer;

        System.arraycopy(
          bufline,
          tokenBegin,
          newbufline,
          0,
          bufsize - tokenBegin);
        bufline = newbufline;

        System.arraycopy(
          bufcolumn,
          tokenBegin,
          newbufcolumn,
          0,
          bufsize - tokenBegin);
        bufcolumn = newbufcolumn;

        bufpos -= tokenBegin;
      }
    }
    catch (Throwable t)
    {
      // No FFDC Code Needed.
      // FFDC driven by wrapper class.      
      FFDC.processException(this,
          cclass,
          "com.ibm.ws.matchspace.selector.impl.IBMUnicodeCharStream.ExpandBuffer",
          t,
          "1:150:1.18");
      throw new Error(t.getMessage());
    }

    available = (bufsize += 2048);
    tokenBegin = 0;
  }

  private final void FillBuff() throws java.io.IOException
  {
    int i;
    
    if (maxNextCharInd == 4096)
      maxNextCharInd = nextCharInd = 0;

    try
    {
      if(!streamClosed)
      {
        if ((i = inputStream.read(nextCharBuf, maxNextCharInd, 4096 - maxNextCharInd))
            == -1)
        {
          inputStream.close();
          streamClosed = true;
        }
        else
          maxNextCharInd += i;
      }
    }
    catch (java.io.IOException e)
    {
      // No FFDC code needed
      tidyUpBuff();
      throw e;
    }

    // Handle the EOF stream case with a less heavyweight exception
    // than IOException    
    if(streamClosed)
    {
      // No FFDC code needed
      tidyUpBuff();
      throw new SelectorStreamEOFException();      
    }
  }

  private final char ReadChar() throws java.io.IOException
  {
    if (++nextCharInd >= maxNextCharInd)
      FillBuff();

    return nextCharBuf[nextCharInd];
  }

  public final char BeginToken() throws java.io.IOException
  {
    if (inBuf > 0)
    {
      --inBuf;
      return buffer[tokenBegin =
        (bufpos == bufsize - 1) ? (bufpos = 0) : ++bufpos];
    }

    tokenBegin = 0;
    bufpos = -1;

    return readChar();
  }

  private final void AdjustBuffSize()
  {
    if (available == bufsize)
    {
      if (tokenBegin > 2048)
      {
        bufpos = 0;
        available = tokenBegin;
      }
      else
        ExpandBuff(false);
    }
    else
      if (available > tokenBegin)
        available = bufsize;
      else
        if ((tokenBegin - available) < 2048)
          ExpandBuff(true);
        else
          available = tokenBegin;
  }

  private final void UpdateLineColumn(char c)
  {
    column++;

    if (prevCharIsLF)
    {
      prevCharIsLF = false;
      line += (column = 1);
    }
    else
      if (prevCharIsCR)
      {
        prevCharIsCR = false;
        if (c == '\n')
        {
          prevCharIsLF = true;
        }
        else
          line += (column = 1);
      }

    switch (c)
    {
      case '\r' :
        prevCharIsCR = true;
        break;
      case '\n' :
        prevCharIsLF = true;
        break;
      case '\t' :
        column--;
        column += (8 - (column & 07));
        break;
      default :
        break;
    }

    bufline[bufpos] = line;
    bufcolumn[bufpos] = column;
  }

  public final char readChar() throws java.io.IOException
  {
    if (inBuf > 0)
    {
      --inBuf;
      return buffer[(bufpos == bufsize - 1) ? (bufpos = 0) : ++bufpos];
    }

    char c;

    if (++bufpos == available)
      AdjustBuffSize();

    buffer[bufpos] = c = ReadChar();

    UpdateLineColumn(c);

    return c;
  }

  /**
   * @deprecated
   * @see #getEndColumn
   */

  public final int getColumn()
  {
    return bufcolumn[bufpos];
  }

  /**
   * @deprecated
   * @see #getEndLine
   */

  public final int getLine()
  {
    return bufline[bufpos];
  }

  public final int getEndColumn()
  {
    return bufcolumn[bufpos];
  }

  public final int getEndLine()
  {
    return bufline[bufpos];
  }

  public final int getBeginColumn()
  {
    return bufcolumn[tokenBegin];
  }

  public final int getBeginLine()
  {
    return bufline[tokenBegin];
  }

  public final void backup(int amount)
  {

    inBuf += amount;
    if ((bufpos -= amount) < 0)
      bufpos += bufsize;
  }

  public IBMUnicodeCharStream(
    java.io.Reader dstream,
    int startline,
    int startcolumn,
    int buffersize)
  {
    inputStream = dstream;
    streamClosed = false;
    line = startline;
    column = startcolumn - 1;

    available = bufsize = buffersize;
    buffer = new char[buffersize];
    bufline = new int[buffersize];
    bufcolumn = new int[buffersize];
    nextCharBuf = new char[4096];
  }

  public IBMUnicodeCharStream(
    java.io.Reader dstream,
    int startline,
    int startcolumn)
  {
    this(dstream, startline, startcolumn, 4096);
  }
  public void ReInit(
    java.io.Reader dstream,
    int startline,
    int startcolumn,
    int buffersize)
  {
    inputStream = dstream;
    streamClosed = false;
    line = startline;
    column = startcolumn - 1;

    if (buffer == null || buffersize != buffer.length)
    {
      available = bufsize = buffersize;
      buffer = new char[buffersize];
      bufline = new int[buffersize];
      bufcolumn = new int[buffersize];
      nextCharBuf = new char[4096];
    }
    prevCharIsLF = prevCharIsCR = false;
    tokenBegin = inBuf = maxNextCharInd = 0;
    nextCharInd = bufpos = -1;
  }

  public void ReInit(java.io.Reader dstream, int startline, int startcolumn)
  {
    ReInit(dstream, startline, startcolumn, 4096);
  }
  public IBMUnicodeCharStream(
    java.io.InputStream dstream,
    int startline,
    int startcolumn,
    int buffersize)
  {
    this(new java.io.InputStreamReader(dstream), startline, startcolumn, 4096);
  }

  public IBMUnicodeCharStream(
    java.io.InputStream dstream,
    int startline,
    int startcolumn)
  {
    this(dstream, startline, startcolumn, 4096);
  }

  public void ReInit(
    java.io.InputStream dstream,
    int startline,
    int startcolumn,
    int buffersize)
  {
    ReInit(
      new java.io.InputStreamReader(dstream),
      startline,
      startcolumn,
      4096);
  }
  public void ReInit(
    java.io.InputStream dstream,
    int startline,
    int startcolumn)
  {
    ReInit(dstream, startline, startcolumn, 4096);
  }

  public final String GetImage()
  {
    if (bufpos >= tokenBegin)
      return new String(buffer, tokenBegin, bufpos - tokenBegin + 1);
    else
      return new String(buffer, tokenBegin, bufsize - tokenBegin)
        + new String(buffer, 0, bufpos + 1);
  }

  public final char[] GetSuffix(int len)
  {
    char[] ret = new char[len];

    if ((bufpos + 1) >= len)
      System.arraycopy(buffer, bufpos - len + 1, ret, 0, len);
    else
    {
      System.arraycopy(
        buffer,
        bufsize - (len - bufpos - 1),
        ret,
        0,
        len - bufpos - 1);
      System.arraycopy(buffer, 0, ret, len - bufpos - 1, bufpos + 1);
    }

    return ret;
  }

  public void Done()
  {
    nextCharBuf = null;
    buffer = null;
    bufline = null;
    bufcolumn = null;
  }

  /**
   * Method to adjust line and column numbers for the start of a token.
   */
  public void adjustBeginLineColumn(int newLine, int newCol)
  {
    int start = tokenBegin;
    int len;

    if (bufpos >= tokenBegin)
    {
      len = bufpos - tokenBegin + inBuf + 1;
    }
    else
    {
      len = bufsize - tokenBegin + bufpos + 1 + inBuf;
    }

    int i = 0, j = 0, k = 0;
    int nextColDiff = 0, columnDiff = 0;

    while (i < len && bufline[j =
      start % bufsize] == bufline[k = ++start % bufsize])
    {
      bufline[j] = newLine;
      nextColDiff = columnDiff + bufcolumn[k] - bufcolumn[j];
      bufcolumn[j] = newCol + columnDiff;
      columnDiff = nextColDiff;
      i++;
    }

    if (i < len)
    {
      bufline[j] = newLine++;
      bufcolumn[j] = newCol + columnDiff;

      while (i++ < len)
      {
        if (bufline[j = start % bufsize] != bufline[++start % bufsize])
          bufline[j] = newLine++;
        else
          bufline[j] = newLine;
      }
    }

    line = bufline[j];
    column = bufcolumn[j];
  }
  
  private final void tidyUpBuff()
  {
    if (bufpos != 0)
    {
      --bufpos;
      backup(0);
    }
    else
    {
      bufline[bufpos] = line;
      bufcolumn[bufpos] = column;
    }
  }
}
