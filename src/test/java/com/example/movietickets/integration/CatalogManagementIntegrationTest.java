package com.example.movietickets.integration;

import com.example.movietickets.dto.ApiModels;
import com.example.movietickets.dto.ApiModels.CityInput;
import com.example.movietickets.dto.ApiModels.HoldInput;
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
import com.example.movietickets.entity.TheaterSeat;
import com.example.movietickets.error.ApiException;
import com.example.movietickets.service.BookingService;
import com.example.movietickets.service.CatalogService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogManagementIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired CatalogService catalog;
    @Autowired BookingService bookings;

    @Test
    void adminCanEditAndDeleteUnreferencedCityButNotReferencedCity() throws Exception {
        mvc.perform(get("/api/admin/shows").with(httpBasic("admin", "adminpass")))
                .andExpect(status().isOk());
        String name = "AdminCity" + UUID.randomUUID().toString().substring(0, 8);
        String body = "{\"name\":\"" + name + "\",\"timeZone\":\"Asia/Kolkata\"}";
        mvc.perform(post("/api/admin/cities").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/cities").with(httpBasic("alice", "alicepass"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        String json = mvc.perform(post("/api/admin/cities").with(httpBasic("admin", "adminpass"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(json).get("id").asLong();
        mvc.perform(put("/api/admin/cities/" + id).with(httpBasic("admin", "adminpass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "Updated\",\"timeZone\":\"Asia/Kolkata\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/cities/" + id).with(httpBasic("admin", "adminpass")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/admin/theaters").with(httpBasic("admin", "adminpass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cityId\":" + id + ",\"name\":\"Hall\"}"))
                .andExpect(status().isCreated());
        mvc.perform(delete("/api/admin/cities/" + id).with(httpBasic("admin", "adminpass")))
                .andExpect(status().isConflict());
    }

    @Test
    void showPreventsOverlappingScheduleAndSeatLayoutChanges() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        City city = catalog.createCity(new ApiModels.CityInput("LockedCity" + suffix, "UTC"));
        Theater theater = catalog.createTheater(new ApiModels.TheaterInput(city.getId(), "LockedTheater"));
        TheaterSeat seat = catalog.createSeat(theater.getId(), new ApiModels.SeatInput("A1", SeatTier.REGULAR));
        Movie movie = catalog.createMovie(new ApiModels.MovieInput("LockedMovie", 120));
        RefundPolicy policy = catalog.createPolicy(new ApiModels.PolicyInput("LockedPolicy" + suffix, 24, 2,
                new BigDecimal("50")));
        Instant time = Instant.parse("2030-11-02T12:00:00Z");
        ApiModels.ShowInput request = new ApiModels.ShowInput(movie.getId(), theater.getId(), policy.getId(), time,
                new BigDecimal("100"), new BigDecimal("150"), BigDecimal.ZERO);
        MovieShow show = catalog.createShow(request);
        assertThrows(ApiException.class, () -> catalog.createSeat(theater.getId(),
                new ApiModels.SeatInput("A2", SeatTier.PREMIUM)));
        assertThrows(ApiException.class, () -> catalog.createShow(new ApiModels.ShowInput(movie.getId(), theater.getId(),
                policy.getId(), time.plusSeconds(1800), new BigDecimal("100"), new BigDecimal("150"), BigDecimal.ZERO)));
        catalog.updateShow(show.getId(), new ApiModels.ShowInput(movie.getId(), theater.getId(), policy.getId(), time,
                new BigDecimal("110"), new BigDecimal("150"), BigDecimal.ZERO));
        bookings.hold(new ApiModels.HoldInput(show.getId(), List.of(seat.getId()), null), "alice");
        assertThrows(ApiException.class, () -> catalog.updateShow(show.getId(), request));
    }

    @Test
    void invalidCityTimeZoneAndLengthReturnBadRequest() throws Exception {
        mvc.perform(post("/api/admin/cities").with(httpBasic("admin", "adminpass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"WrongZone\",\"timeZone\":\"Not/AZone\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/cities").with(httpBasic("admin", "adminpass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + "X".repeat(121) + "\",\"timeZone\":\"UTC\"}"))
                .andExpect(status().isBadRequest());
    }
}
