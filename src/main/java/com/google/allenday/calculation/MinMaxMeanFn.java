package com.google.allenday.calculation;

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

        Accum() {}

        Accum(long sum, long count, Long min, Long max){
            this.sum = sum;
            this.count = count;
            this.min = min;
            this.max = max;
        }

        Accum merge(Accum input) {
            sum += input.sum;
            count += input.count;
            if (input.min != null) {
                if (min == null) {
                    min = input.min;
                } else {
                    min = Math.min(min, input.min);
                }
            }

            if (input.max != null) {
                if (max == null) {
                    max = input.max;
                } else {
                    max = Math.max(max, input.max);
                }
            }

            return this;
        }
    }

    @Override
    public Accum createAccumulator() {
        return new Accum();
    }

    @Override
    public Accum addInput(Accum accum, Long input) {
        return accum.merge(new Accum(input, 1, input, input));
    }

    @Override
    public Accum mergeAccumulators(Iterable<Accum> accums) {
        Accum merged = createAccumulator();
        for (Accum accum : accums) {
            merged.merge(accum);
        }

        return merged;
    }

    @Override
    public Stats extractOutput(Accum accum) {
        return new Stats(accum.count, accum.min, accum.max, ((double) accum.sum) / accum.count);
    }
}