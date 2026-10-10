package io.github.crunchy_eggshell.breadcrumb.storage;

import io.github.crunchy_eggshell.breadcrumb.BreadCrumb;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class Database implements AutoCloseable{

    private final BreadCrumb plugin;
    private final ExecutorService executor;
    private Connection connection;

    public Database(BreadCrumb plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
    }

    public void connect() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File file = new File(dataFolder, "breadcrumb.db");
        String url = "jdbc:sqlite:" + file.getAbsolutePath();
        connection = DriverManager.getConnection(url);
        createTables();
    }

    private void createTables() throws SQLException {
        try(Statement stm = connection.createStatement()) {
            stm.execute("PRAGMA foreign_keys = ON");
            stm.execute("PRAGMA journal_mode = WAL");
            stm.execute("CREATE TABLE IF NOT EXISTS structures ("
                    + " name TEXT PRIMARY KEY,"
                    + " description TEXT,"
                    + " size_x INTEGER NOT NULL,"
                    + " size_y INTEGER NOT NULL,"
                    + " size_z INTEGER NOT NULL,"
                    + " flatness_tolerance INTEGER,"
                    + " next_drop_number INTEGER NOT NULL DEFAULT 1"
                    + ");"
            );
            stm.execute("CREATE TABLE IF NOT EXISTS drops ("
                    + " name TEXT PRIMARY KEY,"
                    + " structure TEXT NOT NULL,"
                    + " created_at INTEGER NOT NULL,"
                    + " expires_at INTEGER"
                    + ");"
            );
            stm.execute("CREATE TABLE IF NOT EXISTS subs ("
                    + " name TEXT PRIMARY KEY,"
                    + " drop_name TEXT NOT NULL,"
                    + " world TEXT NOT NULL,"
                    + " x INTEGER NOT NULL,"
                    + " y INTEGER NOT NULL,"
                    + " z INTEGER NOT NULL,"
                    + " size_x INTEGER NOT NULL,"
                    + " size_y INTEGER NOT NULL,"
                    + " size_z INTEGER NOT NULL,"
                    + " FOREIGN KEY (drop_name) REFERENCES drops (name) ON DELETE CASCADE"
                    + ");"
            );
            stm.execute("CREATE TABLE IF NOT EXISTS sub_entities ("
                    + " sub_name TEXT NOT NULL,"
                    + " entity_uuid TEXT NOT NULL,"
                    + " FOREIGN KEY (sub_name) REFERENCES subs (name) ON DELETE CASCADE, PRIMARY KEY (sub_name, entity_uuid)"
                    + ");"
            );
        }
    }

    public <T> CompletableFuture<T> submit(SqlFunction<T> work) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        executor.submit(() -> {
           try {
               T t = work.apply(connection);
               completableFuture.complete(t);
           }
           catch (Exception e) {
               completableFuture.completeExceptionally(e);
           }
        });
        return completableFuture;
    }

    @Override
    public void close() throws SQLException {
        try {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (connection != null) connection.close();
        }
    }
}
