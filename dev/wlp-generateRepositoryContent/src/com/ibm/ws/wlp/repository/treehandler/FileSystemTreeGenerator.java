package com.ibm.ws.wlp.repository.treehandler;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class FileSystemTreeGenerator {
    private static final String ROOT_MANIFEST_HEADER = "Archive-Root";
    private static final String DEPENDENCY_TAGNAME_XML = "dependency";
    private static final Logger LOGGER = Logger.getLogger(FileSystemTreeGenerator.class.getName());

    public void generateTreeFromDepsXML(ZipFile archiveFile, PathNode rootNode) throws IOException, SAXException, ParserConfigurationException {
        String root = getArchiveRoot(archiveFile);

        ZipEntry ze = archiveFile.getEntry("externaldependencies.xml");
        if (ze == null) {
            LOGGER.info("No externaldependencies.xml exists in the archive " + archiveFile.getName() + " so it was skipped");
            return;
        }

        LOGGER.info("Now looking for nodes from external deps file in archive " + archiveFile.getName());
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

        Document document = docBuilderFactory.newDocumentBuilder().parse(archiveFile.getInputStream(ze));
        NodeList allDeps = document.getElementsByTagName(DEPENDENCY_TAGNAME_XML);

        for (int i = 0; i < allDeps.getLength(); i++) {
            Node node = allDeps.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String thisTargetPath = node.getAttributes().getNamedItem("targetpath").getNodeValue();
                String thisSourceUrl = node.getAttributes().getNamedItem("url").getNodeValue();
                String description = "External Dependency: " + thisSourceUrl;
                //The paths in externaldependencies.xml are relative to the directory extract root
                String fullPath = root + thisTargetPath;
                processPath(fullPath, rootNode, NodeType.EXTERNAL, description);
            }
        }

        LOGGER.info("Processing external deps file complete.");

    }

    public void generateTreeFromArchive(ZipFile archiveFile, PathNode rootNode) throws IOException {
        String root = getArchiveRoot(archiveFile);

        for (Enumeration<? extends ZipEntry> en = archiveFile.entries(); en.hasMoreElements();) {
            ZipEntry ze = en.nextElement();
            String name = ze.getName();
            boolean isDir = ze.isDirectory();
            String description = "";
            NodeType nodeType;
            if (isDir) {
                nodeType = NodeType.DIRECTORY;
            } else {
                nodeType = NodeType.FILE;
                description = "File: Included in Archive";
            }

            LOGGER.fine("Checking " + name);

            if (!name.startsWith(root)) {
                LOGGER.fine(" : Skipped - Not in extract root");
                continue;
            } else {
                LOGGER.fine(" : Processing...");
            }

            // Second param - where to attach to
            processPath(name, rootNode, nodeType, description);

        }

        LOGGER.info("Nodes from Archive Processing complete.");
    }

    private String getArchiveRoot(ZipFile archiveFile) throws IOException {
        String root = "";
        if (archiveFile instanceof JarFile) {
            JarFile jarFile = (JarFile) archiveFile;
            Manifest manifest = jarFile.getManifest();
            Attributes attrs = manifest.getMainAttributes();
            root = attrs.getValue(ROOT_MANIFEST_HEADER);
        }
        return root;
    }

    private void processPath(String path, PathNode targetNode, NodeType nodeType, String description) {
        String[] pathArray = path.split("/");
        PathNode currentNode = targetNode;
        for (int i = 0; i < pathArray.length; i++) {
            String pathElement = pathArray[i];
            if (!currentNode.hasChild(pathElement)) {
                PathNode newChild = new PathNode(pathElement);
                currentNode.addNode(newChild);
                //Only directories have children in File system structures
                currentNode.setNodeType(NodeType.DIRECTORY);

                LOGGER.fine("\tAdded new node for " + pathElement + " at level " + i);

            }
            currentNode = currentNode.getChild(pathElement);
        }
        //All parts of tree constructed for this path - tell the last one whether it is a directory or not.
        //Everything in a JAR has a ZipEntry, so we know we'll cover everything this way.
        currentNode.setNodeType(nodeType);
        if ((description != null) && (!description.equals(""))) {
            currentNode.setDescription(description);
        }
    }

}

enum NodeType {
    DIRECTORY, FILE, EXTERNAL;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}

/**
 * Note: this class has a natural ordering that is inconsistent with equals
 */
class PathNode implements Comparable<PathNode> {
    private String nodeName;
    private SortedSet<PathNode> children;
    private NodeType nodeType;
    //Extra notes that may want to be displayed in the tree
    private String description = "";

    public PathNode() {
        super();
    }

    @Override
    public int compareTo(PathNode n) {
        return this.nodeName.compareTo(n.getName());
    }

    public PathNode(String nodeName) {
        this.nodeName = nodeName;
    }

    public void addNode(PathNode node) {
        if (children == null) {
            children = new TreeSet<PathNode>();
        }
        children.add(node);
    }

    public boolean hasChild(String name) {
        if (children != null) {
            for (PathNode n : children) {
                if (n.getName().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    public PathNode getChild(String name) {
        if (children != null) {
            for (PathNode n : children) {
                if (n.getName().equals(name)) {
                    return n;
                }
            }
        }
        return null;
    }

    public String getName() {
        return nodeName;
    }

    public Set<PathNode> getChildren() {
        return children;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void createJSON(JSONArray baseJson) {
        if (children != null) {
            for (PathNode node : children) {
                JSONObject childObject = new JSONObject();
                childObject.put("type", node.getNodeType().toString());
                childObject.put("name", node.getName());
                childObject.put("id", node.getName());
                if (this.getName() != null) {
                    childObject.put("parent", this.getName());
                }
                baseJson.add(childObject);

                node.createJSON(baseJson);
            }
        }
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

}
