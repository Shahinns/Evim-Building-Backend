package org.example.Interface;

import org.example.entity.naxBuilding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface naxRepository extends JpaRepository<naxBuilding, Long>,
        JpaSpecificationExecutor<naxBuilding> {


    Optional<naxBuilding> findById(Long id);

    List<naxBuilding> findByOtaqSayi(Integer otaqSayi);


    List<naxBuilding> findByOtaqSayiAndQiymetLessThanAndUnvanContainingAndEmlakTipi(Integer otaqSayi, Double qiymet, String unvan, String emlakTipi);

    List<naxBuilding> findByOtaqSayiAndQiymetLessThan(Integer otaqSayi, Double qiymet);

    List<naxBuilding> findAllByOrderByPremiumDescIdDesc();
    List<naxBuilding> findByOwnerEmail(String ownerEmail);

    int countByOwnerEmailAndCreatedAtAfter(String ownerEmail, java.time.LocalDateTime date);

    int countByOwnerIpAndCreatedAtAfter(String ownerIp, java.time.LocalDateTime date);

    int countByOwnerEmailAndStatusIgnoreCase(String ownerEmail, String status);

    int countByOwnerIpAndCreatedAtAfterAndStatusIgnoreCase(String ownerIp, java.time.LocalDateTime date, String status);

    // Oxşar elanlar: eyni satIcare + otaqSayi, özü xaric, qiymət ±40%, maks 4 nəticə
    @Query("SELECT b FROM naxBuilding b WHERE b.id <> :excludeId " +
            "AND (:satIcare IS NULL OR b.satIcare = :satIcare) " +
            "AND (:otaqSayi IS NULL OR b.otaqSayi = :otaqSayi) " +
            "AND (b.qiymet IS NULL OR b.qiymet BETWEEN :minQiymet AND :maxQiymet) " +
            "ORDER BY b.premium DESC, b.id DESC " +
            "LIMIT 4")
    List<naxBuilding> findSimilar(Long excludeId, String satIcare,
                                  Integer otaqSayi,
                                  Double minQiymet, Double maxQiymet);

    // Baxış sayacı üçün köməkçi — naxService-dən save() çağırılır, bu lazım deyil
    // Status üzrə filtr
    List<naxBuilding> findByOwnerEmailOrderByIdDesc(String ownerEmail);
}