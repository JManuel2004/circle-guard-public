-- Extend buildings with coordinates and address
ALTER TABLE buildings ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE buildings ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE buildings ADD COLUMN address TEXT;
