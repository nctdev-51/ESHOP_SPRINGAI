package com.example.eshop.entity;

import java.util.HashMap;
import java.util.Map;

public class UserPreferenceProfile {
    private final Map<String, Double> vector = new HashMap<>();

    public Map<String, Double> getVector() {
        return vector;
    }

    public void add(Map<String, Double> other, double weight) {
        if (other == null) return;
        other.forEach((token, value) -> vector.merge(token, value * weight, Double::sum));
    }

    public void normalize() {
        double norm = Math.sqrt(vector.values().stream().mapToDouble(v -> v * v).sum());
        if (norm == 0) return;
        vector.replaceAll((token, value) -> value / norm);
    }
}
