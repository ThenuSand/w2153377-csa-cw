package com.westminster.smartcampus.config;

import com.westminster.smartcampus.filter.LoggingFilter;
import com.westminster.smartcampus.mapper.GenericExceptionMapper;
import com.westminster.smartcampus.mapper.LinkedResourceNotFoundExceptionMapper;
import com.westminster.smartcampus.mapper.NotFoundExceptionMapper;
import com.westminster.smartcampus.mapper.RoomNotEmptyExceptionMapper;
import com.westminster.smartcampus.mapper.SensorUnavailableExceptionMapper;
import com.westminster.smartcampus.resource.DiscoveryResource;
import com.westminster.smartcampus.resource.RoomResource;
import com.westminster.smartcampus.resource.SensorResource;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS Application subclass. The @ApplicationPath defines the root
 * of every endpoint, and getClasses() explicitly registers resources,
 * mappers and filters so we do not rely on classpath scanning.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // Resources
        classes.add(DiscoveryResource.class);
        classes.add(RoomResource.class);
        classes.add(SensorResource.class);

        // Exception mappers
        classes.add(RoomNotEmptyExceptionMapper.class);
        classes.add(LinkedResourceNotFoundExceptionMapper.class);
        classes.add(SensorUnavailableExceptionMapper.class);
        classes.add(NotFoundExceptionMapper.class);
        classes.add(GenericExceptionMapper.class);

        // Filters
        classes.add(LoggingFilter.class);

        return classes;
    }
}
