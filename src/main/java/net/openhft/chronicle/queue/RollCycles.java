/*
 * Copyright 2016-2020 Chronicle Software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.queue;

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.time.TimeProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Roll cycles to use with the queue.
 * Sparse indexing roll cycles are useful for improving write performance but they slightly slow random access performance (with no effect for sequential reads)
 */
public enum RollCycles implements RollCycle {
    /**
     * Only good for testing
     */
    TEST_SECONDLY(/*---*/"yyyyMMdd-HHmmss'T'", 1000, 1 << 15, 4),
    /**
     * Only good for testing
     */
    TEST4_SECONDLY(/*---*/"yyyyMMdd-HHmmss'T4'", 1000, 32, 4),
    /**
     * 64 million entries per minute
     */
    MINUTELY(/*--------*/"yyyyMMdd-HHmm", 60 * 1000, 2 << 10, 16),
    /**
     * 512 entries per hour
     */
    TEST_HOURLY(/*-----*/"yyyyMMdd-HH'T'", 60 * 60 * 1000, 16, 4),
    /**
     * 256 million entries per hour, indexing every 16th entry
     */
    HOURLY(/*----------*/"yyyyMMdd-HH", 60 * 60 * 1000, 4 << 10, 16),
    /**
     * 2 billion entries per hour, indexing every 64th entry
     */
    LARGE_HOURLY(/*----*/"yyyyMMdd-HH'L'", 60 * 60 * 1000, 8 << 10, 64),
    /**
     * 16 billion entries per hour with sparse indexing (every 1024th entry)
     */
    LARGE_HOURLY_SPARSE("yyyyMMdd-HH'LS'", 60 * 60 * 1000, 4 << 10, 1024),
    /**
     * 16 billion entries per hour with super-sparse indexing (every (2^20)th entry)
     */
    LARGE_HOURLY_XSPARSE("yyyyMMdd-HH'LX'", 60 * 60 * 1000, 2 << 10, 1 << 20),
    /**
     * Only good for testing - 63 entries per day
     */
    TEST_DAILY(/*------*/"yyyyMMdd'T1'", 24 * 60 * 60 * 1000, 8, 1),
    /**
     * Only good for testing
     */
    TEST2_DAILY(/*-----*/"yyyyMMdd'T2'", 24 * 60 * 60 * 1000, 16, 2),
    /**
     * Only good for testing
     */
    TEST4_DAILY(/*-----*/"yyyyMMdd'T4'", 24 * 60 * 60 * 1000, 32, 4),
    /**
     * Only good for testing
     */
    TEST8_DAILY(/*-----*/"yyyyMMdd'T8'", 24 * 60 * 60 * 1000, 128, 8),
    /**
     * 512 million entries per day
     */
    SMALL_DAILY(/*-----*/"yyyyMMdd'S'", 24 * 60 * 60 * 1000, 8 << 10, 8),
    /**
     * 4 billion entries per day, indexing every 64th entry
     */
    DAILY(/*-----------*/"yyyyMMdd", 24 * 60 * 60 * 1000, 8 << 10, 64),
    /**
     * 128 billion entries per day, indexing every 128th entry
     */
    LARGE_DAILY(/*-----*/"yyyyMMdd'L'", 24 * 60 * 60 * 1000, 32 << 10, 128),
    /**
     * 4 trillion entries per day, indexing every 256th entry
     */
    XLARGE_DAILY(/*----*/"yyyyMMdd'X'", 24 * 60 * 60 * 1000, 128 << 10, 256),
    /**
     * 256 trillion entries per day with sparse indexing (every 1024th entry)
     */
    HUGE_DAILY(/*------*/"yyyyMMdd'H'", 24 * 60 * 60 * 1000, 512 << 10, 1024),
    /**
     * 256 trillion entries per day with super-sparse indexing (every (2^20)th entry)
     */
    HUGE_DAILY_XSPARSE("yyyyMMdd'HX'", 24 * 60 * 60 * 1000, 16 << 10, 1 << 20),
    ;

    // don't alter this or you will confuse yourself.
    private static final Iterable<RollCycles> VALUES = Arrays.asList(values());

    private final String format;
    private final int length;
    private final int cycleShift;
    private final int indexCount;
    private final int indexSpacing;
    private final long sequenceMask;

    public static Iterable<RollCycles> all() {
        return VALUES;
    }

    RollCycles(String format, int length, int indexCount, int indexSpacing) {
        this.format = format;
        this.length = length;
        this.indexCount = Maths.nextPower2(indexCount, 8);
        this.indexSpacing = Maths.nextPower2(indexSpacing, 1);
        cycleShift = Math.max(32, Maths.intLog2(indexCount) * 2 + Maths.intLog2(indexSpacing));
        sequenceMask = (1L << cycleShift) - 1;
    }

    @Override
    public String format() {
        return this.format;
    }

    @Override
    public int length() {
        return this.length;
    }

    /**
     *
     * @return this is the size of each index array, note: indexCount^2 is the maximum number of index queue entries.
     */
    @Override
    public int defaultIndexCount() {
        return indexCount;
    }

    @Override
    public int defaultIndexSpacing() {
        return indexSpacing;
    }

    @Override
    public int current(@NotNull TimeProvider time, long epoch) {
        return (int) ((time.currentTimeMillis() - epoch) / length());
    }

    @Override
    public long toIndex(int cycle, long sequenceNumber) {
        return ((long) cycle << cycleShift) + (sequenceNumber & sequenceMask);
    }

    @Override
    public long toSequenceNumber(long index) {
        return index & sequenceMask;
    }

    @Override
    public int toCycle(long index) {
        return Maths.toUInt31(index >> cycleShift);
    }
}
