package ru.project.waygo.retrofit.services;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import ru.project.waygo.dto.route.RouteDTO;

public interface RouteService {
    @GET("api/route/{locationId}")
    Call<RouteDTO> getById(@Path("id") long id);

    @GET("api/route/all")
    Call<List<RouteDTO>> getByCityName(@Query("city") String cityName);
    
}
