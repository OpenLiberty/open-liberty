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

List of Users/Groups
	-For list of Users/Groups present under TDS instance please refer TDS.ldif
	-For list of Users/Groups present under AD instance please refer AD.ldif
	-For list of Users/Groups present under SunOne instance please refer SunOne.ldif

Process for adding new users/groups under required instance
	-Start the required instance using apacheds.bat/sh present under apache_directory_server\apacheds-2.0.0-M15\bin
	-Connect to that instance using any of the LDAP client or apache directory studio
        -Generate the LDIF file using the following commands for the instances you updated. These files will make it easier to
         determine what changes you made during a review, and allows future developers to examine entries without starting
         up the instances.

           ldapsearch -LLL -H ldap://localhost:10389 -D uid=admin,ou=system -w secret -b "o=ibm,c=us" -S "" > TDS.ldif
           ldapsearch -LLL -H ldap://localhost:20389 -D uid=admin,ou=system -w secret -b "dc=secfvt2,dc=austin,dc=ibm,dc=com" -S "" > AD.ldif
           ldapsearch -LLL -H ldap://localhost:30389 -D uid=admin,ou=system -w secret -b "dc=rtp,dc=raleigh,dc=ibm,dc=com" -S "" > SunOne.ldif


        -Stop the Apache DS instance.		
        -Check in all of the modified files.
        -Ensure you update the remote instances as well. The servers can be found in the LDAPUtils class.
