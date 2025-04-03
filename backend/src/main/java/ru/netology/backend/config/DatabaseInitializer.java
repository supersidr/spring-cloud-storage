package ru.netology.backend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            // Проверяем существует ли база данных
            jdbcTemplate.execute("SELECT 1 FROM pg_database WHERE datname = 'cloudservice'");
        } catch (Exception e) {
            // Если база не существует, создаем её
            jdbcTemplate.execute("CREATE DATABASE cloudservice");

            // Подключаемся к новой базе и создаем схему
            jdbcTemplate.execute("\\c cloudservice");

            // Создаем таблицы
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "id SERIAL PRIMARY KEY, " +
                            "login VARCHAR(255) NOT NULL UNIQUE, " +
                            "password VARCHAR(255) NOT NULL)");

            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS tokens (" +
                            "id SERIAL PRIMARY KEY, " +
                            "token VARCHAR(255) NOT NULL UNIQUE, " +
                            "user_id BIGINT NOT NULL REFERENCES users(id), " +
                            "expiry_date TIMESTAMP NOT NULL, " +
                            "active BOOLEAN NOT NULL DEFAULT TRUE)");

            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS files (" +
                            "id SERIAL PRIMARY KEY, " +
                            "filename VARCHAR(255) NOT NULL, " +
                            "storage_filename VARCHAR(255) NOT NULL, " +
                            "size BIGINT NOT NULL, " +
                            "user_id BIGINT NOT NULL REFERENCES users(id), " +
                            "UNIQUE (filename, user_id))");

            // Создаем индексы
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_tokens_token ON tokens(token)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_files_user_id ON files(user_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_tokens_user_id ON tokens(user_id)");

            // Добавляем тестового пользователя
            jdbcTemplate.execute(
                    "INSERT INTO users (login, password) VALUES " +
                            "('user', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a')");
        }
    }
}