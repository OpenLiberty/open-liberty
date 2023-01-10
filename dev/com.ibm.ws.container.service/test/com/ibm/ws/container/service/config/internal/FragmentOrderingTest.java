/*********************************************************************
* Copyright (c) 2023 IBM Corporation and others.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package com.ibm.ws.container.service.config.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

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

public class FragmentOrderingTest {
// https://github.com/jakartaee/servlet/blob/6.0.0/spec/src/main/asciidoc/servlet-spec-body.adoc
//   8.2.2. Ordering of web.xml and web-fragment.xml
//

    // Example 0:
    //
    // <web-fragment>
    //   <name>MyFragment1</name>
    //   <ordering>
    //         <after>
    //           <name>MyFragment2</name>
    //         </after>
    //   </ordering>
    // </web-fragment>
    //
    // web-fragment.xml
    //   <web-fragment>
    //         <name>MyFragment2</name>
    //   </web-fragment>
    //
    // web-fragment.xml
    //   <web-fragment>
    //         <name>MyFragment3</name>
    //         <ordering>
    //           <before>
    //             <others/>
    //           </before>
    //         </ordering>
    //  </web-fragment>
    //
    // web.xml
    //   <web-app>
    //   </web-app>
    //
    // MyFragment3
    // MyFragment2
    // MyFragment1

    private static final int VERSION_50 = 50;
    private static final String WEB_APP_VERSION_50 = "5.0";
    private static final boolean METADATA_COMPLETE = true;

    private static final boolean HAS_ABSOLUTE_ORDERING = true;

    private static final boolean SET_OTHERS = true;

    private static class MockWebApp {
        public final String version;
        public final boolean metadataComplete;

        public final Container container;
        public final WebApp webApp;
        public final AbsoluteOrdering absOrdering;
        public final WebModuleClassesInfo classesInfo;

        public MockWebApp(Mockery mockery,
                          String version, boolean metadataComplete,
                          boolean hasAbsoluteOrdering) {

            this.version = version;
            this.metadataComplete = metadataComplete;

            this.container = mockery.mock(Container.class);
            this.webApp = mockery.mock(WebApp.class);
            this.absOrdering = (hasAbsoluteOrdering ? mockery.mock(AbsoluteOrdering.class) : null);
            this.classesInfo = mockery.mock(WebModuleClassesInfo.class);
        }

        public void expectations(Expectations expectations) throws UnableToAdaptException {
            expectations.allowing(container).adapt(WebApp.class);
            expectations.will(Expectations.returnValue(webApp));

            expectations.allowing(webApp).getVersion();
            expectations.will(Expectations.returnValue(version));
            expectations.allowing(webApp).isSetMetadataComplete();
            expectations.will(Expectations.returnValue(metadataComplete));

            expectations.allowing(webApp).getAbsoluteOrdering();
            expectations.will(Expectations.returnValue(absOrdering));

            expectations.allowing(container).adapt(WebModuleClassesInfo.class);
            expectations.will(Expectations.returnValue(classesInfo));
        }

        public void orderExpectations(Expectations expectations,
                                      List<String> namesBeforeOthers,
                                      boolean setOthers,
                                      List<String> namesAfterOthers) {

            expectations.allowing(absOrdering).getNamesBeforeOthers();
            expectations.will(Expectations.returnValue(namesBeforeOthers));

            expectations.allowing(absOrdering).isSetOthers();
            expectations.will(Expectations.returnValue(setOthers));

            expectations.allowing(absOrdering).getNamesAfterOthers();
            expectations.will(Expectations.returnValue(namesAfterOthers));
        }

        public void classesExpectations(Expectations expectations, ContainerInfo... containerInfos) {
            expectations.allowing(classesInfo).getClassesContainers();

            List<ContainerInfo> infos = new ArrayList<ContainerInfo>(containerInfos.length);
            for (ContainerInfo containerInfo : containerInfos) {
                infos.add(containerInfo);
            }
            expectations.will(Expectations.returnValue(infos));
        }
    }

    private static final boolean HAS_ORDERING = true;

    public static final boolean SET_AFTER = true;
    public static final boolean SET_AFTER_OTHERS = true;

    public static final boolean SET_BEFORE = true;
    public static final boolean SET_BEFORE_OTHERS = true;

    private static class MockFragment {
        public final String name;
        public final String uri;
        public final boolean metadataComplete;
        public final ContainerInfo info;
        public final Container container;
        public final WebFragment fragment;
        public final Ordering ordering;

        public MockFragment(Mockery mockery, String name, String uri, boolean metadataComplete, boolean hasOrdering, String tag) {
            // The URI cannot be null; the name can be null.

            this.name = name;
            this.uri = uri;
            this.metadataComplete = metadataComplete;
            this.info = mockery.mock(ContainerInfo.class, "Info" + tag);
            this.container = mockery.mock(Container.class, "Container" + tag);
            this.fragment = mockery.mock(WebFragment.class, "WebFragment" + tag);
            this.ordering = (hasOrdering ? mockery.mock(Ordering.class, "Ordering" + tag) : null);
        }

        private void expectations(Expectations expectations) throws UnableToAdaptException {
            expectations.allowing(info).getContainer();
            expectations.will(Expectations.returnValue(container));

            expectations.allowing(info).getName();
            expectations.will(Expectations.returnValue(uri));

            expectations.allowing(info).getType();
            expectations.will(Expectations.returnValue(Type.WEB_INF_LIB));

            expectations.allowing(container).adapt(WebFragment.class);
            expectations.will(Expectations.returnValue(fragment));

            expectations.allowing(fragment).getName();
            expectations.will(Expectations.returnValue(name));
            expectations.allowing(fragment).isMetadataComplete();
            expectations.will(Expectations.returnValue(metadataComplete));
            expectations.allowing(fragment).getOrdering();
            expectations.will(Expectations.returnValue(ordering));
        }

        private void orderExpectations(Expectations expectations,
                                       boolean setAfter, List<String> afterNames, boolean setAfterOthers,
                                       boolean setBefore, List<String> beforeNames, boolean setBeforeOthers) {

            expectations.allowing(ordering).isSetAfter();
            expectations.will(Expectations.returnValue(setAfter));

            expectations.allowing(ordering).getAfterNames();
            expectations.will(Expectations.returnValue(afterNames));

            expectations.allowing(ordering).isSetAfterOthers();
            expectations.will(Expectations.returnValue(setAfterOthers));

            expectations.allowing(ordering).isSetBefore();
            expectations.will(Expectations.returnValue(setBefore));

            expectations.allowing(ordering).getBeforeNames();
            expectations.will(Expectations.returnValue(beforeNames));

            expectations.allowing(ordering).isSetBeforeOthers();
            expectations.will(Expectations.returnValue(setBeforeOthers));
        }
    }

    @Test
    public void testFragmentOrderingExample0a() throws Exception {
        String title = "Example 0a";
        System.out.println("Testing [ " + title + " ]");

        Mockery mockery = new Mockery();

        MockWebApp mockWebApp = new MockWebApp(mockery, WEB_APP_VERSION_50, !METADATA_COMPLETE, !HAS_ABSOLUTE_ORDERING);

        String fragment1_name = "MyFragment1";
        String fragment2_name = "MyFragment2";
        String fragment3_name = "MyFragment3";

        MockFragment mockFragment1 = new MockFragment(mockery, fragment1_name, fragment1_name, !METADATA_COMPLETE, HAS_ORDERING, "1");
        MockFragment mockFragment2 = new MockFragment(mockery, fragment2_name, fragment2_name, !METADATA_COMPLETE, !HAS_ORDERING, "2");
        MockFragment mockFragment3 = new MockFragment(mockery, fragment3_name, fragment3_name, !METADATA_COMPLETE, HAS_ORDERING, "3");

        mockery.checking(new Expectations() {
            {
                mockWebApp.expectations(this);

                mockFragment1.expectations(this);
                mockFragment1.orderExpectations(this,
                                                SET_AFTER, names(fragment2_name), !SET_AFTER_OTHERS,
                                                !SET_BEFORE, names(), !SET_BEFORE_OTHERS);

                mockFragment2.expectations(this);

                mockFragment3.expectations(this);
                mockFragment3.orderExpectations(this,
                                                !SET_AFTER, names(), !SET_AFTER_OTHERS,
                                                SET_BEFORE, names(), SET_BEFORE_OTHERS);

                mockWebApp.classesExpectations(this, mockFragment1.info, mockFragment2.info, mockFragment3.info);
            }
        });

        WebFragmentsInfo fragmentsInfo = new WebFragmentsInfoImpl(mockWebApp.container, VERSION_50);

        List<WebFragmentInfo> orderedFragments = fragmentsInfo.getOrderedFragments();
        displayFragments(title, orderedFragments);
        assertEquals("Unexpected size", 3, orderedFragments.size());

        // 2 -> 1
        // 3 -> Others

        // 3, 2, 1

        WebFragmentInfo fragmentInfo1 = orderedFragments.get(0);
        assertEquals("Incorrect first fragment", fragment3_name, fragmentInfo1.getWebFragment().getName());

        WebFragmentInfo fragmentInfo2 = orderedFragments.get(1);
        assertEquals("Incorrect second fragment", fragment2_name, fragmentInfo2.getWebFragment().getName());

        WebFragmentInfo fragmentInfo3 = orderedFragments.get(2);
        assertEquals("Incorrect third fragment", fragment1_name, fragmentInfo3.getWebFragment().getName());

        System.out.println("Testing [ " + title + " ]: Passed");
    }

    // Continuing the example:
    //
    // web.xml
    //
    // <web-app>
    //   <absolute-ordering>
    //     <name>MyFragment3</name>
    //     <name>MyFragment2</name>
    //   </absolute-ordering>
    // </web-app>
    //
    // MyFragment3
    // MyFragment2

    @Test
    public void testFragmentOrderingExample0b() throws Exception {
        String title = "Example 0b";
        System.out.println("Testing [ " + title + " ]");

        Mockery mockery = new Mockery();

        MockWebApp mockWebApp = new MockWebApp(mockery, WEB_APP_VERSION_50, !METADATA_COMPLETE, HAS_ABSOLUTE_ORDERING);

        String fragment1_name = "MyFragment1";
        String fragment2_name = "MyFragment2";
        String fragment3_name = "MyFragment3";

        MockFragment mockFragment1 = new MockFragment(mockery, fragment1_name, fragment1_name, !METADATA_COMPLETE, HAS_ORDERING, "1");
        MockFragment mockFragment2 = new MockFragment(mockery, fragment2_name, fragment2_name, !METADATA_COMPLETE, !HAS_ORDERING, "2");
        MockFragment mockFragment3 = new MockFragment(mockery, fragment3_name, fragment3_name, !METADATA_COMPLETE, HAS_ORDERING, "3");

        mockery.checking(new Expectations() {
            {
                mockWebApp.expectations(this);
                mockWebApp.orderExpectations(this,
                                             names(fragment2_name, fragment3_name),
                                             !SET_OTHERS,
                                             names());

                mockFragment1.expectations(this);
                mockFragment1.orderExpectations(this,
                                                SET_AFTER, names(fragment2_name), !SET_AFTER_OTHERS,
                                                !SET_BEFORE, names(), !SET_BEFORE_OTHERS);

                mockFragment2.expectations(this);

                mockFragment3.expectations(this);
                mockFragment3.orderExpectations(this,
                                                !SET_AFTER, names(), !SET_AFTER_OTHERS,
                                                SET_BEFORE, names(), SET_BEFORE_OTHERS);

                mockWebApp.classesExpectations(this, mockFragment1.info, mockFragment2.info, mockFragment3.info);
            }
        });

        WebFragmentsInfo fragmentsInfo = new WebFragmentsInfoImpl(mockWebApp.container, VERSION_50);

        List<WebFragmentInfo> orderedFragments = fragmentsInfo.getOrderedFragments();
        displayFragments(title, orderedFragments);
        assertEquals("Unexpected size", 2, orderedFragments.size());

        WebFragmentInfo fragmentInfo1 = orderedFragments.get(0);
        assertEquals("Incorrect first fragment", fragment2_name, fragmentInfo1.getWebFragment().getName());

        WebFragmentInfo fragmentInfo2 = orderedFragments.get(1);
        assertEquals("Incorrect second fragment", fragment3_name, fragmentInfo2.getWebFragment().getName());

        System.out.println("Testing [ " + title + " ]: Passed");
    }

    // Example 1
    //
    // Document A:
    // <after>
    //   <others/>
    //   <name>C</name>
    // </after>
    //
    // Document B:
    // <before>
    //   <others/>
    // </before>
    //
    // Document C:
    // <after>
    //   <others/>
    // </after>
    //
    // Document D:
    // no ordering
    //
    // Document E:
    // no ordering
    //
    // Document F:
    // <before>
    //   <others/>
    //   <name>B</name>
    // </before>
    //
    // F, B, D, E, C, A.
    // F, B, E, D, C, A.

    @Test
    public void testFragmentOrderingExample1() throws Exception {
        String title = "Example 1";
        System.out.println("Testing [ " + title + " ]");

        Mockery mockery = new Mockery();

        MockWebApp mockWebApp = new MockWebApp(mockery, WEB_APP_VERSION_50, !METADATA_COMPLETE, !HAS_ABSOLUTE_ORDERING);

        String fragmentA_name = "A";
        String fragmentB_name = "B";
        String fragmentC_name = "C";
        String fragmentD_name = "D";
        String fragmentE_name = "E";
        String fragmentF_name = "F";

        MockFragment mockFragmentA = new MockFragment(mockery, fragmentA_name, fragmentA_name, !METADATA_COMPLETE, HAS_ORDERING, "A");
        MockFragment mockFragmentB = new MockFragment(mockery, fragmentB_name, fragmentB_name, !METADATA_COMPLETE, HAS_ORDERING, "B");
        MockFragment mockFragmentC = new MockFragment(mockery, fragmentC_name, fragmentC_name, !METADATA_COMPLETE, HAS_ORDERING, "C");
        MockFragment mockFragmentD = new MockFragment(mockery, fragmentD_name, fragmentD_name, !METADATA_COMPLETE, !HAS_ORDERING, "D");
        MockFragment mockFragmentE = new MockFragment(mockery, fragmentE_name, fragmentE_name, !METADATA_COMPLETE, !HAS_ORDERING, "E");
        MockFragment mockFragmentF = new MockFragment(mockery, fragmentF_name, fragmentF_name, !METADATA_COMPLETE, HAS_ORDERING, "F");

        mockery.checking(new Expectations() {
            {
                mockWebApp.expectations(this);

                mockFragmentA.expectations(this);
                mockFragmentA.orderExpectations(this,
                                                SET_AFTER, names(fragmentC_name), SET_AFTER_OTHERS,
                                                !SET_BEFORE, names(), !SET_BEFORE_OTHERS);

                mockFragmentB.expectations(this);
                mockFragmentB.orderExpectations(this,
                                                !SET_AFTER, names(), !SET_AFTER_OTHERS,
                                                SET_BEFORE, names(), SET_BEFORE_OTHERS);

                mockFragmentC.expectations(this);
                mockFragmentC.orderExpectations(this,
                                                SET_AFTER, names(), SET_AFTER_OTHERS,
                                                !SET_BEFORE, names(), !SET_BEFORE_OTHERS);

                mockFragmentD.expectations(this);
                mockFragmentE.expectations(this);

                mockFragmentF.expectations(this);
                mockFragmentF.orderExpectations(this,
                                                !SET_AFTER, names(), !SET_AFTER_OTHERS,
                                                SET_BEFORE, names(fragmentB_name), SET_BEFORE_OTHERS);

                mockWebApp.classesExpectations(this,
                                               mockFragmentA.info, mockFragmentB.info, mockFragmentC.info,
                                               mockFragmentD.info, mockFragmentE.info, mockFragmentF.info);
            }
        });

        WebFragmentsInfo fragmentsInfo = new WebFragmentsInfoImpl(mockWebApp.container, VERSION_50);

        List<WebFragmentInfo> orderedFragments = fragmentsInfo.getOrderedFragments();
        displayFragments(title, orderedFragments);
        assertEquals("Unexpected size", 6, orderedFragments.size());

        // F -> B, Others
        // B -> Others
        // Others -> C
        // Others, C -> A
        // D, E

        // F, B, D, E, C, A.
        // F, B, E, D, C, A.

        List<String> allowed0 = names(fragmentF_name, fragmentB_name, fragmentD_name,
                                      fragmentE_name, fragmentC_name, fragmentA_name);
        List<String> allowed1 = names(fragmentF_name, fragmentB_name, fragmentE_name,
                                      fragmentD_name, fragmentC_name, fragmentA_name);

        boolean hasAllowedOrdering = (matches(orderedFragments, allowed0) || matches(orderedFragments, allowed1));
        assertTrue("Disallowed ordering", hasAllowedOrdering);

        System.out.println("Testing [ " + title + " ]: Passed");
    }

    // Example 2:
    //
    //  Document no id:
    //        <after>
    //          <others/>
    //        </after>
    //        <before>
    //          <name>C</name>
    //        </before>
    //
    //  Document B:
    //        <before>
    //          <others/>
    //        </before>
    //
    //  Document C:
    //        no ordering
    //
    //  Document D:
    //        <after>
    //          <others/>
    //        </after>
    //
    //  Document E:
    //        <before>
    //          <others/>
    //        </before>
    //
    //  Document F:
    //        *no ordering*
    //
    //  B, E, F, <no id>, C, D
    //  B, E, F, <no id>, D, C
    //  E, B, F, <no id>, C, D
    //  E, B, F, <no id>, D, C
    //  E, B, F, D, <no id>, C
    //  E, B, F, D, <no id>, C

    @Test
    public void testFragmentOrderingExample2() throws Exception {
        String title = "Example 2";
        System.out.println("Testing [ " + title + " ]");

        Mockery mockery = new Mockery();

        MockWebApp mockWebApp = new MockWebApp(mockery, WEB_APP_VERSION_50, !METADATA_COMPLETE, !HAS_ABSOLUTE_ORDERING);

        String fragment0_name = null;
        String fragment0_uri = "A";
        String fragmentB_name = "B";
        String fragmentC_name = "C";
        String fragmentD_name = "D";
        String fragmentE_name = "E";
        String fragmentF_name = "F";

        MockFragment mockFragment0 = new MockFragment(mockery, fragment0_name, fragment0_uri, !METADATA_COMPLETE, HAS_ORDERING, "0");
        MockFragment mockFragmentB = new MockFragment(mockery, fragmentB_name, fragmentB_name, !METADATA_COMPLETE, HAS_ORDERING, "B");
        MockFragment mockFragmentC = new MockFragment(mockery, fragmentC_name, fragmentC_name, !METADATA_COMPLETE, !HAS_ORDERING, "C");
        MockFragment mockFragmentD = new MockFragment(mockery, fragmentD_name, fragmentD_name, !METADATA_COMPLETE, HAS_ORDERING, "D");
        MockFragment mockFragmentE = new MockFragment(mockery, fragmentE_name, fragmentE_name, !METADATA_COMPLETE, HAS_ORDERING, "E");
        MockFragment mockFragmentF = new MockFragment(mockery, fragmentF_name, fragmentF_name, !METADATA_COMPLETE, !HAS_ORDERING, "F");

        mockery.checking(new Expectations() {
            {
                mockWebApp.expectations(this);

                mockFragment0.expectations(this);
                mockFragment0.orderExpectations(this,
                                                SET_AFTER, names(), SET_AFTER_OTHERS,
                                                SET_BEFORE, names(fragmentC_name), !SET_BEFORE_OTHERS);

                mockFragmentB.expectations(this);
                mockFragmentB.orderExpectations(this,
                                                !SET_AFTER, names(), !SET_AFTER_OTHERS,
                                                SET_BEFORE, names(), SET_BEFORE_OTHERS);

                mockFragmentC.expectations(this);

                mockFragmentD.expectations(this);
                mockFragmentD.orderExpectations(this,
                                                SET_AFTER, names(), SET_AFTER_OTHERS,
                                                !SET_BEFORE, names(), !SET_BEFORE_OTHERS);

                mockFragmentE.expectations(this);
                mockFragmentE.orderExpectations(this,
                                                !SET_AFTER, names(), !SET_AFTER_OTHERS,
                                                SET_BEFORE, names(), SET_BEFORE_OTHERS);

                mockFragmentF.expectations(this);

                mockWebApp.classesExpectations(this,
                                               mockFragment0.info, mockFragmentB.info, mockFragmentC.info,
                                               mockFragmentD.info, mockFragmentE.info, mockFragmentF.info);
            }
        });

        WebFragmentsInfo fragmentsInfo = new WebFragmentsInfoImpl(mockWebApp.container, VERSION_50);

        List<WebFragmentInfo> orderedFragments = fragmentsInfo.getOrderedFragments();
        displayFragments(title, orderedFragments);
        assertEquals("Unexpected size", 6, orderedFragments.size());

        // Others -> 0 -> C
        // B -> Others
        // Others -> D
        // E -> Others
        // F

        // E, B, F, 0, C, D
        // B, E, F, 0, C, D
        //
        // E, B, F, 0, D, C
        // B, E, F, 0, D, C
        //
        // E, B, F, D, 0, C
        // B, E, F, D, 0, C

        List<String> allowed0 = names(fragmentE_name, fragmentB_name, fragmentF_name,
                                      fragment0_name, fragmentC_name, fragmentD_name);
        List<String> allowed1 = names(fragmentB_name, fragmentE_name, fragmentF_name,
                                      fragment0_name, fragmentC_name, fragmentD_name);

        List<String> allowed2 = names(fragmentE_name, fragmentB_name, fragmentF_name,
                                      fragment0_name, fragmentD_name, fragmentC_name);
        List<String> allowed3 = names(fragmentB_name, fragmentE_name, fragmentF_name,
                                      fragment0_name, fragmentD_name, fragmentC_name);

        List<String> allowed4 = names(fragmentE_name, fragmentB_name, fragmentF_name,
                                      fragmentD_name, fragment0_name, fragmentC_name);
        List<String> allowed5 = names(fragmentB_name, fragmentE_name, fragmentF_name,
                                      fragmentD_name, fragment0_name, fragmentC_name);

        boolean hasAllowedOrdering = (matches(orderedFragments, allowed1) || matches(orderedFragments, allowed3) ||
                                      matches(orderedFragments, allowed0) || matches(orderedFragments, allowed2) ||
                                      matches(orderedFragments, allowed4) || matches(orderedFragments, allowed5));
        assertTrue("Disallowed ordering", hasAllowedOrdering);

        System.out.println("Testing [ " + title + " ]: Passed");
    }

    private void displayFragments(String title, List<WebFragmentInfo> orderedInfo) {
        System.out.println(title);
        for (WebFragmentInfo info : orderedInfo) {
            System.out.println("  [ " + info.getWebFragment().getName() + " ] at [ " + info.getLibraryURI() + " ]");
        }
    }

    // Example 3
    //
    // Document A:
    // <after>
    //   <name>B</name>
    // </after>
    //
    // Document B:
    // no ordering
    //
    // Document C:
    // <before>
    //   <others/>
    // </before>
    //
    // Document D:
    // no ordering
    //
    // C, B, D, A
    // C, D, B, A
    // C, B, A, D

    @Test
    public void testFragmentOrderingExample3() throws Exception {
        String title = "Example 3";
        System.out.println("Testing [ " + title + " ]");

        Mockery mockery = new Mockery();

        MockWebApp mockWebApp = new MockWebApp(mockery, WEB_APP_VERSION_50, !METADATA_COMPLETE, !HAS_ABSOLUTE_ORDERING);

        String fragmentA_name = "A";
        String fragmentB_name = "B";
        String fragmentC_name = "C";
        String fragmentD_name = "D";

        MockFragment mockFragmentA = new MockFragment(mockery, fragmentA_name, fragmentA_name, !METADATA_COMPLETE, HAS_ORDERING, "A");
        MockFragment mockFragmentB = new MockFragment(mockery, fragmentB_name, fragmentB_name, !METADATA_COMPLETE, !HAS_ORDERING, "B");
        MockFragment mockFragmentC = new MockFragment(mockery, fragmentC_name, fragmentC_name, !METADATA_COMPLETE, HAS_ORDERING, "C");
        MockFragment mockFragmentD = new MockFragment(mockery, fragmentD_name, fragmentD_name, !METADATA_COMPLETE, !HAS_ORDERING, "D");

        mockery.checking(new Expectations() {
            {
                mockWebApp.expectations(this);

                mockFragmentA.expectations(this);
                mockFragmentA.orderExpectations(this,
                                                SET_AFTER, names(fragmentB_name), !SET_AFTER_OTHERS,
                                                !SET_BEFORE, names(), !SET_BEFORE_OTHERS);

                mockFragmentB.expectations(this);

                mockFragmentC.expectations(this);
                mockFragmentC.orderExpectations(this,
                                                !SET_AFTER, names(), !SET_AFTER_OTHERS,
                                                SET_BEFORE, names(), SET_BEFORE_OTHERS);

                mockFragmentD.expectations(this);

                mockWebApp.classesExpectations(this,
                                               mockFragmentA.info, mockFragmentB.info, mockFragmentC.info, mockFragmentD.info);
            }
        });

        WebFragmentsInfo fragmentsInfo = new WebFragmentsInfoImpl(mockWebApp.container, VERSION_50);

        List<WebFragmentInfo> orderedFragments = fragmentsInfo.getOrderedFragments();
        displayFragments(title, orderedFragments);
        assertEquals("Unexpected size", 4, orderedFragments.size());

        // B -> A
        // C -> Others
        //
        // C, B, D, A
        // C, D, B, A
        // C, B, A, D

        List<String> allowed0 = names(fragmentC_name, fragmentB_name, fragmentD_name, fragmentA_name);
        List<String> allowed1 = names(fragmentC_name, fragmentD_name, fragmentB_name, fragmentA_name);
        List<String> allowed2 = names(fragmentC_name, fragmentB_name, fragmentA_name, fragmentD_name);

        boolean hasAllowedOrdering = (matches(orderedFragments, allowed0) ||
                                      matches(orderedFragments, allowed1) ||
                                      matches(orderedFragments, allowed2));
        assertTrue("Disallowed ordering", hasAllowedOrdering);

        System.out.println("Testing [ " + title + " ]: Passed");
    }

    private static List<String> names(String... names) {
        if (names.length == 0) {
            return Collections.emptyList();
        } else if (names.length == 1) {
            return Collections.singletonList(names[0]);
        } else {
            List<String> returnNames = new ArrayList<>(names.length);
            for (String name : names) {
                returnNames.add(name);
            }
            return returnNames;
        }
    }

    private boolean matches(List<WebFragmentInfo> actual, List<String> expected) {
        int numFragments = expected.size();

        for (int fragmentNo = 0; fragmentNo < numFragments; fragmentNo++) {
            String actualName = actual.get(fragmentNo).getWebFragment().getName();
            String expectedName = expected.get(fragmentNo);

            if (((expectedName == null) && (actualName != null)) ||
                (expectedName != null) && (!expectedName.equals(actualName))) {
                return false;
            }
        }

        return true;
    }
}
