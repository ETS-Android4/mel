package de.mel.filesync.bash;

import de.mel.core.serialize.SerializableEntity;

/**
 * Created by xor on 8/1/17.
 */
public class FsBashDetails implements SerializableEntity {
    private Long created;
    private Long modified;
    private Long iNode;
    private boolean isSymLink;
    private String symLinkTarget;
    private String name;

    public FsBashDetails() {
        //todo indifferent state
        // removing this constructor causes an indifferent state after solving a conflict and transferring files.
        // mel did not go on normally after crash and restart (Android)
    }


    public FsBashDetails(Long created, Long modified, Long iNode, boolean isSymLink, String symLinkTarget, String name) {
        this.created = created;
        this.modified = modified;
        this.iNode = iNode;
        this.isSymLink = isSymLink;
        this.symLinkTarget = symLinkTarget;
        this.name = name;
    }

    public Long getiNode() {
        return iNode;
    }

    public Long getModified() {
        return modified;
    }

    public Long getCreated() {
        return created;
    }

    public boolean isSymLink() {
        return isSymLink;
    }

    public String getSymLinkTarget() {
        return symLinkTarget;
    }

    public String getName() {
        return name;
    }
}
