/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package sessionListener;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author anupw520
 * 
 */
public class CalculateListenerInvoke {

    static int i = 0;
    static final Collection<Object> attrValuesOnDestroy = new ArrayList<Object>();

    /**
     * @return the i
     */
    public static int getI() {
        return i;
    }

    /**
     * @param i the i to set
     */
    public static void setI(int i) {
        CalculateListenerInvoke.i = i;
    }

    public static void addNumber() {

        i++;
        System.out.println("CalculateListenerInvoke: Attribute added" + i);
    }

    public static void subtractNumber() {
        i--;
        System.out.println("CalculateListenerInvoke: Attribute removed" + i);
    }

    public static void printValue() {
        System.out.print(i);
    }

    public static void reset() {
        i = 0;
        System.out.println("CalculateListenerInvoke: attrValuesOnDestroy clear");
        attrValuesOnDestroy.clear();
    }

    public static Collection<Object> getAttrValuesOnDestroy() {
        System.out.println("CalculateListenerInvoke: getAttrValuesOnDestroy --> " + attrValuesOnDestroy);
        return attrValuesOnDestroy;
    }

    public static void addAttrValueOnDestroy(Object value) {
        System.out.println("CalculateListenerInvoke: addAttrValueOnDestroy add -->" + value.toString());
        attrValuesOnDestroy.add(value);
    }
}
