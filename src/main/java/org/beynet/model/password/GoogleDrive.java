package org.beynet.model.password;

import org.apache.lucene.index.IndexWriter;

import java.io.IOException;

/**
 * Created by beynet on 02/11/14.
 */
public class GoogleDrive extends AbstractPassword implements Password {

    public GoogleDrive() {
        super(GOOGLE_DRIVE_ID);
    }
    public GoogleDrive(String refreshToken) {
        super(GOOGLE_DRIVE_ID);
        this.refreshToken = refreshToken;
    }

    @Override
    public String getSummary() {
        return "google drive";
    }

    @Override
    public Password refresh(Password newValues) {
        if (!newValues.getClass().equals(this.getClass())) throw new IllegalArgumentException("class mismatch");
        GoogleDrive gd = (GoogleDrive) newValues;
        return new GoogleDrive(gd.getRefreshToken());
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

    public static final String GOOGLE_DRIVE_ID = "googledrive";
}
