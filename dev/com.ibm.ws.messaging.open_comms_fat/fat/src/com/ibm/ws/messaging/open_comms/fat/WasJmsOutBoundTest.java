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

class Base extends Thread
{
  long    base;
  String  id;

  public Base(long b,String i) 
  {
    base = b; 
    id = i;
  }
  void report(String s)
  {
    Util.TRACE(String.format("GREPMARK %5d [%s] %s",(System.currentTimeMillis()-base),id,s));
  }
}
class Server extends Base
{
  public Server(long b)
  {
    super(b,"S");
  }
  public void run() 
  {
    Util.TRACE_ENTRY();
    try
    {
      report("Starting server.");
      ServerSocketChannel sc = ServerSocketChannel.open();
      sc.bind(new InetSocketAddress("localhost",10000));
      report("Listening.");
      SocketChannel conn = sc.accept(); // blocking
      report("A connection has been made.");
      try { Thread.sleep(500); } catch (InterruptedException ie) {}
      report("Stopping listening.");
      sc.close();
      report("Listening stopped.");
      try { Thread.sleep(100); } catch (InterruptedException ie) {}
      report("Closing connection.");
      conn.close();
      report("Connection closed.");
    }
    catch (IOException ioe)
    {
      ioe.printStackTrace();
    }
    Util.TRACE_EXIT();
  }
};

class Client extends Base
{
  public Client(long b)
  {
    super(b,"C");
  }
  public void run()
  {
    Util.TRACE_ENTRY();
    try
    {
      try { Thread.sleep(100); } catch (InterruptedException ie) {}
      report("Connecting to server.");
      SocketChannel sc = SocketChannel.open();
      sc.connect(new InetSocketAddress("localhost",10000));
      report("Connected.");
      ByteBuffer b = ByteBuffer.allocate(17);
      int num_written = sc.write(b);
      report(num_written+" byte(s) written to socket.");
      sc.configureBlocking(false);
      Selector selector = Selector.open();
      SelectionKey key = sc.register(selector,SelectionKey.OP_READ);
      boolean done = false;
      while (!done)
      {
        report("Entering select.");
        int num_ready = selector.select(5000);
        report("Select returned "+num_ready+" ready socket channel(s).");
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
              report(num_read+" byte(s) read from socket.");
              if (-1==num_read) { done=true; break; }
            }
            catch (IOException ioe)
            {
              report("Exception when reading: "+ioe.getMessage());
              break;
            }
          }
        }
      }
      report("Client terminating.");
      sc.close();
      report("Client terminated.");
    }
    catch (IOException ioe)
    {
      ioe.printStackTrace();
    }
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

  @Test
  public void testSocketCloseOnzOS() throws Exception
  {
    Util.TRACE_ENTRY();
    long        base = System.currentTimeMillis();
    Server      s = new Server(base);
    Client      c = new Client(base);

    Util.TRACE("GREPMARK platform: "+System.getProperty("os.name","<unknown>"));

    Util.TRACE("GREPMARK Starting threads...");
    s.start();
    c.start();
    Util.TRACE("GREPMARK Waiting for threads...");
    s.join();
    c.join();
    Util.TRACE_EXIT();
    if ("zos".equalsIgnoreCase(System.getProperty("os.name",""))) assertNotNull("Forced failure",null);
  }
}
