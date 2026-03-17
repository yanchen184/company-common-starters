package com.company.common.security.service;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Pure-Java formant-based speech synthesizer for digit words (zero through nine).
 * <p>
 * Each digit word is decomposed into phoneme segments. Each phoneme is approximated
 * by its first two formant frequencies (F1, F2) for vowels, or noise/burst characteristics
 * for consonants. The result is robotic but recognizable speech.
 * <p>
 * This is intentionally simple -- no external dependencies, no pre-recorded audio.
 * Quality is "telephone robot" level, which is acceptable for CAPTCHA accessibility.
 */
class FormantSpeechSynthesizer {

    private static final float SAMPLE_RATE = 16000f;
    private static final Random RNG = new Random();

    /**
     * Phoneme segment descriptor.
     */
    private record Phoneme(
            Type type,
            double durationMs,
            double f1,       // first formant (Hz) - used for vowels
            double f2,       // second formant (Hz) - used for vowels
            double f0,       // fundamental frequency (pitch) for voiced sounds
            double noiseFreq // center frequency for noise-based consonants
    ) {
        enum Type {
            VOWEL,          // voiced vowel (formant synthesis)
            NASAL,          // voiced nasal (low formants, nasal buzz)
            FRICATIVE,      // unvoiced noise (s, f, th)
            VOICED_FRIC,    // voiced fricative (v, z)
            PLOSIVE_BURST,  // brief burst (t, k, p release)
            SILENCE         // gap / stop closure
        }

        static Phoneme vowel(double durationMs, double f1, double f2) {
            return new Phoneme(Type.VOWEL, durationMs, f1, f2, 130.0, 0);
        }

        static Phoneme vowel(double durationMs, double f1, double f2, double f0) {
            return new Phoneme(Type.VOWEL, durationMs, f1, f2, f0, 0);
        }

        static Phoneme nasal(double durationMs, double f1, double f2) {
            return new Phoneme(Type.NASAL, durationMs, f1, f2, 130.0, 0);
        }

        static Phoneme fricative(double durationMs, double noiseFreq) {
            return new Phoneme(Type.FRICATIVE, durationMs, 0, 0, 0, noiseFreq);
        }

        static Phoneme voicedFric(double durationMs, double f1, double f2, double noiseFreq) {
            return new Phoneme(Type.VOICED_FRIC, durationMs, f1, f2, 130.0, noiseFreq);
        }

        static Phoneme burst(double durationMs, double noiseFreq) {
            return new Phoneme(Type.PLOSIVE_BURST, durationMs, 0, 0, 0, noiseFreq);
        }

        static Phoneme silence(double durationMs) {
            return new Phoneme(Type.SILENCE, durationMs, 0, 0, 0, 0);
        }
    }

    /**
     * Phoneme sequences for each digit word.
     * F1/F2 values are approximate mid-American English formant frequencies.
     */
    private static final Map<Character, List<Phoneme>> DIGIT_PHONEMES = Map.of(
            // "zero" -> z + ih + r + ow
            '0', List.of(
                    Phoneme.voicedFric(100, 300, 1800, 5000),  // z
                    Phoneme.vowel(120, 400, 1900),              // ih (short)
                    Phoneme.vowel(80, 500, 1300),               // r-colored
                    Phoneme.vowel(200, 500, 900)                // ow
            ),
            // "one" -> w + ah + n
            '1', List.of(
                    Phoneme.vowel(80, 400, 800),                // w (glide)
                    Phoneme.vowel(200, 730, 1100),              // ah
                    Phoneme.nasal(120, 300, 1400)               // n
            ),
            // "two" -> t + uw
            '2', List.of(
                    Phoneme.silence(30),
                    Phoneme.burst(30, 4000),                    // t burst
                    Phoneme.fricative(40, 4000),                // t aspiration
                    Phoneme.vowel(250, 300, 870)                // uw
            ),
            // "three" -> th + r + iy
            '3', List.of(
                    Phoneme.fricative(120, 6000),               // th
                    Phoneme.vowel(60, 500, 1300),               // r
                    Phoneme.vowel(250, 270, 2300)               // iy
            ),
            // "four" -> f + ao + r
            '4', List.of(
                    Phoneme.fricative(100, 7000),               // f
                    Phoneme.vowel(200, 570, 840),               // ao
                    Phoneme.vowel(100, 500, 1300)               // r
            ),
            // "five" -> f + ay + v
            '5', List.of(
                    Phoneme.fricative(100, 7000),               // f
                    Phoneme.vowel(150, 730, 1100),              // ah (diphthong start)
                    Phoneme.vowel(100, 400, 2000),              // ih (diphthong end)
                    Phoneme.voicedFric(80, 300, 1400, 7000)     // v
            ),
            // "six" -> s + ih + k + s
            '6', List.of(
                    Phoneme.fricative(120, 6000),               // s
                    Phoneme.vowel(150, 400, 1900),              // ih
                    Phoneme.silence(40),                         // k closure
                    Phoneme.burst(20, 2500),                    // k burst
                    Phoneme.fricative(100, 6000)                // s
            ),
            // "seven" -> s + eh + v + eh + n
            '7', List.of(
                    Phoneme.fricative(100, 6000),               // s
                    Phoneme.vowel(120, 530, 1850),              // eh
                    Phoneme.voicedFric(60, 300, 1400, 7000),    // v
                    Phoneme.vowel(80, 500, 1700),               // short eh/schwa
                    Phoneme.nasal(100, 300, 1400)               // n
            ),
            // "eight" -> ey + t
            '8', List.of(
                    Phoneme.vowel(150, 500, 1800),              // eh (diphthong start)
                    Phoneme.vowel(100, 350, 2100),              // ih (diphthong end)
                    Phoneme.silence(30),                         // t closure
                    Phoneme.burst(25, 4000)                     // t burst
            ),
            // "nine" -> n + ay + n
            '9', List.of(
                    Phoneme.nasal(80, 300, 1400),               // n
                    Phoneme.vowel(150, 730, 1100),              // ah (diphthong start)
                    Phoneme.vowel(100, 400, 2000),              // ih (diphthong end)
                    Phoneme.nasal(100, 300, 1400)               // n
            )
    );

    /**
     * Synthesize speech for a CAPTCHA code string.
     *
     * @param code the CAPTCHA code (digits)
     * @param gapMs gap between digit words in milliseconds
     * @return raw PCM audio data (16-bit signed, little-endian, mono, 16kHz)
     */
    byte[] synthesize(String code, double gapMs) {
        // Calculate total duration
        double totalMs = 0;
        for (int i = 0; i < code.length(); i++) {
            char ch = code.charAt(i);
            List<Phoneme> phonemes = DIGIT_PHONEMES.get(ch);
            if (phonemes != null) {
                for (Phoneme p : phonemes) {
                    totalMs += p.durationMs();
                }
            }
            if (i < code.length() - 1) {
                totalMs += gapMs;
            }
        }

        int totalSamples = (int) (totalMs / 1000.0 * SAMPLE_RATE);
        double[] samples = new double[totalSamples];
        int offset = 0;

        for (int i = 0; i < code.length(); i++) {
            char ch = code.charAt(i);
            List<Phoneme> phonemes = DIGIT_PHONEMES.get(ch);
            if (phonemes == null) {
                // Unknown char: add silence
                int silSamples = (int) (300.0 / 1000.0 * SAMPLE_RATE);
                offset += silSamples;
                continue;
            }

            // Slight pitch variation per digit for naturalness
            double pitchFactor = 0.95 + RNG.nextDouble() * 0.1;

            for (Phoneme p : phonemes) {
                int segSamples = (int) (p.durationMs() / 1000.0 * SAMPLE_RATE);
                renderPhoneme(p, samples, offset, segSamples, pitchFactor);
                offset += segSamples;
            }

            // Gap between digits
            if (i < code.length() - 1) {
                int gapSamples = (int) (gapMs / 1000.0 * SAMPLE_RATE);
                offset += gapSamples;
            }
        }

        // Normalize and convert to 16-bit PCM
        double maxAbs = 0;
        for (double s : samples) {
            if (Math.abs(s) > maxAbs) {
                maxAbs = Math.abs(s);
            }
        }
        if (maxAbs < 1e-6) {
            maxAbs = 1.0;
        }

        byte[] pcm = new byte[totalSamples * 2];
        for (int i = 0; i < totalSamples; i++) {
            double normalized = samples[i] / maxAbs * 0.85;
            short val = (short) (normalized * Short.MAX_VALUE);
            pcm[i * 2] = (byte) (val & 0xFF);
            pcm[i * 2 + 1] = (byte) ((val >> 8) & 0xFF);
        }

        return pcm;
    }

    float getSampleRate() {
        return SAMPLE_RATE;
    }

    // ---- Internal synthesis methods ----

    private void renderPhoneme(Phoneme p, double[] out, int offset, int length, double pitchFactor) {
        if (offset + length > out.length) {
            length = out.length - offset;
        }
        if (length <= 0) {
            return;
        }

        int fadeLen = Math.min(length / 8, (int) (0.005 * SAMPLE_RATE)); // 5ms fade

        switch (p.type()) {
            case VOWEL -> renderVowel(p, out, offset, length, fadeLen, pitchFactor);
            case NASAL -> renderNasal(p, out, offset, length, fadeLen, pitchFactor);
            case FRICATIVE -> renderFricative(p, out, offset, length, fadeLen);
            case VOICED_FRIC -> renderVoicedFricative(p, out, offset, length, fadeLen, pitchFactor);
            case PLOSIVE_BURST -> renderBurst(p, out, offset, length);
            case SILENCE -> { /* leave zeros */ }
            default -> { /* unknown type, skip */ }
        }
    }

    /**
     * Vowel: glottal pulse train filtered through two resonators (F1, F2).
     * Uses a simple additive formant approach for clarity.
     */
    private void renderVowel(Phoneme p, double[] out, int offset, int length, int fadeLen, double pitchFactor) {
        double f0 = p.f0() * pitchFactor;
        double f1 = p.f1();
        double f2 = p.f2();

        for (int i = 0; i < length; i++) {
            double t = i / (double) SAMPLE_RATE;

            // Glottal source: sum of harmonics (pulse-like waveform)
            double source = glottalSource(t, f0);

            // Simple formant filtering via resonant addition
            // Each formant adds a band of energy; we simulate by amplitude-weighting
            // harmonics near the formant frequencies
            double signal = formantFilter(source, t, f0, f1, f2);

            // Apply envelope
            double env = envelope(i, length, fadeLen);
            out[offset + i] += signal * env * 0.6;
        }
    }

    /**
     * Nasal: similar to vowel but with a lower-amplitude, more muffled quality.
     * Reduced higher formant, added nasal murmur around 250-300 Hz.
     */
    private void renderNasal(Phoneme p, double[] out, int offset, int length, int fadeLen, double pitchFactor) {
        double f0 = p.f0() * pitchFactor;

        for (int i = 0; i < length; i++) {
            double t = i / (double) SAMPLE_RATE;
            double source = glottalSource(t, f0);

            // Nasal: strong low formant (~300 Hz), weak higher formants
            double signal = 0.6 * resonator(source, t, f0, 300)
                          + 0.2 * resonator(source, t, f0, p.f2());

            double env = envelope(i, length, fadeLen);
            out[offset + i] += signal * env * 0.5;
        }
    }

    /**
     * Unvoiced fricative: band-passed noise.
     */
    private void renderFricative(Phoneme p, double[] out, int offset, int length, int fadeLen) {
        double centerFreq = p.noiseFreq();

        for (int i = 0; i < length; i++) {
            double noise = RNG.nextGaussian();
            // Simple band-pass simulation: multiply noise by a sine at center freq
            double t = i / (double) SAMPLE_RATE;
            double filtered = noise * (0.5 + 0.5 * Math.sin(2 * Math.PI * centerFreq * t));

            double env = envelope(i, length, fadeLen);
            out[offset + i] += filtered * env * 0.3;
        }
    }

    /**
     * Voiced fricative: mix of voiced signal and noise.
     */
    private void renderVoicedFricative(Phoneme p, double[] out, int offset, int length, int fadeLen, double pitchFactor) {
        double f0 = p.f0() * pitchFactor;
        double centerFreq = p.noiseFreq();

        for (int i = 0; i < length; i++) {
            double t = i / (double) SAMPLE_RATE;

            // Voiced component
            double voiced = glottalSource(t, f0) * 0.3;

            // Noise component
            double noise = RNG.nextGaussian();
            double filtered = noise * (0.5 + 0.5 * Math.sin(2 * Math.PI * centerFreq * t));

            double env = envelope(i, length, fadeLen);
            out[offset + i] += (voiced + filtered * 0.25) * env * 0.4;
        }
    }

    /**
     * Plosive burst: short burst of noise.
     */
    private void renderBurst(Phoneme p, double[] out, int offset, int length) {
        double centerFreq = p.noiseFreq();

        for (int i = 0; i < length; i++) {
            double t = i / (double) SAMPLE_RATE;
            double noise = RNG.nextGaussian();
            double filtered = noise * (0.5 + 0.5 * Math.sin(2 * Math.PI * centerFreq * t));

            // Quick decay envelope
            double decay = Math.exp(-5.0 * i / length);
            out[offset + i] += filtered * decay * 0.5;
        }
    }

    /**
     * Glottal source: sawtooth-like waveform with soft rolloff (Rosenberg model approximation).
     * Produces a richer harmonic spectrum than a pure sine.
     */
    private double glottalSource(double t, double f0) {
        double period = 1.0 / f0;
        double phase = (t % period) / period; // 0..1 within each glottal cycle

        // Open phase (0..0.6): rising then falling
        // Closed phase (0.6..1.0): zero
        if (phase < 0.4) {
            // Rising: half-sine
            return Math.sin(Math.PI * phase / 0.4);
        } else if (phase < 0.6) {
            // Falling: cosine drop
            double fp = (phase - 0.4) / 0.2;
            return Math.cos(Math.PI * fp / 2.0);
        } else {
            return 0.0; // closed phase
        }
    }

    /**
     * Simple formant filtering: amplify harmonic content near formant frequencies.
     * Uses additive synthesis of the source modulated by formant resonance curves.
     */
    private double formantFilter(double source, double t, double f0, double f1, double f2) {
        // Reconstruct with formant emphasis by adding resonated components
        double r1 = resonator(source, t, f0, f1);
        double r2 = resonator(source, t, f0, f2);

        return 0.55 * r1 + 0.35 * r2;
    }

    /**
     * Resonator: re-synthesize the source with harmonics weighted by proximity
     * to the target formant frequency. This is a simplified approach.
     */
    private double resonator(double source, double t, double f0, double formantFreq) {
        // Sum harmonics of f0, weighted by a Gaussian centered at formantFreq
        // Only sum first ~20 harmonics for performance
        double bw = 120.0; // bandwidth of resonance
        double sum = 0;
        int maxHarmonic = Math.min(20, (int) (SAMPLE_RATE / 2 / f0));

        for (int h = 1; h <= maxHarmonic; h++) {
            double hFreq = h * f0;
            // Gaussian weight centered at formant frequency
            double weight = Math.exp(-0.5 * Math.pow((hFreq - formantFreq) / bw, 2));
            if (weight < 0.01) {
                continue; // skip negligible harmonics
            }
            sum += weight * Math.sin(2 * Math.PI * hFreq * t);
        }

        return sum;
    }

    /**
     * Smooth fade-in / fade-out envelope to prevent clicks.
     */
    private double envelope(int sample, int totalLength, int fadeLen) {
        if (fadeLen <= 0) {
            return 1.0;
        }
        if (sample < fadeLen) {
            return (double) sample / fadeLen;
        } else if (sample > totalLength - fadeLen) {
            return (double) (totalLength - sample) / fadeLen;
        }
        return 1.0;
    }
}
