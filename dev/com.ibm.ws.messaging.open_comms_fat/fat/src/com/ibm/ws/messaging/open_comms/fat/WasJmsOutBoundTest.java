/* ============================================================================
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 * ============================================================================
 */
package com.ibm.ws.messaging.open_comms.fat;

import static org.junit.Assert.assertNotNull;

import componenttest.topology.impl.LibertyServerFactory;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import org.junit.Test;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.AfterClass;
import org.junit.BeforeClass;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.Iterator;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class Server extends Thread
{
  public AtomicBoolean  cont=new AtomicBoolean(true);
  public AtomicBoolean  ready=new AtomicBoolean(false);
  public long           base;
  protected long        num_connections=0;
  void report(SocketChannel sc,Object ... args)
  {
    Object[] a = new Object[args.length+1];
    a[0] = String.format("GREPMARK %s-%04d %6d {%5d} [S] "
                        ,currentThread().getName()
                        ,currentThread().getId()
                        ,(System.currentTimeMillis()-base)
                        ,(null==sc?0:sc.socket().getPort())
                        );
    int i=1;
    for (Object o:args) a[i++] = o;
    Util.TRACE(a);
  }
  public Server(long b)
  {
    super("SL");
    base = b;
  }
  public void run()
  {
    Util.TRACE_ENTRY();

    final ServerSocketChannel sc;
    Selector selector = null;
    try
    {
      report(null,"Starting server.");
      sc = ServerSocketChannel.open();
      sc.bind(new InetSocketAddress("localhost",10000));
      sc.configureBlocking(false);
      selector = Selector.open();
      sc.register(selector,SelectionKey.OP_ACCEPT);
      report(null,"Notifying client that server is ready.");
      ready.set(true);
      synchronized(ready)
      {
        ready.notifyAll();
      }
      report(null,"Listening.");
    }
    catch (IOException ioe)
    {
      ioe.printStackTrace();
      Util.TRACE_EXIT();
      return;
    }
    while (cont.get())
    {
      try
      {
        // wake every 5 seconds (or when there is a connection) so we can check the overall timeout
        selector.selectedKeys().clear();
        if (0!=selector.select(5000))
        {
          final SocketChannel accepted_socket = sc.accept();
          if (null!=accepted_socket)
          {
            ++num_connections;
//            report(accepted_socket,"Accepted connection");
            // spawn a thread to deal with the connection
            new Thread("SA")
            {
              final SocketChannel conn = accepted_socket;
              @Override
              public void run()
              {
                try
                {
                  ByteBuffer b = ByteBuffer.allocate(100);
//                  report(conn,"reading...");
                  int num_read = conn.read(b);
                  b.flip();
                  String decoded = java.nio.charset.Charset.defaultCharset().decode(b).toString();
                  if (!"Close me now".equals(decoded)) report(conn,"Unexpected "+num_read+" byte(s) read:"+decoded);
                  // now we're ready to close off the conversation
                  b.clear();
                  b.putChar('z');
                  b.flip();
//                  report(conn,"writing ACK to port ");
                  int nw = conn.write(b);
//                  report(conn,"wrote "+nw+" byte(s) for ack");

                  // close off the connection
                  try
                  {
                    conn.close();
//                    report(null,"Connection closed without exception.");
                  }
                  catch (IOException ioe)
                  {
                    report(conn,"Exception on close:",ioe);
                    conn.close();
                    cont.set(false);
                  }
                }
                catch (IOException ioe)
                {
                  report(conn,"Exception:",ioe);
                }
              }
            }.start();
          }
          else
          {
            report(null,"accept() returned null");
          }
        }
        else
        {
          report(null,"Select timedout; cont="+cont.get());
        }
      }
      catch (IOException ioe)
      {
        report(null,"Exception accepting new connection:",ioe);
        cont.set(false);
      }
//        if (30000<=(System.currentTimeMillis()-base))
      if (300000<=(System.currentTimeMillis()-base))
      {
        report(null,"Timing out after 5 minutes.");
        cont.set(false);
      }
    }
    try
    {
      report(null,"Stopping listening.");
      selector.close();
      sc.close();
    }
    catch (IOException ioe)
    {
      ioe.printStackTrace();
    }
    report(null,"Listening stopped. "+num_connections+" connection(s) were made.");
    Util.TRACE_EXIT();
  }
};

class Client extends Thread
{
  protected AtomicInteger counter = new AtomicInteger(0);
  protected AtomicInteger count_is_closed = new AtomicInteger(0);
  protected AtomicInteger count_not_eos = new AtomicInteger(0);
  protected AtomicInteger count_eos = new AtomicInteger(0);
  protected AtomicInteger count_exception = new AtomicInteger(0);
  protected Server  server;

  void report(SocketChannel sc,Object ... args)
  {
    Object[] a = new Object[args.length+1];
    a[0] = String.format("GREPMARK %s-%04d %6d {%5d} [C] "
                        ,currentThread().getName()
                        ,currentThread().getId()
                        ,(System.currentTimeMillis()-server.base)
                        ,(null==sc?0:sc.socket().getLocalPort())
                        );
    int i=1;
    for (Object o:args) a[i++] = o;
    Util.TRACE(a);
  }
  public Client(Server s)
  {
    super("CM");
    server = s;
  }
  public void run()
  {
    Util.TRACE_ENTRY();
    synchronized(server.ready)
    {
      try
      {
        while (!server.ready.get()) server.ready.wait(1000);
      }
      catch (InterruptedException ie)
      {
        report(null,"Interrupted.");
        Util.TRACE_EXIT();
        return;
      }
    }
    report(null,"Server is ready, starting connection loop.");
    while (server.cont.get())
    {
      while (50<=counter.get())
      {
        try { Thread.sleep(1000); } catch (InterruptedException ie) { }
      }
      counter.incrementAndGet();
      new Thread("CL")
      {
        protected ByteBuffer  b1,b2;
        @Override
        public void run()
        {
          b1 = java.nio.charset.Charset.defaultCharset().encode("Close me now");
          b2 = java.nio.charset.Charset.forName("UTF-8").encode("A 17 char string.");
          try
          {
            SocketChannel sc = SocketChannel.open();
            sc.connect(new InetSocketAddress("localhost",10000));

            // register it with the selector before it is closed off
            sc.configureBlocking(false);
            Selector selector = Selector.open();
            sc.register(selector,SelectionKey.OP_READ);

            int num_written;
            int num_ready;

            for (int count=0;2>count;++count)
            {
              b1.rewind();
              num_written = sc.write(b1);
//              report(sc,num_written+" byte(s) written to socket");

//              report(sc,"selecting...");
              num_ready = selector.select(10000);
              if (0!=num_ready)
              {
                ByteBuffer b3 = ByteBuffer.allocate(10);
                int num_read = sc.read(b3);
                if (2!=num_read) report(sc,num_read+" byte(s) read from socket for ACK");
                if (-1==num_read)
                {
                  report(sc,"Exiting on EOS");
                  return;
                }
              }
              else if (0==count)
              {
                report(sc,"No ACK; retrying send");
                continue;
              }
              else
              {
                report(sc,"No ACK; aborting");
                return;
              }
              break;
            }

            // write again after it is closed
            b2.rewind();
            num_written = sc.write(b2);
//            report(sc,num_written+" additional byte(s) written to socket");

            boolean done = false;
            selector.selectedKeys().clear();
            do
            {
              num_ready = selector.select(5000);
              if (0==num_ready)
              {
                report(sc,"select timed out.");
              }
            } while (0==num_ready&&server.cont.get());
            if (0!=num_ready)
            {
              Set<SelectionKey> keys = selector.selectedKeys();
              Iterator<SelectionKey> it = keys.iterator();
              if (it.hasNext())
              {
                SelectionKey sk = it.next();
                SocketChannel rc = (SocketChannel)sk.channel();
                if (rc.socket().isClosed())
                {
                  report(rc,"Ready channel is closed");
                  count_is_closed.incrementAndGet();
                }
                else // if (sk.isReadable())
                {
                  try
                  {
                    ByteBuffer b3 = ByteBuffer.allocate(100);
                    int num_read = rc.read(b3);
                    //report(num_read+" byte(s) read from socket.");
                    if (-1!=num_read)
                    {
                      report(rc,"Expected EOS. num_read="+num_read
                                +(0<num_read?",buffer[0]="+b3.get(0):"")
                                +(1<num_read?",buffer[1]="+b3.get(1):"")
                                );
                      count_not_eos.incrementAndGet();
                    }
                    else
                    {
                      count_eos.incrementAndGet();
                    }
                  }
                  catch (IOException ioe)
                  {
                    if (!"Connection reset".equals(ioe.getMessage()))
                    {
                      report(rc,"Exception when reading: ",ioe.getMessage());
                    }
                    count_exception.incrementAndGet();
                  }
                }
              }
              keys.clear();
            }
            selector.close();
            sc.close();
          }
          catch (IOException ioe)
          {
            report(null,"Exception: ",ioe);
          }
          finally
          {
            counter.decrementAndGet();
          }
        }
      }.start();
    }
    server.cont.set(false);    // make sure the server gives up when there won't be a client trying to connect
    report(null,"Client main loop ended; server.cont="+server.cont.get());
    try { Thread.sleep(1000); } catch (InterruptedException e) {}
    report(null,"counter............ "+counter.get());
    report(null,"count_is_closed.... "+count_is_closed.get());
    report(null,"count_not_eos...... "+count_not_eos.get());
    report(null,"count_eos.......... "+count_eos.get());
    report(null,"count_exception.... "+count_exception.get());
    Util.TRACE_EXIT();
  }
};

@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class WasJmsOutBoundTest extends FATBase {
  static {
    server_ = LibertyServerFactory.getLibertyServer("com.ibm.ws.messaging.open_comms.WJOServer");
    client_ = LibertyServerFactory.getLibertyServer("com.ibm.ws.messaging.open_comms.WJOClient");
  }

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    Util.TRACE_ENTRY();
    setup();
    Util.TRACE_EXIT();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    Util.TRACE_ENTRY();
    cleanup();
    Util.TRACE_EXIT();
  }

  @Mode(TestMode.LITE)
  @Test
  public void testWJOSendReceive2LP() throws Exception {
    Util.TRACE_ENTRY();
    runTest(client_,"CommsLP","testQueueSendMessage");
    Util.CODEPATH();
    runTest(client_,"CommsLP","testQueueReceiveMessages");

    Util.CODEPATH();
    String msg = client_.waitForStringInLog("Queue Message", client_.getMatchingLogFile("messages.log"));
    assertNotNull("Could not find the queue message in the message log", msg);
    Util.TRACE_EXIT();
  }

  @Mode(TestMode.LITE)
  @Test
  public void testSocketCloseOnzOS() throws Exception
  {
    Util.TRACE_ENTRY();
    cleanup();
    long        base = System.currentTimeMillis();
    Server      s = new Server(base);
    Client      c = new Client(s);

    Util.TRACE("GREPMARK platform: "+System.getProperty("os.name","<unknown>"));

    Util.TRACE("GREPMARK Starting threads...");
    s.start();
    c.start();
    Util.TRACE("GREPMARK Waiting for threads...");
    s.join();
    c.join();
    Util.TRACE_EXIT();
//    assertNotNull("Forced failure",null);
  }
}
