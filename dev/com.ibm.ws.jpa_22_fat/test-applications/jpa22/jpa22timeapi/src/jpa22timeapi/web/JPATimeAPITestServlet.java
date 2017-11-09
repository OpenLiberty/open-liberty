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

package jpa22timeapi.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Assert;
import org.junit.Test;

import componenttest.app.FATServlet;
import jpa22timeapi.entity.TimeAPIEntity;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestJPAAPI")
public class JPATimeAPITestServlet extends FATServlet {
    @PersistenceContext(unitName = "JPAPU")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Test
    public void testJPA22TimeAPI() throws Exception {
        java.time.LocalDate localDate = LocalDate.now();
        java.time.LocalDateTime localDateTime = LocalDateTime.now();
        java.time.LocalTime localTime = LocalTime.now();
        java.time.OffsetTime offsetTime = OffsetTime.now();
        java.time.OffsetDateTime offsetDateTime = OffsetDateTime.now();

        TimeAPIEntity timeEntity = new TimeAPIEntity();
        timeEntity.setLocalDate(localDate);
        timeEntity.setLocalDateTime(localDateTime);
        timeEntity.setLocalTime(localTime);
        timeEntity.setOffsetDateTime(offsetDateTime);
        timeEntity.setOffsetTime(offsetTime);

        tx.begin();
        em.persist(timeEntity);
        tx.commit();

        em.clear();

        TimeAPIEntity findEntity = em.find(TimeAPIEntity.class, timeEntity.getId());
        Assert.assertNotNull(findEntity);

        Assert.assertEquals(localDate, findEntity.getLocalDate());
        Assert.assertEquals(localDateTime, findEntity.getLocalDateTime());
        Assert.assertEquals(localTime, findEntity.getLocalTime());
        Assert.assertEquals(offsetTime, findEntity.getOffsetTime());
        Assert.assertEquals(offsetDateTime, findEntity.getOffsetDateTime());
    }
}
