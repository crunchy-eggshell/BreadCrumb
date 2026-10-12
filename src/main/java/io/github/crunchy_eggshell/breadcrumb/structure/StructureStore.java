package io.github.crunchy_eggshell.breadcrumb.structure;

import io.github.crunchy_eggshell.breadcrumb.BreadCrumb;
import io.github.crunchy_eggshell.breadcrumb.selection.Selection;
import io.github.crunchy_eggshell.breadcrumb.storage.Database;
import org.bukkit.structure.Structure;
import org.bukkit.util.BlockVector;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.*;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class StructureStore {

    private final BreadCrumb plugin;
    private final Database database;
    private final File folder;
    private final Map<String, StoredStructure> structures;

    public StructureStore(BreadCrumb plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        this.folder = new File(plugin.getDataFolder(), "structures");
        folder.mkdirs();
        this.structures = new ConcurrentHashMap<>();
    }

    public CompletableFuture<Void> loadAll() {
            return database.submit(connection -> {
                try (Statement stm = connection.createStatement()) {
                    ResultSet rs = stm.executeQuery("SELECT * FROM structures");
                    while (rs.next()) {
                        String name = rs.getString("name");
                        String description = rs.getString("description");
                        int sizeX = rs.getInt("size_x");
                        int sizeY = rs.getInt("size_y");
                        int sizeZ = rs.getInt("size_z");
                        Integer flatnessTolerance = rs.getInt("flatness_tolerance");
                        if (rs.wasNull()) flatnessTolerance = null;
                        File file = new File(folder, name + ".nbt");
                        Structure structure = null;
                        if (file.exists()) {
                            try {
                                structure = plugin.getServer().getStructureManager().loadStructure(file);
                            } catch (IOException e) {
                                plugin.getLogger().warning("Something went wrong: " + e);
                                continue;
                            }
                        } else {
                            plugin.getLogger().warning("Missing structure file: " + file.getName());
                            continue;
                        }
                        StoredStructure storedStructure = new StoredStructure(name, description, sizeX, sizeY, sizeZ, flatnessTolerance, structure);
                        structures.put(name, storedStructure);
                    }
                    }
                return null;
            });
    }

    public boolean exists(String name) {
        name = name.toLowerCase();
        return structures.containsKey(name);
    }

    public StoredStructure get(String name) {
        name = name.toLowerCase();
        return structures.get(name);
    }

    public Collection<StoredStructure> all() {
        return structures.values();
    }

    public CompletableFuture<StoredStructure> save(String name, String description, Integer flatnessTolerance, Selection selection) {
        String key = name.toLowerCase();
        Structure structure = plugin.getServer().getStructureManager().createStructure();
        structure.fill(selection.min(), new BlockVector(selection.sizeX(), selection.sizeY(), selection.sizeZ()), true);
        StoredStructure storedStructure = new StoredStructure(key, description, selection.sizeX(), selection.sizeY(), selection.sizeZ(), flatnessTolerance, structure);
        return database.submit(connection -> {
            File file = new File(folder, key + ".nbt");
            try {
                plugin.getServer().getStructureManager().saveStructure(file, structure);
                try (PreparedStatement pstm = connection.prepareStatement("INSERT INTO structures (name, description, size_x, size_y, size_z, flatness_tolerance, next_drop_number) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    pstm.setString(1, key);
                    pstm.setString(2, description);
                    pstm.setInt(3, selection.sizeX());
                    pstm.setInt(4, selection.sizeY());
                    pstm.setInt(5, selection.sizeZ());
                    if (flatnessTolerance == null) {
                        pstm.setNull(6, Types.INTEGER);
                    } else pstm.setInt(6, flatnessTolerance);
                    pstm.setInt(7, 1);
                    pstm.executeUpdate();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return storedStructure;
        }).thenApply(saved -> {
            structures.put(key, saved);
            return saved;
        });
    }

    public CompletableFuture<Void> delete(String name) {
        String key = name.toLowerCase();
        structures.remove(key);
        return database.submit(connection -> {
            File file = new File(folder, key + ".nbt");
            file.delete();
            try (PreparedStatement pstm = connection.prepareStatement("DELETE FROM structures WHERE name = ?")) {
                pstm.setString(1, key);
                pstm.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<Integer> reserveDropNumber(String name) {
        String key = name.toLowerCase();
        return database.submit(connection -> {
            int nextDropNumber = 0;
            try (PreparedStatement pstm = connection.prepareStatement("SELECT next_drop_number FROM structures WHERE name = ?")) {
                pstm.setString(1, key);
                try (ResultSet rs = pstm.executeQuery()) {
                    if (rs.next()) {
                        nextDropNumber = rs.getInt("next_drop_number");
                        try (PreparedStatement ps = connection.prepareStatement("UPDATE structures SET next_drop_number = ? WHERE name = ?")) {
                            ps.setInt(1, nextDropNumber + 1);
                            ps.setString(2, key);
                            ps.executeUpdate();
                        }
                    } else {
                        throw new SQLException("structure was not found");
                    }
                }
            }
            return nextDropNumber;
        });
    }
}
