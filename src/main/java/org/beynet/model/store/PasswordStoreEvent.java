package org.beynet.model.store;

import org.beynet.model.password.Password;

/**
 * Created by beynet on 13/10/2014.
 */
public interface PasswordStoreEvent {
    public Password getPassword();
    public void accept(PasswordStoreEventVisitor v);
}
