/**
 *
 */
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
 * As of September 2017, a known issue prevents @RolesAllowed from working in JAX-RS
 * endpoints unless a @DeclarRoles annotation or web.xml is present to declare the roles.
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
