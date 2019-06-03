package com.google.allenday;

import org.apache.avro.reflect.Nullable;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.transforms.Combine;

public class MinMaxMeanFn extends Combine.CombineFn<Long, MinMaxMeanFn.Accum, Stats> {
    @DefaultCoder(AvroCoder.class)
    static class Accum {
        long sum = 0;
        long count = 0;
        @Nullable
        Long min;
        @Nullable
        Long max;
    }

    @Override
    public Accum createAccumulator() { return new Accum(); }

    @Override
    public Accum addInput(Accum accum, Long input) {
        accum.sum += input;
        accum.count++;

        if (accum.min == null) {
            accum.min = input;
        } else {
            accum.min = Math.min(accum.min, input);
        }

        if (accum.max == null) {
            accum.max = input;
        } else {
            accum.max = Math.max(accum.max, input);
        }

        return accum;
    }

    @Override
    public Accum mergeAccumulators(Iterable<Accum> accums) {
        Accum merged = createAccumulator();
        for (Accum accum : accums) {
            merged.sum += accum.sum;
            merged.count += accum.count;

            if (merged.min == null) {
                merged.min = accum.min;
            } else {
                merged.min = Math.min(merged.min, accum.min);
            }

            if (merged.max == null) {
                merged.max = accum.max;
            } else {
                merged.max = Math.max(merged.max, accum.max);
            }
        }
        return merged;
    }

    @Override
    public Stats extractOutput(Accum accum) {
        return new Stats(accum.min, accum.max, ((double) accum.sum) / accum.count);
    }
}