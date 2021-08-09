package web.war.jaxrs.securitycontext;

// public class MicroProfileApp extends HttpServlet {

public class Utils {

    public static Class<?> thisClass = Utils.class;
    public static final String newLine = System.getProperty("line.separator");

    
   
    

    public static void writeLine(StringBuffer sb, String msg) {
        sb.append(msg + newLine);
    }

    public static void printDivider(StringBuffer sb) {

        Utils.writeLine(sb, "--------------------------------------------------------------------------------");
    }

}
