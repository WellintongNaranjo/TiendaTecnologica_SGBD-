# Tienda Tecnológica - Sistema de Gestión de Inventario y Ventas

Sistema de escritorio desarrollado en JavaFX para la gestión integral de una tienda de tecnología. La aplicación implementa una arquitectura por capas (MVC / Servicio-Repositorio) y guarda sus datos en una base de datos **MySQL 8** mediante **JDBC**. Es la evolución del Proyecto 1 (que usaba archivos CSV) para el curso EIF206 - Programación III.

## Características Principales

- **Gestión de Productos y Proveedores:** Registro, edición y catálogo de productos y proveedores, con la relación N:M entre ambos (tabla `producto_proveedor`).
- **Control de Inventarios:** Entradas, salidas y ajustes de stock. El stock de un producto solo cambia por movimientos de inventario, nunca al editar el producto.
- **Gestión de Ventas y Compras:** Ventas con clientes, carrito y pago; órdenes de compra a proveedores. Anular una venta devuelve al inventario solo las unidades que no regresaron antes por devoluciones aprobadas.
- **Garantías y Devoluciones:** Seguimiento del ciclo de vida de garantías y devoluciones, con reintegro automático al inventario cuando una devolución se aprueba.
- **Generación de Reportes:** Reportes y estadísticas operativas, con exportación a CSV.
- **Persistencia en MySQL:** Esquema relacional normalizado hasta 3FN, acceso con `PreparedStatement` y operaciones de varios pasos dentro de transacciones.
- **Interfaz fluida:** Las consultas más lentas (cargas de tablas, anulación de ventas, recepción de compras, garantías, devoluciones y reportes) corren en un hilo aparte con `Task` de JavaFX.

## Estructura del Proyecto

```
TiendaTecnologica/
├── src/
│   ├── sql/
│   │   ├── schema.sql                 # Crea la base de datos y las tablas (re-ejecutable)
│   │   ├── seed.sql                   # Datos de ejemplo
│   │   ├── crear_usuario_app.sql      # Usuario MySQL de la aplicación
│   │   ├── tienda_tecnologica.mwb     # Modelo de MySQL Workbench
│   │   └── diagrama_er.png            # Diagrama entidad-relación
│   └── main/
│       ├── java/com/tiendatecnologica/
│       │   ├── Main.java
│       │   ├── controller/            # Controladores JavaFX (+ EjecutorEnSegundoPlano)
│       │   ├── enums/                 # Estados, métodos de pago y tipos de movimiento
│       │   ├── model/                 # Clases del dominio
│       │   ├── persistence/           # ConexionBD, Transaccion, RepositorioException
│       │   ├── repository/            # Interfaces Repository y sus implementaciones JDBC
│       │   ├── service/               # Lógica de negocio y validaciones
│       │   └── util/                  # Validador, GeneradorId, ExportadorCsv, Constantes
│       └── resources/
│           ├── fxml/                  # Vistas FXML
│           ├── css/styles.css
│           └── db.properties.example  # Plantilla de la conexión a MySQL
├── pom.xml
└── README.md
```

## Tecnologías Utilizadas

- **Lenguaje:** Java 17 (o superior)
- **Interfaz Gráfica:** JavaFX 21 (FXML y CSS)
- **Gestor de Dependencias y Construcción:** Apache Maven
- **Persistencia:** MySQL 8 con JDBC (`mysql-connector-j`)
- **Modelado de datos:** MySQL Workbench

## Requisitos Previos

- JDK 17 o superior instalado y configurado en el sistema.
- Maven instalado (o hacer uso del wrapper incluido `./mvnw`).
- MySQL 8 o superior y MySQL Workbench.
- Opcional: Scene Builder para la edición gráfica de las vistas `.fxml`.

## Configuración de la Base de Datos

1. En MySQL Workbench, abra y ejecute completo `src/sql/schema.sql`. Crea la base `tienda_tecnologica` y todas las tablas.
2. Ejecute `src/sql/seed.sql` para cargar los datos de ejemplo.
3. Abra `src/sql/crear_usuario_app.sql`, cambie `CAMBIAR_CLAVE` por su propia clave y ejecútelo como `root`. Crea el usuario `tienda_app` con permisos solo sobre esta base.
4. Copie `src/main/resources/db.properties.example` como `src/main/resources/db.properties` y escriba su usuario y clave. Este archivo no se sube a Git.

## Instalación y Ejecución

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/ElAkemi/TiendaTecnologica.git
   cd TiendaTecnologica
   ```

2. **Configurar la base de datos** (ver la sección anterior).

3. **Compilar el proyecto con Maven:**
   ```bash
   ./mvnw clean compile
   ```

4. **Ejecutar la aplicación:**
   ```bash
   ./mvnw javafx:run
   ```

## Modelo de Datos (MySQL)

El esquema está en `src/sql/schema.sql` y tiene 11 tablas:

- `producto`, `proveedor` y `producto_proveedor` (N:M): catálogo y quién surte cada producto.
- `cliente`, `venta` y `detalle_venta`: ventas, clientes e ítems vendidos.
- `orden_compra` y `detalle_compra`: órdenes de compra a proveedores.
- `movimiento_inventario`: histórico de cambios del stock (entradas, salidas y ajustes).
- `garantia` y `devolucion`: seguimiento posventa.

## Notas de diseño

- **Stock:** `producto.cantidad_disponible` solo lo modifica el inventario (ventas, compras, devoluciones aprobadas, anulaciones y ajustes), siempre dejando un movimiento. Por eso el `UPDATE` de un producto no incluye esa columna y el campo de cantidad queda de solo lectura al editar.
- **Transacciones:** vender, anular una venta, recibir una compra y aprobar una devolución guardan todos sus cambios juntos o ninguno.
- **Hilos:** `EjecutorEnSegundoPlano` ejecuta una operación en un `Task` y entrega el resultado o el error en el hilo de JavaFX; ignora solicitudes repetidas mientras la anterior sigue en curso.
