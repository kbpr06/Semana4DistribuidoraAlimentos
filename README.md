Distribuidora de Alimentos – Aplicación Android (Proyecto Final)

Este repositorio contiene el proyecto académico desarrollado en Android Studio, correspondiente al ramo Desarrollo de Aplicaciones Móviles.
La aplicación Distribuidora de Alimentos fue creada de manera incremental a lo largo de las semanas del curso, integrando diversas tecnologías y módulos funcionales hasta llegar a una versión completa y funcional en la Semana 9.

🚀 Funcionalidades implementadas
🔐 Autenticación de usuarios

Registro e inicio de sesión con Firebase Authentication.

Validación de credenciales y manejo de errores.

Redirección automática al menú principal tras el login exitoso.

🛒 Gestión de productos y carrito de compras

Selección de productos desde un catálogo dinámico (Spinner).

Cálculo automático de subtotal y visualización de líneas en un ListView.

Guardado de carritos y pedidos en Firebase Realtime Database.

Transferencia automática del subtotal al módulo de despacho.

🚚 Módulo de despacho

Obtención de la ubicación actual mediante GPS (permisos de ubicación).

Cálculo de la distancia entre el usuario y una ciudad destino usando la fórmula de Haversine.

Cálculo automático del costo de envío según monto y distancia.

Confirmación del pedido y registro del despacho completo en Firebase.

Visualización de resultados (distancia, costo y total) en tiempo real.

🌡️ Monitoreo de temperatura (IoT + Python)

Lectura continua de datos desde Firebase enviados por un script Python que simula un sensor de temperatura.

Actualización automática del valor en la interfaz Android.

Alertas visuales y notificaciones push cuando la temperatura sale del rango permitido (−18 °C a +8 °C).

Botón para probar alarmas manualmente y simular lecturas locales.

📦 Seguimiento de pedidos

Línea de tiempo visual con cuatro estados: Recibido, Preparando, En camino y Entregado.

Opción para simular avances de estado.

Guardado y lectura del progreso desde Firebase en tiempo real.

Diseño con CardView para una interfaz moderna y ordenada.

🧩 Arquitectura de la aplicación

La aplicación se organiza en Activities independientes, cada una con su propio XML y lógica en Java:

Pantalla	Descripción
LoginActivity	Ingreso de credenciales y conexión con Firebase Auth.
MenuActivity	Menú principal con navegación hacia los módulos.
MainActivity	Cálculo inicial de despacho (versión base).
ProductosActivity	Gestión de productos, carrito y conexión con Firebase.
TemperaturaActivity	Monitoreo en tiempo real de la temperatura.
DespachoActivity	Seguimiento visual del estado de pedidos.

El proyecto sigue una arquitectura modular y escalable, con comunicación constante entre las capas de interfaz, lógica y datos en la nube.

🧠 Integración con Firebase y Python

Firebase Authentication: Manejo de usuarios registrados.

Firebase Realtime Database: Almacenamiento en tiempo real de pedidos, carritos, estados y temperaturas.

Script Python (IoT): Simula un sensor de temperatura que envía datos a Firebase cada 5 s, permitiendo la actualización en vivo dentro de la app.

📡 Esta integración permite demostrar un entorno cliente–servidor con sincronización en tiempo real, combinando Android y Python de manera práctica y funcional.

🛠️ Requisitos previos

Android Studio actualizado (versión 2022.3 o superior).

Dispositivo o emulador con API 21 (Lollipop) o superior.

Proyecto vinculado a Firebase con módulos:

Authentication (correo y contraseña).

Realtime Database.

Archivo google-services.json correctamente ubicado en /app.

Python 3.x instalado para ejecutar el script de simulación (puente_firebase.py).

⚙️ Instalación y ejecución

Clonar el repositorio

git clone https://github.com/tuusuario/distribuidora-alimentos.git


Abrir el proyecto en Android Studio

Sincronizar Gradle y dependencias de Firebase

Conectar la app con Firebase (Auth + Database)

Ejecutar el script Python (opcional, para módulo IoT):

python puente_firebase.py


Ejecutar la app en emulador o dispositivo físico.

📸 Evidencias

El repositorio incluye:

Capturas de pantalla del funcionamiento de cada módulo.

Código Java y XML comentado.

Script Python y configuraciones de Firebase.

Documento de informe final con análisis técnico y plan de pruebas.

👩‍💻 Autores

Proyecto académico desarrollado por Jaime Codoceo, Sergio Molina y Karla Pesce Rivas,
para el ramo Desarrollo de Aplicaciones Móviles – Semana 9,
Instituto Profesional AIEP, 2025.
