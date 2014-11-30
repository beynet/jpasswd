package org.beynet.model.password;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.io.IOException;

/**
 * Created by beynet on 28/11/14.
 */
public class Note extends AbstractPassword implements Password {
    private Note() {

    }
    public Note(String title,String content) {
        super();
        this.title = title;
        this.content = content;
    }
    protected Note(String id,String title,String content) {
        super(id);
        this.title = title;
        this.content = content;
    }

    @Override
    public String getSummary() {
        return getTitle();
    }

    @Override
    public Password refresh(Password newValues) {
        if (newValues==null) throw new IllegalArgumentException("new password must not be null");
        if (!newValues.getClass().equals(this.getClass())) throw new IllegalArgumentException("class mismatch");
        return new Note(getId(),((Note)newValues).getTitle(),((Note)newValues).getContent());
    }

    @Override
    public void index(IndexWriter writer) throws IOException {
        // unindex previous version
        Term idTerm = new Term(FIELD_ID,getId());
        Query query = new TermQuery(idTerm);
        writer.deleteDocuments(query);


        Field idField = new StringField(FIELD_ID, getId(), Field.Store.YES);
        StringBuilder sb = new StringBuilder(getTitle());
        sb.append(" ");
        sb.append(getContent());
        Field txtField = new TextField(FIELD_TXT, sb.toString(), Field.Store.YES);

        Document document = new Document();
        document.add(idField);
        document.add(txtField);

        writer.addDocument(document);
        writer.commit();
    }

    @Override
    public void accept(PasswordVisitor v) {
        v.visit(this);
    }


    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    private String title  ;
    private String content;


}
