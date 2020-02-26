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

class Server extends Thread
{
  public AtomicBoolean  cont=new AtomicBoolean(true);
  public AtomicBoolean  ready=new AtomicBoolean(false);
  public AtomicBoolean  client_closed=new AtomicBoolean(false);
  public long     base;
  protected long  num_connections=0;
  void report(String s)
  {
    Util.TRACE(String.format("GREPMARK %5d [S] %s",(System.currentTimeMillis()-base),s));
  }
  public Server(long b)
  {
    base = b;
  }
  public void run() 
  {
    Util.TRACE_ENTRY();

    ServerSocketChannel sc = null;
    Selector selector = null;
    SelectionKey key = null;
    try
    {
      report("Starting server.");
      sc = ServerSocketChannel.open();
      sc.bind(new InetSocketAddress("localhost",10000));
      sc.configureBlocking(false);
      selector = Selector.open();
      key = sc.register(selector,SelectionKey.OP_ACCEPT);
      report("Notifying client that server is ready.");
      ready.set(true);
      synchronized(ready)
      {
        ready.notifyAll();
      }
      report("Listening.");
    }
    catch (IOException ioe)
    {
      ioe.printStackTrace();
    }
    if (null!=sc)
    {
      while (cont.get())
      {
        try
        {
          // wake every 5 seconds (or when there is a connection) so we can check the overall timeout
          //report("selector keys:"+selector.keys().size()+", interestOps="+key.interestOps());
          selector.selectedKeys().clear();
          if (0!=selector.select(5000))
          {
            SocketChannel conn = sc.accept();
            if (null!=conn)
            {
              ++num_connections;
              //report("A connection has been made. Sleeping 100ms then closing connection before write.");
              //try { Thread.sleep(100); } catch (InterruptedException ie) {}
              //report("Closing connection.");
              try
              {
                conn.close();
                //report("Connection closed without exception.");
              }
              catch (IOException ioe)
              {
                report("Exception on close:"+ioe);
                conn.close();
                cont.set(false);
              }
              client_closed.set(true);
              synchronized(client_closed)
              {
                client_closed.notifyAll();
              }
              //report("set client_closed and notified all.");
            }
          }
          else
          {
            report("Select timedout; cont="+cont.get());
          }
        }
        catch (IOException ioe)
        {
          ioe.printStackTrace();
          client_closed.set(true);
          synchronized(client_closed)
          {
            client_closed.notifyAll();
          }
          report("set client_closed and notified all.");
        }
        if (300000<=(System.currentTimeMillis()-base))
        {
          report("Timing out after 5 minutes.");
          cont.set(false);
        }
      }
      try
      {
        report("Stopping listening.");
        selector.close();
        sc.close();
      }
      catch (IOException ioe)
      {
        ioe.printStackTrace();
      }
      report("Listening stopped. "+num_connections+" connection(s) were made.");
    }
    Util.TRACE_EXIT();
  }
};

class Client extends Thread
{
  protected Server  server;

  void report(String s)
  {
    Util.TRACE(String.format("GREPMARK %5d [C] %s",(System.currentTimeMillis()-server.base),s));
  }
  public Client(Server s)
  {
    server = s;
  }
  public void run()
  {
    Util.TRACE_ENTRY();
    try
    {
      synchronized(server.ready)
      {
        try
        {
          while (!server.ready.get()) server.ready.wait(1000);
        }
        catch (InterruptedException ie)
        {
          report("Interrupted.");
          Util.TRACE_EXIT();
          return;
        }
      }
      report("Server is ready, starting connection loop.");
      while (server.cont.get())
      {
        //report("Connecting to server.");
        SocketChannel sc = SocketChannel.open();
        server.client_closed.set(false);
        sc.connect(new InetSocketAddress("localhost",10000));
        synchronized(server.client_closed)
        {
          while (!server.client_closed.get())
          {
            try
            {
              server.client_closed.wait(1000);
            }
            catch (InterruptedException ie)
            {
              report("Wait interrupted.");
            }
            if (!server.cont.get()) break;
          }
        }
        if (!server.cont.get()) break;
        //report("Connected.");
        //report("Connected. Waiting 200ms before writing to what should be a closed socket.");
        //try { Thread.sleep(200); } catch (InterruptedException ie) {}
        ByteBuffer b = ByteBuffer.allocate(17);
        int num_written = sc.write(b);
        //report(num_written+" byte(s) written to socket.");
        sc.configureBlocking(false);
        Selector selector = Selector.open();
        SelectionKey key = sc.register(selector,SelectionKey.OP_READ);
        boolean done = false;
        while (!done&&server.cont.get())
        {
          //report("Entering select with 5s timeout.");
          selector.selectedKeys().clear();
          int num_ready = selector.select(5000);
          //report("Select returned "+num_ready+" ready socket channel(s).");
          if (0==num_ready) break;
          Set<SelectionKey> keys = selector.selectedKeys();
          Iterator<SelectionKey> it = keys.iterator();
          if (it.hasNext())
          {
            SelectionKey sk = it.next();
            SocketChannel rc = (SocketChannel)sk.channel();
            if (rc.socket().isClosed())
            {
              report("Ready channel is closed.");
            }
            else // if (sk.isReadable())
            {
              try
              {
                int num_read = rc.read(b);
                //report(num_read+" byte(s) read from socket.");
                if (0!=num_read) report("Read did not return EOF");
                if (0>=num_read) { done=true; break; }
              }
              catch (IOException ioe)
              {
                report("Exception when reading: "+ioe.getMessage());
                break;
              }
            }
          }
          keys.clear();
        }
        //report("Client closing socket.");
        selector.close();
        sc.close();
        //report("Client socket closed.");
      }
      report("Client main loop ended; server.cont="+server.cont.get());
    }
    catch (IOException ioe)
    {
      report("Client encountered exception:"+ioe);
      ioe.printStackTrace();
    }
    report("Client finished.");
    server.cont.set(false);    // make sure the server gives up when there won't be a client trying to connect
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
    assertNotNull("Forced failure",null);
  }
}
