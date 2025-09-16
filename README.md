Semana 5 – Distribuidora de Alimentos (Android/Java)

Aplicación Android desarrollada en Java con Android Studio para la asignatura Taller de Aplicaciones Móviles.
Partimos del trabajo de la Semana 4 (cálculo de despacho y conversión a radianes) y en Semana 5 incorporamos geolocalización y cálculo de distancia hacia Plazas de Armas usando Haversine.

🚀 Funcionalidades
Núcleo (Semana 4)
Formulario para ingresar:
Monto de compra (CLP)
Distancia (km) (0 a 20)
Grados (para conversión a radianes)

Cálculos:
Costo de despacho según reglas del caso.
Conversión de grados → radianes.
Resultados visibles en pantalla + logs en Logcat.
Botón “Ingresar con Gmail (próximamente)” (prototipo, deshabilitado).


Extensión (Semana 5)

Obtención de ubicación del dispositivo (coordenadas lat/lon).
Selección de Plaza de Armas (Spinner con ciudades).
Cálculo de distancia (km) entre el usuario y la plaza seleccionada con fórmula de Haversine.
UI mejorada con CardView (secciones separadas: compra y geolocalización).

Compatibilidad
Probado en API 21 (Lollipop) y API 26 (Oreo).

🧭 Nota importante (emulador)
En el emulador no hay GPS real. Si ves distancias irreales (ej. ~9000 km), define la ubicación manualmente:
Emulador → ⋮ Extended controls → Location → ingresa lat/lon reales → Set Location.
En un teléfono real la ubicación se obtiene automáticamente al conceder permisos.


🛠️ Requisitos y entorno
Android Studio actualizado
Emuladores API 21 y API 26
SDKs / System Images instaladas (SDK Manager)
AVDs configurados (Device Manager)
Conexión a internet para sincronizar Gradle

▶️ Cómo ejecutar

Abrir el proyecto en Android Studio.
Sync Gradle y esperar a que resuelva dependencias.
En AVD Manager, iniciar un emulador (API 21 o 26).
(Opcional/Emulador) Set Location en Extended controls → Location.
Presionar Run (▶) y seleccionar el dispositivo.
Probar:
Cálculo de despacho y radianes (sección 1).
Obtener ubicación, elegir Plaza y calcular distancia (sección 2).

✅ Requerimientos (resumen)

Funcionales

Ingresar monto, distancia y grados.
Calcular costo de despacho (reglas del caso).
Convertir grados a radianes.
Obtener ubicación del dispositivo.
Seleccionar Plaza de Armas y calcular distancia (Haversine).
Mostrar resultados en pantalla.

No funcionales
Ejecutarse correctamente en API 21 y API 26.
Gestionar permisos de ubicación.
UI organizada con secciones (CardView).
Código comentado línea por línea.

👨‍💻 Autores
Karla Pesce · Jaime Codoceo · Sergio Molina
Asignatura: Taller de Aplicaciones Móviles – Semana 5

Karla Pesce · Jaime Codoceo · Sergio Molina

Asignatura: Taller de Aplicaciones Móviles – Semana 5
