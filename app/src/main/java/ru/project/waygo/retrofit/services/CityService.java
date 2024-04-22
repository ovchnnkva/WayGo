package ru.project.waygo.retrofit.services;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import ru.project.waygo.dto.CityDto;

public interface CityService {
    @GET("api/city")
    Call<List<CityDto>> getByName(@Query("city") String name);
}
