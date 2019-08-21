package com.ibm.ws.security.fat.common.servers;

import java.net.InetAddress;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public class ServerInstanceUtils {
    private final static Class<?> thisClass = ServerInstanceUtils.class;
    //    ServerFileUtils serverFileUtils = new ServerFileUtils();
    //    CommonFatLoggingUtils loggingUtils = new CommonFatLoggingUtils();
    protected static ServerBootstrapUtils bootstrapUtils = new ServerBootstrapUtils();
    public static final String BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME = "fat.server.hostname";
    public static final String BOOTSTRAP_PROP_FAT_SERVER_HOSTIP = "fat.server.hostip";

    public static void addHostNameAndAddrToBootstrap(LibertyServer server) {
        String thisMethod = "addHostNameAndAddrToBootstrap";
        try {
            InetAddress addr = InetAddress.getLocalHost();
            String serverHostName = addr.getHostName();
            String serverHostIp = addr.toString().split("/")[1];

            bootstrapUtils.writeBootstrapProperty(server, BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME, serverHostName);
            bootstrapUtils.writeBootstrapProperty(server, BOOTSTRAP_PROP_FAT_SERVER_HOSTIP, serverHostIp);
        } catch (Exception e) {
            e.printStackTrace();
            Log.info(thisClass, thisMethod, "Setup failed to add host info to bootstrap.properties");
        }
    }
}
