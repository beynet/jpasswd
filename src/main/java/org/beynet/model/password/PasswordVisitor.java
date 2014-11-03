package org.beynet.model.password;

/**
 * Created by beynet on 13/10/2014.
 */
public interface PasswordVisitor {

    public void visit(WebLoginAndPassword t);

    public void visit(GoogleDrive t);

    public void visit(PasswordString s);

}
