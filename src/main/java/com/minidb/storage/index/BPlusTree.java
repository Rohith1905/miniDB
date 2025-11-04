package com.minidb.storage.index;

import java.nio.ByteBuffer;
import java.util.*;

public class BPlusTree {
    private static final int ORDER = 32; // Max keys per node
    
    private BTreeNode root;
    private final int indexFileId;
    
    public BPlusTree(int indexFileId) {
        this.indexFileId = indexFileId;
        this.root = new LeafNode();
    }
    
    /**
     * Search for a key
     */
    public byte[] search(int key) {
        return searchRecursive(root, key);
    }
    
    private byte[] searchRecursive(BTreeNode node, int key) {
        if (node.isLeaf()) {
            LeafNode leaf = (LeafNode) node;
            return leaf.search(key);
        } else {
            InternalNode internal = (InternalNode) node;
            BTreeNode child = internal.findChild(key);
            return searchRecursive(child, key);
        }
    }
    
    /**
     * Insert key-value pair
     */
    public void insert(int key, byte[] value) {
        SplitResult result = insertRecursive(root, key, value);
        
        if (result != null) {
            // Root split - create new root
            InternalNode newRoot = new InternalNode();
            newRoot.keys.add(result.splitKey);
            newRoot.children.add(root);
            newRoot.children.add(result.rightNode);
            root = newRoot;
        }
    }
    
    private SplitResult insertRecursive(BTreeNode node, int key, byte[] value) {
        if (node.isLeaf()) {
            LeafNode leaf = (LeafNode) node;
            leaf.insert(key, value);
            
            if (leaf.keys.size() > ORDER) {
                return splitLeaf(leaf);
            }
            return null;
        } else {
            InternalNode internal = (InternalNode) node;
            int childIndex = internal.findChildIndex(key);
            BTreeNode child = internal.children.get(childIndex);
            
            SplitResult childSplit = insertRecursive(child, key, value);
            
            if (childSplit != null) {
                internal.keys.add(childIndex, childSplit.splitKey);
                internal.children.add(childIndex + 1, childSplit.rightNode);
                
                if (internal.keys.size() > ORDER) {
                    return splitInternal(internal);
                }
            }
            return null;
        }
    }
    
    private SplitResult splitLeaf(LeafNode leaf) {
        int midPoint = leaf.keys.size() / 2;
        
        LeafNode rightLeaf = new LeafNode();
        rightLeaf.keys.addAll(leaf.keys.subList(midPoint, leaf.keys.size()));
        rightLeaf.values.addAll(leaf.values.subList(midPoint, leaf.values.size()));
        
        leaf.keys.subList(midPoint, leaf.keys.size()).clear();
        leaf.values.subList(midPoint, leaf.values.size()).clear();
        
        // Link leaves for range scan
        rightLeaf.next = leaf.next;
        leaf.next = rightLeaf;
        
        return new SplitResult(rightLeaf.keys.get(0), rightLeaf);
    }
    
    private SplitResult splitInternal(InternalNode internal) {
        int midPoint = internal.keys.size() / 2;
        int splitKey = internal.keys.get(midPoint);
        
        InternalNode rightInternal = new InternalNode();
        rightInternal.keys.addAll(internal.keys.subList(midPoint + 1, internal.keys.size()));
        rightInternal.children.addAll(internal.children.subList(midPoint + 1, internal.children.size()));
        
        internal.keys.subList(midPoint, internal.keys.size()).clear();
        internal.children.subList(midPoint + 1, internal.children.size()).clear();
        
        return new SplitResult(splitKey, rightInternal);
    }
    
    /**
     * Range scan [startKey, endKey]
     */
    public List<byte[]> rangeScan(int startKey, int endKey) {
        List<byte[]> results = new ArrayList<>();
        LeafNode leaf = findLeaf(root, startKey);
        
        while (leaf != null) {
            for (int i = 0; i < leaf.keys.size(); i++) {
                int key = leaf.keys.get(i);
                if (key >= startKey && key <= endKey) {
                    results.add(leaf.values.get(i));
                } else if (key > endKey) {
                    return results;
                }
            }
            leaf = leaf.next;
        }
        
        return results;
    }
    
    private LeafNode findLeaf(BTreeNode node, int key) {
        if (node.isLeaf()) {
            return (LeafNode) node;
        }
        
        InternalNode internal = (InternalNode) node;
        BTreeNode child = internal.findChild(key);
        return findLeaf(child, key);
    }
    
    // Inner classes
    private abstract static class BTreeNode {
        abstract boolean isLeaf();
    }
    
    private static class InternalNode extends BTreeNode {
        List<Integer> keys = new ArrayList<>();
        List<BTreeNode> children = new ArrayList<>();
        
        @Override
        boolean isLeaf() { return false; }
        
        int findChildIndex(int key) {
            int i = 0;
            while (i < keys.size() && key >= keys.get(i)) {
                i++;
            }
            return i;
        }
        
        BTreeNode findChild(int key) {
            return children.get(findChildIndex(key));
        }
    }
    
    private static class LeafNode extends BTreeNode {
        List<Integer> keys = new ArrayList<>();
        List<byte[]> values = new ArrayList<>();
        LeafNode next; // For range scans
        
        @Override
        boolean isLeaf() { return true; }
        
        byte[] search(int key) {
            int idx = Collections.binarySearch(keys, key);
            return idx >= 0 ? values.get(idx) : null;
        }
        
        void insert(int key, byte[] value) {
            int idx = Collections.binarySearch(keys, key);
            if (idx >= 0) {
                // Update existing
                values.set(idx, value);
            } else {
                // Insert new
                int insertPos = -(idx + 1);
                keys.add(insertPos, key);
                values.add(insertPos, value);
            }
        }
    }
    
    private record SplitResult(int splitKey, BTreeNode rightNode) {}
}