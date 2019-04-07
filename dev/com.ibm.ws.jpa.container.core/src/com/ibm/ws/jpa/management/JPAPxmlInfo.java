/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
import static com.ibm.ws.jpa.management.JPAConstants.PERSISTENCE_XML_RESOURCE_NAME;

import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jpa.JPAPuId;

/**
 * This is a data container manages persistence unit information defined in a ejb-jar, war or jar.
 * Each of this object represents a persistenc.xml in an application.
 *
 * @see com.ibm.ws.jpa.management.JPAApplInfo
 */
class JPAPxmlInfo {
    private static final TraceComponent tc = Tr.register(JPAPxmlInfo.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    // Scope information associated to this persistence.xml.
    private final JPAScopeInfo ivScopeInfo;

    // Root URL of this persistence.xml.
    private final URL ivRootURL;

    // List of persistence units defined in this persistence.xml.
    private final Map<String, JPAPUnitInfo> ivPuList;

    /**
     * Constructor.
     *
     * @param scopeInfo
     * @param rootURL
     */
    JPAPxmlInfo(JPAScopeInfo scopeInfo, URL rootURL) {
        super();

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "<init>", scopeInfo, rootURL);

        ivScopeInfo = scopeInfo;
        ivRootURL = rootURL;
        ivPuList = new HashMap<String, JPAPUnitInfo>();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "<init>");
    }

    /**
     * Populates the list of persistence units defined in this persistence.xml.
     *
     * @param pxml
     * @param looseConfig
     */
    void extractPersistenceUnits(JPAPXml pxml) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "extractPersistenceUnits : " + pxml);

        // Determines the correct schema version, and uses the correct version
        // of JAXB generated classes to parse the persistence.xml file... and
        // the correct version of .xsd to validate against.            F1879-16302
        JaxbPersistence p = JaxbUnmarshaller.unmarshal(pxml);

        List<JaxbPUnit> pus = p.getPersistenceUnit();
        for (JaxbPUnit pu : pus) {
            // Guarantee to have a puName from <persistence-unit>
            String puName = pu.getName();
            // Set <persistence-unit>
            JPAApplInfo applInfo = pxml.getApplInfo();
            JPAPuId puId = new JPAPuId(applInfo.getApplName(), pxml.getArchiveName(), puName); // d689596
            JPAPUnitInfo puInfo = applInfo.createJPAPUnitInfo(puId, pxml, ivScopeInfo);

            // set XML Schema version
            puInfo.setPersistenceXMLSchemaVersion(p.getVersion());

            // Must set the root URL first for other puInfo attribute to reference
            // determine the root of the persistence unit.
            puInfo.setPersistenceUnitRootUrl(pxml.getRootURL());

            // JaxbPunit abstraction properly maps the TransactionType from
            // the JAXB generated class to the JPA enum value.          F1879-16302
            puInfo.setTransactionType(pu.getTransactionType());

            // Set <persistence-unit>
            puInfo.setPersistenceUnitDescription(pu.getDescription());

            // Set <provider>
            puInfo.setPersistenceProviderClassName(pu.getProvider());

            // Set <jta-data-source>
            puInfo.setJtaDataSource(pu.getJtaDataSource());

            // Set <nonjta-data-source>
            puInfo.setNonJtaDataSource(pu.getNonJtaDataSource());

            // Set <mapping-file>
            puInfo.setMappingFileNames(pu.getMappingFile());

            // Set <jar-file>
            puInfo.setJarFileUrls(pu.getJarFile(), pxml); //PK62950

            // Set <class>
            puInfo.setManagedClassNames(pu.getClazz());

            // Set <shared-cache-mode> (mapped by JaxbPUnit abstraction) // F743-8705 F1879-16302
            puInfo.setSharedCacheMode(pu.getSharedCacheMode());

            // Set <validataion-mode> (mapped by JaxbPUnit abstraction)  // F743-8705 F1879-16302
            puInfo.setValidationMode(pu.getValidationMode());

            // Set <exclude-unlisted-classes>
            puInfo.setExcludeUnlistedClasses(pu.isExcludeUnlistedClasses());

            // Set <properties> (mapped by JaxbPUnit abstraction)       F1879-16302
            puInfo.setProperties(pu.getProperties());

            if (isTraceOn && tc.isDebugEnabled()) {
                String rootURLStr = pxml.getRootURL().getFile();
                int earIndex = rootURLStr.indexOf(applInfo.getApplName() + ".ear"); // d507361
                if (earIndex != -1) { // d507361
                    rootURLStr = rootURLStr.substring(earIndex // d507361
                                                      + applInfo.getApplName().length() + 5); // d507361
                } // d507361
                rootURLStr += PERSISTENCE_XML_RESOURCE_NAME; // d507361
                Tr.debug(tc, "extractPersistenceUnits : " + applInfo.getApplName() +
                             "|" + pxml.getArchiveName() + "|" + rootURLStr + "|" +
                             puInfo.getPersistenceUnitName() + "|" +
                             ivScopeInfo.getScopeType() + "|" + puInfo.dump());
            }

            if (getPuInfo(puName) != null) // d441029
            {
                Tr.warning(tc,
                           "DUPLICATE_PERSISTENCE_UNIT_DEFINED_CWWJP0007W",
                           puName, applInfo.getApplName(), pxml.getArchiveName()); // d460065
                puInfo.close(); // d681393
            } else {
                addPU(puName, puInfo);

                // This getFactory initiates the createEntityManagerFactory call to
                // the persistence provider for this pu. This process will trigger
                // the provider to use the jta/non-jta data source defined in the
                // pu as well as register transformer to the application classloader
                // so that when entity class is loaded, the provider can enhance
                // the entity class.
                //
                // However if the factory is created at this point, the resource
                // ref defined for the associated component has not been initialized
                // yet and hence the java:comp/env scheme of specifying data sources
                // in persistence.xml can not be processed.
                //
                // But, if the factory is NOT created at this point, the provider
                // will not register the transformer, and thus the class will not
                // be transformed when it is loaded.
                //
                // To solve this problem, the factory will be created now, and a
                // 'generic' datasource will be provided, but, the created factory
                // will never be used. When a component finally accesses a factory,
                // (after the component has started), a new factory will be created,
                // one for every java:comp/env name context.  Then, each component
                // will have a factory created with the correct datasource.
                puInfo.initialize(); // d429219 d510184
            }

            JPAIntrospection.visitJPAPUnitInfo(puName, puInfo);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "extractPersistenceUnits : # of PU defined = " + getPuCount());
    }

    /**
     * Close all the active EntityManagers declared in this persistence.xml.
     */
    void close() {
        synchronized (ivPuList) {
            for (JPAPUnitInfo puInfo : ivPuList.values()) {
                puInfo.close();
            }
            ivPuList.clear();
        }
    }

    /**
     * Returns the PersistenceUnitInfo object for persistence unit (puName) or null if not
     * defined.
     */
    JPAPUnitInfo getPuInfo(String puName) {
        synchronized (ivPuList) {
            return ivPuList.get(puName);
        }
    }

    /**
     * Adds the puInfo to the collection maintained in this xml info object.
     *
     * @param puName
     * @param puInfo
     * @return JPAUnitInfo just added.
     */
    JPAPUnitInfo addPU(String puName, JPAPUnitInfo puInfo) {
        synchronized (ivPuList) {
            return ivPuList.put(puName, puInfo);
        }
    }

    /**
     * Returns all persistence unit name defined in this persistence.xml.
     *
     * @return Persistence unit name collection.
     */
    Set<String> getPuNames() {
        synchronized (ivPuList) {
            return ivPuList.keySet();
        }
    }

    /**
     * Returns the number of persistence unit defined in this persistence.xml.
     *
     * @return Defined persistence unit count.
     */
    int getPuCount() {
        synchronized (ivPuList) {
            return ivPuList.size();
        }
    }

    /**
     * Dump this persistence.xml data to the input StringBuilder.
     */
    StringBuilder toStringBuilder(StringBuilder sbuf) {
        synchronized (ivPuList) {
            sbuf.append("\n  PxmlInfo: ScopeName=").append(ivScopeInfo.getScopeName()).append("\tRootURL = ").append(ivRootURL).append("\t# PUs = ").append(ivPuList.size()).append("\t[");
            int index = 0;
            for (JPAPUnitInfo puInfo : ivPuList.values()) {
                puInfo.toStringBuilder(sbuf);
                if (++index < ivPuList.size()) {
                    sbuf.append(", ");
                }
            }
            sbuf.append(']');
        }

        return sbuf;
    }

    /**
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return toStringBuilder(new StringBuilder()).toString();
    }

    void doIntrospect(PrintWriter out) {
        final Map<String, JPAPUnitInfo> ivPuListCopy = new HashMap<String, JPAPUnitInfo>();
        synchronized (ivPuList) {
            ivPuListCopy.putAll(ivPuList);
        }

        for (Map.Entry<String, JPAPUnitInfo> entry : ivPuListCopy.entrySet()) {
            final String puName = entry.getKey();
            final JPAPUnitInfo jpaPUInfo = entry.getValue();

            JPAIntrospection.visitJPAPUnitInfo(puName, jpaPUInfo);
        }
    }
}
