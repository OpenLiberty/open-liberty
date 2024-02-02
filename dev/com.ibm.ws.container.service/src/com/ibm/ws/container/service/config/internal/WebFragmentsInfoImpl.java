/*********************************************************************
 * Copyright (c) 2012, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.config.internal;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.ibm.ws.javaee.dd.PlatformVersion;
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

    private static final HashSet<Integer> METADATA_COMPLETE_SUPPORT = //
                    new HashSet<>(Arrays.asList(WebApp.VERSION_2_5,
                                                WebApp.VERSION_3_0, WebApp.VERSION_3_1,
                                                WebApp.VERSION_4_0,
                                                WebApp.VERSION_5_0,
                                                WebApp.VERSION_6_0, WebApp.VERSION_6_1));

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
            if (isMetadataCompleteSupported(servletSchemaLevel)) {
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

        this.orderedWebFragmentItems = asUnmodifiable(useOrderedWebFragmentItems);
        this.excludedWebFragmentItems = asUnmodifiable(useExcludedWebFragmentItems);
    }

    private static <T> List<T> asUnmodifiable(List<T> source) {
        if (source.isEmpty()) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(source);
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

    /**
     * Verify if the web-app version supports metadata-complete elements.
     * metadata-complete was added in version 2.5.
     *
     * @param version
     *
     * @return true if metadata-complete is supported,
     *         false if metadata-complete is not supported or the schema version is unknown
     */
    private boolean isMetadataCompleteSupported(String version) {
        try {
            int intVersion = PlatformVersion.getVersionInt(version);
            return METADATA_COMPLETE_SUPPORT.contains(intVersion);
        } catch (IllegalArgumentException e) {
            return false;
        }
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
     * Sort web fragments (including the classes folder) into ordered and excluded items.
     *
     * <ul>
     * <li>Throw an exception if a duplicate fragment name is used and relative sorting is used.</li>
     * <li>Generate unique internal names for unnamed fragments.</li>
     * <li>Sort the fragments, using either original ordering, absolute ordering, or relative ordering,
     * according to the web module and fragment descriptors.</li>
     *
     * @param classesInfo              Overall information about the web module class path. Used
     *                                     here to obtain the list of classes containers for the web module and web
     *                                     fragments.
     * @param orderedItems             A results collection. Must initially be empty. Will be
     *                                     populated with the sorted fragments.
     * @param excludedWebFragmentItems A results collection. Must initially be empty. Will be
     *                                     populated only if absolute ordering is used. Populated with fragments which
     *                                     are not listed in the absolute ordering. (This is possible only if the
     *                                     absolute ordering does not contain an others element.)
     *
     * @return The ordered items. Excluded items are not included in the ordered items list.
     */
    private List<WebFragmentInfo> sortWebFragments(WebModuleClassesInfo classesInfo,
                                                   List<WebFragmentInfo> orderedItems,
                                                   List<WebFragmentInfo> excludedWebFragmentItems) throws UnableToAdaptException {

        List<ContainerInfo> classesContainers = classesInfo.getClassesContainers();

        Map<String, WebFragmentItemImpl> webFragmentItemMap = new LinkedHashMap<String, WebFragmentItemImpl>(classesContainers.size());

        String internalGeneratedWebFragmentNamePrefix = "ibm_liberty_webfragment_";
        int internalPrefixLength = internalGeneratedWebFragmentNamePrefix.length();
        StringBuilder stringBuilder = null;
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
                    if (stringBuilder == null) {
                        stringBuilder = new StringBuilder(internalPrefixLength + 8);
                        stringBuilder.append(internalGeneratedWebFragmentNamePrefix);
                    } else {
                        stringBuilder.setLength(internalPrefixLength);
                    }
                    stringBuilder.append(nameSuffix);
                    webFragmentName = stringBuilder.toString();
                    while (!usedWebFragmentNames.add(webFragmentName)) {
                        nameSuffix++;
                        stringBuilder.setLength(internalPrefixLength);
                        stringBuilder.append(nameSuffix);
                        webFragmentName = stringBuilder.toString();
                    }
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

    /**
     * Most basic ordering of fragments. Place the fragment items as they are
     * already ordered in the fragment map.
     *
     * @return The ordered fragment items.
     */
    private List<WebFragmentInfo> originalOrderWebFragments(Map<String, WebFragmentItemImpl> webFragmentItemMap) {
        List<WebFragmentInfo> orderedItems = new ArrayList<WebFragmentInfo>(webFragmentItemMap.size());
        orderedItems.addAll(webFragmentItemMap.values());
        return orderedItems;
    }

    /**
     * An absolute ordering was specified. Sort un-excluded items according to the
     * absolute ordering. Put excluded items in separate storage.
     *
     * @param webFragmentItemMap       The map of all fragment items.
     * @param excludedWebFragmentItems Results collection. Will hold any excluded items.
     *
     * @return The sorted fragment items. Excluded items are not present in the sorted
     *         items list.
     */
    private List<WebFragmentInfo> absoluteOrderWebFragments(Map<String, WebFragmentItemImpl> webFragmentItemMap,
                                                            List<WebFragmentInfo> excludedWebFragmentItems) {

        // Make a copy of the original items.  Ordering must be preserved.

        // Walk through the absolute ordering, placing before and after names as they
        // are given in the absolute ordering.  Note the position of the others element.

        // If an others element is present, place the any remaining fragments
        // in the others position, in their original order.
        //
        // If there is no others element, place any remaining fragments as
        // excluded items.

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

    List<WebFragmentInfo> relativeOrderWebFragments(Map<String, WebFragmentItemImpl> webFragmentItems) throws UnableToAdaptException {
        boolean isDebug = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();

        Map<String, RelativeOrderMetaData> relativeOrderMap = new TreeMap<String, RelativeOrderMetaData>();
        boolean containsRelativeOrderElement = false;
        List<WebFragmentInfo> unnamedOthers = null;

        for (Map.Entry<String, WebFragmentItemImpl> fragmentEntry : webFragmentItems.entrySet()) {
            WebFragmentItemImpl webFragmentItem = fragmentEntry.getValue();
            if (isDebug) {
                Tr.debug(tc, "webFragmentItem->[{0}]", webFragmentItem);
            }

            WebFragment webFragment = webFragmentItem.getWebFragment();

            Ordering ordering = ((webFragment != null) ? webFragment.getOrdering() : null);
            if (ordering != null) {
                if (isDebug) {
                    Tr.debug(tc, "ordering->[{0}]", ordering);
                }

                // Bug found while adding tests for issue 20421.
                // NPE when an un-named fragment has an ordering element.
                // The fragment will be treated as a named other from other
                // fragments (the assigned name can appear in no orderings).
                // The ordering constraints on the fragment, however, will
                // apply, placing other elements relative to the fragment.

                String nameCase;
                String webFragmentName = webFragment.getName();
                if (webFragmentName == null) {
                    nameCase = "assigned";
                    webFragmentName = fragmentEntry.getKey();
                } else {
                    nameCase = "original";
                }
                if (isDebug) {
                    Tr.debug(tc, "Using {0} name->[{1}]", new Object[] { nameCase, webFragmentName });
                }

                RelativeOrderMetaData relativeOrderMetaData = new RelativeOrderMetaData(ordering, webFragmentName, webFragmentItem);
                if (isDebug) {
                    Tr.debug(tc, "relativeOrderMetaData[{0}]", relativeOrderMetaData);
                }

                if (relativeOrderMetaData.isDuplicateNames()) {
                    // Duplicate name exception: When traversing the web-fragments, if multiple
                    // members with the same <name> element are encountered, the application must
                    // log an informative error message including information to help fix the
                    // problem, and must fail to deploy.

                    // TFB: This seems wrong: The stated constraint is against all of the fragments.
                    //      That is, it is an error for more than one fragment to have the same
                    //      name.  The check, here, looks for duplicate names within a relative
                    //      ordering before or after element.  Note the text "multiple members".
                    //      Duplicate names within an ordering are within the same element.

                    if (isDebug) {
                        Tr.debug(tc, "Duplicate names in relative ordering metadata [" + webFragmentName + "]");
                    }
                    String msg = Tr.formatMessage(tc,
                                                  "duplicate.names.found.in.relative.ordering.metadata.CWWKM0477E",
                                                  "Duplicate names found in relative ordering metadata", webFragmentName);
                    throw new UnableToAdaptException(msg);
                }

                if (relativeOrderMap.containsKey(webFragmentName)) {
                    if (isDebug) {
                        Tr.debug(tc, "Duplicate fragment names [" + webFragmentName + "]");
                    }
                    String msg = Tr.formatMessage(tc,
                                                  "two.fragments.have.the.same.name.CWWKM0479E",
                                                  "Two fragments have the same name", webFragmentName);
                    throw new UnableToAdaptException(msg);
                }

                relativeOrderMap.put(webFragmentName, relativeOrderMetaData);
                containsRelativeOrderElement = true;

            } else {
                // TFB: No duplicate name check is performed when there is no relative
                //      ordering element!
                //
                //      This cannot be fixed: A customer may have duplicate fragments which
                //      have no ordering information.

                String webFragmentName = ((webFragment != null) ? webFragment.getName() : null);
                if (webFragmentName != null) {
                    RelativeOrderMetaData relativeOrderMetaData = new RelativeOrderMetaData(null, webFragmentName, webFragmentItem);
                    relativeOrderMap.put(webFragmentName, relativeOrderMetaData);

                    if (isDebug) {
                        Tr.debug(tc, "no ordering, relativeOrderMetaData ->[{0}]", relativeOrderMetaData);
                    }

                } else {
                    if (unnamedOthers == null) {
                        unnamedOthers = new ArrayList<WebFragmentInfo>();
                    }
                    unnamedOthers.add(webFragmentItem);

                    if (isDebug) {
                        Tr.debug(tc, "no ordering, unnamed other ->[{0}]", webFragment);
                    }
                }
            }
        }

        //add others to the map at the end since it should be after by default
        relativeOrderMap = new LinkedHashMap<String, RelativeOrderMetaData>(relativeOrderMap);
        RelativeOrderMetaData othersROMD = RelativeOrderMetaData.getNewRelativeOthersElement();
        relativeOrderMap.put(othersROMD.getName(), othersROMD);

        // Build the relative ordering.

        List<WebFragmentInfo> relativeOrderingList = new ArrayList<>(webFragmentItems.size());

        List<RelativeOrderMetaData> relOrderMetaDataList;
        if (containsRelativeOrderElement && (relOrderMetaDataList = getRelativeOrdering(relativeOrderMap)) != null) {
            int othersPosition = -1;
            int counter = 0;
            for (RelativeOrderMetaData relOrderMetaData : relOrderMetaDataList) {
                if (relOrderMetaData.isOthers()) {
                    othersPosition = counter;
                    if (isDebug) {
                        Tr.debug(tc, "set others position to->[{0}]", othersPosition);
                    }
                } else {
                    WebFragmentInfo orderedInfo = relOrderMetaData.getTarget();
                    relativeOrderingList.add(orderedInfo);

                    if (isDebug) {
                        Tr.debug(tc, "added [{0}] to [{1}]", orderedInfo, relativeOrderingList);
                    }
                }
                relativeOrderMap.remove(relOrderMetaData.getName());
                counter++;
            }

            if (othersPosition != -1) {
                int tempOthersPosition = othersPosition;
                for (RelativeOrderMetaData relOrderMD : relativeOrderMap.values()) {
                    if (isDebug) {
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
                        if (isDebug) {
                            Tr.debug(tc, "adding unnamed other [{0}] at position [{1}]",
                                     unnamedOther, othersPosition);
                        }
                        relativeOrderingList.add(tempOthersPosition, unnamedOther);
                        tempOthersPosition++;
                    }
                }
            }

        } else {
            if (isDebug) {
                if (!containsRelativeOrderElement) {
                    Tr.debug(tc, "no relative ordering elements in the list of fragments");
                } else {
                    Tr.debug(tc, "nothing returned from relative order calculation. return original list");
                }
            }

            relativeOrderingList.addAll(webFragmentItems.values());
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
