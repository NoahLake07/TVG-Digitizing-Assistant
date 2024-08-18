package com.thevideogoat.digitizingassistant.data;

public class Pair<K, V1, V2> {
    private K key;
    private V1 value1;
    private V2 value2;

    public Pair(K key, V1 value1, V2 value2) {
        this.key = key;
        this.value1 = value1;
        this.value2 = value2;
    }

    public K getKey() {
        return key;
    }

    public V1 getValue1() {
        return value1;
    }

    public V2 getValue2() {
        return value2;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public void setValue1(V1 value1) {
        this.value1 = value1;
    }

    public void setValue2(V2 value2) {
        this.value2 = value2;
    }
}