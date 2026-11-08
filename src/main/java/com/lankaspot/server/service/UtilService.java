package com.lankaspot.server.service;

import com.lankaspot.server.constants.RoleConstants;
import com.lankaspot.server.enums.Role;
import com.lankaspot.server.model.User;
import com.lankaspot.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UtilService {

    private final UserRepository userRepository;

    public <K, V> V getFromMap(Map<K, V> map, K key) {
        return map == null ? null : map.get(key);
    }

    public <K, V> V getFromMap(Map<K, V> map, K key, V defaultValue) {
        return map == null ? defaultValue : map.getOrDefault(key, defaultValue);
    }

    public <K, V> boolean existsIn(Map<K, V> map, K key) {
        return map != null && map.containsKey(key);
    }

    public <K, V> void addToMap(Map<K, V> map, K key, V value) {
        if (map != null) {
            map.put(key, value);
        }
    }

    public <K, V> Map<K, V> searchFromMap(Map<K, V> map, Predicate<Map.Entry<K, V>> filter) {
        if (map == null) {
            return Collections.emptyMap();
        }
        return map.entrySet().stream()
                .filter(filter)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    public <K, V> Map<K, V> retrieveAll(Map<K, V> map) {
        return map == null ? Collections.emptyMap() : new LinkedHashMap<>(map);
    }

    public User resolveUser(Authentication authentication) {
        return  userRepository.findByKeycloakUserId(authentication.getName()).orElse(null);
    }

    public UUID resolveUUID(Authentication authentication) {
        return  UUID.fromString(authentication.getName());
    }

    public Role resolveRole(Authentication authentication) {

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new IllegalStateException("Unsupported authentication type");
        }

        Map<String, Object> realmAccess =
                jwtAuth.getToken().getClaim("realm_access");

        if (realmAccess == null) {
            throw new IllegalStateException("No realm roles found");
        }

        Object rolesObject = realmAccess.get("roles");

        if (!(rolesObject instanceof List<?> roles)) {
            throw new IllegalStateException("Invalid realm roles");
        }

        return RoleConstants.ROLE_PRIORITY.stream()
                .filter(role -> roles.contains(role.name()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No supported role found")
                );
    }

}