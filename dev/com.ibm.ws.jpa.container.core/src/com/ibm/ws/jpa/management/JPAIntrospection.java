/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jpa.diagnostics.AbstractIntrospection;
import com.ibm.ws.jpa.diagnostics.JPAORMDiagnostics;
import com.ibm.ws.jpa.diagnostics.OpenJPAIntrospection;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ClassInfoType;
import com.ibm.ws.jpa.diagnostics.puscanner.PersistenceUnitScannerResults;

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

    public static final void registerArchiveSet(Set<String> archivesSet) {
        if (archivesSet == null || archivesSet.isEmpty()) {
            return;
        }

        final JPAIntrospection jpaIntrospector = getJPAIntrospection();
        if (jpaIntrospector != null) {
            try {
                jpaIntrospector.doRegisterArchiveSet(archivesSet);
            } catch (Throwable t) {
                FFDCFilter.processException(t, JPAIntrospection.class.getName() + ".registerArchiveSet", "92");
            }

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
    private final HashMap<JPAApplInfoIntrospect, Set<String>> applicationArchivesMap = new HashMap<JPAApplInfoIntrospect, Set<String>>();

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

    private void doRegisterArchiveSet(Set<String> archivesSet) {
        if (currentAppl != null) {
            Set<String> applicationArchives = applicationArchivesMap.get(currentAppl);
            if (applicationArchives == null) {
                applicationArchives = new TreeSet<String>(new Comparator<String>() {
                    @Override
                    public int compare(String s1, String s2) {
                        return s1.compareTo(s2);
                    }

                });
                applicationArchivesMap.put(currentAppl, applicationArchives);
            }

            applicationArchives.addAll(archivesSet);
        }
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
                if (pxml != null) {
                    puRootURL_pxml_Map.put(url, pxml);
                }
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
                    dout.println("      " + JPAIntrospection.getShortenedURLPath(e2.getKey()) + " (" + e2.getValue().size() + " persistence units)");
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

            dout.println("   Application Modules and Archives:");
            final Set<String> applicationArchives = applicationArchivesMap.get(result.getAppl());
            if (applicationArchives == null || applicationArchives.isEmpty()) {
                dout.println("      -- None were located.");
            } else {
                for (String archiveName : applicationArchives) {
                    dout.println("     " + archiveName);
                }
            }
            dout.println();

            dout.println(result.getBaos().toString());
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

    private String resolvePersistenceXML(final URL puRootURL) {
        final String pxmlPath = "META-INF/persistence.xml";
        final String urlPtcol = puRootURL.getProtocol().toLowerCase();

        Tr.debug(tc, JPAIntrospection.class.getName() + ".resolvePersistenceXML: parsing URL", puRootURL);

        if ("wsjpa".equals(urlPtcol)) {
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
        } else if ("jar".equals(urlPtcol)) {
            // Jar file URL
            try {
                String urlStr = puRootURL.toString();
                URL pxmlURL = (urlStr.endsWith("/")) ? new URL(urlStr + pxmlPath) : new URL(urlStr + "/" + pxmlPath);
                return readData(pxmlURL);
            } catch (Exception e) {
                FFDCFilter.processException(e, JPAIntrospection.class.getName() + ".resolvePersistenceXML", "jar");
            }
        } else if ("file".equals(urlPtcol)) {
            // File URL (possibly exploded jar)
            try {
                String urlStr = puRootURL.toString();
                URL pxmlURL = (urlStr.endsWith("/")) ? new URL(urlStr + pxmlPath) : new URL(urlStr + "/" + pxmlPath);
                return readData(pxmlURL);
            } catch (Exception e) {
                FFDCFilter.processException(e, JPAIntrospection.class.getName() + ".resolvePersistenceXML", "file");
            }
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

        Class emfClass = null;
        if (ivEMFactory != null) {
            emfClass = ivEMFactory.getClass();
        } else if (ivEMFMap != null && ivEMFMap.size() > 0) {
            emfClass = ivEMFMap.values().toArray()[0].getClass();
        } else {
            out.println("Encountered a problem acquiring an EntityManagerFactory from JPAPUnitInfo");
            return;
        }

        if (!AbstractIntrospection.isSupportedPlatform(emfClass)) {
            out.println("Introspector doesn't support EntityManagerFactory impl type " + emfClass.getName());
            return;
        }

        if (ivEMFactory != null && (ivEMFMap == null || ivEMFMap.size() == 0)) {
            out.println("EntityManagerFactory state for ivEMFactory:");
            AbstractIntrospection.getPlatformIntrospection(emfClass).dumpJPAEntityManagerFactoryState(ivEMFactory, out);
        }

        if (ivEMFMap != null && ivEMFMap.size() > 0) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) ivEMFMap).entrySet()) {
                out.println();
                out.println("EntityManagerFactory state for java:comp/env EMF: " + entry.getKey());
                AbstractIntrospection.getPlatformIntrospection(emfClass).dumpJPAEntityManagerFactoryState(entry.getValue(), out);
            }
        }
    }

    /*
     * OpenJPA Diagnostic Enhancements
     */
    private void doDumpOpenJPAPCRegistry(final PrintWriter out) {
        ArrayList<ClassLoader> tclList = new ArrayList<ClassLoader>();
        for (JPAPUnitInfo puinfo : allJPAPUnitInfoList) {
            tclList.add(puinfo.getNewTempClassLoader());
        }

        OpenJPAIntrospection.dumpOpenJPAPCRegistry(tclList, out);
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

}
