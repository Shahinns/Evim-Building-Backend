package org.example.service;

import org.example.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AgencyPackageService {

    public record PackageOffer(String code, String name, double price, int listingLimit) {}

    public static final Map<String, PackageOffer> PACKAGES = new LinkedHashMap<>();

    static {
        PACKAGES.put("P50", new PackageOffer("P50", "Standart", 50.0, 25));
        PACKAGES.put("P70", new PackageOffer("P70", "Plus", 70.0, 40));
        PACKAGES.put("P100", new PackageOffer("P100", "Pro", 100.0, 50));
    }

    private AgencyPackageService() {}

    public static PackageOffer require(String code) {
        PackageOffer offer = PACKAGES.get(code == null ? "" : code.toUpperCase().trim());
        if (offer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paket tapılmadı");
        }
        return offer;
    }

    public static void apply(User user, PackageOffer offer) {
        user.setRole("AGENCY");
        user.setPackageType(offer.code());
        user.setListingLimit(offer.listingLimit());
    }
}
