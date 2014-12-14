package org.beynet.model.store;

/**
 * Created by beynet on 13/10/2014.
 */
public interface PasswordStoreEventVisitor {
    void visit(PasswordRemoved r);
    void visit(PasswordDefinitivelyRemoved r);
    void visit(PasswordModifiedOrCreated p);
}
