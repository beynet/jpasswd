package org.beynet.model.store;

import org.beynet.model.password.Password;

/**
 * Created by beynet on 13/10/2014.
 */
public class PasswordRemoved extends AbstractPasswordStoreEvent implements PasswordStoreEvent {

    public PasswordRemoved(Password p) {
        super(p);
    }


    @Override
    public void accept(PasswordStoreEventVisitor v) {
        v.visit(this);
    }

}
