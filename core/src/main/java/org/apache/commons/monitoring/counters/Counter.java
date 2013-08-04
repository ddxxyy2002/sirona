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

package org.apache.commons.monitoring.counters;

import org.apache.commons.monitoring.Role;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A <code>Metric</code> is a numerical indicator of some monitored application state with support for simple
 * statistics.
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public interface Counter {
    Key getKey();

    void reset();

    void add(double delta);

    void add(double delta, Unit unit);

    AtomicInteger currentConcurrency();

    void updateConcurrency(int concurrency);

    int getMaxConcurrency();

    // --- Statistical indicators --------------------------------------------

    double getMax();

    double getMin();

    long getHits();

    double getSum();

    double getStandardDeviation();

    double getVariance();

    double getMean();

    double getGeometricMean();

    double getSumOfLogs();

    double getSumOfSquares();

    public static class Key {
        private final String name;
        private final Role role;

        public Key(final Role role, final String name) {
            this.role = role;
            this.name = name;
        }

        @Override
        public String toString() {
            return "name=" + name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final Key key = (Key) o;
            return name.equals(key.name) && role.equals(key.role);

        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + role.hashCode();
            return result;
        }

        public String getName() {
            return name;
        }

        public Role getRole() {
            return role;
        }
    }
}