/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.mbeans;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.SessionCookieConfig;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.commons.io.FileUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.ws.kernel.service.util.JavaInfo.Vendor;
import com.ibm.ws.webcontainer.httpsession.SessionManager;
import com.ibm.ws.webcontainer.osgi.DynamicVirtualHost;
import com.ibm.ws.webcontainer.osgi.DynamicVirtualHostManager;
import com.ibm.ws.webcontainer.osgi.WebContainer;
import com.ibm.ws.webcontainer.osgi.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppConfiguration;
import com.ibm.wsspi.channelfw.utils.HostNameUtils;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;
import com.ibm.wsspi.webcontainer.osgi.mbeans.GeneratePluginConfig;

/**
 * Generate the appropriate plugin configuration XML file for the current
 * webcontainer applications.
 */
public class PluginGenerator {

    private static final TraceComponent tc = Tr.register(PluginGenerator.class, com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants.TR_GROUP,
                                                         com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants.NLS_PROPS);
    private static final String styleSheet = " <xsl:stylesheet version=\"1.0\"                                   \n" +
                                             "     xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">           \n" +
                                             "   <xsl:output method=\"xml\"/>                                    \n" +
                                             "   <xsl:param name=\"indent-increment\" select=\"'   '\" />        \n" +
                                             "   <xsl:template match=\"*\">                                      \n" +
                                             "      <xsl:param name=\"indent\" select=\"'&#xA;'\"/>              \n" +
                                             "      <xsl:value-of select=\"$indent\"/>                           \n" +
                                             "      <xsl:copy>                                                   \n" +
                                             "        <xsl:copy-of select=\"@*\" />                              \n" +
                                             "        <xsl:apply-templates>                                      \n" +
                                             "          <xsl:with-param name=\"indent\"                          \n" +
                                             "               select=\"concat($indent, $indent-increment)\"/>     \n" +
                                             "        </xsl:apply-templates>                                     \n" +
                                             "        <xsl:if test=\"*\">                                        \n" +
                                             "          <xsl:value-of select=\"$indent\"/>                       \n" +
                                             "        </xsl:if>                                                  \n" +
                                             "      </xsl:copy>                                                  \n" +
                                             "   </xsl:template>                                                 \n" +
                                             "   <xsl:template match=\"comment()|processing-instruction()\">     \n" +
                                             "      <xsl:copy />                                                 \n" +
                                             "   </xsl:template>                                                 \n" +
                                             "   <xsl:template match=\"text()[normalize-space(.)='']\"/>         \n" +
                                             " </xsl:stylesheet>                                                 \n";
    private static final String NOT_DEFINED = "NOT_DEFINED";
    private static final String DEFAULT_VIRTUAL_HOST = "default_host";
    private static final String PLUGIN_CFG_ALIAS = "pluginConfiguration";
    private static final String HTTP_ALLOWED_ENDPOINT = "allowFromEndpointRef";
    private static final String LOCALHOST = "localhost";

    private static final String TRANSFORMER_FACTORY_JVM_PROPERTY_NAME = "javax.xml.transform.TransformerFactory";

    private static final Object transformerLock = new Object();

    protected enum Role {
        PRIMARY, SECONDARY
    }

    private final PluginConfigData pcd;
    private final BundleContext context;
    private final Bundle bundle;

    // distinguish between implicit generation (when endpoints change) and explicit generation (user mbean request)
    private boolean utilityRequest = true;
    private String appServerName = null;
    private String webServerName = null;

    // save a reference to the previously-generated configuration hash
    private Integer previousConfigHash = null;
    private File cachedFile;

    private static final boolean CHANGE_TRANSFORMER;

    static {
        if (!JavaInfo.vendor().equals(Vendor.IBM)) {
            CHANGE_TRANSFORMER = false;
        } else {
            int majorVersion = JavaInfo.majorVersion();
            CHANGE_TRANSFORMER = majorVersion == 8;
        }
    }

    /**
     * Constructor.
     *
     */
    PluginGenerator(Map<String, Object> config, WsLocationAdmin locSvc, BundleContext context) {
        this.context = context;

        // process plugin configuration data
        PluginConfigData newPcd = null;
        try {
            newPcd = new PluginConfigData(config);

            newPcd.print(tc);
        } catch (Throwable t) {
            FFDCFilter.processException(t, PluginGenerator.class.getName(), "processConfig");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error process Config: " + t.getMessage());
            }
            // pcd remains null, indicating that the config processing failed
        }
        pcd = newPcd;
        appServerName = locSvc.getServerName();

        bundle = context.getBundle();
        cachedFile = bundle.getDataFile("cached-PluginCfg.xml");

        if (cachedFile.exists()) {
            try {

                PluginConfigQuickPeek quickPeek = new PluginConfigQuickPeek(new FileInputStream(cachedFile));
                previousConfigHash = quickPeek.getHashValue();
            } catch (Exception e) {
                // Do nothing we are just trying to avoid doing xml serialization twice.
            }
        }
    }

    private boolean isBundleUninstalled() {
        return bundle.getState() == Bundle.UNINSTALLED;
    }

    /**
     * Generate the XML configuration with the current container information.
     *
     * @param container
     * @param root      install location of plugin; overrides configured values for root install and log path
     * @param name
     */
    @FFDCIgnore(IOException.class)
    protected synchronized void generateXML(String rootLoc, String serverName,
                                            WebContainer container,
                                            SessionManager smgr,
                                            DynamicVirtualHostManager vhostMgr,
                                            WsLocationAdmin locationService,
                                            boolean utilityReq,
                                            File writeDirectory) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "generateXML", "server = " + serverName + ", Framework is stopping = " + FrameworkState.isStopping() + ", pcd = " + pcd + ", this = " + this);
        }

        // Because this method is synchronized there can become a queue of requests waiting which then don't get started
        // for a significant time period. As a result if the servers is now shutting down skip generation.
        if (pcd == null || FrameworkState.isStopping() || container.isServerStopping()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "generateXML", ((FrameworkState.isStopping() || container.isServerStopping()) ? "Server is stopping" : "pcd is null"));
            }
            // add error message in next update
            return;
        }

        if(context == null){
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "generateXML", "Error creating plugin config xml: BundleContext is null");
            }
            return;
        }

        utilityRequest = utilityReq;
        boolean writeFile = true;

        // set up server names, preserving original behavior for explicit (mbean) requests
        if (!utilityRequest) {
            appServerName = serverName;
            webServerName = serverName;
        } else {
            appServerName = locationService.getServerName();
            webServerName = pcd.webServerName;
        }

        BufferedWriter pluginCfgWriter = null;
        WsResource outFile = null;
        FileOutputStream fOutputStream = null;
        try {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Generating webserver plugin cfg for server=" + appServerName);
            }

            String root = rootLoc;
            boolean userOverrideLocation = true;
            if (root == null) {
                root = pcd.PluginInstallRoot;
                userOverrideLocation = false;
            }
            //String root = (null == rootLoc) ? pcd.PluginInstallRoot : rootLoc;
            Map<String, Map<String, Set<URIData>>> clusterUriGroups = new HashMap<String, Map<String, Set<URIData>>>();

            Document output = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

            SimpleDateFormat tmpDateFmt = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z");
            Comment comment = output.createComment(String.format("HTTP server plugin config file for %s generated on %s",
                                                                 appServerName,
                                                                 tmpDateFmt.format(new Date())));
            output.appendChild(comment);

            // create and insert a config root element
            Element rootElement = output.createElement("Config");

            // add in hardcoded properties and any extra properties from the user configuration
            if (!pcd.extraConfigProperties.isEmpty()) {
                if (pcd.TrustedProxyEnable != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Overriding TrustedProxyEnable from extra config properties with the specified value");
                    }
                    pcd.extraConfigProperties.put("TrustedProxyEnable", pcd.TrustedProxyEnable.toString());
                }

                for (String key : pcd.extraConfigProperties.keySet()) {
                    String value = (String) pcd.extraConfigProperties.get(key);
                    rootElement.setAttribute(key, value);
                }
            }
            output.appendChild(rootElement);

            // add log Information
            Element elem = output.createElement("Log");
            //start 142740

            // If user provided install root as argument, use that to generate the log location
            String name = null;
            if (userOverrideLocation) {
                name = root + addSlash(root)
                       + "logs" + File.separatorChar
                       + webServerName + File.separatorChar + pcd.LogFile;;
            } // otherwise use configured value, with LogFileName taking precedence over LogDirLocation
            else {
                if (pcd.LogFileName != null)
                    name = pcd.LogFileName;
                else
                    name = pcd.LogDirLocation + addSlash(pcd.LogDirLocation) + pcd.LogFile;
            }

            if (name.charAt(1) == ':') //check if path specified is a windows path or not and replace File.separatorChar with correct separators
                name = name.replace('/', '\\');
            else
                name = name.replace('\\', '/');

            elem.setAttribute("Name", name);
            //end 142740
            elem.setAttribute("LogLevel", pcd.LogLevel);
            rootElement.appendChild(elem);

            // add esi properties
            Element esiProp1 = output.createElement("Property");
            esiProp1.setAttribute("Name", "ESIEnable");
            esiProp1.setAttribute("Value", pcd.ESIEnable.toString());
            rootElement.appendChild(esiProp1);

            Element esiProp2 = output.createElement("Property");
            esiProp2.setAttribute("Name", "ESIMaxCacheSize");
            esiProp2.setAttribute("Value", pcd.ESIMaxCacheSize.toString());
            rootElement.appendChild(esiProp2);

            Element esiProp3 = output.createElement("Property");
            esiProp3.setAttribute("Name", "ESIInvalidationMonitor");
            esiProp3.setAttribute("Value", pcd.ESIInvalidationMonitor.toString());
            rootElement.appendChild(esiProp3);

            Element esiProp4 = output.createElement("Property");
            esiProp4.setAttribute("Name", "ESIEnableToPassCookies");
            esiProp4.setAttribute("Value", pcd.ESIEnableToPassCookies.toString());
            rootElement.appendChild(esiProp4);

            Element esiProp5 = output.createElement("Property");
            esiProp5.setAttribute("Name", "PluginInstallRoot");
            esiProp5.setAttribute("Value", root);
            rootElement.appendChild(esiProp5);

            HttpEndpointInfo httpEndpointInfo;
            try {
                httpEndpointInfo = new HttpEndpointInfo(context, output, pcd.httpEndpointPid);
            } catch(IllegalStateException e) { //  BundleContext is no longer valid
                if(!this.isBundleUninstalled()){
                    throw e; // Missing for some other reason
                }
                return;
            }

            // Map of virtual host name to the list of alias data being collected...
            Map<String, List<VHostData>> vhostAliasData = new HashMap<String, List<VHostData>>();

            // Process the virtual host configuration..
            Set<DynamicVirtualHost> virtualHostSet = processVirtualHosts(vhostMgr, vhostAliasData, httpEndpointInfo, rootElement);

            // Create the VirtualHostGroup and VirtualHost elements
            for (DynamicVirtualHost vh : virtualHostSet) {
                // Create the VirtualHostGroup in the plugin xml
                Element vhElem = output.createElement("VirtualHostGroup");
                vhElem.setAttribute("Name", vh.getName());
                rootElement.appendChild(vhElem);

                if (!vhostAliasData.containsKey(vh.getName())) {
                    continue;
                }
                // Create a VirtualHost element for each alias
                for (VHostData vh_aliasData : vhostAliasData.get(vh.getName())) {
                    Element aliasElem = output.createElement("VirtualHost");
                    // The IPv6 is already has the [] in alias
                    aliasElem.setAttribute("Name", vh_aliasData.host + ":" + vh_aliasData.port);
                    vhElem.appendChild(aliasElem);
                }
            }

            if (pcd.TrustedProxyGroup != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Adding custom property TrustedProxyGroup element and its associated proxy servers");
                }

                Element tproxyGroupElem = output.createElement("TrustedProxyGroup");
                rootElement.appendChild(tproxyGroupElem);
                for (String trustedProxy : pcd.TrustedProxyGroup) {
                    Element tproxyElem = output.createElement("TrustedProxy");
                    if (trustedProxy.indexOf(":") != -1) {
                        // IPV6
                        tproxyElem.setAttribute("Name", "[" + trustedProxy.trim() + "]");
                    } else {
                        tproxyElem.setAttribute("Name", trustedProxy.trim());
                    }
                    tproxyGroupElem.appendChild(tproxyElem);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Added proxy server " + trustedProxy + " TrustedProxyGroup element");
                    }
                }
            } // end-trusted-proxy

            // define all server clusters
            // config of server clusters
            String serverID = smgr.getCloneID();
            boolean singleServerConfig = true;
            if (serverID == null) {
                serverID = "";
            }
            if (serverID.length() > 0) {
                // if the clone ID is defined, assume that session affinity matters
                singleServerConfig = false;
            }
            char cloneSep = smgr.getCloneSeparator();
            Boolean cloneSeparatorChange = null;
            if (':' == cloneSep) {
                cloneSeparatorChange = Boolean.FALSE;
            } else if ('+' == cloneSep) {
                cloneSeparatorChange = Boolean.TRUE;
            } else {
                throw new IllegalStateException("The session manager is configured to use '" + cloneSep + "' as the clone separator, but " + pcd.PluginConfigFileName
                                                + " only supports ':' and '+'.");
            }
            pcd.cloneSeparatorChange = cloneSeparatorChange;

            Element pServersElem = null;
            Element bServersElem = null;
            int numberOfPrimaryServers = 0;
            int numberOfBackupServers = 0;

            // ------------- SERVER CLUSTER ---------------------
            // A Liberty server can only belong to one cluster
            ServerClusterData scd = pcd.createServerCluster(appServerName + "_" + GeneratePluginConfig.DEFAULT_NODE_NAME + "_Cluster", singleServerConfig);

            // get the server cluster data
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Adding the ServerCluster " + scd.clusterName);
            }
            scd.print(tc);

            // define the server cluster element
            Element sgElem = output.createElement("ServerCluster");

            // Set cluster attributes
            // name is node_server
            sgElem.setAttribute("Name", scd.clusterName);
            sgElem.setAttribute("LoadBalance", scd.loadBalance);
            if (pcd.ignoreAffinityRequests != null) {
                sgElem.setAttribute("IgnoreAffinityRequests", pcd.ignoreAffinityRequests.toString());
            }
            sgElem.setAttribute("RetryInterval", scd.retryInterval.toString());
            sgElem.setAttribute("ServerIOTimeoutRetry", scd.serverIOTimeoutRetry.toString());
            sgElem.setAttribute("RemoveSpecialHeaders", scd.removeSpecialHeaders.toString());
            sgElem.setAttribute("CloneSeparatorChange", scd.cloneSeparatorChange.toString());
            sgElem.setAttribute("PostSizeLimit", scd.postSizeLimit.toString());
            sgElem.setAttribute("PostBufferSize", scd.postBufferSize.toString());
            // retrieve Partition Table when HA-Based Session mgt is configured
            sgElem.setAttribute("GetDWLMTable", scd.GetDWLMTable.toString());

            // define a primary server element if this is a multi server gen
            if (false == scd.singleServerConfig.booleanValue()) {
                pServersElem = output.createElement("PrimaryServers");
                bServersElem = output.createElement("BackupServers");
            }

            // check to see if the server is shutting down; if it is, bail out. A final exit message will be logged in the finally().
            if (pcd == null || FrameworkState.isStopping() || container.isServerStopping()) {
                return;
            }

            if (!httpEndpointInfo.isValid()) {
                // We couldn't find a matching endpoint -- there will be bits missing from
                // the generated plugin config as a result
                comment = output.createComment(" The configured endpoint could not be found. httpEndpointRef=" + httpEndpointInfo.getEndpointId());
                rootElement.appendChild(comment);
            } else {
                // This is unique to liberty: we put the endpoint (http/https) in its
                // own server. (this behavior has existed since 8.5.0.. )
                // As of 8.5.5.2, we will use only one endpoint, so that a single plugin configuration
                // will contain only one server definition (which is good because there was only one server id.. )

                if (!buildServerTransportData(appServerName, serverID, httpEndpointInfo, scd.clusterServers, pcd.IPv6Preferred, container)) {
                    // the server is currently shutting down. A final exit message will be logged in the finally().
                    return;
                }

                // create a server element for each server in the cluster
                for (ServerData sd : scd.clusterServers) {
                    // check to see if the server is shutting down; if it is, bail out. A final exit message will be logged in the finally().
                    if (pcd == null || FrameworkState.isStopping() || container.isServerStopping()) {
                        return;
                    }

                    // get the server data
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Adding the Server definition " + sd.nodeName + "_" + sd.serverName);
                    }
                    sd.print(tc);

                    // create a server element for the server
                    Element serverElem = output.createElement("Server");
                    serverElem.setAttribute("Name", sd.nodeName + "_" + sd.serverName);

                    // add weight and clone id if multi server generation
                    if (false == scd.singleServerConfig.booleanValue()) {

                        serverElem.setAttribute("LoadBalanceWeight", sd.loadBalanceWeight.toString());

                        if (0 < sd.serverID.length()) {
                            serverElem.setAttribute("CloneID", sd.serverID);
                        }
                    }
                    // Set server attributes
                    // Could not find the best match values in liberty now, so just use the default value of metatype
                    serverElem.setAttribute("ConnectTimeout", sd.connectTimeout.toString());
                    serverElem.setAttribute("ServerIOTimeout", sd.serverIOTimeout.toString());
                    if (sd.wsServerIOTimeout != null)
                        serverElem.setAttribute("wsServerIOTimeout", sd.wsServerIOTimeout.toString());
                    if (sd.wsServerIdleTimeout != null)
                        serverElem.setAttribute("wsServerIdleTimeout", sd.wsServerIdleTimeout.toString());
                    serverElem.setAttribute("WaitForContinue", sd.waitForContinue.toString());
                    serverElem.setAttribute("MaxConnections", sd.maxConnections.toString());
                    serverElem.setAttribute("ExtendedHandshake", sd.extendedHandshake.toString());

                    sgElem.appendChild(serverElem);

                    if (sd.transports != null) {
                        // define its transports
                        for (TransportData currentTransport : sd.transports) {
                            Element tElem = output.createElement("Transport");
                            String hostname = currentTransport.host;

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Adding the Transport definition " + hostname);
                            }
                            currentTransport.print(tc);

                            tElem.setAttribute("Hostname", hostname);
                            String transportPort = Integer.toString(currentTransport.port);
                            tElem.setAttribute("Port", transportPort);
                            if (currentTransport.isSslEnabled) {
                                tElem.setAttribute("Protocol", "https");

                                Element sslProp1 = output.createElement("Property");
                                sslProp1.setAttribute("Name", "keyring");
                                sslProp1.setAttribute("Value", pcd.KeyringLocation);
                                tElem.appendChild(sslProp1);

                                Element sslProp2 = output.createElement("Property");
                                sslProp2.setAttribute("Name", "stashfile");
                                sslProp2.setAttribute("Value", pcd.StashfileLocation);
                                tElem.appendChild(sslProp2);

                                if (pcd.CertLabel != null) {
                                    Element sslProp3 = output.createElement("Property");
                                    sslProp3.setAttribute("Name", "certLabel");
                                    sslProp3.setAttribute("Value", pcd.CertLabel);
                                    tElem.appendChild(sslProp3);
                                }
                            } else {
                                tElem.setAttribute("Protocol", "http");
                            }

                            serverElem.appendChild(tElem);
                        }
                    }

                    // append the server cluster element to the root
                    rootElement.appendChild(sgElem);

                    // add the server to the primary servers if this is a multi
                    // server gen
                    if (false == scd.singleServerConfig.booleanValue()) {
                        Element psServerElem = output.createElement("Server");
                        psServerElem.setAttribute("Name", sd.nodeName + "_" + sd.serverName);

                        // Check if the current server is primary or backup
                        if (sd.roleKind == Role.PRIMARY) {
                            pServersElem.appendChild(psServerElem);
                            numberOfPrimaryServers++;
                        } else {
                            bServersElem.appendChild(psServerElem);
                            numberOfBackupServers++;
                        }
                    }
                } // end of for processing each server
            } // end of if we had an endpoint reference

            // append the primary servers if this is a multi server gen
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Number of primary servers: "
                             + numberOfPrimaryServers
                             + " Number of backup servers: "
                             + numberOfBackupServers);
            }

            if (false == scd.singleServerConfig.booleanValue()) {
                if (numberOfPrimaryServers > 0) {
                    sgElem.appendChild(pServersElem);
                }
                if (numberOfBackupServers > 0) {
                    sgElem.appendChild(bServersElem);
                }
            }

            // process the deployed modules for each server cluster (same
            // across all servers in a cluster)
            // deployed module config
            String defaultAffinityCookie = smgr.getDefaultAffinityCookie();
            String affinityUrlIdentifier = smgr.getAffinityUrlIdentifier();

            // all virtual hosts are in the same cluster
            for (DynamicVirtualHost vhost : virtualHostSet) {
                for (Iterator<?> apps = vhost.getWebApps(); apps.hasNext();) {
                    WebApp app = (WebApp) apps.next();
                    // a timing window is possible where a wepp app of "null" is in the list.
                    if (app != null) {
                        DeployedModuleData dmd = new DeployedModuleData(app, defaultAffinityCookie, affinityUrlIdentifier);
                        scd.deployedModules.add(dmd);
                    }
                }
            }

            Map<String, Set<URIData>> uriGroups = new HashMap<String, Set<URIData>>();
            List<String> webGroupIDs = new LinkedList<String>();
            // Check if any applications are deployed on the cluster
            if (!scd.deployedModules.isEmpty()) {
                for (DeployedModuleData dmd : scd.deployedModules) {
                    dmd.print(tc);

                    if (dmd.moduleConfig == null) {
                        continue;
                    }

                    String contextRoot = dmd.moduleConfig.getContextRoot();
                    if (!contextRoot.startsWith("/"))
                        contextRoot = "/" + contextRoot;

                    //String lvh = dmd.moduleBindings.getVirtualHostName();
                    String lvh = dmd.moduleConfig.getVirtualHostName();

                    if (lvh == null || 0 == lvh.length()) {
                        lvh = DEFAULT_VIRTUAL_HOST;
                    }

                    if (!uriGroups.containsKey(lvh)) {
                        uriGroups.put(lvh, new LinkedHashSet<URIData>());
                    }

                    Set<URIData> uriList = uriGroups.get(lvh);
                    // form the web group index and warn if a duplicate context root is being used
                    String wgIndex = lvh + contextRoot;

                    if (webGroupIDs.contains(wgIndex)) {
                        if (utilityRequest) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "duplicate.context.root", contextRoot);
                            }
                        } else
                            Tr.warning(tc, "duplicate.context.root", contextRoot);
                    }

                    // add index for future reference
                    webGroupIDs.add(wgIndex);

                    // Enable File Serving by default
                    // file serving enabled...just add the context root
                    // does not considerate file serving disable!!!
                    String newContextRoot = appendWildCardString(contextRoot);
                    uriList.add(new URIData(newContextRoot, dmd.cookieName, dmd.urlCookieName));

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Computed URIs for the module " + dmd.moduleConfig.getDisplayName());
                    }
                } // end of while processing each deployed module
            }

            // put the cluster's uri groups into the cluster uri hash table,
            // indexed by the cluster
            clusterUriGroups.put(scd.clusterName, uriGroups);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Added the ServerCluster elements");
            }

            // output the uri group and route elements for each cluster
            Set<ClusterUriGroup> cUgsSet = new HashSet<ClusterUriGroup>();

            for (Map.Entry<String, Map<String, Set<URIData>>> entry : clusterUriGroups.entrySet()) {
                String clusterName = entry.getKey();
                uriGroups = entry.getValue();

                String lvh = null;
                Set<URIData> uriList = null;
                // iterate through the uri groups
                for (Map.Entry<String, Set<URIData>> ugEntry : uriGroups.entrySet()) {
                    lvh = ugEntry.getKey();
                    uriList = ugEntry.getValue();

                    Element ugElem = output.createElement("UriGroup");
                    String uriGroupName = lvh + "_" + clusterName + "_URIs";
                    ugElem.setAttribute("Name", uriGroupName);
                    rootElement.appendChild(ugElem);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Adding the URIGroup " + uriGroupName);
                    }

                    // Check if URIList exists
                    if (uriList != null) {
                        for (URIData ud : uriList) {
                            ud.print(tc);

                            Element uriElem = output.createElement("Uri");
                            uriElem.setAttribute("Name", ud.uriName);
                            if (ud.cookieName != null && 0 < ud.cookieName.length()) {
                                uriElem.setAttribute("AffinityCookie",
                                                     ud.cookieName);
                            }
                            uriElem.setAttribute("AffinityURLIdentifier",
                                                 ud.urlCookieName);
                            ugElem.appendChild(uriElem);
                        }
                    }
                    cUgsSet.add(new ClusterUriGroup(lvh, clusterName, uriGroupName));
                }

            } // end-cluster-urigroups

            // ------------------------------------------
            // Create Routes
            for (DynamicVirtualHost vhost : virtualHostSet) {
                for (ClusterUriGroup cug : cUgsSet) {
                    if (vhost.getName().equals(cug.vhostName)) {
                        Element routeElem = output.createElement("Route");
                        routeElem.setAttribute("VirtualHostGroup", vhost.getName());
                        routeElem.setAttribute("UriGroup", cug.uriGroupName);
                        routeElem.setAttribute("ServerCluster", cug.clusterName);
                        rootElement.appendChild(routeElem);
                    }
                }
            }

            // The <RequestMetrics> and the sub elements <filters> are not processed yet
            // bunch of PMI stuff?

            // check to see if the server is shutting down; if it is, bail out. A final exit message will be logged in the finally().
            if (pcd == null || FrameworkState.isStopping() || container.isServerStopping()) {
                return;
            }

            // create the plugin config output file
            // Location of plugin-cfg.xml is the server.output.dir/logs/state for implicit requests, server.output.dir for direct mbean requests

            Boolean fileExists = false;
            if (writeDirectory == null) {
                String outputDirectory = "";
                if (utilityRequest) {
                    // If utilityRequest is true and there was no writeDirectory then write to the server.output.dir/logs/state/ directory
                    outputDirectory = "logs" + File.separatorChar + "state" + File.separatorChar;
                }
                fileExists = locationService.getServerOutputResource(outputDirectory + pcd.PluginConfigFileName).exists();
                outFile = locationService.getServerOutputResource(outputDirectory + pcd.TempPluginConfigFileName);
            } else {
                // Otherwise a writeDirectory was specified
                // Add a trailing slash if one is not present
                String path = writeDirectory.getPath();
                if (path.charAt(path.length() - 1) != File.separatorChar) {
                    path += File.separatorChar;
                }
                File pluginFile = new File(path + pcd.PluginConfigFileName);
                fileExists = pluginFile.exists();
                File temPluginFile = new File(path + pcd.TempPluginConfigFileName);
                // ensure any existing temp file is deleted (should never exist in non failure situations)
                if (temPluginFile.exists())
                    temPluginFile.delete();
                outFile = locationService.asResource(temPluginFile, true);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Output file already exists : " + fileExists);
            }

            // Check to see if the config has changed
            writeFile = hasConfigChanged(output);

            // Only write out to file if we have new or changed configuration information, or if this is an explicit request
            if (writeFile || !utilityRequest || !fileExists) {
                // The bundle must not be uninstalled in order to write the file
                // If writeFile is true write to the cachedFile and copy from there
                // If writeFile is false and the cachedFile doesn't exist write to the cache file and copy from there
                // If writeFile is false and cachedFile exists copy from there
                // If writeFile is false and cachedFile doesn't exist write to cachedFile and copy from there
                try {
                    if (!cachedFile.exists() || writeFile) {
                        fOutputStream = new FileOutputStream(cachedFile);
                        pluginCfgWriter = new BufferedWriter(new OutputStreamWriter(fOutputStream, StandardCharsets.ISO_8859_1));

                        // Write the plugin config file
                        // Create a style sheet to indent the output
                        StreamSource xsltSource = new StreamSource(new StringReader(styleSheet));

                        // Use transform apis to do generic serialization
                        TransformerFactory tfactory = getTransformerFactory();
                        Transformer serializer = tfactory.newTransformer(xsltSource);
                        Properties oprops = new Properties();
                        oprops.put(OutputKeys.METHOD, "xml");
                        oprops.put(OutputKeys.OMIT_XML_DECLARATION, "no");
                        oprops.put(OutputKeys.VERSION, "1.0");
                        oprops.put(OutputKeys.INDENT, "yes");
                        serializer.setOutputProperties(oprops);
                        serializer.transform(new DOMSource(output), new StreamResult(pluginCfgWriter));
                    }
                } catch(IOException e){
                    //path to the cachedFile is broken when bundle was uninstalled
                    if(!this.isBundleUninstalled()){
                        throw e; // Missing for some other reason
                    }
                } finally {
                    if (pluginCfgWriter != null) {
                        pluginCfgWriter.flush();
                        // Ensure data is physically written to disk
                        fOutputStream.getFD().sync();
                        pluginCfgWriter.close();
                    }
                    try {
                        copyFile(cachedFile, outFile.asFile());
                    } catch (IOException e){
                        //cachedFile no longer exists if the bundle was uninstalled
                        if(!this.isBundleUninstalled()){
                            throw e; // Missing for some other reason
                        }
                    }
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "A new plugin configuration file was not written: the configuration did not change.");
                }
            }
        } catch (Throwable t) {
            FFDCFilter.processException(t, PluginGenerator.class.getName(), "generateXML", new Object[] { container });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error creating plugin config xml; " + t.getMessage());
            }
        } finally {
            try {
                // check to see if the server is shutting down; if it is, bail out
                if (pcd == null || FrameworkState.isStopping() || container.isServerStopping()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "generateXML", ((FrameworkState.isStopping() || container.isServerStopping()) ? "Server is stopping" : "pcd is null"));
                    }
                    return;
                }
                
                // Verify that the temp plugin file exists
                if (!outFile.exists()) {
                    throw new FileNotFoundException("File " + outFile.asFile().getAbsolutePath() + " could not be found");
                }
                // Construct the actual plugin file path
                File pluginFile = new File(outFile.asFile().getParentFile(), pcd.PluginConfigFileName);
                

                if (pluginFile.exists()) {
                    FileUtils.forceDelete(pluginFile);
                }

                Files.move(outFile.asFile().toPath(), pluginFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // tell the user where the file is - quietly for implicit requests
                String fullFilePath = pluginFile.getAbsolutePath();
                if (utilityRequest)
                    Tr.info(tc, "plugin.file.generated.info", fullFilePath);
                else
                    Tr.audit(tc, "plugin.file.generated.audit", fullFilePath);
            } catch (Throwable t) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Error renaming the plugin config xml; " + t.getMessage());
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "generateXML");
        }
    }

    @FFDCIgnore(IOException.class)
    public static void copyFile(File in, File out)
                    throws IOException
                {
                    FileChannel inChannel = new
                        FileInputStream(in).getChannel();
                    FileChannel outChannel = new
                        FileOutputStream(out).getChannel();
                    try {
                        inChannel.transferTo(0, inChannel.size(),
                                outChannel);
                    }
                    catch (IOException e) {
                        throw e;
                    }
                    finally {
                        if (inChannel != null) inChannel.close();
                        if (outChannel != null) outChannel.close();
                    }
                }

    private static TransformerFactory getTransformerFactory() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getTransformerFactory", "CHANGE_TRANSORMER = " + CHANGE_TRANSFORMER);
        }

        TransformerFactory tf = null;

        if (CHANGE_TRANSFORMER) {

            // Synchronize setting and restoring the jvm property to prevent this sequence:
            // 1. Thread 1 gets jvm property
            // 2. Thread 1 sets jvm property
            // 3. Thread 2 gets jvm property set by Thread 1
            // 4. Thread 1 resets jvm property to value obtained at 1.
            // 5. Thread 2 resets jvm property to value set by Thread 1.
            synchronized (transformerLock) {

                final String defaultTransformerFactory = getJVMProperty(TRANSFORMER_FACTORY_JVM_PROPERTY_NAME);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "JDK = " + JavaInfo.vendor() + ", JDK level = " + JavaInfo.majorVersion() + "." + JavaInfo.minorVersion() + ", current TF jvm property value = "
                                 + defaultTransformerFactory);
                }

                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        System.setProperty(TRANSFORMER_FACTORY_JVM_PROPERTY_NAME, "org.apache.xalan.processor.TransformerFactoryImpl");
                        return null;
                    }
                });

                tf = TransformerFactory.newInstance();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "IBM JDK : Use transformer factory: " + tf.getClass().getName());
                }

                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        if (defaultTransformerFactory != null)
                            System.setProperty(TRANSFORMER_FACTORY_JVM_PROPERTY_NAME, defaultTransformerFactory);
                        else
                            System.clearProperty(TRANSFORMER_FACTORY_JVM_PROPERTY_NAME);
                        return null;
                    }
                });

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "IBM JDK : TF jvm property value restored: " + getJVMProperty(TRANSFORMER_FACTORY_JVM_PROPERTY_NAME));
                }
            }
        } else {
            tf = TransformerFactory.newInstance();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Not IBM JDK : Use transformer factory: " + tf.getClass().getName());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getTransformerFactory");
        }
        return tf;

    }

    private static String getJVMProperty(final String propertyName) {
        String propValue = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(propertyName);
            }
        });
        return propValue;
    }

    /**
     * Check to see if the current config has the same information as the previously
     * written config. If this config has no new information, return false.
     *
     * @param newConfig the current config information document to be compared
     * @return true if there is new or updated config information
     */
    private boolean hasConfigChanged(Document newConfig) {
        NodeList list = newConfig.getElementsByTagName("*");
        int currentHash = nodeListHashValue(list);

        // Either this is the first time checking the config or there has been some change
        if (this.previousConfigHash == null || currentHash != this.previousConfigHash) {
            this.previousConfigHash = currentHash;
            storeHashValue(newConfig, previousConfigHash);
            return true;
        }
        // No config changes
        else {
            return false;
        }
    }

    /**
     * @param newConfig
     * @param previousConfigHash2
     */
    private void storeHashValue(Document newConfig, Integer configHashValue) {

        Element root = newConfig.getDocumentElement();
        boolean hasHash = root.hasAttribute("ConfigHash");
        if (!hasHash) {
            Attr hashAttribute = newConfig.createAttribute("ConfigHash");
            hashAttribute.setValue(configHashValue.toString());
            root.setAttributeNode(hashAttribute);
        } else
            root.setAttribute("ConfigHash",configHashValue.toString());
    }

    /**
     * Compute a hash by iterating over every Attribute in each Node in this NodeList
     * and summing the hashCode()s of all attribute names and values
     */
    private Integer nodeListHashValue(NodeList list) {
        if (list == null) {
            return null;
        }
        int currentHash = 0;
        int listLength = list.getLength();

        // Iterate over each Node in list
        for (int iterator = 0; iterator < listLength; iterator++) {
            NamedNodeMap map = list.item(iterator).getAttributes();
            int numAttrs = map.getLength();

            // Iterate over all Attributes in this node
            // and sum their hashCode()s
            for (int i = 0; i < numAttrs; i++) {
                Attr attr = (Attr) map.item(i);
                int hash1 = attr.getNodeName().hashCode();
                int hash2 = attr.getNodeValue().hashCode();
                currentHash += hash1 + hash2;
            }
        }
        return currentHash;
    }

    /**
     * Return the hash value stored in the cached document
     */
    private Integer getHashValue(Document doc) {
        if (doc == null) {
            return null;
        }
        Element root = doc.getDocumentElement();
        String hash = root.getAttribute("ConfigHash");
        if (hash != null)
            return new Integer(hash);
        return null;


    }

    Set<DynamicVirtualHost> processVirtualHosts(DynamicVirtualHostManager vhostMgr,
                                                Map<String, List<VHostData>> vhostAliasData,
                                                HttpEndpointInfo httpEndpointInfo,
                                                Element rootElement) throws Exception {

        Document doc = rootElement.getOwnerDocument();

        // All registered virtual host configurations (transport side)
        Map<String, ServiceReference<?>> vhostConfigRefs = getVirtualHostRefs();

        // Set of discovered virtual hosts
        Set<DynamicVirtualHost> virtualHostSet = new HashSet<DynamicVirtualHost>();

        // Map of ports to the virtual host(s) that use it
        // when we print out the virtual host groups in the xml, we can only merge elements
        // together if the port is not used by any other virtual host
        Map<Integer, List<String>> portToVHostNameMap = new HashMap<Integer, List<String>>();

        // Do we have to evaluate all virtual hosts?
        boolean findVirtualHosts = true;
        ServiceReference<?> defaultHost = vhostConfigRefs.get(DEFAULT_VIRTUAL_HOST);
        boolean defaultHostIsCatchAll = true;
        if (defaultHost == null || defaultHost.getProperty("hostAlias") != null) {
            defaultHostIsCatchAll = false;
        }

        // IF there is only one virtual host defined, and there are no aliases configured
        // by the user for the default_host, it will function as the catch-all, and
        // we can generate a simplified plugin-cfg.cml file.
        if (vhostConfigRefs.size() == 1 && defaultHostIsCatchAll) {
            Iterator<DynamicVirtualHost> vHosts = vhostMgr.getVirtualHosts();
            DynamicVirtualHost vh = vHosts.hasNext() ? vHosts.next() : null;

            // Now check for an endpoint restriction.
            if (blockedByRestrictions(defaultHost.getProperty(HTTP_ALLOWED_ENDPOINT))) {
                // There is only one virtual host, and the endpoint that the plugin is configured
                // to use can't talk to it. We're DOA. A comment is added down below because
                // the virtual host set will be empty..
            } else if (vh == null) {
                // This can happen when no applications are defined.
                if (!utilityRequest)
                    Tr.warning(tc, "warn.check.applications");

                Comment comment = doc.createComment(String.format(" No Virtual Hosts were found, possibly because no applications are defined. %n\t"
                                                                  + " Verify that at least one application is defined in the server configuration. "));
                rootElement.appendChild(comment);
                return Collections.emptySet();
            } else {
                // either there were no restrictions defined, or the configured endpoint is in the list
                findVirtualHosts = false;

                virtualHostSet.add(vh);

                // We can produce a simplified configuration. The default virtual host is the
                // only defined virtual host, and it contains only generated aliases that
                // match the configured endpoint.
                // If we have a usable endpoint ref, get the pretty id.
                Comment comment = doc.createComment(String.format(" The default_host contained only aliases for endpoint %s.%n\t"
                                                                  + " The generated VirtualHostGroup will contain only configured web server ports:%n\t\t%s%s%s ",
                                                                  httpEndpointInfo.getEndpointId(),
                                                                  (pcd.webServerHttpPort > 0 ? "webserverPort=" + pcd.webServerHttpPort : ""),
                                                                  (pcd.webServerHttpPort > 0 && pcd.webServerHttpsPort > 0 ? "\n\t\t" : ""),
                                                                  (pcd.webServerHttpsPort > 0 ? "webserverSecurePort=" + pcd.webServerHttpsPort : "")));
                rootElement.appendChild(comment);

                List<VHostData> vh_aliasData = new ArrayList<VHostData>();
                if (pcd.webServerHttpPort > 0) {
                    VHostData webServerHttpPort = new VHostData("*", pcd.webServerHttpPort);
                    vh_aliasData.add(webServerHttpPort);
                    mapPortUsage(portToVHostNameMap, DEFAULT_VIRTUAL_HOST, webServerHttpPort);
                }

                if (pcd.webServerHttpsPort > 0) {
                    VHostData webServerHttpsPort = new VHostData("*", pcd.webServerHttpsPort);
                    vh_aliasData.add(webServerHttpsPort);
                    mapPortUsage(portToVHostNameMap, DEFAULT_VIRTUAL_HOST, webServerHttpsPort);
                }

                // save the list of constructed VHostData
                vhostAliasData.put(DEFAULT_VIRTUAL_HOST, vh_aliasData);
            }
        }

        if (findVirtualHosts) {
            boolean foundWildcardWebserverHttp = false;
            boolean foundWildcardWebserverHttps = false;

            // identify virtual hosts based on virtual hosts used by applications
            for (Iterator<DynamicVirtualHost> i = vhostMgr.getVirtualHosts(); i.hasNext();) {
                DynamicVirtualHost vh = i.next();
                String vh_name = vh.getName();
                ServiceReference<?> vhostConfig = vhostConfigRefs.get(vh_name);

                if (vhostConfig == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Virtual host " + vh.getName() + " has no configuration");
                    }
                    continue;
                }

                // If there is an endpoint restriction on the virtual host, see if the endpoint the
                // plugin will use is permitted
                if (blockedByRestrictions(vhostConfig.getProperty(HTTP_ALLOWED_ENDPOINT))) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Virtual host " + vh.getName() + " is not accessible from configured endpoint",
                                 "plugin endpoint = " + pcd.httpEndpointPid,
                                 "vhost required endpoints = " + getList((String[]) vhostConfig.getProperty(HTTP_ALLOWED_ENDPOINT)));
                    }
                    continue;
                }

                // Add the virtual host to the set
                virtualHostSet.add(vh);

                // Look at all of the aliases defined for this virtual host
                // as reported through the transport -> webcontainer linkage.
                // This will return aliases provided by the user or generated
                // for the transport ports..
                List<String> vh_aliases = vh.getAliases();

                if (vh_aliases.isEmpty()) {
                    // Do not add any default aliases here: all configurations should be present
                    // based on the transport configuration. Something else is wrong (like
                    // misconfigured transport.. )
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Virtual host " + vh.getName() + " has no defined host aliases");
                    }
                } else {
                    List<VHostData> vh_aliasData = new ArrayList<VHostData>();

                    for (String alias : vh_aliases) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "adding " + vh.getName() + " -> " + alias);
                        }

                        VHostData vh_alias = new VHostData(alias);
                        vh_aliasData.add(vh_alias);
                        mapPortUsage(portToVHostNameMap, vh_name, vh_alias);

                        if (vh_alias.host.equals("*")) {
                            if (vh_alias.port == pcd.webServerHttpPort)
                                foundWildcardWebserverHttp = true;
                            if (vh_alias.port == pcd.webServerHttpsPort)
                                foundWildcardWebserverHttps = true;
                        }
                    }

                    // save the list of constructed VHostData
                    vhostAliasData.put(vh_name, vh_aliasData);
                }
            }

            // If we can, make sure we have aliases for the web server ports..
            List<VHostData> vh_aliasData = vhostAliasData.get(DEFAULT_VIRTUAL_HOST);
            if (pcd.webServerHttpPort > 0 && !foundWildcardWebserverHttp) {
                if (defaultHostIsCatchAll
                    && vh_aliasData != null
                    && !blockedByRestrictions(defaultHost.getProperty(HTTP_ALLOWED_ENDPOINT))) {
                    VHostData vhostData = new VHostData("*", pcd.webServerHttpPort);
                    vh_aliasData.add(vhostData);
                    mapPortUsage(portToVHostNameMap, DEFAULT_VIRTUAL_HOST, vhostData);
                } else {
                    // the http port was configured, but there are no virtual hosts that can accept requests for that alias
                    Comment comment = doc.createComment(String.format(" No virtual hosts are configured to accept requests from the webserver http port (*:%s).%n\t "
                                                                      + "Verify that virtualHost elements in server.xml have appropriate hostAlias attributes to support the webserver. ",
                                                                      pcd.webServerHttpPort));
                    rootElement.appendChild(comment);
                }
            }

            if (pcd.webServerHttpsPort > 0 && !foundWildcardWebserverHttps) {
                if (defaultHostIsCatchAll
                    && vh_aliasData != null
                    && !blockedByRestrictions(defaultHost.getProperty(HTTP_ALLOWED_ENDPOINT))) {
                    VHostData vhostData = new VHostData("*", pcd.webServerHttpsPort);
                    vh_aliasData.add(vhostData);
                    mapPortUsage(portToVHostNameMap, DEFAULT_VIRTUAL_HOST, vhostData);

                } else {
                    // the http port was configured, but there are no virtual hosts that can accept requests for that alias
                    Comment comment = doc.createComment(String.format(" No virtual hosts are configured to accept requests from the webserver https port (*:%s).%n\t "
                                                                      + "Verify that virtualHost elements in server.xml have appropriate hostAlias attributes to support the webserver. ",
                                                                      pcd.webServerHttpsPort));
                    rootElement.appendChild(comment);
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Finished finding vhosts and aliases", portToVHostNameMap, vhostAliasData);
            }
        }

        if (virtualHostSet.isEmpty()) {
            // If we have a usable endpoint ref, get the pretty id.
            Comment comment = doc.createComment(String.format(" No virtual hosts are accessible from the configured endpoint (%s).%n\t "
                                                              + "Verify the allowed endpoints for the virtualHost elements in server.xml. ",
                                                              httpEndpointInfo.getEndpointId()));
            rootElement.appendChild(comment);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Finished finding/pruning vhosts and aliases", portToVHostNameMap, vhostAliasData, virtualHostSet);
        }

        return virtualHostSet;
    }

    private boolean blockedByRestrictions(Object restrictions) {
        if (restrictions == null)
            return false;
        List<String> endpoints = getList((String[]) restrictions);
        if (endpoints.isEmpty())
            return false;

        return !endpoints.contains(pcd.httpEndpointPid);
    }

    private void mapPortUsage(Map<Integer, List<String>> portToVHostNameMap,
                              String vh_name,
                              VHostData vh_alias) throws UnknownHostException {
        // Add this vhost to the list that use this port
        List<String> port_vhostName = portToVHostNameMap.get(vh_alias.port);
        if (port_vhostName == null) {
            port_vhostName = new ArrayList<String>();
            portToVHostNameMap.put(vh_alias.port, port_vhostName);
        }
        port_vhostName.add(vh_name);
    }

    Map<String, ServiceReference<?>> getVirtualHostRefs() throws Exception {

        // The default_host can not be disabled.. but we should ignore any others
        // that have been disabled.
        String filter = "(&(service.factoryPid=com.ibm.ws.http.virtualhost)(|(enabled=true)(id=default_host)))";

        // find all registered/enabled virtual hosts.. (the original registered configuration)
        ServiceReference<?> refs[] = context.getAllServiceReferences(null, filter);

        if (refs == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "getVirtualHostRefs -- no configured virtual hosts found");
            }
            return Collections.emptyMap();
        }

        Map<String, ServiceReference<?>> result = new HashMap<String, ServiceReference<?>>();
        for (ServiceReference<?> ref : refs) {
            String id = (String) ref.getProperty("id");
            result.put(id, ref);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getVirtualHostRefs", id, ref);
            }
        }

        return result;
    }

    List<String> getList(String[] property) {
        if (property == null || property.length == 0)
            return Collections.emptyList();
        return Arrays.asList(property);
    }

    /**
     * 
     * @param appServerName
     * @param serverID
     * @param httpEndpointInfo
     * @param serverDataList
     * @param preferIPv6
     * @param container
     * @return false if server shutdown is detected 
     * @throws Exception
     */
    boolean buildServerTransportData(String appServerName,
                                  String serverID,
                                  HttpEndpointInfo httpEndpointInfo,
                                  List<ServerData> serverDataList,
                                  boolean preferIPv6,
                                  WebContainer container) throws Exception {

        String defaultHostName = (String) httpEndpointInfo.getProperty("_defaultHostName");

        String host = (String) httpEndpointInfo.getProperty("host");

        Integer httpPort = (Integer) httpEndpointInfo.getProperty("httpPort"); //start 146189
        if (httpPort == null)
            httpPort = -1;

        Integer httpsPort = (Integer) httpEndpointInfo.getProperty("httpsPort");
        if (httpsPort == null)
            httpsPort = -1; //end 146189

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "buildServerTransportData: adding " + httpEndpointInfo.getEndpointId(),
                     host, defaultHostName, httpPort, httpsPort);
        }

        ServerData sd = new ServerData(appServerName, serverID, pcd);
        serverDataList.add(sd);

        sd.nodeName = GeneratePluginConfig.DEFAULT_NODE_NAME;

        // check to see if the server is shutting down; if it is, bail out. A final exit message will be logged in the caller.
        if (pcd == null || FrameworkState.isStopping() || container.isServerStopping()) {
            return false;
        }

        // hostName is returned in lower case
        sd.hostName = tryDetermineHostName(host, defaultHostName, preferIPv6);
        if (!(utilityRequest) && sd.hostName.equals("localhost"))
            Tr.warning(tc, "collocated.appserver", sd.nodeName, sd.serverName);

        if (httpPort > 0)
            sd.addTransportData(sd.hostName, httpPort, false);
        if (httpsPort > 0)
            sd.addTransportData(sd.hostName, httpsPort, true);

        return true;
    }

    private static String appendWildCardString(String rootURI) {
        String rc = rootURI;
        if (!rc.startsWith("*.")) {
            if (rc.endsWith("/")) {
                rc += "*";
            } else if (!rc.endsWith("/*")) {
                rc += "/*";
            }
        }
        return rc;
    }


protected class XMLRootHandler extends DefaultHandler implements LexicalHandler {
        /**
         * An exception indicating that the parsing should stop. This is usually
         * triggered when the top-level element has been found.
         *
         */
        private class StopParsingException extends SAXException {
                /**
                 * All serializable objects should have a stable serialVersionUID
                 */
                private static final long serialVersionUID = 1L;

                /**
                 * Constructs an instance of <code>StopParsingException</code> with a
                 * <code>null</code> detail message.
                 */
                public StopParsingException() {
                        super((String) null);
                }
        }

        private String hashValue = null;

        /**
         * This is the name of the top-level element found in the XML file. This
         * member variable is <code>null</code> unless the file has been parsed
         * successful to the point of finding the top-level element.
         */
        private String elementFound = null;

        /**
         * These are the attributes of the top-level element found in the XML file.
         * This member variable is <code>null</code> unless the file has been
         * parsed successful to the point of finding the top-level element.
         */
        private Attributes attributesFound = null;

        public String getRootName() {
                return elementFound;
        }

        public Attributes getRootAttributes() {
                return attributesFound;
        }

        public XMLRootHandler() {
        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ext.LexicalHandler#comment(char[], int, int)
         */
        public final void comment(final char[] ch, final int start, final int length) {
                // Not interested.
        }

        /**
         * Creates a new SAX parser for use within this instance.
         *
         * @return The newly created parser.
         *
         * @throws ParserConfigurationException
         *             If a parser of the given configuration cannot be created.
         * @throws SAXException
         *             If something in general goes wrong when creating the parser.
         * @throws SAXNotRecognizedException
         *             If the <code>XMLReader</code> does not recognize the
         *             lexical handler configuration option.
         * @throws SAXNotSupportedException
         *             If the <code>XMLReader</code> does not support the lexical
         *             handler configuration option.
         */
        private final SAXParser createParser(SAXParserFactory parserFactory) throws ParserConfigurationException, SAXException, SAXNotRecognizedException, SAXNotSupportedException {
                // Initialize the parser.
                final SAXParser parser = parserFactory.newSAXParser();
                final XMLReader reader = parser.getXMLReader();
                reader.setProperty("http://xml.org/sax/properties/lexical-handler", this); //$NON-NLS-1$
                try {
                        // be sure validation is "off" or the feature to ignore DTD's will
                        // not apply
                        reader.setFeature("http://xml.org/sax/features/validation", false); //$NON-NLS-1$
                        reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); //$NON-NLS-1$
                } catch (SAXNotRecognizedException e) {
                        // not a big deal if the parser does not recognize the features
                } catch (SAXNotSupportedException e) {
                        // not a big deal if the parser does not support the features
                }
                return parser;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ext.LexicalHandler#endCDATA()
         */
        public final void endCDATA() {
                // Not interested.
        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ext.LexicalHandler#endDTD()
         */
        public final void endDTD() {
                // Not interested.
        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ext.LexicalHandler#endEntity(java.lang.String)
         */
        public final void endEntity(final String name) {
                // Not interested.
        }

        public boolean parseContents(InputSource contents) throws IOException, ParserConfigurationException, SAXException {
                // Parse the file into we have what we need (or an error occurs).
                try {
                        SAXParserFactory factory = SAXParserFactory.newInstance();
                        if (factory == null)
                                return false;
                        final SAXParser parser = createParser(factory);
                        contents.setSystemId("/"); //$NON-NLS-1$
                        parser.parse(contents, this);
                } catch (StopParsingException e) {
                        // Abort the parsing normally. Fall through...
                }
                return true;
        }

        /*
         * Resolve external entity definitions to an empty string. This is to speed
         * up processing of files with external DTDs. Not resolving the contents of
         * the DTD is ok, as only the System ID of the DTD declaration is used.
         *
         * @see org.xml.sax.helpers.DefaultHandler#resolveEntity(java.lang.String,
         *      java.lang.String)
         */
        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
                return new InputSource(new StringReader("")); //$NON-NLS-1$
        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ext.LexicalHandler#startCDATA()
         */
        public final void startCDATA() {
                // Not interested.
        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ext.LexicalHandler#startDTD(java.lang.String,
         *      java.lang.String, java.lang.String)
         */
        public final void startDTD(final String name, final String publicId, final String systemId) throws SAXException {
         // Not interested.
        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
         *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public final void startElement(final String uri, final String elementName, final String qualifiedName, final Attributes attributes) throws SAXException {
                elementFound = elementName == null || elementName.length() == 0 ? qualifiedName: elementName;
                if(elementFound.indexOf(':') != -1){
                        elementFound = elementFound.substring(elementFound.indexOf(':')+1);
                }
                attributesFound = attributes;
                throw new StopParsingException();
        }

        /*
         * (non-Javadoc)
         *
         * @see org.xml.sax.ext.LexicalHandler#startEntity(java.lang.String)
         */
        public final void startEntity(final String name) {
                // Not interested.
        }

        /**
         * @return
         */
        public int getHashValue() {

            String hash = attributesFound.getValue("ConfigHash");
            if (hash != null)
                return new Integer(hash);
            return 0;
        }

}


    protected class PluginConfigQuickPeek  {

            private static final int UNSET = -2;

            private static final int UNKNOWN = -1;

            private XMLRootHandler handler = null;

            private int hash = UNSET;

            public PluginConfigQuickPeek(InputStream in) {
                    if (in != null) {
                            try {
                                    InputSource inputSource = new InputSource(in);
                                    handler = new XMLRootHandler();
                                    handler.parseContents(inputSource);
                            } catch (Exception ex) {
                                    // ignore
                            } finally {
                                    try {
                                            in.reset();
                                    } catch (IOException ex) {
                                            // ignore
                                    }
                            }
                    } else {
                            hash = UNKNOWN;
                    }
            }

            /**
             * Returns the hash value
             *
             * @return
             */
            public int getHashValue() {
                    if (hash == UNSET) {
                            hash = handler.getHashValue();

                            if (hash == UNSET) {
                                    hash = UNKNOWN;
                            }
                    }
                    return hash;
            }


            public void setHashValue(int hashValue) {
                    this.hash = hashValue;
            }



    }

    protected static class ClusterUriGroup {
        protected String clusterName;
        protected String uriGroupName;
        protected String vhostName;

        protected ClusterUriGroup(String vhostName, String clusterName, String uriGroupName) {
            this.vhostName = vhostName;
            this.clusterName = clusterName;
            this.uriGroupName = uriGroupName;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((clusterName == null) ? 0 : clusterName.hashCode());
            result = prime * result + ((uriGroupName == null) ? 0 : uriGroupName.hashCode());
            return result;
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ClusterUriGroup other = (ClusterUriGroup) obj;
            if (clusterName == null) {
                if (other.clusterName != null)
                    return false;
            } else if (!clusterName.equals(other.clusterName))
                return false;
            if (uriGroupName == null) {
                if (other.uriGroupName != null)
                    return false;
            } else if (!uriGroupName.equals(other.uriGroupName))
                return false;
            return true;
        };

    }

    protected static class VHostData {
        protected final String host;
        protected final int port;

        protected VHostData(String inHost, int inPort) throws UnknownHostException {
            this.host = inHost;
            this.port = inPort;
        }

        protected VHostData(String alias) throws UnknownHostException {
            int lastIndex = alias.lastIndexOf(':');
            this.host = alias.substring(0, lastIndex);
            this.port = Integer.valueOf(alias.substring(lastIndex + 1));
        }

        public String toString() {
            return "vhost(http=" + host + ':' + port + ")";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((host == null) ? 0 : host.hashCode());
            result = prime * result + port;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            VHostData other = (VHostData) obj;
            if (host == null) {
                if (other.host != null)
                    return false;
            } else if (!host.equals(other.host))
                return false;
            if (port != other.port)
                return false;
            return true;
        }
    }

    // contains data for each uri entry
    protected static class URIData {
        protected String uriName;
        protected String cookieName;
        protected String urlCookieName;

        /**
         * Constructor.
         *
         * @param uName
         * @param cName
         * @param urlName
         */
        public URIData(String uName, String cName, String urlName) {
            uriName = uName;
            cookieName = cName;
            urlCookieName = urlName;
        }

        /**
         * Debug print this class.
         *
         * @param trace
         */
        public void print(TraceComponent trace) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(trace, "URIData details:");
                Tr.debug(trace, "   uriName       : " + uriName);
                Tr.debug(trace, "   cookieName    : " + cookieName);
                Tr.debug(trace, "   urlCookieName : " + urlCookieName);
            }
        }

        public int hashCode() {
            int result = uriName.hashCode();
            result = 31 * result + cookieName.hashCode();
            result = 31 * result + urlCookieName.hashCode();
            return result;
        }

        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (null == obj || !(obj instanceof URIData))
                return false;
            URIData that = (URIData) obj;
            return this.uriName.equals(that.uriName);
        }
    }

    // contains config data for each plugin config file
    protected static class PluginConfigData {
        // hard-coded properties
        public Integer maxConnections = Integer.valueOf(-1);
        protected String LogFile = "http_plugin.log";
        protected String LogLevel = "Error";
        protected Boolean ESIEnable = Boolean.TRUE;
        protected Integer ESIMaxCacheSize = Integer.valueOf(1024);
        protected Boolean ESIInvalidationMonitor = Boolean.FALSE;
        protected Boolean ESIEnableToPassCookies = Boolean.FALSE;
        protected String TempPluginConfigFileName = ".plugin-cfg.xml";
        protected String PluginConfigFileName = "plugin-cfg.xml";
        protected String loadBalance = "Round Robin";
        protected Boolean ignoreAffinityRequests = Boolean.TRUE;
        protected Integer retryInterval = Integer.valueOf(60);
        protected Boolean removeSpecialHeaders = Boolean.TRUE;
        protected Boolean cloneSeparatorChange = Boolean.FALSE;
        protected Integer postSizeLimit = Integer.valueOf(-1);
        protected Integer postBufferSize = Integer.valueOf(0);
        protected Boolean GetDWLMTable = Boolean.FALSE;
        protected Integer HTTPMaxHeaders = Integer.valueOf(300);
        protected Boolean TrustedProxyEnable = null;
        protected String[] TrustedProxyGroup = null;

        // properties from server configuration -  see metatype-mbeans.properties file
        // properties that exist in metatype file should not have defaults specified here
        // properties with no defaults may be null
        protected String PluginInstallRoot = null;
        protected String webServerName = null;
        protected Integer webServerHttpPort = 70;
        protected Integer webServerHttpsPort = 343;
        protected String KeyringLocation = null;
        protected String StashfileLocation = null;
        protected String CertLabel = null; // no default
        protected Boolean IPv6Preferred = null;
        protected String httpEndpointPid = null;
        protected Long serverIOTimeout = null;
        protected Long wsServerIOTimeout = null; //optional
        protected Long wsServerIdleTimeout = null; //optional
        public Long connectTimeout = null;
        public Boolean extendedHandshake = null;
        public Boolean waitForContinue = null;
        protected String LogFileName = null;
        protected String LogDirLocation = null; //142740
        protected Integer serverIOTimeoutRetry = null;
        protected Hashtable<String, String> extraConfigProperties = new Hashtable<String, String>();
        protected Integer loadBalanceWeight = null;
        protected Role roleKind = null;

        protected PluginConfigData() {
            // nothing
        }

        protected PluginConfigData(Map<String, Object> config) {
            PluginInstallRoot = (String) config.get("pluginInstallRoot");
            webServerName = (String) config.get("webserverName");
            webServerHttpPort = MetatypeUtils.parseInteger(PLUGIN_CFG_ALIAS,
                                                           "webserverPort",
                                                           config.get("webserverPort"),
                                                           webServerHttpPort);
            webServerHttpsPort = MetatypeUtils.parseInteger(PLUGIN_CFG_ALIAS,
                                                            "webserverSecurePort",
                                                            config.get("webserverSecurePort"),
                                                            webServerHttpsPort);
            KeyringLocation = (String) config.get("sslKeyringLocation");
            StashfileLocation = (String) config.get("sslStashfileLocation");
            CertLabel = (String) config.get("sslCertlabel");
            IPv6Preferred = (Boolean) config.get("ipv6Preferred");
            httpEndpointPid = (String) config.get("httpEndpointRef");
            serverIOTimeout = (Long) config.get("serverIOTimeout");
            wsServerIOTimeout = (Long) config.get("wsServerIOTimeout");
            wsServerIdleTimeout = (Long) config.get("wsServerIdleTimeout");
            connectTimeout = (Long) config.get("connectTimeout");
            extendedHandshake = (Boolean) config.get("extendedHandshake");
            waitForContinue = (Boolean) config.get("waitForContinue");
            LogFileName = (String) config.get("logFileName");
            LogDirLocation = (String) config.get("logDirLocation"); //142740
            serverIOTimeoutRetry = (Integer) config.get("serverIOTimeoutRetry");
            loadBalanceWeight = (Integer) config.get("loadBalanceWeight");
            //config.get("serverRole") in a server should not return null; sanity check since we are using equals.
            roleKind = (config.get("serverRole") != null && ((String) config.get("serverRole")).equals("BACKUP")) ? Role.SECONDARY : Role.PRIMARY;
            // PI76699 if the following ESI values are set in server.xml they will override default values.
            if (config.get("ESIEnable") != null) {
                ESIEnable = (Boolean) config.get("ESIEnable");
            }
            if (config.get("ESIMaxCacheSize") != null) {
                ESIMaxCacheSize = (Integer) config.get("ESIMaxCacheSize");
            }
            if (config.get("ESIInvalidationMonitor") != null) {
                ESIInvalidationMonitor = (Boolean) config.get("ESIInvalidationMonitor");
            }
            if (config.get("ESIEnableToPassCookies") != null) {
                ESIEnableToPassCookies = (Boolean) config.get("ESIEnableToPassCookies");
            } // PI76699 End

            TrustedProxyEnable = (Boolean) config.get("trustedProxyEnable");
            String proxyList = (String) config.get("trustedProxyGroup");
            if (proxyList != null) {
                TrustedProxyGroup = proxyList.split(",");
            }

            // populate extra properties map with default values but allow override from user config
            extraConfigProperties.put("ASDisableNagle", "false");
            extraConfigProperties.put("AcceptAllContent", "false");
            extraConfigProperties.put("AppServerPortPreference", "HostHeader");
            extraConfigProperties.put("ChunkedResponse", "false");
            extraConfigProperties.put("FIPSEnable", "false");
            extraConfigProperties.put("IISDisableNagle", "false");
            //
            extraConfigProperties.put("IISPluginPriority", "High");
            extraConfigProperties.put("IgnoreDNSFailures", "false");
            extraConfigProperties.put("RefreshInterval", "60");
            extraConfigProperties.put("ResponseChunkSize", "64");
            extraConfigProperties.put("SSLConsolidate", "false");
            extraConfigProperties.put("TrustedProxyEnable", "false");
            extraConfigProperties.put("VHostMatchingCompat", "false");

            // check for any extra properties (may be specified by IBM Support to address Plug-in issues)
            if (config.get("extraConfigProperties.0.config.referenceType") != null) {
                for (Map.Entry<String, Object> entry : config.entrySet()) {
                    if (entry.getKey().startsWith("extraConfigProperties.0") && !entry.getKey().equals("extraConfigProperties.0.config.referenceType")) {
                        String value = (String) config.get(entry.getKey());
                        // remove the key prefix of extraConfigProperties.0.
                        String key = entry.getKey().substring(24);
                        extraConfigProperties.put(key, value);
                    }
                }
            }
        }

        protected ServerClusterData createServerCluster(String cName, boolean singleServerConfig) {
            ServerClusterData scd = new ServerClusterData(cName, singleServerConfig);
            scd.loadBalance = this.loadBalance;
            scd.retryInterval = this.retryInterval;
            scd.serverIOTimeoutRetry = this.serverIOTimeoutRetry;
            scd.removeSpecialHeaders = this.removeSpecialHeaders;
            scd.cloneSeparatorChange = this.cloneSeparatorChange;
            scd.postSizeLimit = this.postSizeLimit;
            scd.postBufferSize = this.postBufferSize;
            scd.GetDWLMTable = this.GetDWLMTable;
            return scd;
        }

        /**
         * Debug print this class.
         *
         * @param trace
         */
        public void print(TraceComponent trace) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(trace, "PluginConfigData details:");
                Tr.debug(trace, "   PluginInstallRoot       : " + PluginInstallRoot);
                Tr.debug(trace, "   PluginConfigFileName    : " + PluginConfigFileName);
                Tr.debug(trace, "   httpEndpointRef         : " + httpEndpointPid);
                Tr.debug(trace, "   webserver http port     : " + webServerHttpPort);
                Tr.debug(trace, "   webserver https port    : " + webServerHttpsPort);
                Tr.debug(trace, "   LogFile                 : " + LogFile);
                Tr.debug(trace, "   LogLevel                : " + LogLevel);
                Tr.debug(trace, "   LogDirLocation         : " + LogDirLocation);
                Tr.debug(trace, "   ESIEnable               : " + ESIEnable);
                Tr.debug(trace, "   ESIMaxCacheSize         : " + ESIMaxCacheSize);
                Tr.debug(trace, "   ESIInvalidationMonitor  : " + ESIInvalidationMonitor);
                Tr.debug(trace, "   ESIEnableToPassCookies  : " + ESIEnableToPassCookies);
                Tr.debug(trace, "   loadBalance             : " + loadBalance);
                Tr.debug(trace, "   IgnoreAffinityRequests  : " + ignoreAffinityRequests);
                Tr.debug(trace, "   retryInterval           : " + retryInterval);
                Tr.debug(trace, "   serverIOTimeoutRetry    : " + serverIOTimeoutRetry);
                Tr.debug(trace, "   removeSpecialHeaders    : " + removeSpecialHeaders);
                Tr.debug(trace, "   cloneSeparatorChange    : " + cloneSeparatorChange);
                Tr.debug(trace, "   postSizeLimit           : " + postSizeLimit);
                Tr.debug(trace, "   postBufferSize          : " + postBufferSize);
                Tr.debug(trace, "   serverIOTimeout         : " + serverIOTimeout);
                Tr.debug(trace, "   wsServerIOTimeout       : " + wsServerIOTimeout);
                Tr.debug(trace, "   wsServerIdleTimeout     : " + wsServerIdleTimeout);
                Tr.debug(trace, "   GetDWLMTable            : " + GetDWLMTable);
                Tr.debug(trace, "   HTTPMaxHeaders          : " + HTTPMaxHeaders);
                Tr.debug(trace, "   CertLabel               : " + CertLabel);
                Tr.debug(trace, "   KeyringLocation         : " + KeyringLocation);
                Tr.debug(trace, "   StashfileLocation       : " + StashfileLocation);
                Tr.debug(trace, "   TrustedProxyEnable      : " + TrustedProxyEnable);
                Tr.debug(trace, "   TrustedProxyGroup       : " + traceList(TrustedProxyGroup));
                if (!extraConfigProperties.isEmpty())
                    Tr.debug(trace, "   AdditionalConfigProps   : " + extraConfigProperties.toString());
            }
        }

        private String traceList(String[] list) {
            if (list == null || list.length == 0)
                return "none";

            StringBuilder sb = new StringBuilder();
            for (String element : list) {
                sb.append("\n\t").append(element);
            }
            return sb.toString();
        }
    }

    protected static class ServerClusterData {
        protected String clusterName;
        protected String loadBalance;
        protected Integer retryInterval;
        protected Integer serverIOTimeoutRetry;
        protected Boolean removeSpecialHeaders;
        protected Boolean cloneSeparatorChange;
        protected Boolean GetDWLMTable;
        protected Integer postSizeLimit;
        protected Integer postBufferSize;
        protected List<ServerData> clusterServers = new LinkedList<ServerData>();
        protected List<DeployedModuleData> deployedModules = new ArrayList<DeployedModuleData>();
        protected String fileServingEnabled = NOT_DEFINED;
        protected String serveServletsByClassnameEnabled = NOT_DEFINED;
        protected Boolean singleServerConfig;

        /**
         * Constructor.
         *
         * @param cName
         */
        public ServerClusterData(String cName, boolean singleServerConfig) {
            this.clusterName = cName;
            this.singleServerConfig = Boolean.valueOf(singleServerConfig);
        }

        /**
         * Debug print this class.
         *
         * @param trace
         */
        public void print(TraceComponent trace) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(trace, "ServerClusterData details:");
                Tr.debug(trace, "   clusterName                     : " + clusterName);
                Tr.debug(trace, "   loadBalance                     : " + loadBalance);
                Tr.debug(trace, "   retryInterval                   : " + retryInterval);
                Tr.debug(trace, "   serverIOTimeoutRetry            : " + serverIOTimeoutRetry);
                Tr.debug(trace, "   removeSpecialHeaders            : " + removeSpecialHeaders);
                Tr.debug(trace, "   cloneSeparatorChange            : " + cloneSeparatorChange);
                Tr.debug(trace, "   postSizeLimit                   : " + postSizeLimit);
                Tr.debug(trace, "   postBufferSize                  : " + postBufferSize);
                Tr.debug(trace, "   clusterServers                  : " + clusterServers);
                Tr.debug(trace, "   deployedModules                 : " + deployedModules);
                Tr.debug(trace, "   fileServingEnabled              : " + fileServingEnabled);
                Tr.debug(trace, "   serveServletsByClassnameEnabled : " + serveServletsByClassnameEnabled);
                Tr.debug(trace, "   singleServerConfig              : " + singleServerConfig);
                Tr.debug(trace, "   getDWLMTable                    : " + GetDWLMTable);
            }
        }
    }

    protected static String tryDetermineHostName(final String host,
                                                 final String defaultHostName,
                                                 final boolean preferIPv6) {

        String hostName = null;
        if ("*".equals(host) && !LOCALHOST.equals(defaultHostName) && !defaultHostName.isEmpty()) {
            if (HostNameUtils.validLocalHostName(defaultHostName, preferIPv6)) {
                hostName = defaultHostName;
            } else {
                hostName = LOCALHOST;
            }
        } else {
            hostName = HostNameUtils.tryResolveHostName(host, preferIPv6);
        }

        // Bummer. Be safe.
        return hostName == null ? LOCALHOST : hostName;
    }

    /**
     * Returns a File.pathSeparator if the input String does not end in / or \\
     *
     * @param input - The String to test
     * @return A String containing a File.pathSeparator if the input String doesn't end in / or \\
     */
    @Trivial
    private String addSlash(String input) {
        if (input.endsWith("/") || input.endsWith("\\"))
            return "";
        return File.separator;
    }

    // contains data for each configured server
    protected static class ServerData {
        protected String serverName = null;
        protected String serverID = "";
        protected String cellName = null;
        protected String nodeName = null;
        protected String hostName = null;
        protected List<TransportData> transports = new LinkedList<TransportData>();
        protected Long connectTimeout = Long.valueOf(5);
        protected Long serverIOTimeout = Long.valueOf(0);
        protected Long wsServerIOTimeout = null;//optional
        protected Long wsServerIdleTimeout = null;//optional
        protected Boolean waitForContinue = Boolean.FALSE;
        protected Integer maxConnections = Integer.valueOf(-1);
        protected Boolean extendedHandshake = Boolean.FALSE;
        protected Role roleKind = Role.PRIMARY;
        protected String sessionManagerCookieName = "JSESSIONID";
        protected String sessionURLIdentifier = "jsessionid";
        protected String cloneSeparator = null;
        protected String fileServingEnabled = NOT_DEFINED;
        protected String serveServletsByClassnameEnabled = NOT_DEFINED;
        protected Integer loadBalanceWeight = Integer.valueOf(20); //20 is the default value in Liberty

        /**
         * Constructor.
         *
         * @param name
         */
        public ServerData(String name, String serverID, PluginConfigData pcd) {
            this.serverName = name;
            if (serverID != null) {
                this.serverID = serverID;
            }
            this.nodeName = null;
            this.serverIOTimeout = pcd.serverIOTimeout;
            this.wsServerIOTimeout = pcd.wsServerIOTimeout;
            this.wsServerIdleTimeout = pcd.wsServerIdleTimeout;
            this.connectTimeout = pcd.connectTimeout;
            this.waitForContinue = pcd.waitForContinue;
            this.maxConnections = pcd.maxConnections;
            this.extendedHandshake = pcd.extendedHandshake;
            this.loadBalanceWeight = pcd.loadBalanceWeight;
            this.roleKind = pcd.roleKind;
        }

        protected TransportData addTransportData(String hostName, int port, boolean isSecurity) {
            TransportData td = new TransportData(hostName, port, isSecurity);
            this.transports.add(td);
            return td;
        }

        /**
         * Debug print this class.
         *
         * @param trace
         */
        public void print(TraceComponent trace) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(trace, "ServerData details:");
                Tr.debug(trace, "   cellName                        : " + cellName);
                Tr.debug(trace, "   nodeName                        : " + nodeName);
                Tr.debug(trace, "   serverName                      : " + serverName);
                Tr.debug(trace, "   serverID                        : " + serverID);
                Tr.debug(trace, "   hostName                        : " + hostName);
                Tr.debug(trace, "   transports                      : " + transports);
                Tr.debug(trace, "   connectTimeout                  : " + connectTimeout);
                Tr.debug(trace, "   serverIOTimeout                 : " + serverIOTimeout);
                Tr.debug(trace, "   wsServerIOTimeout               : " + wsServerIOTimeout);
                Tr.debug(trace, "   wsServerIdleTimeout             : " + wsServerIdleTimeout);
                Tr.debug(trace, "   waitForContinue                 : " + waitForContinue);
                Tr.debug(trace, "   maxConnections                  : " + maxConnections);
                Tr.debug(trace, "   extendedHandshake               : " + extendedHandshake);
                Tr.debug(trace, "   roleKind                        : " + roleKind);
                Tr.debug(trace, "   sessionManagerCookieName        : " + sessionManagerCookieName);
                Tr.debug(trace, "   sessionURLIdentifier            : " + sessionURLIdentifier);
                Tr.debug(trace, "   cloneSeparator                  : " + cloneSeparator);
                Tr.debug(trace, "   fileServingEnabled              : " + fileServingEnabled);
                Tr.debug(trace, "   serveServletsByClassnameEnabled : " + serveServletsByClassnameEnabled);
                Tr.debug(trace, "   loadBalanceWeight               : " + loadBalanceWeight);
            }
        }
    }

    // contains data for each Transport
    protected static class TransportData {
        protected String host;
        protected int port;
        protected boolean isSslEnabled;

        /**
         * Constructor.
         *
         * @param hostName
         * @param portNumber
         * @param sslEnabled
         */
        public TransportData(String hostName, int portNumber, boolean sslEnabled) {
            this.host = hostName;
            this.port = portNumber;
            this.isSslEnabled = sslEnabled;
        }

        /**
         * Debug print this class.
         *
         * @param trace
         */
        public void print(TraceComponent trace) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(trace, "TransportData details:");
                Tr.debug(trace, "   host        : " + host);
                Tr.debug(trace, "   port        : " + port);
                Tr.debug(trace, "   sslEnabled  : " + isSslEnabled);
            }
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (null == o || !(o instanceof TransportData)) {
                return false;
            }
            TransportData td = (TransportData) o;
            return (td.host.equals(host) && (td.port == port) && (td.isSslEnabled == isSslEnabled));

        }

        public int hashCode() {
            return host.hashCode() + port;
        }

        public String toString() {
            return "transportData(host=" + host + ", port=" + port + ", isSSL=" + isSslEnabled + ")";
        }
    }

    // contains data for each deployed module
    protected static class DeployedModuleData {
        protected WebApp app = null;
        protected WebAppConfiguration moduleConfig;
        // pull actual session config
        protected String cookieName = "JSESSIONID";
        protected String urlCookieName = "jsessionid";
        protected List<String> additionalPatterns = null;

        protected DeployedModuleData(WebApp application, String defaultAffinityCookie, String affinityUrlIdentifier) {
            this.app = application;
            this.moduleConfig = application.getConfiguration();
            SessionCookieConfig cookieConfig = null;
            try {
                cookieConfig = application.getSessionCookieConfig();
            } catch (UnsupportedOperationException exc) {
                // ignore this exception, app is initializing so too early to get a cokkie config name
                // and default can be assumed.
            }
            if (cookieConfig == null) {
                /*-
                 * The SessionCookieConfig is not available if
                 * the ServletContext for the application has not been initialized.
                 *
                 * This typically occurs when the plug-in configuration is generated
                 * before at least one request has been submitted to the application.
                 *
                 * Use the default (server-level) cookie name when this occurs.
                 */
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The application named " + application.getName()
                                 + " has not been initialized yet, so the plugin configuration will use the default (server-level) affinity cookie. If this application programmatically modifies the affinity cookie's name during initialization, that change will not be reflected in the plugin configuration unless the plugin configuration is regenerated after application initialization.");
                }
                if (defaultAffinityCookie != null) {
                    this.cookieName = defaultAffinityCookie;
                }
            } else {
                String cookieName = cookieConfig.getName();
                if (cookieName != null) {
                    this.cookieName = cookieName;
                }
            }
            if (affinityUrlIdentifier != null) {
                this.urlCookieName = affinityUrlIdentifier;
            }
        }

        /**
         * Debug print this class.
         *
         * @param trace
         */
        public void print(TraceComponent trace) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(trace, "DeploymentModuleData details:");
                Tr.debug(trace, "   moduleConfig         : " + moduleConfig);
                Tr.debug(trace, "   cookieName           : " + cookieName);
                Tr.debug(trace, "   urlCookieName        : " + urlCookieName);
                Tr.debug(trace, "   additionalPatterns   : " + additionalPatterns);
            }
        }
    }

    /**
     * Take the pid received from configuration, use it to retrieve the HttpEndpoint service
     * reference, and then make use of the service properties.
     */
    protected static class HttpEndpointInfo {
        private final String httpEndpointId;
        private final ServiceReference<?> httpEndpointRef;

        HttpEndpointInfo(BundleContext context, Document doc, String pid) {
            ServiceReference<?> serviceRef = null;

            // First pass at finding the http endpoint reference based on the pid parameter
            String idFilter = "(service.pid=" + pid + ")";

            if (pid == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "HttpEndpointInfo -- no endpoint specified in config");
                }
                idFilter = "(id=defaultHttpEndpoint)";
            }

            String filter = "(&(enabled=true)(|(httpPort>=1)(httpsPort>=1))" + idFilter + ")";
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "HttpEndpointInfo -- looking for configured endpoints matching " + filter);
            }

            serviceRef = getService(context, filter);
            if (serviceRef == null) {
                // we couldn't find the service matching the above filter, HOWEVER..
                // we do know that the service exists (because config matched it to a pid),
                // it just must not be listening or enabled (didn't match the rest of the filter).
                // Look for just the idFilter so we can find the service to at least give a better
                // indication of why that one wasn't used.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "HttpEndpointInfo -- no enabled/listening endpoints found");
                }
                ServiceReference<?> ref = getService(context, idFilter);
                if (ref != null) {
                    String id = (String) ref.getProperty("id");
                    Comment comment = doc.createComment(String.format(" The endpoint %s was not found or is not enabled",
                                                                      id));
                    doc.getDocumentElement().appendChild(comment);
                } else {
                    Comment comment = doc.createComment(String.format(" No endpoint could be found with pid %s.",
                                                                      pid));
                    doc.getDocumentElement().appendChild(comment);
                }
            }

            // Second pass, if needed: the specified pid wasn't enabled/listening
            if (serviceRef == null) {
                // first, lets see if we can find one with both ports listening
                serviceRef = getService(context, "(&(enabled=true)(httpPort>=1)(httpsPort>=1))");
                if (serviceRef == null) {
                    // otherwise, try for one with at least one port listening
                    serviceRef = getService(context, "(&(enabled=true)(|(httpPort>=1)(httpsPort>=1)))");
                }
            }

            if (serviceRef != null) {
                httpEndpointRef = serviceRef;
                httpEndpointId = (String) serviceRef.getProperty("id");
                Comment comment = doc.createComment(" Configuration generated using httpEndpointRef=" + httpEndpointId);
                doc.getDocumentElement().appendChild(comment);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "HttpEndpointInfo -- could not find a valid endpoint");
                }
                httpEndpointRef = null;
                httpEndpointId = "defaultHttpEndpoint";
            }
        }

        /**
         * @param key Service property to retrieve
         * @return
         */
        public Object getProperty(String key) {
            if (httpEndpointRef != null)
                return httpEndpointRef.getProperty(key);
            return null;
        }

        /**
         * @return
         */
        public boolean isValid() {
            return httpEndpointRef != null;
        }

        /**
         * @return
         */
        public Object getEndpointId() {
            return httpEndpointId;
        }

        @FFDCIgnore(InvalidSyntaxException.class)
        ServiceReference<?> getService(BundleContext context, String filter) {
            ServiceReference<?> refs[] = null;
            try {
                refs = context.getAllServiceReferences(null, filter);
            } catch (InvalidSyntaxException e) {
            }
            if (refs == null || refs.length == 0)
                return null;
            return refs[0];
        }
    }
}
