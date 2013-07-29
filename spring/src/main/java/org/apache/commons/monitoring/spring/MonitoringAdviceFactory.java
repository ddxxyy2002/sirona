/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.monitoring.spring;

import org.aopalliance.aop.Advice;
import org.apache.commons.monitoring.instrumentation.aop.MonitorNameExtractor;
import org.apache.commons.monitoring.spring.aop.AopaliancePerformanceInterceptor;

/**
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class MonitoringAdviceFactory {

    public static Advice getAdvice(MonitoringConfigSource source) {
        AopaliancePerformanceInterceptor interceptor = new AopaliancePerformanceInterceptor();
        if (source.getCategory() != null) {
            interceptor.setCategory(source.getCategory());
        }
        if (source.getMonitorNameExtractor() != null) {
            interceptor.setMonitorNameExtractor(source.getMonitorNameExtractor());
        }
        return interceptor;
    }

    public interface MonitoringConfigSource {
        /**
         * @return the category
         */
        public abstract String getCategory();

        /**
         * @return the monitorNameExtractor
         */
        public abstract MonitorNameExtractor getMonitorNameExtractor();

    }

}
