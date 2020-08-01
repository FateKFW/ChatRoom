package com.rdz.common;

public enum MessageType {
    TEXT("TEXT"), FILE("FILE");

    private String typeStr;

    MessageType(String typeStr) {
        this.typeStr = typeStr;
    }

    public String getStr() {
        return typeStr;
    }
}
