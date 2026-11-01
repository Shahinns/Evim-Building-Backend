package org.example.service;

import org.example.Interface.naxRepository;
import org.example.entity.naxBuilding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.criteria.Predicate;

@Service
public class naxService {

    @Autowired
    private naxRepository naxRepository;

    public naxBuilding save(naxBuilding building) {
        return naxRepository.save(building);
    }

    // ── Premium birinci, sonra ən yeni elanlar ──────────────────────────────
    public List<naxBuilding> getAll() {
        try {
            return naxRepository.findAllByOrderByPremiumDescIdDesc();
        } catch (Exception e) {
            return naxRepository.findAll();
        }
    }

    public Optional<naxBuilding> getnaxBuildingById(Long id) {
        return naxRepository.findById(id);
    }

    public List<naxBuilding> getnaxBuildingByOtaqsayi(Integer otaqSayi) {
        return naxRepository.findByOtaqSayi(otaqSayi);
    }

    public List<naxBuilding> getByOwnerEmail(String email) {
        return naxRepository.findByOwnerEmail(email);
    }

    public int getActiveListingCountByOwner(String email) {
        return naxRepository.countByOwnerEmailAndStatusIgnoreCase(email, "AKTIV");
    }

    public int getListingCountInLast30Days(String email) {
        return naxRepository.countByOwnerEmailAndCreatedAtAfter(email, java.time.LocalDateTime.now().minusDays(30));
    }

    public int getListingCountByIpInLast30Days(String ip) {
        if (ip == null || ip.isBlank()) return 0;
        return naxRepository.countByOwnerIpAndCreatedAtAfter(ip, java.time.LocalDateTime.now().minusDays(30));
    }

    public int getRecentActiveListingCountByIp(String ip) {
        if (ip == null || ip.isBlank()) return 0;
        return naxRepository.countByOwnerIpAndCreatedAtAfterAndStatusIgnoreCase(
                ip, java.time.LocalDateTime.now().minusHours(24), "AKTIV");
    }

    public List<naxBuilding> search(
            Integer otaqSayi,
            Double minQiymet, Double maxQiymet,
            String unvan,
            Double minKv, Double maxKv,
            Integer minMertebe, Integer maxMertebe,
            String temir,
            String satIcare,
            String buildingType,
            String emlakTipi
    ) {
        Specification<naxBuilding> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (otaqSayi != null) {
                if (otaqSayi >= 5) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("otaqSayi"), otaqSayi));
                } else {
                    predicates.add(cb.equal(root.get("otaqSayi"), otaqSayi));
                }
            }
            if (minQiymet != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("qiymet"), minQiymet));
            if (maxQiymet != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("qiymet"), maxQiymet));
            if (unvan != null && !unvan.isEmpty())
                predicates.add(cb.like(cb.lower(root.get("unvan")), "%" + unvan.toLowerCase() + "%"));
            if (minKv != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("kvMetr"), minKv));
            if (maxKv != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("kvMetr"), maxKv));
            if (minMertebe != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("mertebe"), minMertebe));
            if (maxMertebe != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("mertebe"), maxMertebe));
            if (temir != null && !temir.isEmpty())
                predicates.add(cb.equal(cb.lower(root.get("temir")), temir.toLowerCase()));
            if (satIcare != null && !satIcare.isEmpty())
                predicates.add(cb.equal(root.get("satIcare"), satIcare));
            if (buildingType != null && !buildingType.isEmpty())
                predicates.add(cb.equal(root.get("buildingType"), buildingType));
            if (emlakTipi != null && !emlakTipi.isEmpty())
                predicates.add(cb.equal(root.get("emlakTipi"), emlakTipi));

            query.orderBy(
                    cb.desc(root.get("premium")),
                    cb.desc(root.get("createdAt"))
            );

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return naxRepository.findAll(spec);
    }

    public void delete(Long id) {
        naxRepository.deleteById(id);
    }

    // Oxşar elanlar
    public List<naxBuilding> getSimilar(Long excludeId, String satIcare,
                                        Integer otaqSayi, Double qiymet) {
        double min = qiymet != null ? qiymet * 0.6 : 0;
        double max = qiymet != null ? qiymet * 1.4 : Double.MAX_VALUE;
        return naxRepository.findSimilar(excludeId, satIcare, otaqSayi, min, max);
    }

    // Sahibin elanları — ID-yə görə azalan sıra
    public List<naxBuilding> getByOwnerEmailSorted(String email) {
        return naxRepository.findByOwnerEmailOrderByIdDesc(email);
    }
}