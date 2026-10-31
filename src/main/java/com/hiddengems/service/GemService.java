package com.hiddengems.service;

import com.hiddengems.dto.GemRequest;
import com.hiddengems.dto.GemResponse;

import java.util.List;

public interface GemService {

    List<GemResponse> findAll();

    GemResponse findById(Long id);

    List<GemResponse> findNearby(double lat, double lng, double radiusKm);

    GemResponse create(GemRequest request, String username);

    void delete(Long id);

    GemResponse setVerified(Long id, boolean verified);
}
