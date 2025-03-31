package ru.netology.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Error {
    private String message;
    private Integer id;

    public static Error of(String message) {
        return new Error(message, null);
    }

    public static Error of(String message, Integer id) {
        return new Error(message, id);
    }
}
