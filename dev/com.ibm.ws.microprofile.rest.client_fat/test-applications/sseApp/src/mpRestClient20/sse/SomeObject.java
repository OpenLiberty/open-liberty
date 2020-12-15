/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient20.sse;

import java.math.BigInteger;

public class SomeObject {

    private String someString;
    private int someInt;
    private double someDouble;
    private BigInteger someBigInt;
    private Color favoriteColor;

    public enum Color {
        RED,
        BRIGHT_RED
    }

    public SomeObject() {}
    public SomeObject(String someString, int someInt, double someDouble, BigInteger someBigInt, Color favoriteColor) {
        this.someString = someString;
        this.someInt = someInt;
        this.someDouble = someDouble;
        this.someBigInt = someBigInt;
        this.favoriteColor = favoriteColor;
    }

    @Override
    public String toString() {
        return "SomeObject{" + someString + ", " + someInt + ", " + someDouble + ", " + someBigInt + ", " + favoriteColor + "}"; 
    }

    public String getSomeString() {
        return someString;
    }

    public void setSomeString(String someString) {
        this.someString = someString;
    }

    public int getSomeInt() {
        return someInt;
    }

    public void setSomeInt(int someInt) {
        this.someInt = someInt;
    }

    public double getSomeDouble() {
        return someDouble;
    }

    public void setSomeDouble(double someDouble) {
        this.someDouble = someDouble;
    }

    public BigInteger getSomeBigInt() {
        return someBigInt;
    }

    public void setSomeBigInt(BigInteger someBigInt) {
        this.someBigInt = someBigInt;
    }

    public Color getFavoriteColor() {
        return favoriteColor;
    }

    public void setFavoriteColor(Color favoriteColor) {
        this.favoriteColor = favoriteColor;
    }
}
