-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
                                     id SERIAL PRIMARY KEY,
                                     login VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL
    );

-- Таблица файлов
CREATE TABLE IF NOT EXISTS files (
                                     id SERIAL PRIMARY KEY,
                                     filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    size BIGINT NOT NULL,
    hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- Индексы
CREATE INDEX IF NOT EXISTS idx_files_user_id ON files(user_id);
CREATE INDEX IF NOT EXISTS idx_files_filename ON files(filename);
CREATE UNIQUE INDEX IF NOT EXISTS idx_files_user_filename ON files(user_id, filename);