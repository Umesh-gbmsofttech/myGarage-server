ALTER TABLE bookings
  ADD COLUMN service_completed_at DATETIME NULL,
  ADD COLUMN report_description VARCHAR(4000) NULL,
  ADD COLUMN report_created_at DATETIME NULL,
  ADD COLUMN reporter_user_id BIGINT NULL;

ALTER TABLE bookings
  ADD CONSTRAINT fk_bookings_reporter_user
  FOREIGN KEY (reporter_user_id) REFERENCES users(id);
