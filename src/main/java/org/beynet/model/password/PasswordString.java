package org.beynet.model.password;

import java.io.Serializable;

/**
 * Created by beynet on 13/10/2014.
 */
public class PasswordString implements Serializable {

    private static final long serialVersionUID = -6711292708352750467L;

    public PasswordString(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String getHiddenPassword() {
        return "*****";
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void accept(PasswordVisitor p) {
        p.visit(this);
    }

    private String password;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PasswordString that = (PasswordString) o;

        if (password != null ? !password.equals(that.password) : that.password != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return password != null ? password.hashCode() : 0;
    }
}
