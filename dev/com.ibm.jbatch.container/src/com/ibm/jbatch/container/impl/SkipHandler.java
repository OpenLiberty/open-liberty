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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;




import com.ibm.jbatch.container.artifact.proxy.SkipProcessListenerProxy;
import com.ibm.jbatch.container.artifact.proxy.SkipReadListenerProxy;
import com.ibm.jbatch.container.artifact.proxy.SkipWriteListenerProxy;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.execution.impl.RuntimeStepExecution;
import com.ibm.jbatch.jsl.model.Chunk;
import com.ibm.jbatch.jsl.model.ExceptionClassFilter;

public class SkipHandler {

    /**
     *
     * Logic for handling skipped records.
     *
     */

      private static final String className = SkipHandler.class.getName();
        private static Logger logger = Logger.getLogger(SkipHandler.class.getPackage().getName());

      public static final String SKIP_COUNT      = "skip-limit";
      public static final String SKIP_INCLUDE_EX = "include class";
      public static final String SKIP_EXCLUDE_EX = "exclude class";

      private List<SkipProcessListenerProxy> _skipProcessListener = null;
      private List<SkipReadListenerProxy> _skipReadListener = null;
      private List<SkipWriteListenerProxy> _skipWriteListener = null;
      
      private ExceptionMatcher excMatcher = null;
      
      private String _stepId = null;
      private Set<String> _skipIncludeExceptions = null;
      private Set<String> _skipExcludeExceptions = null;
      private int _skipLimit = Integer.MIN_VALUE;
      private long _skipCount = 0;

      public SkipHandler(Chunk chunk, RuntimeStepExecution runtimeStepExecution) 
      {
          _stepId = runtimeStepExecution.getStepName();
          initialize(chunk);
      }

    /**
       * Add the user-defined SkipReadListeners.
       *
       */
      public void addSkipReadListener(List<SkipReadListenerProxy> skipReadListener)
      {
        _skipReadListener = skipReadListener;
      }
      
      /**
       * Add the user-defined SkipWriteListeners.
       *
       */
      public void addSkipWriteListener(List<SkipWriteListenerProxy> skipWriteListener)
      {
        _skipWriteListener = skipWriteListener;
      }
      
      /**
       * Add the user-defined SkipReadListeners.
       *
       */
      public void addSkipProcessListener(List<SkipProcessListenerProxy> skipProcessListener)
      {
        _skipProcessListener = skipProcessListener;
      }


      /**
       * Read the skip exception lists from the BDS props.
       */
      private void initialize(Chunk chunk)
      {
        final String mName = "initialize";

        if(logger.isLoggable(Level.FINER)) 
          logger.entering(className, mName);

        try
        {
            if (chunk.getSkipLimit() != null){
                _skipLimit = Integer.parseInt(chunk.getSkipLimit());
                if (_skipLimit < 0) {
                    throw new IllegalArgumentException("The skip-limit attribute on a chunk cannot be a negative value");
                }
            }
        }
        catch (NumberFormatException nfe)
        {
          throw new RuntimeException("NumberFormatException reading " + SKIP_COUNT, nfe);
        }


        // Read the include/exclude exceptions.

        _skipIncludeExceptions = new HashSet<String>();
        _skipExcludeExceptions = new HashSet<String>();

        if (chunk.getSkippableExceptionClasses() != null) {
            if (chunk.getSkippableExceptionClasses().getIncludeList() != null) {
                List<ExceptionClassFilter.Include> includes = chunk.getSkippableExceptionClasses().getIncludeList();

                for (ExceptionClassFilter.Include include : includes) {
                    _skipIncludeExceptions.add(include.getClazz().trim());
                    logger.finer("SKIPHANDLE: include: " + include.getClazz().trim());
                }

                if (_skipIncludeExceptions.size() == 0) {
                    logger.finer("SKIPHANDLE: include element not present");

                }
            }
        }

        if (chunk.getSkippableExceptionClasses() != null) {
            if (chunk.getSkippableExceptionClasses().getExcludeList() != null) {
                List<ExceptionClassFilter.Exclude> excludes = chunk.getSkippableExceptionClasses().getExcludeList();

                for (ExceptionClassFilter.Exclude exclude : excludes) {
                    _skipExcludeExceptions.add(exclude.getClazz().trim());
                    logger.finer("SKIPHANDLE: exclude: " + exclude.getClazz().trim());
                }

                if (_skipExcludeExceptions.size() == 0) {
                    logger.finer("SKIPHANDLE: exclude element not present");

                }
            }
        }
        
        // Create the ExceptionMatcher Object
        excMatcher = new ExceptionMatcher(_skipIncludeExceptions, _skipExcludeExceptions);

        if (logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, className, mName, "added include exception " + _skipIncludeExceptions
                    + "; added exclude exception " + _skipExcludeExceptions);
            
        if(logger.isLoggable(Level.FINER)) 
          logger.exiting(className, mName, this.toString());
      }


      /**
       * Handle exception from a read failure.
       */
      public void handleExceptionRead(Exception e)
      {
        final String mName = "handleException";
        
        logger.finer("SKIPHANDLE: in skiphandler handle exception on a read");

        if(logger.isLoggable(Level.FINER)) 
          logger.logp(Level.FINE, className, mName, e.getClass().getName() + "; " + this.toString());
        
        if (!isSkipLimitReached() && excMatcher.isSkippableOrRetryable(e))
        {
          // Skip it.  Log it.  Call the SkipListener.
          ++_skipCount;
          logSkip(e);

          if (_skipReadListener != null) {
              for (SkipReadListenerProxy skipReadListenerProxy : _skipReadListener) {
                  skipReadListenerProxy.onSkipReadItem(e);
                }
          }
        }
        else
        {
          // No skip.  Throw it back. don't throw it back - we might want to retry ...
          if(logger.isLoggable(Level.FINER)) 
            logger.logp(Level.FINE, className, mName, "No skip.  Rethrow", e);
            throw new BatchContainerRuntimeException(e);
        }

        if(logger.isLoggable(Level.FINER)) 
          logger.exiting(className, mName, e);
      }

      /** 
       * Handle exception from a process failure.
       */
      public void handleExceptionWithRecordProcess(Exception e, Object w)
      {
        final String mName = "handleExceptionWithRecordProcess";
        if(logger.isLoggable(Level.FINER)) 
          logger.logp(Level.FINE, className, mName, e.getClass().getName() + "; " + this.toString());

        if (!isSkipLimitReached() && excMatcher.isSkippableOrRetryable(e))
        {
          // Skip it.  Log it.  Call the SkipProcessListener.
          ++_skipCount;
          logSkip(e);

          if (_skipProcessListener != null) {
              for (SkipProcessListenerProxy skipProcessListenerProxy : _skipProcessListener) {
                  skipProcessListenerProxy.onSkipProcessItem(w, e);
                }
          }
        }
        else
        {
          // No skip.  Throw it back.
          if(logger.isLoggable(Level.FINER)) 
            logger.logp(Level.FINE, className, mName, "No skip.  Rethrow ", e);
          throw new BatchContainerRuntimeException(e);
        }
      }
      /** 
       * Handle exception from a write failure.
       */
      public void handleExceptionWithRecordListWrite(Exception e, List<?> items)
      {
        final String mName = "handleExceptionWithRecordListWrite(Exception, List<?>)";
        
        if(logger.isLoggable(Level.FINER)) 
          logger.logp(Level.FINE, className, mName, e.getClass().getName() + "; " + this.toString());

        if (!isSkipLimitReached() && excMatcher.isSkippableOrRetryable(e))
        {
          // Skip it.  Log it.  Call the SkipListener.
          ++_skipCount;
          logSkip(e);

          if (_skipWriteListener != null) {
              for (SkipWriteListenerProxy skipWriteListenerProxy : _skipWriteListener) {
                  skipWriteListenerProxy.onSkipWriteItem(items, e);
                }
          }
        }
        else
        {
          // No skip.  Throw it back. - No, exit without throwing
          if(logger.isLoggable(Level.FINER)) 
            logger.logp(Level.FINE, className, mName, "No skip.  Rethrow ", e);
          throw new BatchContainerRuntimeException(e);
        }
      }
      
      /**
         * Check if the skip limit has been reached.
         * 
         * Note: if skip handling isn' enabled (i.e. not configured in the xJCL), then
         * this method will always return TRUE.
         * @return
         */
        private boolean isSkipLimitReached() {
            // Unlimited skips if it is never defined
            if(_skipLimit == Integer.MIN_VALUE) {
                return false;
            }
            
            return (_skipCount >= _skipLimit);
        }
      
      private void logSkip(Exception e)
      {
        Object[] details = { _stepId, e.getClass().getName() + ": " + e.getMessage() };
        if(logger.isLoggable(Level.FINE)) 
          logger.logp(Level.FINE, className, "logSkip", "Logging details: ", details); 
      }


      public long getSkipCount()
      {
        return _skipCount;
      }

      public void setSkipCount(long skipCount)
      {
        final String mName = "setSkipCount";

        _skipCount = skipCount;

        if(logger.isLoggable(Level.FINE)) 
          logger.logp(Level.FINE, className, mName, "setSkipCount: " + _skipCount);
      }

      public String toString()
      {
        return "SkipHandler{" + super.toString() + "}count:limit=" + _skipCount + ":" + _skipLimit;
      }
}