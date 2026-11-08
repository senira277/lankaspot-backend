package com.lankaspot.server.service;

import com.lankaspot.server.dto.openRetrievals.*;
import com.lankaspot.server.model.*;
import com.lankaspot.server.repository.*;
import com.lankaspot.server.response.LankaspotResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OpenRetrievalService {

    private final CategoryRepository categoryRepository;
    private final DistrictRepository districtRepository;
    private final CityRepository cityRepository;
    private final FeatureRepository featureRepository;
    private final FunctionalGroupRepository functionalGroupRepository;

    public LankaspotResponse<AllCategories> getAllCategories() {
        List<CategoryDTO> categories = categoryRepository.findAll()
                .stream()
                .map(this::mapToCategoryDTO)
                .toList();

        return LankaspotResponse.success(
                new AllCategories(categories),
                "All categories retrieved successfully"
        );
    }

    public LankaspotResponse<AllDistricts> getAllDistricts() {
        List<DistrictDTO> districts = districtRepository.findAll()
                .stream()
                .map(this::mapToDistrictDTO)
                .toList();

        return LankaspotResponse.success(
                new AllDistricts(districts),
                "All districts retrieved successfully"
        );
    }

    public LankaspotResponse<AllCities> getAllCities(Long districtId) {
        List<CityDTO> cities = cityRepository.findAllByDistrictId(districtId)
                .stream()
                .map(this::mapToCityDTO)
                .toList();

        return LankaspotResponse.success(
                new AllCities(cities),
                "All cities retrieved successfully"
        );
    }

    public LankaspotResponse<AllFeatures> getAllFeatures(long categoryId) {
        List<FeatureDTO> features = featureRepository.findByCategoryId(categoryId)
                .stream()
                .map(this::mapToFeatureDTO)
                .toList();

        return LankaspotResponse.success(
                new AllFeatures(features),
                "All features retrieved successfully"
        );
    }

    public LankaspotResponse<AllFunctionalGroups> getAllFunctionalGroups() {
        List<FunctionalGroup> functionalGroups = functionalGroupRepository.findAll();

        return LankaspotResponse.success(
                new AllFunctionalGroups(functionalGroups),
                "All functional groups retrieved successfully"
        );
    }

    private CategoryDTO mapToCategoryDTO(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        return dto;
    }

    private DistrictDTO mapToDistrictDTO(District district) {
        DistrictDTO dto = new DistrictDTO();
        dto.setId(district.getId());
        dto.setName(district.getName());
        return dto;
    }

    private CityDTO mapToCityDTO(City city) {
        CityDTO dto = new CityDTO();
        dto.setId(city.getId());
        dto.setName(city.getName());
        return dto;
    }
    private FeatureDTO mapToFeatureDTO(Feature feature) {
        FeatureDTO dto = new FeatureDTO();
        dto.setId(feature.getId());
        dto.setName(feature.getName());
        dto.setValueType(feature.getValueType());
        dto.setUnit(feature.getUnit());
        return dto;
    }
}