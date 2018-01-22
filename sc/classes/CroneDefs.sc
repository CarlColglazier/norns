// utility class to load synthdefs required by crone engines.
// engines could of course define their own defs, this is for shared functionality.
CroneDefs {
    *sendDefs { arg s;
        postln("CroneDefs: sending defs");

        // single read head with fade in/out trigger
        SynthDef.new(\play_fade, {
            arg out=0, buf=0,
            start=0, trig=0, rate=1.0, loop=0,
            gate, fade_time=0.2, fade_shape=0.0,
            mute=0, level=1.0;
            var snd, aenv, amp, phase;
            phase = Sweep.ar(InTrig.kr(trig), BufSampleRate.kr(buf) * rate);
            snd =  BufRd.ar(1, buf, phase + start, loop:loop);
            aenv = Env.asr(fade_time, 1.0, fade_time, fade_shape);
            amp = EnvGen.ar(aenv, gate);
            amp = amp * Lag.ar(K2A.ar(level * (1 - mute)));
            Out.ar(out, (snd * amp));
        }).send(s);

        // mono patch with smoothing
        SynthDef.new(\patch_mono, {
            arg in, out, level=1.0, lag=0.01;
            var ampenv = Lag.ar(K2A.ar(level), lag);
            Out.ar(out, In.ar(in) * ampenv);
        }).send(s);

        // mono patch with smoothing and feedback
        // (InFeedback introduces 1 audio block of delay)
        SynthDef.new(\patch_mono_fb, {
            arg in, out, level=1.0, lag=0.01;
            var ampenv = Lag.ar(K2A.ar(level), lag);
            Out.ar(out, InFeedback.ar(in) * ampenv);
        }).send(s);

		// stereo patch with smoothing
        SynthDef.new(\patch_stereo, {
            arg in, out, level=1.0, lag=0.01;
            var ampenv = Lag.ar(K2A.ar(level), lag);
            Out.ar(out, In.ar(in, 2) * ampenv);
        }).send(s);

		// mono->stereo patch with smoothing and pan
		SynthDef.new(\patch_pan, {
			arg in, out, level=1.0, pan=0, lag=0.01;
			var ampenv = Lag.ar(K2A.ar(level), lag);
			var panenv = Lag.ar(K2A.ar(pan), lag);
			Out.ar(out, Pan2.ar(In.ar(in) * ampenv, pan));
		}).send(s);

        // record with some level smoothing
        SynthDef.new(\rec_smooth, {
            arg buf, in, offset=0, rec=1, pre=0, lag=0.01,
            run=1, loop=0, trig=0, done=0;
            var ins, pres, recs;
            ins = In.ar(in);
            pres = Lag.ar(K2A.ar(pre), lag);
            recs = Lag.ar(K2A.ar(rec), lag);
            RecordBuf.ar(ins, buf,
                recLevel:rec, preLevel:pre,
                offset:offset, trigger: InTrig.kr(trig),
                loop:0, doneAction: done);
        }).send(s);

        // raw mono adc input
        SynthDef.new(\adc, {
			arg in, out;
			Out.ar(out, SoundIn.ar(in))
		}).send(s);

		// envelope follower (audio input, control output)
		SynthDef.new(\amp_env, {
			arg in, out, atk=0.01, rel=0.25;
			//			var amp = abs(A2K.kr(In.ar(in)));
			//			Out.kr(out, LagUD.kr(amp, atk, rel));
			var absin = abs(In.ar(in));
			var amp = A2K.kr(LagUD.ar(absin, atk, rel));
			Out.kr(out, amp);
		}).send(s);

		// pitch follower
		SynthDef.new(\pitch, {
			arg in, out,
			initFreq = 440.0, minFreq = 60.0, maxFreq = 4000.0,
			execFreq = 100.0, maxBinsPerOctave = 16, median = 1,
			ampThreshold = 0.01, peakThreshold = 0.5, downSample = 1, clar=0;
			// Pitch ugen outputs an array of two values:
			// first value is pitch, second is a clarity value in [0,1]
			// if 'clar' argument is 0 (default) then clarity output is binary
			var pc = Pitch.kr(in,
				initFreq , minFreq , maxFreq ,
				execFreq , maxBinsPerOctave , median ,
				ampThreshold , peakThreshold , downSample , clar
			);
			Out.kr(out, pc);
		}).send(s);
    }

}