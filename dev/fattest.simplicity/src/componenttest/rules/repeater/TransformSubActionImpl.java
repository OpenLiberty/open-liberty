/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package componenttest.rules.repeater;

public class TransformSubActionImpl implements TransformSubAction {

    @Override
    public void transform(String[] args) {
        // Note the use of 'com.ibm.ws.JakartaTransformer'.
        // 'org.eclipse.transformer.Transformer' might also be used instead.

        org.eclipse.transformer.cli.TransformerCLI.runWith(new org.eclipse.transformer.cli.JakartaTransformerCLI(System.out, System.err, args));
    }
}