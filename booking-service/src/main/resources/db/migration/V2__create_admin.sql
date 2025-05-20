CREATE TABLE admins (
  id       SERIAL PRIMARY KEY,
  username VARCHAR(50)  NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  roles    VARCHAR(100) NOT NULL
);

INSERT INTO admins(username, password, roles)
VALUES(
  'admin',
  '$2a$10$Fz7/2n4sQHvq7SxjxD/IdOXfrfY9hv.T2vmXEWcWnEJPH.W7sA4eO',  -- bcrypt('adminpass')
  'ADMIN'
)
ON CONFLICT(username) DO NOTHING;
