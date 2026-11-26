package com.lankaspot.server.dto.location;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GeoapifyGeocodingResponse {

    private List<Result> results;

    @Getter
    @Setter
    public static class Result {

        private Double lat;
        private Double lon;
        private String formatted;
        private String name;
        private String city;
        private String district;

    }
}