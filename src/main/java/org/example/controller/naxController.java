package org.example.controller;


import org.example.entity.naxBuilding;
import org.example.entity.User;
import org.example.Interface.UserRepository;
import org.example.service.FileStorageService;
import org.example.service.GeocodeService;
import org.example.service.naxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/buildings")
public class naxController {

    @Autowired
    private naxService naxService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private GeocodeService geocodeService;
    @Autowired
    private UserRepository userRepository;

    // 🔹 Hamısını gətir
    @GetMapping
    public List<naxBuilding> getAll() {
        return naxService.getAll();
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Şəkil yükləmək üçün giriş etməlisiniz"));
        }
        try {
            String url = fileStorageService.store(file);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Fayl saxlanılmadı"));
        }
    }

    // Yeni elan əlavə et (JWT tələb olunur)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@RequestBody naxBuilding newBuilding,
                                    Authentication authentication,
                                    jakarta.servlet.http.HttpServletRequest request) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Giriş tələb olunur"));
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof String email) || email.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("message", "Giriş tələb olunur"));
        }

        // Client IP-ni müəyyən et (proxy/nginx arxasında da işləyir)
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp != null && !clientIp.isBlank()) {
            clientIp = clientIp.split(",")[0].trim();
        } else {
            clientIp = request.getRemoteAddr();
        }

        newBuilding.setId(null);
        newBuilding.setOwnerEmail(email);
        newBuilding.setOwnerIp(clientIp);
        newBuilding.setPremium(false);
        newBuilding.setViewCount(0L);
        newBuilding.setStatus("AKTIV");
        newBuilding.setCreatedAt(LocalDateTime.now());
        newBuilding.setUpdatedAt(LocalDateTime.now());
        if (newBuilding.getImageUrls() == null) {
            newBuilding.setImageUrls(List.of());
        }

        int maxLimit = 10;
        Optional<User> ownerOpt = userRepository.findByEmailIgnoreCase(email);
        boolean isAgency = false;
        if (ownerOpt.isPresent()) {
            User owner = ownerOpt.get();
            if (owner.getListingLimit() != null) {
                maxLimit = owner.getListingLimit();
            }
            isAgency = "AGENCY".equalsIgnoreCase(owner.getRole())
                    || "ADMIN".equalsIgnoreCase(owner.getRole())
                    || maxLimit > 10;
        }

        int activeCount = naxService.getActiveListingCountByOwner(email);
        if (activeCount >= maxLimit) {
            return ResponseEntity.status(403).body(Map.of(
                "message", "Aktiv elan limitiniz dolub (" + maxLimit + "). "
                        + (isAgency
                        ? "Daha çox elan üçün paket alın və ya köhnə elanı deaktiv edin."
                        : "11-ci elan üçün agentlik və ya makler hesabı açılmalıdır.")
            ));
        }

        if (!isAgency && clientIp != null && !clientIp.isBlank()) {
            int ipCount = naxService.getRecentActiveListingCountByIp(clientIp);
            if (ipCount >= 15) {
                return ResponseEntity.status(429).body(Map.of(
                    "message", "Bu IP ünvanından şübhəli aktivlik aşkarlandı. Zəhmət olmasa sonra yenidən cəhd edin."
                ));
            }
        }

        // 🔑 Avtomatik geocoding: əgər latitude/longitude boşdursa, ünvandan tapışdır
        if ((newBuilding.getLatitude() == null || newBuilding.getLongitude() == null)
                && newBuilding.getUnvan() != null && !newBuilding.getUnvan().isBlank()) {
            Double[] coordinates = geocodeService.geocodeAddress(newBuilding.getUnvan());
            if (coordinates != null && coordinates.length == 2) {
                newBuilding.setLatitude(coordinates[0]);
                newBuilding.setLongitude(coordinates[1]);
            }
        }

        naxBuilding saved = naxService.save(newBuilding);
        return ResponseEntity.ok(saved);
    }

    // 🔹 ID ilə tap
    @GetMapping("/{id}")
    public ResponseEntity<naxBuilding> getById(@PathVariable Long id) {
        Optional<naxBuilding> nax = naxService.getnaxBuildingById(id);
        return nax.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 🔹 OTAQ SAYINA GÖRƏ (fix edilmiş)
    @GetMapping("/otaq/{otaqSayi}")
    public ResponseEntity<List<naxBuilding>> getByOtaq(@PathVariable Integer otaqSayi) {
        List<naxBuilding> result = naxService.getnaxBuildingByOtaqsayi(otaqSayi);
        return ResponseEntity.ok(result);
    }

    // 🔥 ƏN VACİB (FILTER + SEARCH)
    @GetMapping("/search")
    public ResponseEntity<List<naxBuilding>> search(
            @RequestParam(required = false) Integer otaqSayi,
            @RequestParam(required = false) Double minQiymet,
            @RequestParam(required = false) Double maxQiymet,
            @RequestParam(required = false) String unvan,
            @RequestParam(required = false) Double minKv,
            @RequestParam(required = false) Double maxKv,
            @RequestParam(required = false) Integer minMertebe,
            @RequestParam(required = false) Integer maxMertebe,
            @RequestParam(required = false) String temir,
            @RequestParam(required = false) String satIcare,
            @RequestParam(required = false) String buildingType,
            @RequestParam(required = false) String emlakTipi
    ) {
        List<naxBuilding> result = naxService.search(
                otaqSayi, minQiymet, maxQiymet,
                unvan, minKv, maxKv,
                minMertebe, maxMertebe,
                temir, satIcare, buildingType, emlakTipi
        );

        return ResponseEntity.ok(result);
    }
    @GetMapping("/my-listings")
    public ResponseEntity<?> getMyListings(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Giriş tələb olunur"));
        }
        String email = (String) authentication.getPrincipal();
        List<naxBuilding> result = naxService.getByOwnerEmail(email);
        return ResponseEntity.ok(result);
    }

    // 🔹 Elanı yenilə (yalnız sahibi)
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody naxBuilding updated,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Giriş tələb olunur"));
        }
        String email = (String) authentication.getPrincipal();

        Optional<naxBuilding> existing = naxService.getnaxBuildingById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        naxBuilding building = existing.get();
        if (!email.equalsIgnoreCase(building.getOwnerEmail())) {
            return ResponseEntity.status(403).body(Map.of("message", "Bu əməliyyat üçün icazəniz yoxdur"));
        }

        // Dəyişdirilə bilən sahələr
        building.setEmlakTipi(updated.getEmlakTipi());
        building.setSatIcare(updated.getSatIcare());
        building.setBuildingType(updated.getBuildingType());
        building.setUnvan(updated.getUnvan());
        building.setOtaqSayi(updated.getOtaqSayi());
        building.setKvMetr(updated.getKvMetr());
        building.setMertebe(updated.getMertebe());
        building.setBinaninMertebeSayi(updated.getBinaninMertebeSayi());
        building.setQiymet(updated.getQiymet());
        building.setTemir(updated.getTemir());
        building.setLift(updated.getLift());
        building.setHasar(updated.getHasar());
        building.setParking(updated.getParking());
        building.setPhone(updated.getPhone());
        building.setWhatsapp(updated.getWhatsapp());
        building.setDescription(updated.getDescription());
        if (updated.getImageUrls() != null) {
            building.setImageUrls(updated.getImageUrls());
        }
        // Koordinat yenilənibsə
        if (updated.getLatitude() != null) building.setLatitude(updated.getLatitude());
        if (updated.getLongitude() != null) building.setLongitude(updated.getLongitude());

        building.setUpdatedAt(LocalDateTime.now());
        naxBuilding saved = naxService.save(building);
        return ResponseEntity.ok(saved);
    }

    // 🔹 Elan statusunu dəyiş: AKTIV | SATILDI | KIRAYE_VERILDI (yalnız sahibi)
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) { 

        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Giriş tələb olunur"));
        }
        String email = (String) authentication.getPrincipal();

        Optional<naxBuilding> existing = naxService.getnaxBuildingById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();

        naxBuilding building = existing.get();
        if (!email.equalsIgnoreCase(building.getOwnerEmail())) {
            return ResponseEntity.status(403).body(Map.of("message", "İcazəniz yoxdur"));
        }

        String rawStatus = body.get("status");
        if (rawStatus == null || rawStatus.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status göndərin"));
        }
        String newStatus = rawStatus.trim().toUpperCase();
        if (!Set.of("AKTIV", "SATILDI", "KIRAYE_VERILDI", "DEAKTIV").contains(newStatus)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status yalnız AKTIV, SATILDI və ya KIRAYE_VERILDI ola bilər"));
        }
        building.setStatus(newStatus);
        building.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(naxService.save(building));
    }

    // 🔹 Elanı sil (yalnız sahibi)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Giriş tələb olunur"));
        }
        String email = (String) authentication.getPrincipal();

        Optional<naxBuilding> existing = naxService.getnaxBuildingById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();

        naxBuilding building = existing.get();
        if (!email.equalsIgnoreCase(building.getOwnerEmail())) {
            return ResponseEntity.status(403).body(Map.of("message", "Bu əməliyyat üçün icazəniz yoxdur"));
        }

        naxService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // 🔹 Baxış sayacı — detail səhifəsi açılanda çağırılır
    @PostMapping("/{id}/view")
    public ResponseEntity<?> incrementView(@PathVariable Long id) {
        Optional<naxBuilding> existing = naxService.getnaxBuildingById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();
        naxBuilding building = existing.get();
        Long current = building.getViewCount() != null ? building.getViewCount() : 0L;
        building.setViewCount(current + 1);
        naxService.save(building);
        return ResponseEntity.ok(Map.of("viewCount", building.getViewCount()));
    }

    // 🔹 Oxşar elanlar — eyni satIcare + otaqSayi, özü xaric
    @GetMapping("/{id}/similar")
    public ResponseEntity<List<naxBuilding>> getSimilar(@PathVariable Long id) {
        Optional<naxBuilding> existing = naxService.getnaxBuildingById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();
        naxBuilding b = existing.get();
        List<naxBuilding> similar = naxService.getSimilar(id, b.getSatIcare(), b.getOtaqSayi(), b.getQiymet());
        return ResponseEntity.ok(similar);
    }

    // 🔹 Elanı VIP et (10 AZN)
    @Transactional
    @PostMapping("/{id}/vip")
    public ResponseEntity<?> makeVip(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Giriş tələb olunur"));
        }
        String email = (String) authentication.getPrincipal();

        Optional<naxBuilding> existing = naxService.getnaxBuildingById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();
        naxBuilding building = existing.get();

        if (!email.equalsIgnoreCase(building.getOwnerEmail())) {
            return ResponseEntity.status(403).body(Map.of("message", "Bu əməliyyat üçün icazəniz yoxdur"));
        }

        if (building.getPremium()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bu elan onsuz da VIP-dir"));
        }

        Optional<User> ownerOpt = userRepository.findByEmailIgnoreCase(email);
        if (ownerOpt.isEmpty()) return ResponseEntity.status(401).build();
        User owner = ownerOpt.get();

        if (owner.getBalance() == null || owner.getBalance() < 10.0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Balansınızda kifayət qədər vəsait yoxdur. Zəhmət olmasa balansı artırın."));
        }

        // Pulun çıxılması
        owner.setBalance(owner.getBalance() - 10.0);
        userRepository.save(owner);

        // Elanın VIP edilməsi
        building.setPremium(true);
        naxBuilding saved = naxService.save(building);

        return ResponseEntity.ok(Map.of("message", "Elan uğurla VIP edildi", "newBalance", owner.getBalance(), "building", saved));
    }
}