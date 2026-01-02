package gavinx.fea.api;

/** Directional IO mode for FE. */
public enum FESideMode {
	NONE,
	IN,
	OUT,
	BOTH;

	public boolean canInsert() {
		return this == IN || this == BOTH;
	}

	public boolean canExtract() {
		return this == OUT || this == BOTH;
	}
}
