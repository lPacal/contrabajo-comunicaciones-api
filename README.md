# Contrabajo - Microservicio de Comunicaciones (MS_Comunicaciones)

Este microservicio es el núcleo de mensajería de la plataforma **Contrabajo**. Se encarga de facilitar y persistir la comunicación bidireccional y en tiempo real entre los clientes y los trabajadores, manteniendo trazabilidad de la lectura de mensajes y gestionando el ciclo de vida de los chats vinculados a ofertas específicas.

---

## Tecnologías y Arquitectura

El microservicio está construido bajo un enfoque de alta concurrencia, comunicación híbrida (síncrona/asíncrona) y seguridad de contexto:

* **Lenguaje:** Java 21
* **Framework:** Spring Boot 3.4.x
* **Comunicación en Tiempo Real:** Spring WebSocket (STOMP)
* **Seguridad (Autenticación):** Spring Security + JJWT (Validación Stateless de Tokens compartida)
* **Gestión de Configuración:** Spring Cloud Config (Client)
    * *Endpoint:* `http://localhost:8888`
* **Persistencia:** Spring Data JPA
* **Base de Datos:** SQL Server
* **Productividad:** Lombok

---

## Estructura del Proyecto

El código sigue una arquitectura de capas diseñada para separar el protocolo en tiempo real de las reglas de negocio de la mensajería:

* `config/`: Configuraciones del servidor, incluyendo el enrutamiento de WebSockets y el filtro de seguridad JWT compartido con `MS_Usuarios`.
* `model/`: Entidades JPA (`ChatOferta`, `MensajeChat`) que representan las salas y el historial transaccional.
* `repository/`: Interfaces para la interacción con SQL Server, incluyendo búsquedas optimizadas para trazabilidad de lectura (Vistos).
* `service/`: Lógica central de seguridad (Anti-IDOR), actualizaciones masivas de estado (Leído/Recibido) y gestión de acceso a las salas.
* `controller/`: Endpoints HTTP REST para interacciones síncronas (Crear chat, obtener historial, desactivar).
* `dto/`: Objetos de transferencia (`ChatOfertaRequestDTO`, `MensajeChatRequestDTO`), unificando las estructuras de entrada y salida para el cliente móvil.
* `utils/`: Utilidades criptográficas (`JwtUtil`) para desencriptar en Base64 la llave maestra y extraer la identidad/rol del usuario.

---

## Configuración del Entorno

Este servicio requiere infraestructura previa para operar de forma segura en la malla de microservicios:

1. **Config Server:** Utiliza un `application.yml` con `fail-fast: true`. Es **obligatorio** tener el servidor de configuración (`config_server`) activo en el puerto `8888` para heredar la llave JWT secreta.
2. **Puerto Local:** Se ejecuta por defecto en el puerto `8083` para aislar sus recursos de los servicios de Usuarios y Ofertas.
3. **WebSockets:** El túnel de comunicación principal para recepción de mensajes se expone mediante STOMP, escuchando colas personalizadas por ID de usuario (`/topic/chat/{id}`).