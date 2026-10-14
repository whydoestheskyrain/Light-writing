CREATE TABLE IF NOT EXISTS learning_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  event_id VARCHAR(100) NOT NULL,
  module_id BIGINT NULL,
  seconds INT NOT NULL,
  event_time DATETIME NOT NULL,
  UNIQUE KEY uk_learning_event_user_event (user_id, event_id),
  KEY idx_learning_event_user_time (user_id, event_time)
);
CREATE TABLE IF NOT EXISTS learning_daily_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  record_date DATE NOT NULL,
  seconds INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_learning_daily_user_date (user_id, record_date)
);
CREATE TABLE IF NOT EXISTS learning_module_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  module_id BIGINT NOT NULL,
  record_date DATE NOT NULL,
  seconds INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_learning_module_user_module_date (user_id, module_id, record_date)
);
