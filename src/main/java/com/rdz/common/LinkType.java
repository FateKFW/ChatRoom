package com.rdz.common;

public enum LinkType {
    CONNECTION("CONN"), INIT("INIT"), MESSAGE("MESS"), CLOSE("CLOS");

    private String typeStr;

    LinkType(String typeStr) {
        this.typeStr = typeStr;
    }

    public String getStr() {
        return typeStr;
    }
}