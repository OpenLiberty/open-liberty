package com.ibm.ws.anno.test.data.sub;

public class Tester {
	public static void main(String[] args) {
		show("p1");
		show("p2");
	}

	public static void show(String path) {
		java.io.File file = new java.io.File(path);
		
		String[] children = file.list();
		
		System.out.println("  [ " + file.getName() + " ] [ " + file.getPath() + " ] [ " + file.getAbsolutePath() + " ]");

		if ( children == null ) {
			System.out.println("  Null");

		} else {
			for ( String child : children ) {
				System.out.println("  [ " + child + " ]");
			}
		}
	}
}
