package org.beynet.model.password;

import org.apache.lucene.index.IndexWriter;

import java.io.IOException;

/**
 * Created by beynet on 02/11/14.
 */
public class OneDrive extends AbstractPassword implements Password {

    public OneDrive() {
        super(ONE_DRIVE_ID);
    }
    public OneDrive(String refreshToken) {
        super(ONE_DRIVE_ID);
        this.refreshToken = refreshToken;
    }

    @Override
    public String getSummary() {
        return "one drive";
    }

    @Override
    public Password refresh(Password newValues) {
        if (!newValues.getClass().equals(this.getClass())) throw new IllegalArgumentException("class mismatch");
        OneDrive gd = (OneDrive) newValues;
        return new OneDrive(gd.getRefreshToken());
    }

    @Override
    public void index(IndexWriter writer) throws IOException {
        // we do not index
    }

    @Override
    public void unIndex(IndexWriter writer) throws IOException {

    }

    @Override
    public void accept(PasswordVisitor v) {

    }


    public String getRefreshToken() {
        return refreshToken;
    }

    private String         refreshToken;

    public static final String ONE_DRIVE_ID = "onedrive";
}
