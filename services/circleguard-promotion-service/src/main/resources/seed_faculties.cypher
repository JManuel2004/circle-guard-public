// Seed mock users for the new faculties
UNWIND [
  {name: 'Faculty of Administrative and Economic Sciences (Negocios y Economía)', healthy: 120, suspect: 15, probable: 5, confirmed: 2},
  {name: 'Faculty of Engineering, Design and Applied Sciences (Barberi de Ingeniería, Diseño y Ciencias Aplicadas)', healthy: 250, suspect: 25, probable: 10, confirmed: 8},
  {name: 'Faculty of Law and Social Sciences (Ciencias Jurídicas y Sociales)', healthy: 180, suspect: 10, probable: 3, confirmed: 1},
  {name: 'Faculty of Natural Sciences (Ciencias Naturales)', healthy: 90, suspect: 8, probable: 2, confirmed: 4},
  {name: 'Faculty of Health Sciences (Ciencias de la Salud)', healthy: 150, suspect: 30, probable: 15, confirmed: 12},
  {name: 'School of Education (Escuela de Educación)', healthy: 70, suspect: 5, probable: 1, confirmed: 0}
] AS dept
// Create Healthy Users
FOREACH (i IN range(1, dept.healthy) |
  MERGE (u:User {anonymousId: 'user-healthy-' + dept.name + '-' + i})
  SET u.department = dept.name, u.status = 'ACTIVE', u.statusUpdatedAt = timestamp()
)
// Create Suspect Users
FOREACH (i IN range(1, dept.suspect) |
  MERGE (u:User {anonymousId: 'user-suspect-' + dept.name + '-' + i})
  SET u.department = dept.name, u.status = 'SUSPECT', u.statusUpdatedAt = timestamp()
)
// Create Probable Users
FOREACH (i IN range(1, dept.probable) |
  MERGE (u:User {anonymousId: 'user-probable-' + dept.name + '-' + i})
  SET u.department = dept.name, u.status = 'PROBABLE', u.statusUpdatedAt = timestamp()
)
// Create Confirmed Users
FOREACH (i IN range(1, dept.confirmed) |
  MERGE (u:User {anonymousId: 'user-confirmed-' + dept.name + '-' + i})
  SET u.department = dept.name, u.status = 'CONFIRMED', u.statusUpdatedAt = timestamp()
);
