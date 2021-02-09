/**
 * 
 */
package mpGraphQL10.basicQuery;

public class Error {

    private String message;
    private Object[] path;
    private String extensions;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object[] getPath() {
        return path;
    }

    public void setPath(Object[] path) {
        this.path = path;
    }

    public String getExtensions() {
        return extensions;
    }

    public void setExtensions(String extensions) {
        this.extensions = extensions;
    }
}
