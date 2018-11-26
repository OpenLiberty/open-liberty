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
package com.ibm.ws.jpa.diagnostics.puscanner;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.spi.PersistenceUnitInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.AnnotationInfoType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.AnnotationValueType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.AnnotationsType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ArrayEntryType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ArrayInstanceType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ClassInfoType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ClassInformationType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ExceptionType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.FieldInfoType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.FieldsType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.InterfacesType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ListEntryType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ListInstanceType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.MapEntryType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.MapInstanceType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.MethodInfoType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.MethodsType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ModifierType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ModifiersType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ObjectFieldInstanceType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ObjectInstanceType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ParameterType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ParametersType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ValueInstanceType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ValueType;
import com.ibm.ws.jpa.diagnostics.ormparser.EntityMappingsDefinition;
import com.ibm.ws.jpa.diagnostics.utils.encapsulation.EncapsulatedData;
import com.ibm.ws.jpa.diagnostics.utils.encapsulation.EncapsulatedDataGroup;

/**
 *
 */
public class PersistenceUnitScannerResults {
    private final List<PersistenceUnitInfo> puiList = new ArrayList<PersistenceUnitInfo>();
    private final Map<URL, String> pxmlMap = new HashMap<URL, String>();

    private final Set<URL> urlSet = new HashSet<URL>();
    private final Map<URL, Set<ClassInfoType>> scannedClassesMap = new HashMap<URL, Set<ClassInfoType>>();

    private final Map<URL, EntityMappingsDefinition> scanned_ormfile_map = new HashMap<URL, EntityMappingsDefinition>();
    private final Map<PersistenceUnitInfo, List<EntityMappingsDefinition>> pu_ormFileParsed_map = new HashMap<PersistenceUnitInfo, List<EntityMappingsDefinition>>();

    public PersistenceUnitScannerResults(List<PersistenceUnitInfo> puiList,
                                         Map<URL, String> pxmlMap,
                                         Set<URL> urlSet,
                                         Map<URL, Set<ClassInfoType>> scannedClassesMap,
                                         Map<URL, EntityMappingsDefinition> scanned_ormfile_map,
                                         Map<PersistenceUnitInfo, List<EntityMappingsDefinition>> pu_ormFileParsed_map) {
        if (puiList != null)
            this.puiList.addAll(puiList);

        if (pxmlMap != null)
            this.pxmlMap.putAll(pxmlMap);

        if (urlSet != null)
            this.urlSet.addAll(urlSet);

        if (scannedClassesMap != null)
            this.scannedClassesMap.putAll(scannedClassesMap);

        if (scanned_ormfile_map != null)
            this.scanned_ormfile_map.putAll(scanned_ormfile_map);

        if (pu_ormFileParsed_map != null)
            this.pu_ormFileParsed_map.putAll(pu_ormFileParsed_map);
    }

    public List<PersistenceUnitInfo> getPersistenceUnitInfoList() {
        return Collections.unmodifiableList(puiList);
    }

    public Map<URL, EntityMappingsDefinition> getAllEntityMappingsDefinitions() {
        return Collections.unmodifiableMap(scanned_ormfile_map);
    }

    public Map<URL, Set<ClassInfoType>> getAllScannedClasses() {
        final Map<URL, Set<ClassInfoType>> retMap = new HashMap<URL, Set<ClassInfoType>>();
        for (Map.Entry<URL, Set<ClassInfoType>> entry : scannedClassesMap.entrySet()) {
            retMap.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
        }

        return retMap;
    }

    public List<EntityMappingsDefinition> getEntityMappingsForPersistenceUnit(PersistenceUnitInfo pui) {
        List<EntityMappingsDefinition> entMappings = pu_ormFileParsed_map.get(pui);
        if (entMappings == null) {
            return new ArrayList<EntityMappingsDefinition>();
        }

        return Collections.unmodifiableList(entMappings);
    }

    public Map<URL, Set<ClassInfoType>> getScannedClassesForPersistenceUnit(PersistenceUnitInfo pui) {
        HashSet<URL> urlSet = new HashSet<URL>();
        urlSet.add(pui.getPersistenceUnitRootUrl());
        if (pui.getJarFileUrls() != null) {
            urlSet.addAll(pui.getJarFileUrls());
        }

        HashMap<URL, Set<ClassInfoType>> retMap = new HashMap<URL, Set<ClassInfoType>>();
        for (URL url : urlSet) {
            if (scannedClassesMap.containsKey(url)) {
                retMap.put(url, Collections.unmodifiableSet(scannedClassesMap.get(url)));
            }
        }

        return retMap;
    }

    public void generateORMDump(PrintWriter out) {
        // Map Persistence Unit Root URLs to PersistenceUnitInfo instances
        final Map<URL, List<PersistenceUnitInfo>> commonPuRootMap = new HashMap<URL, List<PersistenceUnitInfo>>();
        for (PersistenceUnitInfo pui : puiList) {
            final URL puRoot = pui.getPersistenceUnitRootUrl();
            List<PersistenceUnitInfo> puList = commonPuRootMap.get(puRoot);
            if (puList == null) {
                puList = new ArrayList<PersistenceUnitInfo>();
                commonPuRootMap.put(puRoot, puList);
            }
            puList.add(pui);
        }

        final EncapsulatedDataGroup root = EncapsulatedDataGroup.createEncapsulatedDataGroup("ORMDiagnostics", "ORMDiagnostics");
        int id = 0;

        // Set Properties
        root.setProperty("execution.environment", "WebSphere Liberty");

        //
        // Store persistence.xml documents in /PersistenceXML
        // Properties:
        //   pu.root.url - persistence unit root URL
        //

        // Folder for containing application persistence.xml documents
        final String pxmlFolderId = new Integer(id++).toString();
        final EncapsulatedDataGroup pxmlFolder = EncapsulatedDataGroup.createEncapsulatedDataGroup("PersistenceXML", pxmlFolderId);
        root.putDataSubGroup(pxmlFolder);

        final Map<URL, String> puRootToEncapIdMap = new HashMap<URL, String>();
        for (Map.Entry<URL, String> entry : pxmlMap.entrySet()) {
            final String pxmlDocId = new Integer(id++).toString();
            try {
                final EncapsulatedData ed = EncapsulatedData.createEncapsulatedData("persistence.xml", pxmlDocId, entry.getValue().getBytes());
                ed.setProperty("pu.root.url", entry.getKey().toString());
                pxmlFolder.putDataItem(ed);
                puRootToEncapIdMap.put(entry.getKey(), pxmlDocId);
            } catch (Exception e) {
                FFDCFilter.processException(e, PersistenceUnitScannerResults.class.getName() + ".generateORMDump", "pxml-proc");
            }
        }

        //
        // Store server PersistenceUnitInfo.toString() output
        // Properties:
        //    pu.name - persistence unit name
        //    pu.root.url - persistence unit root URL
        //
        final String pxmlToStringFolderId = new Integer(id++).toString();
        final EncapsulatedDataGroup pxmlToStringFolder = EncapsulatedDataGroup.createEncapsulatedDataGroup("PersistenceXMLToString", pxmlToStringFolderId);
        root.putDataSubGroup(pxmlToStringFolder);
        for (Map.Entry<URL, List<PersistenceUnitInfo>> entry : commonPuRootMap.entrySet()) {
            for (PersistenceUnitInfo pui : entry.getValue()) {
                final String docId = new Integer(id++).toString();
                try {
                    final EncapsulatedData ed = EncapsulatedData.createEncapsulatedData("jpa-persistence-unit-info", docId, pui.toString().getBytes());
                    ed.setProperty("pu.name", pui.getPersistenceUnitName());
                    ed.setProperty("pu.root.url", entry.getKey().toString());
                    pxmlToStringFolder.putDataItem(ed);
                } catch (Exception e) {
                    FFDCFilter.processException(e, PersistenceUnitScannerResults.class.getName() + ".generateORMDump", "pxml-tostring");
                }
            }
        }

        //
        // Store Object Relational Mapping Files
        // Properties:
        //   url - URL of the ORM asset
        //
        final String ormFileFolderId = new Integer(id++).toString();
        final EncapsulatedDataGroup ormFileFolder = EncapsulatedDataGroup.createEncapsulatedDataGroup("Object Relational Mapping Files", ormFileFolderId);
        root.putDataSubGroup(ormFileFolder);
        for (Map.Entry<URL, EntityMappingsDefinition> entry : scanned_ormfile_map.entrySet()) {
            final String docId = new Integer(id++).toString();
            try {
                final EntityMappingsDefinition emd = entry.getValue();
                final EncapsulatedData ed = EncapsulatedData.createEncapsulatedData("ORM File", docId, emd.getFileData());
                ed.setProperty("url", emd.getSource().toString());
                ormFileFolder.putDataItem(ed);
            } catch (Exception e) {
                FFDCFilter.processException(e, PersistenceUnitScannerResults.class.getName() + ".generateORMDump", "orm-file-collection");
            }
        }

        //
        // Store Processed Class Info
        // Properties:
        // url - URL of the archive containing the analyzed class
        //
        final String scannedClassesFolderId = new Integer(id++).toString();
        final EncapsulatedDataGroup scannedClassesFolder = EncapsulatedDataGroup.createEncapsulatedDataGroup("Scanned Classes", scannedClassesFolderId);
        root.putDataSubGroup(scannedClassesFolder);
        for (Map.Entry<URL, Set<ClassInfoType>> entry : scannedClassesMap.entrySet()) {
            final String docId = new Integer(id++).toString();
            try {
                final ClassInformationType cit = new ClassInformationType();
                cit.getClassInfo().addAll(entry.getValue());

                ClassLoader currentClassLoader = AccessController.doPrivileged(
                                                                               new PrivilegedAction<ClassLoader>() {
                                                                                   @Override
                                                                                   public ClassLoader run() {
                                                                                       return PersistenceUnitScannerResults.class.getClassLoader();
                                                                                   }
                                                                               });
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final JAXBContext jaxbCtx = JAXBContext.newInstance("com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10", currentClassLoader);
                final Marshaller marshaller = jaxbCtx.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                marshaller.marshal(cit, baos);

                final EncapsulatedData ed = EncapsulatedData.createEncapsulatedData("Classes", docId, baos.toByteArray());
                ed.setProperty("url", entry.getKey().toString());
                scannedClassesFolder.putDataItem(ed);
            } catch (Exception e) {
                FFDCFilter.processException(e, PersistenceUnitScannerResults.class.getName() + ".generateORMDump", "orm-file-collection");
            }
        }

        out.println();
        out.println("Encapsulated JPA Diagnostic Data:");
        out.println();
        try {
            root.write(out);
        } catch (Exception e) {
            FFDCFilter.processException(e, PersistenceUnitScannerResults.class.getName() + ".generateORMDump", "store-encapsulated-data");
        }
    }

    public void printReport(PrintWriter out) {
        // Print ORM Files
        for (Map.Entry<URL, EntityMappingsDefinition> entry : scanned_ormfile_map.entrySet()) {
            out.println();
            out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
            out.println("Object Relational Mapping (ORM) File:");

            out.println("Location: " + PersistenceUnitScannerResults.getShortenedURLPath(entry.getKey()));

            out.println("Referenced by Persistence Unit:");
            for (Map.Entry<PersistenceUnitInfo, List<EntityMappingsDefinition>> e2 : pu_ormFileParsed_map.entrySet()) {
                if (e2.getValue().contains(entry.getValue())) {
                    out.println("   " + e2.getKey().getPersistenceUnitName());
                }
            }

            out.println();
            out.println(new String(entry.getValue().getFileData()));
        }

        // Print Scanned Classes
        for (Map.Entry<URL, Set<ClassInfoType>> entry : scannedClassesMap.entrySet()) {
            out.println();
            out.println("Scanned Classes for location: " + entry.getKey());

            final ArrayList<ClassInfoType> sortedCitList = new ArrayList<ClassInfoType>(entry.getValue());
            Collections.sort(sortedCitList, new Comparator<ClassInfoType>() {
                @Override
                public int compare(ClassInfoType o1, ClassInfoType o2) {
                    if (o1 == null || o2 == null || o1.getClassName() == null || o2.getClassName() == null)
                        return 0;
                    return o1.getClassName().compareTo(o2.getClassName());
                }
            });

            for (ClassInfoType cit : sortedCitList) {
                out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
                out.println("Class: " + cit.getClassName());
                out.println("Location: " + PersistenceUnitScannerResults.getShortenedURLPath(entry.getKey()));
                out.println();
                out.println(process(cit));
            }
            out.println();
        }
    }

    public static String process(ClassInfoType cit) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(cit.getPackageName()).append(";").append("\n");
        sb.append("\n");

        processAnnotationsType(cit.getAnnotations(), "", sb, false);

        printModifiersType(cit.getModifiers(), sb);

        if (cit.isIsInterface()) {
            sb.append("interface ");
        } else {
            sb.append("class ");
        }

        sb.append(cit.getName());
        final String superclassName = cit.getSuperclassName();
        if (superclassName != null && !superclassName.trim().isEmpty() && !superclassName.trim().equals("java.lang.Object")) {
            sb.append(" extends ").append(superclassName.replace("/", ".")).append(" ");
        }

        final InterfacesType ifaces = cit.getInterfaces();
        if (ifaces != null) {
            List<String> ifaceList = ifaces.getInterface();
            if (ifaceList.size() > 0) {
                sb.append(" implements ");
                boolean first = true;
                for (String iface : ifaceList) {
                    if (!first) {
                        sb.append(", ");
                    } else {
                        first = false;
                    }
                    sb.append(iface.replace("/", "."));
                }
            }
        }

        sb.append(" {\n");

        printFields(cit, sb);
        sb.append("\n");
        printMethods(cit, sb);

        sb.append("\n}\n");
        return sb.toString();
    }

    private static void printModifiersType(ModifiersType mods, StringBuilder sb) {
        if (mods == null) {
            return;
        }

        List<ModifierType> modList = mods.getModifier();
        for (ModifierType mt : modList) {
            sb.append(mt.toString().toLowerCase());
            sb.append(" ");
        }
    }

    private static void printFields(ClassInfoType cit, StringBuilder sb) {
        FieldsType ft = cit.getFields();
        if (ft == null) {
            return;
        }
        sb.append("  // Fields\n");

        boolean first = true;
        List<FieldInfoType> fiList = ft.getField();
        for (FieldInfoType fit : fiList) {
            if (first) {
                first = false;
            } else {
                sb.append("\n");
            }
            processAnnotationsType(fit.getAnnotations(), "  ", sb, false);
            sb.append("  ");
            printModifiersType(fit.getModifiers(), sb);
            sb.append(fit.getType()).append(" ");
            sb.append(fit.getName()).append(";\n");
        }
    }

    private static void printMethods(ClassInfoType cit, StringBuilder sb) {
        MethodsType mt = cit.getMethods();
        if (mt == null) {
            return;
        }

        sb.append("  // Methods\n");

        boolean first = true;
        List<MethodInfoType> miList = mt.getMethod();
        for (MethodInfoType mit : miList) {
            if (first) {
                first = false;
            } else {
                sb.append("\n");
            }

            processAnnotationsType(mit.getAnnotations(), "  ", sb, false);
            sb.append("  ");
            printModifiersType(mit.getModifiers(), sb);

            if (mit.isIsCtor()) {
                sb.append(cit.getName());
            } else {
                sb.append(mit.getReturnType()).append(" ");
                sb.append(mit.getMethodName());
            }

            sb.append("(");

            ParametersType pt = mit.getParameters();
            if (pt != null) {
                boolean first2 = true;

                List<ParameterType> ptList = pt.getParameter();
                for (ParameterType parmType : ptList) {
                    if (first2) {
                        first2 = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(parmType.getType());
                    if (parmType.isIsArray()) {
                        sb.append("[]");
                    }
                }
            }

            sb.append(")");

            if (mit.getExceptions() != null) {
                List<ExceptionType> exList = mit.getExceptions().getException();
                sb.append(" throws ");

                boolean first2 = true;
                for (ExceptionType et : exList) {
                    if (first2) {
                        first2 = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(et.getExceptionType());
                }
                sb.append(" ");
            }

            sb.append(";\n");

        }
    }

    private static void processAnnotationsType(AnnotationsType at, String indent, StringBuilder sb, boolean printComma) {
        if (indent == null) {
            indent = "";
        }

        if (at == null) {
            return;
        }

        boolean first = true;

        List<AnnotationInfoType> annoInfoTypeList = at.getAnnotation();
        for (AnnotationInfoType ait : annoInfoTypeList) {
            if (printComma) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
            }
            processAnnotationInfoType(ait, indent, sb);
        }
    }

    private static void processAnnotationInfoType(AnnotationInfoType ait, String indent, StringBuilder sb) {
        final String type = ait.getType();
        sb.append(indent).append("@").append(type);

        final List<AnnotationValueType> avtList = ait.getAnnoKeyVal();
        if (avtList.size() > 0) {
            sb.append("(");

            boolean first = true;

            for (AnnotationValueType avt : avtList) {
                final String name = avt.getName();

                final AnnotationInfoType ait2 = avt.getAnnotation();
                final AnnotationsType at2 = avt.getAnnotations();
                final ValueInstanceType vit = avt.getValue();
                final ArrayInstanceType arrit = avt.getValues();

                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(name).append("=");

                if (vit != null) {
                    processValueInstanceType(vit, sb);
                } else if (ait2 != null) {
                    processAnnotationInfoType(ait2, indent + "  ", sb);
                } else if (at2 != null) {
                    sb.append("{ ");
                    processAnnotationsType(at2, indent + "  ", sb, true);
                    sb.append("} ");
                }
            }

            sb.append(")");
        }

        sb.append("\n");

    }

    private static void processValueInstanceType(ValueInstanceType vit, StringBuilder sb) {
        final ValueType valType = vit.getType();
        if (valType == ValueType.ARRAY) {
            final ArrayInstanceType arrIt = vit.getArray();
            final List<ArrayEntryType> entryList = arrIt.getEntry();
            try {
                sb.append("[");
                boolean first = true;
                for (ArrayEntryType aet : entryList) {
                    if (!first) {
                        sb.append(", ");
                    } else {
                        first = false;
                    }
                    processValueInstanceType(aet.getValue(), sb);
                }
            } finally {
                sb.append("]");
            }
        } else if (valType == ValueType.LIST) {
            final ListInstanceType lit = vit.getList();
            final List<ListEntryType> letList = lit.getEntry();
            try {
                sb.append("{");
                boolean first = true;
                for (ListEntryType let : letList) {
                    if (!first) {
                        sb.append(", ");
                    } else {
                        first = false;
                    }
                    processValueInstanceType(let.getValue(), sb);
                }
            } finally {
                sb.append("}");
            }
        } else if (valType == ValueType.MAP) {
            final MapInstanceType mit = vit.getMap();
            final List<MapEntryType> metList = mit.getEntry();
            try {
                sb.append("{");

                boolean first = true;
                for (MapEntryType met : metList) {
                    if (!first) {
                        sb.append(", ");
                    } else {
                        first = false;
                    }
                    processValueInstanceType(met.getKey(), sb);
                    sb.append(" = ");
                    processValueInstanceType(met.getValue(), sb);
                }

            } finally {
                sb.append("}");
            }
        } else if (valType == ValueType.OBJECT) {
            final ObjectInstanceType oit = vit.getObject();
            final String className = oit.getClassName();
            final int id = oit.getId();
            final List<ObjectFieldInstanceType> fieldList = oit.getField();

            sb.append("<<Object <").append(className).append("> ");
            sb.append("id=").append(id).append(" ");

            try {
                boolean first = true;
                for (ObjectFieldInstanceType field : fieldList) {
                    if (!first) {
                        sb.append(", ");
                    } else {
                        first = false;
                    }

                    final String fieldName = field.getName();
                    sb.append(fieldName).append("= |");
                    processValueInstanceType(field.getValue(), sb);
                    sb.append("|");
                }
            } finally {
                sb.append(">>");
            }
        } else if (valType == ValueType.JAVA_LANG_STRING) {
            sb.append("\"").append(vit.getSimple()).append("\"");
        } else {
            sb.append(vit.getSimple());
        }
    }

    private static String getShortenedURLPath(URL url) {
        final String urlStr = url.toString().replace("%21", "!");
        final String ptcols = urlStr.substring(0, urlStr.indexOf("/"));
        String path = urlStr.substring(urlStr.indexOf("/"));
        if (path.contains(".cache")) {
            path = path.substring(path.lastIndexOf(".cache") + 6);
        }

        int index = path.length();
        int temp = path.lastIndexOf(".jar");
        if (temp > 0)
            index = temp;
        temp = path.lastIndexOf(".war", index);
        if (temp > 0)
            index = temp;
        temp = path.lastIndexOf(".ear", index);
        if (temp > 0)
            index = temp;

        if (index > 0) {
            index = path.lastIndexOf("/", index);
            path = path.substring(index);
        }
        return ptcols + "..." + path;
    }
}
