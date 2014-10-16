package org.beynet.model.password;


import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

import java.io.Serializable;

/**
 * Created by beynet on 12/10/2014.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "class")
@JsonSubTypes({
    @JsonSubTypes.Type(value = WebLoginAndPassword.class)
})
public interface Password extends Serializable {
    @JsonIgnore
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

    /**
     * update current password with values provided by p.
     * current password modified date MUST be modified
     * @param p
     */
    /**
     *
     * @param newValues
     * @return a copy of current password (ie with same id) but with
     * <ul>
     *     <li>getModified date changed</li>
     *     <li>copy.getId().equals(current.getId())</li>
     * </ul>
     */
    public Password refresh(Password newValues);


    public void accept(PasswordVisitor v);
}
