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
package transactionscopedtest;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

public class RequiredTransactionalHelperBean {

    @Transactional(TxType.REQUIRED)
    public void runUnderRequired(Work work) throws Exception {
        work.run();
    }

    public static interface Work {
        public void run() throws Exception;
    }

}
