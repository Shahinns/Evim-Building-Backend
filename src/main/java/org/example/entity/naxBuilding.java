package org.example.entity;

import jakarta.persistence.*;
import org.example.converter.StringListJsonConverter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "buildings")
public class naxBuilding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emlak_tipi", nullable = false)
    private String emlakTipi;

    @Column(name = "otaq_sayi", nullable = false)
    private Integer otaqSayi;

    @Column(name = "qiymet", nullable = false)
    private Double qiymet;

    @Column(name = "unvan", nullable = false)
    private String unvan;

    @Column(name = "sat_icare", nullable = false)
    private String satIcare;

    @Column(name = "kv_metr", nullable = false)
    private Double kvMetr;

    @Column(name = "mertebe", nullable = false)
    private Integer mertebe;

    @Column(name = "binanin_mertebe_sayi", nullable = false)
    private Integer binaninMertebeSayi;

    @Column(name = "temir")
    private String temir;

    @Column(name = "yashayis_sahesi")
    private Double yashayisSahesi;

    @Column(name = "lift")
    private Boolean lift;

    @Column(name = "hasar")
    private Boolean hasar;

    @Column(name = "parking")
    private Boolean parking;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "phone")
    private String phone;

    @Column(name = "whatsapp")
    private String whatsapp;

    // ── YENİ: Premium elan ──────────────────────────────────────────────────
    @Column(name = "premium", nullable = false)
    private Boolean premium = false;

    @Column(name = "building_type")
    private String buildingType; // YENI, KOHNE, etc.


    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Column(name = "status")
    private String status = "AKTIV";


    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Column(name = "image_urls", columnDefinition = "TEXT")
    @Convert(converter = StringListJsonConverter.class)
    private List<String> imageUrls = new ArrayList<>();

    @Column(name = "owner_email")
    private String ownerEmail;

    @Column(name = "owner_ip", length = 64)
    private String ownerIp;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmlakTipi() { return emlakTipi; }
    public void setEmlakTipi(String emlakTipi) { this.emlakTipi = emlakTipi; }

    public Integer getOtaqSayi() { return otaqSayi; }
    public void setOtaqSayi(Integer otaqSayi) { this.otaqSayi = otaqSayi; }

    public Double getQiymet() { return qiymet; }
    public void setQiymet(Double qiymet) { this.qiymet = qiymet; }

    public String getUnvan() { return unvan; }
    public void setUnvan(String unvan) { this.unvan = unvan; }

    public String getSatIcare() { return satIcare; }
    public void setSatIcare(String satIcare) { this.satIcare = satIcare; }

    public Double getKvMetr() { return kvMetr; }
    public void setKvMetr(Double kvMetr) { this.kvMetr = kvMetr; }

    public Integer getMertebe() { return mertebe; }
    public void setMertebe(Integer mertebe) { this.mertebe = mertebe; }

    public Integer getBinaninMertebeSayi() { return binaninMertebeSayi; }
    public void setBinaninMertebeSayi(Integer binaninMertebeSayi) { this.binaninMertebeSayi = binaninMertebeSayi; }

    public String getTemir() { return temir; }
    public void setTemir(String temir) { this.temir = temir; }

    public Double getYashayisSahesi() { return yashayisSahesi; }
    public void setYashayisSahesi(Double yashayisSahesi) { this.yashayisSahesi = yashayisSahesi; }

    public Boolean getLift() { return lift; }
    public void setLift(Boolean lift) { this.lift = lift; }

    public Boolean getHasar() { return hasar; }
    public void setHasar(Boolean hasar) { this.hasar = hasar; }

    public Boolean getParking() { return parking; }
    public void setParking(Boolean parking) { this.parking = parking; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

    public Boolean getPremium() { return premium; }
    public void setPremium(Boolean premium) { this.premium = premium != null ? premium : false; }

    public String getBuildingType() { return buildingType; }
    public void setBuildingType(String buildingType) { this.buildingType = buildingType; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>(); }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getOwnerIp() { return ownerIp; }
    public void setOwnerIp(String ownerIp) { this.ownerIp = ownerIp; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}