package com.ablmvp.sprintassistant.config;

import com.ablmvp.sprintassistant.data.csv.CsvDataLoader;
import com.ablmvp.sprintassistant.data.result.LoadResult;
import com.ablmvp.sprintassistant.metrics.SprintMetricsService;
import com.ablmvp.sprintassistant.metrics.SprintHealthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class DatasetConfiguration {
    @Bean
    public CsvDataLoader csvDataLoader(DataSourceProperties properties) {
        return new CsvDataLoader(properties);
    }

    @Bean
    public LoadResult loadResult(CsvDataLoader loader) throws IOException {
        return loader.load();
    }

    @Bean
    public SprintMetricsService sprintMetricsService() {
        return new SprintMetricsService();
    }

    @Bean
    public SprintHealthService sprintHealthService() {
        return new SprintHealthService();
    }
}
