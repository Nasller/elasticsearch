/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.compute.operator.mvdedupe;

import org.elasticsearch.common.util.LongLongHash;
import org.elasticsearch.compute.aggregation.GroupingAggregatorFunction;
import org.elasticsearch.compute.aggregation.blockhash.AddBlock;
import org.elasticsearch.compute.aggregation.blockhash.BlockHash;
import org.elasticsearch.compute.data.BlockFactory;
import org.elasticsearch.compute.data.IntBlock;
import org.elasticsearch.compute.data.LongBlock;

public class IntLongBlockAdd extends AddBlock {
    private final LongLongHash hash;
    private final MultivalueDedupeInt block1;
    private final MultivalueDedupeLong block2;

    public IntLongBlockAdd(
        BlockFactory blockFactory,
        int emitBatchSize,
        GroupingAggregatorFunction.AddInput addInput,
        LongLongHash hash,
        IntBlock block1,
        LongBlock block2
    ) {
        super(blockFactory, emitBatchSize, addInput);
        this.hash = hash;
        this.block1 = new MultivalueDedupeInt(block1);
        this.block2 = new MultivalueDedupeLong(block2);
    }

    public void add() {
        int positions = block1.block.getPositionCount();
        for (int p = 0; p < positions; p++) {
            add1(p);
        }
        emitOrds();
    }

    private void add1(int position) {
        int first = block1.block.getFirstValueIndex(position);
        int count = block1.block.getValueCount(position);
        switch (count) {
            case 0 -> {
                appendNullSv(position);
            }
            case 1 -> {
                block1.w = 1;
                block1.work[0] = block1.block.getInt(first);
                add2(position, true);
            }
            default -> {
                if (count < MultivalueDedupeLong.ALWAYS_COPY_MISSING) {
                    block1.copyMissing(first, count);
                    add2(position, true);
                } else {
                    block1.copyAndSort(first, count);
                    add2(position, false);
                }
            }
        }
    }

    private void add2(int position, boolean work1IsUnique) {
        int first = block2.block.getFirstValueIndex(position);
        int count = block2.block.getValueCount(position);
        switch (count) {
            case 0 -> {
                appendNullSv(position);
            }
            case 1 -> {
                block2.w = 1;
                block2.work[0] = block2.block.getLong(first);
                finishAdd(position, work1IsUnique, true);
            }
            default -> {
                if (count < MultivalueDedupeLong.ALWAYS_COPY_MISSING) {
                    block2.copyMissing(first, count);
                    finishAdd(position, work1IsUnique, true);
                } else {
                    block2.copyAndSort(first, count);
                    finishAdd(position, work1IsUnique, false);
                }
            }
        }

    }

    private void finishAdd(int position, boolean work1IsUnique, boolean work2IsUnique) {
        if (block1.w == 1) {
            if (block2.w == 1) {
                appendOrdSv(position, Math.toIntExact(BlockHash.hashOrdToGroup(hash.add(block1.work[0], block2.work[0]))));
                return;
            }
        }
        if (work1IsUnique) {
            if (work2IsUnique) {
                finishAddUniqueUnique(position);
            } else {
                finishAddUniqueSorted(position);
            }
        } else {
            if (work2IsUnique) {
                finishAddSortedUnique(position);
            } else {
                finishAddSortedSorted(position);
            }
        }
        finishMv();
    }

    private void finishAddUniqueUnique(int position) {
        for (int i = 0; i < block1.w; i++) {
            finishAddUnique(position, block1.work[i]);
        }
    }

    private void finishAddUniqueSorted(int position) {
        for (int i = 0; i < block1.w; i++) {
            finishAddSorted(position, block1.work[i]);
        }
    }

    private void finishAddSortedUnique(int position) {
        int prev1 = block1.work[0];
        finishAddUnique(position, prev1);
        for (int i = 1; i < block1.w; i++) {
            if (prev1 == block1.work[i]) {
                continue;
            }
            prev1 = block1.work[i];
            finishAddUnique(position, prev1);
        }
    }

    private void finishAddSortedSorted(int position) {
        int prev1 = block1.work[0];
        finishAddSorted(position, prev1);
        for (int i = 1; i < block1.w; i++) {
            if (prev1 == block1.work[i]) {
                continue;
            }
            prev1 = block1.work[i];
            finishAddSorted(position, prev1);
        }
    }

    private void finishAddUnique(int position, int v1) {
        for (int i = 0; i < block2.w; i++) {
            appendOrdInMv(position, Math.toIntExact(BlockHash.hashOrdToGroup(hash.add(v1, block2.work[i]))));
        }
    }

    private void finishAddSorted(int position, int v1) {
        long prev2 = block2.work[0];
        appendOrdInMv(position, Math.toIntExact(BlockHash.hashOrdToGroup(hash.add(v1, prev2))));
        for (int i = 1; i < block2.w; i++) {
            if (prev2 == block2.work[i]) {
                continue;
            }
            prev2 = block2.work[i];
            appendOrdInMv(position, Math.toIntExact(BlockHash.hashOrdToGroup(hash.add(v1, prev2))));
        }
    }
}
