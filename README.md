# 🚌 TFG Bus Platform

Prototipo web de **gestión de transporte de autobuses** orientado al viajero interurbano en el entorno de Jaén. Centraliza la consulta de líneas, rutas, paradas y horarios, ofrece un planificador de viaje, visualización en mapa interactivo y (en iteraciones siguientes) reservas, tickets electrónicos y notificaciones.

> Trabajo Fin de Grado — Grado en Ingeniería Informática · Escuela Politécnica Superior de Jaén · Universidad de Jaén.

---

## 📋 Tabla de contenidos

- [Características](#-características)
- [Stack tecnológico](#-stack-tecnológico)
- [Estructura del repositorio](#-estructura-del-repositorio)
- [Requisitos previos](#-requisitos-previos)
- [Puesta en marcha](#-puesta-en-marcha)
- [Endpoints disponibles](#-endpoints-disponibles)
- [Datos de ejemplo](#-datos-de-ejemplo)
- [Configuración por perfiles](#-configuración-por-perfiles)
- [Flujo de trabajo con Git](#-flujo-de-trabajo-con-git)
- [Roadmap](#-roadmap)
- [Autor](#-autor)

---

## ✨ Características

- 🔐 **Autenticación** con JWT (registro, login, logout).
- 🚌 **Consulta de líneas y paradas** del entorno de Jaén.
- 🗺️ **Mapa interactivo** con Leaflet + OpenStreetMap (tiles CartoDB Positron).
- 🔎 **Planificador de viaje** origen → destino con horarios calculados.
- 🌐 **Internacionalización** ES / EN con cambio en caliente.
- 🎨 **Diseño responsive** con Angular Material (M3).

---

## 🛠 Stack tecnológico

| Capa | Tecnologías |
|------|-------------|
| **Frontend** | Angular 21 (standalone), TypeScript 5.9, SCSS, Angular Material 21, ngx-translate 17, Leaflet 1.9.4 |
| **Backend** | Java 21, Spring Boot 3.4.5, Spring Web, Spring Security, Spring Data JPA, Spring Mail |
| **Autenticación** | JSON Web Tokens (jjwt 0.12.6) |
| **Persistencia** | H2 en memoria (desarrollo) · PostgreSQL (entorno de pruebas) |
| **Build** | Maven (backend) · Angular CLI / Vite (frontend) |
| **Control de versiones** | Git + GitHub |

---

## 📁 Estructura del repositorio

```
tfg-bus-platform/
├── backend/                         # Aplicación Spring Boot
│   ├── src/main/java/com/tfg/busplatform/
│   │   ├── BusPlatformApplication.java
│   │   ├── config/                  # Spring Security, CORS
│   │   ├── controller/              # AuthController
│   │   ├── dto/                     # Request/response objects
│   │   ├── exception/               # GlobalExceptionHandler
│   │   ├── model/                   # Entidades User, Role
│   │   ├── repository/              # JpaRepositories
│   │   ├── security/                # JwtUtil, JwtAuthFilter
│   │   ├── service/                 # AuthService
│   │   └── transport/               # Módulo de transporte (iteración 2)
│   │       ├── controller/
│   │       ├── dto/
│   │       ├── model/
│   │       ├── repository/
│   │       └── service/
│   ├── src/main/resources/
│   │   ├── application.yml          # Configuración común
│   │   ├── application-dev.yml      # Perfil dev (H2)
│   │   └── application-postgres.yml # Perfil postgres
│   └── pom.xml
│
├── frontend/                        # Aplicación Angular
│   ├── src/
│   │   ├── app/
│   │   │   ├── core/                # Servicios, guards, interceptores, modelos
│   │   │   ├── features/            # Vistas (home, auth, lines, map, planner...)
│   │   │   └── shared/              # Componentes reutilizables (navbar)
│   │   ├── assets/i18n/             # es.json, en.json
│   │   ├── environments/
│   │   ├── index.html
│   │   ├── main.ts
│   │   └── styles.scss
│   ├── angular.json
│   └── package.json
│
├── .gitignore
└── README.md
```

---

## 📦 Requisitos previos

| Herramienta | Versión mínima | Notas |
|-------------|----------------|-------|
| **Java JDK** | 21 | Recomendado: [Eclipse Temurin 21](https://adoptium.net/) |
| **Maven** | 3.9+ | Opcional — el proyecto incluye `mvnw` (wrapper) |
| **Node.js** | 20 LTS o superior | Recomendado: 22 LTS |
| **npm** | 10+ | Viene con Node.js |
| **Git** | Cualquier versión reciente | |

> No es necesario instalar PostgreSQL para arrancar: el perfil `dev` usa H2 en memoria.

---

## 🚀 Puesta en marcha

### 1. Clonar el repositorio

```bash
git clone https://github.com/maci0002/tfg-bus-platform.git
cd tfg-bus-platform
```

### 2. Arrancar el backend

```powershell
cd backend

# Compilar
.\mvnw.cmd clean compile

# Arrancar con perfil dev (H2 en memoria — por defecto)
.\mvnw.cmd spring-boot:run
```

El backend queda disponible en **http://localhost:8080/api**.

> 💡 La consola H2 está disponible en `http://localhost:8080/api/h2-console`
> · JDBC URL: `jdbc:h2:mem:bus_platform`
> · Usuario: `sa`
> · Contraseña: *(en blanco)*

### 3. Arrancar el frontend

En otra terminal:

```powershell
cd frontend

# Instalar dependencias (solo la primera vez)
npm install

# Arrancar el servidor de desarrollo
npm start
```

El frontend queda disponible en **http://localhost:4200**.

### 4. Probar la aplicación

| Pantalla | URL |
|----------|-----|
| Inicio (con planificador) | http://localhost:4200/ |
| Listado de líneas | http://localhost:4200/lines |
| Mapa interactivo | http://localhost:4200/map |
| Registro de usuario | http://localhost:4200/register |
| Inicio de sesión | http://localhost:4200/login |

---

## 🔌 Endpoints disponibles

### Autenticación (`/api/auth`)
| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| POST | `/register` | Registro de nuevo usuario | ❌ |
| POST | `/login` | Inicio de sesión, devuelve JWT | ❌ |

### Transporte (`/api/transport`)
| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| GET | `/lines` | Listado de todas las líneas | ❌ |
| GET | `/lines/{id}` | Detalle de una línea (paradas + horarios) | ❌ |
| GET | `/stops` | Listado de paradas con coordenadas | ❌ |
| GET | `/search?origin=&destination=&date=&time=` | Búsqueda de trayectos | ❌ |

#### Ejemplo de búsqueda

```bash
curl "http://localhost:8080/api/transport/search?origin=Jaén&destination=Úbeda&time=09:00"
```

---

## 🌍 Datos de ejemplo

El backend carga automáticamente al arrancar (idempotente — solo si la BD está vacía):

- **16 paradas** del entorno de Jaén (Jaén, Mancha Real, Baeza, Úbeda, Linares, Andújar, Martos, Cazorla, etc.).
- **7 líneas interurbanas** con horarios y recorridos completos:

| Código | Recorrido | Salidas/día |
|--------|-----------|-------------|
| L01 | Jaén → Mancha Real → Baeza → Úbeda | 6 |
| L02 | Jaén → Mengíbar → Espelúy → Linares | 5 |
| L03 | Jaén → Mancha Real → Baeza | 5 |
| L04 | Jaén → Mengíbar → Villanueva → Andújar | 5 |
| L05 | Jaén → Torredelcampo → Torredonjimeno → Martos | 8 |
| L06 | Jaén → Torredelcampo → Martos → Alcaudete | 3 |
| L07 | Jaén → Mancha Real → Baeza → Peal → Cazorla | 3 |

Las coordenadas son aproximadas pero realistas (referenciadas a OpenStreetMap).

---

## ⚙️ Configuración por perfiles

El backend usa **Spring Profiles** para separar entornos:

### Perfil `dev` (por defecto)
- Base de datos H2 en memoria — sin necesidad de instalar nada.
- Consola H2 habilitada en `/api/h2-console`.
- `ddl-auto: update` — el esquema se crea automáticamente.

### Perfil `postgres`
Para usar PostgreSQL en local:

```powershell
# Crear la BD primero (en psql)
# CREATE DATABASE bus_platform;

$env:SPRING_PROFILES_ACTIVE="postgres"
.\mvnw.cmd spring-boot:run
```

Configuración por defecto en `application-postgres.yml`:
- URL: `jdbc:postgresql://localhost:5432/bus_platform`
- Usuario: `postgres`
- Contraseña: `postgres`

---

## 🌿 Flujo de trabajo con Git

A partir de la Iteración 3 cada bloque funcional se desarrolla en su propia rama:

```
main                                      ← rama estable
 │
 ├── feature/iteration-03-bookings        ← reservas + asientos
 ├── feature/iteration-04-payment-ticket  ← pago simulado + ticket QR
 ├── feature/iteration-05-notifications   ← notificaciones, FAQ, contacto
 └── feature/iteration-06-final           ← integración y pruebas finales
```

**Convención de commits** (Conventional Commits simplificado):

| Prefijo | Uso |
|---------|-----|
| `feat:` | Nueva funcionalidad |
| `fix:` | Corrección de error |
| `refactor:` | Cambio de código sin cambio funcional |
| `docs:` | Documentación |
| `chore:` | Configuración, dependencias, etc. |

---

## 🗺️ Roadmap

- [x] **Iteración 1** — Setup + Autenticación con JWT
- [x] **Iteración 2** — Consulta de transporte + Mapa interactivo
- [ ] **Iteración 3** — Reservas con selección de asiento
- [ ] **Iteración 4** — Pago simulado + Ticket electrónico con QR
- [ ] **Iteración 5** — Notificaciones por correo, FAQ y formulario de contacto
- [ ] **Iteración 6** — Integración, pruebas finales y validación

---

## 👤 Autor

**Miguel Ángel Carrasco Infante**
Grado en Ingeniería Informática — Universidad de Jaén
Tutor: José Ignacio Gómez Espínola

---

## 📄 Licencia

Proyecto académico desarrollado como Trabajo Fin de Grado.
