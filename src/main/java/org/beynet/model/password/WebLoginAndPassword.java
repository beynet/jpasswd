package org.beynet.model.password;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.io.IOException;
import java.net.URI;

/**
 * Created by beynet on 13/10/2014.
 */
public class WebLoginAndPassword extends AbstractPassword implements Password {
    /**
     * used by json parser
     */
    private WebLoginAndPassword() {

    }
    public WebLoginAndPassword(URI uri,String login,String password) throws IllegalArgumentException {
        super();
        if (uri==null) throw new IllegalArgumentException("uri must not be null");
        if (login==null) throw new IllegalArgumentException("login must not be null");
        if (password==null) throw new IllegalArgumentException("password must not be null");
        this.uri      = uri ;
        this.login    = login ;
        this.password = new PasswordString(password);
    }
    public WebLoginAndPassword(String id,URI uri,String login,String password) throws IllegalArgumentException {
        super(id);
        if (uri==null) throw new IllegalArgumentException("uri must not be null");
        if (login==null) throw new IllegalArgumentException("login must not be null");
        if (password==null) throw new IllegalArgumentException("password must not be null");
        this.uri      = uri ;
        this.login    = login ;
        this.password = new PasswordString(password);
    }

    @Override
    public Password refresh(Password newValues) {
        if (!newValues.getClass().equals(this.getClass())) throw new IllegalArgumentException("class mismatch");
        WebLoginAndPassword newV = (WebLoginAndPassword)newValues;
        return new WebLoginAndPassword(this.getId(),newV.getUri(),newV.getLogin(),newV.getPassword().getPassword());
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

    @Override
    public void index(IndexWriter writer) throws IOException {
        // unindex previous version
        Term idTerm = new Term(FIELD_ID,getId());
        Query query = new TermQuery(idTerm);
        writer.deleteDocuments(query);


        Field idField = new StringField(FIELD_ID, getId(), Field.Store.YES);
        StringBuilder sb = new StringBuilder(login);
        sb.append(" ");
        sb.append(getUri().toString());
        Field txtField = new TextField(FIELD_TXT, sb.toString(), Field.Store.YES);

        Document document = new Document();
        document.add(idField);
        document.add(txtField);

        writer.addDocument(document);
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

    private  URI            uri;
    private  String         login;
    private  PasswordString password;
}
