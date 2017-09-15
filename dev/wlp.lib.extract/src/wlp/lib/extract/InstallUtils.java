/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package wlp.lib.extract;

public class InstallUtils {

	public static final String PRODUCTNAME = "Liberty";

    public static String getEditionName(String editionCode) {

        String editionCodeUpperCase = editionCode.toUpperCase();
        if (editionCodeUpperCase.equals("BASE"))
            return PRODUCTNAME;
        else if (editionCodeUpperCase.equals("BASE_ILAN"))
            return PRODUCTNAME + " (ILAN)";
        else if (editionCodeUpperCase.equals("DEVELOPERS"))
            return PRODUCTNAME + " for Developers";
        else if (editionCodeUpperCase.equals("EXPRESS"))
            return PRODUCTNAME + " - Express";
        else if (editionCodeUpperCase.equals("EARLY_ACCESS"))
            return PRODUCTNAME + " Early Access";
        else if (editionCodeUpperCase.equals("LIBERTY_CORE"))
            return PRODUCTNAME + " Core";
        else if (editionCodeUpperCase.equals("LIBERTY_CORE_ISV"))
            return PRODUCTNAME + " Core for ISVs";
        else if (editionCodeUpperCase.equals("ND"))
            return PRODUCTNAME + " Network Deployment";
        else if (editionCodeUpperCase.equals("ZOS"))
            return PRODUCTNAME + " z/OS";
        else {
            return editionCode;
        }
    }
}
