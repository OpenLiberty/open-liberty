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
package com.ibm.ws.javaee.ddmodel.ejb;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.ejb.CMRField;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejb.EJBRelation;
import com.ibm.ws.javaee.dd.ejb.EJBRelationshipRole;
import com.ibm.ws.javaee.dd.ejb.Relationships;

public class RelationshipsTest extends EJBJarTestBase {

    String relationships = EJBJarTest.ejbJar20() +
                           "<relationships>" +
                           "<ejb-relation>" +
                           "<ejb-relation-name>EjbRelationName</ejb-relation-name>" +

                           "<ejb-relationship-role>" +
                           "<ejb-relationship-role-name>HorseRoleName</ejb-relationship-role-name>" +
                           "<multiplicity>One</multiplicity>" +
                           "<relationship-role-source>" +
                           "<ejb-name>HorseEJBName</ejb-name>" +
                           "</relationship-role-source>" +
                           "<cmr-field>" +
                           "<cmr-field-name>HorseCMRFieldName</cmr-field-name>" +
                           "</cmr-field>" +
                           "</ejb-relationship-role>" +

                           "<ejb-relationship-role>" +
                           "<ejb-relationship-role-name>RiderRoleName</ejb-relationship-role-name>" +
                           "<multiplicity>Many</multiplicity>" +
                           "<cascade-delete/>" +
                           "<relationship-role-source>" +
                           "<ejb-name>RiderEJBName</ejb-name>" +
                           "</relationship-role-source>" +
                           "<cmr-field>" +
                           "<cmr-field-name>RiderCMRFieldName</cmr-field-name>" +
                           "<cmr-field-type>java.util.Collection</cmr-field-type>" +
                           "</cmr-field>" +
                           "</ejb-relationship-role>" +

                           "<ejb-relationship-role>" +
                           "<ejb-relationship-role-name>SaddleRoleName</ejb-relationship-role-name>" +
                           "<multiplicity>Many</multiplicity>" +
                           "<relationship-role-source>" +
                           "<ejb-name>SaddleEJBName</ejb-name>" +
                           "</relationship-role-source>" +
                           "<cmr-field>" +
                           "<cmr-field-name>SaddleCMRFieldName</cmr-field-name>" +
                           "<cmr-field-type>java.util.Set</cmr-field-type>" +
                           "</cmr-field>" +
                           "</ejb-relationship-role>" +

                           "</ejb-relation>" +
                           "</relationships>" +
                           "</ejb-jar>";

    @Test
    public void testRelationships() throws Exception {
        EJBJar ejbJar = getEJBJar(relationships);
        Relationships relationships = ejbJar.getRelationshipList();

        List<EJBRelation> ejbRelationList = relationships.getEjbRelations();
        Assert.assertEquals(1, ejbRelationList.size());
        EJBRelation ejbRelation0 = ejbRelationList.get(0);
        Assert.assertEquals("EjbRelationName", ejbRelation0.getName());
        List<EJBRelationshipRole> relationshipRoles = ejbRelation0.getRelationshipRoles();
        Assert.assertEquals(3, relationshipRoles.size());
        //Note there should only be 2 relationshipRoles but we have 3 and the parser accepts that

        EJBRelationshipRole relRole0 = relationshipRoles.get(0);
        EJBRelationshipRole relRole1 = relationshipRoles.get(1);
        EJBRelationshipRole relRole2 = relationshipRoles.get(2);
        Assert.assertEquals("HorseRoleName", relRole0.getName());
        Assert.assertEquals("RiderRoleName", relRole1.getName());
        Assert.assertEquals("SaddleRoleName", relRole2.getName());
        Assert.assertEquals(EJBRelationshipRole.MULTIPLICITY_TYPE_ONE, relRole0.getMultiplicityTypeValue());
        Assert.assertEquals(EJBRelationshipRole.MULTIPLICITY_TYPE_MANY, relRole1.getMultiplicityTypeValue());
        Assert.assertEquals(EJBRelationshipRole.MULTIPLICITY_TYPE_MANY, relRole2.getMultiplicityTypeValue());
        Assert.assertEquals(false, relRole0.isCascadeDelete());
        Assert.assertEquals(true, relRole1.isCascadeDelete());
        Assert.assertEquals(false, relRole2.isCascadeDelete());

        Assert.assertEquals("HorseEJBName", relRole0.getSource().getEntityBeanName());
        Assert.assertEquals("RiderEJBName", relRole1.getSource().getEntityBeanName());
        Assert.assertEquals("SaddleEJBName", relRole2.getSource().getEntityBeanName());

        CMRField cmrField0 = relRole0.getCmrField();
        CMRField cmrField1 = relRole1.getCmrField();
        CMRField cmrField2 = relRole2.getCmrField();
        Assert.assertEquals("HorseCMRFieldName", cmrField0.getName());
        Assert.assertEquals("RiderCMRFieldName", cmrField1.getName());
        Assert.assertEquals("SaddleCMRFieldName", cmrField2.getName());
        Assert.assertEquals(CMRField.TYPE_UNSPECIFIED, cmrField0.getTypeValue());
        Assert.assertEquals(CMRField.TYPE_JAVA_UTIL_COLLECTION, cmrField1.getTypeValue());
        Assert.assertEquals(CMRField.TYPE_JAVA_UTIL_SET, cmrField2.getTypeValue());
    }

}
