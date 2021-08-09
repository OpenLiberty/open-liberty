/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wlp.repository.treehandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.xml.sax.SAXException;

/**
 *
 */
public class ProcessTreeToHTML extends Task {

    private String archiveFile = null;
    private String structureSnippetProp = null;
    private String cssSnippetProp = null;

    private final Map<NodeType, String> iconsAsBase64 = new HashMap<NodeType, String>();

    public ProcessTreeToHTML() {
        try {
            for (final NodeType type : NodeType.values()) {
                log("Loading icon file for node type " + type.toString(), Project.MSG_DEBUG);
                iconsAsBase64.put(type, base64StringFromResource("resources/images/" + type.toString() + ".gif"));
            }
        } catch (IOException ioe) {
            throw new BuildException("Could not load image file for node!", ioe);
        }
    }

    @Override
    public void execute() {
        FileSystemTreeGenerator treeGenerator = new FileSystemTreeGenerator();
        // Start with an empty node to hand children off of
        PathNode tree = new PathNode();

        try {
            log("Generating tree from archive file " + archiveFile);
            ZipFile zipFile = null;
            if (archiveFile.endsWith("jar")) {
                zipFile = new JarFile(archiveFile);
            } else {
                zipFile = new ZipFile(archiveFile);
            }
            // Add children included in the archive
            treeGenerator.generateTreeFromArchive(zipFile, tree);
            // Add children that are included as external dependencies
            treeGenerator.generateTreeFromDepsXML(zipFile, tree);
        } catch (IOException e) {
            throw new BuildException("An IOException occurred when attempting to generate tree from supplied archive", e);
        } catch (ParserConfigurationException e) {
            throw new BuildException("A ParserConfigurationException occurred when attempting to generate tree from supplied archive", e);
        } catch (SAXException e) {
            throw new BuildException("A SAXException occurred when attempting to generate tree from supplied archive", e);
        }

        // We store the CSS snippet and HTML snippet separately, and let the Ant put them in the correct place
        StringBuilder structureSnippet = new StringBuilder();
        StringBuilder cssSnippet = new StringBuilder();
        createHTML(tree, structureSnippet, cssSnippet);

        getProject().setProperty(structureSnippetProp, structureSnippet.toString());
        getProject().setProperty(cssSnippetProp, cssSnippet.toString());
    }

    public void createHTML(PathNode tree, StringBuilder structureSnippet, StringBuilder cssSnippet) {
        String sep = "";
        StringBuilder allTypeClasses = new StringBuilder();
        // Turn off normal list bullets for the structure uls 
        cssSnippet.append("\t\tul.structureitem { list-style-type:none; }\n");

        for (final NodeType type : NodeType.values()) {
            String typeName = type.toString();
            // For each type in the enum, add a css class that turns on a background image gif of the same name
            cssSnippet.append("\t\t." + typeName + " { background-image:url(\"data:image/gif;base64," + iconsAsBase64.get(type) + "\"); }\n");
            // Also store away a list of all the types we have, for a common class below
            allTypeClasses.append(sep);
            allTypeClasses.append("." + typeName);
            sep = ",";
        }
        // Common CSS for all of the li classes
        cssSnippet.append("\t\t" + allTypeClasses.toString() + " { height: 20px; background-repeat:no-repeat; background-position:0px 5px; padding-left:20px; }\n");
        doCreateHTMLNodes(structureSnippet, "\t\t", tree);
    }

    private void doCreateHTMLNodes(StringBuilder base, String indent, PathNode baseNode) {
        Set<PathNode> children = baseNode.getChildren();
        // If there are no children on this node, there's nothing to be done
        if (baseNode.getChildren() != null) {
            // We have children, so are a level deeper, which we represent by wrapping in a new <ul>
            base.append(indent + "<ul class=\"structureitem\">\n");
            for (PathNode node : children) {
                // Siblings are just <li>s wrapped in the same <ul>
                base.append(indent + "\t<li class=\"" + node.getNodeType().toString() + "\" title=\"" + node.getDescription() + "\">");
                base.append(node.getName());
                base.append("</li>\n");
                // Now we recurse and handle any children of this child
                doCreateHTMLNodes(base, indent + "\t", node);
            }
            base.append(indent + "</ul>\n");
        }
    }

    private String base64StringFromResource(String resource) throws IOException {
        log("Processing resource into base64: " + resource, Project.MSG_DEBUG);
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(resource);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        byte[] data = new byte[10240];
        int numRead;

        while ((numRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, numRead);
        }
        buffer.flush();

        byte[] imageData = buffer.toByteArray();
        return DatatypeConverter.printBase64Binary(imageData);
    }

    /**
     * @param targetArchive the targetArcive to process
     */
    public void setTargetArchive(String targetArchive) {
        this.archiveFile = targetArchive;
    }

    /**
     * @param structureSnippetProp the structureSnippetProp to set
     */
    public void setStructureSnippetProp(String structureSnippetProp) {
        this.structureSnippetProp = structureSnippetProp;
    }

    /**
     * @param cssSnippetProp the cssSnippetProp to set
     */
    public void setCssSnippetProp(String cssSnippetProp) {
        this.cssSnippetProp = cssSnippetProp;
    }
}
