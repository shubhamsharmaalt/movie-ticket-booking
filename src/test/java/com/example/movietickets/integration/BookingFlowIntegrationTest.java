package com.example.movietickets.integration;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void competingCustomersCannotBookTheSameSeatAndCancellationReleasesIt() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long city = adminPost("/api/admin/cities", "{\"name\":\"City" + suffix + "\",\"timeZone\":\"Asia/Kolkata\"}");
        long theater = adminPost("/api/admin/theaters", "{\"cityId\":" + city + ",\"name\":\"Theater" + suffix + "\"}");
        long seat = adminPost("/api/admin/theaters/" + theater + "/seats", "{\"label\":\"A1\",\"tier\":\"REGULAR\"}");
        long movie = adminPost("/api/admin/movies", "{\"title\":\"Movie" + suffix + "\",\"durationMinutes\":120}");
        long policy = adminPost("/api/admin/refund-policies", "{\"name\":\"Policy" + suffix + "\",\"fullRefundHours\":24,\"partialRefundHours\":2,\"partialPercent\":50}");
        long show = adminPost("/api/admin/shows", "{\"movieId\":" + movie + ",\"theaterId\":" + theater
                + ",\"refundPolicyId\":" + policy + ",\"startsAt\":\"2030-11-02T12:00:00Z\",\"regularPrice\":100,\"premiumPrice\":150,\"weekendSurchargePercent\":20}");
        mvc.perform(get("/api/shows/" + show + "/seats")).andExpect(status().isOk());

        String holdBody = "{\"showId\":" + show + ",\"seatIds\":[" + seat + "]}";
        long aliceBooking = customerPost("alice", "/api/bookings/holds", holdBody, 201);
        customerPost("bob", "/api/bookings/holds", holdBody, 409);
        mvc.perform(get("/api/bookings/" + aliceBooking).with(httpBasic("bob", "bobpass")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/bookings/" + aliceBooking + "/payments").with(httpBasic("bob", "bobpass"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"outcome\":\"SUCCESS\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/bookings/" + aliceBooking + "/payments").with(httpBasic("alice", "alicepass"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"outcome\":\"SUCCESS\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/bookings").with(httpBasic("alice", "alicepass")))
                .andExpect(status().isOk());
        customerPost("bob", "/api/bookings/holds", holdBody, 409);
        mvc.perform(post("/api/bookings/" + aliceBooking + "/cancellation").with(httpBasic("bob", "bobpass")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/bookings/" + aliceBooking + "/cancellation").with(httpBasic("alice", "alicepass")))
                .andExpect(status().isOk());
        customerPost("bob", "/api/bookings/holds", holdBody, 201);
    }

    private long adminPost(String path, String body) throws Exception {
        String json = mvc.perform(post(path).with(httpBasic("admin", "adminpass"))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).get("id").asLong();
    }

    private long customerPost(String user, String path, String body, int status) throws Exception {
        String json = mvc.perform(post(path).with(httpBasic(user, user + "pass"))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(status))
                .andReturn().getResponse().getContentAsString();
        JsonNode result = mapper.readTree(json);
        return result.has("id") ? result.get("id").asLong() : -1;
    }
}
