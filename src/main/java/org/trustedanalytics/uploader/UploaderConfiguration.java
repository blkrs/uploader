/**
 * Copyright (c) 2015 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trustedanalytics.uploader;

import org.trustedanalytics.store.ObjectStore;
import org.trustedanalytics.store.ObjectStoreConfiguration;
import org.trustedanalytics.uploader.client.DataAcquisitionClient;
import org.trustedanalytics.uploader.client.ScramblingSlf4jLogger;
import org.trustedanalytics.uploader.client.UserManagementClient;
import org.trustedanalytics.uploader.core.stream.consumer.ObjectStoreStreamConsumer;
import org.trustedanalytics.uploader.core.stream.decoder.GzipStreamDecoder;
import org.trustedanalytics.uploader.core.stream.decoder.ZipStreamDecoder;
import org.trustedanalytics.uploader.rest.UploadCompleted.UploadCompletedBuilder;
import org.trustedanalytics.uploader.security.OAuth2TokenExtractor;

import feign.Feign;
import feign.Logger;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;

import java.io.InputStream;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Configuration
@Import(ObjectStoreConfiguration.class)
public class UploaderConfiguration {

    @Value("${services.dataacquisition.url}")
    private String dataAcquisitionUrl;

    @Value("${services.user-management.url}")
    private String userManagementUrl;

    @Bean
    public Function<InputStream, InputStream> streamDecoder() {
        return zipStreamDecoder().andThen(gzipStreamDecoder());
    }

    @Bean
    public ZipStreamDecoder zipStreamDecoder() {
        return new ZipStreamDecoder();
    }

    @Bean
    public GzipStreamDecoder gzipStreamDecoder() {
        return new GzipStreamDecoder();
    }

    @Bean
    public BiConsumer<InputStream, UploadCompletedBuilder> streamConsumer(ObjectStore store) {
        return new ObjectStoreStreamConsumer(store);
    }

    @Bean
    public Function<Authentication, String> tokenExtractor() {
        return new OAuth2TokenExtractor();
    }

    @Bean
    public DataAcquisitionClient dataAcquisitionClient() {
        return Feign.builder()
            .encoder(new JacksonEncoder())
            .decoder(new JacksonDecoder())
            .logger(new ScramblingSlf4jLogger(DataAcquisitionClient.class))
            .logLevel(Logger.Level.FULL)
            .target(DataAcquisitionClient.class, dataAcquisitionUrl);
    }

    @Bean
    public UserManagementClient userManagementClient() {
        return Feign.builder()
            .encoder(new JacksonEncoder())
            .decoder(new JacksonDecoder())
            .logger(new ScramblingSlf4jLogger(UserManagementClient.class))
            .logLevel(Logger.Level.FULL)
            .target(UserManagementClient.class, userManagementUrl);
    }
}
