/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.management;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Each EJB jar, Web war and application client jar has it own PU scope, i.e. persistence unit name
 * is unique in its own scope. All persistence archive jars in the EAR's root and "lib" directory
 * are in a single global EAR scope. Persistence unit name in the global must be unique and is
 * visible from the EJB, Web and client jars.
 */
public class JPAScopeInfo {
    private static final TraceComponent tc = Tr.register(JPAScopeInfo.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    // Scope name: Module jar name if scoped to EJB or WEB or EAR_SCOPE_MODULE_NAME if defined in
    // the applicatoin lib
    private final String scopeName;

    // JPA Scope, EJB, Web or Ear
    private final JPAPuScope scope;

    // Collection or persistence.xml information at this scope.
    private final Map<String, JPAPxmlInfo> pxmlsInfo;

    /**
     * Constructor.
     *
     * @param scopeName
     * @param scope
     * @param modClassLoader
     */
    JPAScopeInfo(String scopeName, JPAPuScope scope) {
        this.scopeName = scopeName;
        this.scope = scope;
        this.pxmlsInfo = new HashMap<String, JPAPxmlInfo>();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, toString());
    }

    /**
     * Process each persistence.xml found in an application. Discover all the persistence unit(s)
     * defined in the module.
     *
     * @param pxml
     * @param looseConfig
     */
    void processPersistenceUnit(JPAPXml pxml, JPAApplInfo applInfo) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processPersistenceUnit" + pxml);

        synchronized (pxmlsInfo) {
            URL pxmlRootURL = pxml.getRootURL();
            String pxmlInfoKey = scope == JPAPuScope.EAR_Scope ? pxml.getArchiveName() : pxmlRootURL.toString();
            JPAPxmlInfo pxmlInfo = pxmlsInfo.get(pxmlInfoKey);

            // if root URL is defined for this persistence.xml, it has been processed before,
            // i.e. deployed web module processing will find persistence.xml in the ejb module
            // since the ejb module is in the web module classpath.
            if (pxmlInfo == null) {
                pxmlInfo = new JPAPxmlInfo(this, pxmlRootURL); // F743-23167
                pxmlsInfo.put(pxmlInfoKey, pxmlInfo);

                JPAIntrospection.beginPXmlInfoVisit(pxmlInfo);
                try {
                    // read in all the persistence unit define in this persistence.xml
                    pxmlInfo.extractPersistenceUnits(pxml); //PK62950
                } finally {
                    JPAIntrospection.endPXmlInfoVisit();
                }
            }

        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processPersistenceUnit", getPUList());
    }

    /**
     * Returns the name of this scope.
     */
    String getScopeName() {
        return scopeName;
    }

    /**
    *
    */
    private String getPUList() {
        synchronized (pxmlsInfo) {
            StringBuilder sbuf = new StringBuilder();
            for (JPAPxmlInfo pxmlInfo : pxmlsInfo.values()) {
                pxmlInfo.toStringBuilder(sbuf);
            }
            return sbuf.toString();
        }
    }

    /**
    *
    */
    public JPAPuScope getScopeType() {
        return scope;
    }

    /**
     * Returns the PersistenceUnitInfo object represents the persistence unit (puName) or null
     * if not found. This method also processes relative form of persistence unit definition.
     */
    JPAPUnitInfo getPuInfo(String puName) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getPuInfo : " + puName);

        JPAPUnitInfo rtnVal = null;

        synchronized (pxmlsInfo) {
            // Check if puName uses the '#' syntax.
            int archiveEnd;
            if (scope == JPAPuScope.EAR_Scope && (archiveEnd = puName.indexOf('#')) != -1) {
                String archiveName = puName.substring(0, archiveEnd);
                puName = puName.substring(archiveEnd + 1);
                JPAPxmlInfo pxmlInfo = pxmlsInfo.get(archiveName);
                if (pxmlInfo != null) {
                    rtnVal = pxmlInfo.getPuInfo(puName);
                }
            } else {
                for (JPAPxmlInfo pxmlInfo : pxmlsInfo.values()) {
                    rtnVal = pxmlInfo.getPuInfo(puName);
                    if (rtnVal != null) {
                        break;
                    }
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getPuInfo : " + (rtnVal != null ? rtnVal.getPersistenceUnitName() : "PU not found."));
        return rtnVal;
    }

    /**
     * Returns the number of persistence units in persistence.xml defined in this scope.
     *
     * @return Persistence unit count
     */
    int getAllPuCount() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getAllPuCount", this);
        int rtnCount = 0;

        synchronized (pxmlsInfo) {
            for (JPAPxmlInfo pxmlInfo : pxmlsInfo.values()) {
                rtnCount += pxmlInfo.getPuCount();
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getAllPuCount : " + rtnCount);
        return rtnCount;
    }

    /**
     * Returns an unique PersistenceUnitInfo object if there is only persistence unit defines
     * in this scope, otherwise null is returned.
     */
    JPAPUnitInfo getUniquePuInfo() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getUniquePuInfo");

        JPAPUnitInfo rtnVal = null;

        synchronized (pxmlsInfo) {
            for (JPAPxmlInfo pxmlInfo : pxmlsInfo.values()) {
                Set<String> puNames = pxmlInfo.getPuNames();
                if (rtnVal == null && puNames.size() == 1) {
                    rtnVal = pxmlInfo.getPuInfo(puNames.iterator().next());
                } else {
                    // the current persistence.xml may have 0 persistence unit
                    // <xsd:element name="persistence-unit" minOccurs="0"
                    if (puNames.size() != 0) // d392996.3
                    {
                        rtnVal = null;
                        break;
                    }
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getUniquePuInfo", rtnVal);
        return rtnVal;
    }

    /**
     * Cloase all the active EntityManagers defined in this scope.
     */
    void close() {
        synchronized (pxmlsInfo) {
            for (JPAPxmlInfo pxmlInfo : pxmlsInfo.values()) {
                pxmlInfo.close();
            }
            pxmlsInfo.clear();
        }
    }

    StringBuilder toStringBuilder(StringBuilder sbuf) {
        synchronized (pxmlsInfo) {
            sbuf.append("\nScopeInfo: ScopeName = ").append(scopeName).append("\tScope = ").append(scope);
            for (JPAPxmlInfo pxmlInfo : pxmlsInfo.values()) {
                pxmlInfo.toStringBuilder(sbuf);
            }
        }

        return sbuf;
    }

    @Override
    public String toString() {
        synchronized (pxmlsInfo) {
            return "ScopeName = \"" + scopeName + "\"\tScope = " + scope +
                   "\t# persistence.xml = " + pxmlsInfo.size();
        }
    }

    void doIntrospect() {
        final Map<String, JPAPxmlInfo> pxmlsInfoCopy = new HashMap<String, JPAPxmlInfo>();
        synchronized (pxmlsInfo) {
            pxmlsInfoCopy.putAll(pxmlsInfo);
        }

        for (Map.Entry<String, JPAPxmlInfo> entry : pxmlsInfoCopy.entrySet()) {
            final JPAPxmlInfo value = entry.getValue();

            JPAIntrospection.beginPXmlInfoVisit(value);
            try {
                value.doIntrospect();
            } finally {
                JPAIntrospection.endPXmlInfoVisit();
            }
        }
    }
}
