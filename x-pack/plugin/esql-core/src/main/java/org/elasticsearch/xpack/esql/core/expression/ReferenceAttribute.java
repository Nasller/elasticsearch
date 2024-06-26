/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.esql.core.expression;

import org.elasticsearch.xpack.esql.core.tree.NodeInfo;
import org.elasticsearch.xpack.esql.core.tree.Source;
import org.elasticsearch.xpack.esql.core.type.DataType;

/**
 * Attribute based on a reference to an expression.
 */
public class ReferenceAttribute extends TypedAttribute {

    public ReferenceAttribute(Source source, String name, DataType dataType) {
        this(source, name, dataType, null, Nullability.FALSE, null, false);
    }

    public ReferenceAttribute(
        Source source,
        String name,
        DataType dataType,
        String qualifier,
        Nullability nullability,
        NameId id,
        boolean synthetic
    ) {
        super(source, name, dataType, qualifier, nullability, id, synthetic);
    }

    @Override
    protected Attribute clone(
        Source source,
        String name,
        DataType dataType,
        String qualifier,
        Nullability nullability,
        NameId id,
        boolean synthetic
    ) {
        return new ReferenceAttribute(source, name, dataType, qualifier, nullability, id, synthetic);
    }

    @Override
    protected NodeInfo<ReferenceAttribute> info() {
        return NodeInfo.create(this, ReferenceAttribute::new, name(), dataType(), qualifier(), nullable(), id(), synthetic());
    }

    @Override
    protected String label() {
        return "r";
    }
}
