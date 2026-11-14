package com.auction.app.server.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileStorageRepository implements StorageRepository {

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    @Override
    public <T extends Serializable> void save(String filePath, T data) throws IOException {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("File path cannot be empty");
        }
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }

        // Khởi tạo Path trước khi lock để tránh rò rỉ lock nếu đường dẫn sai cú pháp
        Path target = Paths.get(filePath);
        Path temp = Paths.get(filePath + ".tmp");

        rwLock.writeLock().lock();
        try {
            Path parent = target.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(temp.toFile()))) {
                oos.writeObject(data);
                oos.flush();
            }

            try {
                Files.move(
                        temp,
                        target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(
                        temp,
                        target,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // Bỏ qua nếu file tạm đã được move thành công hoặc không thể xóa ngay
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T load(String filePath)
            throws IOException, ClassNotFoundException {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("File path cannot be empty");
        }

        rwLock.readLock().lock();
        try {
            File file = new File(filePath);
            if (!file.exists() || file.length() == 0) {
                return null;
            }

            try (ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(file))) {
                return (T) ois.readObject();
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
