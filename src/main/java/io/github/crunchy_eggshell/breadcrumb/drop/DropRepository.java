package io.github.crunchy_eggshell.breadcrumb.drop;

import io.github.crunchy_eggshell.breadcrumb.storage.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class DropRepository {

    private final Database database;

    public DropRepository(Database database) {
        this.database = database;
    }

    public CompletableFuture<Void> save(Drop drop) {
        return database.submit(connection -> {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement pstm = connection.prepareStatement("INSERT INTO drops (name, structure, created_at, expires_at) VALUES (?, ?, ?, ?)")) {
                    pstm.setString(1, drop.name());
                    pstm.setString(2, drop.structureName());
                    pstm.setLong(3, drop.createdAt());
                    if (drop.expiresAt() == null) pstm.setNull(4, Types.INTEGER);
                    else pstm.setLong(4, drop.expiresAt());
                    pstm.executeUpdate();
                }
                try (PreparedStatement pstm2 = connection.prepareStatement("INSERT INTO subs (name, drop_name, world, x, y, z, size_x, size_y, size_z) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    for (Sub s : drop.subs()) {
                        pstm2.setString(1, s.name());
                        pstm2.setString(2, drop.name());
                        pstm2.setString(3, s.worldId().toString());
                        pstm2.setInt(4, s.x());
                        pstm2.setInt(5, s.y());
                        pstm2.setInt(6, s.z());
                        pstm2.setInt(7, s.sizeX());
                        pstm2.setInt(8, s.sizeY());
                        pstm2.setInt(9, s.sizeZ());
                        pstm2.addBatch();
                    }
                    pstm2.executeBatch();
                }
                try (PreparedStatement pstm3 = connection.prepareStatement("INSERT INTO sub_entities (sub_name, entity_uuid) VALUES (?, ?)")) {
                    for (Sub s : drop.subs()) {
                        for (UUID e : s.entityIds()) {
                            pstm3.setString(1, s.name());
                            pstm3.setString(2, e.toString());
                            pstm3.addBatch();
                        }
                    }
                    pstm3.executeBatch();
                    connection.commit();
                }
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
            return null;
        });
    }

    public CompletableFuture<List<Drop>> loadAll() {
        return database.submit(connection -> {
            Map<String, List<UUID>> entitiesBySub = new HashMap<>();
            try (PreparedStatement pstm = connection.prepareStatement("SELECT * FROM sub_entities")) {
                ResultSet rs = pstm.executeQuery();
                while (rs.next()) {
                    String subName = rs.getString("sub_name");
                    UUID entityUUID = UUID.fromString(rs.getString("entity_uuid"));
                    entitiesBySub.computeIfAbsent(subName, k -> new ArrayList<>()).add(entityUUID);
                }
            }
            Map<String, List<Sub>> subsByDrop = new HashMap<>();
            try (PreparedStatement pstm2 = connection.prepareStatement("SELECT * FROM subs")) {
                ResultSet rs2 = pstm2.executeQuery();
                while (rs2.next()) {
                    String subName = rs2.getString("name");
                    String dropName = rs2.getString("drop_name");
                    UUID worldUUID = UUID.fromString(rs2.getString("world"));
                    int x = rs2.getInt("x");
                    int y = rs2.getInt("y");
                    int z = rs2.getInt("z");
                    int sizeX = rs2.getInt("size_x");
                    int sizeY = rs2.getInt("size_y");
                    int sizeZ = rs2.getInt("size_z");
                    Sub sub = new Sub(subName, dropName, worldUUID, x, y, z, sizeX, sizeY, sizeZ, List.copyOf(entitiesBySub.getOrDefault(subName, new ArrayList<>())));
                    subsByDrop.computeIfAbsent(dropName, k -> new ArrayList<>()).add(sub);
                }
            }
            List<Drop> drops = new ArrayList<>();
            try (PreparedStatement pstm3 = connection.prepareStatement("SELECT * FROM drops")) {
                ResultSet rs3 = pstm3.executeQuery();
                while (rs3.next()) {
                    String dropName = rs3.getString("name");
                    String structure = rs3.getString("structure");
                    long createdAt = rs3.getLong("created_at");
                    Long expiresAt = rs3.getLong("expires_at");
                    if (rs3.wasNull()) expiresAt = null;
                    Drop drop = new Drop(dropName, structure, createdAt, expiresAt, List.copyOf(subsByDrop.getOrDefault(dropName, new ArrayList<>())));
                    drops.add(drop);
                }
                return drops;
            }
        });
    }

    public CompletableFuture<Void> deleteDrop(String name) {
        return database.submit(connection -> {
            try (PreparedStatement pstm = connection.prepareStatement("DELETE FROM drops WHERE name = ?")) {
                pstm.setString(1, name);
                pstm.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<Void> deleteSub(String name) {
        return database.submit(connection -> {
            try (PreparedStatement pstm = connection.prepareStatement("DELETE FROM subs WHERE name = ?")) {
                pstm.setString(1, name);
                pstm.executeUpdate();
            }
            return null;
        });
    }
}
