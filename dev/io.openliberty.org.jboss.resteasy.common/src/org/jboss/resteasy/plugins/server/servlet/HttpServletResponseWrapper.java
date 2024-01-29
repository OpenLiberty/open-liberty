package org.jboss.resteasy.plugins.server.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.core.ResteasyContext.CloseableContext;
import org.jboss.resteasy.spi.AsyncOutputStream;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class HttpServletResponseWrapper implements HttpResponse
{
   public abstract class AsyncOperation
   {
      CompletableFuture<Void> future = new CompletableFuture<>();
      OutputStream stream;
      public AsyncOperation(final OutputStream stream)
      {
         this.stream = stream;
      }
      public void work(ServletOutputStream sos) {
         try(CloseableContext c = ResteasyContext.addCloseableContextDataLevel(contextDataMap)){
            doWork(sos);
         }
      }
      protected abstract void doWork(ServletOutputStream sos);
   }

   public class WriteOperation extends AsyncOperation
   {

      private byte[] bytes;
      private int offset;
      private int length;

      public WriteOperation(final OutputStream stream, final byte[] bytes, final int offset, final int length)
      {
         super(stream);
         this.bytes = bytes;
         this.offset = offset;
         this.length = length;
      }

      @Override
      protected void doWork(ServletOutputStream sos)
      {
         try
         {
            stream.write(bytes, offset, length);
            // we only are complete if isReady says we're good to write, otherwise
            // we will be complete in the next onWritePossible or onError
            if(sos == null || sos.isReady()) {
               future.complete(null);
            }
         } catch (IOException e)
         {
            future.completeExceptionally(e);
         }
      }

      @Override
      public String toString()
      {
         return "[write: "+new String(bytes)+"]";
      }
   }

   public class FlushOperation extends AsyncOperation
   {

      public FlushOperation(final OutputStream os)
      {
         super(os);
      }

      @Override
      protected void doWork(ServletOutputStream sos)
      {
         try
         {
            stream.flush();
            // we only are complete if isReady says we're good to write, otherwise
            // we will be complete in the next onWritePossible or onError
            if(sos == null || sos.isReady()) {
               future.complete(null);
            }
         } catch (IOException e)
         {
            future.completeExceptionally(e);
         }
      }

      @Override
      public String toString()
      {
         return "[flush]";
      }
   }

   protected HttpServletResponse response;
   protected int status = 200;
   protected MultivaluedMap<String, Object> outputHeaders;
   protected ResteasyProviderFactory factory;
   protected OutputStream outputStream = new DeferredOutputStream();
   protected volatile boolean suppressExceptionDuringChunkedTransfer = true;
   protected HttpServletRequest request;
   protected Map<Class<?>, Object> contextDataMap;

   // RESTEASY-1784
   @Override
   public void setSuppressExceptionDuringChunkedTransfer(boolean suppressExceptionDuringChunkedTransfer) {
      this.suppressExceptionDuringChunkedTransfer = suppressExceptionDuringChunkedTransfer;
   }

   @Override
   public boolean suppressExceptionDuringChunkedTransfer() {
      return suppressExceptionDuringChunkedTransfer;
   }

   /**
    * RESTEASY-684 wants to defer access to outputstream until a write happens
    *
    */
   protected class DeferredOutputStream extends AsyncOutputStream implements WriteListener
   {
      private boolean asyncRegistered;
      private Queue<AsyncOperation> asyncQueue;
      private AsyncOperation lastAsyncOperation;
      private boolean asyncListenerCalled;

      @Override
      public void write(int i) throws IOException
      {
         response.getOutputStream().write(i);
      }

      @Override
      public void write(byte[] bytes) throws IOException
      {
         response.getOutputStream().write(bytes);
      }

      @Override
      public void write(byte[] bytes, int i, int i1) throws IOException
      {
         response.getOutputStream().write(bytes, i, i1);
      }

      @Override
      public void flush() throws IOException
      {
         response.getOutputStream().flush();
      }

      @Override
      public void close() throws IOException
      {
         //NOOP (RESTEASY-1650)
      }

      @Override
      public CompletionStage<Void> asyncFlush()
      {
         AsyncOperation op = new FlushOperation(this);
         queue(op);
         return op.future;
      }

      @Override
      public CompletionStage<Void> asyncWrite(byte[] bytes, int offset, int length)
      {
         AsyncOperation op = new WriteOperation(this, bytes, offset, length);
         queue(op);
         return op.future;
      }

      private void queue(AsyncOperation op)
      {
         // fetch it from the context directly to avoid having to restore the context just in case we're invoked on a context-less thread
         HttpRequest resteasyRequest = (HttpRequest) contextDataMap.get(HttpRequest.class);
         if(request.isAsyncStarted() && !resteasyRequest.getAsyncContext().isOnInitialRequest()) {
            synchronized(this) {
               ServletOutputStream os;
               try
               {
                  os = response.getOutputStream();
               } catch (IOException e)
               {
                  // return a failed future, do not queue it
                  op.future.completeExceptionally(e);
                  return;
               }
               if(!asyncRegistered) {
                  // start the queue
                  asyncRegistered = true;
                  // make sure we have something ready to be executed
                  addToQueue(op);
                  os.setWriteListener(this);
                  // never call isReady before Undertow is ready and has already called our listener at least once
               } else if(asyncListenerCalled && os.isReady()) {
                  // it's possible that we startAsync and queue, then queue another event and the stream becomes ready before
                  // onWritePossible is called, which means we need to flush the queue here to guarantee ordering if that happens
                  addToQueue(op);
                  flushQueue(os);
               } else {
                  // just queue
                  addToQueue(op);
               }
            }
         } else {
            op.work(null);
         }
      }

      private void flushQueue(ServletOutputStream sos)
      {
         if(lastAsyncOperation != null) {
            lastAsyncOperation.future.complete(null);
            lastAsyncOperation = null;
         }

         while(!asyncQueue.isEmpty() && sos.isReady()) {
            lastAsyncOperation = asyncQueue.poll();
            lastAsyncOperation.work(sos);
         }
      }

      private synchronized void addToQueue(AsyncOperation op)
      {
         if(asyncQueue == null) {
            asyncQueue = new ConcurrentLinkedQueue<>();
         }
         asyncQueue.add(op);
      }

      @Override
      public synchronized void onWritePossible() throws IOException
      {
         asyncListenerCalled = true;
         flushQueue(response.getOutputStream());
      }

      @Override
      public synchronized void onError(Throwable t)
      {
         asyncListenerCalled = true;
         if(lastAsyncOperation != null) {
            lastAsyncOperation.future.completeExceptionally(t);
            lastAsyncOperation = null;
         }
         while(!asyncQueue.isEmpty()) {
            AsyncOperation op = asyncQueue.poll();
            if(!op.future.isDone())
               op.future.completeExceptionally(t);
         }
      }
   }

   public HttpServletResponseWrapper(final HttpServletResponse response, final HttpServletRequest request, final ResteasyProviderFactory factory)
   {
      this.response = response;
      this.request = request;
      outputHeaders = new HttpServletResponseHeaders(response, factory);
      this.factory = factory;
      this.contextDataMap = ResteasyContext.getContextDataMap();
   }

   public int getStatus()
   {
      return status;
   }

   public void setStatus(int status)
   {
      this.status = status;
      this.response.setStatus(status);
   }

   public MultivaluedMap<String, Object> getOutputHeaders()
   {
      return outputHeaders;
   }

   public OutputStream getOutputStream() throws IOException
   {
      return outputStream;
   }

   @Override
   public void setOutputStream(OutputStream os)
   {
      this.outputStream = os;
   }

   public void addNewCookie(NewCookie cookie)
   {
      outputHeaders.add(javax.ws.rs.core.HttpHeaders.SET_COOKIE, cookie);
   }

   public void sendError(int status) throws IOException
   {
      response.sendError(status);
   }

   public void sendError(int status, String message) throws IOException
   {
      response.sendError(status, message);
   }

   public boolean isCommitted()
   {
      return response.isCommitted();
   }

   public void reset()
   {
      response.reset();
      outputHeaders = new HttpServletResponseHeaders(response, factory);
   }

   @Override
   public void flushBuffer() throws IOException
   {
      response.flushBuffer();
   }
}
