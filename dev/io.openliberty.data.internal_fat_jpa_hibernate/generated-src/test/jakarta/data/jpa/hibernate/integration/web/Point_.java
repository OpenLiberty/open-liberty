package test.jakarta.data.jpa.hibernate.integration.web;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Point.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Point_ {

	public static final String X = "x";
	public static final String Y = "y";

	
	/**
	 * @see test.jakarta.data.jpa.hibernate.integration.web.Point#x
	 **/
	public static volatile SingularAttribute<Point, Integer> x;
	
	/**
	 * @see test.jakarta.data.jpa.hibernate.integration.web.Point#y
	 **/
	public static volatile SingularAttribute<Point, Integer> y;
	
	/**
	 * @see test.jakarta.data.jpa.hibernate.integration.web.Point
	 **/
	public static volatile EmbeddableType<Point> class_;

}

