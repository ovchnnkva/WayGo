package ru.project.waygo.retrofit;

import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitConfiguration {

    public static String SERVER_URL = "http://158.160.13.203:8080/";
    public <T> T createService(Class<T> service){
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(interceptor)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SERVER_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory
                        .create(new GsonBuilder().setLenient().create()))
                .client(client)
                .build();
        return retrofit.create(service);
    }
}
