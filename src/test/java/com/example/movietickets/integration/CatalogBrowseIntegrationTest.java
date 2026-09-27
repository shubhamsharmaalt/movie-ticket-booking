package com.example.movietickets.integration;

import com.example.movietickets.dto.ApiModels.CityInput;
import com.example.movietickets.dto.ApiModels.MovieInput;
import com.example.movietickets.dto.ApiModels.PolicyInput;
import com.example.movietickets.dto.ApiModels.SeatInput;
import com.example.movietickets.dto.ApiModels.ShowInput;
import com.example.movietickets.dto.ApiModels.TheaterInput;
import com.example.movietickets.entity.City;
import com.example.movietickets.entity.Movie;
import com.example.movietickets.entity.MovieShow;
import com.example.movietickets.entity.RefundPolicy;
import com.example.movietickets.entity.SeatTier;
import com.example.movietickets.entity.Theater;
import com.example.movietickets.service.CatalogService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogBrowseIntegrationTest {
    @Autowired CatalogService catalog;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void publicCanBrowseTheatersAndShowsForOneCityAndLocalDate() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        City first = catalog.createCity(new CityInput("BrowseA" + suffix, "UTC"));
        City second = catalog.createCity(new CityInput("BrowseB" + suffix, "UTC"));
        Theater firstTheater = catalog.createTheater(new TheaterInput(first.getId(), "HallA"));
        Theater secondTheater = catalog.createTheater(new TheaterInput(second.getId(), "HallB"));
        catalog.createSeat(firstTheater.getId(), new SeatInput("A1", SeatTier.REGULAR));
        catalog.createSeat(secondTheater.getId(), new SeatInput("A1", SeatTier.REGULAR));
        Movie movie = catalog.createMovie(new MovieInput("BrowseMovie", 90));
        RefundPolicy policy = catalog.createPolicy(new PolicyInput("BrowsePolicy" + suffix, 24, 2, new BigDecimal("50")));
        Instant when = Instant.parse("2030-11-02T12:00:00Z");
        MovieShow expected = catalog.createShow(new ShowInput(movie.getId(), firstTheater.getId(), policy.getId(), when,
                new BigDecimal("100"), new BigDecimal("150"), BigDecimal.ZERO));
        catalog.createShow(new ShowInput(movie.getId(), secondTheater.getId(), policy.getId(), when,
                new BigDecimal("100"), new BigDecimal("150"), BigDecimal.ZERO));

        String theaters = mvc.perform(get("/api/theaters?cityId=" + first.getId())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(firstTheater.getId(), mapper.readTree(theaters).get(0).get("id").asLong());
        String shows = mvc.perform(get("/api/shows?cityId=" + first.getId() + "&date=2030-11-02"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(1, mapper.readTree(shows).get("content").size());
        assertEquals(expected.getId(), mapper.readTree(shows).get("content").get(0).get("id").asLong());
    }
}
