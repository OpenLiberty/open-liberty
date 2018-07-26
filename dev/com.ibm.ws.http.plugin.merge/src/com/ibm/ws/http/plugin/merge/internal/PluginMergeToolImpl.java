/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.plugin.merge.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.http.plugin.merge.PluginMergeTool;

/**
 * Utility to merge multiple WebSphere generated plugin-cfg.xml files into a single file.
 * Ported from tWAS:
 * SERV1/ws/code/plugincfg/src/com/ibm/websphere/plugincfg/generator/PluginMergeTool.java WASX.SERV1
 *
 * @author bparees
 */
@Component(service = { PluginMergeTool.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM" })
public class PluginMergeToolImpl implements PluginMergeTool {
    private static final TraceComponent traceComponent = Tr.register(PluginMergeToolImpl.class);
    private static final String NO_MERGE_ERR = "Error encountered, no merged file was written";

    private boolean isXdOnly = true;
    private boolean debug = false;
    private int seqNum = 0;
    private final boolean failOver = true; /* 654526 */
    private static boolean precedence = false;
    private Element mergeConfigNode;
    private static Element mergeConfigNode2; //PI07230
    private PluginInfo[] plugins;
    private final ArrayList<PluginInfo> sharedPlugins = new ArrayList<PluginInfo>();
    private final HashSet<String> emptyServerClusters = new HashSet<String>();
    private String encoding = null;
    private String tc = null; //PM25128
    private boolean sortVhostGrp = false; //PM25128
    private int paramCnt = 0; //PM25128
    public ArrayList<String> G_primaryServers = new ArrayList<String>(); //PM38368
    public ArrayList<String> G_backupServers = new ArrayList<String>(); //720290
    private boolean setMatchUriAppVhost = false; //PM64667

    /*
     * JVM prop "com.ibm.ws.pluginmerge.match.appname"
     * if set to "true" the uniqueness test for a shared app will be (uri, appName, vhost)
     * else uniqueness test will be (uri, vhost)
     *
     * NOTE: This will only ever work for ODC generated plugin-cfg.xml files
     */
    private static boolean matchUriAppVhost = false;

    public PluginMergeToolImpl() {}

    @Activate
    protected void activate(ComponentContext cc) {}

    @Deactivate
    protected void deactivate(ComponentContext cc) {}

    /**
     * Removes all comments from the document except for the Properties comment
     * All exceptions are suppressed because this is just a nicety to improve human readability.
     *
     * @param parser
     * @param doc
     * @return
     */
    private boolean removeComments(DocumentBuilder parser, Document doc) {
        try {
            // Check for the traversal module
            DOMImplementation impl = parser.getDOMImplementation();
            if (!impl.hasFeature("traversal", "2.0")) {
                // DOM implementation does not support traversal unable to remove comments
                return false;
            }

            Node root = doc.getDocumentElement();
            DocumentTraversal traversable = (DocumentTraversal) doc;
            NodeIterator iterator = traversable.createNodeIterator(root, NodeFilter.SHOW_COMMENT, null, true);
            Node node = null;
            while ((node = iterator.nextNode()) != null) {
                if (node.getNodeValue().trim().compareTo("Properties") != 0) {
                    root.removeChild(node);
                }
            }
        } catch (FactoryConfigurationError e) {
            return false;
        }

        return true;
    }

    /**
     * Output the finalized merged plugin-cfg.xml
     *
     * @param output - filename for output
     * @throws IOException
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws ParserConfigurationException
     */
    private void printMergedCopy(String output) throws IOException, ParserConfigurationException, TransformerConfigurationException, TransformerException {
        tc = "printMergedCopy - ";
        if (debug)
            Tr.info(traceComponent, tc + "Output File: " + output);
        cleanPlugins();
        FileOutputStream fos = new FileOutputStream(output);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = dbf.newDocumentBuilder();
        Document mergeDoc = parser.newDocument();
        final Comment comment = mergeDoc.createComment(" This config file was generated by plugin's merge tool v1.0.0.2 on " +
                                                       new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z").format(new Date()) + " ");
        mergeDoc.appendChild(comment);

        mergeDoc.appendChild(mergeDoc.importNode(mergeConfigNode, true));
        mergeConfigNode = mergeDoc.getDocumentElement();
        removeComments(parser, mergeDoc);

        mergeConfigNode.appendChild(mergeDoc.importNode(mergeDoc.createComment(" Server Clusters "), true));

        Node[] sc = null;
        sharedPlugins.trimToSize();
        Iterator<PluginInfo> itrSharedPlugins = sharedPlugins.iterator();
        while (itrSharedPlugins.hasNext()) {
            if (failOver)
                sc = (itrSharedPlugins.next()).getFailOverServerClusters();
            else
                sc = (itrSharedPlugins.next()).getSharedServerClusters();

            for (int i = 0; i < sc.length; i++)
                mergeConfigNode.appendChild(mergeDoc.importNode(sc[i], true));
        }

        for (int i = 0; i < plugins.length - paramCnt; i++) {
            if (!plugins[i].getUniquePluginRep().isEmpty()) {
                sc = plugins[i].getUnsharedServerClusters();
                for (int j = 0; j < sc.length; j++)
                    mergeConfigNode.appendChild(mergeDoc.importNode(sc[j], true));
            }
        }

        mergeConfigNode.appendChild(mergeDoc.createComment(" Virtual Host Groups "));

        Node[] vhgs = null;
        itrSharedPlugins = sharedPlugins.iterator();
        while (itrSharedPlugins.hasNext()) {
            vhgs = (itrSharedPlugins.next()).getSharedVHostGrps();
            for (int i = 0; i < vhgs.length; i++) {
                mergeConfigNode.appendChild(mergeDoc.importNode(vhgs[i], true));
            }
        }

        vhgs = null;
        for (int i = 0; i < plugins.length - paramCnt; i++) {
            vhgs = plugins[i].getUnsharedVHostGrp();
            for (int j = 0; j < vhgs.length; j++)
                mergeConfigNode.appendChild(mergeDoc.importNode(vhgs[j], true));
        }

        mergeConfigNode.appendChild(mergeDoc.createComment(" URI Groups "));

        Node[] uriGrps = null;
        itrSharedPlugins = sharedPlugins.iterator();
        while (itrSharedPlugins.hasNext()) {
            uriGrps = (itrSharedPlugins.next()).getUriGrps();
            for (int i = 0; i < uriGrps.length; i++)
                mergeConfigNode.appendChild(mergeDoc.importNode(uriGrps[i], true));
        }

        for (int i = 0; i < plugins.length - paramCnt; i++) {
            uriGrps = plugins[i].getUriGrps();
            for (int j = 0; j < uriGrps.length; j++)
                mergeConfigNode.appendChild(mergeDoc.importNode(uriGrps[j], true));

            //output unqiueUriGrps needed due to vhost def
            uriGrps = plugins[i].getUniqueUriGrps();
            for (int j = 0; j < uriGrps.length; j++)
                mergeConfigNode.appendChild(mergeDoc.importNode(uriGrps[j], true));
        }

        mergeConfigNode.appendChild(mergeDoc.createComment(" Routes "));

        Node[] routes = null;
        itrSharedPlugins = sharedPlugins.iterator();
        while (itrSharedPlugins.hasNext()) {
            routes = (itrSharedPlugins.next()).getRoutes();
            for (int i = 0; i < routes.length; i++)
                mergeConfigNode.appendChild(mergeDoc.importNode(routes[i], true));
        }

        for (int i = 0; i < plugins.length - paramCnt; i++) {
            routes = plugins[i].getRoutes();
            for (int j = 0; j < routes.length; j++)
                mergeConfigNode.appendChild(mergeDoc.importNode(routes[j], true));
        }

        TransformerFactory transfac = TransformerFactory.newInstance();

        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.INDENT, "yes");
        trans.setOutputProperty(OutputKeys.METHOD, "xml");
        trans.setOutputProperty(OutputKeys.ENCODING, encoding);
        trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        //create string from xml tree
        StreamResult result = new StreamResult(fos);
        DOMSource source = new DOMSource(mergeDoc);
        trans.transform(source, result);

        fos.flush();
        fos.close();

        Tr.info(traceComponent, "Merged plugin config file written to " + output);
    }

    private void cleanPlugins() {
        sharedPlugins.trimToSize();
        Iterator<PluginInfo> itrSharedPlugins = sharedPlugins.iterator();
        try {
            while (itrSharedPlugins.hasNext())
                ((itrSharedPlugins.next())).cleanPlugin();

            for (int i = 0; i < plugins.length - paramCnt; i++)
                plugins[i].cleanPlugin();
        } catch (ParserConfigurationException e) {

            Tr.info(traceComponent, NO_MERGE_ERR);
            throw new RuntimeException(e);
        }
    }

    /*
     * Cycle through and compare all AppInfo objects nested in PluginInfo objects.
     * When an AppInfo object is matched create a new AppInfo object and store it in a shared PluginInfo object,
     * deleting the original AppInfo objects that were matched. The shared AppInfo objects contain a list of the servers
     * that map to a matched uri, during cleanup these are used to build server cluster definitions to ensure that matched uris
     * map to a shared server cluster that only contain servers that actually host that uri.
     */
    private void lfMerge() throws ParserConfigurationException {
        tc = "lfMerge - ";
        if (debug)
            Tr.info(traceComponent, tc + "Merging plugins.");
        Iterator<PluginInfo> itrShared = null;
        // p2 - UniquePluginRep - always an input plugin-cfg.xml
        // p1 - UniquePluginRep - may be shared plugin or an input plugin found to the left of input plugin-cfg.xml held by p2
        Hashtable<String, AppInfo> p1, p2 = null;
        // place holders for all info related to the uid
        AppInfo p1AppInfo, p2AppInfo = null;
        Enumeration<String> p1Uids = null;
        // String uid - unique identifier - uri/vhostdef
        String uid = null;
        // loop thru all input plugin-cfg.xml files starting at the second input plugin-cfg.xml
        // inner loop will take care of matching the first input plugin-cfg.xml
        if (debug)
            Tr.info(traceComponent, tc + "Looping through all input files");
        for (int i = 1; i < plugins.length - paramCnt; i++) {
            p2 = plugins[i].getUniquePluginRep();

            sharedPlugins.trimToSize();
            PluginInfo sharedPlugin = null;
            itrShared = sharedPlugins.iterator();
            // if there is a shared plugin representation compare them to the selected input plugin (p2) first
            // this ensures uids that have already been noted as being shared between input plugins correctly end up in a shared group
            while (itrShared.hasNext()) {
                sharedPlugin = (itrShared.next());
                p1 = sharedPlugin.getUniquePluginRep();
                p1Uids = p1.keys();

                // loop thru the shared uids
                while (p1Uids.hasMoreElements()) {
                    uid = p1Uids.nextElement();
                    if (debug)
                        Tr.info(traceComponent, tc + "UID: " + uid);
                    if (p2.containsKey(uid)) {
                        if (debug)
                            Tr.info(traceComponent, tc + "Adding UID to shared: " + uid);
                        p2AppInfo = p2.get(uid);
                        // add the servers from the input plugin-cfg.xml that correspond to the shared uid
                        // to the shared uid in the shared plugin representation
                        sharedPlugin.addSharedServers(plugins[i].getSeqNum(), p2AppInfo.getServerCluster(), (p1.get(uid)), true); /* 654526 */
                        // remove the uid from the input plugin-cfg.xml representation as it's now handles be a shared plugin representation
                        p2.remove(uid);
                    }
                }
            }

            // after all uids that are already known to be shared have been removed from the input plugin-cfg.xml
            // compare the input plugin-cfg.xml being processed to all the input plugin-cfg.xml files to it's left
            // this will result in at most one new shared plugin representation
            PluginInfo newSharedPlugin = null;
            for (int j = 0; j < i; j++) {
                p1 = plugins[j].getUniquePluginRep();
                p1Uids = p1.keys();
                while (p1Uids.hasMoreElements()) {
                    uid = p1Uids.nextElement();
                    // if a common uid is found
                    if (p2.containsKey(uid)) {
                        p1AppInfo = p1.get(uid);
                        p2AppInfo = p2.get(uid);
                        // create a new shared plugin representation unless one was already created
                        // when processing the current input plugin-cfg.xml (p2)
                        if (newSharedPlugin == null) {
                            newSharedPlugin = new PluginInfo(seqNum, p1AppInfo.getServerCluster());
                            seqNum++;
                        }

                        // add the app info to the new shared plugin-cfg.xml
                        // if the uid has already been matched once the servers corresposnding to this uid will be updated
                        newSharedPlugin.addMatch(uid, p1AppInfo.getAppName(), p1AppInfo.getServerCluster(), plugins[j].getSeqNum(),
                                                 p2AppInfo.getServerCluster(), plugins[i].getSeqNum(), p1AppInfo.getUri(), p1AppInfo.getVh());

                        p1.remove(uid);
                        p2.remove(uid);
                    }
                }
                // for all unique uids that remain in the two input plugin-cfg.xmls being processed compare the uris
                // if a uri is found in one that is not in the other mark that uid as needing a unique vhostGrp
                // because that uri was not found to be common due to vhost definition
                //
                //maybe rewrite setReasonForUniqueness()
                //so that it doesn't need to be called twice to ensure that something wasn't skipped
                setReasonForUniqueness(plugins[j], p2);
                setReasonForUniqueness(plugins[i], p1);
                tc = "lfMerge - ";
            }

            // if a newShared plugin was created when comparing input plugin-cfg.xml files add it to the know sharedPlugins list
            if (newSharedPlugin != null)
                sharedPlugins.add(newSharedPlugin);
            if (debug)
                Tr.info(traceComponent, " ");
        }
    }

    private void pMerge() {
        for (int i = 1; i < plugins.length - paramCnt; i++) {
            for (int j = 0; j < i; j++) {
                Hashtable<String, AppInfo> p1 = plugins[j].getUniquePluginRep();
                Hashtable<String, AppInfo> p2 = plugins[i].getUniquePluginRep();

                Enumeration<String> p1Uids = p1.keys();
                while (p1Uids.hasMoreElements()) {
                    String uid = p1Uids.nextElement();
                    if (p2.containsKey(uid))
                        p2.remove(uid);
                }
                //maybe rewrite setReasonForUniqueness()
                //so that it doesn't need to be called twice to ensure that something wasn't skipped
                setReasonForUniqueness(plugins[j], p2);
                setReasonForUniqueness(plugins[i], p1);
            }
        }
    }

    private void setReasonForUniqueness(PluginInfo pgi1, Hashtable<String, AppInfo> p2) {
        tc = "setReasonForUniqueness - ";
        if (debug)
            Tr.info(traceComponent, tc + "Checking Uniqueness.");
        AppInfo info = null;
        Enumeration<AppInfo> enumP2 = p2.elements();
        while (enumP2.hasMoreElements()) {
            info = enumP2.nextElement();
            String uriName = info.getUri().getAttribute("Name");
            if (!pgi1.containedUris.contains(uriName)) {
                info.uniqueVhgNeeded = true;
            } else if (matchUriAppVhost && !pgi1.containedApps.contains(info.getAppName())) {
                info.uniqueVhgNeeded = true;
            } else {
                info.uniqueVhgNeeded = false;
            }
        }
    }

    /**
     * Removes all nodes contained in the NodeList from the Element.
     * Convenience method because NodeList objects in the DOM are live.
     *
     * @param xEml
     * @param nodes
     */
    public static void nodeListRemoveAll(Element xEml, NodeList nodes) {
        int cnt = nodes.getLength();
        for (int i = 0; i < cnt; i++)
            xEml.removeChild(nodes.item(0));
    }

    public static ArrayList<Node> nodeListToDeadArray(NodeList nodes) {
        ArrayList<Node> rtnNodes = null;
        if (nodes != null) {
            rtnNodes = new ArrayList<Node>();
            for (int i = 0; i < nodes.getLength(); i++)
                rtnNodes.add(nodes.item(i).cloneNode(true));

            rtnNodes.trimToSize();
        }

        return rtnNodes;
    }

    private void loadData(String[] files) throws SAXException, IOException, ParserConfigurationException {
        tc = "loadData - ";
        plugins = new PluginInfo[files.length - 1];
        //DOMParser parser = new DOMParser();
        int plgFile = 0;
        String fileName = null;
        //looping through files to see if debug was in the string
        for (int j = 0; j < files.length - 1; j++) {
            if (files[j].equals("-debug")) {
                debug = true;
                //break;
            }
        }
        if (debug)
            Tr.info(traceComponent, tc + "Loading parameters and files");
        for (int i = 0; i < files.length - 1; i++) {
            if (debug)
                Tr.info(traceComponent, tc + " ");
            fileName = files[i];
            //Tr.info(traceComponent,files[i]);
            //parser.parse(files[i]);
            //plugins[i] = new PluginInfo(i, parser.getDocument().getDocumentElement(), files[i]);
            if ((fileName.indexOf("-", 0)) == 0) {
                if (fileName.equals("-sortVhostGrp")) {
                    if (debug)
                        Tr.info(traceComponent, tc + "sortVhostGrp requested");
                    sortVhostGrp = true;
                }
                if (fileName.equals("-setMatchUriAppVhost")) {
                    setMatchUriAppVhost = true;
                    if (debug)
                        Tr.info(traceComponent, tc + "setMatchUriAppVhost requested");
                }
                if (fileName.equals("-debug")) {
                    if (debug)
                        Tr.info(traceComponent, tc + "debug requested");
                    // debug = true; already set above
                }
                if ((!fileName.equals("-debug")) && (!fileName.equals("-sortVhostGrp")) && (!fileName.equals("-setMatchUriAppVhost"))) {
                    Tr.info(traceComponent, "Parameter: " + fileName + " is not valid.  It will be ignored");
                }

                paramCnt++;

            } else {
                if (debug)
                    Tr.info(traceComponent, tc + "Processing file:  " + files[i]);
                File f = new File(files[i]);
                Tr.info(traceComponent, "Found file " + files[i] + ": " + f.exists());
                FileInputStream fis = new FileInputStream(f);
                /*
                 * parser.parse(new InputSource(fis));
                 */

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

                //Using factory get an instance of document builder
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document dom = db.parse(new InputSource(fis));

                //plugins[i] = new PluginInfo(i, parser.getDocument().getDocumentElement(), files[i]);

                /*
                 * plugins[plgFile] = new PluginInfo(plgFile, parser.getDocument().getDocumentElement(), files[i]);
                 */
                plugins[plgFile] = new PluginInfo(plgFile, dom.getDocumentElement(), files[i]);
                seqNum++;
                encoding = dom.getXmlEncoding();

                // convenient handle for processing the document, everything lives under the config node of the xml document
                //PI07230 mergeConfigNode = (Element) dom.getDocumentElement().cloneNode(true);
                mergeConfigNode2 = (Element) dom.getDocumentElement().cloneNode(true); //PI07230
                // all elements present in the first processed doc will remain in the merged doc expect the 4 explicitly removed below

                //PI07230 NodeList nl = mergeConfigNode.getElementsByTagName("IntelligentManagement");
                NodeList nl = mergeConfigNode2.getElementsByTagName("IntelligentManagement"); //PI07230

                if (nl.getLength() == 0) {
                    // the first plugin-cfg provided will be used as the basis for the mergeDoc
                    //if (i == 0) {  Changed to plgFile since it's now possible to send parameters as part of the command line arguments
                    if (plgFile == 0) {

                        mergeConfigNode = (Element) dom.getDocumentElement().cloneNode(true); //PI07230

                        nodeListRemoveAll(mergeConfigNode, mergeConfigNode.getElementsByTagName("ServerCluster"));
                        nodeListRemoveAll(mergeConfigNode, mergeConfigNode.getElementsByTagName("VirtualHostGroup"));
                        nodeListRemoveAll(mergeConfigNode, mergeConfigNode.getElementsByTagName("UriGroup"));
                        nodeListRemoveAll(mergeConfigNode, mergeConfigNode.getElementsByTagName("Route"));
                    }
                } else {
                    Tr.info(traceComponent, "Configurations with IntelligentManagement can not be merged.");
                    throw new RuntimeException("Configurations with IntelligentManagement can not be merged.");
                }
                plgFile++;
            }
        }
    }

    public static void printHelp() {
        System.out.println("\nUSAGE"
                           + "\n  <Liberty_Home>/wlp/usr/servers/<Member_Name>/plugin-cfg1.xml <Liberty_Home>/wlp/usr/servers/<Member_Name>/plugin-cfg2.xml [...] <Liberty_Home>/wlp/usr/servers/<Controller_Name>/<ClusterName>-plugin-cfg.xml\n");
        System.out.println("DESCRIPTION"
                           + "\n    The PluginCfgMerge Tool combines the plugin-cfg.xml files from two or more unbridged "
                           + "\n    servers such that the IBM HTTP Server Plugin will route traffic to all servers. "
                           + "\n    A uri is considered to be shared between two unbridged servers if the uri and  "
                           + "\n    corresponding virtual host definitions are identical."
                           + "\n"
                           + "\n    The contents of the merged plugin-cfg.xml files must be in English language"
                           + "\n"
                           + "\n    Additional parmaters:"
                           + "\n     -debug               = prints additional log statements"
                           + "\n     -sortVhostGrp        = adds VirtualHostGroup name as part of the key.  Use this if a single XML contains"
                           + "\n                            two identical sets of URIs assigned to two different VirtualHostGroup Names."
                           + "\n     -setMatchUriAppVhost = sets the MatchUriAppVhost value."
                           + "\n \n Example with Paramters:"
                           + "\n    -sortVhostGrp -debug <Liberty_Home>/wlp/usr/servers/<Member_Name>/plugin-cfg1.xml <Liberty_Home>/wlp/usr/servers/<Member_Name>/plugin-cfg2.xml [...] <Liberty_Home>/wlp/usr/servers/<Controller_Name>/<ClusterName>-plugin-cfg.xml\n");

        System.exit(1);
    }

    @Override
    public void merge(String argv[]) {
        if (argv.length < 2)
            throw new IllegalArgumentException("Please provide at least 1 plugin-cfg.xml file to merge.");

        PluginMergeToolImpl toolInstance = new PluginMergeToolImpl();
        matchUriAppVhost = Boolean.getBoolean("com.ibm.ws.pluginmerge.match.appname");

        try {
            Tr.info(traceComponent, "Merging...");
            toolInstance.loadData(argv);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        try {
            if (precedence)
                toolInstance.pMerge();
            else
                toolInstance.lfMerge();

            toolInstance.printMergedCopy(argv[argv.length - 1]);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        Tr.info(traceComponent, "Merge Complete");
    }

    public static void main(String argv[]) {
        if (argv.length < 3)
            printHelp();

        matchUriAppVhost = Boolean.getBoolean("com.ibm.ws.pluginmerge.match.appname");
        PluginMergeToolImpl toolInstance = new PluginMergeToolImpl();
        try {
            System.out.println("Merging:");
            toolInstance.loadData(argv);
        } catch (SAXException e) {
            e.printStackTrace();
            System.out.println(NO_MERGE_ERR);
            System.exit(2);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(NO_MERGE_ERR);
            System.exit(3);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            System.out.println(NO_MERGE_ERR);
            System.exit(3);
        }
        try {
            if (precedence)
                toolInstance.pMerge();
            else
                toolInstance.lfMerge();

            toolInstance.printMergedCopy(argv[argv.length - 1]);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(NO_MERGE_ERR);
            System.exit(4);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(NO_MERGE_ERR);
            System.exit(5);
        }

        System.out.println("Merge Complete");
    }

    private class PluginInfo {
        private int seqNum = 0;
        private boolean isShared = false;
        // <Key: String uid (uri/app/vhostdef) || String uid (uri/vhostdef), Value: AppInfo()>
        @SuppressWarnings("unchecked")
        private final Hashtable uniquePluginRep = new Hashtable();

        private ArrayList<String> primaryServers = null;
        private ArrayList<String> backupServers = null;

        private Document sharedDoc = null;
        private Document unsharedDoc = null;
        private Element sharedCluster = null;

        private final Vector<Node> unsharedClusters = new Vector<Node>();
        // <Key: Set containing SeverCluster server names, Value: Node ServerCluster>
        private final Hashtable<Set<String>, Node> allSharedScElements = new Hashtable<Set<String>, Node>();
        // <Key: String vhost name, Value: Element vhost>
        private Hashtable<String, Node> sharedVhosts = null;
        // <Key: String server name, Value: Node server>
        private final Hashtable<String, Node> allSharedServers = new Hashtable<String, Node>();

        private final Vector<Node> unsharedVhg = new Vector<Node>();

        private ArrayList<Node> sharedVhgs = null;
        private Hashtable<String, String> uriVhostGrpMap = null;

        private final Hashtable<String, Node> uriGrps = new Hashtable<String, Node>();
        private final Hashtable<String, Node> uniqueUriGrps = new Hashtable<String, Node>();
        private final Vector<Node> routes = new Vector<Node>();

        private final HashSet<String> containedUris = new HashSet<String>();
        private final HashSet<String> containedApps = new HashSet<String>();

        //720290 - Added code here to indicate a Primary or Backup Type and added values to each arraylist.
        //         This will be used in getUnsharedServerClusters() & getFailOverServerClusters() methods.
        private void listNodes(Node node, String indent, String Type) {
            if (node.hasAttributes()) {
                NamedNodeMap attrs = node.getAttributes();
                for (int i = 0; i < attrs.getLength(); i++) {
                    Attr attribute = (Attr) attrs.item(i);
                    if (attribute.getName().equals("Name")) {
                        if (Type == "Primary") {
                            if (debug)
                                Tr.info(traceComponent, indent + "Found PrimaryServer: " + attribute.getName() + " = " + attribute.getValue());
                            G_primaryServers.add(attribute.getValue());
                        } else {
                            if (debug)
                                Tr.info(traceComponent, indent + "Found BackupServer: " + attribute.getName() + " = " + attribute.getValue());
                            G_backupServers.add(attribute.getValue());
                        }
                    }
                }
            }
            NodeList list = node.getChildNodes();
            if (list.getLength() > 0) {
                //Tr.info(traceComponent,indent+" Child Nodes of "+nodeName+" are:");
                for (int i = 0; i < list.getLength(); i++) {
                    listNodes(list.item(i), indent + "  ", Type);
                }
            }
        }

        /**
         * Constructor for the input plugin-cfg.xml files
         *
         * @param seqNum
         * @param config
         * @param fileLoc
         */
        @SuppressWarnings("unchecked")
        @FFDCIgnore(ArrayIndexOutOfBoundsException.class)
        public PluginInfo(int seqNum, Element config, String fileLoc) {
            // marks the order in which the plugin-cfg.xml were read in
            this.seqNum = seqNum;

            // Begin PM38369 - new code to preserve primary and backupserver designation from input files
            Stack<Node> stack = new Stack<Node>();
            NodeList nodeList1 = config.getElementsByTagName("PrimaryServers");
            if (debug)
                Tr.info(traceComponent, "Storing Primary Server information");
            for (int i = 0; i < nodeList1.getLength(); i++) {
                Node n = nodeList1.item(i);
                Node node = n.getFirstChild();
                Tr.info(traceComponent, "Calling listNodes for : " + n.getNodeName());
                listNodes(n, "", "Primary");

                while (node != null) { // print node information
                    if (node.hasChildNodes()) {
                        // store next sibling in the stack. We return to it after all children are processed.
                        if (node.getNextSibling() != null)
                            stack.push(node.getNextSibling());
                        node = node.getFirstChild();
                    } else {
                        node = node.getNextSibling();
                        if (node == null && !stack.isEmpty())
                            // return to the parent's level.
                            // note that some levels can be skipped if the parent's node was the last one.
                            node = stack.pop();
                    }
                }
            }

            // End PM38369
            //720290 - Added second call to listNodes() to store BackupServers
            NodeList nodeList2 = config.getElementsByTagName("BackupServers");
            if (debug)
                Tr.info(traceComponent, "Gathering Backup Server information");
            for (int i = 0; i < nodeList2.getLength(); i++) {
                Node n2 = nodeList2.item(i);
                Tr.info(traceComponent, "Calling listNodes for : " + n2.getNodeName());
                listNodes(n2, "", "Backup");
            }

            //need to remove primary/backup definitions
            NodeList nl = config.getElementsByTagName("PrimaryServers");
            for (int i = 0; i < nl.getLength();) {
                Node n = nl.item(i);
                n.getParentNode().removeChild(n);
            }
            nl = config.getElementsByTagName("BackupServers");
            for (int i = 0; i < nl.getLength();) {
                Node n = nl.item(i);
                n.getParentNode().removeChild(n);
            }

            //keys = serverCluster names - values = the corresponding serverCluster element
            // ServerClusterName:ServerClusterElement
            Hashtable<String, Node> serverClusters = createTable(nodeListToDeadArray(config.getElementsByTagName("ServerCluster")), "Name");
            //keys = UriGroup names  - values = the corresponding route for a UriGroup
            // UriGroupName:RouteElement
            Hashtable<String, Node> routes = createTable(nodeListToDeadArray(config.getElementsByTagName("Route")), "UriGroup");
            // VirtualHostGroupName:VirtualHostGroupElement
            Hashtable<String, Node> vHostGrps = createTable(nodeListToDeadArray(config.getElementsByTagName("VirtualHostGroup")), "Name");

            // fix-up the serverCluster and VirtualHostGroup names as well as corresponding route attributes to avoid collision
            // ensures that ServerCluster and VHG names are unique to this plugin-cfg.xml when compared to others
            Enumeration<String> keys = serverClusters.keys();
            while (keys.hasMoreElements()) {
                Element cluster = (Element) serverClusters.get(keys.nextElement());
                cluster.setAttribute("Name", cluster.getAttribute("Name") + "_" + seqNum);
            }
            keys = vHostGrps.keys();
            while (keys.hasMoreElements()) {
                Element vhg = (Element) vHostGrps.get(keys.nextElement());
                vhg.setAttribute("Name", vhg.getAttribute("Name") + "_" + seqNum);
            }
            keys = routes.keys();
            while (keys.hasMoreElements()) {
                Element route = (Element) routes.get(keys.nextElement());
                route.setAttribute("ServerCluster", route.getAttribute("ServerCluster") + "_" + seqNum);
                route.setAttribute("VirtualHostGroup", route.getAttribute("VirtualHostGroup") + "_" + seqNum);
            }

            // UriGroupName:UriGroupElement
            Hashtable<String, Node> uriGrpsTmp = createTable(nodeListToDeadArray(config.getElementsByTagName("UriGroup")), "Name");

            // convenient to only work with one of these because changing values in the object
            // as appropriate before cloning it and storing it in uniquePluginRep
            AppInfo info = new AppInfo();

            String uriGrpName, uriName = null;
            Enumeration<String> uriGrpNames = uriGrpsTmp.keys();
            while (uriGrpNames.hasMoreElements()) {
                uriGrpName = uriGrpNames.nextElement();
                if (isXdOnly) {
                    try {
                        // appName - stripped from the uriGrp by removing the preceding /cell/<cellName>/application/ definition
                        info.setAppName(uriGrpName.split("/cell/.+?(/application/)")[1]);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        Tr.info(traceComponent, "Merging a non-ODC generated plugin-cfg.xml");
                        isXdOnly = false;
                        info.setAppName(uriGrpName);

                        if ((!setMatchUriAppVhost) && (matchUriAppVhost)) {
                            PluginMergeToolImpl.matchUriAppVhost = false;
                            Tr.info(traceComponent, "Cannot match based on uri app vhost for non-ODC generated plugin-cfg.xml files. " +
                                                    "com.ibm.ws.pluginmerge.match.appname set to false");
                        } else {
                            PluginMergeToolImpl.matchUriAppVhost = setMatchUriAppVhost;
                        }
                    }
                } else
                    info.setAppName(uriGrpName);

                containedApps.add(info.getAppName());

                Element eUriGrp = (Element) uriGrpsTmp.get(uriGrpName);
                info.setUriGrp((Element) eUriGrp.cloneNode(false));

                try {
                    info.setRoute((Element) ((Element) routes.get(uriGrpName)).cloneNode(true));
                } catch (NullPointerException e) {
                    Tr.info(traceComponent, "Skipping UriGroup " + uriGrpName + " because it does not have a corresponding Route definition");
                    continue;
                }

                String scName = (info.getRoute()).getAttribute("ServerCluster");
                info.setServerCluster((Element) ((Element) serverClusters.get(scName.substring(0, scName.lastIndexOf("_" + seqNum)))).cloneNode(true));

                String vhgName = info.getRoute().getAttribute("VirtualHostGroup");
                Element eVhg = (Element) vHostGrps.get(vhgName.substring(0, vhgName.lastIndexOf("_" + seqNum)));
                info.setVhg((Element) eVhg.cloneNode(false));

                // VirtualHostName:VirtualHostElement
                Hashtable<String, Node> vHosts = createTable(nodeListToDeadArray(eVhg.getElementsByTagName("VirtualHost")), "Name");
                // UriName:UriElement
                Hashtable<String, Node> uris = createTable(nodeListToDeadArray(eUriGrp.getElementsByTagName("Uri")), "Name");

                Enumeration<String> uriNames = uris.keys();
                while (uriNames.hasMoreElements()) {
                    uriName = uriNames.nextElement();
                    info.setUri((Element) ((Element) uris.get(uriName)).cloneNode(true));
                    containedUris.add(uriName);

                    Enumeration<String> eVh = vHosts.keys();
                    while (eVh.hasMoreElements()) {
                        String vhName = eVh.nextElement();
                        info.setVh((Element) ((Element) vHosts.get(vhName)).cloneNode(true));
                        try {
                            if (matchUriAppVhost)
                                uniquePluginRep.put("/uri/" + uriName + "/app/" + info.getAppName() + "/vHost/" + vhName, info.clone());
                            else if (sortVhostGrp) {
                                uniquePluginRep.put("/uri/" + uriName + "/vhostGrp/" + vhgName.substring(0, vhgName.lastIndexOf("_" + seqNum)) + "/vHost/" + vhName, info.clone());
                            } else {
                                uniquePluginRep.put("/uri/" + uriName + "/vHost/" + vhName, info.clone());
                            }

                        } catch (CloneNotSupportedException e) {
                            Tr.info(traceComponent, "Error processing /uri/" + uriName + "/app/" + info.getAppName() + "/vHost/\n" + e.getLocalizedMessage());
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        /**
         * Constructor for a shared plugin-cfg.xml
         *
         * @param seqNum
         * @param serverCluster1
         * @throws ParserConfigurationException
         */
        @SuppressWarnings("unchecked")
        public PluginInfo(int seqNum, Element serverCluster1) throws ParserConfigurationException {
            isShared = true;
            this.seqNum = seqNum;

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = dbf.newDocumentBuilder();
            sharedDoc = parser.newDocument();

            // used as a template for all of the ServerCluster nodes that will be created for this shared element
            sharedCluster = (Element) serverCluster1.cloneNode(false);

            sharedVhosts = new Hashtable<String, Node>();
            sharedVhgs = new ArrayList<Node>();
            uriVhostGrpMap = new Hashtable();
            primaryServers = new ArrayList<String>();
            backupServers = new ArrayList<String>();
        }

        @SuppressWarnings({ "unused", "unchecked" })
        private void addMatch(String uid, String appName, Element serverCluster1, int p1_seq, Element serverCluster2, int p2_seq, Element uri, Element vh) {
            AppInfo sharedAppInfo = null;

            Hashtable sharedServers = null;
            if (uniquePluginRep.containsKey(uid)) {
                sharedAppInfo = (AppInfo) uniquePluginRep.get(uid);
            } else {
                sharedAppInfo = new AppInfo();
                sharedAppInfo.setAppName(appName);
                uniquePluginRep.put(uid, sharedAppInfo);
            }

            sharedAppInfo.setUri(uri);
            sharedAppInfo.setVh(vh);
            sharedVhosts.put(vh.getAttribute("Name"), vh);

            addSharedServers(p1_seq, serverCluster1, sharedAppInfo, true);
            addSharedServers(p2_seq, serverCluster2, sharedAppInfo, true); /* 654526 */
        }

        @SuppressWarnings("unchecked")
        private void addSharedServers(int postfix, Element sc, AppInfo appInfo, boolean isPrimary) {
            Element srvr = null;
            String serverNom = null;
            Iterator itrServers = nodeListToDeadArray(sc.getElementsByTagName("Server")).iterator();
            if (!itrServers.hasNext()) {
                String scName = sc.getAttribute("Name");
                if (!emptyServerClusters.contains(scName)) {
                    Tr.info(traceComponent, "ServerCluster element '" + scName.substring(0, scName.lastIndexOf("_")) + "' from " + sc.getBaseURI()
                                            + " does not contain any Server elements");
                    emptyServerClusters.add(scName);
                }
            }

            while (itrServers.hasNext()) {
                srvr = (Element) itrServers.next();
                serverNom = srvr.getAttribute("Name") + "_" + postfix;

                srvr.setAttribute("Name", serverNom);

                if (appInfo.setSharedServer(serverNom, srvr)) {
                    if (isPrimary)
                        primaryServers.add(serverNom);
                    else
                        backupServers.add(serverNom);
                }

                allSharedServers.put(serverNom, srvr.cloneNode(true));
            }
        }

        public String createSharedName(String nom) {
            return "/cell/sharedCell_" + seqNum + "/" + nom;
        }

        private Hashtable<String, Node> createTable(ArrayList<Node> elements, String attr) {
            Hashtable<String, Node> table = new Hashtable<String, Node>();
            if (elements != null) {
                String key = null;
                Element eTmp = null;
                elements.trimToSize();
                Iterator<Node> itr = elements.iterator();
                while (itr.hasNext()) {
                    eTmp = (Element) itr.next();
                    key = eTmp.getAttribute(attr);
                    if (table.put(key, eTmp) != null && debug) {
                        Tr.info(traceComponent, "Replaced value for key: " + key);
                    }
                }
            }

            return table;
        }

        /**
         * Builds allSharedScElements Hashtable
         * <Key: Set containing SeverCluster server names, Value: Node ServerCluster>
         *
         * @param scNum
         * @param serverNames
         */
        @SuppressWarnings("unchecked")
        private void makeServerClusterElement(int scNum, Set serverNames) {
            Element sc = (Element) sharedCluster.cloneNode(true);
            sc = (Element) sharedDoc.importNode(sc, true);
            sc.setAttribute("Name", "Shared_" + seqNum + "_Cluster_" + scNum);

            Iterator itr = serverNames.iterator();
            while (itr.hasNext())
                sc.appendChild(sharedDoc.importNode(((Element) allSharedServers.get(itr.next())).cloneNode(true), true));

            allSharedScElements.put(serverNames, sc.cloneNode(true));
        }

        @SuppressWarnings("unchecked")
        private void createShared_ServerClusters_VhostGrps() {
            tc = "createShared_ServerClusters_VhostGrps - ";
            int scNum = 0;
            int serverCount = 0;
            Set srvrSetTmp = null;
            HashSet vhostSetTmp = null;
            ArrayList srvrSetList = null;
            AppInfo sharedAppInfo = null;
            // <Key: int - size of all sets contained in the value, Value: ArrayList containing Sets of server names>
            // i.e. - each set contained in a given value contains the same number of members - the sets could be duplicates
            Hashtable serversSetTbl = new Hashtable();
            // < Key: uri name, Value: ArrayList containing Sets of vhost names>
            Hashtable uriVhostSetTbl = new Hashtable();

            Enumeration e = null;
            e = uniquePluginRep.keys(); //PM25128
            //e = uniquePluginRep.elements();
            while (e.hasMoreElements()) {
                String uid = (String) e.nextElement(); //PM25128
                sharedAppInfo = (AppInfo) uniquePluginRep.get(uid); //PM25128
                //sharedAppInfo = (AppInfo) e.nextElement();
                //get a set containing all server names that correspond to a given shared uri
                srvrSetTmp = ((Hashtable) sharedAppInfo.getSharedServersMap().clone()).keySet();
                serverCount = srvrSetTmp.size();
                //add the set into an ArrayList contained in a hashtable based on size
                if (serversSetTbl.containsKey(new Integer(serverCount))) {
                    ((ArrayList) serversSetTbl.get(new Integer(serverCount))).add(srvrSetTmp);
                } else {
                    srvrSetList = new ArrayList();
                    srvrSetList.add(srvrSetTmp);
                    serversSetTbl.put(new Integer(serverCount), srvrSetList);
                }

                //vhost section
                //String uriName = sharedAppInfo.getUri().getAttribute("Name");
                //TODO: this needs to be changed to use uri/vhostGrp for key
                // Tr.info(traceComponent,"/uri/" + uriName + "/vhostGrp/" + vhgName.substring(0, vhgName.lastIndexOf("_" + seqNum)) + "/vHost/" + vhName);
                String uriName = uid.substring(0, uid.indexOf("/vHost/"));
                if (debug)
                    Tr.info(traceComponent, tc + "uriName: " + uriName);
                if (uriVhostSetTbl.containsKey(uriName))
                    ((Set) uriVhostSetTbl.get(uriName)).add(sharedAppInfo.getVh().getAttribute("Name"));
                else {
                    vhostSetTmp = new HashSet();
                    vhostSetTmp.add(sharedAppInfo.getVh().getAttribute("Name"));
                    uriVhostSetTbl.put(uriName, vhostSetTmp);
                }
            }

            //cycle through the server sets that have the same number of members
            //if the sets contain the same members remove one of the sets
            //best case scenario we end up with one server cluster definition for each different set size
            //worst case there will be a unique server cluster for each matched uri
            e = serversSetTbl.keys();
            while (e.hasMoreElements()) {
                srvrSetList = (ArrayList) serversSetTbl.get(e.nextElement());
                srvrSetList.trimToSize();
                Set[] sets = new Set[srvrSetList.size()];
                srvrSetList.toArray(sets);
                for (int i = 0; i < sets.length - 1; i++) {
                    for (int j = i + 1; j < sets.length; j++) {
                        if (sets[i].containsAll(sets[j]))
                            sets[j].clear();
                    }
                }

                for (int i = 0; i < sets.length; i++) {
                    if (!sets[i].isEmpty()) {
                        makeServerClusterElement(scNum, sets[i]);
                        scNum++;
                    }
                }
            }

            //cycle through the vhost values matching size and content
            //also create the vhostGroup elements to be printed out in the mergedoc
            int vhGrpNum = 0;
            Hashtable vhostSetsTbl = new Hashtable();
            ArrayList[] vhostSetList = null;
            e = uriVhostSetTbl.keys();
            while (e.hasMoreElements()) {
                String uriNom = (String) e.nextElement();
                vhostSetTmp = (HashSet) uriVhostSetTbl.get(uriNom);
                if (vhostSetsTbl.containsKey(vhostSetTmp.size())) {
                    boolean found = false;
                    vhostSetList = (ArrayList[]) vhostSetsTbl.get(vhostSetTmp.size());

                    Iterator itrSetList = vhostSetList[0].iterator();
                    Iterator itrVhgName = vhostSetList[1].iterator();
                    while (itrSetList.hasNext()) {
                        String vhgName = (String) itrVhgName.next();
                        if (((Set) itrSetList.next()).containsAll(vhostSetTmp)) {
                            found = true;
                            uriVhostGrpMap.put(uriNom, vhgName);
                            break;
                        }
                    }

                    if (!found) {
                        String newVhgName = createSharedVhostGrp(vhostSetTmp, vhGrpNum);
                        vhGrpNum++;

                        vhostSetList[0].add(vhostSetTmp);
                        vhostSetList[1].add(newVhgName);

                        vhostSetsTbl.put(vhostSetTmp.size(), vhostSetList);
                        uriVhostGrpMap.put(uriNom, newVhgName);
                    }
                } else {
                    String newVhgName = createSharedVhostGrp(vhostSetTmp, vhGrpNum);
                    vhGrpNum++;

                    vhostSetList = new ArrayList[2];
                    vhostSetList[0] = new ArrayList();
                    vhostSetList[1] = new ArrayList();
                    vhostSetList[0].add(vhostSetTmp);
                    vhostSetList[1].add(newVhgName);

                    vhostSetsTbl.put(vhostSetTmp.size(), vhostSetList);
                    uriVhostGrpMap.put(uriNom, newVhgName);
                }
            }
        }

        @SuppressWarnings("unchecked")
        private String createSharedVhostGrp(HashSet vhostsSet, int grpNum) {
            String sharedVhgName = createSharedName("vHostGroup/shared_host_" + grpNum);
            if (debug)
                Tr.info(traceComponent, "createSharedVhostGrp - sharedVghName: " + sharedVhgName);
            Element eVhg = (Element) sharedDoc.createElement("VirtualHostGroup").cloneNode(true);
            eVhg.setAttribute("Name", sharedVhgName);

            Iterator itr = vhostsSet.iterator();
            while (itr.hasNext()) {
                eVhg.appendChild(sharedDoc.importNode(sharedVhosts.get(itr.next()), true));
            }

            sharedVhgs.add(eVhg.cloneNode(true));

            return sharedVhgName;
        }

        @SuppressWarnings("unchecked")
        private void cleanPlugin() throws ParserConfigurationException {
            tc = "cleanPlugin - ";
            if (debug)
                Tr.info(traceComponent, tc + "Cleaning the plugin");
            Enumeration e = null;
            if (isShared) {
                createShared_ServerClusters_VhostGrps();

                Element eRoute = (Element) sharedDoc.createElement("Route").cloneNode(true);

                Hashtable knownUris = new Hashtable();
                e = uniquePluginRep.keys();
                while (e.hasMoreElements()) {
                    String uid = (String) e.nextElement();
                    AppInfo sharedAppInfo = (AppInfo) uniquePluginRep.get(uid);
                    String appName = sharedAppInfo.getAppName();
                    Element eUriGrp = null;

                    //allow for multiple uris to be included in a group
                    //using appName is safe here because one was chosen from the original matched uri
                    if (!uriGrps.containsKey(appName)) {
                        // match to a defined Shared Server Cluster
                        Element sc = null;
                        Set servers = sharedAppInfo.getAppServerSet();
                        Enumeration serverSets = allSharedScElements.keys();
                        while (serverSets.hasMoreElements()) {
                            Set s = (Set) serverSets.nextElement();

                            if (servers.size() == s.size()) {
                                if (s.containsAll(servers)) {
                                    sc = (Element) allSharedScElements.get(s);
                                    break;
                                }
                            }
                        }

                        if (sc == null) {
                            // This could happen if the orginal plugin-cfg.xml contained an empty ServerCluster definition.
                            Tr.info(traceComponent, "Unable to find a mathcing ServerCluster definition for uid " + uid + "\nSkipping the unique instance of " + appName);
                        } else {
                            eUriGrp = (Element) sharedDoc.createElement("UriGroup").cloneNode(true);
                            eUriGrp.setAttribute("Name", createSharedName("application/" + appName));

                            HashSet uris = new HashSet();
                            String uriNom = sharedAppInfo.getUri().getAttribute("Name");
                            uris.add(uriNom);
                            knownUris.put(appName, uris);

                            eUriGrp.appendChild(sharedDoc.importNode(sharedAppInfo.getUri().cloneNode(true), true));
                            uriGrps.put(appName, eUriGrp.cloneNode(true));

                            //eRoute.setAttribute("VirtualHostGroup", (String)uriVhostGrpMap.get(uriNom));
                            String newKeyUriVhg = uid.substring(0, uid.indexOf("/vHost/"));
                            eRoute.setAttribute("VirtualHostGroup", uriVhostGrpMap.get(newKeyUriVhg));
                            eRoute.setAttribute("UriGroup", eUriGrp.getAttribute("Name"));
                            eRoute.setAttribute("ServerCluster", sc.getAttribute("Name"));
                            routes.add(eRoute.cloneNode(true));
                        }
                    } else {
                        HashSet uris = (HashSet) knownUris.get(appName);
                        if (!uris.contains(sharedAppInfo.getUri().getAttribute("Name"))) {
                            uris.add(sharedAppInfo.getUri().getAttribute("Name"));
                            ((Element) uriGrps.get(appName)).appendChild(sharedDoc.importNode((sharedAppInfo.getUri()).cloneNode(true), true));
                        }
                        eUriGrp = (Element) uriGrps.get(appName);
                    }
                }
            } else {
                HashSet knownSc = new HashSet();

                Hashtable knownVhgs = new Hashtable();
                Hashtable knownVhs = new Hashtable();
                Hashtable knownUris = new Hashtable();

                Hashtable knownUniqueVhgs = new Hashtable();
                Hashtable knownUniqueVhs = new Hashtable();
                Hashtable knownUniqueUris = new Hashtable();

                e = uniquePluginRep.elements();
                while (e.hasMoreElements()) {
                    AppInfo uid = (AppInfo) e.nextElement();

                    // handle uriGrps
                    Element uriGrp = (Element) uid.getUriGrp().cloneNode(true);
                    String uriGrpName = uriGrp.getAttribute("Name");
                    if (uid.uniqueVhgNeeded) {
                        if (!uniqueUriGrps.containsKey(uriGrpName)) {
                            Element newUriGrp = (Element) uriGrp.cloneNode(true);
                            newUriGrp.setAttribute("Name", uriGrpName + "_" + this.seqNum);
                            newUriGrp.appendChild((uid.getUri()).cloneNode(true));
                            uniqueUriGrps.put(uriGrpName, newUriGrp.cloneNode(true));

                            HashSet uniqueUris = new HashSet();
                            uniqueUris.add(uid.getUri().getAttribute("Name"));
                            knownUniqueUris.put(uriGrpName, uniqueUris);
                        } else {
                            HashSet uniqueUris = (HashSet) knownUniqueUris.get(uriGrpName);
                            if (!uniqueUris.contains(uid.getUri().getAttribute("Name"))) {
                                uniqueUris.add(uid.getUri().getAttribute("Name"));
                                ((Element) uniqueUriGrps.get(uriGrpName)).appendChild((uid.getUri()).cloneNode(true));
                            }
                        }

                        uid.getRoute().setAttribute("UriGroup", uriGrpName + "_" + this.seqNum);
                    } else {
                        if (!uriGrps.containsKey(uriGrpName)) {
                            uriGrp.appendChild((uid.getUri()).cloneNode(true));
                            uriGrps.put(uriGrpName, uriGrp.cloneNode(true));

                            HashSet uris = new HashSet();
                            uris.add(uid.getUri().getAttribute("Name"));
                            knownUris.put(uriGrpName, uris);
                        } else {
                            HashSet uris = (HashSet) knownUris.get(uriGrpName);
                            if (!uris.contains(uid.getUri().getAttribute("Name"))) {
                                uris.add(uid.getUri().getAttribute("Name"));
                                ((Element) uriGrps.get(uriGrpName)).appendChild((uid.getUri()).cloneNode(true));
                            }
                        }
                    }

                    // handle vhgs
                    Element vhg = uid.getVhg();
                    String vhgName = vhg.getAttribute("Name");
                    if (uid.uniqueVhgNeeded) {
                        if (!knownUniqueVhgs.containsKey(vhgName)) {
                            Element newVhg = (Element) vhg.cloneNode(true);
                            newVhg.setAttribute("Name", vhgName + "_" + this.seqNum);
                            newVhg.appendChild((uid.getVh()).cloneNode(true));
                            knownUniqueVhgs.put(vhgName, newVhg.cloneNode(true));

                            HashSet vhUniqueSet = new HashSet();
                            vhUniqueSet.add(uid.getVh().getAttribute("Name"));
                            knownUniqueVhs.put(vhgName, vhUniqueSet);
                        } else {
                            if (((HashSet) knownUniqueVhs.get(vhgName)).add(uid.getVh().getAttribute("Name")))
                                ((Element) knownUniqueVhgs.get(vhgName)).appendChild((uid.getVh()).cloneNode(true));
                        }

                        uid.getRoute().setAttribute("VirtualHostGroup", vhgName + "_" + this.seqNum);
                    } else {
                        if (!knownVhgs.containsKey(vhgName)) {
                            vhg.appendChild((uid.getVh()).cloneNode(true));
                            knownVhgs.put(vhgName, vhg.cloneNode(true));

                            HashSet vhSet = new HashSet();
                            vhSet.add(uid.getVh().getAttribute("Name"));
                            knownVhs.put(vhgName, vhSet);
                        } else {
                            if (((HashSet) knownVhs.get(vhgName)).add(uid.getVh().getAttribute("Name")))
                                ((Element) knownVhgs.get(vhgName)).appendChild((uid.getVh()).cloneNode(true));
                        }
                    }

                    // handle ServerClusters
                    Element sc = uid.getServerCluster();
                    String scName = sc.getAttribute("Name");
                    if (!knownSc.contains(scName)) {
                        knownSc.add(scName);
                        unsharedClusters.add(sc.cloneNode(true));
                    }

                    // handle routes
                    if (!routes.contains(uid.getRoute()))
                        routes.add((uid.getRoute()));
                }

                unsharedVhg.addAll(knownVhgs.values());
                unsharedVhg.addAll(knownUniqueVhgs.values());
            }
        }

        @SuppressWarnings("unchecked")
        public Hashtable<String, AppInfo> getUniquePluginRep() {
            return uniquePluginRep;
        }

        @SuppressWarnings("unchecked")
        public Node[] getUnsharedServerClusters() throws ParserConfigurationException {
            //PM38369 - reworked this method to add in the Primary and Backup server listings for each cluster.
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = dbf.newDocumentBuilder();
            unsharedDoc = parser.newDocument();
            ArrayList lstNodes = new ArrayList();
            Vector primaries = new Vector();
            Vector backups = new Vector();
            int indx = 0;
            if (debug)
                Tr.info(traceComponent, "Building UnsharedCluster information");
            Iterator itr = unsharedClusters.iterator();
            while (itr.hasNext()) {
                Element tmpSc = (Element) unsharedDoc.importNode(((Node) itr.next()).cloneNode(true), true);
                if (debug)
                    Tr.info(traceComponent, "ServerCluster Name: " + tmpSc.getAttribute("Name"));
                NodeList nlstServers = tmpSc.getElementsByTagName("Server");
                for (int i = 0; i < nlstServers.getLength(); i++) {
                    Element serverName = (Element) nlstServers.item(i);
                    if (debug)
                        Tr.info(traceComponent, "\t Cluster Server Name: " + serverName.getAttribute("Name"));
                    if ((G_primaryServers.contains(serverName.getAttribute("Name")))
                        || (G_primaryServers.isEmpty() || !G_backupServers.contains(serverName.getAttribute("Name")))) { /* 720290 */
                        if (debug)
                            Tr.info(traceComponent, "UnShared: Adding " + serverName.getAttribute("Name") + " to PrimaryServer list");
                        primaries.add(serverName.getAttribute("Name"));
                    } else {
                        if (debug)
                            Tr.info(traceComponent, "UnShared: Adding " + serverName.getAttribute("Name") + " to BackupServer list");
                        backups.add(serverName.getAttribute("Name"));
                    }
                }
                Element eServerTmp = (Element) unsharedDoc.createElement("Server").cloneNode(true);
                eServerTmp = (Element) unsharedDoc.importNode(eServerTmp, true);

                Element ePrimary = (Element) unsharedDoc.createElement("PrimaryServers").cloneNode(true);
                ePrimary = (Element) unsharedDoc.importNode(ePrimary, true);

                primaries.trimToSize();
                Enumeration e = primaries.elements();
                while (e.hasMoreElements()) {
                    eServerTmp.setAttribute("Name", (String) e.nextElement());
                    ePrimary.appendChild(eServerTmp.cloneNode(true));
                }
                tmpSc.appendChild(ePrimary);
                if (!backups.isEmpty()) {
                    if (debug)
                        Tr.info(traceComponent, "Unshared: Backup servers are configured.");
                    Element eBackup = (Element) unsharedDoc.createElement("BackupServers").cloneNode(true);
                    eBackup = (Element) unsharedDoc.importNode(eBackup, true);

                    backups.trimToSize();
                    e = backups.elements();
                    while (e.hasMoreElements()) {
                        eServerTmp.setAttribute("Name", (String) e.nextElement());
                        eBackup.appendChild(eServerTmp.cloneNode(true));
                    }
                    tmpSc.appendChild(eBackup);
                }
                lstNodes.add(tmpSc.cloneNode(true));

                indx++;
                primaries.clear();
                backups.clear();
            }

            //     Look for unique clusters and create <primaryserver> tag
            unsharedClusters.trimToSize();
            lstNodes.trimToSize();
            Node[] nodes = new Node[unsharedClusters.size()];
            unsharedClusters.copyInto(nodes);
            lstNodes.toArray(nodes);

            return nodes;
        }

        @SuppressWarnings("unchecked")
        public Node[] getSharedServerClusters() {
            Node[] rtnNodes = new Node[allSharedScElements.size()];
            Enumeration e = allSharedScElements.elements();
            int i = 0;
            while (e.hasMoreElements()) {
                rtnNodes[i] = ((Node) e.nextElement()).cloneNode(true);
                i++;
            }
            return rtnNodes;
        }

        @SuppressWarnings("unchecked")
        public Node[] getFailOverServerClusters() {
            String subServerName = null;
            ArrayList lstNodes = new ArrayList();
            Vector primaries = new Vector();
            Vector backups = new Vector();
            Collection c = allSharedScElements.values();
            Iterator itr = c.iterator();
            if (debug)
                Tr.info(traceComponent, "Building Shared Cluster information");
            while (itr.hasNext()) {
                Element sc = (Element) ((Element) itr.next()).cloneNode(true);
                NodeList nList = sc.getElementsByTagName("Server");
                for (int i = 0; i < nList.getLength(); i++) {
                    Element e = (Element) nList.item(i);
                    String serverName = e.getAttribute("Name");
                    subServerName = serverName.substring(0, serverName.length() - (serverName.length() - serverName.lastIndexOf("_")));
                    if ((G_primaryServers.contains(subServerName)) || (G_primaryServers.isEmpty() || !G_backupServers.contains(subServerName))) { /* 720290 */
                        if (debug)
                            Tr.info(traceComponent, "Shared: Adding " + serverName + " to PrimaryServer list.");
                        primaries.add(serverName);
                    } else {
                        if (debug)
                            Tr.info(traceComponent, "Shared: Adding " + serverName + " to BackupServer list.");
                        backups.add(serverName);
                    }
                }

                Element eServerTmp = (Element) sharedDoc.createElement("Server").cloneNode(true);
                eServerTmp = (Element) sharedDoc.importNode(eServerTmp, true);

                Element ePrimary = (Element) sharedDoc.createElement("PrimaryServers").cloneNode(true);
                ePrimary = (Element) sharedDoc.importNode(ePrimary, true);

                primaries.trimToSize();
                Enumeration e = primaries.elements();
                while (e.hasMoreElements()) {
                    eServerTmp.setAttribute("Name", (String) e.nextElement());
                    ePrimary.appendChild(eServerTmp.cloneNode(true));
                }
                sc.appendChild(ePrimary);
                if (!backups.isEmpty()) {
                    if (debug)
                        Tr.info(traceComponent, "Shared: Backup servers are configured.");
                    Element eBackup = (Element) sharedDoc.createElement("BackupServers").cloneNode(true);
                    eBackup = (Element) sharedDoc.importNode(eBackup, true);

                    backupServers.trimToSize();
                    e = backups.elements();
                    while (e.hasMoreElements()) {
                        eServerTmp.setAttribute("Name", (String) e.nextElement());
                        eBackup.appendChild(eServerTmp.cloneNode(true));
                    }

                    sc.appendChild(eBackup);
                }
                lstNodes.add(sc.cloneNode(true));

                primaries.clear();
                backups.clear();
            }

            lstNodes.trimToSize();
            Node[] rtnNodes = new Node[lstNodes.size()];
            lstNodes.toArray(rtnNodes);
            return rtnNodes;
        }

        public Node[] getUnsharedVHostGrp() {
            unsharedVhg.trimToSize();
            Node[] rtnNodes = new Node[unsharedVhg.size()];
            unsharedVhg.copyInto(rtnNodes);
            return rtnNodes;
        }

        public Node[] getSharedVHostGrps() {
            sharedVhgs.trimToSize();
            Node[] rtnNodes = new Node[sharedVhgs.size()];
            sharedVhgs.toArray(rtnNodes);
            return rtnNodes;
        }

        @SuppressWarnings("unchecked")
        public Node[] getUriGrps() {
            Node[] rtnNodes = new Node[uriGrps.size()];
            Collection c = uriGrps.values();
            c.toArray(rtnNodes);
            return rtnNodes;
        }

        @SuppressWarnings("unchecked")
        public Node[] getUniqueUriGrps() {
            Node[] rtnNodes = new Node[uniqueUriGrps.size()];
            Collection c = uniqueUriGrps.values();
            c.toArray(rtnNodes);
            return rtnNodes;
        }

        public Node[] getRoutes() {
            routes.trimToSize();
            Node[] rtnNodes = new Node[routes.size()];
            routes.copyInto(rtnNodes);
            return rtnNodes;
        }

        public int getSeqNum() {
            return seqNum;
        }
    }

    private class AppInfo implements Cloneable {
        private String appName = null;
        private Element uriGrp, route, serverCluster, vhg, uri, vh = null;

        @SuppressWarnings("unchecked")
        private final Vector sharedUris = new Vector();
        @SuppressWarnings("unchecked")
        private final Hashtable sharedServers = new Hashtable();

        private boolean uniqueVhgNeeded = false;

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }

        public Element getUriGrp() {
            return uriGrp;
        }

        public void setUriGrp(Element uriGrp) {
            this.uriGrp = uriGrp;
        }

        public Element getRoute() {
            return route;
        }

        public void setRoute(Element route) {
            this.route = route;
        }

        public Element getServerCluster() {
            return serverCluster;
        }

        public void setServerCluster(Element serverCluster) {
            this.serverCluster = serverCluster;
        }

        public Element getVhg() {
            return vhg;
        }

        public void setVhg(Element vhg) {
            this.vhg = vhg;
        }

        public Element getUri() {
            return uri;
        }

        public void setUri(Element uri) {
            this.uri = uri;
        }

        public Element getVh() {
            return vh;
        }

        public void setVh(Element vh) {
            this.vh = vh;
        }

        @SuppressWarnings({ "unchecked", "unused" })
        public void addSharedUriElements(Element uri) {
            sharedUris.add(uri);
        }

        @SuppressWarnings("unused")
        public Node[] getSharedUriElemenets() {
            sharedUris.trimToSize();
            Node[] rtnNodes = new Node[sharedUris.size()];
            sharedUris.copyInto(rtnNodes);
            return rtnNodes;
        }

        @SuppressWarnings("unchecked")
        public boolean setSharedServer(String serverName, Node serverNode) {
            if (sharedServers.put(serverName, serverNode.cloneNode(true)) == null)
                return true;

            return false;
        }

        @SuppressWarnings("unchecked")
        public Set getAppServerSet() {
            return sharedServers.keySet();
        }

        @SuppressWarnings("unchecked")
        public Hashtable getSharedServersMap() {
            return sharedServers;
        }
    }
}
