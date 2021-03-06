/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.telemetry.prometheus;

import io.prometheus.client.SimpleCollector;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.skywalking.oap.server.telemetry.api.*;

/**
 * BaseMetric parent class represents the me
 *
 * @author wusheng
 */
public abstract class BaseMetric<T extends SimpleCollector, C> {
    private static Map<String, Object> ALL_METRICS = new HashMap<>();

    private volatile C metricInstance;
    protected final String name;
    protected final String tips;
    protected final MetricTag.Keys labels;
    protected final MetricTag.Values values;
    private ReentrantLock lock = new ReentrantLock();

    public BaseMetric(String name, String tips, MetricTag.Keys labels,
        MetricTag.Values values) {
        this.name = name;
        this.tips = tips;
        this.labels = labels;
        this.values = values;
    }

    protected boolean isIDReady() {
        return TelemetryRelatedContext.INSTANCE.getId() != null;
    }

    protected C getMetric() {
        if (metricInstance == null) {
            if (isIDReady()) {
                lock.lock();
                try {
                    if (metricInstance == null) {
                        String[] labelNames = new String[labels.getKeys().length + 1];
                        labelNames[0] = "instance";
                        for (int i = 0; i < labels.getKeys().length; i++) {
                            labelNames[i + 1] = labels.getKeys()[i];
                        }

                        String[] labelValues = new String[values.getValues().length + 1];
                        labelValues[0] = TelemetryRelatedContext.INSTANCE.getId();
                        for (int i = 0; i < values.getValues().length; i++) {
                            labelValues[i + 1] = values.getValues()[i];
                        }

                        if (!ALL_METRICS.containsKey(name)) {
                            synchronized (ALL_METRICS) {
                                if (!ALL_METRICS.containsKey(name)) {
                                    ALL_METRICS.put(name, create(labelNames));
                                }
                            }
                        }

                        T metric = (T)ALL_METRICS.get(name);

                        metricInstance = (C)metric.labels(labelValues);
                    }
                } finally {
                    lock.unlock();
                }
            }
        }

        return metricInstance;
    }

    protected abstract T create(String[] labelNames);
}
