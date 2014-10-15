package org.beynet.model.store;

import org.beynet.model.password.Password;

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

    public PasswordStore() {
        passwords = new HashMap<>();
    }


    /**
     * remove password by id
     * @param id
     */
    public void removePassword(String id) {
        synchronized (passwords) {
            final Password password = passwords.remove(id);
            setChanged();
            notifyObservers(new PasswordRemoved(password));
        }
    }

    public void savePassword(Password p) {
        synchronized (passwords) {
            passwords.put(p.getId(), p);
            setChanged();
            notifyObservers(new PasswordModifiedOrCreated(p));
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

    public void save(Path targetFile) throws IOException {
        synchronized (passwords) {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(targetFile, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE))) {
                objectOutputStream.writeObject(this);
            }
        }
    }

    public static PasswordStore fromFile(Path fromFile) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(fromFile))) {
            return (PasswordStore)ois.readObject();
        }
    }

    protected final Map<String,Password> passwords;

}
