/*
 * Copyright 2012 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.jbatch.container.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;





import com.ibm.jbatch.container.artifact.proxy.RetryProcessListenerProxy;
import com.ibm.jbatch.container.artifact.proxy.RetryReadListenerProxy;
import com.ibm.jbatch.container.artifact.proxy.RetryWriteListenerProxy;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.execution.impl.RuntimeStepExecution;
import com.ibm.jbatch.jsl.model.Chunk;
import com.ibm.jbatch.jsl.model.ExceptionClassFilter;

public class RetryHandler {

    /**
     *
     * Logic for handling retryable records.
     *
     * A RetryHandler object is attached to every BDS that inherits from AbstractBatchDataStream.
     *
     */

    private static final String className = RetryHandler.class.getName();
    private static Logger logger = Logger.getLogger(RetryHandler.class.getPackage().getName());

    public static final String RETRY_COUNT      = "retry-limit";
    public static final String RETRY_INCLUDE_EX = "include class";
    public static final String RETRY_EXCLUDE_EX = "exclude class";

    private static final int RETRY_NONE = 0;
    private static final int RETRY_READ = 1;
    private static final int RETRY_PROCESS = 2;
    private static final int RETRY_WRITE = 3;

    private int retryType = RETRY_NONE;

    /*private RetryProcessListenerProxy _retryProcessListener = null;
      private RetryReadListenerProxy _retryReadListener = null;
      private RetryWriteListenerProxy _retryWriteListener = null;*/

    List<RetryProcessListenerProxy> _retryProcessListeners = null;
    List<RetryReadListenerProxy> _retryReadListeners = null;
    List<RetryWriteListenerProxy> _retryWriteListeners = null;
    
    private ExceptionMatcher excMatcher = null;
    private ExceptionMatcher noRBexcMatcher = null;

    private String _stepId = null;
    private Set<String> _retryNoRBIncludeExceptions = null;
    private Set<String> _retryNoRBExcludeExceptions = null;
    private Set<String> _retryIncludeExceptions = null;
    private Set<String> _retryExcludeExceptions = null;
    private int _retryLimit = Integer.MIN_VALUE;
    private long _retryCount = 0;
    private Exception _retryException = null;

    public RetryHandler(Chunk chunk, RuntimeStepExecution runtimeStepExecution)
    {
        _stepId = runtimeStepExecution.getStepName();
        initialize(chunk);
    }


    /**
     * Add the user-defined RetryProcessListener.
     *
     */
    public void addRetryProcessListener(List<RetryProcessListenerProxy> retryProcessListeners)
    {
        _retryProcessListeners =  retryProcessListeners;
    }

    /**
     * Add the user-defined RetryReadListener.
     *
     */
    public void addRetryReadListener(List<RetryReadListenerProxy> retryReadListeners)
    {
        _retryReadListeners = retryReadListeners;
    }

    /**
     * Add the user-defined RetryWriteListener.
     *
     */
    public void addRetryWriteListener(List<RetryWriteListenerProxy> retryWriteListeners)
    {
        _retryWriteListeners = retryWriteListeners;
    }


    /**
     * Read the retry exception lists from the BDS props.
     */
    private void initialize(Chunk chunk)
    {
        final String mName = "initialize";

        if(logger.isLoggable(Level.FINER)) 
            logger.entering(className, mName);

        try
        {
            if (chunk.getRetryLimit() != null){
                _retryLimit = Integer.parseInt(chunk.getRetryLimit());
                if (_retryLimit < 0) {
                    throw new IllegalArgumentException("The retry-limit attribute on a chunk cannot be a negative value");
                }

            }
        }
        catch (NumberFormatException nfe)
        {
            throw new RuntimeException("NumberFormatException reading " + RETRY_COUNT, nfe);
        }

        // Read the include/exclude exceptions.
        _retryIncludeExceptions = new HashSet<String>();
        _retryExcludeExceptions = new HashSet<String>();
        _retryNoRBIncludeExceptions = new HashSet<String>();
        _retryNoRBExcludeExceptions = new HashSet<String>();

        if (chunk.getRetryableExceptionClasses() != null) {
            if (chunk.getRetryableExceptionClasses().getIncludeList() != null) {
                List<ExceptionClassFilter.Include> includes = chunk.getRetryableExceptionClasses().getIncludeList();
                for (ExceptionClassFilter.Include include : includes) {
                    _retryIncludeExceptions.add(include.getClazz().trim());
                    logger.finer("RETRYHANDLE: include: " + include.getClazz().trim());
                }

                if (_retryIncludeExceptions.size() == 0) {
                    logger.finer("RETRYHANDLE: include element not present");

                }
            }
            if (chunk.getRetryableExceptionClasses().getExcludeList() != null) {
                List<ExceptionClassFilter.Exclude> excludes = chunk.getRetryableExceptionClasses().getExcludeList();
                for (ExceptionClassFilter.Exclude exclude : excludes) {
                    _retryExcludeExceptions.add(exclude.getClazz().trim());
                    logger.finer("SKIPHANDLE: exclude: " + exclude.getClazz().trim());
                }

                if (_retryExcludeExceptions.size() == 0) {
                    logger.finer("SKIPHANDLE: exclude element not present");

                }
            }
        }

        if (chunk.getNoRollbackExceptionClasses() != null) {
            if (chunk.getNoRollbackExceptionClasses().getIncludeList() != null) {
                List<ExceptionClassFilter.Include> includes = chunk.getNoRollbackExceptionClasses().getIncludeList();
                for (ExceptionClassFilter.Include include : includes) {
                    _retryNoRBIncludeExceptions.add(include.getClazz().trim());
                    logger.finer("RETRYHANDLE: include: " + include.getClazz().trim());
                }

                if (_retryNoRBIncludeExceptions.size() == 0) {
                    logger.finer("RETRYHANDLE: include element not present");

                }
            }
            if (chunk.getNoRollbackExceptionClasses().getExcludeList() != null) {
                List<ExceptionClassFilter.Exclude> excludes = chunk.getNoRollbackExceptionClasses().getExcludeList();
                for (ExceptionClassFilter.Exclude exclude : excludes) {
                    _retryNoRBExcludeExceptions.add(exclude.getClazz().trim());
                    logger.finer("SKIPHANDLE: exclude: " + exclude.getClazz().trim());
                }

                if (_retryNoRBExcludeExceptions.size() == 0) {
                    logger.finer("SKIPHANDLE: exclude element not present");

                }
            }
        }
        
        // Create the ExceptionMatcher Object
        excMatcher = new ExceptionMatcher(_retryIncludeExceptions, _retryExcludeExceptions);
        noRBexcMatcher = new ExceptionMatcher(_retryNoRBIncludeExceptions, _retryNoRBExcludeExceptions);


        if (logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, className, mName, "added include exception " + _retryIncludeExceptions 
                    + "; added exclude exception " + _retryExcludeExceptions);
            logger.logp(Level.FINE, className, mName, "added include no rollback exception " + _retryNoRBIncludeExceptions
                    + "; added exclude no rollback exception " + _retryNoRBExcludeExceptions);
        }
            
        if(logger.isLoggable(Level.FINER)) {
          logger.exiting(className, mName, this.toString());
        }
      }
      
      public boolean isRollbackException(Exception e)
      {
          return !noRBexcMatcher.isSkippableOrRetryable(e);
      }
      /**
       * Handle exception from a read failure.
       */
      public void handleExceptionRead(Exception e)
      {
        final String mName = "handleExceptionRead";
        
        logger.finer("RETRYHANDLE: in retryhandler handle exception on a read:" + e.toString());

        if(logger.isLoggable(Level.FINER)) 
          logger.logp(Level.FINE, className, mName, e.getClass().getName() + "; " + this.toString());
        
        if (!isRetryLimitReached() && excMatcher.isSkippableOrRetryable(e))
        {
           retryType = RETRY_READ;
           _retryException = e;
          // Retry it.  Log it.  Call the RetryListener.
          ++_retryCount;
          logRetry(e);

          if (_retryReadListeners != null) {
              for (RetryReadListenerProxy retryReadListenerProxy : _retryReadListeners) {
                  retryReadListenerProxy.onRetryReadException(e);
                }
          }
        }
        else
        {
          // No retry.  Throw it back.
          if(logger.isLoggable(Level.FINER)) 
            logger.logp(Level.FINE, className, mName, "No retry.  Rethrow", e);
            throw new BatchContainerRuntimeException(e);
        }

        if(logger.isLoggable(Level.FINER)) 
          logger.exiting(className, mName, e);
      }

      /** 
       * Handle exception from a process failure.
       */
      public void handleExceptionProcess(Exception e, Object w)
      {
        final String mName = "handleExceptionProcess";
        
        if(logger.isLoggable(Level.FINER)) 
          logger.logp(Level.FINE, className, mName, e.getClass().getName() + "; " + this.toString());
        
        if (!isRetryLimitReached() && excMatcher.isSkippableOrRetryable(e))
        {
          retryType = RETRY_PROCESS;
          _retryException = e;
          // Retry it.  Log it.  Call the RetryListener.
          ++_retryCount;
          logRetry(e);

          if (_retryProcessListeners != null) {
              for (RetryProcessListenerProxy retryProcessListenerProxy : _retryProcessListeners) {
                  retryProcessListenerProxy.onRetryProcessException(w, e);
                }
          }
        }
        else
        {
          // No retry.  Throw it back.
          if(logger.isLoggable(Level.FINER)) 
            logger.logp(Level.FINE, className, mName, "No retry.  Rethrow ", e);
          throw new BatchContainerRuntimeException(e);
        }
      }
      
      /** 
       * Handle exception from a write failure.
       */
      public void handleExceptionWrite(Exception e, List<Object> w)
      {
        final String mName = "handleExceptionWrite";
        
        if(logger.isLoggable(Level.FINER)) 
          logger.logp(Level.FINE, className, mName, e.getClass().getName() + "; " + this.toString());

        if (!isRetryLimitReached() && excMatcher.isSkippableOrRetryable(e))
        {
          // Retry it.  Log it.  Call the RetryListener.
          retryType = RETRY_WRITE;
          _retryException = e;
          ++_retryCount;
          logRetry(e);

          if (_retryWriteListeners != null) {
              for (RetryWriteListenerProxy retryWriteListenerProxy : _retryWriteListeners) {
                  retryWriteListenerProxy.onRetryWriteException(w, e);
                }
          }
        }
        else
        {
          // No retry.  Throw it back.
          if(logger.isLoggable(Level.FINER)) 
            logger.logp(Level.FINE, className, mName, "No retry.  Rethrow ", e);
          throw new BatchContainerRuntimeException(e);
        }
      }

      /**
       * Check if the retry limit has been reached.
       *
       * Note: if retry handling isn't enabled (i.e. not configured in xJCL), then this method 
       *       will always return TRUE.
       */
      private boolean isRetryLimitReached()
      {
        // Unlimited retries if it is never defined
        if (_retryLimit == Integer.MIN_VALUE) {
            return false;
        }
          
        return (_retryCount >= _retryLimit);
      }

      
      private void logRetry(Exception e)
      {
        Object[] details = { _stepId, e.getClass().getName() + ": " + e.getMessage() };
        if(logger.isLoggable(Level.FINE)) 
            logger.logp(Level.FINE, className, "logRetry", "Logging details: ", details); 
        }

      public Exception getException()
      {
          return _retryException;
      }
      
      public long getRetryCount()
      {
        return _retryCount;
      }

      public void setRetryCount(long retryCount)
      {
        final String mName = "setRetryCount";

        _retryCount = retryCount;

        if(logger.isLoggable(Level.FINE)) 
          logger.logp(Level.FINE, className, mName, "setRetryCount: " + _retryCount);
      }

      public String toString()
      {
        return "RetryHandler{" + super.toString() + "}count:limit=" + _retryCount + ":" + _retryLimit;
      }

}
