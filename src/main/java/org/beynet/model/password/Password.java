package org.beynet.model.password;


import org.apache.lucene.index.IndexWriter;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by beynet on 12/10/2014.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "class")
@JsonSubTypes({
    @JsonSubTypes.Type(value = WebLoginAndPassword.class),
    @JsonSubTypes.Type(value = GoogleDrive.class),
    @JsonSubTypes.Type(value = DeletedPassword.class)
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

    public void index(IndexWriter writer) throws IOException;
    public void unIndex(IndexWriter writer) throws IOException;

    public void accept(PasswordVisitor v);

    public static final String FIELD_ID  ="id";
    public static final String FIELD_TXT ="txt";
}
