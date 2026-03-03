/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
 package testservlet40.jar.jandex_v35;

public class ComputeIntEncloser {

	public static interface ComputeInt {
		public int compute();
	}

	private static final int x;

	static {
		ComputeInt computeInt = new ComputeInt() {
			@Override
			public int compute() { return 0; }
		};
		
		x = computeInt.compute();
	}

}
