package com.hiddengems.service.impl;

import com.hiddengems.dto.GemRequest;
import com.hiddengems.dto.GemResponse;
import com.hiddengems.entity.Gem;
import com.hiddengems.entity.User;
import com.hiddengems.repository.GemRepository;
import com.hiddengems.repository.UserRepository;
import com.hiddengems.service.GemService;
import com.hiddengems.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GemServiceImpl implements GemService {

    private final GemRepository gemRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<GemResponse> findAll() {
        return gemRepository.findAllByOrderByCreatedAtDesc().stream().map(GemResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GemResponse findById(Long id) {
        return GemResponse.from(getOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GemResponse> findNearby(double lat, double lng, double radiusKm) {
        return gemRepository.findNearby(lat, lng, radiusKm).stream().map(GemResponse::from).toList();
    }

    @Override
    @Transactional
    public GemResponse create(GemRequest request, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilizator inexistent"));

        Gem gem = new Gem();
        gem.setOwner(owner);
        gem.setName(request.name());
        gem.setDescription(request.description());
        gem.setLatitude(request.latitude());
        gem.setLongitude(request.longitude());
        gem.setCategory(request.category());
        Gem saved = gemRepository.save(gem);

        // prietenii primesc "Uite ce a descoperit ..."
        notificationService.notifyFriendsNewGem(owner, saved);

        return GemResponse.from(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!gemRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Locul nu exista");
        }
        gemRepository.deleteById(id);
    }

    @Override
    @Transactional
    public GemResponse setVerified(Long id, boolean verified) {
        Gem gem = getOrThrow(id);
        gem.setVerified(verified);
        return GemResponse.from(gem);
    }

    private Gem getOrThrow(Long id) {
        return gemRepository.findWithOwnerById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Locul nu exista"));
    }
}
