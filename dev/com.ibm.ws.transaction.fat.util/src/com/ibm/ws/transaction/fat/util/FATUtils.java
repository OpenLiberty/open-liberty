package com.ibm.ws.transaction.fat.util;

import static org.junit.Assert.assertNotNull;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public class FATUtils {
	
	private static final Class<FATUtils> c = FATUtils.class;

    public static void startServers(LibertyServer... servers) throws Exception {
        final String method = "startServers";

        for (LibertyServer server : servers) {
            assertNotNull("Attempted to start a null server", server);
            int attempt = 0;
            int maxAttempts = 5;
            
            Log.info(c, method, "Starting " + server.getServerName());

            int status = server.resetStarted();

            Log.info(c, method, "ResetStarted returned " + status);

            do {
                if (attempt++ > 0) {
                    Log.info(c, method, "Waiting 5 seconds after start failure before making attempt " + attempt);
                    try {
                        Thread.sleep(5000);
                    } catch (Exception e) {
                        Log.error(c, method, e);
                    }
                }

                if (server.isStarted()) {
                    String pid = server.getPid();
                    Log.info(c, method,
                             "Server " + server.getServerName() + " is already running." + ((pid != null ? "(pid:" + pid + ")" : "")) + " Maybe it is on the way down.");
                    server.printProcesses();
                    continue;
                }

                ProgramOutput po = null;
                try {
                    po = server.startServerAndValidate(false, false, true);
                } catch (Exception e) {
                    Log.error(c, method, e, "Server start attempt " + attempt + " failed with return code " + (po != null ? po.getReturnCode() : "<unavailable>"));
                }

                if (po != null) {
                    String s;
                    int rc = po.getReturnCode();

                    Log.info(c, method, "ReturnCode: " + rc);

                    s = server.getPid();

                    if (s != null && !s.isEmpty())
                        Log.info(c, method, "Pid: " + s);

                    s = po.getStdout();

                    if (s != null && !s.isEmpty())
                        Log.info(c, method, "Stdout: " + s.trim());

                    s = po.getStderr();

                    if (s != null && !s.isEmpty())
                        Log.info(c, method, "Stderr: " + s.trim());

                    if (rc == 0) {
                        break;
                    } else {
                        String pid = server.getPid();
                        Log.info(c, method,
                                 "Non zero return code starting server " + server.getServerName() + "." + ((pid != null ? "(pid:" + pid + ")" : ""))
                                                     + " Maybe it is on the way down.");
                        server.printProcessHoldingPort(server.getHttpDefaultPort());
                    }
                }
            } while (attempt < maxAttempts);

            if (!server.isStarted()) {
                server.postStopServerArchive();
                throw new Exception("Failed to start " + server.getServerName() + " after " + attempt + " attempts");
            }
        }
    }

    public static void stopServers(String[] toleratedMsgs, LibertyServer... servers) throws Exception {
        final String method = "stopServers";

        for (LibertyServer server : servers) {
            assertNotNull("Attempted to stop a null server", server);
            int attempt = 0;
            int maxAttempts = 5;

            do {
                if (attempt++ > 0) {
                    Log.info(c, method, "Waiting 5 seconds after stop failure before making attempt " + attempt);
                    try {
                        Thread.sleep(5000);
                    } catch (Exception e) {
                        Log.error(c, method, e);
                    }
                }

                if (!server.isStarted()) {
                    Log.info(c, method,
                             "Server " + server.getServerName() + " is not started. Maybe it is on the way up.");
                    continue;
                }

                ProgramOutput po = null;
                try {
                    po = server.stopServer((String[]) null);
                } catch (Exception e) {
                    Log.error(c, method, e, "Server stop attempt " + attempt + " failed with return code " + (po != null ? po.getReturnCode() : "<unavailable>"));
                }

                if (po != null) {
                    String s;
                    int rc = po.getReturnCode();

                    Log.info(c, method, "ReturnCode: " + rc);

                    s = server.getPid();

                    if (s != null && !s.isEmpty())
                        Log.info(c, method, "Pid: " + s);

                    s = po.getStdout();

                    if (s != null && !s.isEmpty())
                        Log.info(c, method, "Stdout: " + s.trim());

                    s = po.getStderr();

                    if (s != null && !s.isEmpty())
                        Log.info(c, method, "Stderr: " + s.trim());

                    if (rc == 0) {
                        break;
                    } else {
                        String pid = server.getPid();
                        Log.info(c, method,
                                 "Non zero return code stopping server " + server.getServerName() + "." + ((pid != null ? "(pid:" + pid + ")" : "")));
                        server.printProcessHoldingPort(server.getHttpDefaultPort());
                    }
                }
            } while (attempt < maxAttempts);

            if (server.isStarted()) {
                server.postStopServerArchive();
                throw new Exception("Failed to stop " + server.getServerName() + " after " + attempt + " attempts");
            }
        }
    }

	public static void stopServers(LibertyServer s1, LibertyServer s2) throws Exception {
		stopServers((String[])null, s1, s2);
	}
}