package ch.heigvd.iict.dmg.labo1.similarities;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.similarities.ClassicSimilarity;

public class MySimilarity extends ClassicSimilarity {
    @Override
    public float tf(float freq) {
        return 1 + (float)Math.log(freq);
    }

    @Override
    public float idf(long docFreq, long numDocs) {
        return (float)Math.log(numDocs/((float)docFreq + 1)) + 1;
    }

    @Override
    public float coord(int overlap, int maxOverlap) {
        return (float)Math.sqrt(overlap/(float)maxOverlap);
    }

    @Override
    public float lengthNorm(FieldInvertState state) {
        return 1;
    }
}
