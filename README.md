# Cinerama

Sistema de gestión de cine: cartelera, venta de entradas con selección de butacas, dulcería, control de acceso en puerta y panel administrativo.

##Campus: Tarapoto
Docentes: Jhan Arly Sanchez Tarrillo - Sergio Omar Valladares Castillo - Joseph Ibrahim Cruz Rodriguez
Cursos: LP2-BD2-ADS
Título del proyecto:  Cinerama
Grupo: 1
Nombres de los miembros del grupo:
- Jonathan Samuel Benavides Flores
- Frocmen Andres Villacorta Meza
- Jose Alonso Flores Pinchi
- Favio Fabrizio Mejia Rojas
  
## Stack

| Parte | Tecnología |
| --- | --- |
| Backend | Spring Boot 3.4.1, Java 21, Spring Security + JWT, JPA/Hibernate |
| Base de datos | Oracle Database 21c XE |
| Frontend | Angular 18 (standalone), Bootstrap 5, SweetAlert2 |

## Requisitos

- JDK 21
- Node.js 18 o superior (probado con 20)
- Oracle 21c XE con el esquema creado y accesible en `localhost:1521/XE`

## Puesta en marcha

### Windows

```bat
Iniciar Cinerama.bat
```

### Linux / macOS

```bash
./start.sh
```

Ambos scripts compilan si hace falta, levantan backend y frontend, y abren el navegador.

### Manual

```bash
# Backend
cd backend
./mvnw -DskipTests spring-boot:run

# Frontend (otra terminal)
cd frontend
npm install
npm run start
```

- Frontend: http://localhost:4200
- Backend: http://localhost:8082/cinerama
- Swagger: http://localhost:8082/cinerama/swagger-ui/index.html

## Base de datos

Ejecutar sobre un esquema vacío, en este orden, desde `backend/db/`:

1. `01_ddl_tablas_storage.sql`
2. `02_procedimientos_reportes.sql`
3. `03_puerta_precio_cancelacion.sql`

`00_dimensionamiento_storage.txt` describe el dimensionamiento del tablespace.

La conexión está en `backend/src/main/resources/application.yaml` (usuario `cineramaBD`, esquema local). Las tablas se crean o actualizan con `ddl-auto: update` al arrancar.

## Cuentas de prueba

| Rol | Usuario | Contraseña |
| --- | --- | --- |
| Administrador | `00000001` | `admin123` |
| Empleado | `11223344` | `empleado123` |
| Cliente | `77889901` | `123456` |

## Módulos

- **Cartelera y película**: funciones por fecha, selección de butacas en sala.
- **Dulcería**: productos con stock y carrito.
- **Compra**: pago, generación de entrada con QR y comprobante.
- **Mis compras**: historial y entradas del cliente.
- **Puerta**: validación de entradas por QR.
- **Soporte**: incidencias del cliente.
- **Admin**: películas, funciones, salas, productos, promociones, clientes, incidencias, reportes y correos.

Las butacas seleccionadas se mantienen en memoria con una reserva de 10 minutos; al agotarse el tiempo o abandonar la página se liberan.

## Estructura

```
backend/
  db/                     scripts SQL de Oracle
  src/main/java/          controladores, servicios, repositorios, entidades
  src/main/resources/     configuración de Spring
frontend/
  src/app/core/           servicios, guards, modelos
  src/app/features/       páginas (cartelera, película, dulcería, admin, …)
  src/app/shared/         componentes reutilizables (header, footer, checkout, …)
```
