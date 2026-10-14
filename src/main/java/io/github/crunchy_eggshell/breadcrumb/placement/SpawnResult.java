package io.github.crunchy_eggshell.breadcrumb.placement;

import io.github.crunchy_eggshell.breadcrumb.drop.Drop;

import java.util.List;

public record SpawnResult(Drop drop, List<String> failedSubs) {
}
