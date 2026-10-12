package io.github.crunchy_eggshell.breadcrumb.placement;

public enum FailReason {
    NOT_NATURAL_GROUND("The block beneath the attempt area isn't on the natural ground list in the config"),
    FLOATING_GROUND("The blocks under the surface aren't solid"),
    NOT_FLAT("The height difference is bigger than the tolerance"),
    TOO_HIGH("The build would go above the world's height limit"),
    BUFFER_BUILT("The buffer has a non-natural block nearby"),
    SPACE_BLOCKED("Something that isn't air or replaceable is in the build's area or buffer");

    private final String description;

    FailReason(String description) {
        this.description = description;
    }

    public String description() {return this.description;}
}
