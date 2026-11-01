package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenStreetMap Nominatim ilə ünvan <-> koordinat çevirən servis (açar tələb etmir).
 * Nominatim qaydaları: saniyədə maksimum 1 sorğu, User-Agent olmalıdır, nəticələr keşlənməlidir.
 */
@Service
public class GeocodeService {

    private static final Logger log = LoggerFactory.getLogger(GeocodeService.class);
    private static final long MIN_INTERVAL_MS = 1100;

    private final RestClient client;
    private final Map<String, Double[]> geocodeCache = new ConcurrentHashMap<>();
    private final Map<String, String> reverseCache = new ConcurrentHashMap<>();
    private long lastCallAt = 0;

    public GeocodeService(
            @Value("${app.geocode.base-url:https://nominatim.openstreetmap.org}") String baseUrl,
            @Value("${app.geocode.user-agent:EvimSat/1.0}") String userAgent) {
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", userAgent)
                .defaultHeader("Accept-Language", "az,en")
                .build();
    }

    /** Ünvanı koordinata çevirir: {latitude, longitude}, tapılmasa null */
    public Double[] geocodeAddress(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        String key = address.trim().toLowerCase();
        Double[] cached = geocodeCache.get(key);
        if (cached != null) {
            return cached;
        }

        try {
            throttle();
            List<Map<String, Object>> results = client.get()
                    .uri("/search?format=jsonv2&q={q}&limit=1&countrycodes=az", address.trim())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            if (results != null && !results.isEmpty()) {
                Map<String, Object> first = results.get(0);
                Double lat = Double.parseDouble(String.valueOf(first.get("lat")));
                Double lon = Double.parseDouble(String.valueOf(first.get("lon")));
                Double[] coords = new Double[]{lat, lon};
                geocodeCache.put(key, coords);
                return coords;
            }
        } catch (Exception e) {
            log.warn("Geocoding xətası: {}", e.getMessage());
        }
        return null;
    }

    /** Koordinatı ünvana çevirir, tapılmasa null */
    public String reverseGeocode(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        String key = String.format("%.5f,%.5f", latitude, longitude);
        String cached = reverseCache.get(key);
        if (cached != null) {
            return cached;
        }

        try {
            throttle();
            Map<String, Object> result = client.get()
                    .uri("/reverse?format=jsonv2&lat={lat}&lon={lon}", latitude, longitude)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (result != null && result.get("display_name") != null) {
                String address = String.valueOf(result.get("display_name"));
                reverseCache.put(key, address);
                return address;
            }
        } catch (Exception e) {
            log.warn("Reverse geocoding xətası: {}", e.getMessage());
        }
        return null;
    }

    /** Nominatim limitinə (1 sorğu/saniyə) riayət etmək üçün */
    private synchronized void throttle() {
        long wait = lastCallAt + MIN_INTERVAL_MS - System.currentTimeMillis();
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallAt = System.currentTimeMillis();
    }
}