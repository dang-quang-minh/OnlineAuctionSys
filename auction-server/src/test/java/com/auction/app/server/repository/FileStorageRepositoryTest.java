package com.auction.app.server.repository;

import com.auction.app.common.model.user.Bidder;
import com.auction.app.common.model.user.User;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileStorageRepositoryTest {

    @Test
    void saveAndLoadSnapshotSuccessfully(@TempDir File tempDir) throws Exception {
        File file = new File(tempDir, "snapshot.dat");
        FileStorageRepository repository = new FileStorageRepository();

        Map<String, User> users = new HashMap<>();
        users.put("u1", new Bidder("u1", "testuser", "p123", "t@example.com"));

        AuctionDataSnapshot snapshot = new AuctionDataSnapshot(users, null, null);
        repository.save(file.getAbsolutePath(), snapshot);

        AuctionDataSnapshot loaded = repository.load(file.getAbsolutePath());
        assertNotNull(loaded);
        assertEquals(1, loaded.getUsers().size());
        assertEquals("testuser", loaded.getUsers().get("u1").getUsername());
    }

    @Test
    void loadNonExistentFileReturnsNull(@TempDir File tempDir) throws Exception {
        File file = new File(tempDir, "does-not-exist.dat");
        FileStorageRepository repository = new FileStorageRepository();

        assertNull(repository.load(file.getAbsolutePath()));
    }

    @Test
    void saveAndLoadValidateArguments() {
        FileStorageRepository repository = new FileStorageRepository();

        assertThrows(IllegalArgumentException.class, () -> repository.save(null, "data"));
        assertThrows(IllegalArgumentException.class, () -> repository.save("", "data"));
        assertThrows(IllegalArgumentException.class, () -> repository.save("path", null));

        assertThrows(IllegalArgumentException.class, () -> repository.load(null));
        assertThrows(IllegalArgumentException.class, () -> repository.load("  "));
    }
}
