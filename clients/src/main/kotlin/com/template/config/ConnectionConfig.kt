package com.template.config

import com.template.config.NodeRPCConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class ConnectionConfig{

    @Bean
    open fun bomAirportConnection(): NodeRPCConnection {
        return NodeRPCConnection("localhost", "user1", "test", 10006)
    }

    @Bean
    open fun amsAirportConnection():NodeRPCConnection{
        return NodeRPCConnection("localhost","user1","test", 10009)
    }
}