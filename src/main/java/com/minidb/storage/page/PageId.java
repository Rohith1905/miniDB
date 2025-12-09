package com.minidb.storage.page;

import java.io.Serializable;

/**
 * PageId uniquely identifies a page: (fileId, pageNumber)
 */
public record PageId(int fileId, int pageNumber) implements Serializable {
    @Override
    public String toString() {
        return String.format("Page(%d:%d)", fileId, pageNumber);
    }
}