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
package test.jsonb.bundle;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.spi.JsonbProvider;
import javax.json.spi.JsonProvider;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import test.jsonp.bundle.Basket;
import test.jsonp.bundle.Basket.Direction;
import test.jsonp.bundle.Basket.Tee;
import test.jsonp.bundle.DiscGolfCourse;

/**
 * OSGi service with a dependency on JsonbProvider.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class ServiceThatRequiresJsonb {

    @Reference
    private JsonbProvider jsonbProvider;

    @Reference
    private JsonProvider jsonpProvider;

    @Activate
    protected void activate(ComponentContext context) throws Exception {
        System.out.println("TEST1: JsonbProvider obtained from declarative services is " + jsonbProvider.getClass().getName());
        System.out.println("TEST1.1: JsonProvider obtained from declarative services is " + jsonpProvider.getClass().getName());

        DiscGolfCourse course = new DiscGolfCourse();
        course.name = "Woodside Park";
        course.street = "3605 Hwy 52 N";
        course.city = "Rochester";
        course.state = "Minnesota";
        course.zipCode = 55901;
        course.baskets.add(new Basket(Tee.CONCRETE, 3, 220, Direction.SE, "Partly Wooded"));
        course.baskets.add(new Basket(Tee.CONCRETE, 3, 300, Direction.E, "Mostly Open"));
        course.baskets.add(new Basket(Tee.CONCRETE, 3, 170, Direction.E, "Wooded"));
        course.baskets.add(new Basket(Tee.CONCRETE, 3, 340, Direction.W, "Mostly Open"));
        course.baskets.add(new Basket(Tee.CONCRETE, 3, 180, Direction.W, "Partly Wooded"));
        course.baskets.add(new Basket(Tee.CONCRETE, 3, 220, Direction.S, "Mostly Open"));
        course.baskets.add(new Basket(Tee.CONCRETE, 3, 330, Direction.W, "Open"));
        course.baskets.add(new Basket(Tee.DIRT, 3, 300, Direction.W, "Mostly Open"));
        course.baskets.add(new Basket(Tee.DIRT, 3, 410, Direction.W, "Partly Wooded"));
        course.baskets.add(new Basket(Tee.DIRT, 3, 300, Direction.W, "Densely Wooded"));
        course.baskets.add(new Basket(Tee.DIRT, 3, 270, Direction.E, "Densely Wooded"));
        course.baskets.add(new Basket(Tee.DIRT, 3, 230, Direction.SW, "Wooded"));
        course.baskets.add(new Basket(Tee.DIRT, 3, 280, Direction.SE, "Partly Wooded"));
        course.baskets.add(new Basket(Tee.DIRT, 3, 320, Direction.NE, "Partly Wooded"));
        course.baskets.add(new Basket(Tee.DIRT, 3, 220, Direction.NE, "Wooded"));
        course.baskets.add(new Basket(Tee.DIRT, 3, 410, Direction.E, "Open"));
        course.baskets.add(new Basket(Tee.CONCRETE, 3, 230, Direction.NE, "Partly Wooded"));
        course.baskets.add(new Basket(Tee.CONCRETE, 3, 350, Direction.NW, "Open"));

        Jsonb jsonb = JsonbBuilder.newBuilder(jsonbProvider).withProvider(jsonpProvider).build();
        String json = jsonb.toJson(course);
        DiscGolfCourse copy = jsonb.fromJson(json, DiscGolfCourse.class);
        String s1 = course.toString();
        String s2 = copy.toString();
        if (s1.equals(s2))
            System.out.println("TEST2: JSON marshalled/unmarshalled successfully " + json);
        else
            System.out.println("TEST2: unmarshalled object does not match: " + s2);

        jsonb.close();
    }
}
