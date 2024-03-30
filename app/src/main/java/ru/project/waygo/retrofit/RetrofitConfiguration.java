package ru.project.waygo.retrofit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitConfiguration {
    public <T> T createService(Class<T> service){
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        OkHttpClient.Builder client = new OkHttpClient.Builder()
                .addInterceptor(interceptor);
        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl("http://192.168.31.70:8080/")
                .baseUrl("http://192.168.31.6:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .client(client.build())
                .build();
        return retrofit.create(service);
    }
}
