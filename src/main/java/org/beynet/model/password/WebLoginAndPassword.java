package org.beynet.model.password;

import java.net.URI;

/**
 * Created by beynet on 13/10/2014.
 */
public class WebLoginAndPassword extends AbstractPassword implements Password {
    public WebLoginAndPassword(URI uri,String login,String password) throws IllegalArgumentException {
        super();
        if (uri==null) throw new IllegalArgumentException("uri must not be null");
        if (login==null) throw new IllegalArgumentException("login must not be null");
        if (password==null) throw new IllegalArgumentException("password must not be null");
        this.uri      = uri ;
        this.login    = login ;
        this.password = new PasswordString(password);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        WebLoginAndPassword that = (WebLoginAndPassword) o;

        if (login != null ? !login.equals(that.login) : that.login != null) return false;
        if (password != null ? !password.equals(that.password) : that.password != null) return false;
        if (uri != null ? !uri.equals(that.uri) : that.uri != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        result = 31 * result + (login != null ? login.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        return result;
    }

    @Override
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        String host = uri.getHost();
        if (host!=null) {
            summary.append(host);

            summary.append(" ");
        }
        else {
            summary.append(uri.toString());
            summary.append(" ");
        }
        return summary.toString();
    }


    @Override
    public void accept(PasswordVisitor v) {
        v.visit(this);
        password.accept(v);
    }

    public URI getUri() {
        return uri;
    }

    public String getLogin() {
        return login;
    }

    public PasswordString getPassword() {
        return password;
    }

    private final URI            uri;
    private final String         login;
    private final PasswordString password;
}
