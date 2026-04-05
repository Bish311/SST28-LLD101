package com.example.cache;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LRUEvictionPolicy implements EvictionPolicy {
    private final HashMap<String, DoublyLinkedNode> nodeMap;
    private final DoublyLinkedNode head;
    private final DoublyLinkedNode tail;
    private final ReentrantLock lock;

    public LRUEvictionPolicy() {
        this.nodeMap = new HashMap<>();
        this.head = new DoublyLinkedNode("HEAD_SENTINEL", null);
        this.tail = new DoublyLinkedNode("TAIL_SENTINEL", null);
        this.head.next = this.tail;
        this.tail.prev = this.head;
        this.lock = new ReentrantLock();
    }

    @Override
    public void recordAccess(String key) {
        lock.lock();
        try {
            if (nodeMap.containsKey(key)) {
                DoublyLinkedNode existing = nodeMap.get(key);
                detach(existing);
                attachToTail(existing);
                return;
            }
            DoublyLinkedNode newNode = new DoublyLinkedNode(key, null);
            attachToTail(newNode);
            nodeMap.put(key, newNode);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String evict() {
        lock.lock();
        try {
            if (head.next == tail) {
                return null;
            }
            DoublyLinkedNode lruNode = head.next;
            detach(lruNode);
            nodeMap.remove(lruNode.key);
            return lruNode.key;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void remove(String key) {
        lock.lock();
        try {
            DoublyLinkedNode node = nodeMap.get(key);
            if (node == null) {
                return;
            }
            detach(node);
            nodeMap.remove(key);
        } finally {
            lock.unlock();
        }
    }

    private void detach(DoublyLinkedNode node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        node.prev = null;
        node.next = null;
    }

    private void attachToTail(DoublyLinkedNode node) {
        node.prev = tail.prev;
        node.next = tail;
        tail.prev.next = node;
        tail.prev = node;
    }

    private static class DoublyLinkedNode {
        private final String key;
        private final String value;
        private DoublyLinkedNode prev;
        private DoublyLinkedNode next;

        DoublyLinkedNode(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
