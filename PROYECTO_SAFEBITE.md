# SafeBite: Tu Guía de Seguridad Alimentaria

**Autores:** David Resa y César Vázquez
**Titulación:** Desarrollo de Aplicaciones Multiplataforma (DAM)
**Modalidad:** Presencial

---

## 1. Introducción y Justificación del Proyecto

En la actualidad, el 8% de los niños y el 3% de los adultos en España sufren alguna alergia alimentaria. La lectura de etiquetas en supermercados es un proceso lento, propenso al error humano y, en ocasiones, confuso debido a la terminología técnica. Además, existe una desconexión entre los usuarios: la información sobre restaurantes seguros o recetas adaptadas suele estar dispersa en foros no verificados.

**SafeBite** se propone como una Super-App que centraliza la salud (perfil de alergias), la tecnología (visión artificial) y la comunidad (red social de apoyo).

## 2. Alcance Detallado del Proyecto

El proyecto se divide en módulos funcionales:
- **Gestión de Identidad y Salud**: Registro con Firebase Auth y configuración de expediente alergológico.
- **Inteligencia de Producto**: Escaneo con ML Kit e integración con la API de Open Food Facts.
- **Ecosistema Colaborativo**: Buscador social, módulo recetario y directorio de restaurantes.

## 3. Estudio de Viabilidad

### 3.1. Viabilidad Técnica
Se emplea el stack tecnológico líder en la industria móvil:
- **Lenguaje**: Kotlin (nativo).
- **Arquitectura**: MVVM (Model-View-ViewModel).
- **Base de Datos Local**: Room (SQLite).
- **Servicios Externos**: Retrofit, Glide, Firebase.

### 3.2. Viabilidad Económica y Legal
Proyecto de bajo coste inicial basado en software libre y capas gratuitas de servicios en la nube. Cumple con el RGPD mediante cifrado de datos sensibles.

## 4. Análisis del Sistema

### 4.1. Requisitos Funcionales
- **RF-01**: Registro mediante email y contraseña cifrada.
- **RF-02**: Edición de perfil de alergias en tiempo real.
- **RF-03**: Escaneo de códigos de barras bajo cualquier condición de luz.
- **RF-04**: Filtrado de recetas por tipo de alérgeno.

### 4.2. Requisitos No Funcionales
- **RNF-01**: Tiempo de respuesta del escaneo < 3 segundos.
- **RNF-02**: Manejo robusto de errores de API externa.
- **RNF-03**: Escalabilidad para soportar hasta 10,000 usuarios activos.

## 5. Diseño de la Base de Datos

| Tabla | Atributos Clave | Relación |
|-------|-----------------|----------|
| Users | id, email, username, avatar | 1 a N con Alergias |
| User_Allergies | user_id, allergy_name, severity | - |
| Scans | id, user_id, barcode, status | 1 a N con Users |
| Community_Posts | id, author_id, type, content | N a 1 con Users |

---

## 6. Contenido del Código y Arquitectura Actual

La aplicación sigue una arquitectura **MVVM (Model-View-ViewModel)** con componentes de **Jetpack Compose** para la interfaz de usuario.

### Estructura de Paquetes:
- `com.example.safebite.ui`: Pantallas de Auth, Home, Profile y Scanner.
- `com.example.safebite.data`: Modelos de datos (`User`, `ProductResponse`) y clientes API (`RetrofitClient`).
- `com.example.safebite.utils`: Utilidades como `BarcodeScanner` basado en ML Kit.

### Clave del Código (Ejemplo de Escaner):
El escáner utiliza `CameraX` vinculado al ciclo de vida de la aplicación, analizando las imágenes en tiempo real a través de un analizador personalizado que invoca a la API de ML Kit.

---

## 7. Propuesta de Mejoras Técnicas

Para escalar SafeBite a una aplicación de nivel profesional, se proponen las siguientes mejoras:

1.  **Inyección de Dependencias (Hilt)**: Migrar el `RetrofitClient` manual a una arquitectura basada en Hilt para facilitar los tests unitarios y la escalabilidad.
2.  **Manejo de Estados con Flow/LiveData**: Implementar `StateFlow` en los ViewModels para una reactividad más robusta en Compose.
3.  **Repositorios con Estrategia Offline-First**: Implementar una capa de repositorio que priorice la caché local de Room antes de consultar la API de Open Food Facts.
4.  **UI/UX Premium**: Implementar Micro-animaciones (Lottie) y un sistema de temas dinámico basado en Material 3.

---

## 8. Nuevas Funcionalidades en Desarrollo

- **Módulo de Recetas Inteligentes**: Sugerencias automáticas de sustitutos para ingredientes alérgenos.
- **Mapa de Restaurantes "Safe"**: Integración con Google Maps para localizar establecimientos con protocolos verificados.
- **Historial de Salud Exportable**: Generación de PDF con el historial de escaneos y perfil para profesionales médicos.
- **Alertas de Comunidad**: Sistema de notificaciones en tiempo real si un producto cambia su formulación.

---

## 9. Cronograma y Programación del Proyecto

### Pautas de Desarrollo:
- **SOLID & Clean Code**: Garantizar que cada clase tenga una única responsabilidad.
- **Code Reviews**: Revisión de código en GitHub para asegurar la calidad.
- **Testing**: Cobertura de tests unitarios para la lógica de cruce de alérgenos.

### Cronograma de Trabajo (10 Semanas):

| Fase | Tarea Principal | Entrega |
|------|-----------------|---------|
| **Fase 1 (Sem 1-2)** | Infraestructura: Git, Firebase y Auth. | App Base |
| **Fase 2 (Sem 3-4)** | Motor de Escaneo: ML Kit y OCR. | Beta Escáner |
| **Fase 3 (Sem 5-6)** | Salud: Perfil alergológico y Sync. | Beta Salud |
| **Fase 4 (Sem 7-8)** | Comunidad: Foros y Recetas. | Beta Social |
| **Fase 5 (Sem 9-10)**| Pulido: Mapas, UX y Despliegue. | Versión 1.0 |

---

**GitHub Repository:** [https://github.com/DavidResa/SafeBite](https://github.com/DavidResa/SafeBite)
