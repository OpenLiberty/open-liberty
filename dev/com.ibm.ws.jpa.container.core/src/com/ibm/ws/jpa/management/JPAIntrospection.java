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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jpa.diagnostics.JPAORMDiagnostics;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ClassInfoType;
import com.ibm.ws.jpa.diagnostics.puscanner.PersistenceUnitScannerResults2;

/**
 *
 */
public class JPAIntrospection {
    private static final ThreadLocal<JPAIntrospection> threadLocal = new ThreadLocal<JPAIntrospection>() {
        @Override
        protected JPAIntrospection initialValue() {
            return new JPAIntrospection();
        }
    };

    public static final JPAIntrospection getJPAIntrospection() {
        return threadLocal.get();
    }

    public static final void endJPAIntrospection() {
        threadLocal.remove();
    }

    private final HashMap<String, JPAApplInfoIntrospect> jpaApplInfoMap = new HashMap<String, JPAApplInfoIntrospect>();

    private JPAApplInfoIntrospect currentAppl = null;
    private JPAScopeInfoIntrospect currentScopeInfo = null;
    private JPAPxmlInfoIntrospect currentPxmlInfo = null;

    public final Map<String, JPAApplInfoIntrospect> getJPAApplInfoIntrospectMap() {
        return Collections.unmodifiableMap(jpaApplInfoMap);
    }

    public void beginApplicationVisit(String appname, JPAApplInfo appl) {
        currentAppl = new JPAApplInfoIntrospect(appname, appl);
        jpaApplInfoMap.put(appname, currentAppl);
    }

    public void endApplicationVisit() {
        currentAppl = null;
    }

    public void beginPUScopeVisit(JPAScopeInfo scopeInfo) {
        if (currentAppl == null) {
            return; // Bad State
        }
        currentScopeInfo = new JPAScopeInfoIntrospect(scopeInfo);
        currentAppl.scopeInfoList.add(currentScopeInfo);
    }

    public void endPUScopeVisit() {
        currentScopeInfo = null;
    }

    public void beginPXmlInfoVisit(JPAPxmlInfo pxmlInfo) {
        if (currentAppl == null || currentScopeInfo == null) {
            return; // Bad State
        }
        currentPxmlInfo = new JPAPxmlInfoIntrospect(pxmlInfo);
        currentScopeInfo.pxmlInfoList.add(currentPxmlInfo);
    }

    public void endPXmlInfoVisit() {
        currentPxmlInfo = null;
    }

    public void visitJPAPUnitInfo(String puName, JPAPUnitInfo jpaPuInfo) {
        if (currentAppl == null || currentScopeInfo == null || currentPxmlInfo == null) {
            return; // Bad State
        }

        currentPxmlInfo.jpaPuInfoList.add(new JPAPUnitInfoIntrospect(puName, jpaPuInfo));
        currentAppl.puCount++;
        currentPxmlInfo.puCount++;
    }

    public void executeIntrospectionAnalysis(final PrintWriter dout) {
        final Map<String, JPAAnalysisResult> analysisResultMap = new HashMap<String, JPAAnalysisResult>();

        try {
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

                jpaAppl.results = JPAORMDiagnostics.performJPAORMDiagnostics(
                                                                             new ArrayList<javax.persistence.spi.PersistenceUnitInfo>(puInfoList),
                                                                             puRootURL_pxml_Map,
                                                                             out);
            }
        } finally {
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
                final PersistenceUnitScannerResults2 scannerResults = result.getAppl().results;

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

    // out.println("************************************************************");
    // out.println(jpaPuInfo.dump());

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
        private PersistenceUnitScannerResults2 results;

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
}
