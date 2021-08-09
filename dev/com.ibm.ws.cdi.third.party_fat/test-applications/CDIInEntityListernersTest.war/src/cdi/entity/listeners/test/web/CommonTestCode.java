/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package cdi.entity.listeners.test.web;

import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.TypedQuery;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import cdi.entity.listeners.test.model.EntityA;
import cdi.entity.listeners.test.model.EntityB;

public class CommonTestCode {
    private final static SecureRandom sr = new SecureRandom();
    
    public static void populate(HttpServletRequest request, HttpServletResponse response, EntityManager em, UserTransaction tx) throws Exception {
        final PrintWriter pw = response.getWriter();        
        
        tx.begin();
        em.joinTransaction();
        em.clear();

        List<EntityA> newEntList = new ArrayList<EntityA>();

        long ctm = System.currentTimeMillis();

        for (int i = 0; i < 10; i++) {
            EntityA nEnt = new EntityA();
            nEnt.setStrData("Number " + sr.nextLong());
            nEnt.setLazyStringData("Lazy Number " + sr.nextLong());
            em.persist(nEnt);
            
            if (i % 3 == 0) {
                EntityB bEnt = new EntityB();
                bEnt.setStrData("Number " + sr.nextLong());
                nEnt.setEntityB(bEnt);
                em.persist(bEnt);
            }        
            
            newEntList.add(nEnt);
        }

        tx.commit();
        em.clear();

        pw.println("<br>Added new Entities:<br>");

        dumpList(newEntList, em, pw);
    }
    
    /*
     * Utility used by the populate and query actions to print a list of EntityA and Entity B
     * entities.  CSS for fancy table display, which isn't required to demonstrate an issue.
     */
    private static void dumpList(List<EntityA> retList, EntityManager em, PrintWriter pw) {
        System.out.println("Dumping list with Load State:");
        EntityManagerFactory emf = em.getEntityManagerFactory();
        PersistenceUnitUtil puu = emf.getPersistenceUnitUtil();

        pw.println("<style>");
        pw.println("#dumptable {");
        pw.println("   font-family: \"Trebuchet MS\", Arial;");
        pw.println("   border-collapse: collapse;");
        pw.println("   width: 100%;");
        pw.println("}");    
        
        pw.println("#dumptable td, #dumptable th {");
        pw.println("   border: 1px solid #ddd;");
        pw.println("   padding: 4px;");
        pw.println("}");
        
        pw.println("#dumptable tr:nth-child(even){background-color: #f2f2f2;}");
        
        pw.println("</style>");
        
        pw.println("<table width=\"100%\" id=\"dumptable\">");
        pw.println("<tr style=\"background-color:Orange;\">");
        pw.println("<td><b>Id</b></td>");
        pw.println("<td><b>String Data</b></td>");
        pw.println("<td><b>String Lazy Data</b></td>");
        pw.println("<td><b>EntityB Id</b></td>");
        pw.println("<td><b>EntityB Data</b></td>");
        pw.println("</tr>");

        for (EntityA m : retList) {
            StringBuilder sb = new StringBuilder();
            sb.append("Member: ");

            pw.println("<tr>");

            // Primary Key
            pw.print("<td><b>");
            if (puu.isLoaded(m, "id")) {
                pw.print(m.getId());
                sb.append(m.getId());
            } else {
                pw.print("NOT LOADED");
                sb.append("NOT LOADED");
            }

            sb.append(" / ");
            pw.println("</b></td>");

            // String Data
            pw.print("<td>");
            if (puu.isLoaded(m, "strData")) {
                pw.print(m.getStrData());
                sb.append(m.getStrData());
            } else {
                pw.print("NOT LOADED");
                sb.append("NOT LOADED");
            }
            sb.append(" / ");
            pw.println("</td>");

            // Lazy String Data
            pw.print("<td>");
            if (puu.isLoaded(m, "lazyStringData")) {
                pw.print(m.getLazyStringData());
                sb.append(m.getLazyStringData());
            } else {
                pw.print("NOT LOADED");
                sb.append("NOT LOADED");
            }
            sb.append(" / ");
            pw.println("</td>");

            // EntityB Data
            if (puu.isLoaded(m, "entityB")) {
                EntityB entB = m.getEntityB();
                
                if (entB != null) {
                    pw.print("<td><b>");
                    if (puu.isLoaded(entB, "id")) {
                        pw.print(entB.getId());
                        sb.append(entB.getId());
                    } else {
                        pw.print("NOT LOADED");
                        sb.append("NOT LOADED");
                    }
                    
                    sb.append(" / ");
                    pw.println("</b></td>");

                    // String Data
                    pw.print("<td>");
                    if (puu.isLoaded(entB, "strData")) {
                        pw.print(entB.getStrData());
                        sb.append(entB.getStrData());
                    } else {
                        pw.print("NOT LOADED");
                        sb.append("NOT LOADED");
                    }
                    sb.append(" / ");
                    pw.println("</td>");
                } else {
                    pw.print("<td colspan=2 style=\"background-color:Tomato;\">EntityB is null</td>");
                    sb.append("EntityB is null");
                }
            } else {
                pw.print("<td colspan=2 style=\"background-color:DodgerBlue;\">EntityB NOT LOADED</td>");
                sb.append("EntityB NOT LOADED");
            }
            
            System.out.println(sb);
            pw.println("</tr>");
        }
        pw.println("</table>");
    }
}
