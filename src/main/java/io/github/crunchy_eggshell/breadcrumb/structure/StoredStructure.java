package io.github.crunchy_eggshell.breadcrumb.structure;

import io.github.crunchy_eggshell.breadcrumb.config.BreadCrumbConfig;
import org.bukkit.structure.Structure;

public record StoredStructure(String name, String description, int sizeX, int sizeY, int sizeZ, Integer flatnessTolerance, Structure structure) {

    public int effectiveTolerance(BreadCrumbConfig config) {
        if (flatnessTolerance() != null) return flatnessTolerance();
        return config.flatnessTolerance();
    }

}
