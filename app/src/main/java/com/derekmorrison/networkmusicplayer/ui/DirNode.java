package com.derekmorrison.networkmusicplayer.ui;

import com.derekmorrison.networkmusicplayer.data.NMPContract;

/**
 * Created by Derek on 8/24/2016.
 */
public class DirNode {

    private int Id;
    private int ParentId;
    private String NodeName;
    private String FilePath;
    private int NodeType;
    private int NodeStatus;

    public DirNode(){}

    public DirNode(int id, int parentId, String nodeName, String filePath, int nodeType, int nodeStatus) {
        Id = id;
        ParentId = parentId;
        NodeName = nodeName;
        FilePath = filePath;
        NodeType = nodeType;
        NodeStatus = nodeStatus;
    }

    public boolean equals(DirNode other){
        return (getId() == other.getId()
                && getParentId() == other.getParentId()
                && getNodeName().equals(other.getNodeName())
                && getFilePath().equals(other.getFilePath())
                && getNodeType() == other.getNodeType()
                && getNodeStatus() == other.getNodeStatus()
                );
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public int getParentId() {
        return ParentId;
    }

    public void setParentId(int parentId) {
        ParentId = parentId;
    }

    public String getNodeName() {
        return NodeName;
    }

    public void setNodeName(String nodeName) {
        NodeName = nodeName;
    }

    public String getFilePath() {
        return FilePath;
    }

    public void setFilePath(String filePath) {
        FilePath = filePath;
    }

    public int getNodeType() {
        return NodeType;
    }

    public void setNodeType(int nodeType) {
        NodeType = nodeType;
    }

    public int getNodeStatus() {
        return NodeStatus;
    }

    public void setNodeStatus(int nodeStatus) {
        NodeStatus = nodeStatus;
    }
}
