/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.fat.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.ibm.websphere.simplicity.log.Log;

public class HttpProxy extends Thread {
    public static int proxyPort = 0;
    static public int CONNECT_RETRIES = 5;
    static public int CONNECT_PAUSE = 5;
    static public int TIMEOUT = 50;
    static public int SERVER_LISTEN_TIMEOUT = 3000;
    static public int BUFSIZ = 1024;
    public static boolean runFlag = false;
    private static Boolean stopped = true;
    private static final String proxyHandled = "_proxyHandled";

    static public boolean logging = false;
    static public OutputStream log = System.out;
    protected Socket socket;

    static private String parent = null;
    static private int parentPort = -1;

    static public void setParentProxy(String name, int pport) {
        parent = name;
        parentPort = pport;
    }

    public HttpProxy(Socket s) {
        socket = s;
        start();
    }

    public void writeLog(int c, boolean browser) throws IOException {
        log.write(c);
    }

    public void writeLog(byte[] bytes, int offset, int len, boolean browser) throws IOException {
        for (int i = 0; i < len; i++)
            writeLog(bytes[offset + i], browser);
    }

    public String processMessage(String url, String host, int port, Socket sock) {
        if (url != null) {
            url = url + proxyHandled;
        }
        java.text.DateFormat cal = java.text.DateFormat.getDateTimeInstance();
        Log.info(HttpProxy.class, "processMessage", cal.format(new java.util.Date()) + " - " + url + " "
                                                     + sock.getInetAddress());
        return url;
    }

    @Override
    public void run() {
        String line;
        String host;
        int port = 80;
        Socket outbound = null;
        Log.info(HttpProxy.class, "run", "Proxy thread is entering!");
        try {
            socket.setSoTimeout(TIMEOUT);

            InputStream is = socket.getInputStream();
            OutputStream os = null;
            try {

                line = "";
                host = "";
                int state = 0;
                boolean space;
                while (true) {
                    int c = is.read();
                    if (c == -1)
                        break;
                    if (logging)
                        writeLog(c, true);
                    space = Character.isWhitespace((char) c);
                    switch (state) {
                        case 0:
                            if (space)
                                continue;
                            state = 1;
                        case 1:
                            if (space) {
                                state = 2;
                                continue;
                            }
                            line = line + (char) c;
                            break;
                        case 2:
                            if (space)
                                continue;
                            state = 3;
                        case 3:
                            if (space) {
                                state = 4;

                                String host0 = host;
                                int n;
                                n = host.indexOf("//");
                                if (n != -1)
                                    host = host.substring(n + 2);
                                n = host.indexOf('/');
                                if (n != -1)
                                    host = host.substring(0, n);

                                n = host.indexOf(":");
                                if (n != -1) {
                                    port = Integer.parseInt(host.substring(n + 1));
                                    host = host.substring(0, n);
                                }
                                host = host.replaceAll("1.1.1.1", "127.0.0.1");

                                if (parent != null) {
                                    host = parent;
                                    port = parentPort;
                                }

                                Log.info(HttpProxy.class, "run", "Prepare to connect to host: " + host + ", url: " + host0);
                                int retry = CONNECT_RETRIES;
                                while (retry-- != 0) {
                                    try {
                                        outbound = new Socket(host, port);
                                        Log.info(HttpProxy.class, "run", "Connected to the target: " + host);
                                        break;
                                    } catch (Exception e) {
                                    }

                                    Thread.sleep(CONNECT_PAUSE);
                                }
                                if (outbound == null)
                                    break;
                                outbound.setSoTimeout(TIMEOUT);
                                os = outbound.getOutputStream();
                                os.write(line.getBytes());
                                os.write(' ');
                                os.write(host0.getBytes());
                                os.write(' ');
                                pipe(is, outbound.getInputStream(), os, socket.getOutputStream());
                                break;
                            }
                            host = host + (char) c;
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            try {
                outbound.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        Log.info(HttpProxy.class, "run", "Proxy thread exited!");
    }

    void pipe(InputStream is0, InputStream is1,
              OutputStream os0, OutputStream os1) throws IOException {
        Log.info(HttpProxy.class, "pipe", "Pipe start.");
        try {
            int ir;
            byte bytes[] = new byte[BUFSIZ];
            boolean flag1 = false;
            boolean flag2 = false;
            while (!flag1 || !flag2) {
                try {
                    if ((ir = is0.read(bytes)) > 0) {
                        os0.write(bytes, 0, ir);
                        if (logging)
                            writeLog(bytes, 0, ir, true);
                    } else if (ir < 0) {
                        if (!flag1)
                            Log.info(HttpProxy.class, "pipe", "Read in end.");
                        flag1 = true;
                    }

                } catch (InterruptedIOException e) {
                }
                try {
                    if ((ir = is1.read(bytes)) > 0) {
                        os1.write(bytes, 0, ir);
                        if (logging)
                            writeLog(bytes, 0, ir, false);
                    } else if (ir < 0) {
                        if (!flag2)
                            Log.info(HttpProxy.class, "pipe", "Read out end.");
                        flag2 = true;
                    }

                } catch (InterruptedIOException e) {
                }
            }
        } catch (Exception e0) {
            Log.info(HttpProxy.class, "pipe", "Pipe error: " + e0.toString() + ".");
        }
        Log.info(HttpProxy.class, "pipe", "Pipe end.");
    }

    static public void runProxy(int port, Class clobj) {
        stopped = false;
        ServerSocket ssock = null;
        Socket sock;
        try {
            ssock = new ServerSocket(port);
            ssock.setSoTimeout(SERVER_LISTEN_TIMEOUT);
            while (runFlag) {
                //Log.info(HttpProxy.class, "runProxy", "Running...");
                Class[] sarg = new Class[1];
                Object[] arg = new Object[1];
                sarg[0] = Socket.class;
                try {
                    java.lang.reflect.Constructor cons = clobj.getDeclaredConstructor(sarg);
                    arg[0] = ssock.accept();
                    cons.newInstance(arg);
                } catch (Exception e) {
                    Socket esock = (Socket) arg[0];
                    if (esock != null) {
                        try {
                            esock.close();
                        } catch (Exception ec) {
                        }
                    }

                }
            }
        } catch (IOException e) {
        }

        if (ssock != null) {
            try {
                ssock.close();

            } catch (IOException e) {

                e.printStackTrace();
            }
        }

        stopped = true;

        Log.info(HttpProxy.class, "runProxy", "Stopped.");
    }

    protected static void startProxy(int port) {
        if (stopped) {
            HttpProxy.log = System.out;
            HttpProxy.logging = false;
            runFlag = true;
            proxyPort = port;
            Log.info(HttpProxy.class, "startProxy", "Starting proxy on port:" + proxyPort + ".");
            HttpProxy.runProxy(proxyPort, HttpProxy.class);
        } else {
            Log.info(HttpProxy.class, "startProxy", "Proxy server is running on port:" + proxyPort + ".");
        }

    }

    protected static boolean stopProxy(int p) {
        if (proxyPort == p && !stopped) {
            Log.info(HttpProxy.class, "stopProxy", "Stopping proxy on port:" + proxyPort + ".");
            runFlag = false;

            while (!stopped) {
                try {
                    Thread.sleep(SERVER_LISTEN_TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Log.info(HttpProxy.class, "stopProxy", "isStopped:" + stopped);
            }

            Log.info(HttpProxy.class, "stopProxy", "Stopped proxy server.");
            return true;
        } else {
            Log.info(HttpProxy.class, "stopProxy", "Port not equals or proxy server stopped.");
            return false;
        }

    }

    static public void main(String args[]) {
        HttpProxy.log = System.out;
        HttpProxy.logging = false;
        runFlag = true;
        proxyPort = 808;
        HttpProxy.runProxy(proxyPort, HttpProxy.class);
    }
}
