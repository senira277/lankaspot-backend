package com.lankaspot.server.service;

import com.lankaspot.server.model.AdQuote;
import com.lankaspot.server.model.Advertisement;
import com.lankaspot.server.model.AdQuoteMedia;
import com.lankaspot.server.model.LiveAdCard;
import com.lankaspot.server.repository.AdQuoteMediaRepository;
import com.lankaspot.server.repository.LiveAdCardRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class LiveAdCardService {
    private final LiveAdCardRepository repository;
    private final AdQuoteMediaRepository mediaRepository;

    @Transactional
    public LiveAdCard upsert(Advertisement advertisement, AdQuote quote) {
        LiveAdCard card = repository.findByAdvertisementId(advertisement.getAdvertisementRef())
                .orElseGet(LiveAdCard::new);
        card.setAdvertisementId(advertisement.getAdvertisementRef());
        card.setCategoryId(quote.getCategory().getId());
        card.setCategoryName(quote.getCategory().getName());
        card.setCityId(quote.getCity().getId());
        card.setCityName(quote.getCity().getName());
        card.setDistrictId(quote.getCity().getDistrict().getId());
        card.setDistrictName(quote.getCity().getDistrict().getName());
        card.setFunctionalGroupId(quote.getFunctionalGroup() == null ? null : quote.getFunctionalGroup().getId());
        card.setFunctionalGroupName(quote.getFunctionalGroup() == null ? null : quote.getFunctionalGroup().getName());
        card.setTitle(quote.getTitle());
        card.setNearestLandmark(quote.getNearestLandmark());
        card.setTown(quote.getTown());
        card.setPrice(quote.getPrice());
        card.setSlug(advertisement.getSlug());
        card.setQuoteRef(quote.getId().toString());
        card.setCoverUrl(mediaRepository.findByAdQuote_IdAndMediaStatusNotOrderBySortOrderAscIdAsc(
                        quote.getId(), com.lankaspot.server.enums.MediaStatus.REQUESTED_DELETION)
                .stream().filter(AdQuoteMedia::isPrimary).findFirst()
                .or(() -> mediaRepository.findByAdQuote_IdOrderBySortOrderAscIdAsc(quote.getId())
                        .stream().findFirst())
                .map(AdQuoteMedia::getUrl).orElse(null));
        card.setSortOrder(advertisement.getSortOrder());
        card.setQueueNumber(advertisement.getQueueNumber());
        card.setExpiresAt(advertisement.getExpiresAt());
        return repository.save(card);
    }

    @Transactional
    public void delete(java.util.UUID advertisementId) {
        repository.deleteByAdvertisementId(advertisementId);
    }
}
