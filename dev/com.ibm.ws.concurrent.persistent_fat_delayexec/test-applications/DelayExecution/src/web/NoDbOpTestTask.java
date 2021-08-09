/*******************************************************************************
 * Copyright (c) 2014, 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Task that increments an entry in a database.
 * The task creates the entry if not already present.
 */
public class NoDbOpTestTask implements Runnable, Callable<Integer>, Serializable {
    private static final long serialVersionUID = 2172862926900136340L;
    static LinkedBlockingQueue<Integer> lbq;

    public NoDbOpTestTask(LinkedBlockingQueue<Integer> lbq) {
    	NoDbOpTestTask.lbq = lbq;
    }

	@Override
	public Integer call() throws Exception {
		Integer result = Integer.valueOf(1000);
    	lbq.put(result);
		return result;
	}
	
    @Override
    public void run() {
        try {
        	call();
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }
}
