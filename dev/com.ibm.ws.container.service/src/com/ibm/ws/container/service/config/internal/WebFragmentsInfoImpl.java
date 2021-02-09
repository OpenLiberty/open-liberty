/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.config.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo.Type;
import com.ibm.ws.container.service.app.deploy.WebModuleClassesInfo;
import com.ibm.ws.container.service.config.WebFragmentInfo;
import com.ibm.ws.container.service.config.WebFragmentsInfo;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.WebFragment;
import com.ibm.ws.javaee.dd.web.common.AbsoluteOrdering;
import com.ibm.ws.javaee.dd.web.common.Ordering;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * Aggregate web fragments information. See the documentation
 * on {@link WebFragmentInfo} for details.
 */
class WebFragmentsInfoImpl implements WebFragmentsInfo {
    /** The trace component. */
    static final TraceComponent tc = Tr.register(WebFragmentsInfoImpl.class);

    //

    /**
     * Create aggregate fragment information for a web module.
     *
     * @param containerToAdapt The container of the web module.
     * @param servletSpecLevel The schema level of the web module descriptor.
     *
     * @throws UnableToAdaptException Thrown if a failure occurs
     */
    WebFragmentsInfoImpl(Container containerToAdapt, int servletSpecLevel) throws UnableToAdaptException {
        // Web container feature ...

        this.servletSpecLevel = servletSpecLevel;

        // Web app ...

        WebApp webApp = containerToAdapt.adapt(WebApp.class); // throws UnableToAdaptException
        if (webApp != null) {
            this.servletSchemaLevel = webApp.getVersion();
            if ("5.0".equals(servletSchemaLevel) || "4.0".equals(servletSchemaLevel) || "3.1".equals(servletSchemaLevel) || "3.0".equals(servletSchemaLevel)
                || "2.5".equals(servletSchemaLevel)) {
                this.isMetadataComplete = webApp.isSetMetadataComplete() && webApp.isMetadataComplete();
            } else {
                this.isMetadataComplete = true; // Default to true for earlier versions.
            }
            this.absoluteOrdering = webApp.getAbsoluteOrdering(); // Null when (servletVersion == 2.5).

        } else {
            this.servletSchemaLevel = null;
            this.isMetadataComplete = false;
            this.absoluteOrdering = null;
        }

        // Results collections ...

        List<WebFragmentInfo> useOrderedWebFragmentItems = new ArrayList<WebFragmentInfo>();
        List<WebFragmentInfo> useExcludedWebFragmentItems = new ArrayList<WebFragmentInfo>();

        WebModuleClassesInfo classesInfo = containerToAdapt.adapt(WebModuleClassesInfo.class); // throws UnableToAdaptException

        sortWebFragments(classesInfo, useOrderedWebFragmentItems, useExcludedWebFragmentItems); // throws UnableToAdaptException

        if (useOrderedWebFragmentItems.isEmpty()) {
            this.orderedWebFragmentItems = Collections.emptyList();
        } else {
            this.orderedWebFragmentItems = Collections.unmodifiableList(useOrderedWebFragmentItems);
        }

        if (useExcludedWebFragmentItems.isEmpty()) {
            this.excludedWebFragmentItems = Collections.emptyList();
        } else {
            this.excludedWebFragmentItems = Collections.unmodifiableList(useExcludedWebFragmentItems);
        }
    }

    // Web container data ...

    /** Cache of the web container feature level. */
    private final int servletSpecLevel;

    @Trivial
    @Override
    public int getServletSpecLevel() {
        return servletSpecLevel;
    }

    // Web module data ...

    /** Cache of the schema version of the web module descriptor. */
    private final String servletSchemaLevel;

    @Trivial
    @Override
    public String getServletSchemaLevel() {
        return servletSchemaLevel;
    }

    /** Cache of the 'metadata-complete' attribute of the web module descriptor. */
    private final boolean isMetadataComplete;

    @Trivial
    @Override
    public boolean isModuleMetadataComplete() {
        return this.isMetadataComplete;
    }

    /** Cache of the 'absolute-ordering' element of the web module descriptor. */
    private final AbsoluteOrdering absoluteOrdering;

    // Results ...

    /** Result storage: The ordered fragments. */
    private final List<WebFragmentInfo> orderedWebFragmentItems;

    @Override
    public List<WebFragmentInfo> getOrderedFragments() {
        return this.orderedWebFragmentItems;
    }

    /** Result storage: The unordered excluded fragments. */
    private final List<WebFragmentInfo> excludedWebFragmentItems;

    @Override
    public List<WebFragmentInfo> getExcludedFragments() {
        return this.excludedWebFragmentItems;
    }

    /**
     * a. Validate whether there are duplicate web fragment name, and throw exception if relative sorting is used
     * b. Generate internal used name for those web fragments, which have not been explicitly configured
     * c. Depending on the absolute ordering configuration in web.xml, execute absolute sorting or relative sorting
     */
    private List<WebFragmentInfo> sortWebFragments(WebModuleClassesInfo classesInfo,
                                                   List<WebFragmentInfo> orderedItems,
                                                   List<WebFragmentInfo> excludedWebFragmentItems) throws UnableToAdaptException {

        List<ContainerInfo> classesContainers = classesInfo.getClassesContainers();

        Map<String, WebFragmentItemImpl> webFragmentItemMap = new LinkedHashMap<String, WebFragmentItemImpl>(classesContainers.size());

        String internalGeneratedWebFragmentNamePrefix = "ibm_liberty_webfragment_";
        int nameSuffix = 0;
        Set<String> usedWebFragmentNames = new HashSet<String>();
        for (ContainerInfo containerInfo : classesContainers) {
            Container container = containerInfo.getContainer();
            String libraryURI = containerInfo.getName();
            if (containerInfo.getType() == Type.WEB_INF_CLASSES) {
                orderedItems.add(new WebFragmentItemImpl(container, null, libraryURI, null, !this.isMetadataComplete));
            } else if (containerInfo.getType() == Type.WEB_INF_LIB) {
                WebFragment webFragment = container.adapt(WebFragment.class);
                boolean isSeed = !this.isMetadataComplete && (webFragment == null || !webFragment.isMetadataComplete());
                String webFragmentName = null;
                if (webFragment != null) {
                    webFragmentName = webFragment.getName();
                    //Add all the before/after names configured, in case users configure an internal generated web fragment name
                    if (webFragment.getOrdering() != null) {
                        Ordering order = webFragment.getOrdering();
                        if (order.isSetBefore()) {
                            usedWebFragmentNames.addAll(order.getBeforeNames());
                        }
                        if (order.isSetAfter()) {
                            usedWebFragmentNames.addAll(order.getAfterNames());
                        }
                    }
                }
                if (webFragmentName != null) {
                    usedWebFragmentNames.add(webFragmentName);
                    if (webFragmentItemMap.containsKey(webFragmentName) && this.absoluteOrdering == null) {
                        throw new UnableToAdaptException(Tr.formatMessage(tc, "NO_UNIQUE_WEB_FRAGMENT_NAMES",
                                                                          webFragmentName, libraryURI,
                                                                          webFragmentItemMap.get(webFragmentName).libraryURI));
                        //Set null for the web-fragment name of current web-fragment.xml, so we could assign it a temporary name in the following codes
                        //webFragmentName = null;
                    }
                }
                if (webFragmentName == null) {
                    webFragmentName = internalGeneratedWebFragmentNamePrefix + nameSuffix;
                    while (usedWebFragmentNames.contains(webFragmentName)) {
                        nameSuffix++;
                        webFragmentName = internalGeneratedWebFragmentNamePrefix + nameSuffix;
                    }
                    usedWebFragmentNames.add(webFragmentName);
                }

                webFragmentItemMap.put(webFragmentName, new WebFragmentItemImpl(container, webFragment, libraryURI, webFragmentName, isSeed));
            } //else type is not webinf_classes or webinf_lib
        }

        // 'original' and 'relative' ordering generates no excluded fragment items.
        //
        // Only 'absolute' ordering can generate excluded fragment items.

        if (this.servletSpecLevel == WebApp.VERSION_3_0) {
            //SERVLET 3.0 COMPATIBILITY. This was shipped Liberty function and must be maintained.
            //Any future changes here should be done with custom property enablement.
            if (this.isMetadataComplete) {
                orderedItems.addAll(originalOrderWebFragments(webFragmentItemMap));
            } else if (this.absoluteOrdering != null) {
                orderedItems.addAll(absoluteOrderWebFragments(webFragmentItemMap, excludedWebFragmentItems));
            } else {
                orderedItems.addAll(relativeOrderWebFragments(webFragmentItemMap));
            }
        } else {
            //SERVLET 3.1 SPEC CORRECT
            //Absolute ordering is used whether metadata-complete is true or not.
            //We use that first, if it's not specified then we check MD-complete. If that's false, then we go into
            //the collection of ordering elements from fragments.
            if (this.absoluteOrdering != null)
                orderedItems.addAll(absoluteOrderWebFragments(webFragmentItemMap, excludedWebFragmentItems));
            else if (this.isMetadataComplete)
                orderedItems.addAll(originalOrderWebFragments(webFragmentItemMap));
            else
                orderedItems.addAll(relativeOrderWebFragments(webFragmentItemMap));
        }

        return orderedItems;
    }

    private List<WebFragmentInfo> originalOrderWebFragments(Map<String, WebFragmentItemImpl> webFragmentItemMap) {
        List<WebFragmentInfo> orderedItems = new ArrayList<WebFragmentInfo>(webFragmentItemMap.size());
        for (Map.Entry<String, WebFragmentItemImpl> mapEntry : webFragmentItemMap.entrySet()) {
            orderedItems.add(mapEntry.getValue());
        }
        return orderedItems;
    }

    private List<WebFragmentInfo> absoluteOrderWebFragments(Map<String, WebFragmentItemImpl> webFragmentItemMap,
                                                            List<WebFragmentInfo> excludedWebFragmentItems) {

        List<WebFragmentInfo> orderedItems = new ArrayList<WebFragmentInfo>(webFragmentItemMap.size());

        Map<String, WebFragmentInfo> candidateWebFragmentItemMap = new LinkedHashMap<String, WebFragmentInfo>(webFragmentItemMap);
        for (String webFragmentName : this.absoluteOrdering.getNamesBeforeOthers()) {
            WebFragmentInfo webFragmentItem = candidateWebFragmentItemMap.remove(webFragmentName);
            if (webFragmentItem != null) {
                orderedItems.add(webFragmentItem);
            }
        }
        int othersBeginIndex = orderedItems.size();
        for (String webFragmentName : this.absoluteOrdering.getNamesAfterOthers()) {
            WebFragmentInfo webFragmentItem = candidateWebFragmentItemMap.remove(webFragmentName);
            if (webFragmentItem != null) {
                orderedItems.add(webFragmentItem);
            }
        }
        if (this.absoluteOrdering.isSetOthers()) {
            orderedItems.addAll(othersBeginIndex, candidateWebFragmentItemMap.values());
        } else {
            excludedWebFragmentItems.addAll(candidateWebFragmentItemMap.values());
        }

        return orderedItems;
    }

    private static class WebFragmentItemImpl implements WebFragmentInfo {
        private final Container container;
        public final WebFragment webFragment;
        public final String libraryURI;
        public final String name;
        public final boolean isSeedFragment;

        public WebFragmentItemImpl(Container container, WebFragment webFragment, String libraryURI, String name, boolean isSeed) {
            this.container = container;
            this.webFragment = webFragment;
            this.libraryURI = libraryURI;
            this.name = name;
            this.isSeedFragment = isSeed;
        }

        @Trivial
        @Override
        public WebFragment getWebFragment() {
            return this.webFragment;
        }

        @Trivial
        @Override
        public String getLibraryURI() {
            return this.libraryURI;
        }

        @Trivial
        @Override
        public Container getFragmentContainer() {
            return this.container;
        }

        @Trivial
        @Override
        public boolean isSeedFragment() {
            return this.isSeedFragment;
        }

        @Trivial
        @Override
        public boolean isPartialFragment() {
            return !this.isSeedFragment;
        }

        @Trivial
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            WebFragmentItemImpl other = (WebFragmentItemImpl) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }

        @Trivial
        @Override
        public int hashCode() {
            return (name == null) ? 0 : name.hashCode();
        }

        @Trivial
        @Override
        public String toString() {
            return name + "(" + libraryURI + ")";
        }
    }

    static class RelativeOrderMetaData {

        private boolean visited = false;
        private ArrayList<String> afterNameList;
        private ArrayList<String> beforeNameList;
        private final String name;
        private final WebFragmentInfo target;
        private final boolean isOthers;
        private OthersEnum othersEnum = OthersEnum.UNSPECIFIED;
        private final boolean duplicateNames;

        private static RelativeOrderMetaData getNewRelativeOthersElement() {
            return new RelativeOrderMetaData(null, "com_ibm_ws_webfragmerger_others", null, true);
        }

        private RelativeOrderMetaData(Ordering ordering, String webFragmentName, WebFragmentInfo webFragmentInfo) {
            this(ordering, webFragmentName, webFragmentInfo, false);
        }

        private RelativeOrderMetaData(Ordering ordering, String webFragmentName, WebFragmentInfo webFragmentInfo, boolean isOthers) {
            boolean duplicateNames = false;
            if (ordering != null && ordering.isSetAfter()) {
                afterNameList = new ArrayList<String>();
                List<String> afterNames = ordering.getAfterNames();
                for (String afterName : afterNames) {
                    if (afterNameList.contains(afterName)) {
                        duplicateNames = true;
                    } else {
                        afterNameList.add(afterName);
                    }
                }
                if (ordering.isSetAfterOthers()) {
                    othersEnum = OthersEnum.AFTER;
                }
            }

            if (ordering != null && ordering.isSetBefore()) {
                beforeNameList = new ArrayList<String>();
                List<String> beforeNames = ordering.getBeforeNames();
                for (String beforeName : beforeNames) {
                    if (beforeNameList.contains(beforeName)) {
                        duplicateNames = true;
                    } else {
                        beforeNameList.add(beforeName);
                    }
                }
                if (ordering.isSetBeforeOthers()) {
                    if (othersEnum == OthersEnum.AFTER || othersEnum == OthersEnum.CONFLICT)
                        othersEnum = OthersEnum.CONFLICT;
                    else
                        othersEnum = OthersEnum.BEFORE;
                }
            }

            this.name = webFragmentName;
            this.target = webFragmentInfo;
            this.isOthers = isOthers;
            this.duplicateNames = duplicateNames;
        }

        @Trivial
        public boolean isDuplicateNames() {
            return duplicateNames;
        }

        @Trivial
        public List<String> getAfterNameList() {
            return afterNameList;
        }

        @Trivial
        public List<String> getBeforeNameList() {
            return beforeNameList;
        }

        @Trivial
        public void setVisited(boolean visited) {
            this.visited = visited;
        }

        @Trivial
        public boolean isVisited() {
            return visited;
        }

        @Trivial
        public String getName() {
            return name;
        }

        @Trivial
        public WebFragmentInfo getTarget() {
            return target;
        }

        public void addAfterName(String name) {
            if (afterNameList == null)
                afterNameList = new ArrayList<String>();
            if (!afterNameList.contains(name))
                afterNameList.add(name);
        }

        public void addBeforeName(String name) {
            if (beforeNameList == null)
                beforeNameList = new ArrayList<String>();
            if (!beforeNameList.contains(name))
                this.beforeNameList.add(name);
        }

        @Trivial
        public OthersEnum getOthersEnum() {
            return othersEnum;
        }

        @Trivial
        public void setOthersEnum(OthersEnum othersEnum) {
            this.othersEnum = othersEnum;
        }

        @Trivial
        public boolean isOthers() {
            return this.isOthers;
        }

        public boolean hasOrderingMetaData() {
            return ((afterNameList != null && !afterNameList.isEmpty()) || (beforeNameList != null && !beforeNameList.isEmpty()));
        }
    }

    static enum OthersEnum {
        UNSPECIFIED,
        AFTER,
        BEFORE,
        CONFLICT
    }

    List<WebFragmentInfo> relativeOrderWebFragments(Map<String, WebFragmentItemImpl> webFragmentItemMap) throws UnableToAdaptException {
        boolean isTracing = TraceComponent.isAnyTracingEnabled();

        Map<String, RelativeOrderMetaData> relativeOrderMap = new TreeMap<String, RelativeOrderMetaData>();
        boolean containsRelativeOrderElement = false;
        List<WebFragmentInfo> unnamedOthers = null;
        int othersPosition = -1;
        List<WebFragmentInfo> relativeOrderingList;

        for (Map.Entry<String, WebFragmentItemImpl> warFragmentFile : webFragmentItemMap.entrySet()) {
            WebFragmentItemImpl webFragmentInfo = warFragmentFile.getValue();
            if (isTracing && tc.isDebugEnabled()) { // FINER
                Tr.debug(tc, "webFragmentInfo->[{0}]", webFragmentInfo);
            }
            WebFragment currentWebFragment = webFragmentInfo.getWebFragment();
            Ordering ordering = currentWebFragment != null ? currentWebFragment.getOrdering() : null;
            if (ordering != null) {
                if (isTracing && tc.isDebugEnabled()) { // FINER
                    Tr.debug(tc, "ordering->[{0}]", ordering);
                }
                RelativeOrderMetaData relativeOrderMetaData = new RelativeOrderMetaData(ordering, currentWebFragment.getName(), webFragmentInfo);

                String name = currentWebFragment.getName();
                if (relativeOrderMetaData.isDuplicateNames()) {
                    if (isTracing && tc.isDebugEnabled()) {
                        Tr./* error */debug(tc, "duplicate names found in relative ordering metadata of [" + name + "]");
                    }
                    String msg = Tr.formatMessage(tc, "duplicate.names.found.in.relative.ordering.metadata.CWWKM0477E",
                                                  "Duplicate names found in relative ordering metadata", name);
                    throw new UnableToAdaptException(msg);
                }

                //TODO: what if you have two fragments with the same name.
                // throw an exception?

                //Duplicate name exception: if, when traversing the web-fragments, multiple
                //members with the same <name> element are encountered, the application must
                //log an informative error message including information to help fix the
                //problem, and must fail to deploy

                if (isTracing && tc.isDebugEnabled()) { // FINER
                    Tr.debug(tc, "current relativeOrderMetaData[{0}]", relativeOrderMetaData);
                }
                if (relativeOrderMap.containsKey(name)) {
                    if (isTracing && tc.isDebugEnabled()) {
                        Tr./* error */debug(tc, "two fragments have the name [" + name + "]");
                    }
                    String msg = Tr.formatMessage(tc, "two.fragments.have.the.same.name.CWWKM0479E",
                                                  "Two fragments have the same name", name);
                    throw new UnableToAdaptException(msg);
                }

                relativeOrderMap.put(name, relativeOrderMetaData);
                containsRelativeOrderElement = true;

            } else {
                if (isTracing && tc.isDebugEnabled()) { // FINER
                    Tr.debug(tc, "no ordering for->[{0}]", webFragmentInfo);
                }
                if (currentWebFragment != null && currentWebFragment.getName() != null) {
                    RelativeOrderMetaData relativeOrderMetaData = new RelativeOrderMetaData(null, currentWebFragment.getName(), webFragmentInfo);
                    if (isTracing && tc.isDebugEnabled()) { // FINER
                        Tr.debug(tc, "no ordering, current relativeOrderMetaData[{0}]", relativeOrderMetaData);
                    }
                    relativeOrderMap.put(currentWebFragment.getName(), relativeOrderMetaData);

                } else {
                    if (unnamedOthers == null) {
                        unnamedOthers = new ArrayList<WebFragmentInfo>();
                    }
                    if (isTracing && tc.isDebugEnabled()) { // FINER
                        Tr.debug(tc, "no ordering, no name [{0}]", currentWebFragment);
                    }
                    unnamedOthers.add(webFragmentInfo);
                }
            }
        }

        //add others to the map at the end since it should be after by default
        relativeOrderMap = new LinkedHashMap<String, RelativeOrderMetaData>(relativeOrderMap);
        RelativeOrderMetaData othersROMD = RelativeOrderMetaData.getNewRelativeOthersElement();
        relativeOrderMap.put(othersROMD.getName(), othersROMD);

        relativeOrderingList = new ArrayList<WebFragmentInfo>();
        List<RelativeOrderMetaData> relOrderMetaDataList;
        if (containsRelativeOrderElement && (relOrderMetaDataList = getRelativeOrdering(relativeOrderMap)) != null) {
            int counter = 0;
            //TODO: update this code if getAbsoluteOrdering returns null when not specified at all
            for (RelativeOrderMetaData relOrderMetaData : relOrderMetaDataList) {
                if (relOrderMetaData.isOthers()) {
                    othersPosition = counter;
                    if (isTracing && tc.isDebugEnabled()) { // FINER
                        Tr.debug(tc, "set others position to-> [ {0} ]", othersPosition);
                    }
                } else {
                    WebFragmentInfo orderedWarFragmentFile = relOrderMetaData.getTarget();
                    relativeOrderingList.add(orderedWarFragmentFile);

                    if (isTracing && tc.isDebugEnabled()) { // FINER
                        Tr.debug(tc, "added [{0}] to [{1}]", orderedWarFragmentFile, relativeOrderingList);
                    }
                }
                relativeOrderMap.remove(relOrderMetaData.getName());
                counter++;
            }

            if (othersPosition != -1) {
                int tempOthersPosition = othersPosition;
                for (RelativeOrderMetaData relOrderMD : relativeOrderMap.values()) {
                    if (isTracing && tc.isDebugEnabled()) { // FINER
                        Tr.debug(tc, "adding other [{0}] at position [{1}]",
                                 relOrderMD.getTarget(), othersPosition);
                    }
                    WebFragmentInfo warFragmentFile = relOrderMD.getTarget();
                    relativeOrderingList.add(tempOthersPosition, warFragmentFile);
                    tempOthersPosition++;
                }
                //unnamedOthers will contain all of the other jars in the WEB-INF/lib even if they do not contain web-fragment.xml
                if (unnamedOthers != null) {
                    for (WebFragmentInfo unnamedOther : unnamedOthers) {
                        if (isTracing && tc.isDebugEnabled()) { // FINER
                            Tr.debug(tc, "adding unnamed other [{0}] at position [{1}]",
                                     unnamedOther, othersPosition);
                        }
                        relativeOrderingList.add(tempOthersPosition, unnamedOther);
                        tempOthersPosition++;
                    }
                }
            }
        } else {
            if (isTracing && tc.isDebugEnabled()) { // FINER
                if (!containsRelativeOrderElement) {
                    Tr.debug(tc, "no relative ordering elements in the list of fragments");
                } else {
                    Tr.debug(tc, "nothing returned from relative order calculation. return original list");
                }
            }
            for (Map.Entry<String, WebFragmentItemImpl> warFragmentFile : webFragmentItemMap.entrySet()) {
                WebFragmentItemImpl webFragmentInfo = warFragmentFile.getValue();
                relativeOrderingList.add(webFragmentInfo);
            }
        }

        return relativeOrderingList;
    }

    //

    public static List<RelativeOrderMetaData> getRelativeOrdering(Map<String, RelativeOrderMetaData> relativeOrderMap) throws UnableToAdaptException {

        boolean isTracing = TraceComponent.isAnyTracingEnabled();
        Set<Map.Entry<String, RelativeOrderMetaData>> relativeOrderSet = relativeOrderMap.entrySet();

        //add pointers in each direction
        for (Map.Entry<String, RelativeOrderMetaData> relativeOrderEntry : relativeOrderSet) {

            if (isTracing && tc.isDebugEnabled()) {
                Tr.debug(tc, "adding pointers in each direction for [{0}]", relativeOrderEntry);
            }

            RelativeOrderMetaData relativeOrderMetaData = relativeOrderEntry.getValue();

            List<String> beforeNames = relativeOrderMetaData.getBeforeNameList();
            if (beforeNames != null) {
                for (String beforeName : beforeNames) {
                    RelativeOrderMetaData beforeRelativeOrderMetaData = relativeOrderMap.get(beforeName);
                    if (beforeRelativeOrderMetaData != null) {
                        if (isTracing && tc.isDebugEnabled()) {
                            Tr.debug(tc, "add after to [{0}] for [{1}]", beforeRelativeOrderMetaData, relativeOrderMetaData);
                        }
                        beforeRelativeOrderMetaData.addAfterName(relativeOrderMetaData.getName());
                    }
                }
            }

            List<String> afterNames = relativeOrderMetaData.getAfterNameList();
            if (afterNames != null) {
                for (String afterName : afterNames) {
                    RelativeOrderMetaData afterRelativeOrderMetaData = relativeOrderMap.get(afterName);
                    if (afterRelativeOrderMetaData != null) {
                        if (isTracing && tc.isDebugEnabled()) {
                            Tr.debug(tc, "add before to [{0}] for [{1}]", afterRelativeOrderMetaData, relativeOrderMetaData);
                        }
                        afterRelativeOrderMetaData.addBeforeName(relativeOrderMetaData.getName());
                    }
                }
            }

        }

        for (Map.Entry<String, RelativeOrderMetaData> relativeOrderEntry : relativeOrderSet) {
            RelativeOrderMetaData relativeOrderMetaData = relativeOrderEntry.getValue();
            propagateOthersMetaData(relativeOrderMetaData, relativeOrderMap);
        }

        LinkedList<String> orderedList = new LinkedList<String>();
        LinkedList<String> stack;

        List<RelativeOrderMetaData> beforeOthers = new ArrayList<RelativeOrderMetaData>();
        List<RelativeOrderMetaData> unspecified = new ArrayList<RelativeOrderMetaData>();
        List<RelativeOrderMetaData> afterOthers = new ArrayList<RelativeOrderMetaData>();

        for (Map.Entry<String, RelativeOrderMetaData> relativeOrderEntry : relativeOrderSet) {
            RelativeOrderMetaData relativeOrderMetaData = relativeOrderEntry.getValue();
            OthersEnum othersEnum = relativeOrderMetaData.getOthersEnum();
            if (othersEnum == OthersEnum.AFTER) {
                afterOthers.add(relativeOrderMetaData);
            } else if (othersEnum == OthersEnum.BEFORE) {
                beforeOthers.add(relativeOrderMetaData);
            } else if (othersEnum == OthersEnum.UNSPECIFIED) {
                unspecified.add(relativeOrderMetaData);
            } else {
                if (isTracing && tc.isDebugEnabled()) {
                    Tr./* error */debug(tc, relativeOrderMetaData + " says its both before and after others");
                }
                String msg = Tr.formatMessage(tc, "relative.ordering.metadata.is.before.and.after.others.CWWKM0478E",
                                              "Relative ordering metadata says its both before and after others", relativeOrderMetaData);
                throw new UnableToAdaptException(msg);
            }
        }

        if (isTracing && tc.isDebugEnabled()) {
            Tr.debug(tc, "afterOthers [{0}], beforeOthers [{1}], unspecified [{2}]", afterOthers, beforeOthers, unspecified);
        }

        for (RelativeOrderMetaData relativeOrderMetaData : beforeOthers) {
            stack = new LinkedList<String>();

            traverseAfter(relativeOrderMetaData, orderedList, stack, relativeOrderMap, false);
        }

        for (RelativeOrderMetaData relativeOrderMetaData : unspecified) {
            stack = new LinkedList<String>();

            traverseAfter(relativeOrderMetaData, orderedList, stack, relativeOrderMap, true);
        }

        for (RelativeOrderMetaData relativeOrderMetaData : afterOthers) {
            stack = new LinkedList<String>();

            traverseAfter(relativeOrderMetaData, orderedList, stack, relativeOrderMap, false);
        }

        List<RelativeOrderMetaData> relOrderingList = new ArrayList<RelativeOrderMetaData>();
        for (String orderedString : orderedList) {
            relOrderingList.add(relativeOrderMap.get(orderedString));
        }

        if (isTracing && tc.isEventEnabled()) {
            Tr.event(tc, "relOrderingList->" + relOrderingList);
        }

        return relOrderingList;

    }

    @Trivial
    private static void propagateOthersMetaData(RelativeOrderMetaData relativeOrderMetaData, Map<String, RelativeOrderMetaData> relativeOrderMap) throws UnableToAdaptException {
        boolean isTracing = TraceComponent.isAnyTracingEnabled();
        OthersEnum othersEnum = relativeOrderMetaData.getOthersEnum();
        if (isTracing && tc.isDebugEnabled()) {
            Tr.debug(tc, "others for [{0}] is [{1}]", relativeOrderMetaData, othersEnum);
        }
        if (othersEnum != OthersEnum.UNSPECIFIED) {
            if (othersEnum == OthersEnum.AFTER) {
                List<String> beforeNameList = relativeOrderMetaData.getBeforeNameList();
                setChildOthersMetaData(beforeNameList, othersEnum, relativeOrderMap, relativeOrderMetaData);
            } else {
                List<String> afterNameList = relativeOrderMetaData.getAfterNameList();
                setChildOthersMetaData(afterNameList, othersEnum, relativeOrderMap, relativeOrderMetaData);
            }

        }
    }

    private static void setChildOthersMetaData(List<String> nameList,
                                               OthersEnum parentOthersEnum,
                                               Map<String, RelativeOrderMetaData> relativeOrderMap,
                                               RelativeOrderMetaData parentRelativeOrderMetaData) throws UnableToAdaptException {

        boolean isTracing = TraceComponent.isAnyTracingEnabled();
        if (nameList != null) {
            for (String name : nameList) {
                RelativeOrderMetaData relOrderMetadata = relativeOrderMap.get(name);
                if (relOrderMetadata != null) {
                    if (relOrderMetadata.getOthersEnum() != OthersEnum.UNSPECIFIED
                        && relOrderMetadata.getOthersEnum() != parentOthersEnum) {

                        if (isTracing && tc.isDebugEnabled()) {
                            Tr./* error */debug(tc, "relation to others is conflicting between [" + parentRelativeOrderMetaData + ","
                                                    + relOrderMetadata + "]");
                        }
                        String msg = Tr.formatMessage(tc, "conflicting.relative.order.metadata.CWWKM0480E",
                                                      "Conflicting relative order metadata", parentRelativeOrderMetaData, relOrderMetadata);
                        throw new UnableToAdaptException(msg);
                    } else {

                        if (relOrderMetadata.getOthersEnum() == parentOthersEnum) {//already traversed
                            if (isTracing && tc.isDebugEnabled()) {
                                Tr.debug(tc, "already traversed [{0}]", relOrderMetadata);
                            }
                            continue;
                        }
                        if (isTracing && tc.isDebugEnabled()) {
                            Tr.debug(tc, "set others for [{0}] to [{1}]", relOrderMetadata, parentOthersEnum);
                        }
                        relOrderMetadata.setOthersEnum(parentOthersEnum);
                        propagateOthersMetaData(relOrderMetadata, relativeOrderMap);
                    }
                }
            }
        }
    }

    private static void traverseAfter(RelativeOrderMetaData parentRelOrdMetaData,
                                      LinkedList<String> orderedList, LinkedList<String> stack, Map<String, RelativeOrderMetaData> relativeOrderMap,
                                      boolean isUnspecifiedList) throws UnableToAdaptException {
        boolean isTracing = TraceComponent.isAnyTracingEnabled();
        String parentName = parentRelOrdMetaData.getName();
        if (stack.contains(parentName)) {
            int listSize = stack.size();
            StringBuilder strBuild = new StringBuilder();
            strBuild.append("loop found in list [" + parentName);

            for (int i = listSize - 1; i >= 0; i--) {
                strBuild.append("," + stack.get(i));
            }
            strBuild.append("]");

            String errorMsg = strBuild.toString();

            if (isTracing && tc.isDebugEnabled()) {
                Tr./* error */debug(tc, errorMsg);
            }

            String msg = Tr.formatMessage(tc, "loop.found.in.list.CWWKM0481E",
                                          errorMsg);
            throw new UnableToAdaptException(msg);
        } else {
            if (isTracing && tc.isDebugEnabled()) {
                Tr.debug(tc, "adding [" + parentName + "] to stack [" + stack + "]");
            }
            stack.add(parentName);
        }

        if (!parentRelOrdMetaData.isVisited()) {
            parentRelOrdMetaData.setVisited(true);

            List<String> afterNameList = parentRelOrdMetaData.getAfterNameList();
            if (afterNameList != null && !afterNameList.isEmpty()) {
                for (String afterName : afterNameList) {
                    RelativeOrderMetaData childRelOrdMetaData = relativeOrderMap.get(afterName);
                    if (childRelOrdMetaData == null) {
                        if (isTracing && tc.isDebugEnabled()) {
                            Tr./* warning */debug(tc, "unable to locate frag in relative ordering named->" + afterName);
                        }
                    } else {
                        traverseAfter(childRelOrdMetaData, orderedList, stack, relativeOrderMap, isUnspecifiedList);
                    }
                }
            } else if (isTracing && tc.isDebugEnabled()) {
                Tr.debug(tc, "after name list is empty for [{0}]", parentRelOrdMetaData);
            }

            if (!orderedList.contains(parentName)) {
                if (isUnspecifiedList && !parentRelOrdMetaData.hasOrderingMetaData() && !parentRelOrdMetaData.isOthers()) {
                    if (isTracing && tc.isDebugEnabled()) {
                        Tr.debug(tc, "[{0}] has no ordering metadata, insert later with unnamed others", parentName);
                    }
                } else {
                    orderedList.add(parentName);
                    if (isTracing && tc.isDebugEnabled()) {
                        Tr.debug(tc, "added [{0}] to orderedList[{1}]", parentName, orderedList);
                    }
                }
            } else {
                //I don't think you should ever get here.
                String msg = Tr.formatMessage(tc, "ordered.list.already.contains.CWWKM0482E",
                                              parentName);
                throw new UnableToAdaptException(msg);
            }
        } else if (isTracing && tc.isDebugEnabled()) {
            Tr.debug(tc, "[{0}] already visited", parentRelOrdMetaData);
        }

        if (isTracing && tc.isDebugEnabled()) {
            Tr.debug(tc, "before stack.removeLast() call: ", parentRelOrdMetaData, stack, orderedList);
        }
        stack.removeLast();
    }
}
