package ru.project.waygo.retrofit.services;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CityService {
    @GET("api/city")
    Call<String> getByName(@Query("city") String name);
}
