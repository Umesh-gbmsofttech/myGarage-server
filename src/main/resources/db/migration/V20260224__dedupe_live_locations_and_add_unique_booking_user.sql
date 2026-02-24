-- Remove duplicate live_locations rows, keeping the newest row (highest id)
DELETE ll
FROM live_locations ll
JOIN live_locations newer
  ON ll.booking_id = newer.booking_id
 AND ll.user_id = newer.user_id
 AND ll.id < newer.id;

-- Enforce uniqueness for one live location row per booking/user pair
ALTER TABLE live_locations
ADD CONSTRAINT uk_live_locations_booking_user UNIQUE (booking_id, user_id);
