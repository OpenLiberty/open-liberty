/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.aries.buildtasks.semantic.versioning.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.ibm.aries.buildtasks.semantic.versioning.model.FeatureInfo.ApiSpiJarKey;
import com.ibm.aries.buildtasks.semantic.versioning.model.decls.ClassDeclaration;
import com.ibm.aries.buildtasks.semantic.versioning.model.decls.FieldDeclaration;
import com.ibm.aries.buildtasks.semantic.versioning.model.decls.MethodDeclaration;
import com.ibm.ws.featureverifier.internal.GlobalConfig;

/**
 * A collection of xml binding code...
 * going from DOM to the various decl objects, all wrapped up in a FrameworkInfo object.
 * 
 * Could be replaced by a java binding api, grew this way incrementally,
 * and is functional enough not to need changing at present.
 */
public class XMLBaseLineUtils {

    public static FrameworkInfo loadBaseXML(File frameworkLog) throws ParserConfigurationException, SAXException, IOException {
        //Map<PkgInfo, PackageContent> pkgInfo = new TreeMap<PkgInfo, PackageContent>();
        VersionedEntityMap<PkgInfo, PackageContent> pkgInfo = new VersionedEntityMap<PkgInfo, PackageContent>();
        List<FeatureInfo> features = new ArrayList<FeatureInfo>();
        Map<VersionedEntity, Set<PkgInfo>> bundles = new TreeMap<VersionedEntity, Set<PkgInfo>>();

        FrameworkInfo fi = new FrameworkInfo();
        fi.pkgInfo = pkgInfo;
        fi.featureInfos = features;
        fi.bundleToPkgs = bundles;

        ZipInputStream zis = null;
        Document d = null;
        try {
            zis = new ZipInputStream(new FileInputStream(frameworkLog));

            @SuppressWarnings("unused")
            ZipEntry theOnlyEntry = zis.getNextEntry();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            d = db.parse(zis);
        } finally {
            try {
                if (zis != null)
                    zis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Map<String, FeatureInfo> nameToFeatureMap = new HashMap<String, FeatureInfo>();
        Map<FeatureInfo, Set<String>> nestedFeatures = new HashMap<FeatureInfo, Set<String>>();

        Node baseline = d.getElementsByTagName("baseline").item(0);
        NodeList baselineContent = baseline.getChildNodes();
        for (int b = 0; b < baselineContent.getLength(); b++) {
            Node base = baselineContent.item(b);
            if (base.getNodeName().equals("packages")) {
                NodeList packageList = base.getChildNodes();
                processPackages(fi, packageList);
            } else if (base.getNodeName().equals("features")) {
                NodeList featureList = base.getChildNodes();
                processFeatures(features, featureList, nameToFeatureMap, nestedFeatures);
            }
        }

        //configure the aggregate maps for each feature.. 
        for (FeatureInfo f : fi.featureInfos) {
            f.aggregateFeatureInfo = new HashSet<FeatureInfo>();
            for (String s : nestedFeatures.get(f)) {
                FeatureInfo nested = nameToFeatureMap.get(s);
                if (nested == null) {
                    throw new IllegalStateException("Framework xml references missing nested feature " + s + " from feature " + f.getName());
                }
                f.aggregateFeatureInfo.add(nameToFeatureMap.get(s));
            }
            f.setAggregateReady();
        }

        return fi;
    }

    //used during processing of runtime to build class declarations for split package evaluation
    public static PackageContent processSinglePackageXml(String xml) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputSource is = new InputSource();
        //System.out.println("Parsing .. "+xml);
        is.setCharacterStream(new StringReader(xml));
        try {
            Document d = db.parse(is);

            Node pkg = d.getElementsByTagName("package").item(0);

            VersionedEntityMap<PkgInfo, PackageContent> pkgInfo = new VersionedEntityMap<PkgInfo, PackageContent>();
            List<FeatureInfo> features = new ArrayList<FeatureInfo>();
            Map<VersionedEntity, Set<PkgInfo>> bundles = new TreeMap<VersionedEntity, Set<PkgInfo>>();

            FrameworkInfo fi = new FrameworkInfo();
            fi.pkgInfo = pkgInfo;
            fi.featureInfos = features;
            fi.bundleToPkgs = bundles;

            boolean processingFrameworkXml = false;
            processPackage(fi, pkg, processingFrameworkXml);

            Set<PackageContent> p = fi.pkgInfo.get(fi.pkgInfo.keySet().iterator().next());
            if (p == null || p.size() != 1) {
                throw new RuntimeException("Error parsing single package xml for compare.");
            }
            //while this looks like an npe waiting to happen.. we checked that above ;p
            PackageContent pc = p.iterator().next();

            return pc;
        } catch (SAXParseException e) {
            System.out.println("ERROR: xml " + xml);
            throw e;
        }
    }

    private static void processFeatureContent(FeatureInfo fi, Node apispiNode, Map<FeatureInfo, Set<String>> nestedFeatures) {
        NodeList features = apispiNode.getChildNodes();
        Set<String> nested = new HashSet<String>();
        nestedFeatures.put(fi, nested);
        for (int j = 0; j < features.getLength(); j++) {
            Node feature = features.item(j);
            if ("feature".equals(feature.getNodeName())) {
                NamedNodeMap pattrs = feature.getAttributes();
                String name = pattrs.getNamedItem("name").getTextContent();
                //shortName is optionally available .. but we don't use it at the mo.
                nested.add(name);
            }
        }
    }

    private static void processJarContent(FeatureInfo fi, Node apispiNode) {
        NodeList jars = apispiNode.getChildNodes();
        Map<ApiSpiJarKey, Map<String, Boolean>> jarContent = new HashMap<ApiSpiJarKey, Map<String, Boolean>>();
        for (int j = 0; j < jars.getLength(); j++) {
            Node jar = jars.item(j);
            if ("jar".equals(jar.getNodeName())) {
                NamedNodeMap pattrs = jar.getAttributes();
                String location = pattrs.getNamedItem("location").getTextContent();
                Map<String, Boolean> entryPaths = new HashMap<String, Boolean>();
                NodeList entries = jar.getChildNodes();
                for (int e = 0; e < entries.getLength(); e++) {
                    Node entry = entries.item(e);
                    if ("entry".equals(entry.getNodeName())) {
                        String entryName = entry.getTextContent();

                        boolean ignore = false;
                        String resName = entryName.replace('.', '/');
                        if (entryName.endsWith(".class")) {
                            resName = resName.substring(0, resName.length() - ".class".length());
                            if (GlobalConfig.isClassIgnoredInBaseline(resName)) {
                                ignore = true;
                            }
                        }

                        if (!ignore) {
                            entryPaths.put(entryName, Boolean.FALSE);
                        } else {
                            System.out.println("Suppressing " + entryName + " as it's ignored by config");
                        }
                    }
                }
                ApiSpiJarKey key = FeatureInfo.extractKeyFromResources(entryPaths, location, "");
                jarContent.put(key, entryPaths);
            }
        }
        //now add the jarContent to the fi.
        fi.setResources(jarContent);
        fi.source = "xml";
    }

    private static void processApiSPI(FeatureInfo fi, Node apispiNode, boolean api) {
        NodeList pkgs = apispiNode.getChildNodes();
        for (int p = 0; p < pkgs.getLength(); p++) {
            Node pkg = pkgs.item(p);
            if ("pkg".equals(pkg.getNodeName())) {
                NamedNodeMap pattrs = pkg.getAttributes();
                String declarers = pattrs.getNamedItem("declarers").getTextContent();
                String symbname = pattrs.getNamedItem("symbname").getTextContent();
                //version is optional.. but important if present.
                String piversion;
                if (pattrs.getNamedItem("version") != null) {
                    piversion = pattrs.getNamedItem("version").getTextContent();
                    if (piversion != null && "null".equals(piversion)) {
                        piversion = null;
                    }
                } else {
                    piversion = null;
                }
                PkgInfo pi = new PkgInfo(symbname, piversion, null);
                Map<String, String> featureAttribs = new HashMap<String, String>();
                for (int a = 0; a < pattrs.getLength(); a++) {
                    Node attrib = pattrs.item(a);
                    //ignore the attribs we used to get here..
                    if (!attrib.getNodeName().equals("declarers") && !attrib.getNodeName().equals("symbname")) {
                        featureAttribs.put(attrib.getNodeName(), attrib.getTextContent());
                    }
                }

                if (declarers == null)
                    declarers = "XMLDECLATTRIBMISSING";

                String dcls[];
                if (declarers.indexOf(',') != -1) {
                    dcls = declarers.split(",");
                } else {
                    dcls = new String[] { declarers };
                }
                for (String d : dcls) {
                    if (api) {
                        if (d.equals(fi.getName())) {
                            fi.addLocalApi(pi, featureAttribs);
                        }
                        fi.addApi(pi, featureAttribs, d);
                    } else {
                        if (d.equals(fi.getName())) {
                            fi.addLocalSpi(pi, featureAttribs);
                        }
                        fi.addSpi(pi, featureAttribs, d);
                    }
                }
            }
        }
    }

    private static void processFeatures(List<FeatureInfo> features,
                                        NodeList featuresList,
                                        Map<String, FeatureInfo> nameToFeatureMap,
                                        Map<FeatureInfo, Set<String>> nestedFeatures) {
        for (int x = 0; x < featuresList.getLength(); x++) {
            Node ftr = featuresList.item(x);
            if (ftr.getNodeName().equals("feature")) {
                NamedNodeMap attrs = ftr.getAttributes();
                //name & version are mandatory for feature.
                String name = attrs.getNamedItem("name").getTextContent();
                String version = attrs.getNamedItem("version").getTextContent();
                String shortName = null;
                if (attrs.getNamedItem("shortName") != null)
                    shortName = attrs.getNamedItem("shortName").getTextContent();
                String visibility = null;
                if (attrs.getNamedItem("visibility") != null)
                    visibility = attrs.getNamedItem("visibility").getTextContent();

                String singleton = null;
                if (attrs.getNamedItem("singleton") != null)
                    singleton = attrs.getNamedItem("singleton").getTextContent();
                Boolean singletonBoolean = Boolean.valueOf(singleton);

                String autoFeature = null;
                if (attrs.getNamedItem("autoFeature") != null)
                    autoFeature = attrs.getNamedItem("autoFeature").getTextContent();
                Boolean autoFeatureBoolean = Boolean.valueOf(autoFeature);

                //additional attributes may be present too.
                Map<String, String> featureAttribs = new HashMap<String, String>();
                for (int a = 0; a < attrs.getLength(); a++) {
                    Node attrib = attrs.item(a);
                    featureAttribs.put(attrib.getNodeName(), attrib.getTextContent());
                }

                //create & store the feature info.
                FeatureInfo fi = new FeatureInfo(name, version, visibility, shortName, featureAttribs, singletonBoolean, autoFeatureBoolean);

                //should this feature be ignored totally ?
                if (!GlobalConfig.isFeatureToBeIgnoredInBaseline(fi.getName())) {

                    features.add(fi);
                    nameToFeatureMap.put(fi.getName(), fi);

                    //update feature info by parsing api/spi packages.
                    NodeList apispi = ftr.getChildNodes();
                    for (int i = 0; i < apispi.getLength(); i++) {
                        Node pi = apispi.item(i);
                        if (pi.getNodeName().equals("api")) {
                            processApiSPI(fi, pi, true);
                        } else if (pi.getNodeName().equals("spi")) {
                            processApiSPI(fi, pi, false);
                        } else if (pi.getNodeName().equals("jarContent")) {
                            processJarContent(fi, pi);
                        } else if (pi.getNodeName().equals("featureContent")) {
                            processFeatureContent(fi, pi, nestedFeatures);
                        }
                    }
                }
            }
        }
    }

    private static void processPackages(FrameworkInfo frameworkInfo,
                                        NodeList packageList) {
        for (int x = 0; x < packageList.getLength(); x++) {
            Node pkg = packageList.item(x);
            if (pkg.getNodeName().equals("package")) {
                boolean processingFrameworkXml = true;
                processPackage(frameworkInfo, pkg, processingFrameworkXml);
            }
        }
    }

    private static void processPackage(FrameworkInfo frameworkInfo,
                                       Node pkg,
                                       boolean processingFrameworkXml) {
        NamedNodeMap attrs = pkg.getAttributes();
        String name = attrs.getNamedItem("symbolicName").getTextContent();
        String version = attrs.getNamedItem("version").getTextContent();
        String bundleName = attrs.getNamedItem("fromBundle").getTextContent();
        String bundleVersion = attrs.getNamedItem("fromBundleVersion").getTextContent();
        VersionedEntity fromBundle = new VersionedEntity(bundleName, bundleVersion);
        PkgInfo pi = new PkgInfo(name, version, bundleName, bundleVersion, null);
        PackageContent pc = new PackageContent(pi);
        NodeList pkgContent = pkg.getChildNodes();
        for (int p = 0; p < pkgContent.getLength(); p++) {
            Node content = pkgContent.item(p);
            if (content.getNodeName().equals("clsdecl")) {
                ClassDeclaration cd = processClsDecl(pi, content, processingFrameworkXml);
                //skip classes that are ignored.. 
                if (cd != null) {
                    pc.addClass(cd.getName(), cd);
                }
            } else if (content.getNodeName().equals("xsd")) {
                NamedNodeMap xsdattrs = content.getAttributes();
                String path = xsdattrs.getNamedItem("path").getTextContent();
                String hash = xsdattrs.getNamedItem("hash").getTextContent();
                pc.addXsd(path, hash);
            }
        }

        //should we ignore this package?
        if (!(GlobalConfig.isPackageToBeIgnoredInBaseline(pi, bundleName + "@" + bundleVersion, null) || GlobalConfig.isPackageToBeIgnoredInBaseline(pi,
                                                                                                                                                     bundleName, null))) {
            frameworkInfo.pkgInfo.merge(pi, pc);
            if (!frameworkInfo.bundleToPkgs.containsKey(fromBundle)) {
                frameworkInfo.bundleToPkgs.put(fromBundle, new HashSet<PkgInfo>());
            }
            frameworkInfo.bundleToPkgs.get(fromBundle).add(pi);
        }
    }

    private static FieldDeclaration processFieldDecl(PkgInfo pi, String className, Node content) {
        int access = 0;
        String fldname = null;
        String signature = null;
        String desc = null;
        String value = null;
        NodeList classContent = content.getChildNodes();
        for (int c = 0; c < classContent.getLength(); c++) {
            Node classInfo = classContent.item(c);
            String classInfoName = classInfo.getNodeName();
            if ("name".equals(classInfoName)) {
                fldname = classInfo.getTextContent();
            } else if ("access".equals(classInfoName)) {
                access = Integer.valueOf(classInfo.getTextContent());
            } else if ("desc".equals(classInfoName)) {
                desc = classInfo.getTextContent();
            } else if ("signature".equals(classInfoName)) {
                signature = classInfo.getTextContent();
            } else if ("value".equals(classInfoName)) {
                value = classInfo.getTextContent();

                //unhexify the value.
                byte[] bytes = new byte[value.length() / 2];
                for (int i = 0; i < value.length(); i += 2) {
                    bytes[i / 2] = (byte) ((Character.digit(value.charAt(i), 16) << 4) + Character.digit(value.charAt(i + 1), 16));
                }
                value = new String(bytes);

            }
        }
        FieldDeclaration fd = new FieldDeclaration(access, fldname, desc, signature, value);
        return fd;
    }

    private static MethodDeclaration processMethodDecl(PkgInfo pi, String className, Node content) {
        int access = 0;
        String mthdname = null;
        String signature = null;
        String desc = null;
        List<String> exceptionsList = new ArrayList<String>();

        NodeList classContent = content.getChildNodes();
        for (int c = 0; c < classContent.getLength(); c++) {
            Node classInfo = classContent.item(c);
            String classInfoName = classInfo.getNodeName();
            if ("name".equals(classInfoName)) {
                mthdname = classInfo.getTextContent();
            } else if ("access".equals(classInfoName)) {
                access = Integer.valueOf(classInfo.getTextContent());
            } else if ("desc".equals(classInfoName)) {
                desc = classInfo.getTextContent();
            } else if ("signature".equals(classInfoName)) {
                signature = classInfo.getTextContent();
            } else if ("exceptions".equals(classInfoName)) {
                NodeList exs = classInfo.getChildNodes();
                for (int e = 0; e < exs.getLength(); e++) {
                    Node exc = exs.item(e);
                    if ("exception".equals(exc.getNodeName())) {
                        exceptionsList.add(exc.getTextContent());
                    }
                }
            }
        }
        String[] exceptions = exceptionsList.size() > 0 ? exceptionsList.toArray(new String[] {}) : null;

        if (mthdname == null || desc == null) {
            System.out.println("Hmm.. trying to create a method with a null name & desc .. ");
            System.out.println("content " + content.toString() + " content " + content.getNodeName());
        }

        MethodDeclaration md = new MethodDeclaration(access, mthdname, desc, signature, exceptions);
        return md;
    }

    private static ClassDeclaration processClsDecl(PkgInfo pi, Node content,
                                                   boolean processingFrameworkXml) {
        int access = 0;
        String clsname = null;
        String signature = null;
        String superName = null;
        String serialuid = null;
        List<String> interfaces = new ArrayList<String>();
        List<String> supers = new ArrayList<String>();
        List<MethodDeclaration> methods = new ArrayList<MethodDeclaration>();
        List<FieldDeclaration> fields = new ArrayList<FieldDeclaration>();
        Map<String, Set<MethodDeclaration>> supermethods = new HashMap<String, Set<MethodDeclaration>>();
        Map<String, FieldDeclaration> superfields = new HashMap<String, FieldDeclaration>();

        NodeList classContent = content.getChildNodes();
        for (int c = 0; c < classContent.getLength(); c++) {
            Node classInfo = classContent.item(c);
            String classInfoName = classInfo.getNodeName();
            if ("name".equals(classInfoName)) {
                clsname = classInfo.getTextContent();
            } else if ("access".equals(classInfoName)) {
                access = Integer.valueOf(classInfo.getTextContent());
            } else if ("superName".equals(classInfoName)) {
                superName = classInfo.getTextContent();
            } else if ("signature".equals(classInfoName)) {
                signature = classInfo.getTextContent();
            } else if ("serialuid".equals(classInfoName)) {
                serialuid = classInfo.getTextContent();
            } else if ("interfaces".equals(classInfoName)) {
                NodeList ifs = classInfo.getChildNodes();
                for (int i = 0; i < ifs.getLength(); i++) {
                    Node intf = ifs.item(i);
                    if ("intf".equals(intf.getNodeName())) {
                        interfaces.add(intf.getTextContent());
                    }
                }
            } else if ("supers".equals(classInfoName)) {
                NodeList ss = classInfo.getChildNodes();
                for (int s = 0; s < ss.getLength(); s++) {
                    Node spr = ss.item(s);
                    if ("super".equals(spr.getNodeName())) {
                        supers.add(spr.getTextContent());
                    }
                }
            } else if ("fields".equals(classInfoName)) {
                NodeList flds = classInfo.getChildNodes();
                for (int f = 0; f < flds.getLength(); f++) {
                    Node fld = flds.item(f);
                    if ("fielddecl".equals(fld.getNodeName())) {
                        FieldDeclaration fd = processFieldDecl(pi, clsname, fld);
                        if (fd != null)
                            fields.add(fd);
                    }
                }
            } else if ("methods".equals(classInfoName)) {
                NodeList mthds = classInfo.getChildNodes();
                for (int m = 0; m < mthds.getLength(); m++) {
                    Node mthd = mthds.item(m);
                    if ("methoddecl".equals(mthd.getNodeName())) {
                        MethodDeclaration md = processMethodDecl(pi, clsname, mthd);
                        if (md != null)
                            methods.add(md);
                    }
                }
            } else if ("superfields".equals(classInfoName)) {
                NodeList flds = classInfo.getChildNodes();
                for (int f = 0; f < flds.getLength(); f++) {
                    Node fld = flds.item(f);
                    if ("fielddecl".equals(fld.getNodeName())) {
                        FieldDeclaration fd = processFieldDecl(pi, clsname, fld);
                        if (fd != null)
                            superfields.put(fd.getName(), fd);
                    }
                }
            } else if ("supermethods".equals(classInfoName)) {
                NodeList mthds = classInfo.getChildNodes();
                for (int m = 0; m < mthds.getLength(); m++) {
                    Node mthd = mthds.item(m);
                    if ("methoddecl".equals(mthd.getNodeName())) {
                        MethodDeclaration md = processMethodDecl(pi, clsname, mthd);
                        if (md != null) {
                            if (!supermethods.containsKey(md.getName())) {
                                supermethods.put(md.getName(), new HashSet<MethodDeclaration>());
                            }
                            supermethods.get(md.getName()).add(md);
                        }
                    }
                }
            }
        }

        if (processingFrameworkXml && GlobalConfig.isClassIgnoredInBaseline(clsname)) {
            System.out.println("Suppressing " + clsname + " as it's ignored by config");
            return null;
        }

        ClassDeclaration cd = new ClassDeclaration(access, clsname, signature, superName, interfaces.toArray(new String[] {}));

        //add in data not addable via constructor.
        if (serialuid != null) {
            cd.setSerialVersionUID(Long.valueOf(serialuid));
        }
        for (MethodDeclaration md : methods) {
            cd.addMethods(md);
        }
        for (FieldDeclaration fd : fields) {
            cd.addFields(fd);
        }
        cd.addFieldInUpperChain(superfields);
        cd.addMethodsInUpperChain(supermethods);
        for (String spr : supers) {
            cd.addSuper(spr);
        }

        return cd;
    }

}
