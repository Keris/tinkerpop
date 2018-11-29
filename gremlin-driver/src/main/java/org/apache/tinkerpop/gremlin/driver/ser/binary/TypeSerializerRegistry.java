/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.driver.ser.binary;

import org.apache.tinkerpop.gremlin.driver.ser.SerializationException;
import org.apache.tinkerpop.gremlin.driver.ser.binary.types.*;
import org.apache.tinkerpop.gremlin.process.traversal.Bytecode;

import java.util.*;

public class TypeSerializerRegistry {
    public static final TypeSerializerRegistry INSTANCE = build().create();

    public static Builder build() {
        return new Builder();
    }

    public static class Builder {
        private final List<RegistryEntry> list = new LinkedList<>(Arrays.asList(
                new RegistryEntry<>(String.class, new StringSerializer()),
                new RegistryEntry<>(UUID.class, new UUIDSerializer()),
                new RegistryEntry<>(Map.class, new MapSerializer()),
                new RegistryEntry<>(List.class, new ListSerializer()),
                new RegistryEntry<>(Set.class, new SetSerializer()),
                new RegistryEntry<>(Integer.class, SingleTypeSerializer.IntSerializer),
                new RegistryEntry<>(Long.class, SingleTypeSerializer.LongSerializer),
                new RegistryEntry<>(Double.class, SingleTypeSerializer.DoubleSerializer),
                new RegistryEntry<>(Float.class, SingleTypeSerializer.FloatSerializer),
                new RegistryEntry<>(Short.class, SingleTypeSerializer.ShortSerializer),
                new RegistryEntry<>(Boolean.class, SingleTypeSerializer.BooleanSerializer),
                new RegistryEntry<>(Byte.class, SingleTypeSerializer.ByteSerializer),
                new RegistryEntry<>(Class.class, new ClassSerializer()),
                new RegistryEntry<>(Date.class, new DateSerializer(DataType.TIMESTAMP)),
                new RegistryEntry<>(Date.class, new DateSerializer(DataType.DATE)),
                new RegistryEntry<>(Bytecode.class, new ByteCodeSerializer())));

        /**
         * Adds a serializer for a built-in type.
         */
        public <T> Builder add(final Class<T> type, final TypeSerializer<T> serializer) {
            if (serializer.getDataType() == DataType.CUSTOM) {
                throw new IllegalArgumentException("DataType can not be CUSTOM, use addCustomType() method instead");
            }

            if (serializer.getDataType() == DataType.UNSPECIFIED_NULL) {
                throw new IllegalArgumentException("Adding a serializer for a UNSPECIFIED_NULL is not permitted");
            }

            if (serializer instanceof CustomTypeSerializer) {
                throw new IllegalArgumentException(
                        "CustomTypeSerializer implementations are reserved for customtypes");
            }

            list.add(new RegistryEntry<>(type, serializer));
            return this;
        }

        /**
         * Adds a serializer for a custom type.
         */
        public <T> Builder addCustomType(final Class<T> type, final CustomTypeSerializer<T> serializer) {
            if (serializer == null) {
                throw new NullPointerException("serializer can not be null");
            }

            if (serializer.getDataType() != DataType.CUSTOM) {
                throw new IllegalArgumentException("Custom serializer must use CUSTOM data type");
            }

            if (serializer.getTypeName() == null) {
                throw new NullPointerException("serializer custom type name can not be null");
            }

            list.add(new RegistryEntry<>(type, serializer));
            return this;
        }

        /**
         * Creates a new {@link TypeSerializerRegistry} instance based on the serializers added.
         */
        public TypeSerializerRegistry create() {
            return new TypeSerializerRegistry(list);
        }
    }

    private static class RegistryEntry<T> {
        private final Class<T> type;
        private final TypeSerializer<T> typeSerializer;

        private RegistryEntry(Class<T> type, TypeSerializer<T> typeSerializer) {
            this.type = type;
            this.typeSerializer = typeSerializer;
        }

        public Class<T> getType() {
            return type;
        }

        public DataType getDataType() {
            return typeSerializer.getDataType();
        }

        public String getCustomTypeName() {
            if (getDataType() != DataType.CUSTOM) {
                return null;
            }

            CustomTypeSerializer customTypeSerializer = (CustomTypeSerializer) typeSerializer;
            return customTypeSerializer.getTypeName();
        }

        public TypeSerializer<T> getTypeSerializer() {
            return typeSerializer;
        }
    }

    private final Map<Class<?>, TypeSerializer<?>> serializers = new HashMap<>();
    private final Map<Class<?>, TypeSerializer<?>> serializersByInterface = new HashMap<>();
    private final Map<DataType, TypeSerializer<?>> serializersByDataType = new HashMap<>();
    private final Map<String, CustomTypeSerializer> serializersByCustomTypeName = new HashMap<>();

    private TypeSerializerRegistry(final Collection<RegistryEntry> entries) {
        for (RegistryEntry entry : entries) {
            put(entry);
        }
    }

    private void put(final RegistryEntry entry) {
        final Class type = entry.getType();
        final TypeSerializer serializer = entry.getTypeSerializer();

        if (type == null) {
            throw new NullPointerException("Type can not be null");
        }

        if (serializer == null) {
            throw new NullPointerException("Serializer instance can not be null");
        }

        if (serializer.getDataType() == null) {
            throw new NullPointerException("Serializer data type can not be null");
        }

        if (!type.isInterface()) {
            // Direct class match
            serializers.put(type, serializer);
        } else {
            // Interface can be assigned by provided type
            serializersByInterface.put(type, serializer);
        }

        if (serializer.getDataType() != DataType.CUSTOM) {
            serializersByDataType.put(serializer.getDataType(), serializer);
        } else {
            serializersByCustomTypeName.put(entry.getCustomTypeName(), (CustomTypeSerializer) serializer);
        }
    }

    public <T> TypeSerializer<T> getSerializer(final Class<T> type) throws SerializationException {
        TypeSerializer<?> serializer = serializers.get(type);

        if (serializer == null) {
            // Find by interface
            for (Map.Entry<Class<?>, TypeSerializer<?>> entry : serializersByInterface.entrySet()) {
                if (entry.getKey().isAssignableFrom(type)) {
                    serializer = entry.getValue();
                    break;
                }
            }
        }

        return validateInstance(serializer, type.getTypeName());
    }

    public <T> TypeSerializer<T> getSerializer(final DataType dataType) throws SerializationException {
        if (dataType == DataType.CUSTOM) {
            throw new IllegalArgumentException("Custom type serializers can not be retrieved using this method");
        }

        return validateInstance(serializersByDataType.get(dataType), dataType.toString());
    }

    /**
     * Gets the serializer for a given custom type name.
     */
    public <T> CustomTypeSerializer<T> getSerializerForCustomType(final String name) throws SerializationException {
        CustomTypeSerializer serializer = serializersByCustomTypeName.get(name);

        if (serializer == null) {
            throw new SerializationException(String.format("Serializer for custom type '%s' not found", name));
        }

        return serializer;
    }

    private static TypeSerializer validateInstance(final TypeSerializer serializer, final String typeName) throws SerializationException {
        if (serializer == null) {
            throw new SerializationException(String.format("Serializer for type %s not found", typeName));
        }

        return serializer;
    }
}
