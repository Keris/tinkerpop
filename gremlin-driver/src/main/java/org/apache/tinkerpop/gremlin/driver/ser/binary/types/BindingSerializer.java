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
package org.apache.tinkerpop.gremlin.driver.ser.binary.types;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import org.apache.tinkerpop.gremlin.driver.ser.SerializationException;
import org.apache.tinkerpop.gremlin.driver.ser.binary.DataType;
import org.apache.tinkerpop.gremlin.driver.ser.binary.GraphBinaryReader;
import org.apache.tinkerpop.gremlin.driver.ser.binary.GraphBinaryWriter;
import org.apache.tinkerpop.gremlin.process.traversal.Bytecode;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class BindingSerializer extends SimpleTypeSerializer<Bytecode.Binding> {

    public BindingSerializer() {
        super(DataType.BINDING);
    }

    @Override
    Bytecode.Binding readValue(final ByteBuf buffer, final GraphBinaryReader context) throws SerializationException {
        final String k = context.readValue(buffer, String.class, false);
        return new Bytecode.Binding<>(k, context.read(buffer));
    }

    @Override
    public ByteBuf writeValue(final Bytecode.Binding value, final ByteBufAllocator allocator, final GraphBinaryWriter context) throws SerializationException {
        final CompositeByteBuf result = allocator.compositeBuffer(2);
        result.addComponent(true, context.writeValue(value.variable(), allocator, false));
        result.addComponent(true, context.write(value.value(), allocator));
        return result;
    }
}
