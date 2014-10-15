package org.beynet.model.store;

import org.beynet.model.password.Password;

/**
 * Created by beynet on 13/10/2014.
 */
public abstract class AbstractPasswordStoreEvent implements PasswordStoreEvent {

    public AbstractPasswordStoreEvent(Password p) {
        this.password = p;
    }

    @Override
    public Password getPassword() {
        return password;
    }

    private Password password;
}
