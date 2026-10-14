package io.github.crunchy_eggshell.breadcrumb.drop;

import java.util.List;

public record Drop(String name, String structureName, long createdAt, Long expiresAt, List<Sub> subs) {

    public boolean isPermanent() {
        return expiresAt() == null;
    }

    public boolean isExpired(long now) {
        return !isPermanent() && now >= expiresAt;
    }

}
