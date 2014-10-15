package org.beynet.model.password;


import java.io.Serializable;

/**
 * Created by beynet on 12/10/2014.
 */
public interface Password extends Serializable {
    /**
     * short summary of current password
     * @return
     */
    public String getSummary();

    public Long getModified();

    /**
     * @return password uniq id
     */
    public String getId();


    public void accept(PasswordVisitor v);
}
