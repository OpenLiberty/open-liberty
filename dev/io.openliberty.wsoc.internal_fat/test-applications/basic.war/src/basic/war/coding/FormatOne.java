/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package basic.war.coding;

public class FormatOne {

    String _first = "Initial String";
    String _second = "IDONTCARE";
    String _third = "blah";

    String errorData = null;

    public FormatOne(String first, String second, String third) {
        _first = first;
        _second = second;
        _third = third;

    }

    public FormatOne(String errorData) {
        this.errorData = errorData;
    }

    public static FormatOne doDecoding(String s) {

        String[] vals = s.split(":");
        if (vals.length == 3)
            return new FormatOne(vals[0], vals[1], vals[2]);
        else
            return new FormatOne(s);
    }

    public static String doEncoding(FormatOne fo) {
        if (fo.errorData != null)
            return fo.errorData;
        else
            return fo._third + ":" + fo._second + ":" + fo._first;
    }

}
