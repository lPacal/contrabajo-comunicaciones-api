package com.contrabajo.comunicaciones_api.config;

import com.contrabajo.comunicaciones_api.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
            
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // Si no hay token o no empieza con Bearer, lo dejamos pasar (SecurityConfig lo bloqueará después si la ruta es protegida)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        
        try {
            username = jwtUtil.extractUsername(jwt);

            // Si hay usuario y el contexto de seguridad aún no está configurado
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                if (jwtUtil.isTokenValid(jwt)) {
                    // Extraemos el rol y lo formateamos como le gusta a Spring Security
                    String role = jwtUtil.extractRole(jwt);
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                    
                    // Creamos el token de autenticación para el contexto
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            username, null, Collections.singletonList(authority)
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Autorizamos al usuario en esta petición
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Si el token expira o es inválido, simplemente no configuramos el contexto (se devolverá 403/401)
            System.out.println("Error procesando el token JWT: " + e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
}