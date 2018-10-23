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
package com.ibm.ws.jpa.management;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jpa.diagnostics.JPAORMDiagnostics;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ClassInfoType;
import com.ibm.ws.jpa.diagnostics.puscanner.PersistenceUnitScannerResults;

/**
 *
 */
public class JPAIntrospection {
    private static final TraceComponent tc = Tr.register(JPAIntrospection.class,
                                                         "JPAORM",
                                                         "com.ibm.ws.jpa.jpa");

    private static final ThreadLocal<JPAIntrospection> threadLocal = new ThreadLocal<JPAIntrospection>() {
        @Override
        protected JPAIntrospection initialValue() {
            return new JPAIntrospection();
        }
    };

    public static final void beginJPAIntrospection() {
        final JPAIntrospection jpaIntrospector = threadLocal.get();
        jpaIntrospector.validIntrospector = true;
    }

    public static final void endJPAIntrospection() {
        threadLocal.remove();
    }

    public static final void beginApplicationVisit(String appname, JPAApplInfo appl) {
        final JPAIntrospection jpaIntrospector = getJPAIntrospection();
        if (jpaIntrospector != null) {
            try {
                jpaIntrospector.doBeginApplicationVisit(appname, appl);
            } catch (Throwable t) {
                FFDCFilter.processException(t, JPAIntrospection.class.getName() + ".beginApplicationVisit", "66");
            }
        }
    }

    public static final void endApplicationVisit() {
        final JPAIntrospection jpaIntrospector = getJPAIntrospection();
        if (jpaIntrospector != null) {
            jpaIntrospector.doEndApplicationVisit();
        }
    }

    public static final void beginPUScopeVisit(JPAScopeInfo scopeInfo) {
        final JPAIntrospection jpaIntrospector = getJPAIntrospection();
        if (jpaIntrospector != null) {
            try {
                jpaIntrospector.doBeginPUScopeVisit(scopeInfo);
            } catch (Throwable t) {
                FFDCFilter.processException(t, JPAIntrospection.class.getName() + ".beginPUScopeVisit", "84");
            }

        }
    }

    public static final void endPUScopeVisit() {
        final JPAIntrospection jpaIntrospector = getJPAIntrospection();
        if (jpaIntrospector != null) {
            jpaIntrospector.doEndPUScopeVisit();
        }
    }

    public static final void beginPXmlInfoVisit(JPAPxmlInfo pxmlInfo) {
        final JPAIntrospection jpaIntrospector = getJPAIntrospection();
        if (jpaIntrospector != null) {
            try {
                jpaIntrospector.doBeginPXmlInfoVisit(pxmlInfo);
            } catch (Throwable t) {
                FFDCFilter.processException(t, JPAIntrospection.class.getName() + ".beginPXmlInfoVisit", "103");
            }
        }
    }

    public static final void endPXmlInfoVisit() {
        final JPAIntrospection jpaIntrospector = getJPAIntrospection();
        if (jpaIntrospector != null) {
            jpaIntrospector.doEndPXmlInfoVisit();
        }
    }

    public static final void visitJPAPUnitInfo(String puName, JPAPUnitInfo jpaPuInfo) {
        final JPAIntrospection jpaIntrospector = getJPAIntrospection();
        if (jpaIntrospector != null) {
            try {
                jpaIntrospector.doVisitJPAPUnitInfo(puName, jpaPuInfo);
            } catch (Throwable t) {
                FFDCFilter.processException(t, JPAIntrospection.class.getName() + ".visitJPAPUnitInfo", "121");
            }
        }
    }

    public static final void executeIntrospectionAnalysis(final PrintWriter dout) {
        final JPAIntrospection jpaIntrospector = getJPAIntrospection();
        if (jpaIntrospector != null) {
            try {
                jpaIntrospector.doExecuteIntrospectionAnalysis(dout);
                dout.println();
                jpaIntrospector.doDumpJPARuntimeStates(dout);
                dout.println();
                jpaIntrospector.doDumpOpenJPAPCRegistry(dout);
            } catch (Throwable t) {
                FFDCFilter.processException(t, JPAIntrospection.class.getName() + ".executeIntrospectionAnalysis", "132");
            }
        }
    }

    public static final void executeTraceAnalysis() {
        final JPAIntrospection jpaIntrospector = getJPAIntrospection();
        if (jpaIntrospector != null) {
            try {
                jpaIntrospector.doExecuteTraceAnalysis();
            } catch (Throwable t) {
                FFDCFilter.processException(t, JPAIntrospection.class.getName() + ".executeTraceAnalysis", "143");
            }
        }
    }

    private static final JPAIntrospection getJPAIntrospection() {
        final JPAIntrospection jpaIntrospector = threadLocal.get();
        if (jpaIntrospector.validIntrospector == false) {
            threadLocal.remove();
            return null;
        }

        return jpaIntrospector;
    }

    /*
     * Internal Implementation
     */

    private boolean validIntrospector = false;

    private final HashMap<String, JPAApplInfoIntrospect> jpaApplInfoMap = new HashMap<String, JPAApplInfoIntrospect>();
    private final List<JPAPUnitInfo> allJPAPUnitInfoList = new ArrayList<JPAPUnitInfo>();

    private JPAApplInfoIntrospect currentAppl = null;
    private JPAScopeInfoIntrospect currentScopeInfo = null;
    private JPAPxmlInfoIntrospect currentPxmlInfo = null;

    public final Map<String, JPAApplInfoIntrospect> getJPAApplInfoIntrospectMap() {
        return Collections.unmodifiableMap(jpaApplInfoMap);
    }

    private void doBeginApplicationVisit(String appname, JPAApplInfo appl) {
        currentAppl = new JPAApplInfoIntrospect(appname, appl);
        jpaApplInfoMap.put(appname, currentAppl);
    }

    private void doEndApplicationVisit() {
        currentAppl = null;
    }

    private void doBeginPUScopeVisit(JPAScopeInfo scopeInfo) {
        if (currentAppl == null) {
            return; // Bad State
        }
        currentScopeInfo = new JPAScopeInfoIntrospect(scopeInfo);
        currentAppl.scopeInfoList.add(currentScopeInfo);
    }

    private void doEndPUScopeVisit() {
        currentScopeInfo = null;
    }

    private void doBeginPXmlInfoVisit(JPAPxmlInfo pxmlInfo) {
        if (currentAppl == null || currentScopeInfo == null) {
            return; // Bad State
        }
        currentPxmlInfo = new JPAPxmlInfoIntrospect(pxmlInfo);
        currentScopeInfo.pxmlInfoList.add(currentPxmlInfo);
    }

    private void doEndPXmlInfoVisit() {
        currentPxmlInfo = null;
    }

    private void doVisitJPAPUnitInfo(String puName, JPAPUnitInfo jpaPuInfo) {
        if (currentAppl == null || currentScopeInfo == null || currentPxmlInfo == null) {
            return; // Bad State
        }

        currentPxmlInfo.jpaPuInfoList.add(new JPAPUnitInfoIntrospect(puName, jpaPuInfo));
        currentAppl.puCount++;
        currentPxmlInfo.puCount++;

        allJPAPUnitInfoList.add(jpaPuInfo);
    }

    private Map<String, JPAAnalysisResult> executeDiagnostics(boolean isIntrospectorDump) {
        final Map<String, JPAAnalysisResult> analysisResultMap = new HashMap<String, JPAAnalysisResult>();

        for (Map.Entry<String, JPAApplInfoIntrospect> entry : jpaApplInfoMap.entrySet()) {
            final String appName = entry.getKey();
            final JPAApplInfoIntrospect jpaAppl = entry.getValue();
            final JPAApplInfo appl = jpaAppl.getJPAApplInfo();

            final JPAAnalysisResult analysisResult = new JPAAnalysisResult(appName, jpaAppl);
            analysisResultMap.put(appName, analysisResult);

            final PrintWriter out = analysisResult.getPw();

            final ArrayList<JPAPUnitInfo> puInfoList = new ArrayList<JPAPUnitInfo>();
            final HashMap<URL, List<JPAPUnitInfo>> puRootURL_PUInfo_Map = new HashMap<URL, List<JPAPUnitInfo>>();
            final HashMap<URL, String> puRootURL_pxml_Map = new HashMap<URL, String>();

            final List<JPAScopeInfoIntrospect> scopeInfoList = jpaAppl.getScopeInfoList();
            for (JPAScopeInfoIntrospect puScope : scopeInfoList) {
                final JPAScopeInfo scopeInfo = puScope.getJPAScopeInfo();

                final List<JPAPxmlInfoIntrospect> pxmlList = puScope.getPxmlInfoList(); // persistence.xml
                for (JPAPxmlInfoIntrospect pxml : pxmlList) {
                    final JPAPxmlInfo pxmlInfo = pxml.getJPAPxmlInfo();
                    final List<JPAPUnitInfoIntrospect> jpaPuInfoList = pxml.getJPAPUnitInfoList();
                    for (JPAPUnitInfoIntrospect puInfoIntro : jpaPuInfoList) {
                        final JPAPUnitInfo jpaPuInfo = puInfoIntro.getJpaPuInfo();
                        final URL puRootURL = jpaPuInfo.getPersistenceUnitRootUrl();

                        puInfoList.add(jpaPuInfo);
                        analysisResult.registerJPAPUInfo(puInfoIntro);

                        List<JPAPUnitInfo> list = puRootURL_PUInfo_Map.get(puRootURL);
                        if (list == null) {
                            list = new ArrayList<JPAPUnitInfo>();
                            puRootURL_PUInfo_Map.put(puRootURL, list);
                        }
                        list.add(jpaPuInfo);
                    }
                }
            }

            // Get the persistence.xml associated with each persistence unit root.
            for (URL url : puRootURL_PUInfo_Map.keySet()) {
                String pxml = resolvePersistenceXML(url);
                puRootURL_pxml_Map.put(url, pxml);
            }

            if (isIntrospectorDump) {
                for (Map.Entry<URL, String> ent : puRootURL_pxml_Map.entrySet()) {
                    out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                    out.println("Contents of persistence.xml at Peristence Unit Root:\n" + ent.getKey());
                    out.println();
                    out.println(ent.getValue());
                    out.println();

                    List<JPAPUnitInfo> puInfo_List = puRootURL_PUInfo_Map.get(ent.getKey());
                    for (JPAPUnitInfo puInfo : puInfo_List) {
                        out.println(puInfo.dump());
                        out.println("************************************************************");
                    }
                }

                jpaAppl.results = JPAORMDiagnostics.performJPAORMDiagnosticsForIntrospector(
                                                                                            new ArrayList<javax.persistence.spi.PersistenceUnitInfo>(puInfoList),
                                                                                            puRootURL_pxml_Map,
                                                                                            out);
            } else {
                jpaAppl.results = JPAORMDiagnostics.performJPAORMDiagnosticsForTrace(
                                                                                     new ArrayList<javax.persistence.spi.PersistenceUnitInfo>(puInfoList),
                                                                                     puRootURL_pxml_Map);
            }

        }

        return analysisResultMap;
    }

    private void doExecuteTraceAnalysis() {
        if (!(tc.isAnyTracingEnabled() && tc.isDebugEnabled())) {
            return;
        }

        try {
            final Map<String, JPAAnalysisResult> analysisResultMap = executeDiagnostics(false);

            for (Map.Entry<String, JPAAnalysisResult> entry : analysisResultMap.entrySet()) {
                final JPAAnalysisResult result = entry.getValue();
                final JPAApplInfoIntrospect applInfoIntro = result.getAppl();
                final PersistenceUnitScannerResults results = applInfoIntro.results;

                if (results == null) {
                    continue;
                }

                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                results.generateORMDump(new PrintWriter(baos));

                final StringBuilder sb = new StringBuilder();
                sb.append("\nJPA Diagnostic Dump for Application " + entry.getKey() + " : \n");
                sb.append(baos.toString());

                Tr.debug(tc, "JPAORMDiagnostics Dump", sb.toString());
            }
        } catch (Throwable t) {
            FFDCFilter.processException(t, JPAIntrospection.class.getName() + ".executeTraceAnalysis", "185");
        }
    }

    private void doExecuteIntrospectionAnalysis(final PrintWriter dout) {
        final Map<String, JPAAnalysisResult> analysisResultMap = executeDiagnostics(true);

        dout.println("Applications: ");
        dout.println();

        for (Map.Entry<String, JPAAnalysisResult> entry : analysisResultMap.entrySet()) {
            dout.println(entry.getKey() + " ("
                         + entry.getValue().getAppl().getPersistenceUnitCount()
                         + " persistence units, " + entry.getValue().getLineCount() + " lines) :");
            dout.println("   Persistence Unit Roots:");
            final Map<URL, List<JPAPUnitInfoIntrospect>> urlPUInfoMap = entry.getValue().getURL_JPAPuInfoMap();
            if (urlPUInfoMap.size() == 0) {
                dout.println("      None");
            } else {
                for (Map.Entry<URL, List<JPAPUnitInfoIntrospect>> e2 : urlPUInfoMap.entrySet()) {
                    dout.println("      " + getShortenedURLPath(e2.getKey()) + " (" + e2.getValue().size() + " persistence units)");
                }
            }
        }

        dout.println();

        for (Map.Entry<String, JPAAnalysisResult> entry : analysisResultMap.entrySet()) {
            final JPAAnalysisResult result = entry.getValue();
            final PersistenceUnitScannerResults scannerResults = result.getAppl().results;

            dout.println();
            dout.println("################################################################################");
            dout.println("Application \"" + entry.getKey() + "\":");

            if (scannerResults == null) {
                dout.println("   No JPA Materials to Analyze.");
                continue;
            }

            dout.println("   Total ORM Files: " + scannerResults.getAllEntityMappingsDefinitions().size());

            {
                final Map<URL, Set<ClassInfoType>> urlCitMap = scannerResults.getAllScannedClasses();
                int count = 0;
                for (Set<ClassInfoType> citSet : urlCitMap.values()) {
                    count += citSet.size();
                }

                dout.println("   Total JPA Involved Classes: " + count);
            }

            final Map<URL, List<JPAPUnitInfoIntrospect>> urlPUInfoMap = entry.getValue().getURL_JPAPuInfoMap();
            if (urlPUInfoMap.size() == 0) {
                dout.println("   Persistence Unit Roots:");
                dout.println("      None");
            } else {
                dout.println("   Persistence Unit Roots:");
                for (Map.Entry<URL, List<JPAPUnitInfoIntrospect>> e2 : urlPUInfoMap.entrySet()) {
                    dout.println("      " + getShortenedURLPath(e2.getKey()) + " (" + e2.getValue().size() + " persistence units)");
                }

                dout.println("   Persistence Units:");
                for (Map.Entry<URL, List<JPAPUnitInfoIntrospect>> e2 : urlPUInfoMap.entrySet()) {
                    dout.println("      At Persistence Unit Root: " + getShortenedURLPath(e2.getKey()));
                    final List<JPAPUnitInfoIntrospect> ispecList = new ArrayList<JPAPUnitInfoIntrospect>(e2.getValue());
                    Collections.sort(ispecList, new Comparator<JPAPUnitInfoIntrospect>() {
                        @Override
                        public int compare(JPAPUnitInfoIntrospect o1, JPAPUnitInfoIntrospect o2) {
                            if (o1 == null || o2 == null || o1.puName == null || o2.puName == null) {
                                return 0;
                            }

                            return o1.puName.compareTo(o2.puName);
                        }
                    });
                    for (JPAPUnitInfoIntrospect puIspec : ispecList) {
                        dout.println("         " + puIspec.getPuName());
                    }
                }
            }

            dout.println();
            dout.println(result.getBaos().toString());
        }
    }

    private String getShortenedURLPath(URL url) {
        final String urlStr = url.toString().replace("%21", "!");
        final String ptcols = urlStr.substring(0, urlStr.indexOf("/"));
        String path = urlStr.substring(urlStr.indexOf("/"));
        if (path.contains(".cache")) {
            path = path.substring(path.lastIndexOf(".cache") + 6);
        }

        return ptcols + "..." + path;
    }

    private String resolvePersistenceXML(final URL puRootURL) {
        final String pxmlPath = "META-INF/persistence.xml";
        final String urlPtcol = puRootURL.getProtocol();

        if (urlPtcol.toLowerCase().contains("wsjpa")) {
            // WSJPA: zip-format InputStream
            try {
                try (ZipInputStream zis = new ZipInputStream(puRootURL.openStream())) {
                    ZipEntry ze = null;
                    while ((ze = zis.getNextEntry()) != null) {
                        if (ze.getName().equals(pxmlPath)) {
                            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            final byte[] buffer = new byte[4096];
                            int bytesRead = -1;
                            while ((bytesRead = zis.read(buffer)) >= 0) {
                                baos.write(buffer, 0, bytesRead);
                            }

                            return baos.toString();
                        }
                    }
                }
                return null;
            } catch (Exception e) {
                FFDCFilter.processException(e, JPAIntrospection.class.getName() + ".resolvePersistenceXML", "wsjpa");
            }
        } else if (urlPtcol.toLowerCase().contains("jar")) {
            // Jar file URL
            try {
                String urlStr = puRootURL.toString();
                URL pxmlURL = (urlStr.endsWith("/")) ? new URL(urlStr + pxmlPath) : new URL(urlStr + "/" + pxmlPath);
                return readData(pxmlURL);
            } catch (Exception e) {
                FFDCFilter.processException(e, JPAIntrospection.class.getName() + ".resolvePersistenceXML", "jar");
            }
        } else if (urlPtcol.toLowerCase().contains("file")) {
            // File URL (possibly exploded jar)
        }

        return null;
    }

    private String readData(URL url) {
        final StringBuilder sb = new StringBuilder();
        BufferedReader br = null;

        try {
            br = new BufferedReader(new InputStreamReader(url.openStream()));

            String in = null;
            while ((in = br.readLine()) != null) {
                sb.append(in);
            }
        } catch (IOException e) {
            FFDCFilter.processException(e, JPAIntrospection.class.getName() + ".readData", "readData");
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Throwable t) {
                }
            }
        }

        return sb.toString();
    }

    public class JPAApplInfoIntrospect {
        private final String appname;
        private final JPAApplInfo appl;
        private final ArrayList<JPAScopeInfoIntrospect> scopeInfoList = new ArrayList<JPAScopeInfoIntrospect>();
        private int puCount = 0;
        private PersistenceUnitScannerResults results;

        public JPAApplInfoIntrospect(String appname, JPAApplInfo appl) {
            this.appname = appname;
            this.appl = appl;
        }

        public JPAApplInfo getJPAApplInfo() {
            return appl;
        }

        public List<JPAScopeInfoIntrospect> getScopeInfoList() {
            return Collections.unmodifiableList(scopeInfoList);
        }

        public int getPersistenceUnitCount() {
            return puCount;
        }
    }

    public class JPAScopeInfoIntrospect {
        private final JPAScopeInfo scopeInfo;
        private final ArrayList<JPAPxmlInfoIntrospect> pxmlInfoList = new ArrayList<JPAPxmlInfoIntrospect>();

        public JPAScopeInfoIntrospect(JPAScopeInfo scopeInfo) {
            this.scopeInfo = scopeInfo;
        }

        public JPAScopeInfo getJPAScopeInfo() {
            return scopeInfo;
        }

        public List<JPAPxmlInfoIntrospect> getPxmlInfoList() {
            return Collections.unmodifiableList(pxmlInfoList);
        }
    }

    public class JPAPxmlInfoIntrospect {
        private final JPAPxmlInfo pxmlInfo;
        private final ArrayList<JPAPUnitInfoIntrospect> jpaPuInfoList = new ArrayList<JPAPUnitInfoIntrospect>();
        private int puCount = 0;

        public JPAPxmlInfoIntrospect(JPAPxmlInfo pxmlInfo) {
            this.pxmlInfo = pxmlInfo;
        }

        public JPAPxmlInfo getJPAPxmlInfo() {
            return pxmlInfo;
        }

        public List<JPAPUnitInfoIntrospect> getJPAPUnitInfoList() {
            return Collections.unmodifiableList(jpaPuInfoList);
        }

        public int getPersistenceUnitCount() {
            return puCount;
        }
    }

    public class JPAPUnitInfoIntrospect {
        private final String puName;
        private final JPAPUnitInfo jpaPuInfo;

        public JPAPUnitInfoIntrospect(String puName, JPAPUnitInfo jpaPuInfo) {
            this.puName = puName;
            this.jpaPuInfo = jpaPuInfo;
        }

        public String getPuName() {
            return puName;
        }

        public JPAPUnitInfo getJpaPuInfo() {
            return jpaPuInfo;
        }
    }

    public class JPAAnalysisResult {
        private final String appName;
        private final JPAApplInfoIntrospect appl;
        private final ByteArrayOutputStream baos;
        private final PrintWriter pw;

        private final HashMap<URL, List<JPAPUnitInfoIntrospect>> puRootURL_JPAPuInfoMap = new HashMap<URL, List<JPAPUnitInfoIntrospect>>();

        public JPAAnalysisResult(String appName, JPAApplInfoIntrospect appl) {
            this.appName = appName;
            this.appl = appl;

            baos = new ByteArrayOutputStream();
            pw = new PrintWriter(baos);
        }

        public String getAppName() {
            return appName;
        }

        public JPAApplInfoIntrospect getAppl() {
            return appl;
        }

        public ByteArrayOutputStream getBaos() {
            return baos;
        }

        public PrintWriter getPw() {
            return pw;
        }

        public Map<URL, List<JPAPUnitInfoIntrospect>> getURL_JPAPuInfoMap() {
            return puRootURL_JPAPuInfoMap;
        }

        public void registerJPAPUInfo(JPAPUnitInfoIntrospect puInfoIntrospect) {
            final URL puRootURL = puInfoIntrospect.getJpaPuInfo().getPersistenceUnitRootUrl();
            List<JPAPUnitInfoIntrospect> introList = puRootURL_JPAPuInfoMap.get(puRootURL);
            if (introList == null) {
                introList = new ArrayList<JPAPUnitInfoIntrospect>();
                puRootURL_JPAPuInfoMap.put(puRootURL, introList);
            }
            introList.add(puInfoIntrospect);
        }

        public long getLineCount() {
            final byte newline = '\n';
            long count = 0;
            for (byte b : baos.toByteArray()) {
                if (newline == b) {
                    count++;
                }
            }

            return count;
        }
    }

    /*
     * Methods involved with dumping the current state of the JPA EntityManagerFactory instances associated with the JPAPUnitInfo objects.
     */

    private final static String[] openJPAEntityManagerFactoryImplClasses = { "org.apache.openjpa.persistence.EntityManagerFactoryImpl",
                                                                             "com.ibm.ws.persistence.EntityManagerFactoryImpl" };
    private final static String[] eclipselinkEntityManagerFactoryImplClasses = { "org.eclipse.persistence.internal.jpa.EntityManagerFactoryImpl" };

    private void doDumpJPARuntimeStates(final PrintWriter out) {
        out.println();
        out.println("################################################################################");
        out.println("################################################################################");
        out.println("################################################################################");
        out.println();
        out.println("Dumping JPA Runtime States:");
        out.println();

        for (JPAPUnitInfo puinfo : allJPAPUnitInfoList) {
            doDumpJPARuntimeState(puinfo, out);
        }
    }

    private void doDumpJPARuntimeState(final JPAPUnitInfo jpaPuInfo, final PrintWriter out) {
        out.println();
        out.println("JPA Runtime State Analysis for JPAPUnitInfo: ");
        out.println(jpaPuInfo.dump());
        out.println();

        // First, get the EntityManagerFactory associated with the JPAPUnitInfo, or the multitudes of them if this is a persistence
        // unit that has a java:comp/env JNDI name in the jta-data-source and/or non-jta-data-source
        Object ivEMFactory = null; // get EntityManagerFactory ivEMFactory
        try {
            ivEMFactory = reflectObjValue(jpaPuInfo, "ivEMFactory");
        } catch (Throwable t) {
            out.println("*** Unable to access ivEMFactory due to Exception ***");
        }

        Map ivEMFMap = null; // EntityManager for every java:comp, when Datasource is in java:comp. Map<J2EEName, EntityManagerFactory>
        try {
            ivEMFMap = (Map) reflectObjValue(jpaPuInfo, "ivEMFMap");
        } catch (Throwable t) {
            out.println("*** Unable to access ivEMFactory due to Exception ***");
        }

        if (ivEMFactory == null && (ivEMFMap == null || ivEMFMap.isEmpty())) {
            // No EntityManagerFactories to work with, so there is very little to work with.
            out.println("No EntityManagerFactory available for introspection with persistence unit " + jpaPuInfo.getPersistenceUnitName());
            return;
        }

        String emfClassName = null;
        if (ivEMFactory != null) {
            emfClassName = ivEMFactory.getClass().getName();
        } else if (ivEMFMap != null && ivEMFMap.size() > 0) {
            emfClassName = ivEMFMap.values().toArray()[0].getClass().getName();
        } else {
            out.println("Encountered a problem acquiring an EntityManagerFactory from JPAPUnitInfo");
            return;
        }

        if (ivEMFactory != null && (ivEMFMap == null || ivEMFMap.size() == 0)) {
            out.println("EntityManagerFactory state for ivEMFactory:");
            if (eclipselinkEntityManagerFactoryImplClasses[0].equals(emfClassName)) {
                // Is an Eclipselink EntityManagerFactory
                doDumpEclipselinkEntityManagerFactoryState(ivEMFactory, out);
            } else if (openJPAEntityManagerFactoryImplClasses[0].equals(emfClassName) || openJPAEntityManagerFactoryImplClasses[1].equals(emfClassName)) {
                // Is an OpenJPA EntityManagerFactory
                doDumpOpenJPAEntityManagerFactoryState(ivEMFactory, out);
            } else {
                out.println("Introspector doesn't support EntityManagerFactory impl type " + emfClassName);
            }
        }

        if (ivEMFMap != null && ivEMFMap.size() > 0) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) ivEMFMap).entrySet()) {
                out.println();
                out.println("EntityManagerFactory state for java:comp/env EMF: " + entry.getKey());
                if (eclipselinkEntityManagerFactoryImplClasses[0].equals(emfClassName)) {
                    // Is an Eclipselink EntityManagerFactory
                    doDumpEclipselinkEntityManagerFactoryState(entry.getValue(), out);
                } else if (openJPAEntityManagerFactoryImplClasses[0].equals(emfClassName) || openJPAEntityManagerFactoryImplClasses[1].equals(emfClassName)) {
                    // Is an OpenJPA EntityManagerFactory
                    doDumpOpenJPAEntityManagerFactoryState(entry.getValue(), out);
                } else {
                    out.println("Introspector doesn't support EntityManagerFactory impl type " + emfClassName);
                }
            }
        }

    }

    /*
     * OpenJPA Introspection Support
     */

    private void doDumpOpenJPAEntityManagerFactoryState(final Object emf, final PrintWriter out) {
        out.println("OpenJPA EntityManagerFactory: " + getInstanceClassAndAddress(emf));

        try {
            Object brokerFactory = reflectObjValue(emf, "_factory");

            out.println("   _factory = " + brokerFactory); // DelegatingBrokerFactory _factory
            out.println("   _metaModel = " + reflectObjValue(emf, "_metaModel")); // MetamodelImpl _metaModel

            // Extract the actual BrokerFactory from the DelegatingBrokerFactory
            while (brokerFactory != null && isCastable("org.apache.openjpa.kernel.DelegatingBrokerFactory", brokerFactory.getClass())) {
                Object broker = reflectObjValue(brokerFactory, "_factory");
                if (broker == null) {
                    brokerFactory = reflectObjValue(brokerFactory, "_del"); // Inner Delegate: DelegatingBrokerFactory _del
                } else {
                    brokerFactory = broker;
                    break;
                }
            }
            out.println("   Actual Broker Factory = " + brokerFactory);

            // Dump info about the Broker Factory, if we were able to get it
            if (brokerFactory != null && isCastable("org.apache.openjpa.kernel.AbstractBrokerFactory", brokerFactory.getClass())) {
                out.println();
                out.println("   Unwrapped _factory = " + brokerFactory);
                out.println("   AbstractBrokerFactory values:");
                out.println("      _closed = " + reflectObjValue(emf, "_closed")); // boolean
                out.println("      _readOnly = " + reflectObjValue(emf, "_readOnly")); // boolean

                final Object _conf = reflectObjValue(brokerFactory, "_conf");
                printOpenJPAConfiguration(_conf, out, "      _conf: ");

                if (_conf != null) {
                    out.println();
                    final Object mdr = reflectObjValue(_conf, "metaRepository");
                    if (mdr != null) {
                        printOpenJPAMetadataRepository(mdr, out, "      mdr: ");
                    }
                }

                // Dump Broker Information -- these are persistence contexts/EntityManager instances
                Set brokers = new HashSet();
                Set _brokers = (Set) reflectObjValue(brokerFactory, "_brokers"); // weak-ref tracking of open brokers
                if (_brokers != null) {
                    brokers.addAll(_brokers); // Copy the _brokers Set into our own Set for work
                }
                if (brokers.size() > 0) {
                    out.println();
                    out.println("   Active OpenJPA Brokers (Persistence Contexts): ");
                    for (Object broker : brokers) {
                        printOpenBrokerImpl(broker, out, "      ");
                    }
                }

            }

            if (brokerFactory != null && isCastable("org.apache.openjpa.jdbc.kernel.JDBCBrokerFactory", brokerFactory.getClass())) {
                out.println("   JDBCBrokerFactory values:");
                out.println("      _synchronizedMappings = " + reflectObjValue(emf, "_synchronizedMappings")); // boolean
            }

        } catch (Throwable t) {
            FFDCFilter.processException(t, JPAIntrospection.class.getName() + ".doDumpOpenJPAEntityManagerFactoryState", "689");
        }

    }

    private void printOpenJPAConfiguration(final Object config, final PrintWriter out, final String indent) {
        out.println(indent + "OpenJPA Configuration = " + config);
        if (config == null || indent == null) {
            return;
        }

        try {
            if (config != null) {
                out.println(indent + " ._appCL = " + reflectObjValue(config, "_appCL")); // ClassLoader
                // Dump all config options exposed by getter methods
                final List<Method> getMethods = getMethodsWithPrefix(config.getClass(), "get");
                int nullValCount = 0;
                for (Method m : getMethods) {
                    try {
                        final Object val = reflectMethodCall(config, m);
                        if (val == null) {
                            nullValCount++;
                        } else {
                            out.print(indent + " ." + m.getName().substring(3) + " = ");
                            out.println(poa(val, indent + "      ", true));
                        }
                    } catch (Throwable t) {
                        out.println(indent + " . problem calling " + m.getName());
                    }
                }
                out.println(indent + " # of get methods = " + getMethods.size());
                out.println(indent + " # of null properties = " + nullValCount); // 500+ average null values which we don't need to see
            }
        } catch (Throwable t) {

        }
    }

    private void printOpenJPAMetadataRepository(final Object mdr, final PrintWriter out, final String indent) throws Exception {
        out.println(indent + "OpenJPA MetaDataRepository = " + mdr);
        if (mdr == null || indent == null) {
            return;
        }

        final boolean isMDR = isCastable("org.apache.openjpa.meta.MetaDataRepository", mdr.getClass());
        final boolean isMR = isCastable("org.apache.openjpa.jdbc.meta.MappingRepository", mdr.getClass());
        if (isMDR || isMR) {
            out.println(indent + " ._factory = " + reflectObjValue(mdr, "_factory")); // MetaDataFactory
            out.println(indent + " ._filterRegisteredClasses = " + reflectObjValue(mdr, "_filterRegisteredClasses")); // boolean
            out.println(indent + " ._locking = " + reflectObjValue(mdr, "_locking")); // boolean
            out.println(indent + " ._preload = " + reflectObjValue(mdr, "_preload")); // boolean
            out.println(indent + " ._preloadComplete = " + reflectObjValue(mdr, "_preloadComplete")); // boolean

            out.println();
            final Map<?, ?> _metas = (Map) reflectObjValue(mdr, "_metas"); // Map<Class<?>, ClassMetaData>
            out.println(indent + " _metas = " + getInstanceClassAndAddress(_metas) + " (" + _metas.size() + " items)");
            final HashMap<?, ?> metasCopy = new HashMap(_metas);
            for (Map.Entry<?, ?> entry : metasCopy.entrySet()) {
                out.println(indent + "   " + entry.getKey() + " = " + poa(entry.getValue(), indent + "      ", true));
            }

            out.println();
            final Map<?, ?> _metaStringMap = (Map) reflectObjValue(mdr, "_metaStringMap"); // Map<String, ClassMetaData>
            out.println(indent + " _metaStringMap = " + getInstanceClassAndAddress(_metaStringMap) + " (" + _metaStringMap.size() + " items)");
            final HashMap<?, ?> metasStringMapCopy = new HashMap(_metaStringMap);
            for (Map.Entry<?, ?> entry : metasStringMapCopy.entrySet()) {
                out.println(indent + "   " + entry.getKey() + " = " + poa(entry.getValue(), indent + "      ", true));
            }

            out.println();
            final Map<?, ?> _impls = (Map) reflectObjValue(mdr, "_impls"); // Map<Class<?>, Collection<Class<?>>>
            out.println(indent + " _impls = " + getInstanceClassAndAddress(_impls) + " (" + _impls.size() + " items)");
            final HashMap<?, ?> implsCopy = new HashMap(_impls);
            for (Map.Entry<?, ?> entry : implsCopy.entrySet()) {
                out.println(indent + "   " + entry.getKey() + " = " + poa(entry.getValue(), indent + "      ", true));
            }

            out.println();
            final Map<?, ?> _ifaces = (Map) reflectObjValue(mdr, "_ifaces"); // Map<Class<?>, Class<?>>
            out.println(indent + " _ifaces = " + getInstanceClassAndAddress(_ifaces) + " (" + _ifaces.size() + " items)");
            final HashMap<?, ?> ifacesCopy = new HashMap(_ifaces);
            for (Map.Entry<?, ?> entry : ifacesCopy.entrySet()) {
                out.println(indent + "   " + entry.getKey() + " = " + poa(entry.getValue(), indent + "      ", true));
            }

            out.println();
            final Map<?, ?> _queries = (Map) reflectObjValue(mdr, "_queries"); // Map<String, QueryMetaData>
            out.println(indent + " _queries = " + getInstanceClassAndAddress(_queries) + " (" + _queries.size() + " items)");
            final HashMap<?, ?> queriesCopy = new HashMap(_queries);
            for (Map.Entry<?, ?> entry : queriesCopy.entrySet()) {
                out.println(indent + "   " + entry.getKey() + " = " + poa(entry.getValue(), indent + "      ", true));
            }

            out.println();
            final Map<?, ?> _seqs = (Map) reflectObjValue(mdr, "_seqs"); // Map<String, QueryMetaData>
            out.println(indent + " _seqs = " + getInstanceClassAndAddress(_seqs) + " (" + _seqs.size() + " items)");
            final HashMap<?, ?> seqsCopy = new HashMap(_seqs);
            for (Map.Entry<?, ?> entry : seqsCopy.entrySet()) {
                out.println(indent + "   " + entry.getKey() + " = " + poa(entry.getValue(), indent + "      ", true));
            }

            out.println();
            final Map<?, ?> _aliases = (Map) reflectObjValue(mdr, "_aliases"); // Map<String, List<Class<?>>
            out.println(indent + " _aliases = " + getInstanceClassAndAddress(_aliases) + " (" + _aliases.size() + " items)");
            final HashMap<?, ?> aliasesCopy = new HashMap(_aliases);
            for (Map.Entry<?, ?> entry : aliasesCopy.entrySet()) {
                out.println(indent + "   " + entry.getKey() + " = " + poa(entry.getValue(), indent + "      ", true));
            }

            out.println();
            final Map<?, ?> _metamodel = (Map) reflectObjValue(mdr, "_metamodel"); // Map<Class<?>, Class<?>>
            out.println(indent + " _metamodel = " + getInstanceClassAndAddress(_metamodel) + " (" + _metamodel.size() + " items)");
            final HashMap<?, ?> metamodelCopy = new HashMap(_metamodel);
            for (Map.Entry<?, ?> entry : metamodelCopy.entrySet()) {
                out.println(indent + "   " + entry.getKey() + " = " + poa(entry.getValue(), indent + "      ", true));
            }

            out.println();
            final Map<?, ?> _subs = (Map) reflectObjValue(mdr, "_subs"); // Map<Class<?>, List<Class<?>>>
            out.println(indent + " _subs = " + getInstanceClassAndAddress(_subs) + " (" + _subs.size() + " items)");
            final HashMap<?, ?> _subsCopy = new HashMap(_subs);
            for (Map.Entry<?, ?> entry : _subsCopy.entrySet()) {
                out.println(indent + "   " + entry.getKey() + " = " + poa(entry.getValue(), indent + "      ", true));
            }
        }
        if (isMR) {
            // MappingRepository extends MetaDataRepository
            out.println();
            out.println(indent + " MappingRepository Specific Values");
            out.println(indent + "  ._dict = " + reflectObjValue(mdr, "_dict")); // DBDictionary
            out.println(indent + "  ._defaults = " + reflectObjValue(mdr, "_defaults")); // MappingDefaults
            out.println(indent + "  ._results = " + poa(reflectObjValue(mdr, "_results"), indent + "     ", true)); // Map<Object, QueryResultMapping>
            out.println(indent + "  ._schema = " + reflectObjValue(mdr, "_schema")); // SchemaGroup
            out.println(indent + "  ._installer = " + reflectObjValue(mdr, "_installer")); // StrategyInstaller
        }
    }

    private void printOpenBrokerImpl(final Object brokerImpl, final PrintWriter out, final String indent) throws Exception {
        out.println(indent + "OpenJPA BrokerImpl = " + brokerImpl);
        if (brokerImpl == null || indent == null) {
            return;
        }

        try {
            out.println(indent + " ._compat = " + reflectObjValue(brokerImpl, "_compat")); // Compatibility
            out.println(indent + " ._runtime = " + reflectObjValue(brokerImpl, "_runtime")); // ManagedRuntime
            out.println(indent + " ._lm = " + reflectObjValue(brokerImpl, "_lm")); // LockManager
            out.println(indent + " ._im = " + reflectObjValue(brokerImpl, "_im")); // InverseManager
            out.println(indent + " ._call = " + reflectObjValue(brokerImpl, "_call")); // OpCallbacks
            out.println(indent + " ._instm = " + reflectObjValue(brokerImpl, "_instm")); // InstrumentationManager
            out.println(indent + " ._loader = " + reflectObjValue(brokerImpl, "_loader")); // ClassLoader

            out.println(indent + " ._nontransRead = " + reflectObjValue(brokerImpl, "_nontransRead"));
            out.println(indent + " ._nontransWrite = " + reflectObjValue(brokerImpl, "_nontransWrite"));
            out.println(indent + " ._retainState = " + reflectObjValue(brokerImpl, "_retainState"));
            out.println(indent + " ._autoClear = " + reflectObjValue(brokerImpl, "_autoClear"));
            out.println(indent + " ._restoreState = " + reflectObjValue(brokerImpl, "_restoreState"));
            out.println(indent + " ._optimistic = " + reflectObjValue(brokerImpl, "_optimistic"));
            out.println(indent + " ._ignoreChanges = " + reflectObjValue(brokerImpl, "_ignoreChanges"));
            out.println(indent + " ._multithreaded = " + reflectObjValue(brokerImpl, "_multithreaded"));
            out.println(indent + " ._managed = " + reflectObjValue(brokerImpl, "_managed"));
            out.println(indent + " ._syncManaged = " + reflectObjValue(brokerImpl, "_syncManaged"));
            out.println(indent + " ._connRetainMode = " + reflectObjValue(brokerImpl, "_connRetainMode"));
            out.println(indent + " ._evictDataCache = " + reflectObjValue(brokerImpl, "_evictDataCache"));
            out.println(indent + " ._populateDataCache = " + reflectObjValue(brokerImpl, "_connRetainMode"));
            out.println(indent + " ._connRetainMode = " + reflectObjValue(brokerImpl, "_populateDataCache"));
            out.println(indent + " ._largeTransaction = " + reflectObjValue(brokerImpl, "_largeTransaction"));
            out.println(indent + " ._autoDetach = " + reflectObjValue(brokerImpl, "_autoDetach"));
            out.println(indent + " ._detachState = " + reflectObjValue(brokerImpl, "_detachState"));
            out.println(indent + " ._detachedNew = " + reflectObjValue(brokerImpl, "_detachedNew"));
            out.println(indent + " ._orderDirty = " + reflectObjValue(brokerImpl, "_orderDirty"));
            out.println(indent + " ._cachePreparedQuery = " + reflectObjValue(brokerImpl, "_cachePreparedQuery"));
            out.println(indent + " ._cacheFinderQuery = " + reflectObjValue(brokerImpl, "_cacheFinderQuery"));
            out.println(indent + " ._suppressBatchOLELogging = " + reflectObjValue(brokerImpl, "_suppressBatchOLELogging"));
            out.println(indent + " ._allowReferenceToSiblingContext = " + reflectObjValue(brokerImpl, "_allowReferenceToSiblingContext"));
            out.println(indent + " ._postLoadOnMerge = " + reflectObjValue(brokerImpl, "_postLoadOnMerge"));
            out.println(indent + " ._flags = " + reflectObjValue(brokerImpl, "_flags"));
            out.println(indent + " ._isSerializing = " + reflectObjValue(brokerImpl, "_isSerializing"));
            out.println(indent + " ._closed = " + reflectObjValue(brokerImpl, "_closed"));
            out.println(indent + " ._transEventManager = " + reflectObjValue(brokerImpl, "_transEventManager"));
            out.println(indent + " ._transCallbackMode = " + reflectObjValue(brokerImpl, "_transCallbackMode"));
            out.println(indent + " ._lifeEventManager = " + reflectObjValue(brokerImpl, "_lifeEventManager"));
            out.println(indent + " ._lifeCallbackMode = " + reflectObjValue(brokerImpl, "_lifeCallbackMode"));
            out.println(indent + " ._dmLite = " + reflectObjValue(brokerImpl, "_dmLite"));
            out.println(indent + " ._initializeWasInvoked = " + reflectObjValue(brokerImpl, "_initializeWasInvoked"));
            out.println(indent + " ._fromWriteBehindCallback = " + reflectObjValue(brokerImpl, "_fromWriteBehindCallback"));
            out.println(indent + " ._fcs = " + reflectObjValue(brokerImpl, "_fcs"));
            out.println(indent + " ._printParameters = " + reflectObjValue(brokerImpl, "_printParameters"));

            final Map _userObjects = (Map) reflectObjValue(brokerImpl, "_userObjects"); // Map<Object, Object>
            out.println(indent + " ._userObjects size = " + ((_userObjects != null) ? _userObjects.size() : 0));

            final Map _cache = (Map) reflectObjValue(brokerImpl, "_cache"); // ManagedCache
            out.println(indent + " ._cache = " + _cache);

        } catch (Throwable t) {

        }
    }

    /*
     * OpenJPA Diagnostic Enhancements
     */
    private void doDumpOpenJPAPCRegistry(final PrintWriter out) {
        Class pcRegClass = null;
        for (JPAPUnitInfo puinfo : allJPAPUnitInfoList) {
            final ClassLoader c = puinfo.getNewTempClassLoader();
            try {
                pcRegClass = c.loadClass("org.apache.openjpa.enhance.PCRegistry");
                break;
            } catch (ClassNotFoundException cnfe) {
                // Next!
            }
        }

        if (pcRegClass == null) {
            // None of the temp ClassLoaders used by the persistence units were able to find PCRegistry.
            // For now, this is expected to happen with Liberty JPA features > 2.0.
            return;
        }

        out.println();
        out.println("################################################################################");
        out.println("OpenJPA PCRegistry Dump");
        out.println("################################################################################");

        // Access the PCRegistry._metas Map<Class<?>,Meta> static field
        try {
            final Field _metas = pcRegClass.getDeclaredField("_metas");
            final boolean accessible = _metas.isAccessible();
            Object _metasObjVal = null; // Type is Map<Class<?>,Meta>
            try {
                _metas.setAccessible(true);
                _metasObjVal = _metas.get(null);
            } finally {
                _metas.setAccessible(accessible);
            }

            if (_metasObjVal == null) {
                return;
            }

            final Map<?, ?> _metasMap = (Map<?, ?>) _metasObjVal;
            for (Map.Entry<?, ?> entry : _metasMap.entrySet()) {
                final Class cls = (Class) entry.getKey();
                final Object metaObj = entry.getValue();

                out.println();
                out.println("Class " + cls.getName() + " " + getObjectAddress(cls));
                out.println("  ClassLoader: " + poa(cls.getClassLoader()));
                out.println("  CodeSource: " + poa(cls.getProtectionDomain().getCodeSource()));

                if (metaObj == null) {
                    continue;
                }

                try {
                    final Class metaObjClass = metaObj.getClass();
                    final String alias = (String) reflectObjValue(metaObj, "alias");
                    final Object pc = reflectObjValue(metaObj, "pc"); // PersistenceCapable type
                    final Class pcSuper = (Class) reflectObjValue(metaObj, "pcSuper");
                    final String pcTxt = getInstanceClassAndAddress(pc); // Calling a PC's toString() can be dangerous.  Don't do it.

                    out.println("  Meta alias: " + alias); // metaObjClass.getDeclaredField("alias").get(metaObj)); // String
                    out.println("  Meta pc: " + poa(pcTxt)); // metaObjClass.getDeclaredField("pc").get(metaObj)); // PersistenceCapable
                    out.println("  Meta pcSuper: " + poa(pcSuper)); // metaObjClass.getDeclaredField("pcSuper").get(metaObj));

                    out.print("  Meta fieldNames: ");
                    final Object[] fieldNames = (Object[]) reflectObjValue(metaObj, "fieldNames"); // metaObjClass.getDeclaredField("fieldNames").get(metaObj); // String[]
                    for (Object o : fieldNames) {
                        out.print(poa(o));
                        out.print(" ");
                    }
                    out.println();

                    out.print("  Meta fieldTypes: ");
                    final Object[] fieldTypes = (Object[]) reflectObjValue(metaObj, "fieldTypes"); // (Object[]) metaObjClass.getDeclaredField("fieldTypes").get(metaObj); // Class<?>[]
                    for (Object o : fieldTypes) {
                        out.print(poa(o));
                        out.print(" ");
                    }
                    out.println();
                } catch (Throwable t) {
                    FFDCFilter.processException(t, JPAIntrospection.class.getName() + ".doDumpOpenJPAPCRegistry", "708");
                }
            }
        } catch (Throwable t) {
            FFDCFilter.processException(t, JPAIntrospection.class.getName() + ".doDumpOpenJPAPCRegistry", "713");
        }
    }

    /*
     * Introspection Support for Eclipselink EntityManagerFactory instances
     */

    private void doDumpEclipselinkEntityManagerFactoryState(final Object entityManagerFactory, final PrintWriter out) {
        out.println("Entered doDumpEclipselinkEntityManagerFactoryState");
    }

    /*
     * Utility methods used for introspecting JPA Persistence Provider Implementation Data Structures
     */
    private static Object reflectObjValue(Object o, String field) throws Exception {
        final Class<?> c = o.getClass();
        final Field f = findField(c, field); //        c.getField(field);
        if (f == null) {
            return null;
        }
        final boolean accessible = f.isAccessible();
        try {
            f.setAccessible(true);
            return f.get(o);
        } finally {
            f.setAccessible(accessible);
        }
    }

    private static Field findField(final Class<?> c, String field) {
        if (c == null) {
            return null;
        }

        try {
            return c.getDeclaredField(field);
        } catch (Exception e) {

        }

        if (Object.class.equals(c)) {
            return null;
        }

        return findField(c.getSuperclass(), field);
    }

    private static Object reflectMethodCall(Object o, Method m) throws Exception {
        if (m == null) {
            return null;
        }

        final boolean accessible = m.isAccessible();
        try {
            m.setAccessible(true);
            return m.invoke(o);
        } finally {
            m.setAccessible(accessible);
        }
    }

    private static Object reflectMethodCall(Object o, String method) throws Exception {
        final Class<?> c = o.getClass();
        final Method m = findMethod(c, method);

        return reflectMethodCall(o, m);
    }

    private static Method findMethod(final Class<?> c, String methodName) {
        if (c == null) {
            return null;
        }

        try {
            Method[] methods = c.getMethods();
            if (methods != null) {
                for (Method m : methods) {
                    if (!m.equals(methodName)) {
                        continue;
                    }

                    final Class[] pt = m.getParameterTypes();

                    if (pt != null && pt.length > 0) {
                        // Cannot support calling a method with any arguments
                        continue;
                    }

                    return m;
                }
            }
        } catch (Exception e) {

        }

        if (Object.class.equals(c)) {
            return null;
        }

        return findMethod(c.getSuperclass(), methodName);
    }

    private static List<Method> getMethodsWithPrefix(final Class<?> c, final String prefix) {
        ArrayList<Method> methodList = new ArrayList<Method>();

        if (c == null) {
            return methodList;
        }

        try {
            Method[] methods = c.getDeclaredMethods();
            if (methods != null) {
                for (Method m : methods) {
                    if (!m.getName().startsWith(prefix)) {
                        continue;
                    }

                    final Class[] pt = m.getParameterTypes();

                    if (pt != null && pt.length > 0) {
                        // Cannot support calling a method with any arguments
                        continue;
                    }

                    methodList.add(m);
                }
            }
        } catch (Exception e) {

        }

        if (!Object.class.equals(c)) {
            methodList.addAll(getMethodsWithPrefix(c.getSuperclass(), prefix));
        }

        return methodList;
    }

    private static boolean isCastable(String superclassClass, Class<?> c) {
        if (c == null || superclassClass == null || superclassClass.trim().isEmpty()) {
            return false;
        }

        if (superclassClass.equals(c.getName())) {
            return true;
        }

        if (Object.class != c) {
            return isCastable(superclassClass, c.getSuperclass());
        }

        return false;
    }

    private static String getObjectAddress(Object o) {
        if (o == null) {
            return "";
        }

        if (o.getClass().isPrimitive()) {
            return ""; // Primitives do not have object addresses
        }

        return "@" + Integer.toHexString(System.identityHashCode(o));
    }

    private static String poa(Object o, String indent, boolean dumpCollection) {
        return poa(o, indent, dumpCollection, 0);
    }

    private static String poa(Object o, String indent, boolean dumpCollection, int depth) {
        if (dumpCollection == false || o == null || depth > 5) {
            return poa(o);
        }

        final Class<?> oClass = o.getClass();
        final StringBuilder sb = new StringBuilder();

        if (oClass.isArray()) {
            sb.append(getInstanceClassAndAddress(o));
            final Object[] objarr = (Object[]) o;
            int idx = 0;
            for (Object obj : objarr) {
                sb.append("\n").append(indent).append(idx++).append(" : ");
                sb.append(poa(obj, indent + "   ", true, depth++));
            }
        } else if (Collection.class.isInstance(o)) {
            sb.append(getInstanceClassAndAddress(o));

            if (Map.class.isInstance(o)) {
                Map<?, ?> map = (Map<?, ?>) o;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    sb.append(":\n").append(indent).append(poa(entry.getKey())).append(" :");
                    sb.append(poa(entry.getValue(), indent + "   ", true, depth++));
                }
            } else {
                Collection<?> c = (Collection<?>) o;
                for (Object obj : c) {
                    sb.append(":\n").append(indent).append(poa(obj, indent + "   ", true, depth++));
                }
            }

        } else {
            return poa(o);
        }

        return sb.toString();
    }

    /**
     * Print Object toString() and Address (does not navigate into Collections or Arrays)
     */
    private static String poa(Object o) {
        if (o == null) {
            return "<<null>>";
        }

        return o.toString() + " " + getObjectAddress(o);
    }

    private static String getInstanceClassAndAddress(Object o) {
        if (o == null) {
            return "<<null>>";
        }

        final Class<?> oClass = o.getClass();
        if (oClass.isPrimitive()) {
            return oClass.getName();
        }
        return oClass.getName() + getObjectAddress(oClass);
    }
}
