/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

package com.ibm.ws.query.utils;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

import javax.persistence.EntityManager;

import com.ibm.ws.query.entities.interfaces.IAddressBean;
import com.ibm.ws.query.entities.interfaces.ICharityFund;
import com.ibm.ws.query.entities.interfaces.ICustomerBean;
import com.ibm.ws.query.entities.interfaces.IDeptBean;
import com.ibm.ws.query.entities.interfaces.IEmpBean;
import com.ibm.ws.query.entities.interfaces.ILineItem;
import com.ibm.ws.query.entities.interfaces.IOrderBean;
import com.ibm.ws.query.entities.interfaces.IPart;
import com.ibm.ws.query.entities.interfaces.IPartBase;
import com.ibm.ws.query.entities.interfaces.IPartComposite;
import com.ibm.ws.query.entities.interfaces.IPersonBean;
import com.ibm.ws.query.entities.interfaces.IProduct;
import com.ibm.ws.query.entities.interfaces.IProjectBean;
import com.ibm.ws.query.entities.interfaces.ISupplier;
import com.ibm.ws.query.entities.interfaces.ITaskBean;
import com.ibm.ws.query.entities.interfaces.ITypeTestBean;
import com.ibm.ws.query.entities.interfaces.IUsage;
import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;

/**
 *
 */
public class SetupQueryTestCase extends AbstractTestLogic {
    protected EntityManager _em = null;
    protected JPAPersistenceProvider _pvdr = null;

    protected static boolean isXmlOrMap = false;

    protected static String _dbVendorName = null;
    protected static String _schemaName = "";
    protected static double DOUBLE_DB_MAX_VALUE = 1;
    protected static double DOUBLE_DB_MIN_VALUE = 1;
    protected static float FLOAT_DB_MAX_VALUE = 1;
    protected static float FLOAT_DB_MIN_VALUE = 1;
    protected static int INT_DB_MAX_VALUE = 1;
    protected static int INT_DB_MIN_VALUE = 1;
    protected static long LONG_DB_MAX_VALUE = 1;
    protected static long LONG_DB_MIN_VALUE = 1;

    protected static double INITIAL_DOUBLE_DB_MAX_VALUE = 1;
    protected static double INITIAL_DOUBLE_DB_MIN_VALUE = 1;
    protected static float INITIAL_FLOAT_DB_MAX_VALUE = 1;
    protected static float INITIAL_FLOAT_DB_MIN_VALUE = 1;
    protected static int INITIAL_INT_DB_MAX_VALUE = 1;
    protected static int INITIAL_INT_DB_MIN_VALUE = 1;
    protected static long INITIAL_LONG_DB_MAX_VALUE = 1;
    protected static long INITIAL_LONG_DB_MIN_VALUE = 1;

    static boolean debug = true;
    static int noErrors = 0;
    static int noQuery = 0;
    protected static Calendar currentCal = Calendar.getInstance();
    protected static long currMillis = currentCal.getTimeInMillis();
    protected static int currNanos = 0;
    protected static long diff = 0L;
    protected static Timestamp currentTs = null;
    protected static String currentTsString = null;
    protected static long nativeQueryCurrMillis = 0L;

    IDeptBean d1 = null;
    IDeptBean d2 = null;
    IDeptBean d3 = null;
    IDeptBean d4 = null;
    IDeptBean d5 = null;

    IAddressBean a1 = null;
    IAddressBean a2 = null;
    IAddressBean a3 = null;
    IAddressBean a4 = null;
    IAddressBean a5 = null;
    IAddressBean a6 = null;
    IAddressBean a7 = null;
    IAddressBean a8 = null;
    IAddressBean a9 = null;

    IEmpBean e1 = null;
    IEmpBean e2 = null;
    IEmpBean e3 = null;
    IEmpBean e4 = null;
    IEmpBean e5 = null;
    IEmpBean e6 = null;
    IEmpBean e7 = null;
    IEmpBean e8 = null;
    IEmpBean e9 = null;
    IEmpBean e10 = null;

    IProjectBean pj1 = null;
    IProjectBean pj2 = null;
    IProjectBean pj3 = null;

    ITaskBean t1 = null;
    ITaskBean t2 = null;
    ITaskBean t3 = null;
    ITaskBean t4 = null;
    ITaskBean t5 = null;

    ICharityFund cf1 = null;
    ICharityFund cf2 = null;

    ISupplier s1 = null;
    ISupplier s2 = null;

    IPartBase p1 = null;
    IPartBase p2 = null;
    IPartBase p3 = null;

    IPartComposite p4 = null;
    IPartComposite p5 = null;
    IPartComposite p6 = null;

    ICustomerBean c1 = null;
    ICustomerBean c2 = null;

    IProduct pr1 = null;

    IOrderBean o1 = null;
    IOrderBean o2 = null;

    ILineItem l1 = null;
    ILineItem l2 = null;

    IPersonBean original = null;

    ITypeTestBean typeTest1 = null;
    ITypeTestBean typeTest2 = null;
    ITypeTestBean typeTest3 = null;
    ITypeTestBean typeTest4 = null;
    ITypeTestBean typeTest5 = null;
    ITypeTestBean typeTest6 = null;
    ITypeTestBean typeTest7 = null;
    ITypeTestBean typeTest8 = null;
    ITypeTestBean typeTest9 = null;
    // only used to set min/max values
    ITypeTestBean typeTestA = null;

    public SetupQueryTestCase(EntityManager em, String dbVendorName, boolean ano) {
        _em = em;
        _pvdr = JPAPersistenceProvider.resolveJPAPersistenceProvider(em);
        _dbVendorName = dbVendorName;
        isXmlOrMap = !ano;
    }

    public void setUp(String app) throws Exception {
        System.out.println("starting SetupQueryTestCase - setup(" + app + ")");

//        _tu = TestUtils.getInstance();
//        System.out.println("_tu.getMapTextContext()" + _tu.getMapTextContext());
//        if (_tu.getMapTextContext().toLowerCase().contains("ano")) {
//            setXmlOrMap(false);
//            System.out.println("setXmlOrMap(false)");
//        } else {
//            setXmlOrMap(true);
//            System.out.println("setXmlOrMap(true)");
//        }

//        _dbVendorName = getDbVendorNameFromPersistenceXml();
//        System.out.println("_dbVendorName = " + _dbVendorName);
//        System.out.println("isXmlOrMap = " + isXmlOrMap);
//        validateDbVendorName(_dbVendorName);

        if (app.equals("empdept")) {
            setUpPersonBean();
            setUpEmpDept();
        } else if (app.equals("part-composite")) {
            setUpPartComposite();
        } else if (app.equals("PersonBean")) {
            setUpPersonBean();
//        } else if (app.equals("typetest")) {
//            setUpTypetest();
//        } else if (app.startsWith("Jse")) {
//            setUpPdqTest();
//        } else if (app.startsWith("Jee")) {
//            setUpPdqTest();
        } else {
            throw new Exception("no setup available for app " + app);
        }
    }

    private void createPOJOs() throws Exception {
        System.out.println("starting createPOJOs()");
        if (isXmlOrMap) {
            System.out.println("createPOJOs for XML");
            d1 = new com.ibm.ws.query.entities.xml.XMLDeptBean(210, "Development");
            d2 = new com.ibm.ws.query.entities.xml.XMLDeptBean(220, "Service");
            d3 = new com.ibm.ws.query.entities.xml.XMLDeptBean(300, "Sales");
            d4 = new com.ibm.ws.query.entities.xml.XMLDeptBean(200, "Admin");
            d5 = new com.ibm.ws.query.entities.xml.XMLDeptBean(100, "CEO");

            a1 = new com.ibm.ws.query.entities.xml.XMLAddressBean("1780 Mercury Way", "Morgan Hill", "CA", "95037");
            a2 = new com.ibm.ws.query.entities.xml.XMLAddressBean("512 Venus Drive", "Morgan Hill", "CA", "95037");
            a3 = new com.ibm.ws.query.entities.xml.XMLAddressBean("12440 Vulcan Avenue", "Los Altos", "CA", "95130");
            a4 = new com.ibm.ws.query.entities.xml.XMLAddressBean("4983 Plutonium Avenue", "Palo Alto", "CA", "95140");
            a5 = new com.ibm.ws.query.entities.xml.XMLAddressBean("182 Martian Street", "San Jose", "CA", "95120");
            a6 = new com.ibm.ws.query.entities.xml.XMLAddressBean("555 Silicon Valley Drive", "San Jose", "CA", "94120");
            a7 = new com.ibm.ws.query.entities.xml.XMLAddressBean("6200 Vegas Drive", "San Jose", "CA", "95120");
            a8 = new com.ibm.ws.query.entities.xml.XMLAddressBean("150 North First Apt E1", "San Jose", "CA", "94003");
            a9 = new com.ibm.ws.query.entities.xml.XMLAddressBean("8900 Jupiter Park", "San Jose", "CA", "94005");

            // used when no sequence generation for empbean
            e1 = new com.ibm.ws.query.entities.xml.XMLEmpBean(1, "david", 12.1, (com.ibm.ws.query.entities.xml.XMLDeptBean) d1, 1, false, a1, a6);
            e2 = new com.ibm.ws.query.entities.xml.XMLEmpBean(2, "andrew", 13.1, (com.ibm.ws.query.entities.xml.XMLDeptBean) d1, 1, false, a1, a6);
            e3 = new com.ibm.ws.query.entities.xml.XMLEmpBean(3, "minmei", 15.5, (com.ibm.ws.query.entities.xml.XMLDeptBean) d4, 2, true, a1, a6);
            e4 = new com.ibm.ws.query.entities.xml.XMLEmpBean(4, "george", 0, (com.ibm.ws.query.entities.xml.XMLDeptBean) d4, 2, true, a2, a6);
            e5 = new com.ibm.ws.query.entities.xml.XMLEmpBean(5, "ritika", 0, (com.ibm.ws.query.entities.xml.XMLDeptBean) d2, 3, true, a3, a6);
            e6 = new com.ibm.ws.query.entities.xml.XMLEmpBean(6, "ahmad", 0, (com.ibm.ws.query.entities.xml.XMLDeptBean) d5, 3, true, a4, a4);
            e7 = new com.ibm.ws.query.entities.xml.XMLEmpBean(7, "charlene", 0, (com.ibm.ws.query.entities.xml.XMLDeptBean) d1, 4, true, a5, a6);
            e8 = new com.ibm.ws.query.entities.xml.XMLEmpBean(8, "Tom Rayburn", 0, (com.ibm.ws.query.entities.xml.XMLDeptBean) d5, 4, true, a7, a6);
            e9 = new com.ibm.ws.query.entities.xml.XMLEmpBean(9, "harry", 0, (com.ibm.ws.query.entities.xml.XMLDeptBean) d1, 5, true, a8, a9);
            e10 = new com.ibm.ws.query.entities.xml.XMLEmpBean(10, "Catalina Wei", 0, null, 5, true, null, a6);

            pj1 = new com.ibm.ws.query.entities.xml.XMLProjectBean(1000, "WebSphere Version 1", (byte) 10, (short) 20, 50, (com.ibm.ws.query.entities.xml.XMLDeptBean) d1);
            pj2 = new com.ibm.ws.query.entities.xml.XMLProjectBean(2000, "WebSphere Feature Pack", (byte) 40, (short) 10, 100, (com.ibm.ws.query.entities.xml.XMLDeptBean) d2);
            pj3 = new com.ibm.ws.query.entities.xml.XMLProjectBean(3000, "WebSphere Community Edition", (byte) 45, (short) 10, 100, null);

            t1 = new com.ibm.ws.query.entities.xml.XMLTaskBean(1010, "Design ", "Design ", (com.ibm.ws.query.entities.xml.XMLProjectBean) pj1);
            t2 = new com.ibm.ws.query.entities.xml.XMLTaskBean(1020, "Code", "Code", (com.ibm.ws.query.entities.xml.XMLProjectBean) pj1);
            t3 = new com.ibm.ws.query.entities.xml.XMLTaskBean(1030, "Test", "Test", (com.ibm.ws.query.entities.xml.XMLProjectBean) pj1);
            t4 = new com.ibm.ws.query.entities.xml.XMLTaskBean(2010, "Design", "Design", (com.ibm.ws.query.entities.xml.XMLProjectBean) pj2);
            t5 = new com.ibm.ws.query.entities.xml.XMLTaskBean(2020, "Code, Test", "Code, Test", (com.ibm.ws.query.entities.xml.XMLProjectBean) pj2);

            cf1 = new com.ibm.ws.query.entities.xml.XMLCharityFund("WorldWildlifeFund", 1000.);
            cf2 = new com.ibm.ws.query.entities.xml.XMLCharityFund("GlobalWarmingFund", 2000.);

        } else {
            System.out.println("createPOJOs for ANO");
            d1 = new com.ibm.ws.query.entities.ano.DeptBean(210, "Development");
            d2 = new com.ibm.ws.query.entities.ano.DeptBean(220, "Service");
            d3 = new com.ibm.ws.query.entities.ano.DeptBean(300, "Sales");
            d4 = new com.ibm.ws.query.entities.ano.DeptBean(200, "Admin");
            d5 = new com.ibm.ws.query.entities.ano.DeptBean(100, "CEO");

            a1 = new com.ibm.ws.query.entities.ano.AddressBean("1780 Mercury Way", "Morgan Hill", "CA", "95037");
            a2 = new com.ibm.ws.query.entities.ano.AddressBean("512 Venus Drive", "Morgan Hill", "CA", "95037");
            a3 = new com.ibm.ws.query.entities.ano.AddressBean("12440 Vulcan Avenue", "Los Altos", "CA", "95130");
            a4 = new com.ibm.ws.query.entities.ano.AddressBean("4983 Plutonium Avenue", "Palo Alto", "CA", "95140");
            a5 = new com.ibm.ws.query.entities.ano.AddressBean("182 Martian Street", "San Jose", "CA", "95120");
            a6 = new com.ibm.ws.query.entities.ano.AddressBean("555 Silicon Valley Drive", "San Jose", "CA", "94120");
            a7 = new com.ibm.ws.query.entities.ano.AddressBean("6200 Vegas Drive", "San Jose", "CA", "95120");
            a8 = new com.ibm.ws.query.entities.ano.AddressBean("150 North First Apt E1", "San Jose", "CA", "94003");
            a9 = new com.ibm.ws.query.entities.ano.AddressBean("8900 Jupiter Park", "San Jose", "CA", "94005");

            // used when no sequence generation for empbean
            e1 = new com.ibm.ws.query.entities.ano.EmpBean(1, "david", 12.1, (com.ibm.ws.query.entities.ano.DeptBean) d1, 1, false, a1, a6);
            e2 = new com.ibm.ws.query.entities.ano.EmpBean(2, "andrew", 13.1, (com.ibm.ws.query.entities.ano.DeptBean) d1, 1, false, a1, a6);
            e3 = new com.ibm.ws.query.entities.ano.EmpBean(3, "minmei", 15.5, (com.ibm.ws.query.entities.ano.DeptBean) d4, 2, true, a1, a6);
            e4 = new com.ibm.ws.query.entities.ano.EmpBean(4, "george", 0, (com.ibm.ws.query.entities.ano.DeptBean) d4, 2, true, a2, a6);
            e5 = new com.ibm.ws.query.entities.ano.EmpBean(5, "ritika", 0, (com.ibm.ws.query.entities.ano.DeptBean) d2, 3, true, a3, a6);
            e6 = new com.ibm.ws.query.entities.ano.EmpBean(6, "ahmad", 0, (com.ibm.ws.query.entities.ano.DeptBean) d5, 3, true, a4, a4);
            e7 = new com.ibm.ws.query.entities.ano.EmpBean(7, "charlene", 0, (com.ibm.ws.query.entities.ano.DeptBean) d1, 4, true, a5, a6);
            e8 = new com.ibm.ws.query.entities.ano.EmpBean(8, "Tom Rayburn", 0, (com.ibm.ws.query.entities.ano.DeptBean) d5, 4, true, a7, a6);
            e9 = new com.ibm.ws.query.entities.ano.EmpBean(9, "harry", 0, (com.ibm.ws.query.entities.ano.DeptBean) d1, 5, true, a8, a9);
            e10 = new com.ibm.ws.query.entities.ano.EmpBean(10, "Catalina Wei", 0, null, 5, true, null, a6);

            pj1 = new com.ibm.ws.query.entities.ano.ProjectBean(1000, "WebSphere Version 1", (byte) 10, (short) 20, 50, (com.ibm.ws.query.entities.ano.DeptBean) d1);
            pj2 = new com.ibm.ws.query.entities.ano.ProjectBean(2000, "WebSphere Feature Pack", (byte) 40, (short) 10, 100, (com.ibm.ws.query.entities.ano.DeptBean) d2);
            pj3 = new com.ibm.ws.query.entities.ano.ProjectBean(3000, "WebSphere Community Edition", (byte) 45, (short) 10, 100, null);

            t1 = new com.ibm.ws.query.entities.ano.TaskBean(1010, "Design ", "Design ", (com.ibm.ws.query.entities.ano.ProjectBean) pj1);
            t2 = new com.ibm.ws.query.entities.ano.TaskBean(1020, "Code", "Code", (com.ibm.ws.query.entities.ano.ProjectBean) pj1);
            t3 = new com.ibm.ws.query.entities.ano.TaskBean(1030, "Test", "Test", (com.ibm.ws.query.entities.ano.ProjectBean) pj1);
            t4 = new com.ibm.ws.query.entities.ano.TaskBean(2010, "Design", "Design", (com.ibm.ws.query.entities.ano.ProjectBean) pj2);
            t5 = new com.ibm.ws.query.entities.ano.TaskBean(2020, "Code, Test", "Code, Test", (com.ibm.ws.query.entities.ano.ProjectBean) pj2);

            cf1 = new com.ibm.ws.query.entities.ano.CharityFund("WorldWildlifeFund", 1000.);
            cf2 = new com.ibm.ws.query.entities.ano.CharityFund("GlobalWarmingFund", 2000.);
        }
    }

    protected void setUpPersonBean() throws Exception {
        System.out.println("starting SetupQueryTestCase - setUpPersonBean()");

        if (isXmlOrMap) {
            _em.getTransaction().begin();
            List<IPersonBean> dlist = _em.createQuery("select s from com.ibm.ws.query.entities.xml.PersonBean s").getResultList();
            for (IPersonBean s : dlist) {
                _em.remove(s);
            }
            _em.getTransaction().commit();

            original = new com.ibm.ws.query.entities.xml.XMLPersonBean();
        } else {
            _em.getTransaction().begin();
            List<IPersonBean> dlist = _em.createQuery("select s from com.ibm.ws.query.entities.ano.PersonBean s").getResultList();
            for (IPersonBean s : dlist) {
                _em.remove(s);
            }
            _em.getTransaction().commit();

            original = new com.ibm.ws.query.entities.ano.PersonBean();
        }
        _em.getTransaction().begin();
        original.setFirst("OpenJPA");
        _em.persist(original);
        _em.getTransaction().commit();
    }

    protected void setUpEmpDept() throws Exception {
        System.out.println("starting SetupQueryTestCase - setupEmpDept()");
        _em.getTransaction().begin();

        // // scenario that removes all entities but may delete row mult
        // times(if fk cascade delete,fails with occ exception)

        // // scenario that deletes all rows but may not remove all entities (if
        // fk cascade delete,needs clear())

        if (isXmlOrMap) {
            List<IDeptBean> dlist = _em.createQuery("select s from com.ibm.ws.query.entities.xml.XMLDeptBean s").getResultList();
            for (IDeptBean s : dlist) {
                _em.remove(s);
            }
            _em.flush();
            List<ITaskBean> telist = _em.createQuery("select s from com.ibm.ws.query.entities.xml.XMLEmpBean e, in (e.tasks) s").getResultList();
            for (ITaskBean s : telist) {
                _em.remove(s);
            }
            _em.flush();
            List<ITaskBean> tlist = _em.createQuery(
                                                    "select s from com.ibm.ws.query.entities.xml.TaskBean s")
                            .getResultList();
            for (ITaskBean s : tlist) {
                _em.remove(s);
            }
            _em.flush();
            List<IEmpBean> elist = _em.createQuery(
                                                   "select s from com.ibm.ws.query.entities.xml.XMLEmpBean s")
                            .getResultList();
            for (IEmpBean s : elist) {
                _em.remove(s);
            }
            _em.flush();
            List<IProjectBean> plist = _em
                            .createQuery(
                                         "select s from com.ibm.ws.query.entities.xml.ProjectBean s")
                            .getResultList();
            for (IProjectBean s : plist) {
                _em.remove(s);
            }
            _em.flush();
            List<IAddressBean> alist = _em
                            .createQuery(
                                         "select s from com.ibm.ws.query.entities.xml.AddressBean s")
                            .getResultList();
            for (IAddressBean s : alist) {
                _em.remove(s);
            }
            _em.flush();

        } else {
            List<IDeptBean> dlist = _em.createQuery(
                                                    "select s from com.ibm.ws.query.entities.ano.DeptBean s")
                            .getResultList();
            for (IDeptBean s : dlist) {
                _em.remove(s);
            }
            _em.flush();
            List<ITaskBean> telist = _em
                            .createQuery(
                                         "select s from com.ibm.ws.query.entities.ano.EmpBean e, in (e.tasks) s")
                            .getResultList();
            for (ITaskBean s : telist) {
                _em.remove(s);
            }
            _em.flush();
            List<ITaskBean> tlist = _em.createQuery(
                                                    "select s from com.ibm.ws.query.entities.ano.TaskBean s")
                            .getResultList();
            for (ITaskBean s : tlist) {
                _em.remove(s);
            }
            _em.flush();
            List<IEmpBean> elist = _em.createQuery(
                                                   "select s from com.ibm.ws.query.entities.ano.EmpBean s")
                            .getResultList();
            for (IEmpBean s : elist) {
                _em.remove(s);
            }
            _em.flush();
            List<IProjectBean> plist = _em
                            .createQuery(
                                         "select s from com.ibm.ws.query.entities.ano.ProjectBean s")
                            .getResultList();
            for (IProjectBean s : plist) {
                _em.remove(s);
            }
            _em.flush();
            List<IAddressBean> alist = _em
                            .createQuery(
                                         "select s from com.ibm.ws.query.entities.ano.AddressBean s")
                            .getResultList();
            for (IAddressBean s : alist) {
                _em.remove(s);
            }
            _em.flush();
        }

//        String sql = "delete from " + _schemaName + "OPENJPA_SEQUENCE_TABLE";
//        Query delSeqTable = _em.createNativeQuery(sql);
//        delSeqTable.executeUpdate();

        _em.flush();
        _em.clear();

        // "delete from OPENJPA_SEQUENCE_TABLE" requires a commit here,
        // otherwise hangs
        _em.getTransaction().commit();
        _em.getTransaction().begin();

        // create entity instances
        createPOJOs();

        t1.addEmp(e1);
        t2.addEmp(e1);
        t2.addEmp(e2);
        t2.addEmp(e9);
        t3.addEmp(e9);
        t3.addEmp(e5);
        t4.addEmp(e1);

        _em.persist(a1);
        _em.persist(a2);
        _em.persist(a3);
        _em.persist(a4);
        _em.persist(a5);
        _em.persist(a6);
        _em.persist(a7);
        _em.persist(a8);
        _em.persist(a9);

        _em.persist(e1);
        _em.persist(e2);
        _em.persist(e3);
        _em.persist(e4);
        _em.persist(e5);
        _em.persist(e6);
        _em.persist(e7);
        _em.persist(e8);
        _em.persist(e9);
        _em.persist(e10);

        _em.persist(t1);
        _em.persist(t2);
        _em.persist(t3);
        _em.persist(t4);
        _em.persist(t5);
        _em.persist(pj1);
        _em.persist(pj2);
        _em.persist(pj3);

        _em.persist(d1);
        _em.persist(d2);
        _em.persist(d3);
        _em.persist(d4);
        _em.persist(d5);

        d1.setReportsTo(d4);
        d2.setReportsTo(d4);
        d3.setReportsTo(d5);
        d4.setReportsTo(d5);
        d5.setReportsTo(d5);

        d1.setMgr(e3);
        d2.setMgr(e4);
        d3.setMgr(e6);
        d4.setMgr(e8);
        d5.setMgr(e10);

        d1.setCharityFund(cf1);
        d2.setCharityFund(cf2);

        _em.getTransaction().commit();
        _em.clear();

        // gfh needed for test 53,55 because of defect
        if (isXmlOrMap) {
            List<IEmpBean> elist = _em.createQuery("select s from com.ibm.ws.query.entities.xml.XMLEmpBean s").getResultList();
        } else {
            List<IEmpBean> elist = _em.createQuery("select s from com.ibm.ws.query.entities.ano.EmpBean s").getResultList();
        }
    }

    protected void setUpPartComposite() throws Exception {
        System.out.println("starting SetupQueryTestCase - setUpPartComposite()");

        if (isXmlOrMap) {
            _em.getTransaction().begin();

            List<ISupplier> slist = _em.createQuery("select s from com.ibm.ws.query.entities.xml.XMLSupplier s").getResultList();
            for (ISupplier s : slist) {
                s.getSupplies().clear();
                _em.remove(s);
            }
            List<IUsage> ulist = _em.createQuery("select u from com.ibm.ws.query.entities.xml.Usage as u").getResultList();
            for (IUsage u : ulist) {
                _em.remove(u);
            }
            List<IPart> plist = _em.createQuery("select p from com.ibm.ws.query.entities.xml.XMLPart as p").getResultList();
            for (IPart p : plist) {
                _em.remove(p);
            }

            List<IOrderBean> olist = _em.createQuery("select o from com.ibm.ws.query.entities.xml.OrderBean as o").getResultList();
            for (IOrderBean p : olist) {
                _em.remove(p);
            }

            List<ILineItem> llist = _em.createQuery("select l from com.ibm.ws.query.entities.xml.LineItem as l").getResultList();
            for (ILineItem p : llist) {
                _em.remove(p);
            }

            List<IProduct> prlist = _em.createQuery("select p from com.ibm.ws.query.entities.xml.Product as p").getResultList();
            for (IProduct p : prlist) {
                _em.remove(p);
            }
            List<ICustomerBean> clist = _em.createQuery("select p from com.ibm.ws.query.entities.xml.CustomerBean as p").getResultList();
            for (ICustomerBean p : clist) {
                _em.remove(p);
            }

            _em.getTransaction().commit();

            s1 = new com.ibm.ws.query.entities.xml.XMLSupplier(1, "S1");
            s2 = new com.ibm.ws.query.entities.xml.XMLSupplier(2, "S2");

            p1 = new com.ibm.ws.query.entities.xml.XMLPartBase(10, "P10", 10.00, 15.25);
            p2 = new com.ibm.ws.query.entities.xml.XMLPartBase(11, "P11", 110.00, 25.80);
            p3 = new com.ibm.ws.query.entities.xml.XMLPartBase(12, "P12", 114.00, 82.01);

            p4 = new com.ibm.ws.query.entities.xml.XMLPartComposite(20, "C20", 7.5, 1.0);
            p5 = new com.ibm.ws.query.entities.xml.XMLPartComposite(21, "C21", 0, 15.0);
            p6 = new com.ibm.ws.query.entities.xml.XMLPartComposite(99, "C99", 10, 20);

            c1 = new com.ibm.ws.query.entities.xml.XMLCustomerBean(01, "cust1", 1);
            c2 = new com.ibm.ws.query.entities.xml.XMLCustomerBean(02, "cust2", 2);

            pr1 = new com.ibm.ws.query.entities.xml.XMLProduct(1, "baffles", 10);

            o1 = new com.ibm.ws.query.entities.xml.XMLOrderBean(1, 502.5, false, (com.ibm.ws.query.entities.xml.XMLCustomerBean) c1);
            o2 = new com.ibm.ws.query.entities.xml.XMLOrderBean(2, 502.5, false, (com.ibm.ws.query.entities.xml.XMLCustomerBean) c2);

            l1 = new com.ibm.ws.query.entities.xml.XMLLineItem(1, (com.ibm.ws.query.entities.xml.XMLProduct) pr1, 5, 100.5, (com.ibm.ws.query.entities.xml.XMLOrderBean) o1);
            l2 = new com.ibm.ws.query.entities.xml.XMLLineItem(2, (com.ibm.ws.query.entities.xml.XMLProduct) pr1, 5, 100.5, (com.ibm.ws.query.entities.xml.XMLOrderBean) o2);

        } else {
            _em.getTransaction().begin();

            List<ISupplier> slist = _em.createQuery("select s from com.ibm.ws.query.entities.ano.Supplier s").getResultList();
            for (ISupplier s : slist) {
                s.getSupplies().clear();
                _em.remove(s);
            }
            List<IUsage> ulist = _em.createQuery("select u from com.ibm.ws.query.entities.ano.Usage as u").getResultList();
            for (IUsage u : ulist) {
                _em.remove(u);
            }
            List<IPart> plist = _em.createQuery("select p from com.ibm.ws.query.entities.ano.Part as p").getResultList();
            for (IPart p : plist) {
                _em.remove(p);
            }

            List<IOrderBean> olist = _em.createQuery("select o from com.ibm.ws.query.entities.ano.OrderBean as o").getResultList();
            for (IOrderBean p : olist) {
                _em.remove(p);
            }

            List<ILineItem> llist = _em.createQuery("select l from com.ibm.ws.query.entities.ano.LineItem as l").getResultList();
            for (ILineItem p : llist) {
                _em.remove(p);
            }

            List<IProduct> prlist = _em.createQuery(
                                                    "select p from com.ibm.ws.query.entities.ano.Product as p")
                            .getResultList();
            for (IProduct p : prlist) {
                _em.remove(p);
            }
            List<ICustomerBean> clist = _em.createQuery("select p from com.ibm.ws.query.entities.ano.CustomerBean as p").getResultList();
            for (ICustomerBean p : clist) {
                _em.remove(p);
            }

            _em.getTransaction().commit();

            s1 = new com.ibm.ws.query.entities.ano.Supplier(1, "S1");
            s2 = new com.ibm.ws.query.entities.ano.Supplier(2, "S2");

            p1 = new com.ibm.ws.query.entities.ano.PartBase(10, "P10", 10.00, 15.25);
            p2 = new com.ibm.ws.query.entities.ano.PartBase(11, "P11", 110.00, 25.80);
            p3 = new com.ibm.ws.query.entities.ano.PartBase(12, "P12", 114.00, 82.01);

            p4 = new com.ibm.ws.query.entities.ano.PartComposite(20, "C20", 7.5, 1.0);
            p5 = new com.ibm.ws.query.entities.ano.PartComposite(21, "C21", 0, 15.0);
            p6 = new com.ibm.ws.query.entities.ano.PartComposite(99, "C99", 10, 20);

            c1 = new com.ibm.ws.query.entities.ano.CustomerBean(01, "cust1", 1);
            c2 = new com.ibm.ws.query.entities.ano.CustomerBean(02, "cust2", 2);

            pr1 = new com.ibm.ws.query.entities.ano.Product(1, "baffles", 10);

            o1 = new com.ibm.ws.query.entities.ano.OrderBean(1, 502.5, false, (com.ibm.ws.query.entities.ano.CustomerBean) c1);
            o2 = new com.ibm.ws.query.entities.ano.OrderBean(2, 502.5, false, (com.ibm.ws.query.entities.ano.CustomerBean) c2);

            l1 = new com.ibm.ws.query.entities.ano.LineItem(1, (com.ibm.ws.query.entities.ano.Product) pr1, 5, 100.5, (com.ibm.ws.query.entities.ano.OrderBean) o1);
            l2 = new com.ibm.ws.query.entities.ano.LineItem(2, (com.ibm.ws.query.entities.ano.Product) pr1, 5, 100.5, (com.ibm.ws.query.entities.ano.OrderBean) o2);
        }

        _em.getTransaction().begin();

        _em.persist(s1);
        _em.persist(s2);

        _em.persist(p1);
        _em.persist(p2);
        _em.persist(p3);

        s1.addPart(p1).addPart(p2).addPart(p3);
        s2.addPart(p1).addPart(p3);

        _em.persist(p4);
        _em.persist(p5);
        _em.persist(p6);

        _em.persist(c1);
        _em.persist(c2);
        _em.persist(pr1);
        _em.persist(o1);
        _em.persist(o2);
        _em.persist(l1);
        _em.persist(l2);

        p4.addSubPart(_em, 4, p1);
        p5.addSubPart(_em, 1, p2).addSubPart(_em, 1, p3).addSubPart(_em, 4, p1);
        p6.addSubPart(_em, 1, p4).addSubPart(_em, 1, p5).addSubPart(_em, 4, p1);

        pr1.setBackorder(20);

        _em.getTransaction().commit();
        _em.clear();

    }

}
