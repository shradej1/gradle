/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal;

import org.gradle.api.DynamicExtension;

import java.util.Map;

public class DynamicExtensionDynamicObjectAdapter extends BeanDynamicObject {

    private final DynamicExtension extension;

    public DynamicExtensionDynamicObjectAdapter(DynamicExtension extension) {
        super(extension);
        this.extension = extension;
    }

    public boolean hasProperty(String name) {
        return super.hasProperty(name) || extension.has(name);
    }

    public Map<String, ?> getProperties() {
        return extension.getProperties();
    }

    protected DynamicExtension getExtension() {
        return extension;
    }
}