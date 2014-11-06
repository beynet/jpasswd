package org.beynet.model.password;

import org.apache.lucene.index.IndexWriter;

import java.io.IOException;

/**
 * Created by beynet on 05/11/14.
 */
public class DeletedPassword extends AbstractPassword implements Password {
    public DeletedPassword() {
        
    }
    public DeletedPassword(String id) {
        super(id);
    }

    @Override
    public String getSummary() {
        return "";
    }

    @Override
    public Password refresh(Password newValues) {
        if (newValues==null) throw new IllegalArgumentException("new password must not be null");
        if (!newValues.getClass().equals(this.getClass())) throw new IllegalArgumentException("class mismatch");
        return new DeletedPassword(newValues.getId());
    }

    @Override
    public void index(IndexWriter writer) throws IOException {

    }

    @Override
    public void accept(PasswordVisitor v) {
        v.visit(this);
    }
}
