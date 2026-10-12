package io.github.crunchy_eggshell.breadcrumb.placement;

import org.bukkit.Location;

public record CheckResult(Location location, FailReason reason, String detail) {

    public static CheckResult success(Location location) {
        return new CheckResult(location, null, "");
    }

    public static CheckResult fail(FailReason reason, String detail) {
        return new CheckResult(null, reason, detail);
    }

    public boolean passed() {
        return this.location() != null;
    }

}
