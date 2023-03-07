/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

package servlets.startup;

import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import com.ibm.tx.jta.UserTransactionFactory;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/StartupServlet" }, loadOnStartup = 1)
public class StartupServlet extends FATServlet {

    /**
     * Attempt a user transaction at application startup.
     */
    @Override
    public void init() {
        System.out.println("--- StartupServlet init starting ---");
        UserTransaction ut = UserTransactionFactory.getUserTransaction();
        try {
            ut.begin();
            ut.commit();
        } catch (Exception e) {
            System.out.println("--- StartupServlet init completed with exception: " + e + "---");
            e.printStackTrace(System.out);
            return;
        }
        System.out.println("--- StartupServlet init completed without exception ---");
    }

}
