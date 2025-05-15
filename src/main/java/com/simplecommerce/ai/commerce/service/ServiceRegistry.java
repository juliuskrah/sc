package com.simplecommerce.ai.commerce.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class ServiceRegistry {
    private final Map<String, ServiceInfo> services = new ConcurrentHashMap<>();

    public void registerService(ServiceInfo serviceInfo) {
        services.put(serviceInfo.getId(), serviceInfo);
    }

    public void unregisterService(String serviceId) {
        services.remove(serviceId);
    }

    public ServiceInfo getService(String serviceId) {
        return services.get(serviceId);
    }

    public Map<String, ServiceInfo> getAllServices() {
        return Map.copyOf(services);
    }
}
