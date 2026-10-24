package br.com.medclinic.application;

import java.util.List;

public record Page<T>(List<T> items, int number, int total) {
    public static <T> Page<T> of(List<T> items, int requestedPage) {
        int number = Math.max(0, Math.min(requestedPage, Math.max(0, (items.size() - 1) / 20)));
        return new Page<>(items.stream().skip((long) number * 20).limit(20).toList(), number, items.size());
    }

    public boolean hasPrevious() {
        return number > 0;
    }

    public boolean hasNext() {
        return (long) (number + 1) * 20 < total;
    }
}
