package org.beynet.model.store;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.beynet.model.Config;
import org.beynet.model.MainPasswordError;
import org.beynet.model.password.DeletedPassword;
import org.beynet.model.password.GoogleDrive;
import org.beynet.model.password.Password;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Created by beynet on 12/10/2014.
 * This class is responsible to store passwords.
 * First implentation : password stored "in memory"
 *
 */
public class PasswordStore extends Observable implements Serializable {


    private static final long serialVersionUID = 774794719034730233L;

    @Override
    public void notifyObservers(Object arg) {
        super.notifyObservers(arg);
    }

    /**
     * remove password by id
     * @param id
     */
    public void removePassword(String id) {
        final Password notify ;
        final Password found ;
        synchronized (passwords) {
            found = passwords.remove(id);
            if (found!=null) {
                notify = new DeletedPassword(found.getId());
                passwords.put(id,notify);
            }
            else {
                return;
            }
            setChanged();
            notifyObservers(new PasswordRemoved(notify));
        }
        try {
            found.unIndex(writer);
        } catch (IOException e) {
            logger.error("error un indexing password",e);
        }
    }

    /**
     * @return google drive refresh token if such a token exists in the data base or null
     */
    public String getGoogleDriveRefreshToken() {
        synchronized (passwords) {
            final Password found = passwords.get(GoogleDrive.GOOGLE_DRIVE_ID);
            if (found==null) return null;
            if ( !(found instanceof GoogleDrive) ) {
                passwords.remove(GoogleDrive.GOOGLE_DRIVE_ID);
                return null;
            }
            final GoogleDrive password = (GoogleDrive) found;
            return password.getRefreshToken();
        }
    }

    /**
     * save an new password in the database
     * @param p
     */
    public void savePassword(Password p) {
        synchronized (passwords) {
            passwords.put(p.getId(), p);
            setChanged();
            notifyObservers(new PasswordModifiedOrCreated(p));
        }
        try {
            p.index(writer);
        } catch (IOException e) {
            logger.error("error indexing password",e);
        }
    }

    public void reIndexeLuceneDataBase() {
        try {
            writer.deleteAll();
            writer.commit();
        }catch(IOException e) {
            logger.error("error occured during re indexation of the lucene database");
        }
        synchronized (passwords) {
            for (Map.Entry<String,Password> entry:passwords.entrySet()) {
                try {
                    entry.getValue().index(writer);
                } catch (Exception e) {
                    logger.error("unable to index a password",e);
                }
            }
        }
    }

    /**
     * merge changes retrieved from store into current store
     * @param store
     */
    public void merge(PasswordStore store) {
        synchronized (this.passwords) {
            synchronized (store.passwords) {
                final Set<Map.Entry<String, Password>> entries = store.passwords.entrySet();
                for (Map.Entry<String, Password> remoteEntry : entries) {
                    Password current = this.passwords.get(remoteEntry.getKey());
                    final Password remotePassword = remoteEntry.getValue();
                    if (current==null || current.getModified()< remotePassword.getModified()) {
                        savePassword(remotePassword);
                    }
                }
            }
        }
    }


    private void save(OutputStream os) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithType(new TypeReference<HashMap<String, Password>>(){}).writeValue(result, passwords);
        try {
            os.write(Config.getInstance().encrypt(result.toByteArray()));
        }catch(RuntimeException e) {
            throw new IOException(e);
        }
    }


    public void save() throws IOException {
        synchronized (passwords) {
            try (OutputStream os = Files.newOutputStream(storePath, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
                save(os);
            }
        }
    }

    public byte[] getFileContent() throws IOException {
        synchronized (passwords) {
            if (!passwords.isEmpty()) {
                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                save(byteArrayOutputStream);
                return byteArrayOutputStream.toByteArray();
            }
            else return null;
        }
    }

    /**
     * construct a password store from byte array
     * @param fromMemory : decrypted password store content
     * @param config
     * @throws IOException
     */
    public PasswordStore(byte[] fromMemory,Config config) throws IOException {
        try {
            init(null, fromMemory, config);
        } catch (MainPasswordError e) {
            throw new RuntimeException("no such exception should be thrown there - programmatic error detected ?",e);
        }
    }

    public PasswordStore(Path fromFile,Config config) throws IOException,MainPasswordError {
        init(fromFile, null, config);
    }

    public PasswordStore(Path fromFile,byte[] fromMemory,Config config) throws IOException,MainPasswordError {
        init(fromFile,fromMemory,config);
    }
    private void init(Path fromFile,byte[] fromMemory,Config config) throws IOException,MainPasswordError{
        this.storePath = fromFile;
        if (this.storePath!=null) {
            this.idxPath = this.storePath.getParent().resolve(storePath.getFileName() + ".idx");
            if (Files.exists(storePath) && Files.size(storePath) != 0) {
                ObjectMapper mapper = new ObjectMapper();
                final byte[] bytes = config.decrypt(Files.readAllBytes(fromFile));
                passwords = mapper.readValue(new ByteArrayInputStream(bytes), new TypeReference<HashMap<String, Password>>() {
                });
            } else {
                passwords = new HashMap<>();
            }

            //create lucene index
            Directory dir = FSDirectory.open(this.idxPath.toFile());
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_4_10_1, analyzer);

            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            this.writer = new IndexWriter(dir, iwc);
        }
        else {
            ObjectMapper mapper = new ObjectMapper();
            passwords = mapper.readValue(new ByteArrayInputStream(fromMemory), new TypeReference<HashMap<String, Password>>() {
            });
        }
    }

    private IndexReader createReader() throws IOException {
        return DirectoryReader.open(writer,true);
    }

    public Map<String,Password> search(String query) throws IOException {
        if (query==null) {
            Map<String, Password> result = new HashMap<>();
            synchronized (passwords) {
                result.putAll(passwords);
            }
            return result;
        }
        else {
            final IndexReader reader = createReader();
            try {
                IndexSearcher searcher = new IndexSearcher(reader);

                BooleanQuery booleanQuery = new BooleanQuery();

                Query patternQuery = new WildcardQuery(new Term(Password.FIELD_TXT, "*" + query + "*"));
                booleanQuery.add(patternQuery, BooleanClause.Occur.MUST);

                Map<String, Password> result = new HashMap<>();
                synchronized (passwords) {

                    TopScoreDocCollector collector = TopScoreDocCollector.create(1000, true);

                    searcher.search(booleanQuery, collector);
                    ScoreDoc[] hits = collector.topDocs().scoreDocs;

                    for (int i = 0; i < hits.length; ++i) {
                        int docId = hits[i].doc;
                        Document d = searcher.doc(docId);
                        final Password password = passwords.get(d.get(Password.FIELD_ID));
                        if (password != null) result.put(password.getId(), password);
                    }
                }
                return result;
            } finally {
                reader.close();
            }
        }
    }


    public void close() throws IOException {
        if (this.writer!=null) this.writer.close();
    }

    public PasswordStore(Path fromFile) throws IOException, ClassNotFoundException,MainPasswordError {
        this(fromFile,Config.getInstance());
    }

    public void sendAllPasswords(Observer suscriber) {
        synchronized (passwords) {
            for (Password password : passwords.values()) {
                suscriber.update(this,new PasswordModifiedOrCreated(password));
            }
        }
    }

    protected    Map<String,Password> passwords;
    protected    Path                 storePath;
    protected    Path                 idxPath  ;
    protected    IndexWriter          writer   ;


    private final static Logger logger = Logger.getLogger(PasswordStore.class);

}
