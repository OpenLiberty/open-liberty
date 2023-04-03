package org.jboss.resteasy.plugins.server.servlet;

import org.jboss.resteasy.concurrent.ContextualExecutors;
import org.jboss.resteasy.concurrent.ContextualScheduledExecutorService;
import org.jboss.resteasy.core.AbstractAsynchronousResponse;
import org.jboss.resteasy.core.AbstractExecutionContext;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyAsynchronousContext;
import org.jboss.resteasy.spi.ResteasyAsynchronousResponse;
import org.jboss.resteasy.spi.RunnableWithException;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Servlet3AsyncHttpRequest extends HttpServletInputMessage
{
   protected HttpServletResponse response;
   protected ResteasyAsynchronousContext asynchronousContext;

   public Servlet3AsyncHttpRequest(final HttpServletRequest httpServletRequest, final HttpServletResponse response, final ServletContext servletContext, final HttpResponse httpResponse, final ResteasyHttpHeaders httpHeaders, final ResteasyUriInfo uriInfo, final String s, final SynchronousDispatcher synchronousDispatcher)
   {
      super(httpServletRequest, response, servletContext, httpResponse, httpHeaders, uriInfo, s, synchronousDispatcher);
      this.response = response;
      asynchronousContext = new Servlet3ExecutionContext((ServletRequest) httpServletRequest);
   }

   @Override
   public ResteasyAsynchronousContext getAsyncContext()
   {
      return asynchronousContext;
   }

   private class Servlet3ExecutionContext extends AbstractExecutionContext
   {
      protected final ServletRequest servletRequest;
      protected volatile boolean done;
      protected volatile boolean cancelled;
      protected volatile boolean wasSuspended;
      protected Servlet3AsynchronousResponse asynchronousResponse;

      Servlet3ExecutionContext(final ServletRequest servletRequest)
      {
         super(Servlet3AsyncHttpRequest.this.dispatcher, Servlet3AsyncHttpRequest.this, Servlet3AsyncHttpRequest.this.httpResponse);
         this.servletRequest = servletRequest;
      }

      private class Servlet3AsynchronousResponse extends AbstractAsynchronousResponse implements AsyncListener, AutoCloseable
      {
         private final ScheduledExecutorService asyncScheduler;
         private final Object responseLock = new Object();
         protected ScheduledFuture<?> timeoutFuture; // this is to get around TCK tests that call setTimeout in a separate thread which is illegal.

         private Servlet3AsynchronousResponse()
         {
            super(Servlet3ExecutionContext.this.dispatcher, Servlet3ExecutionContext.this.request, Servlet3ExecutionContext.this.response);
            asyncScheduler = ContextualExecutors.scheduledThreadPool();
         }

         @Override
         public boolean resume(Object entity)
         {
            synchronized (responseLock)
            {
               if (done) return false;
               if (cancelled) return false;
               AsyncContext asyncContext = getAsyncContext();
               done = true;
               return internalResume(entity, t -> {
                  try {
                     asyncContext.complete();
                  } finally {
                     close();
                  }
               });
            }

         }

         @Override
         public void complete()
         {
            synchronized (responseLock)
            {
               if (done) return;
               if (cancelled) return;
               try {
                  AsyncContext asyncContext = getAsyncContext();
                  done = true;
                  asyncContext.complete();
               } finally {
                  close();
               }
            }

         }

         @Override
         public boolean resume(Throwable exc)
         {
            synchronized (responseLock)
            {
               if (done) return false;
               if (cancelled) return false;
               AsyncContext asyncContext = getAsyncContext();
               done = true;
               return internalResume(exc, t -> {
                  try {
                     asyncContext.complete();
                  } finally {
                     close();
                  }
               });
            }
         }

         @Override
         public void initialRequestThreadFinished()
         {
            // done
         }

         @Override
         public boolean setTimeout(long time, TimeUnit unit) throws IllegalStateException
         {
            //getAsyncContext().setTimeout(-1);
            synchronized (responseLock)
            {
               if (done || cancelled)
                  return false;

               // this is to get around TCK tests that call setTimeout in a separate thread which is illegal.
               if (timeoutFuture != null && !timeoutFuture.cancel(false))
               {
                  return false;
               }
               if (time <= 0) return true;
               Runnable task = new Runnable()
               {
                  @Override
                  public void run()
                  {
                     LogMessages.LOGGER.debug(Messages.MESSAGES.scheduledTimeout());
                     handleTimeout();
                  }
               };
               LogMessages.LOGGER.debug(Messages.MESSAGES.schedulingTimeout());
               timeoutFuture = asyncScheduler.schedule(task, time, unit);
            }
            return true;
         }

         @Override
         public boolean cancel()
         {
            LogMessages.LOGGER.debug(Messages.MESSAGES.cancel());
            synchronized (responseLock)
            {
               if (cancelled) {
                  LogMessages.LOGGER.debug(Messages.MESSAGES.alreadyCanceled());
                  return true;
               }
               if (done) {
                  LogMessages.LOGGER.debug(Messages.MESSAGES.alreadyDone());
                  return false;
               }
               done = true;
               cancelled = true;
               AsyncContext asyncContext = getAsyncContext();
               LogMessages.LOGGER.debug(Messages.MESSAGES.cancellingWith503());
               return internalResume(Response.status(Response.Status.SERVICE_UNAVAILABLE).build(), t -> {
                  try {
                     asyncContext.complete();
                  } finally {
                     close();
                  }
               });
            }
         }

         @Override
         public boolean cancel(int retryAfter)
         {
            synchronized (responseLock)
            {
               if (cancelled) return true;
               if (done) return false;
               done = true;
               cancelled = true;
               AsyncContext asyncContext = getAsyncContext();
               return internalResume(Response.status(Response.Status.SERVICE_UNAVAILABLE).header(HttpHeaders.RETRY_AFTER, retryAfter).build(),
                  t -> {
                  try {
                     asyncContext.complete();
                  } finally {
                     close();
                  }
                  });
            }
         }

         @Override
         public boolean cancel(Date retryAfter)
         {
            synchronized (responseLock)
            {
               if (cancelled) return true;
               if (done) return false;
               done = true;
               cancelled = true;
               AsyncContext asyncContext = getAsyncContext();
               return internalResume(Response.status(Response.Status.SERVICE_UNAVAILABLE).header(HttpHeaders.RETRY_AFTER, retryAfter).build(),
                  t -> {
                  try {
                     asyncContext.complete();
                  } finally {
                     close();
                  }
                  });
            }
         }


         @Override
         public boolean isCancelled()
         {
            return cancelled;
         }

         @Override
         public boolean isDone()
         {
            return done;
         }

         @Override
         public boolean isSuspended()
         {
            return !done && !cancelled;
         }

         @Override
         public void onComplete(AsyncEvent asyncEvent) throws IOException
         {
            LogMessages.LOGGER.debug(Messages.MESSAGES.onComplete());
            synchronized (responseLock)
            {
               done = true;
               close();
            }
         }

         @Override
         public void onTimeout(AsyncEvent asyncEvent) throws IOException
         {
            LogMessages.LOGGER.debug(Messages.MESSAGES.onTimeout());
            synchronized (responseLock)
            {
               if (done || cancelled) return;

               response.reset();
               handleTimeout();
            }
         }

         protected void handleTimeout()
         {
            if (done) return;
            try {
               if (timeoutHandler != null) {
                  timeoutHandler.handleTimeout(this);
                  return;
               }
               resume(new ServiceUnavailableException());
            } finally {
//               close(); // liberty Change
            }
         }

         @Override
         public void onError(AsyncEvent asyncEvent) throws IOException
         {
            synchronized (responseLock)
            {
               cancelled = true;
               done = true;
               close();
            }
         }

         @Override
         public void onStartAsync(AsyncEvent asyncEvent) throws IOException
         {
         }

         @Override
         public void close() {
            asyncScheduler.shutdown();
         }
      }

      @Override
      public ResteasyAsynchronousResponse getAsyncResponse()
      {
         return asynchronousResponse;
      }

      @Override
      public ResteasyAsynchronousResponse suspend() throws IllegalStateException
      {
         return suspend(-1);
      }

      @Override
      public ResteasyAsynchronousResponse suspend(long millis) throws IllegalStateException
      {
         return suspend(millis, TimeUnit.MILLISECONDS);
      }

      @Override
      public ResteasyAsynchronousResponse suspend(long time, TimeUnit unit) throws IllegalStateException
      {
         AsyncContext asyncContext = setupAsyncContext();
         asyncContext.setTimeout(unit.toMillis(time));
         return asynchronousResponse;
      }

      @Override
      public void complete() {
         if (wasSuspended && asynchronousResponse != null) asynchronousResponse.complete();
      }

      protected AsyncContext setupAsyncContext()
      {
         if (servletRequest.isAsyncStarted())
         {
            throw new IllegalStateException(Messages.MESSAGES.alreadySuspended());
         }
         asynchronousResponse = new Servlet3AsynchronousResponse();
         AsyncContext asyncContext = servletRequest.startAsync();
         asyncContext.addListener(asynchronousResponse);
         wasSuspended = true;
         //set time out to -1 and resteasy will take care of timeout
         asyncContext.setTimeout(-1);
         return asyncContext;
      }


      private AsyncContext getAsyncContext()
      {
         AsyncContext asyncContext = servletRequest.getAsyncContext();
         if (asyncContext == null)
         {
            throw new IllegalStateException(Messages.MESSAGES.requestNotSuspended());
         }
         return asyncContext;
      }

      @Override
      public boolean isSuspended()
      {
         return wasSuspended;
      }

    @Override
    public CompletionStage<Void> executeBlockingIo(RunnableWithException f, boolean hasInterceptors) {
        CompletableFuture<Void> ret = new CompletableFuture<>();
        if(hasInterceptors && isOnIoThread()) {
           ret.completeExceptionally(new RuntimeException("Cannot use blocking IO with interceptors when we're on the IO thread"));
           return ret;
        }
        try {
            f.run();
            ret.complete(null);
        } catch (Exception e) {
            ret.completeExceptionally(e);
        }
        return ret;
    }

    @Override
    public CompletionStage<Void> executeAsyncIo(CompletionStage<Void> f) {
       // check if this CF is already resolved
       CompletableFuture<Void> ret = f.toCompletableFuture();
       // if it's not resolved, we may need to suspend
       if(!ret.isDone() && !isSuspended()) {
           suspend();
           return ret;
       }
       return f;
    }

    private boolean isOnIoThread()
    {
       // Undertow-specific, but servlet has no equivalent
       return Thread.currentThread().getClass().getName().equals("org.xnio.nio.WorkerThread");
    }
   }
}
