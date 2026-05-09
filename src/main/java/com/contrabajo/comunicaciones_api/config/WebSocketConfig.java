package com.contrabajo.comunicaciones_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Los clientes se suscriben a rutas que empiezan con "/topic"
        config.enableSimpleBroker("/topic");
        
        // Los clientes envían mensajes a rutas que empiezan con "/app"
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint con SockJS — para futuros clientes web/navegador
        registry.addEndpoint("/ws-comunicaciones")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Endpoint nativo — para clientes Android/iOS (WebSocket puro, sin SockJS)
        registry.addEndpoint("/ws-comunicaciones-native")
                .setAllowedOriginPatterns("*");
    }
}