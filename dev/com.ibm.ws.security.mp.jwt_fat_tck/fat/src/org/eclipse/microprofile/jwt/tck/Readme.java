/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.microprofile.jwt.tck;

public class Readme {

}
/*
 * A few notes about these tests:
 * They are from the MicroProfile JWT TCK.
 * They are ported with minimal change to the Liberty FAT framework from Arquillian.
 * The original tests are at https://github.com/eclipse/microprofile-jwt-auth
 *
 * Web.xml has been adapted for the Liberty implementation.
 *
 *
 * To create a signer certificate from the raw keys used in the TCK, the
 * following commands were used:
 *
 * openssl req -x509 -key privateKey.pem -nodes -days 3650 -newkey rsa:2048 -out temp.pem
 * openssl x509 -outform der -in temp.pem -out temp.der
 * then use ikeyman to add signer temp.der to keys.jks
 *
 *
 *
 *
 */
