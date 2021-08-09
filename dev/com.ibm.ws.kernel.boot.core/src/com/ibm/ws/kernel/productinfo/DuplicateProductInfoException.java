/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.productinfo;

import java.io.NotSerializableException;
import java.io.ObjectOutputStream;

@SuppressWarnings("serial")
public class DuplicateProductInfoException extends Exception {
    private final ProductInfo productInfo1;
    private final ProductInfo productInfo2;

    DuplicateProductInfoException(ProductInfo productInfo1, ProductInfo productInfo2) {
        super(productInfo1.getId() + ": " + productInfo1.getFile().getAbsoluteFile() + " and " + productInfo2.getFile().getAbsolutePath());
        this.productInfo1 = productInfo1;
        this.productInfo2 = productInfo2;
    }

    public ProductInfo getProductInfo1() {
        return productInfo1;
    }

    public ProductInfo getProductInfo2() {
        return productInfo2;
    }

    private void writeObject(ObjectOutputStream oos) throws NotSerializableException {
        throw new NotSerializableException();
    }
}
