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

@WebServlet({ "/SimpleClientServlet2" })
public class SimpleClientServlet extends ClientServletBase {

    private static final long serialVersionUID = 1L;

	@Test
    public void testWSATRE001FVT() {
    	assertTrue(execute(BASE_URL, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE002FVT() {
    	assertTrue(execute(BASE_URL, "rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE003FVT() {
    	assertTrue(execute(BASE_URL, noXARes, OneXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testWSATRE004FVT() {
    	assertTrue(execute(BASE_URL, noXARes, OneXAResVoteRollback, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
    public void testWSATRE005FVT() {
    	assertTrue(execute(BASE_URL, noXARes, OneXAResVoteReadonly, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE006FVT() {
    	assertTrue(execute(BASE_URL, noXARes, OneXARes, "rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE007FVT() {
    	assertTrue(execute(BASE_URL, noXARes, TwoXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testWSATRE008FVT() {
    	assertTrue(execute(BASE_URL, noXARes, TwoXAResVoteRollback, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
    public void testWSATRE009FVT() {
    	assertTrue(execute(BASE_URL, noXARes, TwoXAResVoteReadonlyCommit, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE010FVT() {
    	assertTrue(execute(BASE_URL, noXARes, TwoXAResVoteReadonly, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE011FVT() {
    	assertTrue(execute(BASE_URL, noXARes, TwoXARes, "rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE012FVT() {
    	assertTrue(execute(BASE_URL, OneXARes, noXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.HeuristicCommitException" })
    public void testWSATRE013FVT() {
    	assertTrue(execute(BASE_URL, OneXAResVoteRollback, noXARes, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
    public void testWSATRE014FVT() {
    	assertTrue(execute(BASE_URL, OneXAResVoteReadonly, noXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE015FVT() {
    	assertTrue(execute(BASE_URL, OneXARes, noXARes, "rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE016FVT() {
    	assertTrue(execute(BASE_URL, OneXARes, OneXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
    public void testWSATRE017FVT() {
    	assertTrue(execute(BASE_URL, OneXAResVoteRollback, OneXARes, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
    public void testWSATRE018FVT() {
    	assertTrue(execute(BASE_URL, OneXARes, OneXAResVoteRollback, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
    public void testWSATRE019FVT() {
    	assertTrue(execute(BASE_URL, OneXAResVoteReadonly, OneXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE020FVT() {
    	assertTrue(execute(BASE_URL, OneXARes, OneXAResVoteReadonly, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE021FVT() {
    	assertTrue(execute(BASE_URL, OneXAResVoteReadonly, OneXAResVoteReadonly, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE022FVT() {
    	assertTrue(execute(BASE_URL, OneXARes, OneXARes, OneXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE023FVT() {
    	assertTrue(execute(BASE_URL, OneXARes, OneXARes, "rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE024FVT() {
    	assertTrue(execute(BASE_URL, TwoXARes, OneXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testWSATRE025FVT() {
    	assertTrue(execute(BASE_URL, TwoXAResVoteRollback, OneXARes, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testWSATRE026FVT() {
    	assertTrue(execute(BASE_URL, TwoXARes, OneXAResVoteRollback, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
    public void testWSATRE027FVT() {
    	assertTrue(execute(BASE_URL, TwoXAResVoteReadonly, OneXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE028FVT() {
    	assertTrue(execute(BASE_URL, TwoXARes, OneXAResVoteReadonly, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE029FVT() {
    	assertTrue(execute(BASE_URL, TwoXAResVoteReadonly, OneXAResVoteReadonly, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE030FVT() {
    	assertTrue(execute(BASE_URL, TwoXARes, OneXARes, OneXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE031FVT() {
    	assertTrue(execute(BASE_URL, TwoXARes, OneXARes, "rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE032FVT() {
    	assertTrue(execute(BASE_URL, OneXARes, TwoXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testWSATRE033FVT() {
    	assertTrue(execute(BASE_URL, OneXAResVoteRollback, TwoXARes, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testWSATRE034FVT() {
    	assertTrue(execute(BASE_URL, OneXARes, TwoXAResVoteRollback, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
    public void testWSATRE035FVT() {
    	assertTrue(execute(BASE_URL, OneXAResVoteReadonly, TwoXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE036FVT() {
    	assertTrue(execute(BASE_URL, OneXARes, TwoXAResVoteReadonly, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE037FVT() {
    	assertTrue(execute(BASE_URL, OneXAResVoteReadonly, TwoXAResVoteReadonly, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE038FVT() {
    	assertTrue(execute(BASE_URL, OneXARes, TwoXARes, OneXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE039FVT() {
    	assertTrue(execute(BASE_URL, OneXARes, TwoXARes, "rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE040FVT() {
    	assertTrue(execute(BASE_URL, TwoXARes, TwoXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testWSATRE041FVT() {
    	assertTrue(execute(BASE_URL, TwoXAResVoteRollback, TwoXARes, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testWSATRE042FVT() {
    	assertTrue(execute(BASE_URL, TwoXARes, TwoXAResVoteRollback, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
    public void testWSATRE043FVT() {
    	assertTrue(execute(BASE_URL, TwoXAResVoteReadonly, TwoXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE044FVT() {
    	assertTrue(execute(BASE_URL, TwoXARes, TwoXAResVoteReadonly, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE045FVT() {
    	assertTrue(execute(BASE_URL, TwoXAResVoteReadonly, TwoXAResVoteReadonly, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE046FVT() {
    	assertTrue(execute(BASE_URL, TwoXARes, TwoXARes, OneXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE047FVT() {
    	assertTrue(execute(BASE_URL, TwoXARes, TwoXARes, "rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testWSATRE048FVT() {
    	assertTrue(execute(BASE_URL, noXARes, OneXARes, "setRollbackOnlyBeforeWSCall", "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
    public void testWSATRE049FVT() {
    	assertTrue(execute(BASE_URL, noXARes, OneXARes, "setRollbackOnlyBeforeWSCall", "rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testWSATRE050FVT() {
    	assertTrue(execute(BASE_URL, noXARes, OneXARes, "setRollbackOnlyAfterWSCall", "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
    public void testWSATRE051FVT() {
    	assertTrue(execute(BASE_URL, noXARes, OneXARes, "setRollbackOnlyAfterWSCall", "rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testWSATRE052FVT() {
    	assertTrue(execute(BASE_URL, OneXARes, OneXARes, "setRollbackOnlyAfterWSCall", "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
    public void testWSATRE053FVT() {
    	assertTrue(execute(BASE_URL, OneXARes, OneXARes, "setRollbackOnlyAfterWSCall", "rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE054FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL, noXARes, OneXARes, OneXARes, noXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE055FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL, noXARes, OneXARes, OneXARes, OneXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testWSATRE056FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL, noXARes, OneXARes, OneXARes, OneXAResVoteRollback, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testWSATRE057FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL, noXARes, OneXAResVoteRollback, OneXARes, OneXARes, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testWSATRE058FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL, noXARes, OneXARes, OneXAResVoteRollback, OneXARes, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
    public void testWSATRE059FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL, noXARes, TwoXARes, TwoXARes, noXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE060FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL, noXARes, TwoXARes, TwoXARes, TwoXARes, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testWSATRE061FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL, noXARes, TwoXARes, TwoXARes, TwoXAResVoteRollback, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testWSATRE062FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL, noXARes, TwoXAResVoteRollback, TwoXARes, TwoXARes, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testWSATRE063FVT() {
    	assertTrue(execute(BASE_URL, BASE_URL, noXARes, TwoXARes, TwoXAResVoteRollback, TwoXARes, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }
}
