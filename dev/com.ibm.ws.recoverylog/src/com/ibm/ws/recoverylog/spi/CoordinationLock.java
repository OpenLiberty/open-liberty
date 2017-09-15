/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
// Class: CoordinationLock
//------------------------------------------------------------------------------
/**
* <p>
* This class provides an exclusive lock facility between processes. It makes use
* of the file locking support provided by the NIO FileChannel. 
* </p>
*/
public class CoordinationLock
{
  /**
  * WebSphere RAS TraceComponent registration
  */
  private static final TraceComponent tc = Tr.register(CoordinationLock.class,
                                           TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

  /**
  *
  */
  public final static int LOCK_SUCCESS = 1;
  public final static int LOCK_FAILURE = 2;
  public final static int LOCK_INTERRUPT = 4;

  /**
  * A static constant that defines the number of attempts that will be made to
  * aquire the lock. Set to 0 for infinate number of attempts.
  */
  final static int LOCKRETRYCOUNT = 0;

  /**
  * A static constant that defines the delay between lock attempts.
  * d255605 - reduce to 10 secs to improve response as DCS is faster than
  * Windows file processing and we often miss an attempt.
  */
  final static int LOCKRETRYDELAY = 10000; // @255605C

  /**
  * A static constant that defines the number of lock attempts per message. (ie every 5 mins)
  */
  final static int LOCKRETRYTIMES = 30; // @255605C

  /**
  * The name of the first recovery log file.
  */ 
  private static String RECOVERY_FILE_1_NAME = "log1";

  /**
  * The name of the second recovery log file.
  */ 
  private static String RECOVERY_FILE_2_NAME = "log2";

  CoordinationLockHandle _handle1 = null;
  CoordinationLockHandle _handle2 = null;

  /**
  * The expanded directory string that identifies where the lock file resides.
  */
  private String  _lockDirectory;

  /**
  * A boolean flag to indicate if the lock file has either been created or has 
  * been confirmed to exist already.
  */
  private boolean _lockFilesExist = false;

  /**
  *
  */
  private boolean _interrupted = false;

  //------------------------------------------------------------------------------
  // Method: CoordinationLock.CoordinationLock
  //------------------------------------------------------------------------------
  /**
  * Constructor. Creates a new coordination lock. This lock is created under the
  * directory lockDirectory/LOCKDIRNAME. The filename of the lock is LOCKFILENAME.
  *
  * @param lockDirectory The directory in which the lock should reside.
  */
  public CoordinationLock(String lockDirectory)
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "CoordinationLock", lockDirectory);
    
    _lockDirectory = lockDirectory;
   
    if (tc.isEntryEnabled()) Tr.exit(tc, "CoordinationLock", this);
  }

  //------------------------------------------------------------------------------
  // Method: CoordinationLock.lock
  //------------------------------------------------------------------------------
  /**
  * Obtains the exclusive lock.
  */
  public int lock()
  {
      if (tc.isEntryEnabled()) Tr.entry(tc, "lock");

      int result = LOCK_FAILURE;

      // Ensure that the target lock files exist.
      if (!_lockFilesExist)
      {
          if (tc.isDebugEnabled()) Tr.debug(tc,"Confirming/Creating the lock files");

          _handle1 = ensureFileExists(RECOVERY_FILE_1_NAME);
          if (_handle1 != null)
          {
              _handle2 = ensureFileExists(RECOVERY_FILE_2_NAME);

              if (_handle2 != null)
              {
                _lockFilesExist = true;
              }
              else
              {
                Tr.error(tc,"CWRLS0004_RECOVERY_LOG_CREATE_FAILED", _lockDirectory + File.separator + RECOVERY_FILE_2_NAME);
                _handle1 = null;
              }
          }
          else
          {
            Tr.error(tc,"CWRLS0004_RECOVERY_LOG_CREATE_FAILED",  _lockDirectory + File.separator + RECOVERY_FILE_1_NAME);
          }
      }
      else
      {
          if (tc.isDebugEnabled()) Tr.debug(tc,"Already Confirmed/Created the lock files");
      }

      if (_lockFilesExist)
      {
        result = obtainLock(_handle1);

        if (result == LOCK_SUCCESS)
        {
          result = obtainLock(_handle2);

          if (result != LOCK_SUCCESS)
          {
            if (result == LOCK_FAILURE)
            {
               Tr.error(tc,"CWRLS0005_RECOVERY_LOG_LOCK_FAILED", _lockDirectory + File.separator + RECOVERY_FILE_2_NAME);
            }

            releaseLock(_handle1);
          }
        }
        else if (result == LOCK_FAILURE)
        {
          Tr.error(tc,"CWRLS0005_RECOVERY_LOG_LOCK_FAILED", _lockDirectory + File.separator + RECOVERY_FILE_1_NAME);
        }
      }

      if (tc.isEntryEnabled()) Tr.exit(tc, "lock",new Integer(result));
      return result;
  }

  //------------------------------------------------------------------------------
  // Method: CoordinationLock.unlock
  //------------------------------------------------------------------------------
  /**
  * Releases the exclusive lock.
  */
  public void unlock()
  {
      if (tc.isEntryEnabled()) Tr.entry(tc, "unlock");

      if (_handle2 != null)
      {
        releaseLock(_handle2);
      }
      if (_handle1 != null)
      {
        releaseLock(_handle1);
      }

      _handle2=null;
      _handle1=null;

      if (tc.isEntryEnabled()) Tr.exit(tc, "unlock");
  }

  //------------------------------------------------------------------------------
  // Method: CoordinationLock.obtainLock
  //------------------------------------------------------------------------------
  /**
  * Obtains the exclusive lock.
  */
  private int obtainLock(CoordinationLockHandle handle)
  {
      if (tc.isEntryEnabled()) Tr.entry(tc, "obtainLock",handle);

      int result = LOCK_FAILURE;

      try
      {
          final File file = new File(handle._directory,handle._fileName);

          try
          {
              // handle._raf = (RandomAccessFile)AccessController.doPrivileged(
              handle._raf = (RandomAccessFile) Configuration.getAccessController().
                  doPrivileged(
              new java.security.PrivilegedExceptionAction()
              {
                public java.lang.Object run() throws Exception
                {
                    if (tc.isEntryEnabled()) Tr.entry(tc, "run", this);

//                    RandomAccessFile raf = new RandomAccessFile(file, "rw");
                    RandomAccessFile raf = RLSAccessFile.getRLSAccessFile(file); // @255605C

                    if (tc.isEntryEnabled()) Tr.exit(tc, "run");
                    return raf;
                }
              });

              handle._channel = handle._raf.getChannel();
          }
          catch (java.security.PrivilegedActionException exc)
          {
              FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.CoordinationLock.obtainLock", "253", this);
              throw exc;
          }
          catch (Exception exc)
          {
              FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.CoordinationLock.obtainLock", "258", this);
              throw exc;
          }

          boolean halt = false;
          int lockAttempt = 0;

          while (!halt)
          {
              lockAttempt++;

              if (tc.isDebugEnabled()) Tr.debug(tc,"RLSHA: Lock attempt #" + lockAttempt + " on lock file " + handle);

              handle._fileLock = handle._channel.tryLock();

              if (handle._fileLock != null)
              {
                  if (tc.isDebugEnabled()) Tr.debug(tc,"RLSHA: Obtained an exclusive access lock on lock file " + handle);
                  halt = true;
                  result = LOCK_SUCCESS;
              }
              else
              {
                  if (tc.isDebugEnabled()) Tr.debug(tc,"RLSHA: Unable to obtain an exclusive access lock on lock file " + handle);

                  // Should we allow a retry on this lock.
                  if ((LOCKRETRYCOUNT <= 0) || (lockAttempt < LOCKRETRYCOUNT))
                  {
                      if (LOCKRETRYDELAY > 0)
                      {
                          // Output a message if we cannot get a lock - delay the message for the
                          // first pass through...
                          int l = lockAttempt - 2;
                          if ((l/LOCKRETRYTIMES)*LOCKRETRYTIMES == l)
                          {
                            Tr.warning(tc,"CWRLS0026_RECOVERY_LOG_LOCK_RETRY", file);
                          }
                          try
                          {
                              if (tc.isDebugEnabled()) Tr.debug(tc,"Sleeping for " + LOCKRETRYDELAY + " ms before re-try");
                              Thread.sleep(LOCKRETRYDELAY);
                          }
                          catch (Exception exc)
                          {
                              // No FFDC code needed
                          }
                      }

                      // INTERRUPT HANDLER - Check that the interrupt has not been flagged. If it has then abort
                      // and return the interrupted status.
                      if (_interrupted)
                      {
                        halt = true;
                        result = LOCK_INTERRUPT;
                        try                  // @255605A
                        {
                          handle._raf.close();
                        }
                        catch (Exception e)
                        {
                        }
                      }
                  }
                  else
                  {
                      if (tc.isDebugEnabled()) Tr.debug(tc,"Retry count exceeded, aborting lock attempt on lock file " + handle);
                      halt = true;
                      try                  // @255605A
                      {
                        handle._raf.close();
                      }
                      catch (Exception e)
                      {
                      }
                  }
              }
          }
      }
      catch (Exception exc)
      {
          if (tc.isDebugEnabled()) Tr.debug(tc,"An unexpected exception has occured when trying to obtain the exclusive lock " + exc);
          // d251015 - Output the exception to assist with PD when locks fail as we can get some
          // non-obvious errors with NFS, etc.  The callers message will also assist.
          Tr.error(tc,"CWRLS0024_EXC_DURING_RECOVERY", exc);
          if (handle._raf != null) // @255605A
          {
              try
              {
                  handle._raf.close();
              }
              catch (Exception e)
              {
              }
          }
      }

      if (tc.isEntryEnabled()) Tr.exit(tc, "obtainLock",new Integer(result));
      return result;
  }


  //------------------------------------------------------------------------------
  // Method: CoordinationLock.releaseLock
  //------------------------------------------------------------------------------
  /**
  * Releases the exclusive lock.
  */
  public void releaseLock(CoordinationLockHandle clh)
  {
      if (tc.isEntryEnabled()) Tr.entry(tc, "releaseLock",clh);

      if (clh._fileLock != null)
      {
          try
          {
              clh._fileLock.release();
              clh._fileLock = null;
          }
          catch (Exception exc)
          {
          }
      }

      if (clh._channel != null)
      {
          try
          {
              clh._channel.close();
          }
          catch (Exception exc)
          {
          }
      }

      if (clh._raf != null)
      {
          try
          {
              clh._raf.close();
              clh._raf = null;
          }
          catch (Exception exc)
          {
          }
      }

      if (tc.isEntryEnabled()) Tr.exit(tc, "releaseLock");
  }

  //------------------------------------------------------------------------------
  // Method: CoordinationLock.ensureFileExists
  //------------------------------------------------------------------------------
  /**
  */
  public CoordinationLockHandle ensureFileExists(String lockFile)
  {
      if (tc.isEntryEnabled()) Tr.entry(tc, "ensureFileExists",lockFile);

      CoordinationLockHandle clh = null;

      // If the directory exists then ensure that the lock file exists.
      if (RLSUtils.createDirectoryTree(_lockDirectory))
      {
          final File file = new File(_lockDirectory,lockFile);

          // Ensure that the file exists. Creating the RandomAccessFile object
          // will create the file if it does not.
          try
          {
              // AccessController.doPrivileged(
              Configuration.getAccessController().doPrivileged(
              new java.security.PrivilegedExceptionAction()
              {
                public java.lang.Object run() throws Exception
                {
                    if (tc.isEntryEnabled()) Tr.entry(tc, "run", this);

                    // Cause the file to be created and then close its handle
                    RandomAccessFile raf = new RandomAccessFile(file, "rw");
                    raf.close();

                    if (tc.isEntryEnabled()) Tr.exit(tc, "run");
                    return null;
                }
              });

              if (file.exists())
              {
                clh = new CoordinationLockHandle();
                clh._fileName = lockFile;
                clh._directory = _lockDirectory;
              }
          }
          catch (java.security.PrivilegedActionException exc)
          {
              FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.CoordinationLock.ensureFileExists", "418", this);
          }
          catch (Exception exc)
          {
              FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.CoordinationLock.ensureFileExists", "422", this);
          }

          if (clh != null)
          {
            if (tc.isDebugEnabled()) Tr.debug(tc, "Lock file exists");
          }
          else
          {
            if (tc.isDebugEnabled()) Tr.debug(tc, "Unable to create the lock file");
          }
      }
      else
      {
          if (tc.isDebugEnabled()) Tr.debug(tc, "Unable to create the lock directory");
      }

      if (tc.isEntryEnabled()) Tr.exit(tc, "ensureFileExists",clh);
      return clh;
  }

  public class CoordinationLockHandle
  {
    RandomAccessFile _raf = null;
    FileChannel _channel = null; 
    String _fileName = null;
    String _directory = null;
    FileLock _fileLock = null;

    void raf(RandomAccessFile raf)
    {
      _raf=raf;
    }

    public String toString()
    {
      return _directory + File.separator + _fileName;
    }
  }

  public synchronized void interrupt()
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "interrupt");

    _interrupted = true;

    if (tc.isEntryEnabled()) Tr.exit(tc, "interrupt");
  }
}
