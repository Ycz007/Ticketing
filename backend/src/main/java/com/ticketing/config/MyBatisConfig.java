package com.ticketing.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.ticketing", annotationClass = org.apache.ibatis.annotations.Mapper.class)
public class MyBatisConfig { }
