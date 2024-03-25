/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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
package web.simpleclient;

import static org.junit.Assert.assertTrue;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.tx.jta.ut.util.XAResourceImpl;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;

@WebServlet({ "/ComplexClientServlet" })
public class ComplexClientServlet extends ClientServletBase {
    private static final long serialVersionUID = 1L;

	@Test
    public void testWSATRE064FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL2, noXARes, OneXARes, OneXARes, noXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE065FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL2, noXARes, OneXARes, OneXARes, OneXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
    public void testWSATRE066FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL2, noXARes, OneXARes, OneXARes, OneXAResVoteRollback, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
    public void testWSATRE067FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL2, noXARes, OneXAResVoteRollback, OneXARes, OneXARes, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testWSATRE068FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL2, noXARes, OneXARes, OneXAResVoteRollback, OneXARes, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
    public void testWSATRE069FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL2, noXARes, TwoXARes, TwoXARes, noXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE070FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL2, noXARes, TwoXARes, TwoXARes, TwoXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
    public void testWSATRE071FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL2, noXARes, TwoXARes, TwoXARes, TwoXAResVoteRollback, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
    public void testWSATRE072FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL2, noXARes, TwoXAResVoteRollback, TwoXARes, TwoXARes, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
    public void testWSATRE073FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL2, noXARes, TwoXARes, TwoXAResVoteRollback, TwoXARes, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }
}