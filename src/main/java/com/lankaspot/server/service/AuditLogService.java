
package com.lankaspot.server.service;

import com.lankaspot.server.model.AuditLog;
import com.lankaspot.server.model.User;
import com.lankaspot.server.repository.AuditLogRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.temporal.TemporalAccessor;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final EntityManagerFactory entityManagerFactory;

    @Transactional
    public void record(
            User actor,
            String action,
            String entityType,
            Long entityId,
            Object oldData,
            Object newData
    ) {
        String ip = null;
        String userAgent = null;

        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            ip = truncate(request.getRemoteAddr(), 45);
            userAgent = truncate(request.getHeader("User-Agent"), 1000);
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setActor(actor);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setOldData(toJson(oldData));
        auditLog.setNewData(toJson(newData));
        auditLog.setIpAddress(ip);
        auditLog.setUserAgent(userAgent);
        auditLog.setCreatedAt(java.time.LocalDateTime.now());

        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> historyFor(String entityType, Long entityId) {
        return auditLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                        entityType, entityId);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }

        try {
            Object snapshot = toSafeSnapshot(value, new IdentityHashMap<>());
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            // Do not let audit payload serialization failures break the operation.
            log.warn("Could not serialise audit payload of type {}",
                    value.getClass().getName(), e);

            return "{\"serializationError\":\"Audit payload could not be serialised\"}";
        }
    }

    private Object toSafeSnapshot(
            Object value,
            IdentityHashMap<Object, Boolean> visited
    ) throws Exception {

        if (value == null || isSimpleValue(value.getClass())) {
            return value;
        }

        if (visited.containsKey(value)) {
            return "[CircularReference]";
        }

        visited.put(value, Boolean.TRUE);

        try {
            // Handle Hibernate proxies without forcing lazy initialization.
            if (value instanceof HibernateProxy proxy) {
                var initializer = proxy.getHibernateLazyInitializer();

                if (initializer.isUninitialized()) {
                    Map<String, Object> reference = new LinkedHashMap<>();
                    reference.put("entityType",
                            initializer.getPersistentClass().getSimpleName());
                    reference.put("id", initializer.getIdentifier());
                    return reference;
                }

                value = initializer.getImplementation();
            }

            // Handle JPA entities by capturing only basic attributes.
            EntityType<?> entityType = getEntityType(value.getClass());

            if (entityType != null) {
                return snapshotEntity(value, entityType);
            }

            // Handle lists, sets and other collections of entities.
            if (value instanceof Collection<?> collection) {
                List<Object> result = new ArrayList<>(collection.size());

                for (Object item : collection) {
                    result.add(toSafeSnapshot(item, visited));
                }

                return result;
            }

            if (value instanceof Map<?, ?> map) {
                Map<String, Object> result = new LinkedHashMap<>();

                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    result.put(String.valueOf(entry.getKey()),
                            toSafeSnapshot(entry.getValue(), visited));
                }

                return result;
            }

            if (value.getClass().isArray()) {
                List<Object> result = new ArrayList<>();
                int length = Array.getLength(value);

                for (int i = 0; i < length; i++) {
                    result.add(toSafeSnapshot(Array.get(value, i), visited));
                }

                return result;
            }

            // Preserve DTOs, records and other non-entity objects.
            return value;

        } finally {
            visited.remove(value);
        }
    }

    private Map<String, Object> snapshotEntity(
            Object entity,
            EntityType<?> entityType
    ) throws Exception {

        Map<String, Object> snapshot = new LinkedHashMap<>();

        snapshot.put("entityType", entityType.getName());
        snapshot.put("id",
                entityManagerFactory.getPersistenceUnitUtil()
                        .getIdentifier(entity));

        for (Attribute<?, ?> attribute : entityType.getAttributes()) {

            // Do not traverse relationships such as @ManyToOne,
            // @OneToMany or @ManyToMany.
            if (attribute.getPersistentAttributeType()
                    != Attribute.PersistentAttributeType.BASIC) {
                continue;
            }

            String name = attribute.getName();

            // Skip lazy basic attributes that have not been loaded.
            if (!Hibernate.isPropertyInitialized(entity, name)) {
                continue;
            }

            try {
                Object fieldValue = readAttribute(entity, attribute);
                snapshot.put(name, fieldValue);
            } catch (Exception e) {
                log.debug("Could not read audit attribute {}", name);
            }
        }

        return snapshot;
    }

    private EntityType<?> getEntityType(Class<?> type) {
        try {
            return entityManagerFactory.getMetamodel().entity(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Object readAttribute(
            Object entity,
            Attribute<?, ?> attribute
    ) throws Exception {

        var member = attribute.getJavaMember();

        if (member instanceof Field field) {
            field.setAccessible(true);
            return field.get(entity);
        }

        if (member instanceof Method method) {
            method.setAccessible(true);
            return method.invoke(entity);
        }

        throw new IllegalArgumentException(
                "Unsupported attribute member: " + attribute.getName());
    }

    private boolean isSimpleValue(Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || type == String.class
                || type == Boolean.class
                || type == Character.class
                || Number.class.isAssignableFrom(type)
                || UUID.class.isAssignableFrom(type)
                || Date.class.isAssignableFrom(type)
                || TemporalAccessor.class.isAssignableFrom(type)
                || type == Class.class;
    }

    private static String truncate(String value, int maxLength) {
        return value == null
                ? null
                : value.length() > maxLength
                ? value.substring(0, maxLength)
                : value;
    }
}
