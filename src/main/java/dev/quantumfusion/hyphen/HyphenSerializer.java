package dev.quantumfusion.hyphen;

public interface HyphenSerializer<D, IO> {
	D decode(IO io);

	void encode(D data, IO io);
}