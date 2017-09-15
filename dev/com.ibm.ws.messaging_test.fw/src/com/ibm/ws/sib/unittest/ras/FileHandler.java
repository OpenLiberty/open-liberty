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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * <p>A custom implementation of a FileHandler that allows the file being 
 *   written to to be changed during a test run.
 * </p>
 * 
 * <p>It should be noted that the com.ibm.ws.sib.unittest.ras.Logger will have
 *   already worked out if the record was to be logged so this handler does not
 *   determine this. It would not be called if nothing were to be logged.
 * </p>
 *
 * <p>SIB build component: sib.unittest.ras</p>
 *
 * @author nottinga
 * @version 1.5
 * @since 1.0
 */
public class FileHandler extends Handler
{
  /* ************************************************************************** */
  /**
   * An AtomicallyCloseableWriter is a wrapper object for a Writer that can
   * be safely closed by any thread. Any write operations in progress when the
   * write is requested will complete (and the last write operation will close
   * the Writer if there were active writers
   *
   */
  /* ************************************************************************** */
  private static class AtomicallyCloseableWriter
  {
    /** The actual writer object onto which to output */
    private final Writer        _realWriter;
    /** How many threads are active (+1 if the writer is still open) */
    private final AtomicInteger _activeCount = new AtomicInteger(1);
    
    /* -------------------------------------------------------------------------- */
    /* AtomicallyCloseableWriter constructor
    /* -------------------------------------------------------------------------- */
    /**
     * Construct a new AtomicallyCloseableWriter.
     *
     * @param realWriter  The Writer on which to actually operate
     */
    public AtomicallyCloseableWriter(Writer realWriter)
    {
      _realWriter = realWriter;
    }
    
    /* -------------------------------------------------------------------------- */
    /* close method
    /* -------------------------------------------------------------------------- */
    /**
     * Close the writer when it's no longer in use
     * 
     * @see java.io.Writer#close()
     * @throws IOException
     */
    public void close() throws IOException
    {
      decrementCountAndCloseIfZero();
    }

    /* -------------------------------------------------------------------------- */
    /* flush method
    /* -------------------------------------------------------------------------- */
    /**
     * @see java.io.Writer#flush()
     * @throws IOException
     */
    public void flush() throws IOException
    {
      // Increment the active count
      int count = _activeCount.getAndIncrement();
      
      // if the count was greater than zero, the file is open
      try
      {
        if (count > 0)
          _realWriter.flush();
      }
      finally
      {
        // NOTE: Any exception from the close will override that from the flush
        decrementCountAndCloseIfZero();
      }      
    }

    /* -------------------------------------------------------------------------- */
    /* write method
    /* -------------------------------------------------------------------------- */
    /**
     * @see java.io.Writer#write(java.lang.String)
     * @param str
     * @throws IOException
     */
    public void write(String str) throws IOException
    {
      int count = _activeCount.getAndIncrement();
      
      // if the count was greater than zero, the file is open
      try
      {
        if (count > 0)
          _realWriter.write(str);
      }
      finally
      {
        // NOTE: Any exception from the close will override that from the write
        decrementCountAndCloseIfZero();
      }      
    }

    /* -------------------------------------------------------------------------- */
    /* decrementCountAndCloseIfZero method
    /* -------------------------------------------------------------------------- */
    /**
     * Decrement the active count atomically and if it is now zero, close the
     * writer object
     * 
     * @throws IOException
     */
    private void decrementCountAndCloseIfZero() throws IOException
    {
      int count = _activeCount.decrementAndGet();
      if (count == 0)
        _realWriter.close();
    }
  }
  
  /** An atomic reference to the writer object we're going to use. It's
   * an atomic reference so that we don't need to synchronize on any methods
   * in this class, while still allowing the file we're using to be updated.
   */
  private AtomicReference<AtomicallyCloseableWriter> reference_to_writer = new AtomicReference<AtomicallyCloseableWriter>();
  
  /* -------------------------------------------------------------------------- */
  /* FileHandler constructor
  /* -------------------------------------------------------------------------- */
  /**
   * Construct a new FileHandler.
   *
   */
  public FileHandler()
  {
    setFormatter(new AdvancedFormatter());
  }
  
  /* ------------------------------------------------------------------------ */
  /* publish method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @see Handler#publish(LogRecord)
   */
  public void publish(LogRecord arg0)
  {
    AtomicallyCloseableWriter writer = reference_to_writer.get();
    
    if (writer != null)
    {
      try
      {
        writer.write(getFormatter().format(arg0));
        writer.flush();
      }
      catch (IOException ioe)
      {
        reportError("Error publishing", ioe, ErrorManager.WRITE_FAILURE);
      }
    }
  }

  /* ------------------------------------------------------------------------ */
  /* flush method                                    
   /* ------------------------------------------------------------------------ */
  /**
   * @see Handler#flush()
   */
  public void flush()
  {
    AtomicallyCloseableWriter writer = reference_to_writer.get();
     
    if (writer != null)
    {
      try
      {
        writer.flush();
      }
      catch (IOException ioe)
      {
        reportError("Error flushing", ioe, ErrorManager.FLUSH_FAILURE);
      }
    }
  }

  /* ------------------------------------------------------------------------ */
  /* close method                                    
   /* ------------------------------------------------------------------------ */
  /**
   * @see Handler#close()
   */
  public void close() throws SecurityException
  {
    close(reference_to_writer.get());
  }

  /* -------------------------------------------------------------------------- */
  /* close method
  /* -------------------------------------------------------------------------- */
  /**
   * Close a specified AtomicallyCloseableWriter
   * 
   * @param writer
   */
  private void close(AtomicallyCloseableWriter writer)
  {
    if (writer != null)
    {
      try
      {
        writer.close();
      }
      catch (IOException ioe)
      {
        reportError("Error closing", ioe, ErrorManager.CLOSE_FAILURE);
      }
    }
  }

  /* ------------------------------------------------------------------------ */
  /* setFile method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method sets the file to be used for trace. If a file has been set then
   * calling this method a second time will result in the output being sent to
   * a new file.
   * 
   * @param file the file to send to.
   */
  public void setFile(File file)
  {
    try
    {
      // Note: we must get the new writer created and available before we close the old one
      AtomicallyCloseableWriter oldfile = reference_to_writer.getAndSet(new AtomicallyCloseableWriter(new FileWriter(file)));
      close(oldfile);
    }
    catch (IOException ioe)
    {
      reportError("Error opening " + file, ioe, ErrorManager.OPEN_FAILURE);
    }
  }
}
