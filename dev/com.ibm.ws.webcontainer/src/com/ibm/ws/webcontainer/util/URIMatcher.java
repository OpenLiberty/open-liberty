/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.ibm.wsspi.webcontainer.WCCustomProperties;

public class URIMatcher {
    protected ClauseNode root;
    protected HashMap<String, Object> extensions = new HashMap<String, Object>();

    protected static final String starString = "*"; // PM06111
    protected ClauseNode defaultNode;
    protected static int range = 91;

    public static int computeHash(String key) {
        int hash = 0;
        for (int i = 0; i < key.length(); i++) {
            hash = hash * range + key.charAt(i);
        }

        return hash;
    }

    protected static class Result {
        public ClauseNode node;
        public Object target;

        public Result(ClauseNode node, Object target) {
            this.node = node;
            this.target = target;
        }
    }

    public URIMatcher() {
        this(false, false); // PM06111
    }

    public URIMatcher(boolean scalable) {
        this(scalable, false); // PM06111
    }

    // PM06111 - New constructor
    public URIMatcher(boolean scalable, boolean stringKeys) {
        init();
    }

    public URIMatcher(boolean scalable, int r) {
        range = r;
        init();
    }

    // For test only
    //
    URIMatcher(String file) throws Exception {
        this();
        BufferedReader in = new BufferedReader(new FileReader(file));

        String line = in.readLine();

        while (line != null) {
            if (line.equals("")) {
                line = in.readLine();
                continue;
            }
            StringTokenizer tok = new StringTokenizer(line);
            String tok1 = tok.nextToken();
            String tok2 = tok.nextToken();
            put(tok1, tok2);

            line = in.readLine();
        }

        in.close();
    }

    private void init() {
        if (root == null) {
            root = new ClauseNode(null, "/", null); // we could add the default target here
        }
    }

    // Rule: the URI should start with a '/' except for extension rules
    // 
    public void put(String uri, Object target) throws Exception {
        if (uri.startsWith("*.")) {
            extensions.put(uri.substring(2), target);
            return;
        }
        //Special case for Servlet spec "" mapping to only the context root of app
        if(uri.equals("")){
            root.setTarget(target);
            return;
        }

        int startIdx = 0;
        if (uri.length() > 0 && uri.charAt(0) == '/') {
            startIdx = 1;
        }

        // logic: split on the '/' character and add each clause
        // as you traverse down the tree.
        ClauseNode currentNode = root;
        int slashIdx = uri.indexOf('/', startIdx);
        while (slashIdx != -1) {
            currentNode = currentNode.add(new ClauseNode(currentNode, uri.substring(startIdx, slashIdx), null)); // PM06111
            startIdx = slashIdx + 1;
            slashIdx = uri.indexOf('/', startIdx);
        }
        slashIdx = uri.length(); // set it to the end of the string
        if (startIdx < slashIdx) {
            // we have more characters
            String segment = uri.substring(startIdx, slashIdx);
            if (segment.equals(starString)) {
                if (currentNode.getStarTarget() != null) {
                    throw new Exception("Mapping clash for " + currentNode.getTarget() + ": Target " + target + " already exists at node " + currentNode.getClause());
                }
                currentNode.setStarTarget(target);
            } else {
                currentNode = currentNode.add(new ClauseNode(currentNode, segment, target)); // PM06111
            }
        }

        if (root.getStarTarget() != null) {
            defaultNode = root; // PM06111
        }
    }

    public Iterator iterator() {
        List<Object> l = root.targets();
        l.addAll(extensions.values());
        return l.listIterator(); // 316624
        // return l.iterator(); 316624
    }

    public Object match(String uri) {
        Result result = findNode(uri);

        // if we have the default (/*) node, we need to check extensions first, e.g., /foo.jsp 
        if (result != null && result.node != defaultNode) {
            return result.target;
        }

        Object target = findByExtension(uri);
        if (target != null) {
            return target;
        }

        // use the defaultNode "/*"
        if (defaultNode != null)
            return defaultNode.getStarTarget();

        // not found
        return null;
    }

    /**
     * @param uri
     */
    protected Object findByExtension(String uri) {
        // extension matching
        //
        int dot = uri.lastIndexOf(".");
        if (dot != -1) {
            Object target = extensions.get(uri.substring(dot + 1));
            if (target != null) {
                return target;
            }
        }

        return null;
    }

    protected Result findNode(String uri) {
        Result result = null;
        ClauseNode currentNode = root;
        ClauseNode starNode = defaultNode;

        int startIdx = 1;
        int slashIdx;
        boolean done = false;
        while (!done) {
            slashIdx = uri.indexOf('/', startIdx);

            String segment;
            if (slashIdx == -1) {
                // last segment
                done = true;
                slashIdx = uri.length();
                segment = (startIdx < slashIdx) ? uri.substring(startIdx, slashIdx) : null;
            } else {
                segment = uri.substring(startIdx, slashIdx);
            }

            if (segment != null) {
                currentNode = currentNode.traverse(segment); // PM06111

                if (currentNode == null) {
                    // no exact match
                    done = true;
                } else if (done && currentNode.getTarget() != null) {
                    // we have an exact match (last segment) with a target
                    return new Result(currentNode, currentNode.getTarget());
                } else if (currentNode.getStarTarget() != null) {
                    // a match, check if we need to adjust the starTarget
                    starNode = currentNode;
                }

                startIdx = slashIdx + 1;
            } else {
                // we are done since segment == null
                // we walked past the current node with an ending /
                if(WCCustomProperties.STRICT_SERVLET_MAPPING){        
                    if (currentNode.getStarTarget() != null) {
                        // we have an exact match (last segment) with a target
                        return new Result(currentNode, currentNode.getStarTarget());
                    }
                }
                else {
                    if (currentNode.getTarget() != null) {
                        // we have an exact match (last segment) with a target
                        return new Result(currentNode, currentNode.getTarget());
                    }
                }                
                // no exact match
                currentNode = null;
            }
        }

        // return the star node if it exists
        if (starNode != null) {
            result = new Result(starNode, starNode.getStarTarget());
        }

        return result;
    }

    /**
     * Returns a list of all targets that match the specified uri in the
     * increasing order of specificity
     */
    public List matchAll(String uri) {
        ClauseNode currentNode = root;

        ArrayList<Object> returnList = new ArrayList<Object>();

        // extension matching done first in the matchAll case
        // since it is most generic
        //
        int dot = uri.lastIndexOf(".");
        if (dot != -1) {
            Object tar = extensions.get(uri.substring(dot + 1));
            if (tar != null) {
                returnList.add(tar);
            }
        }

        // add the default node if it exists
        if (defaultNode != null) {
            returnList.add(defaultNode.getStarTarget());
        }

        // walk the nodes adding star targets only
        boolean exact = true;

        int startIdx = 1;
        int slashIdx;
        boolean done = false;
        while (!done) {
            slashIdx = uri.indexOf('/', startIdx);

            String segment;
            if (slashIdx == -1) {
                // last segment
                done = true;
                slashIdx = uri.length();
                segment = (startIdx < slashIdx) ? uri.substring(startIdx, slashIdx) : null;
            } else {
                segment = uri.substring(startIdx, slashIdx);
            }

            if (segment != null) {
                currentNode = currentNode.traverse(segment); // PM06111

                if (currentNode == null) {
                    // no exact match, matches star node if it exists
                    exact = false;
                    done = true;
                } else if (currentNode.getStarTarget() != null) {
                    returnList.add(currentNode.getStarTarget());
                }

                startIdx = slashIdx + 1;
            }
        }

        if (exact) {
            // add exact only nodes since star nodes were added already
            Object target = currentNode.getTarget();
            if (target != null && currentNode.getStarTarget() == null) {
                returnList.add(target);
            }
        }

        return returnList;
    }

    public void remove(String path) {
        if (path.startsWith("*.")) {
            // is a *.xxx mapping, hence remove from the extensions table
            extensions.remove(path.substring(2));
        }
        root.remove(path);
    }

    public static void main(String[] args) {
        try {
            URIMatcher t = new URIMatcher();

            t.put("/portal/*", new String("/portal/* target"));
            t.put("/portal/abc/xyz/*", new String("/portal/abc/xyz/* target1"));
            t.put("/exact/exact.jsp", new String("/exact/exact.jsp"));

            List l = t.matchAll("/portal/abc/xyz/blah.jsp");
            Iterator it = l.iterator();
            while (it.hasNext()) {
                System.out.println("out:" + it.next());
            }

            l = t.matchAll("/exact/exact.jsp");
            it = l.iterator();
            while (it.hasNext()) {
                System.out.println("out:" + it.next());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // System.out.println("Started");
        // try
        // {
        // URIMatcher t = new URIMatcher("map.txt");
        // System.out.println("Result : "+(t.match("/foo/bar/index.html")));
        // //servlet1
        // System.out.println("Result : "+(t.match("/baz/index.html")));
        // //servlet2
        // System.out.println("Result : "+(t.match("/catalog/index.html")));
        // //serv1
        // System.out.println("Result : "+(t.match("/baz"))); //null
        // System.out.println("Result : "+(t.match("/catalog"))); //servlet3
        // System.out.println("Result : "+(t.match("/far/inside/corp/index.html")));
        // //servlet5
        // System.out.println("Result : "+(t.match("/catalog/racecar.bop")));
        // //serv1
        // System.out.println("Result : "+(t.match("/servlet/snoop")));
        // ///servlet/snoop
        // System.out.println("Result : "+(t.match("/servlet/snot/matt")));
        // //hit
        // System.out.println("Result : "+(t.match("/rdfweb.org/people/danbri/rdfweb/danbri-biblio.rdf")));
        // System.out.println("Result : "+(t.matchAll("/baz/truly/madly")));
        // System.out.println("Result : "+(t.matchAll("/baz/truly/madly/deeply")));
        //
        //
        // // long startTime = System.currentTimeMillis();
        // //
        // // int HITS = 400000;
        // //
        // // for ( int i=0; i < HITS; i++ )
        // // {
        // // t.match("/foo/bar/index.html");
        // // t.match("/baz/index.html");
        // // t.match("/baz");
        // // t.match("/catalog");
        // // t.match("/far/inside/corp/index.html");
        // // t.match("/catalog/racecar.bop");
        // // t.match("/servlet/snoop");
        // // t.match("/catalog/what/the/hell");
        // // }
        // //
        // // long endTime = System.currentTimeMillis();
        // //
        // // System.out.println("Average match time = "+ (double)(endTime -
        // startTime) / (double)HITS);
        // }
        // catch ( Exception e )
        // {
        // e.printStackTrace();
        // }
        //
        // System.out.println("trying to match " + args[0]);
        // try
        // {
        // URIMatcher t = new URIMatcher();
        // t.put("/x/y/z/*", "/x/y/z/*");
        // t.put("*.jsp", "*.jsp");
        // t.put("/x/y/*", "/x/y/*");
        // t.put("/x/*", "/x/*"); ;
        //
        //
        // java.util.List l = t.matchAll(args[0]);
        // for ( java.util.Iterator itr = l.iterator(); itr.hasNext(); )
        // {
        // System.out.println(itr.next());
        // }
        // }
        // catch ( Exception e )
        // {
        // e.printStackTrace();
        // }
        //
        //
    }

    /**
     * @param path
     * @param target
     */
    public Object replace(String uri, Object newTarget) throws Exception {
        if (uri.startsWith("*.")) {
            Object oldTarget = null;
            String extension = uri.substring(2);
            if (extensions.containsKey(extension))
                oldTarget = extensions.put(extension, newTarget);
            return oldTarget;
        }

        return root.replace(uri, newTarget);
    }

    /**
     * @param path
     * @return
     */
    public boolean exists(String uri) {
        if (uri.startsWith("*.")) {
            return extensions.get(uri.substring(2)) != null;
        }

        Result result = findNode(uri);

        if (result != null) {
            // only match exact but have to deal with /* endings
            ClauseNode node = result.node;
            int nodeLen = node.getDepth();
            if (node.getStarTarget() != null) {
                nodeLen += 2; // add /* ending
            }

            return nodeLen == uri.length();
        }

        return false;
    }

}