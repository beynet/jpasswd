package org.beynet.model.password;

import java.util.UUID;

/**
 * Created by beynet on 13/10/2014.
 */
public abstract class AbstractPassword implements Password {


    private static final long serialVersionUID = -7952570445890817630L;

    protected AbstractPassword() {
        this(UUID.randomUUID().toString());
    }
    protected AbstractPassword(String id) {
        if (id==null||id.isEmpty()) throw new IllegalArgumentException("id must not be null nor empty");
        this.id=id;
        modified = new Long(System.currentTimeMillis());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractPassword that = (AbstractPassword) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (modified != null ? !modified.equals(that.modified) : that.modified != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = modified != null ? modified.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }


    @Override
    public String getId() {
        return id;
    }

    @Override
    public Long getModified() {
        return modified;
    }

    private Long modified ;
    private final String id;
}
